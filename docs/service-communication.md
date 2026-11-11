# 跨服务业务流程与调用链路（第 08 周）

> 对应作业「选择一个跨服务业务流程」。本文档明确**调用方、被调方、接口路径**与调用链路图。

## 1. 选择的业务流程

选择 **会员卡业务（办卡）与 入场核验业务（入场）** 两条必须跨服务完成的流程：

| 流程 | 调用方 | 被调方 | 远程接口路径 |
| --- | --- | --- | --- |
| 办卡（校验会员存在） | `card-service` | `member-service` | `GET /api/members/{memberId}` |
| 入场（验卡：本人/有效期/状态/次数） | `checkin-service` | `card-service` | `GET /api/cards/{cardId}` |

场景真实性：办卡前必须确认会员存在（否则卡挂在幽灵会员名下）；入场前必须确认卡是本人的、在有效期内、状态正常、次卡还有剩余次数并扣减。任一校验不通过，调用方**中止流程、不写本地数据**。

## 2. 调用链路图

```mermaid
flowchart LR
    C[客户端] -->|POST /api/cards| CARD["card-service<br/>8082"]
    C -->|POST /api/checkins| CHK["checkin-service<br/>8083"]

    subgraph member-service 多实例（负载均衡）
        M1["member-1<br/>172.19.0.6:8081"]
        M2["member-2<br/>172.19.0.7:8081"]
    end

    CARD -->|"OpenFeign 服务名 member-service<br/>Nacos 发现 + LoadBalancer 轮询"| M1
    CARD --> M2
    CHK -->|"OpenFeign 服务名 card-service"| CARD
```

文字版链路：

1. **办卡链路**：`POST /api/cards`（card-service, 8082）→ `MemberClient#getMember`（OpenFeign，服务名 `member-service`）→ `GET /api/members/{memberId}`（member-service，2 实例轮询）→ 校验通过 → card-service 落库 `gym_card`。
2. **入场链路**：`POST /api/checkins`（checkin-service, 8083）→ `CardClient#getCard`（OpenFeign，服务名 `card-service`）→ `GET /api/cards/{cardId}`（card-service）→ 验卡 + 扣次 → checkin-service 落库 `gym_checkin`。

## 3. 调用实现要点

- **服务名调用**：`@FeignClient(name = "member-service", path = "/api/members")`，`name` 与被调方 `spring.application.name` 一致，不写死容器 IP；同一服务的多实例由 **Spring Cloud LoadBalancer 轮询**分发。
- **失败处理**（`FeignErrorConfig` ErrorDecoder + `GlobalExceptionHandler`）：
  - 被调方 404 → `NotFoundException` → HTTP 404（业务不存在，原样透传）；
  - 其他 HTTP 错误 / 连接失败 / 无可用实例 → `RemoteServiceException` / `FeignException` / `IllegalStateException` → HTTP 503（依赖不可用，明确报错）；
  - 错误信息清晰：包含被调方服务名、HTTP 状态码与"请求已中止、未写入数据"的结论。
- **超时**：连接 2s / 读取 3s（`spring.cloud.openfeign.client.config.default`）；`Retryer.NEVER_RETRY` 不自动重试——写操作超时后结果可能在远端已生效（未知状态），由人工按记录核对，避免重复请求造成歧义。
- **多实例部署**：`compose.yaml` 中 `member-service`（宿主 8081）与 `member-service-2`（宿主 8085）以相同 `spring.application.name` 注册，绑定**不同宿主机端口**；两个实例的容器日志各打印 `GYM-INSTANCE <实例名> <方法> <路径>`，作为负载均衡命中的直接证据（见 `scripts/e2e-test.sh` 检查 4）。

## 4. 验证

```bash
bash scripts/e2e-test.sh   # 从仓库根目录运行；10 项检查全过退出码 0
```

实测结果（2026-11-11）：member-service 注册 2 个 healthy 实例；6 次经 card-service 办卡全部 201，两实例日志命中 3/3 次轮询分布；办卡 → 入场全链路 201。