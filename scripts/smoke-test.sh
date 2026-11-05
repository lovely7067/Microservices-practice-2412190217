#!/usr/bin/env bash
# ============================================================
# 椰汁健身房微服务 - 冒烟测试脚本（从仓库根目录运行）
# 用法：bash scripts/smoke-test.sh
# 功能（第 07 周）：
#   1. 检查 Nacos 控制台 HTTP 地址（http://localhost:18080，v3 控制台独立端口，
#      不使用旧版 8848/nacos 控制台地址）
#   2. 检查 Nacos 服务注册列表（三个业务服务是否已注册，读取 .env 中的分组与凭据）
#   3. 检查每个服务的健康接口
# 全部通过返回 0，任一检查失败返回 1（错误不会被忽略）
# ============================================================
set -u

# 仓库根目录（脚本位于 scripts/ 下，向上取一层）
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# 读取 .env（只取 Nacos 相关变量，不打印密码/Token）
if [ -f .env ]; then
  NACOS_GROUP_VALUE=$(grep -E '^NACOS_GROUP=' .env | cut -d= -f2)
  NACOS_USERNAME_VALUE=$(grep -E '^NACOS_USERNAME=' .env | cut -d= -f2)
  NACOS_PASSWORD_VALUE=$(grep -E '^NACOS_PASSWORD=' .env | cut -d= -f2)
fi
NACOS_GROUP_VALUE=${NACOS_GROUP_VALUE:-GYM_YZ_GROUP}
NACOS_USERNAME_VALUE=${NACOS_USERNAME_VALUE:-nacos}
NACOS_CONSOLE_URL="http://localhost:18080"
NACOS_SERVER_URL="http://localhost:8848"

# 服务清单：名称 -> 宿主机端口（与根目录 compose.yaml 的端口映射一致）
declare -A SERVICES=(
  ["member-service"]=8081
  ["card-service"]=8082
  ["checkin-service"]=8083
)

echo "=========================================="
echo " 椰汁健身房微服务冒烟测试（含 Nacos 注册检查）"
echo "=========================================="

PASS=0
FAIL=0

# ---------- 检查 1：Nacos 控制台可访问（v3 控制台端口 18080） ----------
HTTP_CODE=$(curl -s -o /tmp/smoke_nacos_console.json -w "%{http_code}" --max-time 5 "${NACOS_CONSOLE_URL}/v3/console/health/readiness" 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
  echo "[PASS] Nacos 控制台  (${NACOS_CONSOLE_URL})  -> 200  就绪"
  PASS=$((PASS + 1))
else
  echo "[FAIL] Nacos 控制台  (${NACOS_CONSOLE_URL})  -> HTTP ${HTTP_CODE:-无响应}"
  FAIL=$((FAIL + 1))
fi

# ---------- 检查 2：Nacos 注册列表（三个业务服务均应已注册） ----------
TOKEN=$(curl -s -X POST "${NACOS_SERVER_URL}/nacos/v3/auth/user/login" 2>/dev/null \
  -d "username=${NACOS_USERNAME_VALUE}&password=${NACOS_PASSWORD_VALUE}" \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "[FAIL] Nacos 登录失败，无法检查注册列表（请确认 .env 中的 NACOS_USERNAME/NACOS_PASSWORD）"
  FAIL=$((FAIL + 1))
else
  for NAME in member-service card-service checkin-service; do
    BODY=$(curl -s --max-time 5 \
      "${NACOS_SERVER_URL}/nacos/v3/admin/ns/instance/list?serviceName=${NAME}&groupName=${NACOS_GROUP_VALUE}&accessToken=${TOKEN}" 2>/dev/null)
    IP=$(echo "$BODY" | sed -n 's/.*"ip":"\([^"]*\)".*/\1/p')
    PORT=$(echo "$BODY" | sed -n 's/.*"port":\([0-9]*\).*/\1/p')
    if echo "$BODY" | grep -q '"healthy":true'; then
      echo "[PASS] Nacos 注册  $NAME  分组=${NACOS_GROUP_VALUE}  实例=${IP}:${PORT}  healthy=true"
      PASS=$((PASS + 1))
    else
      echo "[FAIL] Nacos 注册  $NAME  分组=${NACOS_GROUP_VALUE}  未发现健康实例"
      FAIL=$((FAIL + 1))
    fi
  done
fi

# ---------- 检查 3：各服务健康接口 ----------
for NAME in member-service card-service checkin-service; do
  PORT=${SERVICES[$NAME]}
  URL="http://localhost:${PORT}/actuator/health"
  HTTP_CODE=$(curl -s -o /tmp/smoke_${NAME}.json -w "%{http_code}" --max-time 5 "$URL" 2>/dev/null)
  BODY=$(cat /tmp/smoke_${NAME}.json 2>/dev/null)
  if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q '"status":"UP"'; then
    echo "[PASS] $NAME  ($URL)  -> 200  status=UP"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $NAME  ($URL)  -> HTTP ${HTTP_CODE:-无响应}  ${BODY}"
    FAIL=$((FAIL + 1))
  fi
done

# 附加检查：单体版本（保留运行，但不计入微服务冒烟结果）
MONO_CODE=$(curl -s -o /tmp/smoke_monolith.json -w "%{http_code}" --max-time 5 "http://localhost:8080/actuator/health" 2>/dev/null)
echo "[INFO] monolith(单体保留版) /actuator/health -> HTTP ${MONO_CODE:-无响应}（不计入冒烟结果）"

rm -f /tmp/smoke_*.json

echo "------------------------------------------"
echo " 冒烟测试结果: 通过 ${PASS} / 失败 ${FAIL}"
echo "------------------------------------------"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
