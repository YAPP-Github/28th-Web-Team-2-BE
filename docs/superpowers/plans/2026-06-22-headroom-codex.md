# Headroom Codex Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a repository-scoped Headroom installation that gives Codex CLI automatic proxy compression and gives Codex desktop and CLI on-demand MCP compression.

**Architecture:** A project-local Python virtual environment owns the third-party package and remains gitignored. Small Bash launchers resolve the Git root, disable telemetry, and invoke either `headroom wrap codex` or `headroom mcp serve`; Codex discovers the latter from project `.codex/config.toml` and uses it through a repo-local skill.

**Tech Stack:** Bash, Python 3.10+, Headroom `headroom-ai[proxy,code]`, Codex project config, MCP stdio, Codex Agent Skills.

---

## File map

- `.headroom/.gitignore`: excludes the local Headroom runtime without modifying the root ignore file.
- `scripts/setup-headroom`: creates and populates `.headroom/venv`.
- `scripts/codex-headroom`: starts Codex CLI through Headroom's proxy wrapper.
- `.codex/config.toml`: registers the project Headroom MCP server.
- `.agents/skills/headroom/SKILL.md`: defines when Codex should compress or retrieve content.
- `.agents/skills/headroom/agents/openai.yaml`: exposes skill UI metadata and the MCP dependency.
- `.agents/skills/headroom/scripts/serve-mcp`: starts the project-local Headroom MCP server.
- `tests/headroom/fixtures/fake-headroom`: deterministic launcher test double.
- `tests/headroom/setup_headroom_test.sh`: tests the bootstrap interface and ignore rule.
- `tests/headroom/codex_headroom_test.sh`: tests CLI argument and environment forwarding.
- `tests/headroom/headroom_skill_mcp_test.sh`: validates the skill, MCP launcher, and Codex config.

### Task 1: Project-local Headroom bootstrap

**Files:**
- Create: `tests/headroom/setup_headroom_test.sh`
- Create: `scripts/setup-headroom`
- Create: `.headroom/.gitignore`

- [ ] **Step 1: Write the failing bootstrap contract test**

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"

help_output="$("$ROOT/scripts/setup-headroom" --help)"
grep -Fq 'Usage: ./scripts/setup-headroom' <<<"$help_output"
grep -Fqx '*' "$ROOT/.headroom/.gitignore"
grep -Fqx '!.gitignore' "$ROOT/.headroom/.gitignore"
bash -n "$ROOT/scripts/setup-headroom"

echo 'setup_headroom_test: PASS'
```

- [ ] **Step 2: Run the test and verify it fails because the setup script does not exist**

Run: `bash tests/headroom/setup_headroom_test.sh`

Expected: FAIL with `scripts/setup-headroom: No such file or directory`.

- [ ] **Step 3: Implement the bootstrap and ignore rule**

Create `.headroom/.gitignore`:

```gitignore
*
!.gitignore
```

Create `scripts/setup-headroom`:

```bash
#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo 'Usage: ./scripts/setup-headroom'
  echo 'Creates .headroom/venv and installs headroom-ai[proxy,code].'
}

case "${1:-}" in
  --help|-h)
    usage
    exit 0
    ;;
  '') ;;
  *)
    usage >&2
    exit 2
    ;;
esac

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"
PYTHON_BIN="${PYTHON:-python3}"
VENV="$ROOT/.headroom/venv"
REQUIREMENT="${HEADROOM_REQUIREMENT:-headroom-ai[proxy,code]}"

command -v "$PYTHON_BIN" >/dev/null 2>&1 || {
  echo "Python not found: $PYTHON_BIN" >&2
  exit 1
}

"$PYTHON_BIN" -c 'import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else "Headroom requires Python 3.10+")'

if [[ ! -x "$VENV/bin/python" ]]; then
  "$PYTHON_BIN" -m venv "$VENV"
fi

"$VENV/bin/python" -m pip install --upgrade pip
"$VENV/bin/python" -m pip install "$REQUIREMENT"
"$VENV/bin/python" -c 'import headroom; print(f"Headroom {headroom.__version__} installed")'
```

Make both scripts executable: `chmod +x scripts/setup-headroom tests/headroom/setup_headroom_test.sh`.

- [ ] **Step 4: Run the bootstrap contract test**

Run: `bash tests/headroom/setup_headroom_test.sh`

Expected: `setup_headroom_test: PASS`.

- [ ] **Step 5: Commit only Task 1 files**

```bash
git add .headroom/.gitignore scripts/setup-headroom tests/headroom/setup_headroom_test.sh
git commit --only -m "build: add project Headroom bootstrap" -- .headroom/.gitignore scripts/setup-headroom tests/headroom/setup_headroom_test.sh
```

### Task 2: Codex CLI automatic-compression wrapper

**Files:**
- Create: `tests/headroom/fixtures/fake-headroom`
- Create: `tests/headroom/codex_headroom_test.sh`
- Create: `scripts/codex-headroom`

- [ ] **Step 1: Write the fake executable and failing wrapper test**

Create `tests/headroom/fixtures/fake-headroom`:

```bash
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "${HEADROOM_TELEMETRY:-unset}"
printf '%s\n' "$@"
```

Create `tests/headroom/codex_headroom_test.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
FAKE="$ROOT/tests/headroom/fixtures/fake-headroom"

output="$(HEADROOM_BIN="$FAKE" "$ROOT/scripts/codex-headroom" --search)"
[[ "$output" == $'off\nwrap\ncodex\n--search' ]]

if HEADROOM_BIN="$ROOT/.headroom/missing" "$ROOT/scripts/codex-headroom" >/dev/null 2>&1; then
  echo 'expected missing Headroom executable to fail' >&2
  exit 1
fi

echo 'codex_headroom_test: PASS'
```

Make the fixture and test executable.

- [ ] **Step 2: Run the test and verify it fails because the wrapper does not exist**

Run: `bash tests/headroom/codex_headroom_test.sh`

Expected: FAIL with `scripts/codex-headroom: No such file or directory`.

- [ ] **Step 3: Implement the CLI wrapper**

Create `scripts/codex-headroom`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"
HEADROOM_BIN="${HEADROOM_BIN:-$ROOT/.headroom/venv/bin/headroom}"

if [[ ! -x "$HEADROOM_BIN" ]]; then
  echo 'Headroom is not installed. Run ./scripts/setup-headroom first.' >&2
  exit 1
fi

export HEADROOM_TELEMETRY=off
exec "$HEADROOM_BIN" wrap codex "$@"
```

Make the wrapper executable.

- [ ] **Step 4: Run syntax and wrapper tests**

Run: `bash -n scripts/codex-headroom && bash tests/headroom/codex_headroom_test.sh`

Expected: `codex_headroom_test: PASS`.

- [ ] **Step 5: Commit only Task 2 files**

```bash
git add scripts/codex-headroom tests/headroom/fixtures/fake-headroom tests/headroom/codex_headroom_test.sh
git commit --only -m "feat: wrap Codex CLI with Headroom" -- scripts/codex-headroom tests/headroom/fixtures/fake-headroom tests/headroom/codex_headroom_test.sh
```

### Task 3: Codex skill and MCP integration

**Files:**
- Create: `tests/headroom/headroom_skill_mcp_test.sh`
- Create: `.agents/skills/headroom/SKILL.md`
- Create: `.agents/skills/headroom/agents/openai.yaml`
- Create: `.agents/skills/headroom/scripts/serve-mcp`
- Create: `.codex/config.toml`

- [ ] **Step 1: Write the failing skill and MCP contract test**

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
FAKE="$ROOT/tests/headroom/fixtures/fake-headroom"
SKILL="$ROOT/.agents/skills/headroom"

output="$(HEADROOM_BIN="$FAKE" "$SKILL/scripts/serve-mcp")"
[[ "$output" == $'off\nmcp\nserve' ]]

python3 /Users/kangchaewon/.codex/skills/.system/skill-creator/scripts/quick_validate.py "$SKILL"
grep -Fq '[mcp_servers.headroom]' "$ROOT/.codex/config.toml"
grep -Fq 'headroom_compress' "$ROOT/.codex/config.toml"
grep -Fq 'value: "headroom"' "$SKILL/agents/openai.yaml"

codex mcp list --json | python3 -c '
import json, sys
servers = json.load(sys.stdin)
assert any(server["name"] == "headroom" and server["enabled"] for server in servers)
'

echo 'headroom_skill_mcp_test: PASS'
```

Make the test executable.

- [ ] **Step 2: Run the test and verify it fails because the skill does not exist**

Run: `bash tests/headroom/headroom_skill_mcp_test.sh`

Expected: FAIL with `.agents/skills/headroom/scripts/serve-mcp: No such file or directory`.

- [ ] **Step 3: Initialize the repo-local skill**

Run:

```bash
python3 /Users/kangchaewon/.codex/skills/.system/skill-creator/scripts/init_skill.py headroom \
  --path .agents/skills \
  --resources scripts \
  --interface 'display_name=Headroom' \
  --interface 'short_description=Compress and restore large Codex context' \
  --interface 'default_prompt=Use $headroom to compress a large tool result before analysis.'
```

Expected: `.agents/skills/headroom` created with `SKILL.md`, `agents/openai.yaml`, and `scripts/`.

- [ ] **Step 4: Replace the generated skill instructions with the minimal Headroom workflow**

```markdown
---
name: headroom
description: Use when Codex must analyze large logs, JSON, search results, diffs, file contents, or other bulky tool output and the Headroom MCP tools are available.
---

# Headroom

Use Headroom to reduce bulky context while preserving a retrieval path to the original.

## Workflow

1. Skip compression for small, focused content or exact byte-sensitive work.
2. Call `headroom_compress` for large logs, JSON, search results, diffs, or repeated output before extended analysis.
3. Keep the returned hash with conclusions derived from compressed content.
4. Call `headroom_retrieve` with the hash and a narrow query when details are missing; request the full original only when necessary.
5. Call `headroom_stats` only when the user asks for compression metrics or when validating this integration.

Never claim that MCP use transparently proxies Codex desktop traffic. Automatic request compression is available only through `./scripts/codex-headroom`.
```

- [ ] **Step 5: Add the MCP dependency to `agents/openai.yaml`**

```yaml
interface:
  display_name: "Headroom"
  short_description: "Compress and restore large Codex context"
  default_prompt: "Use $headroom to compress a large tool result before analysis."

dependencies:
  tools:
    - type: "mcp"
      value: "headroom"
      description: "Local Headroom compression, retrieval, and stats tools"

policy:
  allow_implicit_invocation: true
```

- [ ] **Step 6: Implement the MCP launcher**

Create `.agents/skills/headroom/scripts/serve-mcp`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"
HEADROOM_BIN="${HEADROOM_BIN:-$ROOT/.headroom/venv/bin/headroom}"

if [[ ! -x "$HEADROOM_BIN" ]]; then
  echo 'Headroom is not installed. Run ./scripts/setup-headroom first.' >&2
  exit 1
fi

export HEADROOM_TELEMETRY=off
exec "$HEADROOM_BIN" mcp serve
```

Make the launcher executable.

- [ ] **Step 7: Register the project MCP server in `.codex/config.toml`**

```toml
[mcp_servers.headroom]
command = "bash"
args = ["-lc", "exec \"$(git rev-parse --show-toplevel)/.agents/skills/headroom/scripts/serve-mcp\""]
enabled = true
required = false
enabled_tools = ["headroom_compress", "headroom_retrieve", "headroom_stats"]
default_tools_approval_mode = "auto"
startup_timeout_sec = 30
tool_timeout_sec = 120

[mcp_servers.headroom.env]
HEADROOM_TELEMETRY = "off"
```

- [ ] **Step 8: Run the skill, launcher, and config tests**

Run: `bash -n .agents/skills/headroom/scripts/serve-mcp && bash tests/headroom/headroom_skill_mcp_test.sh`

Expected: validator reports the skill is valid and the test prints `headroom_skill_mcp_test: PASS`.

- [ ] **Step 9: Commit only Task 3 files**

```bash
git add .agents/skills/headroom tests/headroom/headroom_skill_mcp_test.sh
git add -f .codex/config.toml
git commit --only -m "feat: connect Headroom to Codex MCP" -- .agents/skills/headroom .codex/config.toml tests/headroom/headroom_skill_mcp_test.sh
```

### Task 4: Install Headroom and verify the live integration

**Files:**
- No tracked file changes expected.

- [ ] **Step 1: Run all offline tests before downloading dependencies**

Run:

```bash
bash tests/headroom/setup_headroom_test.sh
bash tests/headroom/codex_headroom_test.sh
bash tests/headroom/headroom_skill_mcp_test.sh
```

Expected: all three tests print `PASS`.

- [ ] **Step 2: Install Headroom into the project-local virtual environment**

Run: `./scripts/setup-headroom`

Expected: pip completes and the final line starts with `Headroom ` and ends with ` installed`.

- [ ] **Step 3: Verify the installed package and command surfaces without making provider API calls**

Run:

```bash
./.headroom/venv/bin/python -c 'import headroom; print(headroom.__version__)'
./.headroom/venv/bin/headroom mcp serve --help
./.headroom/venv/bin/headroom wrap --help
```

Expected: a version string and successful help output for both MCP serving and wrapping.

- [ ] **Step 4: Verify Codex sees only the intended Headroom MCP tools**

Run:

```bash
codex mcp get headroom --json | python3 -c '
import json, sys
server = json.load(sys.stdin)
assert server["enabled"]
assert server["enabled_tools"] == ["headroom_compress", "headroom_retrieve", "headroom_stats"]
print("headroom MCP config: PASS")
'
```

Expected: `headroom MCP config: PASS`.

- [ ] **Step 5: Re-run all offline tests against the installed layout**

Run:

```bash
bash tests/headroom/setup_headroom_test.sh
bash tests/headroom/codex_headroom_test.sh
bash tests/headroom/headroom_skill_mcp_test.sh
git diff --check
git status --short
```

Expected: all tests pass, `git diff --check` emits nothing, `.headroom/` is absent from status, and the user's pre-existing staged deletions and unrelated `.codex/skills` files remain untouched.
