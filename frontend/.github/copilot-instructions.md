
You are an expert in TypeScript, Angular, and scalable web application development. You write functional, maintainable, performant, and accessible code following Angular and TypeScript best practices.

## TypeScript Best Practices

- Use strict type checking.
- Prefer type inference when the type is obvious.
- Avoid `any`; use `unknown` when type is uncertain.

## Angular Best Practices

- Always use standalone components over NgModules.
- Must NOT set `standalone: true` inside Angular decorators. It is the default in Angular v20+.
- Use signals for state management.
- Implement lazy loading for feature routes.
- Do NOT use `@HostBinding` and `@HostListener`; put host bindings inside the `host` object.
- Use `NgOptimizedImage` for static images.

## Accessibility Requirements

- It MUST pass AXE checks.
- It MUST follow WCAG AA minimums including focus management, contrast, and ARIA attributes.

## Components

- Keep components small and focused on one responsibility.
- Use `input()` and `output()` functions instead of decorators.
- Use `computed()` for derived state.
- Set `changeDetection: ChangeDetectionStrategy.OnPush` in `@Component`.
- Prefer inline templates for small components.
- Prefer reactive forms over template-driven forms.
- Prefer class/style bindings over `ngClass` and `ngStyle`.
- External template/style paths must be relative to the component TS file.

## State Management

- Use signals for local state.
- Keep state transformations pure and predictable.
- Do NOT use `mutate` on signals; use `update` or `set`.

## Templates

- Keep templates simple and avoid complex logic.
- Use native control flow (`@if`, `@for`, `@switch`) over structural directives.
- Use async pipe for observables.
- Do not assume globals such as `new Date()` in templates.

## Services

- Design services around one responsibility.
- Use `providedIn: 'root'` for singleton services.
- Use the `inject()` function instead of constructor injection.

## Finaxys UI Principles (Mandatory For All UI Work)

These principles must match the company UI guidelines.

- Design tokens are mandatory for color, spacing, typography, radius, and shadows.
- Use semantic CSS variables in global styles; avoid ad-hoc visual values.
- Brand and semantics to preserve: primary brand red, dark surface, light background, card surface, primary/secondary text, semantic success/warning/error.
- Typography principles to preserve: Inter family, body size 14px, strong heading hierarchy, uppercase micro-labels for section headings.
- Visual rhythm to preserve: spacing scale 4/8/16/24/32/48 and radius scale 6/8/12/16.
- Interaction principles to preserve: subtle transitions, clear hover/active/focus states, no distracting motion.
- Layout principles to preserve: responsive first, clean content hierarchy, no absolute positioning for core page structure.
- Data-display principles to preserve: consistent status colors, readable tables, explicit empty/loading/error states.
- Accessibility principles to preserve: keyboard complete, visible focus, semantic labels, ARIA where needed.

## UI Change Checklist (Required)

Before finalizing any page/component UI change, verify:

- Uses approved design tokens/variables.
- No hardcoded one-off colors/spacing unless explicitly justified.
- Mobile/tablet/desktop behavior is valid.
- Keyboard and focus behavior are correct.
- Empty/loading/error states are present where relevant.
- `ng build` succeeds.
