#!/bin/sh
# Usage: ./removeTag.sh <tag> <remote>
set -eu

tag="${1:?usage: $0 <tag> [remote]}"
remote="${2:-origin}"

git tag -d "$tag" 2>/dev/null || echo "no local tag $tag (skipping)"
git push "$remote" --delete "$tag"
echo "removed $tag"