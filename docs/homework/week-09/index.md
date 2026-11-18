# 第 09 周作业：API Gateway + JWT 认证

> 作业目标：为个人微服务系统加入 API Gateway 和 JWT 认证，实现统一入口、路由转发和受保护接口访问。
> 状态：全部完成。网关与认证代码已提交，`scripts/auth-test.sh` 9 场景 12 断言全部通过（退出码 0），第 08 周 e2e 10/10 无回归。

## 1. API Gateway

**设计**：新建独立工程 services/gateway-service（Spring Cloud Gateway / WebFlux，Spring Cloud 2025.1 与其它服务同一版本线），统一对外端口 8080（单体保留版让出 8080 改用 8090）。路由目标全部使用 Nacos 服务名（lb://），共 5 条：auth、members、cards、checkins、health（/api/health -> member-service /actuator/health，保留健康检查入口）。依赖链：gateway 依赖三服务与 Nacos 全部 healthy 后启动。

**运行命令**：docker compose up -d --build gateway-service（或整组重建 docker compose up -d --build）

**验证结果**：
- GET http://localhost:8080/api/health -> 200（经网关转发到 member-service actuator）
- 网关注册 Nacos：GYM_YZ_GROUP gateway-service 172.19.0.8:8080（见容器日志）
- 截图：screenshots/01-gateway-routes.png（网关路由配置）、02-gateway-health.png（/api/health 200）

**实测截图清单（`screenshots/`）**：

| 文件 | 内容 | 证明点 |
| --- | --- | --- |
| `gateway-01-routes-config.png` | IDEA 中的 `gateway-service/src/main/resources/application.yml` | 5 条路由（auth/members/cards/checkins/health）、目标全部为 `lb://` 服务名不写死 IP、health-route 的 SetPath 健康入口 |
| `gateway-02-health-entry.png` | `docker compose ps` + `curl http://localhost:8080/api/health` | gateway-service 容器映射宿主 8080（统一入口）且 healthy；经网关访问 /api/health 返回 UP（转发到 member-service actuator） |
| `gateway-03-route-forward.png` | `curl -i http://localhost:8080/api/members` + 网关日志 | 经网关访问业务接口返回 401 JSON（请求已穿过网关到达 member 鉴权链路）；日志含 `nacos registry, GYM_YZ_GROUP gateway-service 172.19.0.8:8080 register finished` |

## 2. 认证服务

**设计**：认证功能写在用户相关服务 member-service 中。新增 app_user 表（AppUser 实体：username 唯一、password_hash、role），两种角色 USER/ADMIN。登录接口 POST /api/auth/login：BCrypt（spring-security-crypto，强度 10）比对密码哈希，jjwt 0.12 签发 HS256 JWT（sub=用户名、role、2h 过期）。密码只在启动时由 DataInitializer 经 BCrypt 哈希落库，代码与数据库均无明文；JWT 密钥走环境变量 JWT_SECRET（.env 不入库，.env.example 占位）。

**运行命令**：curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d {"username":"user1","password":"user123"}

**验证结果**：登录成功 200 返回 {token, tokenType=Bearer, expiresIn=7200, role=USER}；密码错误/用户不存在 -> 401（统一文案防枚举）。测试账号见 README 第 09 周小节。
- 截图：screenshots/03-login-token.png（登录返回 Token）

## 3. 网关鉴权

**设计**：网关全局过滤器 JwtAuthFilter（GlobalFilter，order=-100）：白名单（/api/auth/login、/api/health、/actuator/**）放行；无 Token 或验签失败 -> 401 JSON；管理接口 /api/members/statistics 要求 Token 内 role=ADMIN，否则 -> 403。验签通过后剥离客户端伪造的 X-User-Id / X-User-Role / X-User-Name / X-Gateway-Auth 四个头，再从 Token claims 重新注入可信身份头与网关签名头 X-Gateway-Auth。

**防绕过说明**：下游 member-service 的管理接口额外校验 X-Gateway-Auth 头等于 JWT_SECRET（客户端无法预知），绕过网关直连 8081 也返回 403"管理接口必须经 API 网关访问"；生产部署建议下游端口不映射宿主机、只暴露网关 8080（详见 docs/security.md 第 5 节）。

**验证结果**：见 auth-test 场景 5/6——USER 访问 statistics 403；同时伪造 X-User-Role: ADMIN 与 X-Gateway-Auth 头仍 403（伪造头被剥离，身份以 Token 为准）。
- 截图：screenshots/04-unauthorized-401.png（未登录 401）、05-forbidden-403.png（越权 403）

**实测截图清单（`screenshots/`）**：

| 文件 | 内容 | 证明点 |
| --- | --- | --- |
| `gwauth-01-auth-test-output.png` | `bash scripts/auth-test.sh` 完整输出 | 12 项 PASS 一图覆盖：未登录 401、登录 200、带 Token 200、USER 访问管理接口 403（角色校验）、伪造 X-User-Role: ADMIN 仍 403（剥离伪造头）、篡改 Token 401、退出码 0 |
| `gwauth-02-filter-code.png` | IDEA 中的 `JwtAuthFilter.java` | `③ 管理接口的角色校验`（Token 内 role != ADMIN → 403）与 `④ 剥离伪造身份头重注入可信身份`（STRIP_HEADERS.forEach(remove) + header 注入） |
| `gwauth-03-bypass-rejected.png` | 绕过网关直连 8081 的三次 curl | 不带签名头 / 伪造 X-Gateway-Auth（forged-secret / wrong-value）访问管理接口全部 403 Forbidden——下游防绕过实测 |

## 4. 测试脚本

**设计**：scripts/auth-test.sh 从仓库根目录运行，覆盖四个必测场景加扩展：①未登录访问受保护接口应失败（401）②登录获取 Token（200）③携带 Token 访问受保护接口应成功（200）④低权限 Token 访问管理接口应失败（403）；另覆盖健康入口、ADMIN 放行、伪造头、篡改 Token、密码错误共 9 场景 12 断言。脚本设计为可重复运行（每次重新登录取新 Token），输出 [PASS]/[FAIL] 清晰状态，任一失败退出非零。

**运行命令**：bash scripts/auth-test.sh

**验证结果**：auth-test 结果: 通过 12 / 失败 0，退出码 0（连续运行两次结果一致）；第 08 周 scripts/e2e-test.sh 复跑 10/10 无回归。
- 截图：screenshots/06-auth-test-output.png（脚本完整输出）

**实测截图清单（`screenshots/`）**：

| 文件 | 内容 | 证明点 |
| --- | --- | --- |
| `authtest-01-script-design.png` | `Get-Content scripts/auth-test.sh -TotalCount 30 -Encoding UTF8` | 脚本头部注释：四个必测场景（未登录失败/登录取 Token/带 Token 成功/低权限访问管理接口失败）+ 扩展场景 + "可重复运行"设计说明 |
| `authtest-02-run-output.png` | `bash scripts/auth-test.sh` 完整运行输出 | 12 项 [PASS] 逐条列出（401/200/403 全覆盖）、"通过 12 / 失败 0"、退出码 0 |
| `authtest-03-repeatable.png` | 连续两次运行 auth-test.sh | 两次均为"通过 12 / 失败 0"且中间未清数据未重启——证明可重复运行、不依赖历史状态 |

## 单元测试

member-service 12 个（新增 JwtServiceTest 3 + AuthFlowTest 5）；gateway-service 7 个（JwtAuthFilterTest：白名单/401/403/伪造头剥离/可信头注入）。构建命令：

    monolith\mvnw.cmd -f services/member-service/pom.xml test
    monolith\mvnw.cmd -f services/gateway-service/pom.xml test