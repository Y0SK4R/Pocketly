# POCKETLY: MASTER BUILD PROMPT (v2)

Attach the three reference images to this same message in AI Studio (the nine-screen sheet, the Investments + Growth calculator pair, and the Home screen). Then paste everything below. If a small detail is hard to read in the nine-screen sheet, upload those screens individually as well.

---

## 0. ROLE AND MISSION

You are a senior product engineer, motion designer and finance-domain specialist working as one person. Build **Pocketly**, a private, offline personal finance tracker for a single user, as a mobile-first web app that will be packaged into an Android APK with Capacitor and used on a Samsung Galaxy S24 Ultra.

The attached images are the visual source of truth for layout, hierarchy, palette and component style. Match them closely. Do not copy any other app's name, logo, icons or mascot. Everything branded (name, mascot, icon set usage, copy) is original to Pocketly.

Quality bar: it should feel like a polished, award-level app-store product made by a small studio with a point of view. It must not feel like a generated template. When in doubt between "fast and generic" and "slower and considered", choose considered. Token and time budget are not a constraint, so do the work fully. Do not stub, do not leave TODOs, do not hand-wave.

Work autonomously. Do not stop to ask for approval between stages. When finished, give a short report of what is complete, what is partial, and anything you could not verify.

---

## 0.5 REFERENCE IMAGES: WHAT EACH ONE IS AND HOW TO USE IT

You are given three images. Study them closely before designing. Use them for layout, hierarchy, proportions, component shapes, density and color. Treat the written spec in this prompt as the source of truth for behavior and for anything the images disagree on.

1. **Nine-screen sheet** (a grid of phone screens): Accounts, Overview, Budgets, Add transaction (bottom sheet over a dimmed Home), Scheduled, Activity, Profile in light mode, and Profile in dark mode.
2. **Investments + Growth calculator pair:** two screens side by side.
3. **Home screen:** a single large screen. This is the most precise reference for spacing, card proportions, ring size, bar chart and transaction rows. Use it as the quality benchmark for the other screens.

**Where the images and this spec differ, follow this spec:**
- **Bottom navigation.** The images show different tab sets (Home, Accounts, Overview, Budgets, Profile on most screens, and Home, Accounts, Investments, Budgets, Profile on the investing screens). The final navigation is: **Home, Accounts, Budgets, Invest, Profile.** Overview is not a tab; it is a "Cash flow | Overview" switch inside Home. Label the fourth tab "Invest".
- **Numbers and sample text.** All amounts in the images are demo content. Several are mathematically inconsistent (for example the calculator's $18,946 from $13,000 put in). Do not hardcode them. Compute everything from the data and the formulas in section 5.
- **Add button.** Some screens show the "+" at the top right or inline. Use one floating mint "+" above the bottom bar, as described in section 8.
- **Cosmetic polish.** The images are quick mockups. Where spacing, alignment, icon style or chart quality in them looks rough, improve it, keeping the same structure.

**Not shown in any image (design these yourself from the descriptions in this prompt, matching the same style):**
- The lock screen and its sleepy coin-pouch mascot (section 8.1)
- Dark mode for every screen except Profile (use the dark Profile as the reference for dark surfaces and contrast)
- Account detail, Holding detail, transaction search, onboarding, all empty states, and all forms and sheets other than Add transaction

**Do not reproduce any branding, logo, wordmark, mascot or illustration style from third-party apps.** Pocketly's name, mascot and icons are original.

After building each screen, compare it against its reference image, list the differences you find (spacing, hierarchy, type size, alignment, color, chart shape), and fix them before moving on.

---

## 1. PRODUCT INTENT

- **Who:** one person tracking their own money on their own phone. Privacy matters, so nothing leaves the device.
- **Primary job:** log a purchase in about 3 taps, then instantly see what it did to today, this month and the budget. Everything else (charts, budgets, investing) supports that loop.
- **Secondary job:** understand where money goes and watch yield-bearing savings grow, including "what if I put in X per month" projections.
- **Feeling:** calm, clear, quietly rewarding. Money apps cause anxiety, so this one should feel like exhaling. Rewards are small and earned (staying under budget, hitting a savings milestone). Over-budget is stated plainly and never scolds.
- **Anti-goals (a failure even if it technically works):** cluttered dashboards, gamified confetti spam, dark patterns, fake data presented as real, anything requiring an account, a server, or a network, jank in animation.

---

## 2. TARGET DEVICE AND PLATFORM RULES

- Samsung Galaxy S24 Ultra: about 412 x 891 CSS px viewport, 19.5:9, 120Hz AMOLED, centered punch-hole camera, gesture navigation. Portrait only.
- Design for one-handed use. Primary actions and navigation sit in the lower half. The top of the screen holds read-only content (big numbers, charts). Touch targets are at least 48 x 48 px.
- Respect safe areas using `env(safe-area-inset-top/bottom)`. Content must clear the status bar, the camera cutout and the gesture pill. No horizontal page scroll anywhere.
- Set `<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">`, disable text-size inflation, disable pull-to-refresh and overscroll glow on the app shell, disable tap highlight flash, and use `touch-action: manipulation`.
- Must also render correctly on smaller Android phones (360 px wide) with no clipped text or overlapping controls. Test mentally at 360, 412 and 430 px.

---

## 3. TECH STACK AND ARCHITECTURE

**Stack:** React 18 + TypeScript (strict) + Vite, Tailwind CSS driven by CSS variables, Framer Motion (`motion`), Zustand for UI state, Dexie (IndexedDB) for persistence, Lucide for all icons, Vitest for unit tests of the finance math. Hand-built SVG charts (no chart library) so every animation is controlled.

**Fonts must be bundled locally** (the app is offline): use `@fontsource-variable/*` packages. No Google Fonts links, no CDNs, no runtime network calls of any kind. No API keys.

**Routing:** hash-based routing (`#/home`), relative asset paths (`base: './'` in Vite), so it loads from `file`/Capacitor webview without a server.

**Folder structure (use exactly this idea):**
```
src/
  app/            shell, router, providers, theme, lock gate
  design/         tokens.css, motion.ts (springs, durations, variants), primitives (Card, Pill, Button, Sheet, NumberRoll, Skeleton, EmptyState)
  features/
    lock/  home/  overview/  accounts/  budgets/  invest/  calculator/
    transactions/  scheduled/  activity/  profile/
  charts/         BarChart, LineChart, Donut, Rings, LiquidGauge, Sparkline
  data/           db.ts (Dexie schema + migrations), repositories, seed.ts
  domain/         money.ts, periods.ts, budgets.ts, yield.ts, projections.ts, scheduling.ts (pure functions, fully typed, fully tested)
  native/         biometric.ts, haptics.ts, notifications.ts, storage-export.ts (interfaces + web fallbacks)
  mascot/         Mascot.tsx (original SVG character + states)
```

**Native integrations** are wrapped behind small interfaces with working web fallbacks, so the app runs in a browser preview and upgrades when running inside Capacitor:
- Biometrics: PIN-only in the browser. In Capacitor, use a biometric auth plugin. PIN is always the fallback.
- Haptics: `navigator.vibrate` fallback, Capacitor Haptics when available.
- Notifications: in-app Activity feed always. Local notifications through Capacitor when available (scheduled payments due, budget at 90% and 100%).
- Export: download CSV/JSON in browser, Capacitor Filesystem + Share when available.

**PIN security:** never store the PIN in plain text. Store a salted hash (PBKDF2 or SHA-256 with a per-install random salt via Web Crypto). Lock the app after the app is backgrounded for more than 30 seconds (setting: immediately / 30s / 5 min). After 5 wrong attempts, add an increasing cooldown (30s, 1m, 5m).

---

## 4. DATA MODEL (TypeScript, implement exactly or better)

All money is stored as **integer minor units (cents)**. Never store floats for money. Dates are stored as ISO strings in local time with an explicit timezone-safe day key (`YYYY-MM-DD`).

```ts
type ID = string; // nanoid
type Currency = string; // ISO 4217, default "USD"

interface Account {
  id: ID; name: string; kind: 'wallet'|'checking'|'credit'|'savings'|'cash';
  icon: string; color: string; openingBalance: number; currency: Currency;
  archivedAt?: string; createdAt: string; sortOrder: number;
}
interface Category {
  id: ID; name: string; icon: string; color: string;
  kind: 'expense'|'income'; archivedAt?: string; sortOrder: number;
}
interface Transaction {
  id: ID; kind: 'expense'|'income'|'transfer'; amount: number; // always positive; sign comes from kind
  accountId: ID; toAccountId?: ID; // transfers only
  categoryId?: ID; note?: string; merchant?: string;
  date: string; dayKey: string; scheduledId?: ID; createdAt: string; updatedAt: string;
}
interface Budget {
  id: ID; categoryId: ID | 'all'; amount: number;
  period: 'monthly'; // keep room for 'weekly' later
  rollover: false; createdAt: string;
}
interface ScheduledItem {
  id: ID; name: string; kind: 'expense'|'income'; amount: number;
  accountId: ID; categoryId?: ID;
  repeat: 'weekly'|'monthly'|'yearly'|'once'; nextDue: string; paused: boolean;
  autoPost: boolean; // if true, create the transaction when due; else just remind
}
interface Holding {
  id: ID; name: string; type: 'savings'|'tbill'|'cd'|'bond'|'index'|'other';
  mode: 'yield'|'manual';
  // yield mode:
  apy?: number; // e.g. 0.045
  startDate?: string; maturityDate?: string; // maturity optional (CDs, T-bills)
  contributions: { id: ID; date: string; amount: number }[]; // positive = deposit, negative = withdrawal
  // manual mode:
  valueHistory?: { date: string; value: number }[]; // user-entered current values
  icon: string; color: string; archivedAt?: string;
}
interface ActivityItem { id: ID; kind: 'budget90'|'budget100'|'scheduledPaid'|'scheduledDue'|'milestone'; title: string; body: string; createdAt: string; readAt?: string; refId?: ID }
interface Settings {
  currency: Currency; theme: 'light'|'dark'|'system'; pinHash?: string; pinSalt?: string;
  biometric: boolean; lockAfter: 'immediate'|'30s'|'5m'; onboarded: boolean; demoLoaded: boolean;
}
```

Use Dexie with a versioned schema and write the migration scaffold, so future changes do not wipe data.

---

## 5. DOMAIN RULES AND MATH (put in `/domain`, pure functions, unit-tested)

**Balances**
- Account balance = openingBalance + income - expenses + transfers in - transfers out. Credit accounts can be negative and display as negative.
- Transfers move money between accounts and are **never counted as income or expense** in cash flow, overview, budgets or category totals.
- Archived accounts keep history but are hidden from pickers and from new totals except "Total wealth" history.

**Periods**
- "This month" = the current calendar month in the device's local timezone, month start inclusive, next month start exclusive.
- Period tabs on Home/Overview/Budgets show previous month, selected month, and "This month". Swiping or tapping changes the period.
- "vs previous period" compares equal-length windows. If the previous period had zero spend, show "New" instead of a percentage. Never divide by zero and never print NaN or Infinity.

**Budgets**
- Spent = sum of expenses in the category for the period.
- Over budget when spent > budget: bar turns coral and fills 100%, and text shows "$609 of $220". Never let a bar overflow its track.
- "Ideal pace" line = budget x (dayOfPeriod / daysInPeriod).
- Gauge ("Well done, you saved $X of $Y budget") uses the **All** budget: saved = budget - spent. If saved < 0, the copy changes to "You're $X over budget" and the liquid turns coral. Never celebrate when over.
- Budget at 90% and 100% trigger one Activity item each, once per category per period.

**Yield holdings (mode = yield)**
- APY already includes compounding. Value of one deposit `c` made on date `d` at time `t`: `c * (1 + apy) ^ ((t - d) / 365 days)`. Holding value = sum over all contributions. Withdrawals are negative contributions on their date.
- Growth = current value - sum of contributions. Show all-time growth as an amount and percentage of money put in (guard against zero).
- Estimated yearly yield = sum of `value * apy` for active yield holdings.
- If a maturity date has passed, stop accrual at maturity and show a "Matured" pill.
- Chart: for each day in range, "Value" is the accrued total, and "Money put in" is the cumulative contributions step line.

**Manual holdings (mode = manual)**
- Value is the latest entry in `valueHistory`. Growth compares with total contributions. Updating the value adds a history point, which feeds the chart.
- Label manual items "Updated {relative date}" so the number never pretends to be live.

**Growth calculator**
- Inputs: starting amount P, monthly contribution PMT, yearly yield (APY) a, years Y, compounding (monthly | yearly).
- Monthly compounding: monthly rate i = (1 + a)^(1/12) - 1; contributions at end of each month; value after m months: `FV = P(1+i)^m + PMT * ((1+i)^m - 1) / i`. If i = 0 use `P + PMT*m`.
- Yearly compounding: simulate month by month. Interest accrues as simple interest (`balance * a / 12`) into a pending pot, and the pot is added to the balance at every 12th month. Contributions enter the balance at month end.
- Outputs: final value, total put in (P + PMT x months), total earned, a per-month curve for the chart, and the value at the scrubber's position.
- **Reference test:** P = 5,000, PMT = 100, a = 4.5%, Y = 10, monthly compounding must give about $22,813 total, $17,000 put in, about $5,813 earned (within a few dollars). The numbers in the mockup image ($18,946, $13,000) are illustrative and not consistent, so compute real values and do not hardcode them.
- Always label results "Estimate, not financial advice".

**Scheduled items**
- When the app opens, process missed due dates (loop until caught up). `autoPost` items create transactions dated on their due date, not "today". Monthly items on the 29th-31st clamp to the last day of shorter months.
- Never double-post: guard with `scheduledId + dayKey` uniqueness.

**Unit tests (Vitest), at minimum:** balances with transfers, period boundaries (month ends, DST changes), budget pace, over-budget gauge copy, deposit/withdrawal accrual, maturity stop, calculator reference case, zero-rate case, zero-previous-period case, scheduled clamp (Jan 31 -> Feb 28/29), idempotent posting.

---

## 6. DESIGN SYSTEM

Follow the mockups. The mockups pin the direction (soft lavender-grey canvas, white rounded cards, mint for positive, coral for negative), so keep it. The freedom is in execution quality and in a few deliberate choices below. Spend boldness in one place and keep everything else quiet and disciplined.

**The one memorable idea: liquid.** Mint "liquid" is Pocketly's signature. It appears in the budget gauge (a rising wave), in the lock-screen mascot's coin pouch, and in the Add Transaction save confirmation (a short mint ripple). It does not appear anywhere else. Every other element is calm and flat so the liquid reads as special.

**Color tokens (define as CSS variables, light / dark):**
- `--bg`: `#F3F2F8` / `#000000`
- `--surface`: `#FFFFFF` / `#121217`
- `--surface-2` (inner rows, inputs): `#F7F6FB` / `#1A1A22`
- `--ink`: `#16161D` / `#F4F4F8`
- `--ink-muted`: `#6B6B7B` / `#9A9AAB`
- `--line`: `#E7E6EF` / `#25252F`
- `--mint`: `#19C48B` / `#2BE0A4`
- `--mint-soft`: `#DDF7EE` / `#0F2E25`
- `--coral`: `#FF6B6B` / `#FF8585`
- `--coral-soft`: `#FFE6E6` / `#33191C`
- `--amber`: `#F5A524` (due-soon pills)
- Lock screen only: cream `#FBF0DF` with a peach wash `#F8CE8C`. Dark lock variant uses `#14110C` with a warm brown wash.
- Category palette (pastel fill + saturated glyph): orange `#FFA31A`, pink `#F2619C`, sky `#3EA6F0`, lime `#78C142`, purple `#8C56E0`, teal `#4FC3A9`, clay `#B65A3F`, slate `#6B7A8F`. Every category icon sits in a circle or rounded square tinted with its soft color.
- Contrast: all text meets WCAG AA in both themes. Do not rely on color alone for positive/negative. Pair with +/- signs and arrows.

**Typography.** Use two clearly distinct families, both bundled:
- **Bricolage Grotesque** (variable) for screen titles and every large number. It has character at big sizes. Use weight 700-800 for hero numbers, 600 for titles.
- **Figtree** (variable) for UI text, labels, lists and buttons.
- Use tabular figures (`font-variant-numeric: tabular-nums`) for all amounts so columns and count-up animations do not jitter. If a face lacks them, set amounts in Figtree with tabular nums.
- Scale (px): 12 caption, 14 label/body-small, 16 body, 18 row title, 22 section title, 28 screen title, 40 hero number, 56 keypad amount. Line-height 1.2 for numbers, 1.4 for text. Sentence case everywhere. No ALL CAPS labels, no tracked-out eyebrow labels above headings, no arrows appended to button text.

**Shape and elevation (hierarchy, not one radius for everything):**
- Hero cards 28px radius. Inner rows and inputs 18px. Chips and tabs 14px. Pills and buttons fully rounded. Icon tiles 14px.
- Elevation: cards use a very soft two-layer shadow (e.g. `0 1px 2px rgba(22,22,29,.04), 0 8px 24px rgba(22,22,29,.06)`). In dark mode, drop shadows and use a 1px `--line` border instead.
- No decorative gradient washes except the subtle mint wash behind the Accounts hero and the lock screen peach wash.

**Spacing scale:** 4, 8, 12, 16, 20, 24, 32. Page gutter 16px. Card inner padding 20px. Gap between cards 12px. Use only these values.

**Icons:** Lucide only. One stroke width (1.75). Map every default category to a Lucide icon (Utensils, ShoppingBag, Car, Home, ShoppingBasket, Zap, HeartPulse, Plane, Ticket, Landmark, PiggyBank, TrendingUp, Banknote, etc.). Never use emoji as icons. Emoji may appear only inside user-entered notes.

**Charts (all hand-built SVG):**
- Bars: rounded top corners (radius 6), paired income (mint) and expense (coral) per month, light horizontal gridlines at 4 steps, y-axis labels in muted caption text, current month emphasized, tap a month to see a tooltip with both values.
- Lines: smooth monotone curves (not wiggly overshoot), gradient fill from 18% to 0% opacity, a dot on the latest point, long-press or drag scrub with a vertical guide and floating value label.
- Donut: gap of 2deg between slices, round icon bubbles outside the ring connected to their slice with percent labels, no overlap (if labels would collide, nudge them radially or collapse small slices into "Other"). Tapping a slice highlights it and shows its amount in the center.
- Rings: 56px circular progress rings with category icon, arc fills to the category's spent/budget (or share of spending if no budget).
- Empty charts show a designed empty state, never a blank axis.

---

## 7. MOTION SYSTEM

Motion answers the user's actions and confirms what changed. Do not add scattered entrance effects. There is one orchestrated entrance per screen, and everything else is a response to touch.

**Rules:** animate only `transform` and `opacity` (plus SVG stroke-dashoffset and path morphs on charts) to hold 120Hz. Never animate width/height/top/left/box-shadow. Use `will-change` sparingly. Respect `prefers-reduced-motion` (replace movement with a short fade and skip loops). Provide a Settings toggle "Reduce motion" that follows the same path.

**Tokens (in `design/motion.ts`):**
- `spring.tap`: stiffness 520, damping 32 (button/key press)
- `spring.sheet`: stiffness 380, damping 36 (bottom sheets, drag-snap)
- `spring.soft`: stiffness 220, damping 26 (cards, tab pill)
- `ease.out`: cubic-bezier(.22, 1, .36, 1), `ease.inOut`: cubic-bezier(.65, 0, .35, 1)
- durations: 120 (micro), 220 (standard), 360 (reveal), 600 (hero chart draw), 900 (count-up)

**Choreography:**
- **Screen entrance (one per screen):** hero number counts up (900ms, ease-out) while the first chart draws (bars grow from baseline with 25ms stagger, lines draw via stroke-dashoffset, donut sweeps clockwise, rings fill). Lists fade in with a 30ms stagger capped at the first 8 rows. Then nothing else moves until the user acts.
- **Tab change:** content cross-fades (220ms) with a 12px vertical offset. The active mint pill in the bottom bar glides to the new tab with `spring.soft`.
- **Press feedback:** every tappable scales to 0.96 with `spring.tap`, plus a light haptic tick on keypad keys and primary buttons.
- **Lock screen:** PIN dots fill with a small scale-pop. Wrong PIN: dots shake horizontally (3 oscillations, 320ms) with an error haptic, then clear. Correct PIN: the mascot perks up, the screen lifts off upward (translateY -100%, 420ms) revealing Home beneath. Fingerprint success does the same.
- **Add Transaction sheet:** opens with `spring.sheet`, draggable by its handle with velocity-based snap (closed / full), backdrop dims proportionally to sheet position. Typing the amount: digits slide in from below with a short vertical spring, the decimal point is handled smoothly, max 9 digits. On Save: a mint ripple expands from the Save button (the one allowed liquid moment outside budgets), the sheet closes, the new row slides into the Home list at the top of "Today" while the day total and the cash flow chip tick to their new values.
- **Budget gauge:** the liquid level animates to the percent saved with `spring.soft`, and two phase-shifted wave paths drift horizontally in a continuous 6s loop (transform translateX only, on an oversized SVG path). A faint specular highlight sits on the glass. The loop pauses when the screen is not visible (use `IntersectionObserver` and the Page Visibility API).
- **Mascot:** idle breathing (scale 1 -> 1.03, 3.2s loop), occasional slow blink every 4-7s (random). States: `sleepy` (lock idle), `perk` (PIN correct: eyes open, ears up, small hop), `proud` (shown on Budgets when under budget at month-end pace, a tiny nod, once per session), `oops` (wrong PIN: squints and shakes slightly).
- **Lists:** swipe a transaction left for Delete (coral) and right for Edit, with the row following the finger. Deleting shows an "Undo" snackbar for 5 seconds.
- **Skeletons** (shimmering `--surface-2` blocks matching the final layout) replace spinners everywhere. Data is local, so most loads are instant. Keep a minimum of 0 ms (do not fake delay).
- Charts update smoothly when data changes (tween path `d`, interpolate bar heights) rather than redrawing.

---

## 8. SCREENS (build all; every control must work)

Bottom navigation (floating rounded bar, 5 tabs): **Home, Accounts, Budgets, Invest, Profile.** The active tab is a mint-soft pill with a mint icon and label. A floating mint circular "+" (56px) sits above the bar on the right on Home, Accounts, Budgets, Invest (opens Add Transaction on Home/Accounts/Budgets, Add Investment on Invest) and Scheduled.

### 8.1 Lock
- Warm cream background with a peach wash at the top. Text "Enter your PIN" in the upper third. Four dots. A 3x4 keypad in the lower half with soft rounded-square keys: 1-9, bottom row backspace, 0, fingerprint. The mascot sleeps in the bottom-right corner above the gesture area.
- First launch (no PIN set): the same screen reads "Create a PIN", asks twice ("Confirm your PIN"), then offers fingerprint ("Use fingerprint to unlock?"). Provide a "Skip" for a PIN-less mode that goes straight to Home.
- Fingerprint prompt appears automatically on open when enabled. The fingerprint key retries it.

### 8.2 Home
- Top bar: "Pocketly" (Bricolage, 600) left, bell (with unread dot) and search on the right. Search opens a full-screen transaction search (merchant, note, category, amount) with filter chips.
- Row of 5 category rings (horizontally scrollable if more than 5), "All" first. Tapping a ring filters the list and chart to that category.
- Hero card: cash flow number (40px) with caption "Cash flow this month", mint chip "+$2,600", coral chip "-$432". A **"Cash flow | Overview"** segmented switch sits at the top of the card. Cash flow mode shows the 9-month grouped bars. Overview mode shows the expense donut view (section 8.3) with period tabs.
- "Scheduled" summary card ("4 transactions, -$172.47") opens Scheduled.
- Transaction list grouped by day with daily totals ("Today -$111.88"). Row: category icon tile, category name (row title), "Account name, merchant" (muted), amount (coral for expense, mint for income, neutral ink for transfers). Tap opens an edit sheet, swipe for edit/delete.
- Empty state (no data): mascot sleeping, "Nothing logged yet" and a mint button "Add your first transaction" plus a text button "Load demo data".

### 8.3 Overview (inside Home)
- Tabs: previous month, selected month, "This month".
- Expenses card: total, delta vs previous period with a colored arrow, donut with icon bubbles and percentages. Tapping a slice or legend item drills into that category's transactions.
- Cash flow card below: total, income and expense chips, small bars. Swipe horizontally for extra cards (Top merchants, Income vs spend by week) with the same design.

### 8.4 Accounts
- Hero: "Total wealth" (muted), the number (40px), change vs last month with arrow, smooth line chart of total wealth over time (scrubbable).
- Wallet list card: icon tile, name, kind, balance, monthly change (colored). Tap an account for its detail page: the same hero pattern (balance, line, donut for its expenses) and its transactions. Long-press to reorder (drag handle appears).
- Search pill and filter button sit above the bottom nav. The "+" button adds a transaction, and a text button "New account" opens the account form (name, kind, icon, color, opening balance).
- Total wealth includes investments (from Invest) as a separate line in the breakdown, labeled "Investments".

### 8.5 Budgets
- Tabs for periods. Glass circular gauge (liquid wave) with "Well done, you saved" / "$531.16" / "of $5,600 budget" (copy adapts when over). Below it, a "Spent / ideal pace / budget" line chart for the selected period.
- Categories card ("Spent / Budget"): rows with category icon, name, "$609 of $220" and a progress bar (mint or coral). Tap a row to edit the amount through a small sheet with a numeric keypad.
- "Set a budget" card for categories without a budget, each with a mint "Set" pill. Include an overall "All" budget row first. Budgets are per month and carry forward automatically as the same amount until changed.

### 8.6 Invest
- Hero: "Total invested value" and the number, all-time gain in mint ("+$2,310.45, +9.4% all time") or coral if negative. Range tabs 1M / 6M / 1Y / All. Line chart with two series: "Value" (solid mint) and "Money put in" (dashed `--ink-muted`), the legend under the chart, scrub to see both values and the gap.
- Holdings card: icon tile, name, yield label ("4.5% APY", "Variable"), value, and a mint/coral growth chip. Tap opens Holding detail: its own chart, contribution history, "Add deposit", "Withdraw", "Edit rate", "Archive". Manual holdings show "Update value".
- "Estimated yearly yield" card with the total (yield holdings only) and a tiny explanatory line "Based on current balances and rates".
- Floating "Add investment" button opens a form: name, type, mode (auto-grows from a rate, or I'll enter the value), principal, APY (percent input), start date, optional maturity date, icon/color.
- Link "Try the growth calculator" opens the calculator.

### 8.7 Growth calculator
- Result card: final value (hero number), caption "after 10 years", "You put in $X, you earn $Y", and a stacked bar (lavender for put in, mint for earned) that animates proportionally.
- Chart: growth curve with soft mint gradient and a draggable scrubber showing "Year 6: $14,520" style tooltips (snap to months, haptic tick on year boundaries).
- Controls card with large sliders and also tappable value labels that open a numeric entry: starting amount, monthly contribution, yearly yield (0-20%, step 0.1), years (1-50). Compounding segmented control (Monthly / Yearly).
- "Save as investment" turns the inputs into a new yield holding (starting amount as the first contribution, today as start date) and navigates to it.
- Footer line: "Estimate, not financial advice."

### 8.8 Add Transaction (bottom sheet)
- Segmented control Expense / Income / Transfer. Giant amount (56px) with a blinking caret. Horizontally scrolling category chips (last used first, the selected chip filled with its category color). Compact rows: account (chevron to a picker), date (defaults to Today, picker offers Today / Yesterday / Pick date), note.
- For Transfer: from-account and to-account replace the category row, and the same account cannot be chosen twice.
- Custom keypad: digits, decimal point, backspace, large wide mint "Save" button above the gesture area. Save is disabled while the amount is 0. After saving an expense, show a snackbar "Saved, Food -$18.50" with "Undo".
- A "Save and add another" option appears as a secondary text button.
- Remember the last used account and category. Opening the sheet from a category ring preselects that category.
- Edit mode reuses the same sheet with "Save changes" and a coral "Delete" text button.

### 8.9 Scheduled
- Summary card "Next 30 days, -$172.47". List of recurring items with icon tile, name, repeat label ("monthly"), next date, amount, and an amber "Due in 3 days" pill when within 7 days (coral "Overdue" if past and not auto-posted). Pause/resume toggle in the detail sheet. Floating "+" opens a form: name, amount, kind, account, category, repeat, first date, "Post automatically" switch.

### 8.10 Activity
- Opened by the bell. Grouped "Today" / "Earlier". Cards with soft-tinted icon tiles. Unread items show a small mint dot. Tapping an item marks it read and deep-links (budget -> Budgets, scheduled -> Scheduled). "Mark all read" in the header.
- Empty: "You're all caught up".

### 8.11 Profile
- Avatar (initials or chosen color) with name and a "Local data only" badge with a lock icon. Grouped cards:
  - **Security:** PIN (change/remove), Fingerprint (switch), Lock after (immediately / 30 seconds / 5 minutes).
  - **Money:** Currency, Categories (add/edit/reorder/archive), Accounts, Budgets.
  - **Data:** Export data (CSV for transactions, JSON full backup), Import backup (JSON), Load demo data, Clear all data (coral, with a typed confirmation "DELETE").
  - **Appearance:** Light / Dark / System, Reduce motion.
  - **About:** version, "Your data never leaves this phone."
- Dark mode for every screen must be fully designed. No leftover light surfaces, no low-contrast text.

---

## 9. COPY AND MICROCOPY RULES

Words are part of the design. Write from the user's perspective, in plain language, sentence case, active voice. Be specific rather than clever.
- Buttons name the exact result: "Save", "Save changes", "Add investment", "Delete transaction". The same action keeps the same name everywhere ("Save" produces "Saved").
- Errors explain what happened and how to fix it, and never apologize or blame: "Enter an amount above $0." "That PIN doesn't match. Try again." "Choose a different account for the transfer."
- Empty states invite action in one line plus one button. Examples: "No budgets yet. Set one to see your pace." "No investments yet. Add one or try the calculator."
- Money tone: plain facts. Over budget reads "You're $432 over budget" with no emoji or scolding. Under budget reads "You saved $531".
- No lorem ipsum, no "Coming soon" buttons, no placeholder text anywhere.

---

## 10. DEMO DATA

Provide `seed.ts` that loads, on request ("Load demo data"), the following, so every screen looks like the mockups on first run:
- Accounts: Main Wallet, Checking, Credit Card, Savings with balances matching the Accounts mockup.
- 9 months of income and expense history that produces the Home bar chart shape, plus today and yesterday's transactions shown in the Home mockup.
- Budgets matching the mockups (Food & Drink, Shopping, Dining out, Transport and the overall budget), scheduled items (Rent, Phone plan, Streaming, Gym), and four holdings (High-yield savings 4.5% APY, Treasury bills 4.1%, Index fund manual, Certificate of deposit 5.0%).
- Use generated dates relative to today so the data is always current. Mark demo records so "Clear all data" removes them cleanly.
- First run (nothing loaded) shows a short three-step onboarding: welcome with the mascot, "Pick your currency", "Set a PIN (optional)", then Home with the empty state.

---

## 11. ACCESSIBILITY, PERFORMANCE, QUALITY FLOOR

- Contrast AA in both themes. Visible keyboard/focus rings (2px mint outline, offset 2px). All icon-only buttons have `aria-label`. Charts expose a text summary via `aria-label` and a visually hidden data table. Respect system font scaling up to 130% without breaking layouts (use rem and flexible containers).
- Performance: the first interaction in under 100ms on a phone, list virtualization for more than 200 transactions, memoize chart computations, avoid layout thrash, no re-renders of the whole app when a single number changes. Only animate compositor-friendly properties.
- Robustness: validate every form, trim names, cap amounts at 9,999,999.99, handle empty and giant lists, handle very long merchant names with ellipsis, handle negative balances, never show NaN, Infinity, "undefined" or "-0.00".
- Persist after every write. Survive a hard refresh at any point. Wrap IndexedDB access in try/catch and show a clear message if storage is unavailable.
- No dead code, no console errors or warnings, no unused dependencies, no `any`.

---

## 12. PACKAGING (README.md)

Write a README with exact steps and commands to turn this project into an APK, assuming Node and Android Studio are installed:
1. `npm install`, `npm run build`
2. `npm i @capacitor/core @capacitor/cli @capacitor/android` and the plugins needed for biometric auth, haptics, local notifications, filesystem and share
3. `npx cap init Pocketly com.pocketly.app --web-dir=dist`, `npx cap add android`, `npx cap sync`
4. Required `AndroidManifest.xml` permission entries (biometric, notifications incl. Android 13+ runtime request), and where to put the app icon and splash (describe an adaptive icon concept: mint droplet in a rounded-square on lavender, matching the mascot's pouch)
5. `npx cap open android`, build a debug APK, then a signed release APK/AAB, with the exact menu path in Android Studio
6. A short troubleshooting list (white screen = wrong base path, plugin not found = forgot `cap sync`, biometrics not showing = missing permission)

Also generate `capacitor.config.ts` and a `.gitignore`.

---

## 13. PROCESS (follow in this order, in a single run)

1. **Plan first, briefly.** Before writing code, output a compact design plan (palette tokens, type roles, layout notes per tab, the liquid motif and where it appears, motion tokens). Then review that plan against this brief: if any part looks like a generic default rather than a choice made for Pocketly, change it and say what you changed. Do not wait for approval. Continue immediately.
2. Build in this order: (a) design tokens, primitives, theme, shell, router, bottom nav, lock screen with the mascot, (b) data layer, domain math with tests, seed, (c) Add Transaction sheet and Home with list, rings, bars, (d) Overview, Accounts, Budgets with the liquid gauge, (e) Invest, Holding detail, Growth calculator, (f) Scheduled, Activity, Profile, onboarding, export/import, (g) motion polish pass, dark mode pass, accessibility pass, empty/error state pass, README.
3. After each stage, silently self-critique against the mockups: spacing, alignment, hierarchy, type scale, and animation smoothness. Fix drift before moving on.
4. Final pass checklist (verify each, fix, then report): every mockup screen exists and matches; the Add Transaction flow takes about 3 taps; totals update everywhere after any change; dark mode is complete; no layout breaks at 360 px; calculator reference test passes; the app works fully offline with no network calls; no emoji icons; no placeholder text; no console errors.

## 14. DO NOT
- Do not add accounts, login, cloud sync, analytics, ads or any network request.
- Do not use random or fake "live" market data. Say "Updated {date}" for manual values.
- Do not use emoji as icons, ALL CAPS labels, tracked-out eyebrow labels, arrows on button text, or the default purple-gradient AI look.
- Do not use a charting library, a UI kit, or CSS animations on layout properties.
- Do not copy the name, logos, icons or mascot of any app shown in references. Pocketly and its sleepy coin-pouch mascot are original.
- Do not hardcode the example numbers from the mockups into the logic. They are demo content only.
