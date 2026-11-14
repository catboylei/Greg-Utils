#!/usr/bin/env bash
# Usage:
#   tangled-artifact.sh upload <repo-name> <tag> <file>
#   tangled-artifact.sh delete <tag>          # removes artifact records attached to <tag>
# Env: ATP_IDENTIFIER, TNGL_UPLOADER_APP_PASSWORD   (optional: PDS_URL to skip auto-detection)

# i want to kill myself
set -euo pipefail

curlj() { curl -sS --fail-with-body "$@"; }   # prints the server's error body on failure
log() { echo "[$(date +%T)] $*" >&2; }

# tag object hash -> unpadded base64 (the form atproto "bytes" fields use)
b64tag() { git rev-parse "refs/tags/$1" | xxd -r -p | base64 -w0 | tr -d '='; }

resolve_pds() {
  if [ -n "${PDS_URL:-}" ]; then echo "$PDS_URL"; return; fi
  log "resolving handle $ATP_IDENTIFIER"
  local did doc
  did=$(curlj "https://public.api.bsky.app/xrpc/com.atproto.identity.resolveHandle?handle=$ATP_IDENTIFIER" | jq -r .did)
  case "$did" in
    did:plc:*) doc=$(curlj "https://plc.directory/$did") ;;
    did:web:*) doc=$(curlj "https://${did#did:web:}/.well-known/did.json") ;;
    *) echo "cannot resolve PDS for '$did'" >&2; exit 1 ;;
  esac
  jq -r '.service[] | select(.id=="#atproto_pds") | .serviceEndpoint' <<<"$doc"
}

login() {
  PDS=$(resolve_pds)
  log "PDS is $PDS, logging in"
  local s
  s=$(curlj -X POST "$PDS/xrpc/com.atproto.server.createSession" \
    -H 'Content-Type: application/json' \
    -d "$(jq -n --arg i "$ATP_IDENTIFIER" --arg p "$TNGL_UPLOADER_APP_PASSWORD" '{identifier:$i,password:$p}')")
  JWT=$(jq -r .accessJwt <<<"$s")
  DID=$(jq -r .did <<<"$s")
  log "logged in as $DID"
}

upload() {
  local repo=$1 tag=$2 file=$3 uri blob
  login

  log "listing records"
  uri=$(curlj "$PDS/xrpc/com.atproto.repo.listRecords?repo=$DID&collection=sh.tangled.repo&limit=100" |
        jq -r --arg n "$repo" '.records[] | select(.value.name==$n) | .uri' | head -1)
  [ -n "$uri" ] || { echo "repo '$repo' not found in $DID's records" >&2; exit 1; }

  blob=$(curlj -X POST "$PDS/xrpc/com.atproto.repo.uploadBlob" \
    -H "Authorization: Bearer $JWT" -H 'Content-Type: application/octet-stream' \
    --data-binary @"$file" | jq .blob)

  jq -n --arg did "$DID" --arg repo "$uri" --arg name "$(basename "$file")" \
        --arg tag "$(b64tag "$tag")" --arg now "$(date -u +%Y-%m-%dT%H:%M:%SZ)" --argjson blob "$blob" \
    '{repo:$did, collection:"sh.tangled.repo.artifact",
      record:{"$type":"sh.tangled.repo.artifact", name:$name, repo:$repo,
              tag:{"$bytes":$tag}, createdAt:$now, artifact:$blob}}' |
  curlj -X POST "$PDS/xrpc/com.atproto.repo.createRecord" \
    -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' -d @- >/dev/null

  echo "uploaded $(basename "$file") to $tag"
}

delete() {
  local tag=$1 b rkey
  login
  b=$(b64tag "$tag")
  log "listing records"
  curlj "$PDS/xrpc/com.atproto.repo.listRecords?repo=$DID&collection=sh.tangled.repo.artifact&limit=100" |
    jq -r --arg b "$b" '.records[] | select(.value.tag["$bytes"]==$b) | .uri | split("/") | last' |
  while read -r rkey; do
    [ -n "$rkey" ] || continue
    curlj -X POST "$PDS/xrpc/com.atproto.repo.deleteRecord" \
      -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
      -d "$(jq -n --arg d "$DID" --arg k "$rkey" '{repo:$d,collection:"sh.tangled.repo.artifact",rkey:$k}')" >/dev/null
    echo "deleted artifact record $rkey for $tag"
  done
}

case "${1:-}" in
  upload) shift; upload "$@" ;;
  delete) shift; delete "$@" ;;
  *) echo "usage: $0 upload <repo> <tag> <file> | delete <tag>" >&2; exit 2 ;;
esac