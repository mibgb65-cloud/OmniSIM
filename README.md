# OmniSIM

**SIM & eSIM Renewal Manager**

Never miss a SIM renewal again.

OmniSIM is a small, native Android utility for keeping track of SIM and eSIM
renewal, recharge, and keep-alive dates. It is designed for a single person with a
handful of numbers and works without an account or backend.

## Features

- Home screen focused on the SIM that needs attention next
- Overdue, due-today, due-soon, active, and archived states derived from dates
- Persistent SIM/eSIM records with search and practical filters
- 30, 60, 90, 120, 180, 365-day and custom renewal cycles
- Fast **Mark as Renewed** flow with an editable calculated next date
- Per-SIM and filterable global renewal history, optional amount, notes, and safe external renewal links
- Archive/restore and confirmation-protected deletion
- Approximate daily local reminders with per-offset controls and deduplication
- System, light, and dark themes with optional Material You dynamic color
- Phone-number masking enabled by default
- Automatic daily ECB reference-rate conversion for a combined cost estimate,
  with an offline cached-rate fallback
- Built-in privacy and permission details plus a concise usage guide in Settings
- Versioned JSON export and validated, transactional database restore through the
  Android Storage Access Framework

## Screenshots

Screenshots will be added after the first tagged release.

| Home | SIM details | Settings |
| --- | --- | --- |
| _Coming soon_ | _Coming soon_ | _Coming soon_ |

## Privacy

OmniSIM is local-first.

Your phone numbers, SIM information, renewal dates and notes are stored locally on
your Android device.

OmniSIM does not require an account and does not upload your SIM data to a remote
server.

The app contains no analytics, telemetry, advertising, remote logging, or cloud
service. Core SIM tracking and reminders work offline. When the Usage screen needs
currency conversion, OmniSIM downloads the European Central Bank's public daily
reference-rate XML and caches it locally; no SIM data, phone numbers, prices, or
renewal dates are sent with that request. A renewal website is opened only after
the user explicitly taps it, using the user's chosen browser.

## Android requirements

- Android 6.0 (API 23) or newer
- Android 13+ asks for notification permission only after the user chooses to
  enable reminders in Settings
- Android 12+ can use wallpaper-derived Material You colors

## Development setup

1. Install Android Studio or an Android SDK with API 36 and Build Tools 35.0.0 or
   newer.
2. Install JDK 17 or newer. The project is compiled to Java 17 bytecode.
3. Clone the repository and open its root directory in Android Studio.
4. Let Gradle sync, then run the `app` configuration on an emulator or device.

The project uses a checked-in Gradle wrapper. On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Tests and lint

```powershell
.\gradlew.bat test
.\gradlew.bat lint
```

Business-logic tests cover renewal-date calculations, status precedence, reminder
matching/deduplication, and backup validation.

## Architecture

OmniSIM intentionally uses a small architecture:

```text
Jetpack Compose UI
        ↓
AppViewModel + immutable StateFlow state
        ↓
SimRepository / SettingsRepository
        ↓
Room / DataStore Preferences
```

A simple application container supplies the database, repositories, backup manager,
and reminder scheduler. No dependency-injection framework or cloud service is used.

Room stores three tables:

- `sims` — SIM identity and renewal configuration
- `renewal_history` — actual renewal events, cascade-owned by a SIM
- `reminder_state` — the unique SIM/date/offset notifications already sent

Renewal deadlines are stored as ISO `LocalDate` values. Only creation/update metadata
uses `Instant`, avoiding timezone shifts in calendar deadlines.

## Backup and restore

Settings → Data provides JSON export and import. Android's document picker chooses
the destination/source, so OmniSIM does not need broad storage permission.

Backups contain a `backupVersion`, SIMs, renewal history, and relevant settings.
Before showing the restore confirmation, OmniSIM parses and validates the complete
document, including IDs, references, dates, required values, prices, and safe HTTP(S)
links. Database replacement is a Room transaction; invalid input leaves the current
data unchanged.

## Notification behavior

OmniSIM schedules one battery-friendly periodic WorkManager job. It checks enabled
offsets (30, 14, 7, 3, 1, and 0 days plus overdue by default) and records a unique
`SIM ID + renewal date + offset` marker after notifying. Renewing a SIM clears the
old markers for that SIM.

WorkManager is deliberately approximate. Android may defer background work because
of Doze, battery saver, app standby, or manufacturer-specific policies. Notifications
are therefore date reminders, not guaranteed exact-time alarms. OmniSIM does not
request exact-alarm permission or run a continuous background service.

## Contributing

Issues and focused pull requests are welcome. Keep changes within OmniSIM's goal: a
fast, private, local-first renewal reminder for a small personal SIM collection.

Before submitting a change, run:

```powershell
.\gradlew.bat assembleDebug test lint
```

Do not add analytics, account systems, cloud infrastructure, or unnecessary
permissions/dependencies.

## License

OmniSIM is released under the [MIT License](LICENSE).
