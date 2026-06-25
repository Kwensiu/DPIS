# Migration And Shared Boundaries

Use this reference when a task crosses `legacy` and `modern`, or when a new
shared helper is proposed.

## Goal

Extract only the semantics that are truly common. Do not merge the hook stacks
just because both ultimately mutate similar runtime state.

## Migration Order

1. State what behavior is shared at the product level.
2. State what remains framework-specific at the hook level.
3. Move only the shared semantic rule into common code.
4. Keep flavor-owned registration, callback, and framework object handling in
   the flavor tree.
5. Add comments where the shared code intentionally stops and flavor-specific
   code resumes.

## Good Shared Targets

- capability-independent config interpretation
- app/package scoping rules
- mutation policies
- route planner decisions
- value translation between stored config and runtime target

## Bad Shared Targets

- fake common hook interfaces that erase framework differences
- giant adapters whose only job is to make Legacy look like Modern
- helper layers that hide which flavor owns install, callback, or fallback
- helpers that smuggle flavor lifecycle timing across the boundary

## Migration Questions

- Is the extracted helper independent of hook framework types?
- Would this abstraction remain useful if one flavor disappeared?
- Does the abstraction reduce real duplication, or does it only flatten visual
  differences?
- Can fallback behavior still be explained clearly after the refactor?
- Has the lifecycle mismatch between Legacy and Modern install timing been
  called out explicitly?
