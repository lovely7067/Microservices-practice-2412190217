# 第 07 周作业：Nacos 服务注册与发现

在 [第 06 周微服务拆分](../week-06/index.md) 的基础上接入 Nacos，把跨服务业务调用从固定地址改为通过服务名发现实例。

---

## 1. Nacos 部署与注册

**做了什么**（修改原有工程，未新建一套）：

- 根目录 `compose.yaml` 新增 `nacos` 服务：镜像 `nacos/nacos-server:v3.2.4`，控制台映射 `18080:8080`（与 8848 服务端、9848 SDK gRPC 端口区分）；开启鉴权（`NACOS_AUTH_TOKEN` / `NACOS_AUTH_IDENTITY_KEY` / `NACOS_AUTH_IDENTITY_VALUE`，真实值只放本地 `.env`，提交 `.env.example` 占位模板）；注册数据存命名卷 `gym-nacos-data` 持久化
- 三个业务服务 pom 通过统一 BOM（`spring-cloud-alibaba-dependencies` **2025.1.0.0**，适配 Spring Boot 4.0.x）引入 `spring-cloud-starter-alibaba-nacos-discovery`，未单独覆盖 Nacos Client 版本，未引入 Config Starter
- 各服务 `application.yml` 增加注册配置：`server-addr=${NACOS_SERVER_ADDR:localhost:8848}`、`username/password` 环境变量注入、`group=${NACOS_GROUP:GYM_YZ_GROUP}`（个人项目独立分组）；Compose 中 `NACOS_SERVER_ADDR=nacos:8848`
- 服务名与 `docs/microservices-design.md` 一致：`member-service` / `card-service` / `checkin-service`（各自 `spring.application.name`）

**初始化过程**（先独立启动 Nacos，再启动业务服务）：

```bash
docker compose up -d nacos                                    # 1. 启动 Nacos（等控制台就绪）
curl -X POST "http://localhost:8848/nacos/v3/auth/user/admin" -d "password=你的密码"   # 2. 初始化管理员
docker compose up -d --build                                  # 3. 启动业务服务（depends_on nacos healthy）
```

**注册结果**（`nacos/v3/admin/ns/instance/list` 实测，分组 `GYM_YZ_GROUP`）：

| 服务 | 实例地址 | healthy |
| --- | --- | --- |
| member-service | 172.19.0.6:8081 | true |
| card-service | 172.19.0.7:8082 | true |
| checkin-service | 172.19.0.8:8083 | true |

控制台（http://localhost:18080 登录后选分组 `GYM_YZ_GROUP`）同样能看到 3 个服务与实例。容器共 7 个：`gym-nacos`（healthy）+ 第 06 周的 6 个。

## 2. 服务发现与业务调用

**选择的跨服务业务流程：办卡**（card-service 办卡前必须校验会员存在于 member-service；同时 checkin-service → card-service 的入场验卡也一并改造）。

- `DiscoveryInstanceResolver.resolve("member-service")`：每次操作都调用 `DiscoveryClient.getInstances()` 获取当前已知实例（**不在启动时读取一次并永久保存地址**），取第一个实例的 URI（本周不实现轮询/权重路由/重试/熔断）
- `MemberClient.getMember(memberId)`：用解析出的实例 URI + 原业务路径 `/api/members/{id}` 发送 HTTP 请求；HTTP 连接超时 2s、读取超时 3s 保留
- 已删除不再使用的固定地址配置：`member-service.base-url`、`CARD_SERVICE_BASE_URL` 环境变量等；仅保留注册中心地址、数据库与超时配置
- 失败处理：实例列表为空 → `RemoteServiceException`（**503**）；目标返回 404 → 原样抛 `NotFoundException`（**404** 业务响应）；**不跳过校验、不写本地数据**

**请求示例与结果**：

```
POST http://localhost:8082/api/cards
{"memberId":1,"cardType":"MONTHLY","startDate":"2026-11-01","endDate":"2027-11-01"}

实际结果：201 Created
{"id":4,"memberId":1,"cardType":"MONTHLY","status":"ACTIVE",...}
```

card-service 先经 DiscoveryClient 拿到 member-service 实例 `172.19.0.6:8081`，再向该实例发起 `GET /api/members/1` 校验通过后落库。

## 3. 上线、下线与恢复验证

在同一业务流程（办卡）上实测：

**① 全部依赖正常**：办卡 201（见上），`gym_card.membership_card` 记录数随成功 +1，业务数据变化正确。

**② 下线被调用服务**：

```bash
docker compose stop member-service
# 等待心跳超时（约 15-30 秒）后查询 Nacos：
# GET /nacos/v3/admin/ns/instance/list?serviceName=member-service&groupName=GYM_YZ_GROUP
# 实际结果：{"code":0,"message":"success","data":[]}   <- 实例已被摘除，列表为空

# 用新的有效业务参数发起办卡：
POST /api/cards {"memberId":1,"cardType":"YEARLY","startDate":"2026-11-05","endDate":"2027-11-05"}
# 实际结果：503 Service Unavailable（DiscoveryClient 无可用实例，明确返回依赖不可用错误）
# 数据检查：gym_card 记录数与下线前一致（下线前后各查一次 COUNT(*) 对比），确认没有绕过校验新增本地业务记录
```

**③ 恢复服务**：

```bash
docker compose start member-service
# 等待重新注册（实测约 30 秒内 healthy=true），再次办卡：
POST /api/cards {"memberId":1,"cardType":"YEARLY","startDate":"2026-11-05","endDate":"2027-11-05"}
# 实际结果：201 Created（id=5），恢复正常
```

**SDK 注册状态与 Actuator 应用健康的区别**（用自己的系统举例说明）：

- Nacos 中的实例状态（`healthy=true/false`）来自 **SDK 心跳**：客户端每 5 秒向 Nacos 发心跳，15 秒未收到标记 false，30 秒剔除——它反映"进程在不在、网络通不通"
- Actuator `/actuator/health` 反映**应用内部健康**（数据源连接、磁盘等）：例如 member-service 的 MySQL 连接失败时 Actuator 返回 `DOWN`，但进程还活着、心跳仍在发，Nacos 实例仍是 `healthy=true`
- 结论：**Actuator 返回 DOWN 不会自动使 Nacos 摘除实例**（本系统未配置把健康状态同步给 Nacos 的回调）；两者是独立的健康视角，下线验证以"容器停止 → 心跳停止 → Nacos 摘除"为准。服务发现也不消除跨服务写操作部分成功的业务窗口（如办卡成功后 member-service 立即损坏数据），不要求自动补偿或分布式事务

## 4. 脚本、测试与说明

**`scripts/smoke-test.sh`（第 07 周更新，从仓库根目录运行 `bash scripts/smoke-test.sh`）**：

- 检查 1：Nacos 控制台 HTTP 地址可访问（`http://localhost:18080/v3/console/health/readiness`，**不使用旧版 8848/nacos 控制台地址**）
- 检查 2：Nacos 注册列表——用 `.env` 中的分组与账号登录后查询三个业务服务，均应存在 `healthy=true` 的实例（输出实例 IP:端口）
- 检查 3：每个服务的 `/actuator/health` 均为 200 + `status=UP`
- 任一检查失败即 `exit 1`（退出非零），错误不会被忽略后仍报成功；脚本不打印密码/Token
- 实测结果：**通过 7 / 失败 0，退出码 0**

**单元/集成测试（不依赖真实 Nacos 或 MySQL）**：

- 测试配置（各服务 `src/test/resources/application.yml`）中 `spring.cloud.nacos.discovery.enabled=false` 且 `spring.cloud.service-registry.auto-registration.enabled=false`——关闭 Nacos 自动注册，沿用 H2 内存库与 Mockito 模拟远程响应
- 新增 `DiscoveryInstanceResolverTest`（card/checkin 各 2 个用例）：模拟 `DiscoveryClient`，覆盖**实例列表为空 → 抛依赖不可用错误**与"有实例 → 取第一个实例 URI"，不连接真实 Nacos
- 业务测试沿用第 06 周：跨服务校验成功/404/503/有效期/次卡规则等（Mockito 模拟 Client）

**运行测试的命令**（README 同步更新）：

```bash
# Windows（monolith 自带包装器）
monolith\mvnw.cmd -f services/member-service/pom.xml test
monolith\mvnw.cmd -f services/card-service/pom.xml test
monolith\mvnw.cmd -f services/checkin-service/pom.xml test
```

实测：member 4 / card 9 / checkin 9 全部 BUILD SUCCESS。

## 5. 提交内容清单

- Nacos 依赖与注册配置：三个 `services/<服务名>/pom.xml`、`application.yml`
- 服务发现调用代码：`DiscoveryInstanceResolver`、`MemberClient`、`CardClient`（固定地址配置已删除）
- 空实例测试：两个 `DiscoveryInstanceResolverTest`
- 包含 Nacos 的 `compose.yaml`、`.env.example`（占位模板，真实凭据在本地 `.env` 不提交）
- `scripts/smoke-test.sh`（Nacos 控制台 + 注册列表 + 服务健康）
- `docs/microservices-design.md` 追加 Nacos 章节；README 启动步骤与本页记录
- 截图见 `screenshots/`
