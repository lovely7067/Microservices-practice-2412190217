# 第四周作业记录（核心业务流程：模型、Service、REST API 与数据库持久化）

## 1. 业务建模

### 实现思路

本周在 `docs/project-proposal.md` 中更新了业务建模部分，确定实现的核心业务流程为**"会员登记与办卡"**：

- **流程**：前台录入会员信息 → 校验手机号唯一 → 创建 Member 档案 → 为会员办理会员卡（校验有效期与次卡次数）→ 创建 MembershipCard → 查询会员及其名下卡片。
- **实体与关系**：`Member`（会员，1）— `MembershipCard`（会员卡，N），外键 `member_id`；一名会员可持有多张卡，每张卡必须归属唯一会员。
- **业务规则**（在 Service 中检查）：
  1. 手机号唯一（重复登记 → 409）
  2. 卡结束日期必须晚于开始日期（否则 → 400）
  3. 次卡必须指定大于 0 的剩余次数（否则 → 400）
  4. 为不存在的会员办卡 → 404

## 2. 单体应用实现

### 实现思路

- **model 层**（`com.zjgsu.yz.gym.model`）：`Member`、`MembershipCard` 两个 JPA 实体，以及 `CardType`、`CardStatus` 枚举。
- **service 层**（`com.zjgsu.yz.gym.service`）：`MemberService` 承载全部业务规则与事务，构造器注入两个 Repository。
- **web 层**（`com.zjgsu.yz.gym.web`）：`MemberController` 只负责 HTTP 输入输出（参数校验用 `@Valid` + DTO），不编写任何数据访问逻辑；输入输出使用 `web/dto` 包下的 `CreateMemberRequest`、`CreateCardRequest`、`MemberResponse`、`CardResponse`，不直接暴露实体。
- **异常**（`com.zjgsu.yz.gym.exception`）：`NotFoundException`（404）、`DuplicatePhoneException`（409）、`InvalidRequestException`（400），通过 `@ResponseStatus` 映射状态码。

### REST 接口清单（前缀均为 /api/...）

| 方法 | 路径 | 说明 | 成功状态码 |
| --- | --- | --- | --- |
| POST | `/api/members` | 会员登记（创建） | 201 |
| GET | `/api/members` | 查询全部会员 | 200 |
| GET | `/api/members/{id}` | 按 ID 查询会员 | 200 |
| POST | `/api/members/{id}/cards` | 为会员办卡（创建） | 201 |
| GET | `/api/members/{id}/cards` | 查询会员名下卡片 | 200 |

### 运行命令与结果

在 `monolith/` 目录执行 `.\mvnw.cmd spring-boot:run` 启动应用后：

```
POST /api/members {"name":"张三","phone":"13800001111","gender":"男","birthday":"1995-06-01"}  → 201
GET  /api/members/{id}   → 200，返回会员档案
POST /api/members/{id}/cards {"cardType":"TIMES",...,"remainingTimes":30}  → 201
GET  /api/members/{id}/cards → 200，返回卡片列表
重复手机号登记 → 409；非法有效期/次卡次数 → 400；会员不存在 → 404
```

## 3. 数据库持久化

### 实现思路

- 数据访问代码放入 `repository` 包：`MemberRepository`（含 `existsByPhone`、`findByPhone`）、`MembershipCardRepository`（含 `findByMemberId`），均继承 `JpaRepository`。
- 运行环境接入本地 MySQL 8.x（`spring.datasource` 配置见 `application.yml`），`ddl-auto: update` 由 Hibernate 自动建表；建库语句与配置说明写在根目录 `README.md`。
- 表设计文档见根目录 `docs/database.md`：两张表的字段、主键、唯一约束（`member.phone`）、外键约束（`membership_card.member_id`）及应用层业务规则。
- 测试数据可重复准备：`src/test/resources/data.sql` 种子数据 + 测试代码 `@AfterEach` 清理，H2 内存库每次构建自动初始化。

### 运行命令与结果

```
.\mvnw.cmd test    → 测试环境自动切换 H2，自动建表并执行 data.sql，13 个测试全部通过
```

## 4. 测试与验证

### 实现思路

| 测试类 | 验证内容 |
| --- | --- |
| `MemberRepositoryTest` | Repository 读写：保存后按 ID 查询、`existsByPhone`、按会员查询卡片 |
| `MemberServiceTest` | Service 业务规则：重复手机号拒绝、有效期校验、次卡次数校验、正常办卡状态为 ACTIVE |
| `GymApiIntegrationTest` | API 正常与错误响应：201/200 正常流、409（重复手机号）、404（会员不存在）、400（非法有效期/次卡无次数） |
| `GymApplicationTests` / `PingControllerTest` | 第 03 周的上下文加载与健康检查回归测试 |

测试使用 H2 内存数据库（`src/test/resources/application.yml`），不依赖本地 MySQL，数据每次自动准备并清理，可重复运行。

### 运行命令与结果

```
cd monolith
.\mvnw.cmd test

[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

截图见 `screenshots/` 目录。

## 5. 本周完成内容汇总

1. ✅ 更新 `docs/project-proposal.md`：核心业务流程、实体关系、业务规则
2. ✅ 新增 `docs/database.md`：两张业务表的结构、约束与 ER 关系
3. ✅ model / repository / service / web 分层实现，5 个 REST 接口（`/api/...` 前缀）
4. ✅ 3 条业务规则在 Service 中实现并有对应测试
5. ✅ MySQL 持久化（JPA Repository）+ DTO 承载 API 输入 + data.sql 测试数据
6. ✅ 13 个测试全部通过（`.\mvnw test` 可重复执行）
7. ✅ README 补充 MySQL 安装建库说明、启动命令与 4 个 curl 示例
