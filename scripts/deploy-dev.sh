#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
IMAGE="${IMAGE:-542495332980.dkr.ecr.ap-northeast-2.amazonaws.com/backend:dev}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/backend.pem}"
EC2_USER="${EC2_USER:-ec2-user}"
EC2_HOST="${EC2_HOST:-54.116.41.244}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-/home/ec2-user/app}"
HEALTH_URL="${HEALTH_URL:-https://api.looky.my/v3/api-docs}"
DEPLOY_REF="${DEPLOY_REF:-HEAD}"
WORKTREE_DIR="$(mktemp -d /tmp/looky-server-deploy.XXXXXX)"
TAR_FILE="$WORKTREE_DIR/backend-dev.tar"

cleanup() {
  git -C "$ROOT_DIR" worktree remove --force "$WORKTREE_DIR" >/dev/null 2>&1 || rm -rf "$WORKTREE_DIR"
}
trap cleanup EXIT

echo "Deploy ref: $DEPLOY_REF"
echo "Image: $IMAGE"
echo "Host: $EC2_USER@$EC2_HOST"

git -C "$ROOT_DIR" worktree add --detach "$WORKTREE_DIR" "$DEPLOY_REF"

docker build -t "$IMAGE" "$WORKTREE_DIR"
docker save -o "$TAR_FILE" "$IMAGE"
scp -i "$SSH_KEY" "$TAR_FILE" "$EC2_USER@$EC2_HOST:$REMOTE_APP_DIR/backend-dev.tar"

ssh -i "$SSH_KEY" "$EC2_USER@$EC2_HOST" \
  "cd '$REMOTE_APP_DIR' && docker load -i backend-dev.tar && docker compose up -d --force-recreate app && docker compose ps app"

for attempt in {1..12}; do
  status="$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL" || true)"
  if [[ "$status" == "200" ]]; then
    echo "Health check OK: $HEALTH_URL"
    exit 0
  fi
  echo "Health check pending: attempt=$attempt status=$status"
  sleep 5
done

ssh -i "$SSH_KEY" "$EC2_USER@$EC2_HOST" "cd '$REMOTE_APP_DIR' && docker compose logs --tail=120 app"
echo "Health check failed: $HEALTH_URL" >&2
exit 1
