#!/usr/bin/env bash
# =============================================================================
# auth-test.sh —— 第 09 周：API Gateway + JWT 认证端到端验证
#
# 用法：必须在仓库根目录运行
#   bash scripts/auth-test.sh
#
# 前置：docker compose up -d --build 全部容器 healthy（含 gateway-service）
#
# 验证场景（全部经网关统一入口 http://localhost:8080）：
#   1. 网关健康检查入口 /api/health 可用
#   2. 未登录访问受保护接口 → 401
#   3. 登录获取 Token（测试账号 user1/user123，角色 USER）→ 200
#   4. 携带 Token 访问受保护接口 → 200
#   5. 低权限 Token 访问管理接口 → 403（越权被阻止）
#   6. 伪造 X-User-Role 请求头也无法越过网关 → 403（身份头以 Token 为准）
#   7. ADMIN 登录后访问管理接口 → 200（admin/admin123）
#   8. 无效/篡改 Token → 401
#   9. 密码错误 → 401
#
# 设计为可重复运行：每次运行重新登录获取新 Token，不依赖上一次的状态。
# 任一场景失败脚本退出非零。
# =============================================================================
set -u

GATEWAY="http://localhost:8080"
PASS=0
FAIL=0
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# 从 JSON 响应中提取字段值（不依赖 jq）
json_field() {
    grep -o "\"$1\":\"[^\"]*\"" "$2" | head -1 | cut -d'"' -f4
}

check() {
    local name="$1" expect="$2" actual="$3"
    if [ "$expect" = "$actual" ]; then
        echo "[PASS] $name (HTTP $actual)"
        PASS=$((PASS + 1))
    else
        echo "[FAIL] $name —— 期望 HTTP $expect，实际 HTTP ${actual:-无响应}"
        FAIL=$((FAIL + 1))
    fi
}

echo "===== 第 09 周：API Gateway + JWT 认证验证 ====="
echo "统一入口: $GATEWAY"
echo ""

# ---------- 场景 1：网关健康检查入口 ----------
HTTP=$(curl -s -o "$TMP/health.json" -w "%{http_code}" --max-time 5 "$GATEWAY/api/health" 2>/dev/null)
check "网关健康检查入口 GET /api/health（路由转发可用）" 200 "$HTTP"

# ---------- 场景 2：未登录访问受保护接口，应失败 ----------
HTTP=$(curl -s -o "$TMP/noauth.json" -w "%{http_code}" --max-time 5 "$GATEWAY/api/members" 2>/dev/null)
check "未登录访问受保护接口 GET /api/members → 401" 401 "$HTTP"

# ---------- 场景 3：登录获取 Token ----------
HTTP=$(curl -s -o "$TMP/login.json" -w "%{http_code}" --max-time 5 \
    -X POST "$GATEWAY/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"user1","password":"user123"}' 2>/dev/null)
check "登录获取 Token（user1 / USER 角色）→ 200" 200 "$HTTP"
TOKEN=$(json_field "token" "$TMP/login.json")
ROLE=$(json_field "role" "$TMP/login.json")
if [ -n "$TOKEN" ] && [ "$ROLE" = "USER" ]; then
    echo "[PASS] 登录响应包含 Token 与角色 role=USER"
    PASS=$((PASS + 1))
else
    echo "[FAIL] 登录响应缺少 Token 或角色不符（role=${ROLE:-缺失}）"
    FAIL=$((FAIL + 1))
fi

# ---------- 场景 4：携带 Token 访问受保护接口，应成功 ----------
HTTP=$(curl -s -o "$TMP/authed.json" -w "%{http_code}" --max-time 5 \
    "$GATEWAY/api/members" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null)
check "携带 Token 访问受保护接口 GET /api/members → 200" 200 "$HTTP"

# ---------- 场景 5：低权限 Token 访问管理接口，应失败（越权被阻止） ----------
HTTP=$(curl -s -o "$TMP/lowrole.json" -w "%{http_code}" --max-time 5 \
    "$GATEWAY/api/members/statistics" \
    -H "Authorization: Bearer $TOKEN" 2>/dev/null)
check "USER Token 访问管理接口 GET /api/members/statistics → 403" 403 "$HTTP"

# ---------- 场景 6：伪造身份请求头也无法越过网关 ----------
HTTP=$(curl -s -o "$TMP/forged.json" -w "%{http_code}" --max-time 5 \
    "$GATEWAY/api/members/statistics" \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-User-Role: ADMIN" \
    -H "X-User-Id: 999" \
    -H "X-Gateway-Auth: forged-secret" 2>/dev/null)
check "伪造 X-User-Role: ADMIN 请求头访问管理接口 → 仍 403（网关剥离伪造头）" 403 "$HTTP"

# ---------- 场景 7：ADMIN 登录后访问管理接口，应成功 ----------
HTTP=$(curl -s -o "$TMP/admin-login.json" -w "%{http_code}" --max-time 5 \
    -X POST "$GATEWAY/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' 2>/dev/null)
check "管理员登录（admin / ADMIN 角色）→ 200" 200 "$HTTP"
ADMIN_TOKEN=$(json_field "token" "$TMP/admin-login.json")
HTTP=$(curl -s -o "$TMP/stats.json" -w "%{http_code}" --max-time 5 \
    "$GATEWAY/api/members/statistics" \
    -H "Authorization: Bearer $ADMIN_TOKEN" 2>/dev/null)
check "ADMIN Token 访问管理接口 GET /api/members/statistics → 200" 200 "$HTTP"
STATS=$(grep -o '"totalMembers":[0-9]*' "$TMP/stats.json" 2>/dev/null | head -1)
if [ -n "$STATS" ]; then
    echo "[PASS] 管理接口返回统计数据（$STATS）"
    PASS=$((PASS + 1))
else
    echo "[FAIL] 管理接口响应缺少统计数据"
    FAIL=$((FAIL + 1))
fi

# ---------- 场景 8：无效 / 篡改 Token，应失败 ----------
HTTP=$(curl -s -o "$TMP/badtoken.json" -w "%{http_code}" --max-time 5 \
    "$GATEWAY/api/members" \
    -H "Authorization: Bearer ${TOKEN}tampered" 2>/dev/null)
check "篡改 Token（追加字符破坏签名）→ 401" 401 "$HTTP"

# ---------- 场景 9：密码错误，应失败 ----------
HTTP=$(curl -s -o "$TMP/badpass.json" -w "%{http_code}" --max-time 5 \
    -X POST "$GATEWAY/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"user1","password":"wrong-password"}' 2>/dev/null)
check "密码错误登录 → 401" 401 "$HTTP"

echo ""
echo "===== auth-test 结果: 通过 $PASS / 失败 $FAIL ====="
if [ "$FAIL" -gt 0 ]; then
    echo "退出码: 1"
    exit 1
fi
echo "退出码: 0"
exit 0
