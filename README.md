# Microservices-practice-2412190217

## **微服务开发与实践**

俞哲 2412190217 计科2402
本仓库用于后续提交作业

# 椰汁健身房会员管理系统

## 项目简介

椰汁健身房会员管理系统是一个面向现代健身房的综合性会员管理与服务预约平台。项目致力于为健身房提供一套从会员入会、课程购买、预约签到到教练管理的全流程数字化解决方案。

## 业务背景

随着全民健身意识的提升，传统健身房依靠纸质登记或 Excel 表格管理会员、排课和签到的方式已无法满足高效运营的需求。特别是在高峰期，会员排队签到、课程约满冲突、教练排班混乱等问题频发。因此，构建一个支持多角色协同、数据实时同步的健身房管理系统具有强烈的现实业务价值。

## 目标用户及需求

1. **健身房会员**：需要在线查看课程表、预约私教/团课、扫码签到、查看剩余次数/会籍有效期、购买续费。
2. **私人教练/团课教练**：需要查看个人排班、管理所授课程、确认会员预约、记录会员体测数据。
3. **场馆管理员**：需要管理会员信息、教练排班、课程发布、财务流水统计、器材/场地状态维护。

## 功能设计（初步边界）

- **用户中心**：会员/教练/管理员的注册、登录、个人信息管理。
- **课程与排班**：课程发布、教练排班、课程容量控制。
- **预约与签到**：会员在线预约课程、取消预约、线下扫码/手动签到。
- **会籍与订单**：会员卡类型管理、续费充值、订单记录。
- **数据统计**：管理员查看每日签到率、课程满员率、营收报表。

## 后续演进方向

本项目将作为本学期持续演进的唯一项目，计划按以下阶段迭代：

1. **阶段一（当前）**：需求分析与功能规划，完成 README 和项目骨架搭建。
2. **阶段二**：数据库设计与持久化，实现核心业务实体的 CRUD。
3. **阶段三**：服务拆分与微服务架构改造，引入认证授权。
4. **阶段四**：引入消息队列处理预约通知，使用分布式事务保证扣费与预约的一致性。
5. **阶段五**：完善监控、日志与容器化部署（Docker）。


## 项目目标和适用场景

**项目目标**：构建一套面向现代健身房的综合性数字化管理平台，解决传统健身房在会员管理、课程预约、签到及财务统计中依赖人工、效率低下的痛点，提升场馆运营效率和会员健身体验。
**适用场景**：适用于中小型单体健身房、连锁健身房门店，覆盖从会员入会、购买会籍、预约课程、现场签到到教练排班、管理员数据统计的全流程业务。

## 主要用户角色及需求


| 用户角色   | 核心需求                                                                                |
| :--------- | :-------------------------------------------------------------------------------------- |
| **会员**   | 在线查看课程表、预约/取消私教或团课、扫码签到、查看会籍有效期与剩余次数、在线续费充值。 |
| **教练**   | 查看个人排班与授课表、确认会员预约、记录会员体测数据、查看课时统计。                    |
| **管理员** | 管理会员与教练信息、发布课程与排班、查看营收流水与签到率报表、维护场馆器材/场地状态。   |

## 功能模块划分及具体功能


| 模块名称       | 具体功能                                                                 |
| :------------- | :----------------------------------------------------------------------- |
| **用户中心**   | 会员/教练/管理员的注册、登录、个人信息维护、权限控制。                   |
| **课程与排班** | 课程发布、教练排班、课程容量设置、课程状态管理（未开始/进行中/已结束）。 |
| **预约与签到** | 会员在线预约、取消预约、名额校验、扫码签到、手动签到。                   |
| **会籍与订单** | 会员卡类型管理、续费充值、订单生成、支付状态流转、退款处理。             |
| **数据统计**   | 每日签到率、课程满员率、营收报表、教练课时统计。                         |

## 核心业务流程（会员预约课程）

以下为系统中最典型的核心业务流程，后续可基于此流程引入分布式锁、消息通知和分布式事务。

```mermaid
sequenceDiagram
    participant M as 会员
    participant S as 系统
    participant C as 教练
    participant A as 管理员

    M->>S: 浏览课程表并选择课程
    S->>S: 校验会籍有效期与课程剩余名额
    alt 校验通过
        S->>S: 锁定名额并生成预约记录
        S->>M: 发送预约成功通知
        S->>C: 发送新学员预约通知
        M->>S: 到达场馆扫码签到
        S->>S: 核销预约并记录签到时间
        S->>A: 更新签到率与营收报表
    else 校验失败（名额满/会籍过期）
        S->>M: 返回失败原因并提示
    end
```


## 当前项目范围和暂不实现的内容

* **当前项目范围（本学期内）**：完成需求分析与功能规划、数据库设计与持久化、核心业务模块的 CRUD 实现、基础认证授权。
* **暂不实现的内容**：暂不涉及真实支付渠道对接（使用模拟支付）、暂不涉及硬件门禁/闸机对接、暂不涉及多租户 SaaS 化改造。

## 后续可扩展的演进方向


| 演进方向     | 具体内容                                                         |
| ------------ | ---------------------------------------------------------------- |
| **认证**     | 引入 JWT + Spring Security，实现多角色细粒度权限控制。           |
| **服务拆分** | 将用户、课程、订单、签到等模块拆分为独立微服务。                 |
| **消息**     | 引入 RocketMQ/RabbitMQ，实现预约通知、签到提醒、营销推送。       |
| **事务**     | 引入 Seata 或本地消息表，保证“扣费 + 预约”的分布式事务一致性。 |
| **监控**     | 引入 Prometheus + Grafana，监控服务健康度与业务指标。            |
| **部署**     | 使用 Docker + Kubernetes 实现容器化编排与自动化部署。            |

## 项目结构与运行方式

```
仓库根目录/
├── monolith/          # Spring Boot 单体应用（Maven 工程）
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/           # 源码与测试代码（包名 com.zjgsu.yz，分层：model/repository/service/web/dto/exception）
├── docs/
│   ├── homework/      # 历次作业记录
│   ├── database.md    # 数据库设计文档
│   └── project-proposal.md   # 项目立项说明
└── README.md
```

**技术栈**：Java 21（LTS）+ Spring Boot 4.0.x + Spring Data JPA + MySQL（测试用 H2）+ Maven（`mvnw` 包装器）

## 本地 MySQL 安装与建库

1. **安装 MySQL 8.x**：从 https://dev.mysql.com/downloads/installer/ 下载 MySQL Installer 安装（或使用 phpStudy/XAMPP 自带的 MySQL），安装时设置 root 密码。
2. **建库**：用命令行或 Navicat 执行：
   ```sql
   CREATE DATABASE gym DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. **连接配置**：修改 `monolith/src/main/resources/application.yml` 中的 `spring.datasource.username` / `password` 为本机 MySQL 的实际账号密码（默认 `root/root`，端口 3306）。
4. 表结构由 JPA 自动创建（`ddl-auto: update`），无需手工执行建表脚本；设计说明见 `docs/database.md`。
5. 运行测试不需要 MySQL（自动切换 H2 内存数据库）。

## 启动与测试

```bash
cd monolith

# 运行测试（H2 内存库，可重复执行）
./mvnw test                # Windows 下使用 .\mvnw.cmd test

# 启动应用（需本地 MySQL，默认端口 8080）
./mvnw spring-boot:run     # Windows 下使用 .\mvnw.cmd spring-boot:run
```

## 验证接口

- `GET http://localhost:8080/api/ping` → `{"status":"UP","app":"gym","time":"..."}`
- `GET http://localhost:8080/actuator/health` → `{"status":"UP",...}`

### curl 示例（核心业务：会员登记与办卡）

**1. 登记会员（创建，返回 201）**

```bash
curl -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"张三","phone":"13800001111","gender":"男","birthday":"1995-06-01"}'
```

**2. 查询全部会员**

```bash
curl http://localhost:8080/api/members
```

**3. 为会员办卡（创建会员卡，返回 201）**

```bash
curl -X POST http://localhost:8080/api/members/1/cards \
  -H "Content-Type: application/json" \
  -d '{"cardType":"TIMES","startDate":"2026-10-18","endDate":"2027-10-18","remainingTimes":30}'
```

**4. 查询某会员名下的会员卡**

```bash
curl http://localhost:8080/api/members/1/cards
```

> Windows PowerShell 下如遇引号转义问题，建议使用 Git Bash 执行上述 curl 命令。
>
> 错误响应：重复手机号 → 409；卡结束日期早于/等于开始日期或次卡剩余次数 ≤ 0 → 400；为不存在的会员办卡 → 404。

## 微服务架构（第 06 周）

单体系统已拆分为 3 个微服务（设计文档：`docs/microservices-design.md`），单体版本保留在 `monolith/` 可运行：

| 服务 | 端口 | 职责 | 独立数据库 | 基础包 |
| --- | --- | --- | --- | --- |
| `services/member-service` | 8081 | 会员档案（登记/查询） | `gym_member`（账号 member_u） | `com.zjgsu.yz.member` |
| `services/card-service` | 8082 | 会员卡（办卡前 HTTP 调 member-service 校验） | `gym_card`（账号 card_u） | `com.zjgsu.yz.card` |
| `services/checkin-service` | 8083 | 入场核验（验卡前 HTTP 调 card-service） | `gym_checkin`（账号 checkin_u） | `com.zjgsu.yz.checkin` |

**一键启动**（根目录 compose.yaml，共 6 个容器：单体 2 个 + 微服务 MySQL 实例 + 3 个微服务）：

```bash
docker compose up -d --build
bash scripts/smoke-test.sh      # 调用每个服务的健康接口（Git Bash）
```

**运行单个服务的测试**（Java 21）：

```bash
mvn -f services/member-service/pom.xml test    # 本地安装了 mvn 时
monolith\mvnw.cmd -f services/member-service/pom.xml test    # 或用包装器（Windows）
monolith\mvnw.cmd -f services/card-service/pom.xml test
monolith\mvnw.cmd -f services/checkin-service/pom.xml test
```

**跨服务调用**：card-service 办卡前调用 member-service；checkin-service 入场前调用 card-service。第 07 周起改为 **Nacos 服务发现**：每次调用前通过 `DiscoveryClient.getInstances("服务名")` 获取当前实例（不在启动时读取一次并永久保存地址），实例列表为空或调用失败返回 503 且不写本地数据；保留 HTTP 连接超时 2s、读取超时 3s。仅取第一个实例，不实现轮询、权重路由、自动重试或熔断。

## Nacos 服务注册与发现（第 07 周）

三个业务服务通过 `spring-cloud-starter-alibaba-nacos-discovery`（Spring Cloud Alibaba **2025.1.0.0** BOM，适配 Spring Boot 4.0.x）注册到 Nacos：

| 配置项 | 值 | 说明 |
| --- | --- | --- |
| 服务名 | `member-service` / `card-service` / `checkin-service` | 与 `docs/microservices-design.md` 一致，同一服务多实例同名 |
| 分组 | `GYM_YZ_GROUP` | 个人项目独立分组，与课堂示例隔离 |
| 控制台 | http://localhost:18080 | v3 控制台独立端口（容器内 8080） |
| 服务端 API | 8848 | 宿主机映射，SDK gRPC 走 9848 |
| 命名空间 | 默认（public） | 首次部署使用默认命名空间 |

**启动步骤**（真实密钥/密码只放本地 `.env`，提交的 `.env.example` 仅为占位模板）：

```bash
# 1. 准备环境变量（.env 中需包含 NACOS_AUTH_TOKEN / NACOS_AUTH_IDENTITY_KEY /
#    NACOS_AUTH_IDENTITY_VALUE / NACOS_PASSWORD / NACOS_GROUP）
cp .env.example .env    # 然后填入自己的值

# 2. 先独立启动 Nacos 并初始化管理员（首次执行一次）
docker compose up -d nacos
# 等控制台就绪后初始化管理员密码（返回 username/password 即成功）：
curl -X POST "http://localhost:8848/nacos/v3/auth/user/admin" -d "password=你的密码"

# 3. 启动业务服务与数据库
docker compose up -d --build

# 4. 冒烟测试（Nacos 控制台就绪 + 注册列表 + 三服务健康，任一失败退出非零）
bash scripts/smoke-test.sh
```

**控制台查看**：浏览器打开 http://localhost:18080 ，用 `.env` 中的 `NACOS_USERNAME`/`NACOS_PASSWORD` 登录，左侧「服务管理 → 服务列表」选分组 `GYM_YZ_GROUP`，可看到 3 个服务及其实例 IP 与端口。

**运行单个服务的测试**（不依赖真实 Nacos 或 MySQL，测试配置已关闭自动注册，沿用 H2 与模拟远程响应）：

```bash
monolith\mvnw.cmd -f services/member-service/pom.xml test    # Windows 包装器
monolith\mvnw.cmd -f services/card-service/pom.xml test
monolith\mvnw.cmd -f services/checkin-service/pom.xml test
```

## 容器化运行（第五周：Docker + Compose）

应用与 MySQL 已容器化，可一键构建镜像并启动整套环境。

**前置条件**：已安装 Docker Desktop 并能拉取镜像（网络受限时无法拉取基础镜像）。

**1. 准备环境变量**

```bash
Copy-Item .env.example .env    # Windows PowerShell；macOS/Linux 用 cp .env.example .env
```

按需修改 `.env`：`MYSQL_ROOT_PASSWORD`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_HOST_PORT`（默认 3307，本机 3306 已被本地 MySQL 占用）、`APP_PORT`（默认 8080）。`.env` 已在 `.gitignore` 中，不会提交。

**2. 一键构建并启动**

```bash
docker compose up -d --build      # 构建镜像并后台启动
docker compose ps                 # 查看状态（mysql 应为 healthy）
docker compose logs -f app        # 查看应用日志
```

启动后应用监听宿主机 `http://localhost:8080`，MySQL 容器映射到宿主机 `3307` 端口。

**3. 常用命令**

```bash
docker compose down               # 停止并删除容器（命名卷保留，数据不丢）
docker compose down -v            # 连同数据卷一起删除（数据清空）
docker volume ls                  # 确认数据卷 gym-mysql-data 是否保留
docker compose up -d              # 再次一键启动
```

**4. 容器内业务验证（与本地运行一致）**

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/ping
curl -X POST http://localhost:8080/api/members -H "Content-Type: application/json" -d '{"name":"容器化会员","phone":"13900007777","gender":"男","birthday":"1996-04-10"}'
curl http://localhost:8080/api/members
curl -X POST http://localhost:8080/api/members/1/cards -H "Content-Type: application/json" -d '{"cardType":"TIMES","startDate":"2026-10-23","endDate":"2027-10-23","remainingTimes":30}'
curl http://localhost:8080/api/members/1/cards
```

**5. 数据持久化验证**

`docker compose down` 删除容器后重新 `docker compose up -d`，之前创建的会员与会员卡数据仍可查询（数据保存在命名卷 `gym-mysql-data` 中）。

## 当前尚未实现的业务能力

- 真实支付渠道对接（当前为模拟收费）
- 硬件门禁/闸机对接
- 多租户 SaaS 化改造
- 会员卡核销扣次与到期状态自动流转（计划后续迭代）
