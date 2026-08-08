# OmniSIM Design System

This file is the local source of truth for OmniSIM's visual language. It adapts
`D:\googledown\mobile_app_soft_minimal_design_guide.md` to a native Android,
local-first SIM renewal utility without copying another product's branding or
features.

## Principles

- Status before actions: each screen answers one primary question first.
- Soft minimalism: cool-gray pages, white surfaces, near-black text, generous whitespace.
- One accent: warm yellow is reserved for the single primary action on a screen.
- Atmospheric blue: use soft blue for hero/status areas, not for every card.
- Progressive disclosure: keep root screens scannable and move detail into sheets or detail screens.
- Native Android: Material 3 semantics, 48 dp touch targets, system font, predictable back behavior.

## Tokens

- Screen padding: 20 dp
- Card padding: 20 dp
- Row gap: 12 dp
- Section gap: 32 dp
- Small radius: 12 dp
- Medium radius: 16 dp
- Card radius: 22 dp
- Large panel radius: 30 dp
- Sheet top radius: 34 dp
- Primary button: 56 dp high, pill shape
- Page title: 34 sp, bold
- Section title: 22–24 sp, semibold
- Body: 16 sp
- Metadata: 12–14 sp, muted gray

## Color roles

- Page background: `#F3F4F6`
- Surface: `#FFFFFF`
- Primary text: `#0B0B0C`
- Secondary text: `#5F6670`
- Soft blue: `#D7ECFA`
- Primary action: `#FFE24A`
- Border/divider: `#E7E9ED`

## Component rules

- Root feature screens use a large start-aligned title; settings and modal/detail screens use a centered title.
- Cards are white and normally have no border or shadow.
- Related controls may share one card; independent actions use independent cards.
- Secondary buttons are white, lightly outlined, and pill-shaped.
- Bottom navigation floats inside 20 dp side margins with one animated selected capsule.
- Sheets use a large rounded top, circular close action, and a fixed primary action where needed.
- Use one Material icon family and never use emoji as interface icons.

## Avoid

- Dense dashboards, decorative charts, heavy shadows, multiple accent colors.
- Repeating the same information in several cards.
- Using dividers to compensate for insufficient spacing.
- Adding travel, account, rewards, marketing, or cloud features that do not serve SIM renewal tracking.
