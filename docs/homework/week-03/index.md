# 第三周作业记录（阶段一：单体工程搭建）

## 1. 本周计划

本周对应实验指导书"阶段一/第 03 周"，目标是将项目从规划推进到可运行的单体工程，具体计划如下：

1. **创建 Spring Boot 工程**：在仓库根目录 `monolith/` 文件夹下创建可运行的 Maven 工程（包名 `com.zjgsu.yz`）。
2. **完成基础配置**：以 YAML 格式编写 `application.yml`，配置应用名称、服务端口及 Actuator 健康检查端点。
3. **实现验证接口**：编写一个 GET 接口用于验证服务运行状态。
4. **启动测试**：确保 `mvnw test` 全部通过。
5. **文档完善**：提交 `docs/project-proposal.md`（项目名称、目标用户、优先场景、两个核心模型），并完善根目录 `README.md`。

## 2. 项目立项文档

已提交 `docs/project-proposal.md`，内容包括：

- **项目名称**：椰汁健身房会员管理系统
- **目标用户**：健身房前台/店员、会员、店长/管理员三类角色
- **优先实现场景**：① 会员登记与办卡；② 会员卡到期提醒与续费；③ 课程预约（后续迭代）
- **两个核心模型**：`Member`（会员）与 `MembershipCard`（会员卡），两者为 1 — N 关系

## 3. 工程与配置说明

- **环境**：Java 21（LTS）+ Spring Boot 4.0.8 + Maven（`mvnw` 包装器）
- **包名**：`com.zjgsu.yz`
- **工程位置**：`monolith/`，依赖为 `spring-boot-starter-webmvc`、`spring-boot-starter-actuator` 及对应 test 依赖
- **配置文件**：`monolith/src/main/resources/application.yml`（YAML 格式，配置端口 8080、应用名 `gym`、暴露 `health`/`info` 端点）

> 注：本周曾尝试 Java 25 配合 Spring Boot 4.0.x，`maven-compiler-plugin` 编译时报"不支持发行版本 25"，已降级为 Java 21 LTS 解决。

## 4. 启动命令与接口验证

启动命令（在 `monolith/` 目录下）：

```
.\mvnw.cmd spring-boot:run
```

验证接口（浏览器访问）：

**GET http://localhost:8080/api/ping**

```json
{"app":"gym","time":"2026-09-28T20:28:41.849907500","status":"UP"}
```

**GET http://localhost:8080/actuator/health**

```json
{"status":"UP"}
```

截图见 `screenshots/` 目录。

## 5. 启动测试（Task 3）

测试命令（在 `monolith/` 目录下）：

```
.\mvnw.cmd test
```

测试结果：**BUILD SUCCESS**，共 2 个测试全部通过：

- `GymApplicationTests.contextLoads()`：验证 Spring 上下文（含自定义 Controller）能正常加载
- `PingControllerTest.pingShouldReturnUp()`：验证 `GET /api/ping` 返回 HTTP 200 且响应包含 `"status":"UP"`

```
[INFO] Results:
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

截图见 `screenshots/` 目录。

## 6. 本周完成内容汇总

1. ✅ 在 `monolith/` 下创建 Spring Boot 4.0.8 单体工程（Java 21，包名 `com.zjgsu.yz`）
2. ✅ 完成 `application.yml` 基础配置（YAML 格式）
3. ✅ 实现 GET 验证接口 `/api/ping`，实测返回 `{"status":"UP",...}`
4. ✅ `mvnw test` 全部通过（2 个测试，0 失败）
5. ✅ 提交 `docs/project-proposal.md` 立项文档
6. ✅ 完善 `README.md`：补充 Java/Maven 环境要求、启动和测试命令、接口地址、项目结构及当前尚未实现的业务能力

## 7. 问题记录

1. **Java 版本兼容问题**：最初使用 Java 25，Maven 编译时报 `不支持发行版本 25`，排查后将 JDK、IDEA（Project Structure / Maven Runner）全部统一为 Microsoft OpenJDK 21，并移除 `pom.xml` 中显式的 `<source>/<target>` 配置，改由 `<java.version>21</java.version>` 统一控制，问题解决。
2. **Spring Boot 4 测试 API 变更**：`TestRestTemplate` 在 Spring Boot 4 中已移除，接口测试改为使用 `MockMvc`（standalone 模式）实现。
