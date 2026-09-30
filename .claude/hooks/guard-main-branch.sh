#!/usr/bin/env bash
# PreToolUse (Bash): refuse git commit/push while on main. CLAUDE.md: work on a feature branch
# and open a PR; never commit directly to main.
input=$(cat)
cmd=$(jq -r '.tool_input.command // empty' <<<"$input")
grep -qE '(^|[;&|(]|&&)[[:space:]]*git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+(commit|push)\b' <<<"$cmd" || exit 0

dir=$(jq -r '.cwd // empty' <<<"$input")
branch=$(git -C "${dir:-.}" branch --show-current 2>/dev/null)
# Only a real `git push` whose own arguments name main; prose such as a commit message that
# mentions "push ... main" must not trip this.
pushes_main=$(grep -qE '(^|[;&|(])[[:space:]]*git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push[[:space:]][^;&|]*[[:space:]:+](refs/heads/)?main([[:space:]]|$)' <<<"$cmd" && echo yes)
[ "$branch" = main ] || [ -n "$pushes_main" ] || exit 0

jq -n '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny",
  permissionDecisionReason: "Never commit or push directly to main (CLAUDE.md, Git etiquette). Create a feature branch, commit there, and open a PR with gh."}}'
