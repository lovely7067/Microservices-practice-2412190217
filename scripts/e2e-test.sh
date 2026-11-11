#!/usr/bin/env bash
# ============================================================
# 椰汁健身房微服务 - 端到端验证脚本（第 08 周：OpenFeign + 负载均衡）
# 用法：必须能从仓库根目录运行 -> bash scripts/e2e-test.sh
# 检查项：
#   1. Nacos 控制台就绪（http://localhost:18080）
#   2. member-service 在 Nacos（分组 GYM_YZ_GROUP）注册 >= 2 个 healthy 实例
#   3. 全部服务健康接口 UP（member 8081/8085、card 8082、checkin 8083）
#   4. 负载均衡：连续 6 次经 card-service 办卡（OpenFeign 按服务名调用 member-service），
#      从两个 member 实例容器的 GYM-INSTANCE 请求日志统计命中次数，
#      两个实例都被实际访问才通过（仅看注册列表不算完成分发验证）
#   5. 跨服务全链路：办卡（card 调 member 校验）201 -> 入场（checkin 调 card 验卡）201
# 任一检查失败退出码为 1；脚本不打印密码与 Token。
# ============================================================
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

NACOS_GROUP_VALUE=$(grep -E '^NACOS_GROUP=' .env | cut -d= -f2)
NACOS_USERNAME_VALUE=$(grep -E '^NACOS_USERNAME=' .env | cut -d= -f2)
NACOS_PASSWORD_VALUE=$(grep -E '^NACOS_PASSWORD=' .env | cut -d= -f2)
NACOS_GROUP_VALUE=${NACOS_GROUP_VALUE:-GYM_YZ_GROUP}
NACOS_USERNAME_VALUE=${NACOS_USERNAME_VALUE:-nacos}
NACOS_SERVER_URL="http://localhost:8848"
NACOS_CONSOLE_URL="http://localhost:18080"

PASS=0; FAIL=0
ok()  { echo "[PASS] $1"; PASS=$((PASS+1)); }
bad() { echo "[FAIL] $1"; FAIL=$((FAIL+1)); }

echo "=========================================="
echo " e2e 验证：OpenFeign 跨服务调用 + 负载均衡"
echo "=========================================="

# ---------- 1. Nacos 控制台就绪 ----------
CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$NACOS_CONSOLE_URL/v3/console/health/readiness" 2>/dev/null)
if [ "$CODE" = "200" ]; then ok "Nacos 控制台就绪 ($NACOS_CONSOLE_URL)"; else bad "Nacos 控制台未就绪 (HTTP ${CODE:-无响应})"; fi

# ---------- 2. member-service 注册 >= 2 个 healthy 实例 ----------
TOKEN=$(curl -s -X POST "$NACOS_SERVER_URL/nacos/v3/auth/user/login" -d "username=${NACOS_USERNAME_VALUE}&password=${NACOS_PASSWORD_VALUE}" 2>/dev/null | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
BODY=$(curl -s --max-time 5 "$NACOS_SERVER_URL/nacos/v3/admin/ns/instance/list?serviceName=member-service&groupName=${NACOS_GROUP_VALUE}&accessToken=${TOKEN}" 2>/dev/null)
HEALTHY_COUNT=$(echo "$BODY" | grep -o '"healthy":true' | wc -l | tr -d ' ')
if [ "${HEALTHY_COUNT:-0}" -ge 2 ]; then
  ok "member-service 注册 ${HEALTHY_COUNT} 个 healthy 实例（分组 ${NACOS_GROUP_VALUE}）"
  echo "$BODY" | grep -o '"ip":"[^"]*","port":[0-9]*' | sed 's/"ip":"/  实例 /; s/","port":/:/'
else
  bad "member-service healthy 实例不足 2 个（当前 ${HEALTHY_COUNT:-0}）"
fi

# ---------- 3. 服务健康接口 ----------
for P in 8081 8085 8082 8083; do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://localhost:${P}/actuator/health" 2>/dev/null)
  BODY=$(curl -s --max-time 5 "http://localhost:${P}/actuator/health" 2>/dev/null)
  if [ "$CODE" = "200" ] && echo "$BODY" | grep -q '"status":"UP"'; then
    ok "健康检查 端口 ${P} -> UP"
  else
    bad "健康检查 端口 ${P} -> HTTP ${CODE:-无响应}"
  fi
done

# ---------- 4. 负载均衡：两实例都被实际访问 ----------
count_hits() { docker logs "$1" 2>&1 | grep -c "GYM-INSTANCE .* GET /api/members" || true; }
BASE1=$(count_hits gym-member-service)
BASE2=$(count_hits gym-member-service-2)
echo "  连续 6 次经 card-service 办卡（memberId=1，OpenFeign -> LoadBalancer -> member 2 实例）..."
SUCCESS=0
for i in 1 2 3 4 5 6; do
  CODE=$(curl -s -o /tmp/e2e_card_$i.json -w "%{http_code}" --max-time 15 -X POST "http://localhost:8082/api/cards" \
    -H "Content-Type: application/json" \
    -d '{"memberId":1,"cardType":"TIMES","startDate":"2026-11-11","endDate":"2027-11-11","remainingTimes":30}' 2>/dev/null)
  if [ "$CODE" = "201" ]; then SUCCESS=$((SUCCESS+1)); fi
done
if [ "$SUCCESS" -eq 6 ]; then ok "6 次办卡全部 201 Created（card-service 经 Feign 校验会员）"; else bad "办卡成功 ${SUCCESS}/6"; fi
sleep 2
DELTA1=$(( $(count_hits gym-member-service) - BASE1 ))
DELTA2=$(( $(count_hits gym-member-service-2) - BASE2 ))
echo "  实例 member-1 新增请求 ${DELTA1} 次 / member-2 新增请求 ${DELTA2} 次"
if [ "$DELTA1" -gt 0 ] && [ "$DELTA2" -gt 0 ]; then
  ok "负载均衡验证：两个 member 实例都被实际访问（日志证据 GYM-INSTANCE）"
else
  bad "负载均衡验证：存在零命中实例（member-1: ${DELTA1}, member-2: ${DELTA2}）"
fi

# ---------- 5. 跨服务全链路：办卡 -> 入场 ----------
CARD_ID=$(curl -s --max-time 15 -X POST "http://localhost:8082/api/cards" -H "Content-Type: application/json" \
  -d '{"memberId":1,"cardType":"TIMES","startDate":"2026-11-11","endDate":"2027-11-11","remainingTimes":30}' 2>/dev/null \
  | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
if [ -n "${CARD_ID:-}" ] && [ "${CARD_ID:-0}" -gt 0 ]; then
  ok "跨服务办卡成功（card 调 member 校验，卡 id=${CARD_ID}）"
  CODE=$(curl -s -o /tmp/e2e_checkin.json -w "%{http_code}" --max-time 15 -X POST "http://localhost:8083/api/checkins" \
    -H "Content-Type: application/json" -d "{\"memberId\":1,\"cardId\":${CARD_ID}}" 2>/dev/null)
  if [ "$CODE" = "201" ]; then
    ok "跨服务入场成功（checkin 调 card 验卡，201 Created）"
  else
    bad "入场请求失败（HTTP ${CODE:-无响应}）"
  fi
else
  bad "办卡响应中未取得卡 id，无法继续入场验证"
fi

rm -f /tmp/e2e_card_*.json /tmp/e2e_checkin.json
echo "------------------------------------------"
echo " e2e 结果: 通过 ${PASS} / 失败 ${FAIL}"
echo "------------------------------------------"
if [ "$FAIL" -gt 0 ]; then exit 1; fi
exit 0