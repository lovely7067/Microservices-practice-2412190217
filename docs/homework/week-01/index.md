PS C:\Users\Yu> java -version
java version "17.0.10" 2024-01-16 LTS
Java(TM) SE Runtime Environment (build 17.0.10+11-LTS-240)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.10+11-LTS-240, mixed mode, sharing)

PS C:\Users\Yu> mvn -version
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: C:\Program Files\apache-maven-3.9.16-bin\apache-maven-3.9.16
Java version: 17.0.10, vendor: Oracle Corporation, runtime: C:\Program Files\Java\jdk-17
Default locale: zh_CN, platform encoding: GBK
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"

PS C:\Users\Yu> git -version
unknown option: -version
usage: git [-v | --version] [-h | --help] [-C <path>] [-c <name>=<value>]
[--exec-path[=<path>]] [--html-path] [--man-path] [--info-path]
[-p | --paginate | -P | --no-pager] [--no-replace-objects] [--no-lazy-fetch]
[--no-optional-locks] [--no-advice] [--bare] [--git-dir=<path>]
[--work-tree=<path>] [--namespace=<name>] [--config-env=<name>=<envvar>]
<command> [<args>]

PS C:\Users\Yu> docker version
Client:
Version:           29.7.2
API version:       1.55
Go version:        go1.26.5
Git commit:        a7dcaa6
Built:             Wed Aug  5 18:31:33 2026
OS/Arch:           windows/amd64
Context:           desktop-linux
request returned 500 Internal Server Error for API route and version http://%2F%2F.%2Fpipe%2FdockerDesktopLinuxEngine/v1.55/version, check if the server supports the requested API version

PS C:\Users\Yu> docker compose version
Docker Compose version v5.5.1

Q1:
将整体服务拆分成更加细小且功能完善独立的服务。

Q2:
单体架构是一个完整且不能拆分的架构，而微服务架构可进行拆分，才分完后的功能也能实现。

Q3:
先实现单体结构时，思路更加清楚且结构比较完整，不容易丢失忽略功能，有利于后续拆分成微服务。

Q4:
便于在不同环境下重复测试，以免丢失原先数据。