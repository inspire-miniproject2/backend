#!/usr/bin/env bash
set -euo pipefail

readonly SUBJECT_PATTERN='^\[(Add|Fix|Update|Remove|Refactor|Docs|Style|Setting)\]_(0[1-9]|[12][0-9]|3[01])/.+$'

show_help() {
  cat <<'EOF'
Commit title format:
  [Type]_DD/FileOrFeature

Allowed types:
  Add, Fix, Update, Remove, Refactor, Docs, Style, Setting

Example:
  [Add]_07/ComplaintCreate
EOF
}

validate_subject() {
  local subject="$1"

  if [[ "$subject" =~ ^Merge[[:space:]] ]]; then
    return 0
  fi

  if [[ ! "$subject" =~ $SUBJECT_PATTERN ]]; then
    echo "Invalid commit title: $subject" >&2
    show_help >&2
    return 1
  fi
}

validate_file() {
  local message_file="$1"
  local subject
  subject="$(sed -n '1p' "$message_file")"
  validate_subject "$subject"
}

validate_range() {
  local commit_range="$1"
  local failed=0

  while IFS= read -r subject; do
    if ! validate_subject "$subject"; then
      failed=1
    fi
  done < <(git log --no-merges --format='%s' "$commit_range")

  return "$failed"
}

case "${1:-}" in
  --file)
    validate_file "${2:?commit message file is required}"
    ;;
  --range)
    validate_range "${2:?commit range is required}"
    ;;
  --subject)
    validate_subject "${2:?commit subject is required}"
    ;;
  *)
    show_help
    exit 2
    ;;
esac

