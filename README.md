# Keyora

An elegant, privacy-first Android keyboard with Apple-inspired minimal design and **per-app Light/Dark themes**.

Keyora is a real Android Input Method Editor (IME). It appears in system keyboard settings, types into any text field, and lets you choose how the keyboard looks in WhatsApp, Chrome, Telegram, and other apps.

> **Important limitation:** Keyora does **not** currently detect whether another app is using Light Mode or Dark Mode internally. Android does not provide a universal API for an IME to read a host app’s private theme. Instead, Keyora lets you configure the keyboard theme per application.

## Features

- Real system keyboard via `InputMethodService`
- Clean, minimal QWERTY layout with numbers and symbols
- Shift, caps lock, backspace, enter, space, globe (IME picker)
- Global Light / Dark / System Default themes
- **Per-app theme rules** (e.g. WhatsApp → Dark, Chrome → Light)
- Onboarding and settings to enable/select the keyboard
- Privacy-first: no network permission, no analytics, no cloud typing
- Placeholder on-device suggestions (disabled for password fields)
- Responsive layout for phones and landscape

## Screenshots

_Add device screenshots here after installing on an emulator or phone._

- Keyboard (Light)
- Keyboard (Dark)
- Settings → Appearance
- Settings → App Themes

## Architecture

```
app/src/main/java/com/keyora/keyboard/
  ime/           InputMethodService, controller, input handling, Compose host
  ime/ui/        Keyboard Compose UI
  theme/         Theme models, rule manager, detectors, colors
  settings/      DataStore repository, ViewModel, settings screens
  appdetector/   EditorInfo package detection + installed app listing
  suggestions/   Modular SuggestionEngine (placeholder MVP)
  onboarding/    First-run flow
  enablement/    IME enable/select helpers
```

### Theme resolution

1. Explicit per-app rule (`LIGHT` / `DARK`)
2. Else global keyboard theme
3. If global is `SYSTEM_DEFAULT`, use Android night mode

### How Android IME works (short)

1. The app declares an `InputMethodService` with `BIND_INPUT_METHOD` and `@xml/method` metadata.
2. The user enables Keyora in **Settings → System → Languages & input → On-screen keyboard**.
3. When a text field focuses, Android binds to the IME and calls `onStartInput` / `onCreateInputView`.
4. Keyora reads `EditorInfo.packageName` to know which app is requesting input, resolves a theme, and commits text through `InputConnection`.

## Technology

- Kotlin
- Gradle Kotlin DSL
- Jetpack Compose + Material 3
- `InputMethodService`
- DataStore Preferences
- Coroutines + ViewModel
- minSdk 26, targetSdk 36

## How to build

Requirements:

- Android SDK (API 36 platform recommended)
- JDK 17

```bash
./gradlew assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

Run unit tests:

```bash
./gradlew test
```

## How to install on a device

```bash
./gradlew installDebug
```

Or sideload `app-debug.apk`.

## How to enable the keyboard

1. Open **Keyora** from the app launcher.
2. Complete onboarding, or open settings and tap **Enable Keyora**.
3. In system settings, turn **Keyora** on.
4. Tap **Select Keyora as Keyboard** (or switch via the globe key / system picker).
5. Open any text field — Keyora should appear.

Keyora never changes the default keyboard without your action.

## How to configure per-app themes

1. Open Keyora → **App Themes**.
2. Tap **+** and pick an app (launchable apps listed via PackageManager).
3. Choose **System Default**, **Light**, or **Dark**.
4. Save.

Example:

| App | Rule |
| --- | --- |
| WhatsApp | Dark |
| Chrome | Light |
| Telegram | Dark |
| Others | Follow global / system theme |

Rules persist across app restarts and device reboots via DataStore.

## Privacy

- No cloud processing
- No analytics / ads in MVP
- Typed text never leaves the device
- Passwords are not stored; suggestions are off for password fields
- No AccessibilityService
- No `QUERY_ALL_PACKAGES` — app picker uses launcher intent `<queries>`
- No microphone / network permissions

## Limitations

- English QWERTY only in MVP
- Suggestions are a simple on-device placeholder, not AI prediction
- Does **not** auto-detect host app Light/Dark UI themes
- Accent long-press covers common Latin accents only
- Landscape supported; foldables not specially tuned

## Future roadmap

- Automatic app theme detection where technically possible
- AI-powered suggestions / advanced autocorrection
- Multilingual layouts
- Emoji search, clipboard manager
- Custom key colors / backgrounds
- Haptic & sound controls
- Swipe typing / voice typing
- User-created themes & scheduling

## Contribution guide

1. Fork / branch from `main`.
2. Keep the IME typing path fast and crash-safe.
3. Prefer official Android IME APIs — no AccessibilityService shortcuts.
4. Run `./gradlew assembleDebug test` before opening a PR.
5. Document user-facing privacy or permission changes.

## License

MVP sample project. Third-party AndroidX / Compose libraries are under Apache License 2.0.
