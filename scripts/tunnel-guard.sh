#!/bin/bash
# 隧道守护：为云端 BFF（localhost:8787）建立公网入口。
# 策略：优先 Pinggy（60 分钟），且只接受带 IPv4 的节点；多次失败回退 Serveo。
# 日志写在仓库内 .run/（沙箱的 /tmp 会被自动清理）。
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/.run"
mkdir -p "$RUN"
LOG="$RUN/tunnel.log"
GUARD_LOG="$RUN/guard.log"
PROXY="${SSH_PROXY_CMD:-nc -X connect -x 127.0.0.1:18080 %h %p}"

log() { echo "[guard] $(date -u +%H:%M:%S) $*" | tee -a "$GUARD_LOG"; }

push_config() {
  local url="$1"
  printf '{\n  "baseUrl": "%s"\n}\n' "$url" > "$ROOT/config/backend.json"
  git -C "$ROOT" add config/backend.json
  git -C "$ROOT" -c user.name='yaunfei' \
      -c user.email='29477759+yaunfei@users.noreply.github.com' \
      commit -q -m "chore(tunnel): point BFF to $url" >/dev/null 2>&1 || true
  git -C "$ROOT" -c credential.helper='store --file=/run/shared/git/credentials' \
      push -q origin HEAD:main >/dev/null 2>&1 || true
}

start_pinggy() {
  : > "$LOG"
  ssh -p 443 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 \
      -o ServerAliveCountMax=2 -o ExitOnForwardFailure=yes -o ConnectTimeout=20 \
      -o ProxyCommand="$PROXY" -R0:localhost:8787 a.pinggy.io >> "$LOG" 2>&1 &
  SSH_PID=$!
  URL=""
  for _ in $(seq 1 40); do
    URL=$(grep -oE 'https://[a-z0-9-]+\.free\.pinggy\.net' "$LOG" | head -1)
    [ -n "$URL" ] && break
    kill -0 "$SSH_PID" 2>/dev/null || return 1
    sleep 1
  done
  [ -z "$URL" ] && return 1
  local host
  host=${URL#https://}
  if getent ahostsv4 "$host" | grep -q .; then
    return 0
  fi
  log "pinggy node is IPv6-only, retry"
  kill "$SSH_PID" 2>/dev/null
  wait "$SSH_PID" 2>/dev/null
  return 1
}

start_serveo() {
  : > "$LOG"
  ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=30 -o ExitOnForwardFailure=yes \
      -o ConnectTimeout=20 -o ProxyCommand="$PROXY" -R 80:localhost:8787 \
      nokey@localhost.run -- --output json >> "$LOG" 2>&1 &
  SSH_PID=$!
  URL=""
  for _ in $(seq 1 40); do
    URL=$(grep -oE 'https://[a-z0-9]+\.lhr\.life' "$LOG" | head -1)
    [ -n "$URL" ] && return 0
    kill -0 "$SSH_PID" 2>/dev/null || return 1
    sleep 1
  done
  return 1
}

while true; do
  ok=0
  MODE=""
  for _ in 1 2 3 4 5 6; do
    if start_pinggy; then ok=1; MODE=pinggy; break; fi
    sleep 2
  done
  if [ "$ok" = "0" ]; then
    log "pinggy unavailable, falling back to serveo"
    if start_serveo; then ok=1; MODE=serveo; fi
  fi

  if [ "$ok" = "1" ]; then
    log "tunnel up ($MODE): $URL"
    push_config "$URL"
    # 沙箱出网受限，HTTP 探活不可信（会误判），只以 SSH 会话存活为准。
    # Pinggy 免费版到期会主动断开 SSH，届时自然进入重连。
    while kill -0 "$SSH_PID" 2>/dev/null; do
      sleep 20
    done
    wait "$SSH_PID" 2>/dev/null
  else
    log "all tunnels failed, retry in 5s"
    sleep 5
  fi
done
