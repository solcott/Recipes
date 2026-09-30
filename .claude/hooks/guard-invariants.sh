#!/usr/bin/env bash
# PreToolUse (Write|Edit): ask before an edit introduces something CLAUDE.md or .claude/rules/
# deliberately rules out. Matches call/declaration syntax only, so KDoc that mentions a name
# (BrowserHistoryEffect.web.kt explains rememberSaveableBackStack) passes.
input=$(cat)
file=$(jq -r '.tool_input.file_path // empty' <<<"$input")
text=$(jq -r '[.tool_input.content, .tool_input.new_string] | map(select(. != null)) | join("\n")' <<<"$input")
[ -n "$file" ] && [ -n "$text" ] || exit 0

reasons=()
case "$file" in
  *.kt)
    grep -q 'fallbackToDestructiveMigration[A-Za-z]*(' <<<"$text" &&
      reasons+=("fallbackToDestructiveMigration: Room has no destructive fallback on purpose; the crash is the reminder to write a Migration (.claude/rules/storage.md).")
    grep -q 'rememberSaveableBackStack(' <<<"$text" &&
      reasons+=("rememberSaveableBackStack: BackStack stubs forward()/backward(), which silently breaks browser back/forward on web; stay on rememberSaveableNavStack (.claude/rules/circuit.md).")
    ;;
  *.kts | *.toml)
    grep -qE '\biosX64\b' <<<"$text" &&
      reasons+=("iosX64: deliberately not a target; Intel Macs can't run the iOS simulator build (.claude/rules/gradle-build.md).")
    grep -qE 'plugin\.parcelize|kotlin-parcelize' <<<"$text" &&
      reasons+=("kotlin-parcelize: @Parcelize comes from io.github.solcott.kmp.parcelize, and Screens are not Parcelable (.claude/rules/gradle-build.md).")
    ;;
esac
[ ${#reasons[@]} -gt 0 ] || exit 0

reason=$(printf '%s ' "${reasons[@]}")
jq -n --arg r "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse",
  permissionDecision: "ask", permissionDecisionReason: ("Project invariant: " + $r)}}'
