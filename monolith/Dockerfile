# syntax=docker/dockerfile:1

########## 构建阶段：使用 Maven 镜像编译打包 ##########
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 使用阿里云 Maven 镜像加速依赖下载
COPY .mvn/docker-settings.xml /root/.m2/settings.xml

# 先只复制 pom.xml 下载依赖，利用 Docker 层缓存加速重复构建
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 再复制源码编译打包（跳过测试：测试在宿主机 ./mvnw test 中执行）
COPY src ./src
RUN mvn -B -DskipTests package && mv target/*.jar app.jar

########## 运行阶段：使用 JRE 镜像，仅包含可运行 JAR ##########
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
