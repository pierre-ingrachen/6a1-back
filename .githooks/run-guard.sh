#!/usr/bin/env bash
set -u

TOP="$(git rev-parse --show-toplevel)"
if [ -f "$TOP/pom.xml" ]; then
  NAME="6a1-back"
else
  NAME="6a1-front"
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/scripts"
ln -s "$TOP" "$WORK/$NAME"

GUARD_ROOT="$WORK" GUARD_ALLOW_FILE="$TOP/.githooks/audit-guard.allow" "$TOP/.githooks/audit-guard.sh" "$@"
