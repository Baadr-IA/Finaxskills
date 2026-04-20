---
applyTo: "src/**"
description: "Use when creating or modifying Angular pages, components, templates, or frontend styles. Enforce Finaxys company UI principles."
---

# Finaxys UI Guidelines For Angular Template Frontend

## Goal
Apply the company UI principles for any frontend change.

## Core Principles

- Preserve brand identity: use the Finaxys red as primary action color and keep neutral surfaces consistent.
- Preserve semantic clarity: success, warning, and error states must always use consistent semantic colors.
- Preserve readability: use a clear heading hierarchy and compact enterprise-friendly body text.
- Preserve spacing rhythm: use one consistent spacing scale and avoid random pixel values.
- Preserve accessibility: keyboard navigation, focus visibility, and WCAG AA contrast are mandatory.

## Required Token System

All tokens are declared in `src/styles.css` as CSS custom properties and documented in `src/tokens/FINAXYS_DESIGN_GUIDE.md`. The raw token values are in `src/tokens/design-tokens.json`.

Use CSS variables for all new UI work — never hardcode hex values or pixel values:

- Colors: `--finaxys-brand-red`, `--finaxys-surface-dark`, `--finaxys-surface-background`, `--finaxys-surface-card`, `--finaxys-text-primary`, `--finaxys-text-secondary`, `--finaxys-border`, `--finaxys-success`, `--finaxys-warning`, `--finaxys-error`.
- Typography: `--font-family-base`, `--font-size-h1/h2/h3/h4/base/label`, `--font-weight-bold/semibold/medium/normal`, `--line-height-heading/body`, `--letter-spacing-tight/wide`.
- Radius: `--radius-sm` (6px), `--radius-md` (8px), `--radius-lg` (12px), `--radius-xl` (16px).
- Spacing: `--spacing-xs` (4px) through `--spacing-xxl` (48px).
- Shadows: `--shadow-sm` through `--shadow-xl`.

If a token is missing, add it to `src/styles.css` first, then consume it in components.

## Available Utility Classes

The following global utility classes are declared in `src/styles.css`:

- **Badges**: `.badge-success`, `.badge-warning`, `.badge-error`, `.badge-finaxys`
- **Button**: `.btn-finaxys` (primary brand button with hover/focus states)
- **Card**: `.card-finaxys` (white card with border, shadow, hover lift)
- **Section header**: `.section-header` (uppercase micro-label, 11px)
- **Text colors**: `.text-success`, `.text-warning`, `.text-error`, `.text-finaxys`
- **Backgrounds**: `.bg-finaxys-dark`, `.bg-finaxys-light`, `.bg-finaxys-red`
- **Status dots**: `.status-dot` + `.status-dot-success/warning/error/info`

Prefer these classes before writing custom one-off CSS.

## Component and Page Rules

- Prefer reusable Angular components over page-specific one-off markup.
- Keep components focused and composable.
- Build responsive layouts by default (mobile first).
- Avoid absolute positioning for primary page structure.
- Always provide empty/loading/error states for data-heavy UI.
- Use consistent status chips, labels, and indicators across screens.

## Styling Rules

- Prefer semantic class names and CSS variables over hardcoded values.
- Avoid introducing a second visual language.
- Do not add decorative effects that conflict with enterprise style.
- Keep animations subtle and functional.

## Accessibility Rules

- Every interactive element must have visible focus.
- Forms must have explicit labels and useful validation feedback.
- Icon-only controls must include accessible names.
- Dialogs and menus must support keyboard interaction and escape behavior.

## Stack Boundary

- Do not introduce React or React UI libraries into this Angular project.
- Recreate the principles with Angular templates, Angular components, and project CSS conventions.

## Done Criteria

A UI change is complete only when:

- It follows tokenized visual rules.
- It is responsive on mobile and desktop.
- It preserves accessibility requirements.
- It compiles successfully with ng build.
