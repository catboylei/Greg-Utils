#!/usr/bin/env bash
# usage: delete-tag.sh <tag>
set -u
TAG=$1

export GIT_SSH_COMMAND="ssh ${SSH_DEBUG:+-v} -i $HOME/.ssh/privkey -o IdentitiesOnly=yes -o BatchMode=yes -o ConnectTimeout=15 -o AddressFamily=inet -o IPQoS=none -o KexAlgorithms=curve25519-sha256"

for attempt in 1 2 3; do
  echo "delete attempt $attempt"
  timeout 30 git push origin --delete "$TAG" && break
  echo "push exit code: $?"
  sleep 3
done

if timeout 30 git ls-remote --exit-code --tags origin "refs/tags/$TAG" >/dev/null 2>&1; then
  echo "tag $TAG still exists on the remote" >&2
  exit 1
fi
echo "tag $TAG is gone from the remote"
git tag -d "$TAG" 2>/dev/null || true