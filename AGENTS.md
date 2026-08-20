# OmniSIM repository guidance

This repository contains **OmniSIM**, a native Android SIM/eSIM renewal manager.
Treat this file as the local implementation contract for all work in this repository.

## Engineering behavior

- State material assumptions before implementing ambiguous behavior.
- Prefer the simplest solution that fully satisfies the requirement.
- Keep changes surgical: do not refactor or reformat unrelated code.
- Every changed line should trace to a product requirement or a test/build fix.
- Work in verifiable increments and keep the project compilable.
- Put business logic outside composables and cover critical logic with unit tests.
- Do not leave fake data paths, placeholder core actions, or core-feature TODOs.
- Keep every version-controlled Kotlin or Gradle Kotlin source file (`*.kt`, `*.kts`)
  at or below 600 physical lines, including comments and blank lines. Generated and
  build-output directories are excluded. Split files by responsibility instead of
  suppressing or weakening the `checkCodeFileLength` verification task.

## Product identity and scope

- App name: `OmniSIM`
- Subtitle: `SIM & eSIM Renewal Manager`
- Tagline: `Never miss a SIM renewal again.`
- Package: `app.omnisim.android`
- License: MIT
- Platform: Android only
- Product: lightweight, local-first renewal/recharge/keep-alive reminders for a single
  user managing a small number of SIMs and eSIMs.

Never add accounts, login, cloud sync/backend, billing, analytics, telemetry,
advertising, desktop/web/iOS targets, eSIM provisioning, carrier integration,
payments, SMS/call/contact access, scanning/OCR, AI, widgets, or multi-user features.

## Technical direction

Use native Android technologies:

- Kotlin and Jetpack Compose
- Material 3 and Navigation Compose
- Room/SQLite
- DataStore Preferences
- Coroutines and Flow
- WorkManager and Android notification APIs

Use a simple dependency flow:

```text
Compose UI -> ViewModel -> Repository -> Room / DataStore
```

A small application container is preferred over dependency-injection ceremony.
Use immutable UI state and `StateFlow`. Keep date, validation, reminder, backup,
and persistence logic out of composables. Avoid unnecessary third-party libraries.

## Core experience

The app must quickly answer:

1. What SIMs do I have?
2. Which SIM needs attention next?
3. After renewal, when is the next renewal?

Primary flow:

```text
Open -> see nearest renewal -> select SIM -> recharge externally
-> Mark as Renewed -> confirm actual date -> calculate/edit next date
-> save history -> reschedule reminders
```

Use exactly four bottom-navigation destinations: Home, SIMs, Usage, Settings. Renewal
history belongs on the SIM detail screen. Usage is a first-class destination and
must not be removed as unreachable or treated as dead code. The top app bar may
expose Add SIM.

## Design contract

- Polished native Android utility: minimal, calm, clean, modern, utility-first.
- Use Material 3, edge-to-edge layouts, proper system bars and accessible semantics.
- Support light, dark, system theme and optional Material You dynamic color.
- Avoid gradients, glassmorphism, neon, decorative charts, excessive shadows/cards,
  crowded layouts, and excessive animation.
- Use text as well as restrained color for status. Keep touch targets and contrast
  accessible and use stable keys in lazy lists.
- No onboarding carousel. Use polished empty states and concise snackbars.

## Navigation and screens

### Home

Home is not an analytics dashboard. Sort active SIMs by next renewal date. The
nearest record is visible first. Show urgent records under **Needs attention**
(Overdue, Due Today, Due Soon) and the rest under **Upcoming**. Urgent cards show
name/carrier, masked number, time remaining, exact date, cycle, and
**Mark as Renewed**. With no records, show an Add SIM empty state.

### SIM list

Show a clean vertical list with name, carrier/country where available, masked
phone number, next date, days remaining, and textual status. Provide search over
name, carrier, phone number, and country. Filters: Active, Due Soon, Overdue,
Archived. Archived records do not appear in Home.

### Usage

Show local renewal-cost summaries for active SIMs without turning Home into an
analytics dashboard. Present daily, 30-day, and 365-day estimates, per-currency
breakdowns, data-coverage guidance, and links to complete missing price or cycle
information. A combined total may use public European Central Bank reference
rates in the user's default currency. Cache the most recent valid rates for
offline fallback and clearly label loading, cached, partial, and unavailable
states. Do not add telemetry, tracking, decorative charts, or remote SIM-data
processing.

### Add/edit SIM

Use a full-screen Compose form. Required: display name, carrier, next renewal
date. Optional: phone, country, SIM type, plan, last renewal date, renewal cycle,
amount, currency, renewal website, notes. SIM types are `eSIM` (default) and
`Physical SIM`. Cycle options are 30, 60, 90, 120, 180, 365 days, Custom,
monthly on a fixed day from 1 through 31, and no automatic cycle. A monthly day
that does not exist in a shorter month resolves to that month's final day. Use
native Material date pickers.

Validate required fields, positive cycles, non-negative prices, and HTTP(S) URLs.
Do not over-validate international phone numbers. Store blank optional fields as
null when practical.

### SIM detail

Show identity/contact data, status and days remaining, last/next renewal, cycle,
price, notes, and recent renewal history. Provide working actions for Mark as
Renewed, Open Renewal Website, Edit, Archive/Restore, and Delete. Only open
`http` and `https` URLs. Confirm destructive deletion and cascade-delete history.

### Mark as renewed

Use a Material 3 modal bottom sheet. Default actual renewal date to today. If a
cycle exists, calculate:

```text
next renewal date = actual renewal date + cycle days
```

Never calculate from the previous expiry date. Let the user override the generated
next date before confirmation. On confirm, transactionally create history, update
last/next dates, store optional amount/notes, refresh UI, clear obsolete reminder
state, and reschedule reminders. Give concise success feedback.

### Settings

Persist in DataStore:

- System/Light/Dark theme
- Dynamic color toggle
- Default warning period: 3, 7, 14 (default), 30, or custom days
- Mask phone numbers (enabled by default)
- Individual reminder offsets
- Default currency
- Backup/restore entry points
- App version, MIT license, and open-source information

Explain notification value before requesting Android 13+ permission; never request
it immediately on first launch without context.

## Data model

Use UUID string primary keys and Room type converters. Calendar deadlines use
`LocalDate` directly; metadata timestamps use `Instant`.

`SimEntity` includes identity, carrier/country/phone/type/plan, last and next
renewal dates, optional cycle, price/currency, safe renewal URL, notes, archived,
and created/updated timestamps.

`RenewalHistoryEntity` includes its UUID, owning SIM UUID, actual renewal date,
previous renewal date, calculated/confirmed next date, optional amount/currency/
notes, and created timestamp. Use an appropriate foreign key with cascade delete.

`ReminderStateEntity` records a unique `(simId, renewalDate, reminderOffset)` key
so a reminder is never sent repeatedly for one cycle. Cascade with the SIM.

Settings generally belong in DataStore, not Room. Supply explicit Room migrations
when the schema changes and export schemas for validation.

## Date and status rules

Centralize and test all date logic. Required utility behavior includes:

```kotlin
daysUntilRenewal(today, renewalDate)
calculateNextRenewalDate(actualRenewalDate, cycleDays)
calculateNextMonthlyRenewalDate(actualRenewalDate, dayOfMonth)
calculateRenewalStatus(today, renewalDate, warningPeriodDays, archived)
```

Derived status precedence:

1. Archived when `archived == true`
2. Overdue when next date is before today
3. Due Today when dates are equal
4. Due Soon when remaining days are within the warning period
5. Active otherwise

Do not persist a duplicate derived status.

## Notifications

Use WorkManager for battery-friendly approximate daily checks that survive restarts.
No continuous service or exact alarm. Default supported reminders: 30, 14, 7, 3,
and 1 days before, renewal day (0), and overdue. Users can enable/disable offsets.
Archived records never notify. Mask phone numbers according to settings.

Create a notification channel. Notifications open the matching SIM detail via a
deep link/pending intent. A direct Renewed action is optional; omit it if it harms
reliability. Deduplicate using `sim_id + renewal_date + reminder_offset`, and clear
obsolete state after renewal. Document that WorkManager timing is approximate and
manufacturer battery policies can delay delivery.

## Backup and restore

Use Android Storage Access Framework; never request broad storage permission.
Export a versioned JSON document containing SIMs, renewal history, and relevant
settings. Suggested name: `omnisim-backup-YYYY-MM-DD.json`.

Restore must read and validate JSON, schema version, and required fields before
showing confirmation. Import all database records transactionally. A failed restore
must leave existing data unchanged. Support future format migration via
`backupVersion` (v1 initially). Reject unsafe renewal URL schemes during import.

## Privacy, permissions, and offline behavior

Core SIM management, renewal history, reminders, and backup behavior must work
entirely offline. Internet access is limited to fetching public European Central
Bank daily reference rates, European Commission InforEuro monthly rates, checking
official GitHub releases, and user-initiated external renewal or update links.
These requests must never include SIM data,
phone numbers, prices, renewal dates, or notes. Keep recent valid exchange rates
cached so Usage remains useful offline. Do not include analytics, crash reporting,
advertising, telemetry, tracking, or remote logging.

Keep permissions minimal. Notification permission is expected where Android
requires it. Do not request contacts, SMS, phone, call logs, location, camera,
microphone, or broad storage access.

## Required implementation quality

All core actions must use real persistent data and survive app/process restarts:
add, edit, archive, restore, delete, renew, history, usage, settings, notifications,
backup, and restore. Use Room `Flow`, off-main-thread file/database work, stable
Compose list keys, and accessible labels. Optimize for a few to a few dozen records;
do not introduce premature caching or abstraction.

Prioritize unit tests for:

- 30/90/180/365-day and fixed monthly-day renewal calculations
- all derived statuses
- reminder offset matching and deduplication
- valid backup, invalid JSON, unsupported version, and missing required fields

Before declaring completion, run and pass:

```text
gradlew assembleDebug
gradlew test
gradlew lint
```

Manually review the critical persistence flow:

```text
Add -> reopen -> view on Home -> renew -> verify next date/history
-> reminder rescheduled -> export -> restore without data loss
```

## Documentation

Maintain a professional README covering product purpose, features, screenshot
placeholder, privacy, Android requirements, setup/build instructions, backup and
restore, notification limitations, contributing, and MIT license. State clearly:

> OmniSIM is local-first. Your phone numbers, SIM information, renewal dates and
> notes are stored locally on your Android device. OmniSIM does not require an
> account and does not upload your SIM data to a remote server.
