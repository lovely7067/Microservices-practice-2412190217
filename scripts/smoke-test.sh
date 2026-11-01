#!/usr/bin/env bash
# ============================================================
# 椰汁健身房微服务 - 冒烟测试脚本（从仓库根目录运行）
# 用法：bash scripts/smoke-test.sh
# 功能：调用每个服务的健康接口，全部通过返回 0，任一失败返回 1
# ============================================================
set -u

# 仓库根目录（脚本位于 scripts/ 下，向上取一层）
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# 服务清单：名称 -> 宿主机端口（与根目录 compose.yaml 的端口映射一致）
declare -A SERVICES=(
  ["member-service"]=8081
  ["card-service"]=8082
  ["checkin-service"]=8083
)

echo "=========================================="
echo " 椰汁健身房微服务冒烟测试"
echo "=========================================="

PASS=0
FAIL=0

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
