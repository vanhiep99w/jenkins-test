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

## Bypass (Not Recommended)

```bash
git commit -m "message" --no-verify
```
