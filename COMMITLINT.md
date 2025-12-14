# Commitlint Setup

This project uses [commitlint](https://commitlint.js.org/) to enforce conventional commit messages.

## Setup (One-time)

```bash
npm install
```

This installs commitlint and sets up the git hook automatically.

## Commit Message Format

```
type(scope): description

[optional body]

[optional footer]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `style` | Code style (formatting, semicolons, etc) |
| `refactor` | Code refactoring |
| `test` | Adding or updating tests |
| `chore` | Maintenance tasks |
| `ci` | CI/CD changes |
| `perf` | Performance improvements |
| `revert` | Revert previous commit |
| `build` | Build system changes |

### Examples

```bash
# Good commits
git commit -m "feat: add user authentication"
git commit -m "fix: resolve memory leak in cache"
git commit -m "docs: update API documentation"
git commit -m "ci: add commitlint to pipeline"

# Bad commits (will be rejected)
git commit -m "update stuff"
git commit -m "fixed bug"
git commit -m "WIP"
```

## Test Commit Message Locally

```bash
echo "feat: my commit message" | npx commitlint
```

## CI Control Flags

You can control CI/CD pipeline behavior by adding flags to your commit message:

### Skip Flags

| Flag | Effect |
|------|--------|
| `[skip ci]` or `[ci skip]` | Skip entire pipeline |
| `[skip tests]` | Skip unit/integration tests |
| `[skip review]` | Skip AI code review |
| `[skip deploy]` | Skip deployment stages |
| `[skip docker]` | Skip Docker build/push |
| `[skip lint]` | Skip commitlint validation |
| `[skip release]` | Skip semantic release |

### Action Flags

| Flag | Effect |
|------|--------|
| `[only build]` | Only run build, skip tests/deploy |
| `[wip]` | Work in progress: only lint + build |
| `[hotfix]` | Fast-track: skip tests, deploy immediately |
| `[full ci]` | Run ALL stages including disabled ones |
| `[rollback]` | Rollback to previous deployment |

### Deploy Flags

| Flag | Effect |
|------|--------|
| `[deploy dev]` | Force deploy to dev environment |
| `[deploy uat]` | Force deploy to UAT environment |
| `[deploy prod]` | Force deploy to production (requires approval) |

### Examples

```bash
# Skip entire CI
git commit -m "docs: update readme [skip ci]"

# Skip tests only
git commit -m "fix: quick hotfix [skip tests]"

# Skip AI review
git commit -m "chore: minor update [skip review]"

# Only build, no tests or deploy
git commit -m "feat: wip feature [only build]"

# Work in progress - minimal CI
git commit -m "feat: incomplete feature [wip]"

# Hotfix - fast deploy
git commit -m "fix: critical bug [hotfix]"

# Force deploy to specific environment
git commit -m "feat: new feature [deploy dev]"
git commit -m "fix: bug fix [deploy uat]"

# Rollback previous deployment
git commit -m "revert: rollback bad deploy [rollback]"

# Combine flags
git commit -m "fix: urgent fix [skip tests] [skip review]"

# Run full CI pipeline
git commit -m "feat: important feature [full ci]"
```

## Bypass Commitlint (Not Recommended)

```bash
git commit -m "message" --no-verify
```
