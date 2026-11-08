# 代码模块导读（微服务部分 · 第 06–07 周）

> 用途：汇报答辩时快速定位代码、应对提问。配合 `docs/microservices-design.md`（设计视角）一起看。
> 仓库：https://github.com/lovely7067/Microservices-practice-2412190217

---

## 1. 仓库结构总览

```
gym/
├── monolith/                 # 第 01–05 周单体（保留可运行，未参与微服务调用链）
├── services/
│   ├── member-service/       # 会员档案（8081，库 gym_member，账号 member_u）
│   ├── card-service/         # 会员卡（8082，库 gym_card，账号 card_u）
│   └── checkin-service/      # 入场核验（8083，库 gym_checkin，账号 checkin_u）
├── docker/mysql-init.sql     # 微服务专用 MySQL 初始化：3 库 + 3 账号 GRANT
├── compose.yaml              # 一键编排 7 容器（含 Nacos v3.2.4）
├── scripts/smoke-test.sh     # 冒烟测试（7 项检查，任一失败 exit 1）
├── week06-api-test.http      # IDEA HTTP Client 请求集（10 个请求，可复跑）
└── docs/
    ├── microservices-design.md       # 拆分设计 + 架构图 + Nacos 章节
    ├── code-walkthrough.md           # 本文件
    └── homework/week-06|07/          # 作业记录与截图
```

每个服务内部是标准分层，以 card-service 为例：

```
com.zjgsu.yz.card
├── CardServiceApplication        # 启动类（@SpringBootApplication）
├── model/                        # JPA 实体与枚举（CardType/CardStatus/MembershipCard）
├── repository/                   # Spring Data JPA 接口
├── service/                      # 业务逻辑（跨服务校验在这里）
├── web/                          # REST 控制器 + DTO（请求/响应模型）
├── client/                       # 跨服务 HTTP 客户端（MemberClient + DiscoveryInstanceResolver）
├── config/                       # RestTemplate 超时配置
└── exception/                    # 业务异常（NotFoundException / InvalidRequestException / RemoteServiceException）
```

## 2. 服务职责与接口一览

| 服务 | 接口 | 说明 |
| --- | --- | --- |
| member-service | `GET /api/members`、`GET /api/members/{id}`、`POST /api/members`、`GET /actuator/health` | 会员登记（手机号唯一 409）、列表、详情 |
| card-service | `POST /api/cards`、`GET /api/cards/{id}`、`GET /api/cards?memberId=`、`GET /actuator/health` | 办卡前**必须**远程校验会员存在 |
| checkin-service | `POST /api/checkins`、`GET /api/checkins`、`GET /actuator/health` | 入场前**必须**远程验卡（本人 + 有效期 + 状态 + 次数） |

- 每个 Controller 都很薄：参数校验（`@Valid` + DTO 注解）→ 调 Service → DTO 返回。
- 全局异常处理：`@RestControllerAdvice` 把异常映射为 HTTP 状态码（404/400/409/503），Controller 不写 try/catch。

## 3. 核心代码讲解（最可能被问的部分）

### 3.1 `client/DiscoveryInstanceResolver` —— 服务发现寻址（第 07 周新增）

```java
@Component
public class DiscoveryInstanceResolver {
    private final DiscoveryClient discoveryClient;   // 构造器注入，Spring Cloud 自动装配

    public URI resolve(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (instances == null || instances.isEmpty()) {
            throw new RemoteServiceException("服务无可用实例: " + serviceId);   // → 503
        }
        return instances.get(0).getUri();            // 本周只取第一个实例
    }
}
```

**要点：**
- 每次**业务操作前**调用（不是启动时调用一次存下来），因此实例重启换 IP、扩缩容都能感知。
- 拿不到实例就抛 503，**不会拿着旧地址盲发请求**。
- `DiscoveryClient` 是 Spring Cloud 的抽象接口，Nacos 实现（`NacosDiscoveryClient`）由 starter 自动注册，代码不直接依赖 Nacos API——这也是测试可以 Mock 它的原因。
- 本周明确不做：轮询/随机负载均衡、权重路由、自动重试、熔断（取第一个实例）。

### 3.2 `client/MemberClient`（card-service）—— 跨服务 HTTP 调用

```java
public MemberDto getMember(Long memberId) {
    URI baseUri = instanceResolver.resolve(MEMBER_SERVICE_ID);   // 每次解析
    String url = baseUri + "/api/members/" + memberId;           // 实例 URI + 原业务路径
    try {
        ResponseEntity<MemberDto> resp = restTemplate.getForEntity(url, MemberDto.class);
        return resp.getBody();
    } catch (HttpClientErrorException.NotFound e) {
        throw new NotFoundException("会员不存在: id=" + memberId);      // 404 原样转业务异常
    } catch (RestClientException e) {
        throw new RemoteServiceException("member-service 调用失败", e);  // 超时/拒绝连接 → 503
    }
}
```

**要点：**
- `MEMBER_SERVICE_ID = "member-service"`（public static final），与对方 `spring.application.name` 一致；checkin-service 的 `CardClient` 同构（`CARD_SERVICE_ID = "card-service"`）。
- 错误翻译规则：目标返回 404 → 业务 404；连接失败/超时等 `RestClientException` → 503。校验不通过绝不写本地数据。
- `MemberDto` 是跨服务契约（本服务自己的副本类），不依赖对方实体——服务间只共享 JSON 契约，不共享代码和数据库。

### 3.3 `service/MembershipCardService` —— 办卡业务规则

`createCard(request)` 的顺序（顺序就是考点）：
1. **先远程校验**：`memberClient.getMember(request.getMemberId())`——404/503 直接抛出，本地不落任何数据；
2. 校验有效期 `startDate < endDate`（400）；
3. 次卡必须带 `remainingTimes`（400）；
4. 通过后才 `repository.save()`，返回 `CardResponse`。

> 老师若问"远程成功、本地失败怎么办"：确实存在这个窗口（会员校验通过后 member-service 数据变化、或本地落库失败），这是跨服务数据一致性固有问题；本周不做补偿/分布式事务，在文档中明确说明，改进方向是 Saga/消息最终一致。

### 3.4 `service/CheckinService` —— 入场核验规则

`createCheckin` 校验链：卡存在（404）→ 卡属于该会员（400）→ 状态 ACTIVE（400）→ 未过期（400）→ 次卡有剩余次数并**扣减一次**（`@Transactional` 保证扣减与写入场记录原子）→ 写 `CheckinRecord`（跨服务引用只存 memberId/cardId，不建外键）。

### 3.5 `config/RestTemplateConfig` —— 超时配置

```java
@Value("${http-client.connect-timeout-ms:2000}") int connectTimeout;
@Value("${http-client.read-timeout-ms:3000}")    int readTimeout;
// SimpleClientHttpRequestFactory 设置 connect/read 超时，防止被依赖服务拖垮
```

### 3.6 配置文件要点（三个服务结构相同）

```yaml
spring:
  application.name: member-service        # 注册到 Nacos 的服务名
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}   # Compose 中为 nacos:8848
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:}
        group: ${NACOS_GROUP:GYM_YZ_GROUP}                 # 个人项目独立分组
```

- 凭据/地址全部环境变量化（12-Factor），真实值只在本地 `.env`，仓库只提交 `.env.example` 占位模板。
- **测试配置** `src/test/resources/application.yml` 额外加了：
  `spring.cloud.nacos.discovery.enabled=false` 与 `spring.cloud.service-registry.auto-registration.enabled=false`——测试不连真实 Nacos，配 H2 内存库，本地就能 `mvnw test`。

### 3.7 `compose.yaml` 中 Nacos 的编排

- 镜像 `nacos/nacos-server:v3.2.4`；控制台 **18080**:8080（v3 控制台独立端口）、服务端 8848、SDK gRPC 9848；
- 鉴权：`NACOS_AUTH_ENABLE=true` + `NACOS_AUTH_TOKEN`（Base64 ≥32 字节）+ identity key/value；首次用 `POST /nacos/v3/auth/user/admin` 初始化管理员密码；
- `depends_on: nacos: condition: service_healthy`（带 readiness 健康检查），保证先注册中心后业务服务；
- 数据卷 `gym-nacos-data` 持久化。

### 3.8 `scripts/smoke-test.sh`（7 项检查，任一失败 exit 1）

1. Nacos 控制台就绪（`GET :18080/v3/console/health/readiness`）；
2. 用 `.env` 凭据登录 Nacos，逐一查三个服务在 `GYM_YZ_GROUP` 的实例列表（需 `healthy=true`）；
3. 三个服务 `/actuator/health` = 200 UP。
脚本只输出 PASS/FAIL，不打印密码与 Token。

## 4. 测试体系

| 层 | 内容 | 依赖 |
| --- | --- | --- |
| 业务单元测试 | Mockito 模拟 `MemberClient`/`CardClient`：成功 / 404 / 503 / 有效期 / 次卡 / 冻结卡 | 无外部依赖 |
| 服务发现测试 | `DiscoveryInstanceResolverTest`（card/checkin 各 2 例）：Mock `DiscoveryClient`，覆盖**空实例抛 503** 与"取第一个实例 URI" | 无外部依赖 |
| 上下文测试 | `*ApplicationTests.contextLoads`（配置关闭自动注册） | H2 |
| 冒烟测试 | smoke-test.sh 对运行中的容器做端到端检查 | Docker |

数量：member 4 / card 9 / checkin 9 = **22 全部通过**。

## 5. 老师可能问的问题（速答）

**Q1：为什么每次操作都 `getInstances`，而不是启动时解析一次缓存地址？**
缓存地址在对方重启换 IP、扩缩容时立即失效；每次解析才能体现服务发现的意义。代价是每次调用多一次本地内存查询（Nacos 客户端有本地订阅缓存，不是每次都走网络）。

**Q2：为什么用 RestTemplate 而不是 OpenFeign？**
课程要求理解服务发现的底层过程（自己 resolve 实例、拼 URL、处理错误码）；Feign 是在这一步之上再封装。骨架上换 Feign 只需把 `resolve+getForEntity` 换成接口代理，业务层不用动。

**Q3：只取第一个实例，多实例怎么办？**
本周是故意的简化边界（作业明确允许）。改进：`instances.get(index % size)` 或引入 Spring Cloud LoadBalancer 做轮询。

**Q4：503 和 404 的区别？**
404 是"依赖服务正常工作并告诉你资源不存在"（业务错误，会透传给客户端）；503 是"依赖本身不可达/无实例"（基础设施错误）。两者都不写本地数据。

**Q5：为什么跨服务不建数据库外键？**
三服务三库物理隔离，外键跨不了库；引用完整性由调用时校验保证（办卡时验会员、入场时验卡）。这是微服务数据自治的标准做法。

**Q6：办卡校验通过后，member-service 数据变了怎么办？（一致性窗口）**
存在该窗口。本周方案是"先验后写 + 失败不写"，不做自动补偿；改进方向是 Saga / 消息最终一致性。文档中有明确说明。

**Q7：Nacos 的 healthy 和 Actuator health 什么关系？**
独立视角。Nacos healthy 来自 SDK 心跳（5s 一次，15s 未见标 false，30s 剔除），只说明进程/网络在；Actuator 检查应用内部（数据源等）。MySQL 挂了 Actuator=DOWN，但心跳还在，Nacos 不会摘除实例（本系统未把健康状态回调给 Nacos）。

**Q8：测试为什么不连真实 Nacos？**
单测要快且稳定。测试配置关闭自动注册（`discovery.enabled=false` + `auto-registration.enabled=false`），用 Mockito 模拟 `DiscoveryClient` 和跨服务客户端，H2 替代 MySQL——`mvnw test` 任何机器都能跑。

**Q9：group（GYM_YZ_GROUP）和 namespace 的区别？**
namespace 是最大的隔离单元（本作业用默认 public），group 是 namespace 内的服务逻辑分组，用于区分不同项目/环境，防止同学间同名服务互相调用。个人项目用独立分组与课堂示例隔离。

**Q10：控制台为什么是 18080 而不是 8848？**
Nacos 3.x 控制台从服务端拆出独立端口（容器内 8080），8848 只提供服务端 API，SDK 走 9848 gRPC。旧教程"8848/nacos 控制台"是 2.x 的用法。

**Q11：`depends_on: service_healthy` 解决什么问题？**
只保证启动顺序（Nacos 就绪后再启动业务服务），不保证调用时依赖一定可用——运行期可用性靠服务发现 + 503 兜底，这是两套机制。

**Q12：怎么在本地复跑验证？**
```bash
docker compose up -d nacos          # 首次先初始化管理员
curl -X POST "http://localhost:8848/nacos/v3/auth/user/admin" -d "password=你的密码"
docker compose up -d --build
bash scripts/smoke-test.sh          # 7 项全 PASS，退出码 0
monolith\mvnw.cmd -f services/card-service/pom.xml test   # 单服务测试
```
