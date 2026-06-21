# Headroom Codex Integration

## Goal

Provide Headroom in this repository without changing a developer's global Codex configuration or application dependencies.

- Codex CLI gets automatic traffic compression through a project wrapper.
- Codex desktop and CLI get on-demand compression, retrieval, and stats through a project MCP server.
- A repo-local `headroom` skill tells Codex when to use the MCP tools.

## Scope and constraints

- Install Headroom into a gitignored project-local virtual environment.
- Do not proxy the desktop app: the app is not launched through `headroom wrap codex`.
- Disable Headroom telemetry by default.
- Do not store API keys, tokens, proxy logs, or virtual-environment files in git.
- Do not modify Gradle dependencies or existing application code.

## Components

### Bootstrap and CLI wrapper

Add `scripts/setup-headroom` to create `.headroom/venv` and install `headroom-ai[proxy,code]`. Track `.headroom/.gitignore` so the runtime remains excluded without overlapping the user's root `.gitignore` changes.

Add `scripts/codex-headroom` to resolve the repository root, require the local installation, set `HEADROOM_TELEMETRY=off`, and run `headroom wrap codex` with the caller's arguments. This is the automatic-compression path for Codex CLI.

### Desktop and CLI MCP integration

Add a project `.codex/config.toml` entry for a stdio MCP server. It starts a repo-local launcher that resolves the Git root and executes `headroom mcp serve` from `.headroom/venv`; it must fail with a clear bootstrap instruction when Headroom is missing.

The MCP server exposes only `headroom_compress`, `headroom_retrieve`, and `headroom_stats`. It runs with telemetry disabled. Because Codex loads project configuration only for trusted projects, users must trust this repository and start a new Codex session after setup.

### Codex skill

Create `.agents/skills/headroom` with the Codex skill initializer. Its instructions direct Codex to compress large logs, JSON, search results, diffs, and generated output before analysis; retain the retrieval hash; retrieve the original when compressed data is insufficient; and avoid compression for small or already focused content.

The skill is advisory. It does not claim that desktop-app traffic is transparently proxied.

## User workflow

1. Run `./scripts/setup-headroom` once from the repository root.
2. For automatic compression in a terminal, run `./scripts/codex-headroom` instead of `codex`.
3. For the Codex desktop app, trust the project, restart or start a new session, and use the Headroom MCP tools through the `headroom` skill when content is large.

## Verification and rollback

Verify the local package version, the MCP server handshake through the project config, a compression-and-retrieval smoke test, and CLI-wrapper argument forwarding. Do not make provider API calls during verification.

To roll back, remove the project skill, MCP configuration, launcher scripts, and `.headroom/` directory.
