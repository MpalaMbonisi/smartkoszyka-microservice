// commitlint.config.js
// Enforces Conventional Commits: https://www.conventionalcommits.org
//
// Allowed types:
//   feat     – a new feature
//   fix      – a bug fix
//   docs     – documentation only changes
//   style    – formatting, missing semi-colons, etc (no logic change)
//   refactor – code change that neither fixes a bug nor adds a feature
//   test     – adding missing tests or correcting existing tests
//   build    – changes to build system or external dependencies (Maven, Docker)
//   ci       – changes to CI configuration files (.github/workflows)
//   chore    – other changes that don't modify src or test files
//   revert   – reverts a previous commit
//
// Example valid messages:
//   feat(auth-service): add JWT refresh token support
//   fix(common-lib): defensive copy in ErrorResponse.getMessage()
//   ci: add commit-lint job to CI workflow

module.exports = {
  extends: ["@commitlint/config-conventional"],
  rules: {
    // Enforce a scope (module name) — warn only so it's not blocking
    "scope-empty": [1, "never"],
    // Keep subjects concise
    "header-max-length": [2, "always", 150],
    // Types list (same as config-conventional but explicit for docs)
    "type-enum": [
      2,
      "always",
      [
        "feat",
        "fix",
        "docs",
        "style",
        "refactor",
        "test",
        "build",
        "ci",
        "chore",
        "revert",
      ],
    ],
  },
};
