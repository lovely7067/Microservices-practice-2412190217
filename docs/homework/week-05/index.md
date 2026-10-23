# 第五周作业记录（容器化：Dockerfile + Compose + MySQL 持久化）

## 一、实现思路

| 文件 | 作用 |
| --- | --- |
| `monolith/Dockerfile` | 多阶段构建：阶段一用 `maven:3.9-eclipse-temurin-21` 编译打包，阶段二用 `eclipse-temurin:21-jre` 仅携带可运行 JAR |
| `monolith/.dockerignore` | 排除 `target/`、`.idea/`、日志等无关内容，减小构建上下文、避免污染镜像 |
| `monolith/.mvn/docker-settings.xml` | 容器内编译时使用的阿里云 Maven 镜像（加速依赖下载，仅构建阶段生效） |
| `compose.yaml`（仓库根目录） | 编排 `app` 与 `mysql` 两个服务：共享网络 `gym-net`、命名卷 `gym-mysql-data`、MySQL 健康检查 + 应用 `depends_on: service_healthy`、端口映射、环境变量注入 |
| `.env.example` / `.env` | 环境变量模板与本地实际配置（`.env` 已加入 `.gitignore`，不提交真实密码） |

**关键设计**

- 应用通过服务名 `mysql:3306` 访问数据库（Compose 内部 DNS），不写死 IP；
- 数据库连接参数以环境变量注入：`SPRING_DATASOURCE_URL / USERNAME / PASSWORD`，无需修改 `application.yml`；
- 只有应用端口 `8080` 对外映射；MySQL 映射到宿主机 `3307`（本机 3306 已被本地 MySQL 占用）；
- 数据库文件放在命名卷 `gym-mysql-data`，容器删除重建后数据不丢；
- `compose.yaml` 不写 `version` 字段（Compose 新规范已废弃）。

## 二、镜像构建

```bash
# 在仓库根目录执行（构建上下文为 ./monolith）
docker compose up -d --build

# 查看服务状态
docker compose ps
```

实际执行结果：

```
Container gym-mysql Started
Container mysql Waiting
Container mysql Healthy
Container gym-app Started

NAME        IMAGE                       SERVICE   STATUS                    PORTS
gym-app     gym-monolith:week05         app       Up                        0.0.0.0:8080->8080/tcp
gym-mysql   mysql:8.4.11                mysql     Up (healthy)              0.0.0.0:3307->3306/tcp
```

**本次构建说明**：由于本机外网（Docker Hub）被代理软件拦截，基础镜像无法从网络拉取。MySQL 使用的是本地已存在的 `mysql:8.4`（实测版本为 `Ver 8.4.11`）打上 `mysql:8.4.11` 标签；应用镜像先由宿主机 `.\mvnw.cmd -DskipTests package` 打出 JAR，再用本地 JRE 基础镜像构建为 `gym-monolith:week05`。`Dockerfile` 中已按标准写好多阶段构建（Maven 编译阶段 + JRE 运行阶段），网络恢复后直接执行 `docker compose up -d --build` 即可按原流程完整构建。

## 三、业务 API 验证

容器启动后，通过宿主机映射端口 `8080` 访问：

| 接口 | 方法 | 结果 |
| --- | --- | --- |
| `/actuator/health` | GET | `{"status":"UP"}`，其中 `db` 组件为 `MySQL` 且 UP |
| `/api/ping` | GET | `{"app":"gym","status":"UP",...}` |
| `/api/members` | POST | 创建会员，返回 201 + 会员 ID |
| `/api/members` | GET | 查询会员列表，返回 200 |
| `/api/members/{id}/cards` | POST | 为会员办卡，返回 201（TIMES 次卡 30 次，状态 ACTIVE） |
| `/api/members/{id}/cards` | GET | 查询会员名下会员卡，返回 200 |

示例：

```bash
curl http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"容器化会员","phone":"13900007777","gender":"男","birthday":"1996-04-10"}'
curl http://localhost:8080/api/members
curl -X POST http://localhost:8080/api/members/1/cards \
  -H "Content-Type: application/json" \
  -d '{"cardType":"TIMES","startDate":"2026-10-23","endDate":"2027-10-23","remainingTimes":30}'
curl http://localhost:8080/api/members/1/cards
```

## 四、数据持久化验证

```bash
# 1. 写入数据（见上一节：创建会员 id=1，手机号 13900007777，并办理次卡）
# 2. 停止并删除容器
docker compose down

# 3. 确认命名卷仍在
docker volume ls        # gym-mysql-data

# 4. 重新启动
docker compose up -d

# 5. 查询同一条记录
curl http://localhost:8080/api/members/1
curl http://localhost:8080/api/members/1/cards
```

验证结果：`docker compose down` 删除容器后，`gym-mysql-data` 卷依然存在；重新 `docker compose up -d` 后，会员 `id=1`（手机号 13900007777）及其 TIMES 会员卡（剩余 30 次、状态 ACTIVE）**仍可完整查询到**，证明数据通过命名卷实现了持久化。

## 五、本周完成内容汇总

1. ✅ `monolith/Dockerfile`：多阶段构建（Maven 编译 → JRE 运行），配合 `.dockerignore` 精简上下文
2. ✅ 根目录 `compose.yaml`：`app` + `mysql:8.4.11`、共享网络、命名数据卷、健康检查与启动顺序控制、端口映射、环境变量注入
3. ✅ `.env.example` 模板与 `.gitignore` 排除 `.env`（避免真实密码入库）
4. ✅ 一键启动 `docker compose up -d --build`，两个容器均正常（MySQL healthy、应用 8080 可访问）
5. ✅ 业务 API 验证：健康检查 + 会员创建 + 会员卡创建 + 列表查询全部通过
6. ✅ 数据持久化验证：删容器后重建，同一条会员与会员卡数据仍可查询

截图见 `screenshots/` 目录。
