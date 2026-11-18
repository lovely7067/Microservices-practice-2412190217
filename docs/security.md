# 安全设计：API Gateway + JWT 认证（第 09 周）

> 本文记录认证流程、角色权限模型、网关路由表与防绕过措施。
> 配套代码：services/gateway-service/（网关）、services/member-service/（登录与 JWT 签发）。

## 1. 组件与统一入口

| 组件 | 端口 | 职责 |
| --- | --- | --- |
| gateway-service | 8080（宿主机唯一对外入口） | 统一入口、路由转发、JWT 验签、角色校验、剥离伪造身份头 |
| member-service | 8081 / 8085（容器网络） | 登录接口、JWT 签发（用户相关服务的认证职责） |
| card-service / checkin-service | 8082 / 8083（容器网络） | 业务服务，不直接暴露给外部 |

## 2. 网关路由表（目标全部为 Nacos 服务名，不写死 IP）

| 路由 ID | 匹配路径 | 目标 | 鉴权 |
| --- | --- | --- | --- |
| auth-route | /api/auth/** | lb://member-service | 白名单（登录无需 Token） |
| member-route | /api/members/** | lb://member-service | 需登录；/statistics 子路径需 ADMIN |
| card-route | /api/cards/** | lb://card-service | 需登录 |
| checkin-route | /api/checkins/** | lb://checkin-service | 需登录 |
| health-route | /api/health | lb://member-service -> /actuator/health | 白名单（健康检查入口） |

## 3. 认证流程

- 登录：POST /api/auth/login -> member-service 用 BCrypt 比对 app_user 表密码哈希，签发 HS256 JWT（sub=用户名、role=角色、2h 过期，issuer=member-service）。
- 访问：客户端携带 Authorization: Bearer <token>，网关 JwtAuthFilter（GlobalFilter，order=-100）先于路由验签。
- 密钥：环境变量 JWT_SECRET，member 与 gateway 共享同一值；真实值只在本地 .env（已 gitignore），仓库仅提交 .env.example 占位，代码中无硬编码密钥。
- 失败语义：未登录/坏 Token -> 401 JSON；角色不足 -> 403 JSON，消息清晰可读。

## 4. 角色权限模型

| 角色 | 测试账号 | 可访问 |
| --- | --- | --- |
| USER | user1 / user123 | /api/members/**（除 statistics）、/api/cards/**、/api/checkins/** |
| ADMIN | admin / admin123 | 全部接口，含管理接口 GET /api/members/statistics |

- 密码以 BCrypt 哈希存储（spring-security-crypto，强度 10），数据库 password_hash 列与代码中均无明文。
- 测试账号由 DataInitializer 在表空时自动创建（启动时 BCrypt 加密落库），保证 auth-test.sh 可重复运行。
- 登录失败统一 401，不区分"用户名不存在"与"密码错误"，避免用户名枚举。
## 5. 防伪造与防绕过（网关鉴权关键设计）

**① 覆盖客户端伪造的身份请求头**

客户端可自行携带 X-User-Role: ADMIN、X-User-Id: 999 等头冒充身份。网关在验签通过后：

1. 一律剥离请求中的 X-User-Id / X-User-Role / X-User-Name / X-Gateway-Auth 四个头；
2. 再从 Token 的 claims（无法伪造，验签保证）解出用户身份，重新注入 X-User-Id / X-User-Role；
3. 注入网关签名头 X-Gateway-Auth（值 = JWT_SECRET，仅网关持有）。

**② 下游如何避免被绕过网关直连**

以管理接口 GET /api/members/statistics 为例（两级校验）：

- 第一级（网关）：Token 内 role != ADMIN 直接 403，请求不会到达下游；
- 第二级（member-service）：该端点要求请求头 X-Gateway-Auth 等于 JWT_SECRET——客户端不知道这个密钥值，伪造该头无效；不带或带错返回 403"管理接口必须经 API 网关访问"。这保证绕过 8080 直连容器映射端口 8081 时管理接口同样拒绝。

**③ 部署层加固（生产建议）**

当前 Compose 中下游服务的宿主机端口（8081/8085/8082/8083）为周内验证与健康检查而保留；生产部署应只映射网关 8080，下游端口不暴露宿主机，外部流量物理上只能走网关。跨服务调用（card->member、checkin->card）走容器网络内部，不经过网关，由服务间信任（容器网络隔离）保障。

## 6. 复跑验证

    bash scripts/auth-test.sh    # 9 个场景 12 项断言，全部通过退出码 0

单元测试：member-service JwtServiceTest（签发/解析/篡改拒绝）+ AuthFlowTest（登录成功/密码错/防绕过 403/带网关头 200）；gateway-service JwtAuthFilterTest（白名单/401/403/伪造头剥离/可信头注入）。