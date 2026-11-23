#!/bin/bash
# 隧道守护：为云端 BFF（localhost:8787）建立公网入口。
# 主用 Pinggy（免费 60 分钟，Serveo 仅 10 分钟）；会话到期/异常时自动重连，
# 并把最新地址写回 GitHub 的 config/backend.json，供 App 回源。
set -u
cd "$(dirname "$0")/.." || exit 1

REPO_ROOT="$(pwd)"
PUSH_CRED="${GIT_CREDENTIAL_FILE:-/run/shared/git/credentials}"

push_config() {
  local url="$1"
  printf '{\n  "baseUrl": "%s"\n}\n' "$url" > "$REPO_ROOT/config/backend.json"
  git -C "$REPO_ROOT" add config/backend.json
  git -C "$REPO_ROOT" -c user.name='yaunfei' \
      -c user.email='29477759+yaunfei@users.noreply.github.com' \
      commit -q -m "chore(tunnel): point BFF to $url" >/dev/null 2>&1 || true
  git -C "$REPO_ROOT" -c credential.helper="store --file=$PUSH_CRED" \
      push -q origin HEAD:main >/dev/null 2>&1 || true
}

start_tunnel() {
  : > /tmp/tunnel.log
  ssh -p 443 -o StrictHostKeyChecking=no \
      -o ServerAliveInterval=30 -o ServerAliveCountMax=2 \
      -o ExitOnForwardFailure=yes -o ConnectTimeout=25 \
      -o ProxyCommand="${SSH_PROXY_CMD:-nc -X connect -x 127.0.0.1:18080 %h %p}" \
      -R0:localhost:8787 a.pinggy.io >> /tmp/tunnel.log 2>&1 &
  SSH_PID=$!
  URL=""
  for _ in $(seq 1 45); do
    URL=$(grep -oE 'https://[a-z0-9-]+\.free\.pinggy\.net' /tmp/tunnel.log | head -1)
    [ -n "$URL" ] && return 0
    kill -0 "$SSH_PID" 2>/dev/null || return 1
    sleep 1
  done
  return 1
}

while true; do
  if start_tunnel; then
    echo "[guard] $(date -u +%H:%M:%S) tunnel up: $URL"
    push_config "$URL"
    while true; do
      sleep 45
      CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$URL/healthz" 2>/dev/null)
      if [ "$CODE" = "200" ] && kill -0 "$SSH_PID" 2>/dev/null; then
        continue
      fi
      echo "[guard] $(date -u +%H:%M:%S) healthz=$CODE, recycling tunnel"
      kill "$SSH_PID" 2>/dev/null
      wait "$SSH_PID" 2>/dev/null
      break
    done
  else
    echo "[guard] $(date -u +%H:%M:%S) start failed, retrying"
    kill "$SSH_PID" 2>/dev/null
    sleep 5
  fi
done
