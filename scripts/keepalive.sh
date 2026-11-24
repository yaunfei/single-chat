#!/bin/bash
# 保活：沙箱会定期收割后台进程，此脚本负责把 BFF 与隧道守护拉起来。
# 由平台级定时任务（Schedule）周期调用，不依赖自身常驻。
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STAMP() { date -u +%H:%M:%S; }

# 1) BFF（node :8787）
if ! curl -s -o /dev/null --max-time 5 http://localhost:8787/healthz; then
  if [ ! -f "$ROOT/server/.env" ]; then
    # .env 被清则重建 dev 占位配置（IM_SECRET_KEY 等用户补发真值）
    JWT=$(openssl rand -hex 32 2>/dev/null || head -c32 /dev/urandom | od -An -tx1 | tr -d ' \n')
    printf 'PORT=8787\nJWT_SECRET=%s\nJWT_TTL_SECONDS=604800\nIM_SDK_APP_ID=1600174706\nIM_SECRET_KEY=dev-only-change-me\nIM_USERSIG_EXPIRE_SECONDS=604800\nDEV_SHOW_SMS_CODE=true\nCORS_ORIGIN=*\n' "$JWT" > "$ROOT/server/.env"
  fi
  ( cd "$ROOT/server" && setsid nohup node src/server.mjs > "$ROOT/.run-bff.log" 2>&1 < /dev/null & )
  sleep 2
  if curl -s -o /dev/null --max-time 5 http://localhost:8787/healthz; then
    echo "[keepalive] $(STAMP) bff restarted"
  else
    echo "[keepalive] $(STAMP) bff FAILED to start (see .run-bff.log)"
  fi
fi

# 2) 隧道守护
if ! pgrep -f 'tunnel-guard\.sh' > /dev/null; then
  mkdir -p "$ROOT/.run"
  ( cd "$ROOT" && setsid nohup ./scripts/tunnel-guard.sh > /dev/null 2>&1 < /dev/null & )
  echo "[keepalive] $(STAMP) tunnel guard restarted"
fi

# 3) 状态摘要
BFF=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://localhost:8787/healthz)
URL=$(git -C "$ROOT" show origin/main:config/backend.json 2>/dev/null | grep -oE 'https://[a-z0-9.-]+' | head -1)
GUARD=$(pgrep -f 'tunnel-guard\.sh' > /dev/null && echo up || echo down)
echo "[keepalive] $(STAMP) bff=$BFF guard=$GUARD url=$URL"
