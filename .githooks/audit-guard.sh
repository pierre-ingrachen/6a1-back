#!/usr/bin/env bash
# Garde déterministe : détecte les violations des règles du projet sur les fichiers modifiés.
# Usage : audit-guard.sh [--all | --file <chemin>]
set -u

ROOT="${GUARD_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
ALLOW_FILE="${GUARD_ALLOW_FILE:-$ROOT/scripts/audit-guard.allow}"
REPOS=". 6a1-back 6a1-front"
RESULTS="$(mktemp)"
FILES="$(mktemp)"
trap 'rm -f "$RESULTS" "$FILES"' EXIT

mode="changed"
single_file=""
case "${1:-}" in
  --all) mode="all" ;;
  --file) mode="file"; single_file="${2:-}" ;;
  "") ;;
  *) echo "usage: $0 [--all | --file <chemin>]" >&2; exit 64 ;;
esac

cd "$ROOT" || exit 64

allow_errors=0
if [ -f "$ALLOW_FILE" ]; then
  while IFS= read -r allow_line || [ -n "$allow_line" ]; do
    case "$allow_line" in ""|"#"*) continue ;; esac
    justification="${allow_line#*|*|}"
    if [ "$(printf '%s' "$allow_line" | tr -cd '|' | wc -c | tr -d ' ')" -lt 2 ] || [ -z "$(printf '%s' "$justification" | tr -d ' ')" ]; then
      echo "scripts/audit-guard.allow: exception sans justification: $allow_line" >> "$RESULTS"
      allow_errors=1
    fi
  done < "$ALLOW_FILE"
fi

is_allowed() {
  local rule="$1" path="$2" allow_rule allow_path rest
  [ -f "$ALLOW_FILE" ] || return 1
  while IFS='|' read -r allow_rule allow_path rest || [ -n "$allow_rule" ]; do
    case "$allow_rule" in ""|"#"*) continue ;; esac
    [ -n "$(printf '%s' "$rest" | tr -d ' ')" ] || continue
    if [ "$allow_rule" = "$rule" ]; then
      case "$allow_path" in
        *"*") case "$path" in "${allow_path%\*}"*) return 0 ;; esac ;;
        *) [ "$path" = "$allow_path" ] && return 0 ;;
      esac
    fi
  done < "$ALLOW_FILE"
  return 1
}

emit() {
  is_allowed "$2" "$1" && return 0
  echo "$1:$3: $2" >> "$RESULTS"
}

match_lines() {
  local file="$1" rule="$2" regex="$3" number
  grep -nE -- "$regex" "$file" 2>/dev/null | cut -d: -f1 | while IFS= read -r number; do
    emit "$file" "$rule" "$number"
  done
}

match_lines_excluding() {
  local file="$1" rule="$2" regex="$3" exclusion="$4" number
  grep -nE -- "$regex" "$file" 2>/dev/null | grep -vE -- "$exclusion" | cut -d: -f1 | while IFS= read -r number; do
    emit "$file" "$rule" "$number"
  done
}

collect_changed() {
  local repo prefix
  for repo in $REPOS; do
    [ -e "$repo/.git" ] || continue
    prefix=""
    [ "$repo" = "." ] || prefix="$repo/"
    {
      git -C "$repo" diff --name-only HEAD 2>/dev/null || git -C "$repo" diff --name-only 2>/dev/null
      git -C "$repo" ls-files --others --exclude-standard 2>/dev/null
    } | while IFS= read -r path; do
      [ -n "$path" ] || continue
      if [ "$repo" = "." ]; then
        case "$path" in 6a1-back/*|6a1-front/*) continue ;; esac
      fi
      echo "$prefix$path"
    done
  done
}

collect_all() {
  find -L . \( -name node_modules -o -name target -o -name dist -o -name .git -o -name .angular -o -name .idea \) -prune -o -type f -print | sed 's|^\./||'
}

case "$mode" in
  all) collect_all > "$FILES" ;;
  file) printf '%s\n' "${single_file#"$ROOT"/}" > "$FILES" ;;
  *) collect_changed | sort -u > "$FILES" ;;
esac

check_empty_catch() {
  local file="$1"
  awk -v file="$file" '
    /catch[[:space:]]*\(Exception[^)]*\)[[:space:]]*\{[[:space:]]*\}/ { print file ":" NR ": B-EMPTY-CATCH"; next }
    /catch[[:space:]]*\(Exception[^)]*\)[[:space:]]*\{[[:space:]]*$/ { pending = NR; next }
    pending && /^[[:space:]]*\}[[:space:]]*$/ { print file ":" pending ": B-EMPTY-CATCH"; pending = 0; next }
    { pending = 0 }
  ' "$file" 2>/dev/null | while IFS= read -r found; do
    emit "${found%%:*}" "B-EMPTY-CATCH" "$(printf '%s' "$found" | cut -d: -f2)"
  done
}

check_subscribe_cleanup() {
  local file="$1"
  grep -q "takeUntilDestroyed" "$file" 2>/dev/null && return 0
  match_lines "$file" "F-SUBSCRIBE-LEAK" '\.subscribe\('
}

check_unhandled_http_flow() {
  local file="$1"
  grep -qE 'switchMap\(|mergeMap\(' "$file" 2>/dev/null || return 0
  grep -qE 'Service|HttpClient' "$file" 2>/dev/null || return 0
  grep -qE 'catchError|toLoadState' "$file" 2>/dev/null && return 0
  match_lines "$file" "F-HTTP-FLOW-WITHOUT-CATCH" 'switchMap\(|mergeMap\('
}

scan_file() {
  local file="$1"
  [ -f "$file" ] || return 0
  case "$file" in
    6a1-back/src/main/*.java)
      case "$file" in */controller/*|*/controllers/*) match_lines "$file" "B-CORS" '@CrossOrigin' ;; esac
      if grep -qE 'extends[[:space:]]+[A-Za-z]*Exception' "$file"; then
        match_lines "$file" "B-RESPONSE-STATUS" '@ResponseStatus'
      fi
      match_lines "$file" "B-SYSOUT" 'System\.(out|err)|printStackTrace'
      match_lines "$file" "B-SELECT-STAR" 'SELECT[[:space:]]+\*|[^A-Za-z_]s\.\*'
      match_lines "$file" "B-SECRET" '(password|secret|token)[A-Za-z]*[[:space:]]*=[[:space:]]*"[^"$]+"'
      check_empty_catch "$file"
      match_lines_excluding "$file" "B-REQUEST-BODY-WITHOUT-VALID" '@RequestBody' '@Valid'
      ;;
  esac
  case "$file" in
    6a1-back/src/main/resources/*.properties|6a1-back/src/main/resources/*.yml|6a1-back/src/main/resources/*.yaml)
      match_lines "$file" "B-ERROR-MESSAGE-EXPOSED" 'server\.error\.include-message'
      match_lines_excluding "$file" "B-SECRET" '^[[:space:]]*[A-Za-z0-9_.-]*password[[:space:]]*[=:][[:space:]]*[^[:space:]]' '\$\{'
      ;;
  esac
  case "$file" in
    docker-compose*.yml|*/docker-compose*.yml|docker-compose*.yaml|*/docker-compose*.yaml)
      match_lines_excluding "$file" "B-DOCKER-PORT-EXPOSED" '^[[:space:]]*-[[:space:]]*"?[0-9]+:[0-9]+' '127\.0\.0\.1'
      ;;
  esac
  case "$file" in
    6a1-front/src/app/*.ts|6a1-front/src/app/*.html)
      case "$file" in
        *.spec.ts) ;;
        *)
          match_lines "$file" "F-HARDCODED-URL" 'https?://'
          check_subscribe_cleanup "$file"
          check_unhandled_http_flow "$file"
          match_lines "$file" "F-CONSOLE-LOG" 'console\.log'
          ;;
      esac
      match_lines "$file" "F-ANY" ':[[:space:]]*any([^A-Za-z0-9_]|$)|<any>|as any'
      match_lines "$file" "F-DOUBLE-BANG" '!!'
      match_lines "$file" "F-NGMODULE" '@NgModule'
      match_lines "$file" "F-STRUCTURAL-DIRECTIVE" '\*ng(If|For)'
      ;;
  esac
  case "$file" in
    6a1-back/src/*.java)
      match_lines "$file" "NO-COMMENTS" '^[[:space:]]*(//|/\*)|[[:space:]]//'
      ;;
    6a1-front/src/app/*.ts)
      match_lines_excluding "$file" "NO-COMMENTS" '^[[:space:]]*(//|/\*)|[[:space:]]//' '//[[:space:]]*@ts-'
      ;;
    6a1-front/src/app/*.html)
      match_lines "$file" "NO-COMMENTS" '<!--'
      ;;
    6a1-front/src/app/*.scss)
      match_lines "$file" "NO-COMMENTS" '^[[:space:]]*(//|/\*)|[[:space:]]//'
      ;;
  esac
}

while IFS= read -r candidate; do
  [ -n "$candidate" ] && scan_file "$candidate"
done < "$FILES"

if [ -s "$RESULTS" ]; then
  echo "audit-guard: violations détectées" >&2
  sort -u "$RESULTS" >&2
  exit 1
fi
echo "audit-guard: aucune violation" >&2
exit 0
