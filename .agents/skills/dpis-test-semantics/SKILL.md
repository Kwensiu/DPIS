---
name: dpis-test-semantics
description: Review, write, or clean DPIS tests so they pin a product rule instead of a source line. Use when adding tests, reviewing tests, cleaning source smoke tests, or when a test fails only because a method name, literal, or file spelling changed.
---

# DPIS test semantics

Each test must name the product rule it pins, in one sentence. If an implementation rename or
rewrite keeps that rule, the test still passes. Delete or rewrite a test that fails only because a
line, method name, literal, or old spelling changed. Do not add a test that has no rule.

Call the behavior when it can be called. A source read is allowed only when the contract is wiring
that cannot be called, and only for that contract. One wiring contract keeps one anchor that cannot
be called. A set of method names, local variables, or string literals is still a source pin. A
comment does not change that.

## Review

For each new or touched test, write the rule in one sentence. If you cannot, delete the test. Before
deleting a test that already names a product rule, pin that same rule with a call. If it cannot be
called, say why. Do not only delete the test. Do not add more source-text checks to make a line-pin
suite green.

When replacing or removing an existing rule test, use this order:

1. Identify the product rule the test names.
2. Add an equivalent callable behavior test, or document why the rule cannot be called and keep one
   minimal wiring anchor.
3. Run the replacement test successfully.
4. Only then remove or reduce the old source-based assertions.

Do not delete a rule test in the same step that merely splits or removes its source assertions. A
passing source-smoke cleanup is not evidence that the product rule is still covered.
