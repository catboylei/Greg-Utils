#!/bin/sh
# Usage: ./addTags.sh <tag> <"message"> <remote>
set -eu

tag="${1:?usage: $0 <tag> [message] [remote]}"
msg="${2:-Release $tag}"
remote="${3:-origin}"

if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
  echo "tag $tag already exists locally" >&2
  exit 1
fi

git tag -a "$tag" -m "$msg"
git push "$remote" "$tag"
echo "added and pushed $tag"