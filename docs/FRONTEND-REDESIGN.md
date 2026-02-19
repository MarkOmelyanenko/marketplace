# Frontend Redesign: Audit, Design System & Implementation Plan

## 1. QUICK AUDIT

### 1.1 Routes & Screens

| Portal | Route | Screen | Purpose |
|--------|--------|--------|---------|
| **Partner Portal** | `/login` | Login | Partner ID input → localStorage, redirect to /offers |
| | `/offers` | OffersList | List partner's offers (table), Create Offer CTA |
| | `/offers/new` | CreateOffer | Form: title (140 char), description; create offer → redirect to detail |
| | `/offers/:id` | OfferDetails | View/edit offer, status pill, AI suggestions, Apply AI, Publish flow, Save |
| **Buyer Portal** | `/login` | Login | Buyer ID input → localStorage, redirect to /catalog |
| | `/catalog` | Catalog | Grid of published offers (cards), Buy button, My Orders |
| | `/orders` | Orders | Table of orders (id, offer, amount, status, created); polling for PENDING_PAYMENT |
| **Ops Dashboard** | `/login` | Login | Ops token input → localStorage, redirect to /search |
| | `/search` | Search | Filters: paymentId, offerId, partnerId; results table; Open → payment detail |
| | `/payments/:id` | PaymentDetails | Core fields, Retry webhook, Timeline list |

### 1.2 Important UI Components (Current)

- **Login forms** (3x): centered card, single input, submit button, error text.
- **Tables**: Offers list, Orders list, Payments search results — all ad-hoc styled (border-collapse, padding, header bg).
- **Status representation**: Inline styles with status→color mapping in OffersList, OfferDetails, Orders, PaymentDetails (DRAFT/yellow, PUBLISHED/green, etc.).
- **Forms**: CreateOffer, OfferDetails (title, description, char count, Save/Cancel/Create).
- **Cards**: Catalog offer cards (border, padding, shadow); OfferDetails info blocks; PaymentDetails core fields & timeline.
- **Buttons**: Primary (blue/green), secondary (gray), danger (red) — all inline.
- **Error/alert banners**: Red background, inline styles.
- **Loading**: Plain "Loading..." text only.
- **Empty states**: Short text only, no illustration or clear CTA in some places.

### 1.3 Current Styling Approach & Pain Points

- **Approach**: Plain CSS (Vite default `index.css` + `App.css`) + **heavy inline styles** in every page. No UI library (no MUI/Chakra/Tailwind).
- **Pain points**:
  - Inconsistent colors and spacing (each screen defines its own grays, blues, greens).
  - No design tokens; status colors duplicated and hard-coded in multiple files.
  - No reusable button/input/card components — copy-paste and drift.
  - `index.css` uses `prefers-color-scheme` and generic button styles that conflict with inline overrides.
  - No loading skeletons, no consistent empty state component.
  - Focus styles only from browser default (or index.css button outline); not consistently applied to inputs/links.
  - Three separate apps with no shared UI code; styling diverges (e.g. buyer has dark mode in index.css, partner/ops lighter).

**Decision**: Implement a **CSS-variable-based design system + small React UI primitives** in each app. No new dependencies (no Tailwind/shadcn) to avoid build/config changes and keep the stack minimal; design tokens + component CSS + React components give full control and consistency with least risk.

---

## 2. DESIGN SYSTEM SPEC

### 2.1 Color Tokens

| Token | Light | Dark (optional) | Usage |
|-------|--------|------------------|--------|
| `--color-bg` | `#f6f6f6` | `#1a1a1a` | Page background |
| `--color-surface` | `#ffffff` | `#252525` | Cards, panels, modals |
| `--color-border` | `#e0e0e0` | `#3a3a3a` | Borders, dividers |
| `--color-text` | `#1a1a1a` | `#f0f0f0` | Primary text |
| `--color-text-muted` | `#666666` | `#a0a0a0` | Secondary, hints |
| `--color-accent` | `#e95420` | `#ff6b3d` | Allegro-like warm orange (buttons, links, highlights) |
| `--color-accent-hover` | `#c94a1a` | `#ff8555` | Hover |
| `--color-success` | `#0d8050` | `#12a35c` | Success, PAID, PUBLISHED |
| `--color-warning` | `#b45f06` | `#e07810` | Warning, PENDING_PAYMENT |
| `--color-error` | `#c7162b` | `#e01f38` | Error, FAILED, validation |
| `--color-info` | `#0d6b9e` | `#1896d1` | Info, ENRICHING |

### 2.2 Typography

| Role | Size | Weight | Line height |
|------|------|--------|-------------|
| `--font-family` | — | — | `'Inter', system-ui, sans-serif` |
| h1 | `1.75rem` | 600 | 1.25 |
| h2 | `1.25rem` | 600 | 1.3 |
| h3 | `1.0625rem` | 600 | 1.35 |
| body | `1rem` | 400 | 1.5 |
| small | `0.875rem` | 400 | 1.45 |
| caption | `0.75rem` | 400 | 1.4 |

### 2.3 Spacing & Layout

- **Spacing scale**: `4, 8, 12, 16, 24, 32, 40, 48` (px) — use as `--space-1` … `--space-8`.
- **Content max-width**: `1200px`; **form max-width**: `640px`.
- **Radii**: `--radius-sm: 6px`, `--radius-md: 8px`, `--radius-lg: 12px`.
- **Shadows**: `--shadow-sm: 0 1px 3px rgba(0,0,0,0.08)`, `--shadow-md: 0 4px 12px rgba(0,0,0,0.08)`.

### 2.4 Component Guidelines

- **Button**: Primary (accent bg), Secondary (border + transparent), Danger (error bg). Size: default, small. Disabled: reduced opacity, no pointer. Focus: 2px outline offset.
- **Input / TextArea / Select**: Border `--color-border`, focus ring accent, label above, optional helper/error below.
- **StatusPill**: Maps status string to variant: `success` (PUBLISHED, PAID, CAPTURED), `warning` (PENDING_PAYMENT, DRAFT, PUBLISH_PENDING), `error` (FAILED, PUBLISH_FAILED, CANCELLED), `info` (ENRICHING), `neutral` (READY, etc.). Small, rounded, bold text.
- **Card**: Surface bg, radius-lg, shadow-md, padding 24px.
- **Modal/Dialog**: Overlay + surface, focus trap and Escape to close (optional in v1).
- **Toast**: Fixed position (e.g. top-right), surface + shadow, success/error variant; auto-dismiss or close button.
- **Table**: Sticky header (optional), compact padding (12px 16px), zebra optional; numbers right-aligned; clear empty state and loading skeleton.
- **Tabs**: Underline or pill; selected state with accent.
- **Skeleton**: Placeholder blocks with subtle pulse animation.
- **EmptyState**: Icon or illustration area, title, description, primary CTA button.

---

## 3. IMPLEMENTATION PLAN

### Step 1 – Design system foundation (each app)

- Add `src/design-system/tokens.css` (CSS variables only).
- Add `src/design-system/components.css` (classes for layout, typography, and component base styles).
- Add `src/design-system/` React components: `Button`, `Input`, `TextArea`, `Select`, `StatusPill`, `Card`, `Modal`, `Toast` (context + provider), `Table`, `Tabs`, `Skeleton`, `EmptyState`.
- Entry: import tokens + components.css in `main.jsx` and `index.css` (or App.css).

### Step 2 – Partner portal

- **Files to add**: `tokens.css`, `components.css`, `Button.jsx`, `Input.jsx`, `TextArea.jsx`, `Select.jsx`, `StatusPill.jsx`, `Card.jsx`, `Modal.jsx`, `Toast.jsx` (provider + hook), `Table.jsx`, `Tabs.jsx`, `Skeleton.jsx`, `EmptyState.jsx`; `AppShell.jsx`, `PageHeader.jsx`.
- **Files to change**: `index.css` (reset + tokens import), `App.css` (minimal, layout only), `App.jsx` (wrap routes in AppShell where needed, ToastProvider), `Login.jsx`, `OffersList.jsx`, `CreateOffer.jsx`, `OfferDetails.jsx` — replace inline styles with design-system components and classes. Add `publishOffer` to `api.js` and use it in OfferDetails.

### Step 3 – Buyer portal

- **Files to add**: Same design-system structure as partner (tokens, components.css, UI kit, AppShell, PageHeader).
- **Files to change**: `index.css`, `App.css`, `App.jsx`, `Login.jsx`, `Catalog.jsx`, `Orders.jsx` — use UI kit, skeletons, EmptyState.

### Step 4 – Ops dashboard

- **Files to add**: Same design-system structure + AppShell, PageHeader.
- **Files to change**: `index.css`, `App.css`, `App.jsx`, `Login.jsx`, `Search.jsx`, `PaymentDetails.jsx` — use UI kit, StatusPill for payment status, skeleton, EmptyState for “no results”.

### Step 5 – Quality

- Ensure all interactive elements have visible focus ring (e.g. `:focus-visible` with accent outline).
- Verify contrast (WCAG AA) for text and status pills.
- Smoke-test: all routes load, API calls unchanged (list/create/get/update/publish/orders/search/payments/timeline/retry).
- Remove duplicate styling: no inline style objects for layout/colors; use classes and components only.

---

## 4. FILE LIST (Summary)

**Partner portal**

- Add: `src/design-system/tokens.css`, `components.css`, `Button.jsx`, `Input.jsx`, `TextArea.jsx`, `Select.jsx`, `StatusPill.jsx`, `Card.jsx`, `Modal.jsx`, `Toast.jsx`, `ToastProvider.jsx`, `Table.jsx`, `Tabs.jsx`, `Skeleton.jsx`, `EmptyState.jsx`, `AppShell.jsx`, `PageHeader.jsx`; `index.js` (barrel).
- Change: `src/index.css`, `src/App.css`, `src/App.jsx`, `src/api.js`, `src/pages/Login.jsx`, `OffersList.jsx`, `CreateOffer.jsx`, `OfferDetails.jsx`.

**Buyer portal**

- Add: same `src/design-system/*` set + `AppShell.jsx`, `PageHeader.jsx`.
- Change: `src/index.css`, `src/App.css`, `src/App.jsx`, `src/pages/Login.jsx`, `Catalog.jsx`, `Orders.jsx`.

**Ops dashboard**

- Add: same `src/design-system/*` set + `AppShell.jsx`, `PageHeader.jsx`.
- Change: `src/index.css`, `src/App.css`, `src/App.jsx`, `src/pages/Login.jsx`, `Search.jsx`, `PaymentDetails.jsx`.

No backend or API contract changes.

---

## 5. QUALITY CHECKLIST (Post-Implementation)

- **Visual consistency**: All three portals use the same tokens (accent #e95420, surfaces, typography) and shared component patterns (Button, Input, Card, StatusPill, Table, etc.).
- **Focus states**: Buttons and form controls use `:focus-visible` with `--focus-ring` (2px accent outline); nav links use the same. No focus traps required for current flows.
- **Contrast**: Text on background uses `--color-text` on `--color-bg`/`--color-surface`; status pills use subtle backgrounds with semantic colors (success/warning/error) that meet readability.
- **Keyboard**: Forms are tabbable; primary actions (Submit, Create, Publish, Buy, Search) are buttons; navigation is via React Router Links (keyboard activatable).
- **API behavior**: No changes to `api.js` request/response shapes; `publishOffer` was added to partner `api.js` and used in OfferDetails (replacing inline fetch). All other endpoints and headers unchanged.
- **No duplicated styling**: Inline style objects removed from page components; layout uses design-system classes and component props. Minimal inline style remains only where layout (e.g. grid columns, gap) is dynamic.
- **Loading & empty states**: Skeleton loaders (table and card) and EmptyState components with clear CTAs are used across Partner (offers list, offer details), Buyer (catalog, orders), and Ops (search, payment details).
- **Error handling**: Alert component used for errors; retry actions where appropriate (e.g. Offers list, payment details).
