#!/usr/bin/env bash
# SessionStart: warn early if the build can't start. MEALDB_API_KEY is required
# (build-logic ProjectExt.kt). Checks presence only; never prints a value.
props=("$HOME/.gradle/gradle.properties" "${CLAUDE_PROJECT_DIR:-.}/gradle.properties")
has_prop() { grep -qsE "^[[:space:]]*$1[[:space:]]*[=:][[:space:]]*[^[:space:]]" "${props[@]}"; }

missing=()
{ [ -n "$MEALDB_API_KEY" ] || has_prop MEALDB_API_KEY; } ||
  missing+=("MEALDB_API_KEY (add to ~/.gradle/gradle.properties or export it; key from https://www.themealdb.com/api.php)")
[ ${#missing[@]} -gt 0 ] || exit 0

msg="Gradle builds will fail until these are set: $(printf '%s; ' "${missing[@]}")"
jq -n --arg m "$msg" '{systemMessage: $m,
  hookSpecificOutput: {hookEventName: "SessionStart", additionalContext: $m}}'
