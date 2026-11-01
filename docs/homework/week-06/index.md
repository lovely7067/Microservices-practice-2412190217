# 第 06 周作业记录（微服务拆分：边界设计 + 独立数据访问 + HTTP 通信 + 本地编排）

## 一、服务拆分设计

设计文档见 `docs/microservices-design.md`，要点：

- 单体 `monolith/` 保留可运行（不覆盖、不改名），拆分出的服务位于仓库根目录 `services/`，每个服务都有独立 `pom.xml` 和 `src/`；
- 拆分映射：`Member` → **member-service**；`MembershipCard` → **card-service**；新增入场核验场景 → **checkin-service**（共 3 个微服务）；
- 每个服务独享数据库与账号（`gym_member`/`member_u`、`gym_card`/`card_u`、`gym_checkin`/`checkin_u`），跨服务关联只保存对方 ID（`member_id`/`card_id` 普通列 + 索引），**不建跨库外键、不直接调用对方 Repository**；
- 调用链：`checkin-service → card-service → member-service`（均为 RestTemplate 真实 HTTP 调用）；
- 文档含 Mermaid 架构图与"远端成功、本地失败"窗口分析。

## 二、代码拆分

```
services/
├── member-service/    # :8081  会员档案（登记/查询），独享 gym_member
├── card-service/      # :8082  会员卡（办卡/查卡），独享 gym_card
└── checkin-service/   # :8083  入场核验，独享 gym_checkin
```

每个服务包含：独立 `pom.xml`、独立 `application.yml`、≥1 个 controller、≥1 个 service、健康接口（`/actuator/health`）、≥1 个测试。

**跨服务调用实现**（作业要求的核心点）：

- card-service 办卡前用 `RestTemplate` 调用 `GET /api/members/{id}`（`MemberClient`）；checkin-service 入场前调用 `GET /api/cards/{id}`（`CardClient`）；
- 被调服务地址写在 `application.yml`（`member-service.base-url` / `card-service.base-url`），可用环境变量 `MEMBER_SERVICE_BASE_URL` / `CARD_SERVICE_BASE_URL` 覆盖（Compose 中配置为服务名地址）；
- `RestTemplate` 配置连接超时 2s、读取超时 3s（`connect-timeout-ms` / `read-timeout-ms` 可配置）；
- 失败语义：依赖资源不存在 → 404；依赖服务不可达/超时 → **503 并中止请求，不写本地数据**（有单元测试覆盖）。

**测试命令**（在仓库根目录执行，Java 21；本地未装 mvn 时可用 monolith 自带的 Maven 包装器）：

```bash
# 方式一：本地 mvn
mvn -f services/member-service/pom.xml test
mvn -f services/card-service/pom.xml test
mvn -f services/checkin-service/pom.xml test

# 方式二：Maven 包装器（Windows PowerShell，本仓库实际使用的命令）
monolith\mvnw.cmd -f services/member-service/pom.xml test
monolith\mvnw.cmd -f services/card-service/pom.xml test
monolith\mvnw.cmd -f services/checkin-service/pom.xml test
```

实测结果（全部通过）：

| 服务 | 测试数 | 结果 |
| --- | --- | --- |
| member-service | 4（上下文加载 + 登记成功/重复手机号 409/查询 404） | BUILD SUCCESS |
| card-service | 7（上下文加载 + 办卡成功/会员 404/依赖 503/有效期 400/次卡 400/查卡 404） | BUILD SUCCESS |
| checkin-service | 7（上下文加载 + 核验成功/卡 404/依赖 503/非本会员 400/冻结 400/过期 400） | BUILD SUCCESS |

## 三、本地编排

`compose.yaml` 更新为 6 个服务：

| Compose 服务 | 说明 |
| --- | --- |
| `mysql` + `app` | 第 05 周单体版本（保留可运行，gym 库，端口 3307/8080） |
| `mysql-ms` | 微服务专用 MySQL 实例（新数据卷 `gym-ms-mysql-data`，端口 3308；首次启动执行 `docker/mysql-init.sql` 创建 3 库 3 账号并授权隔离） |
| `member-service` | 8081，连 `mysql-ms:3306/gym_member`，账号 `member_u` |
| `card-service` | 8082，连 `gym_card`，账号 `card_u`；`MEMBER_SERVICE_BASE_URL=http://member-service:8081` |
| `checkin-service` | 8083，连 `gym_checkin`，账号 `checkin_u`；`CARD_SERVICE_BASE_URL=http://card-service:8082` |

要点：容器间一律通过 Compose 服务名通信（`mysql-ms`、`member-service`、`card-service`），**不使用 localhost**；数据卷保留数据；账号权限用 GRANT 限定在各自库，数据库层面强制服务边界。

**一键启动**：

```bash
docker compose up -d --build     # 构建并启动全部服务
docker compose ps                # 6 个容器全部 Up（两个 mysql 均 healthy）
bash scripts/smoke-test.sh       # 冒烟测试
```

## 四、验证

### 1. 冒烟测试（scripts/smoke-test.sh，从仓库根目录运行）

```
[PASS] member-service  (http://localhost:8081/actuator/health)  -> 200  status=UP
[PASS] card-service  (http://localhost:8082/actuator/health)  -> 200  status=UP
[PASS] checkin-service  (http://localhost:8083/actuator/health)  -> 200  status=UP
[INFO] monolith(单体保留版) /actuator/health -> HTTP 200（不计入冒烟结果）
 冒烟测试结果: 通过 3 / 失败 0     退出码 0
```

### 2. 完整跨服务业务流程（登记 → 办卡 → 入场）

| 步骤 | 请求 | 状态码 | 数据变化 |
| --- | --- | --- | --- |
| 1. 登记会员 | POST `:8081/api/members` {"name":"微服务会员","phone":"13700001111",...} | 201 | `gym_member.member` 新增 id=1 |
| 2. 办卡 | POST `:8082/api/cards` {"memberId":1,"cardType":"TIMES",...,"remainingTimes":20} | 201 | card-service 先 HTTP 调 member-service 校验会员存在，`gym_card.membership_card` 新增 id=1（member_id=1） |
| 3. 入场核验 | POST `:8083/api/checkins` {"memberId":1,"cardId":1} | 201 | checkin-service 先 HTTP 调 card-service 验卡（ACTIVE、未过期、属于本人），`gym_checkin.checkin_record` 新增 id=1（allowed=true） |

三个库的数据落库结果均已通过 SQL 查询确认（见截图）。

### 3. 失败场景（明确错误、不写脏数据）

| 场景 | 请求 | 状态码 | 数据变化 |
| --- | --- | --- | --- |
| 为不存在的会员办卡 | POST `:8082/api/cards` {"memberId":99999,...} | 404 | member-service 返回 404，card-service 不写库 |
| 用别人的卡入场 | POST `:8083/api/checkins` {"memberId":99,"cardId":1} | 400 | 卡不属于该会员，不写库 |
| 用不存在的卡入场 | POST `:8083/api/checkins` {"memberId":1,"cardId":88888} | 404 | 不写库 |
| **member-service 停机时办卡** | `docker compose stop member-service` 后 POST `:8082/api/cards` | **503** | card-service 校验中止，`gym_card` 记录数不变（仍为 1） |
| **card-service 停机时入场** | `docker compose stop card-service` 后 POST `:8083/api/checkins` | **503** | checkin-service 校验中止，`gym_checkin` 记录数不变（仍为 1） |

恢复两个服务后复跑 `scripts/smoke-test.sh`，3/3 通过。

### 4. "远端成功、本地失败"窗口

入场核验中：card-service 验卡成功（远端成功）之后、本地 `checkin_record` 落库之前若本地库故障，会出现"卡已核验但无入场记录"的不一致窗口。本周按要求只做顺序流程与基本失败处理（明确报错、不写脏数据），跨服务原子事务/补偿/消息机制留待后续演进（分析见 `docs/microservices-design.md` 第五节）。

## 五、问题记录

1. **环境变量覆盖不生效导致 503**：最初 Compose 中把被调服务地址写成 `MEMBER_SERVICE_BASEURL`，但 Spring 对 `member-service.base-url` 的环境变量映射是 `MEMBER_SERVICE_BASE_URL`（`-` 也转为 `_`），导致 card-service 仍用 localhost 默认值、容器内连不上 member-service 而返回 503。修正环境变量名后恢复。
2. **测试配置覆盖主配置**：`src/test/resources/application.yml` 会覆盖主配置，需要在其中补充跨服务地址占位配置（单元测试用 Mockito 模拟 Client，不真正发 HTTP）。
3. Windows 下 PATH 中的 `bash.exe` 是 WSL 存根（无发行版），运行 smoke-test.sh 需使用 Git Bash：`"C:\Program Files\Git\bin\bash.exe" scripts/smoke-test.sh`。

## 六、本周完成内容汇总

1. ✅ `docs/microservices-design.md`：拆分映射、3 个服务的职责/端口/库表、调用关系、Mermaid 架构图、边界规则、"远端成功本地失败"窗口分析
2. ✅ `services/` 下 3 个独立 Spring Boot 工程（独立 pom.xml/src/、独立配置、controller/service/健康接口/测试齐全，基础包 `com.zjgsu.yz.<服务名>`）
3. ✅ RestTemplate 跨服务 HTTP 调用（地址可配置、超时可配置、失败语义明确）
4. ✅ `compose.yaml` 一键启动 6 个容器（独立数据库 + 独立账号 + 服务名通信 + 数据卷）
5. ✅ `scripts/smoke-test.sh` 从仓库根目录可运行（3/3 通过）
6. ✅ 跨服务流程与失败场景全部实测验证
