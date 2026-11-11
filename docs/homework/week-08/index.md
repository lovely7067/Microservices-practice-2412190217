# 第 08 周作业记录（OpenFeign 跨服务调用 + 负载均衡验证 + 端到端测试）

> 作业目标：服务间同步调用 + 测试验证协作流程。评分构成：跨服务流程设计 25% / OpenFeign 实现 30% / 负载均衡验证 15% / 测试脚本 20% / 文档与截图 10%。

## 1. 选择一个跨服务业务流程

- **设计**：选择两条必须跨服务的流程——办卡（card-service 调 member-service 校验会员）与入场（checkin-service 调 card-service 验卡扣次）。调用链路图（Mermaid）、调用方/被调方/接口路径见 `docs/service-communication.md`。
- **完成情况**：两条链路全部实现并可复现；任一校验失败调用方中止且不写本地数据。
- **关键结论**：跨服务引用只存 ID 不建外键，引用完整性由调用时校验保证。

## 2. 使用 OpenFeign

- **实现思路**：
  - 三服务统一 BOM（Spring Cloud 2025.1.0 + SCA 2025.1.0.0）新增 `spring-cloud-starter-openfeign` 与 `spring-cloud-starter-loadbalancer`；
  - `MemberClient`（card）与 `CardClient`（checkin）由 RestTemplate 手写调用改为 **@FeignClient 接口**：`name` = 被调方 `spring.application.name`（服务名调用，不写死容器 IP），`path` = 被方业务前缀；
  - 启动类 `@EnableFeignClients`；第 07 周的 `DiscoveryInstanceResolver`（手动解析实例）与 `RestTemplateConfig` 删除，由 Feign + LoadBalancer 接管；
  - **失败处理**：`FeignErrorConfig` 的 `ErrorDecoder` 把被调方 404 → `NotFoundException`（404 透传）、其余状态 → `RemoteServiceException`（503）；Feign 本地异常（无可用实例等，无 HTTP 响应不走 ErrorDecoder）由新增 `GlobalExceptionHandler` 统一映射 503，错误信息含服务名、状态码与"未写入数据"结论；
  - **超时**：`spring.cloud.openfeign.client.config.default` 连接 2s / 读取 3s（替代原 RestTemplate 超时）；`Retryer.NEVER_RETRY` 关闭自动重试。
- **运行命令**：

  ```bash
  docker compose up -d --build
  bash scripts/e2e-test.sh
  ```

- **结果**：6 次经 Feign 办卡全部 201；404（会员不存在）/ 503（依赖不可用）语义与直连版一致；三服务测试 11+10+4=25 全部 BUILD SUCCESS。

## 3. 负载均衡验证

- **实现思路**：
  - `compose.yaml` 新增 `member-service-2`（同名注册 `member-service`，宿主端口 **8085** 避免与实例 1 的 8081 冲突）；
  - member-service 每个实例注册 `INSTANCE_NAME` 环境变量，并用 `InstanceTagFilter` 对每个 `/api/*` 请求打日志 `GYM-INSTANCE <实例名> <方法> <路径>`；
  - 验证方式为**请求日志计数**（不是只看注册列表）：连续 6 次经 card-service 办卡，统计两实例容器日志新增的命中数，两实例均被访问才通过；
  - 修正一个实测发现的问题：调用方与被调方同时启动时存在注册时序竞争（订阅列表缺实例、轮询全命中单实例），给 member/card/checkin 加 actuator healthcheck、调用方 `depends_on: service_healthy` 后启动，实测轮询恢复 3/3 分布。
- **运行命令**：`bash scripts/e2e-test.sh`（检查 2 输出实例 IP:端口，检查 4 输出两实例命中分布）。
- **结果**：member-service 2 个 healthy 实例（172.19.0.6:8081 / 172.19.0.7:8081）；6 次办卡命中 member-1 ×3、member-2 ×3，轮询分布均匀。

## 4. 测试

- **实现思路**：
  - **Feign client 测试**：`FeignErrorDecoderTest`（card/checkin）——不发真实 HTTP，验证 ErrorDecoder 把 404 解码为 NotFoundException、500/503 解码为 RemoteServiceException，且异常不可被静默吞掉；
  - **service 层测试**（沿用并覆盖新语义）：Mockito 模拟 Feign 客户端，办卡/入场在远程 404、503 时中止且**不写本地数据**（确认不会把调用失败当成业务成功）；
  - **端到端**：`scripts/e2e-test.sh` 从仓库根目录运行，10 项检查（控制台就绪、2 实例注册、4 端口健康、6 次办卡全 201、双实例日志命中、办卡→入场全链路 201），任一失败 exit 1；
  - **写操作超时说明**：Feign `NEVER_RETRY`，写请求超时后结果可能在被调方已生效（未知状态），不做自动重试，按记录幂等核对；本作业统一不重试，失败明确返回 503。
- **运行命令**：

  ```bash
  monolith\mvnw.cmd -f services/card-service/pom.xml test     # Tests run: 11, Failures: 0, Errors: 0
  monolith\mvnw.cmd -f services/checkin-service/pom.xml test  # Tests run: 10, Failures: 0, Errors: 0
  monolith\mvnw.cmd -f services/member-service/pom.xml test   # Tests run: 4,  Failures: 0, Errors: 0
  bash scripts/e2e-test.sh                                    # 通过 10 / 失败 0, 退出码 0
  ```

- **结果**：三服务单元/集成测试 25 个全部通过；e2e 10 项检查全部 PASS。

## 5. 交付物清单（GitHub）

| 内容 | 位置 |
| --- | --- |
| OpenFeign 依赖 / 客户端 / 调用逻辑 / 失败处理 | `services/*/pom.xml`、`services/card-service/.../client/MemberClient.java`、`services/checkin-service/.../client/CardClient.java`、`config/FeignErrorConfig.java`、`exception/GlobalExceptionHandler.java` |
| 多实例编排 | `compose.yaml`（member-service-2、healthcheck 依赖链） |
| 端到端脚本 | `scripts/e2e-test.sh` |
| 调用链路图文档 | `docs/service-communication.md` |
| 测试 | `services/*/src/test/.../FeignErrorDecoderTest.java` 等 |
| 截图 | `docs/homework/week-08/screenshots/` |

截图清单（`screenshots/`）：

| 文件 | 内容 | 证明点 |
| --- | --- | --- |
| `openfeign-01-feign-client-code.png` | IDEA 中的 `MemberClient.java` | `@FeignClient(name="member-service", path="/api/members")` 服务名调用，不写死容器 IP |
| `openfeign-02-error-decoder-code.png` | IDEA 中的 `FeignErrorConfig.java` | 404→NotFoundException 透传、其余→RemoteServiceException(503)，错误信息含服务名与状态码 |
| `openfeign-03-e2e-test-output.png` | `bash scripts/e2e-test.sh` 完整输出 | 2 个 healthy 实例、6 次经 Feign 办卡全 201、双实例命中 3/3、全链路 201、通过 10/失败 0 |
