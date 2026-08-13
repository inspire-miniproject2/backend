#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

git -C "$PROJECT_DIR" config core.hooksPath .githooks
chmod +x "$PROJECT_DIR/.githooks/commit-msg"
chmod +x "$PROJECT_DIR/scripts/validate-commit-message.sh"

echo "Git commit message validation is enabled."
echo "Format: [Type]_DD/FileOrFeature"

