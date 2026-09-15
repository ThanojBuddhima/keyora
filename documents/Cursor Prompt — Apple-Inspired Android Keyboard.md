You are an expert Android engineer. Help me build a production-quality Android keyboard application from scratch.

## PROJECT

Build an Android keyboard app called **Keyora**.

The goal is to create an **Apple-inspired, clean, minimal Android keyboard** with a polished UI and a unique feature:

> The keyboard can automatically switch between Light and Dark themes based on the current app, using user-configured per-app theme rules.

Example:

- Android system theme: Light
- WhatsApp: Dark → keyboard automatically uses Dark
- Chrome: Light → keyboard automatically uses Light
- Telegram: Dark → keyboard automatically uses Dark

Do NOT copy Apple's proprietary assets, exact UI, icons, or branding. The design should be inspired by the general characteristics of modern iOS keyboards: clean, minimal, rounded keys, subtle shadows, balanced spacing, smooth animations and premium appearance.

---

# 1. TECHNOLOGY STACK

Use:

- Kotlin
- Android Studio project
- Gradle Kotlin DSL
- Jetpack Compose where appropriate
- Android InputMethodService for the keyboard
- Material 3
- AndroidX
- Coroutines where useful
- ViewModel for settings/state management
- DataStore Preferences for persistent settings

Minimum Android version: Android 8.0 (API 26)

Target the latest stable Android SDK available in the environment.

Avoid unnecessary third-party libraries.

The keyboard itself must work as a real Android system keyboard, not merely as a demo UI.

---

# 2. CORE FUNCTIONALITY

Implement a real Android IME.

The keyboard must:

- appear in text fields
- allow typing
- support backspace
- support enter/return
- support space
- support shift
- support uppercase/lowercase
- support numbers
- support symbols
- support switching between keyboard layouts
- support cursor movement where practical
- support deleting characters
- support long-press behavior where practical
- support suggestions/autocorrection architecture, but keep advanced prediction optional for the MVP

Use Android's InputMethodService and InputConnection correctly.

Do not fake keyboard input using accessibility services.

---

# 3. KEYBOARD DESIGN

Create a premium minimal keyboard.

Visual direction:

- Apple-inspired
- clean
- modern
- minimal
- rounded rectangular keys
- subtle shadows/elevation
- smooth spacing
- highly readable typography
- no unnecessary visual clutter
- responsive layout for different screen sizes

Do not use Apple's exact design or copyrighted assets.

Create our own icons using vector drawables or Compose icons.

Keyboard should include:

### Main keyboard

Q W E R T Y U I O P

A S D F G H J K L

Shift Z X C V B N M Backspace

123 Globe/Language Microphone Space Return

Adapt the layout according to screen width.

---

# 4. LIGHT THEME

Create a polished light keyboard.

Example visual direction:

Background:
- very light gray

Keys:
- white/light gray
- subtle elevation

Text:
- dark gray/black

Special keys:
- slightly darker background than normal keys

The exact colors should be defined centrally in a theme system so they can easily be changed later.

Do not hard-code colors throughout the UI.

---

# 5. DARK THEME

Create a polished dark keyboard.

Example:

Background:
- dark gray/near black

Keys:
- slightly lighter dark gray

Text:
- white/light gray

Special keys:
- slightly different dark tone

Again, define everything centrally in the theme system.

---

# 6. MAIN UNIQUE FEATURE: PER-APP THEMES

The most important feature is:

## App-specific keyboard themes

Users should be able to configure:

App → Keyboard Theme

Example:

WhatsApp → Dark

Chrome → Light

Telegram → Dark

Gmail → Light

Instagram → Dark

The keyboard should identify the application currently requesting text input and select the corresponding user-configured theme.

Do NOT attempt to inspect the actual visual theme of every application in the MVP.

Instead, implement a reliable per-app rule system.

---

# 7. HOW APP DETECTION SHOULD WORK

Use the InputMethodService/InputConnection context to determine the current input editor/application where possible.

Investigate the correct Android APIs for determining the package/application associated with the current input connection.

Do not use AccessibilityService just to detect the current app.

Do not require root.

Do not use invasive permissions.

If Android does not provide the package name in a particular situation, gracefully fall back to the default keyboard theme.

Create a clean abstraction such as:

AppContextDetector

with functionality similar to:

getCurrentPackageName()

Do not invent APIs. Verify that the implementation is actually supported by the Android SDK.

---

# 8. THEME RULE ENGINE

Create a ThemeRuleManager.

It should support:

getThemeForPackage(packageName)

setThemeForPackage(packageName, theme)

removeThemeForPackage(packageName)

getAllRules()

Theme values:

LIGHT
DARK
SYSTEM_DEFAULT

Example:

com.whatsapp → DARK
com.google.android.googlequicksearchbox → LIGHT

If no rule exists:

use SYSTEM_DEFAULT.

---

# 9. SETTINGS APPLICATION

Create a normal Android settings/activity application for configuring the keyboard.

The user should be able to open:

Keyora Settings

Main sections:

## Appearance

- Keyboard Theme
  - System Default
  - Light
  - Dark

## App Themes

Show:

"Customize keyboard theme for individual apps"

Then display a list of applications.

Example:

WhatsApp
[Dark ▼]

Chrome
[Light ▼]

Telegram
[Dark ▼]

Gmail
[System Default ▼]

Allow users to:

- add an app
- choose Light
- choose Dark
- choose System Default
- remove a rule

---

# 10. APP SELECTION UI

Build an app picker.

Display installed applications that can reasonably be configured.

Each item should contain:

- application icon
- application name
- package name optionally
- current keyboard theme

Example:

WhatsApp
Dark

Chrome
Light

Telegram
System Default

Use Android PackageManager to retrieve installed applications.

Only request permissions that are genuinely required.

Avoid broad package visibility permissions unless absolutely necessary.

If Android package visibility restrictions apply, design the implementation according to modern Android requirements.

---

# 11. LIVE THEME SWITCHING

When the keyboard opens:

1. Determine current application package.
2. Query ThemeRuleManager.
3. Determine the selected theme.
4. Apply the theme to the keyboard.

Example:

Keyboard opens in WhatsApp:

Current package:
com.whatsapp

Rule:
DARK

Result:
Dark keyboard.

Then user switches to Chrome:

Current package:
com.android.chrome

Rule:
LIGHT

Result:
Light keyboard.

The keyboard should update its theme when the input context changes.

Avoid restarting the entire application unnecessarily.

---

# 12. DEFAULT THEME LOGIC

Priority:

1. Per-app theme rule
2. Global keyboard theme setting
3. System theme

For example:

Global setting:
SYSTEM_DEFAULT

WhatsApp:
DARK

Chrome:
LIGHT

Then:

WhatsApp → DARK

Chrome → LIGHT

Other apps → Android system theme

If global setting is manually set to DARK, apps with SYSTEM_DEFAULT should use DARK.

---

# 13. SETTINGS STORAGE

Use Jetpack DataStore Preferences.

Store:

globalTheme

appThemeRules

Example conceptual data:

globalTheme = SYSTEM_DEFAULT

appThemeRules = {
    "com.whatsapp": "DARK",
    "com.android.chrome": "LIGHT"
}

Create a repository:

SettingsRepository

It should expose Flow-based state where appropriate.

Do not use SharedPreferences unless there is a strong technical reason.

---

# 14. KEYBOARD STATE

Create a proper keyboard state model.

Example:

KeyboardState:

- shiftEnabled
- capsLock
- currentLayout
- currentTheme
- currentPackageName
- language
- suggestionMode

Use a ViewModel/state architecture where appropriate.

Do not put all logic inside the composable UI.

Separate:

UI

Keyboard state

Input handling

Theme handling

Settings

Persistence

---

# 15. INPUT HANDLING

Implement:

Character input:

commitText()

Backspace:

deleteSurroundingText()

Enter:

sendKeyEvent() or appropriate InputConnection behavior

Space:

commitText(" ", 1)

Shift:

change keyboard letter case

Numbers:

switch to number layout

Symbols:

switch to symbol layout

Make sure the keyboard works with common Android text fields.

Test with:

- normal text fields
- password fields
- search fields
- multiline fields
- numeric fields

Respect EditorInfo.inputType where appropriate.

For example, if the field is numeric, consider displaying an appropriate numeric keyboard.

---

# 16. SUGGESTIONS

For MVP:

Create the architecture for suggestions but do NOT attempt to build a sophisticated AI/autocomplete engine.

Create:

SuggestionEngine

It can initially provide basic placeholder or dictionary-based suggestions.

Keep it modular so a future version can implement:

- word prediction
- autocorrection
- personalization
- multilingual suggestions

Do not send typed text to a server.

---

# 17. PRIVACY

This is extremely important.

The keyboard handles sensitive information.

Design Keyora as privacy-first.

Requirements:

- no cloud processing
- no analytics in MVP
- no advertising
- no sending typed text to external servers
- no storing passwords
- no storing sensitive text
- no network permission unless genuinely required

Add a clear privacy explanation inside settings.

For password fields:

Do not store or process entered password text for suggestions.

Disable suggestions for password fields.

Use Android EditorInfo/inputType appropriately.

---

# 18. PERMISSIONS

Minimize permissions.

Do NOT request:

- Accessibility Service
- root
- contacts
- SMS
- microphone unless microphone functionality is actually implemented
- unnecessary storage permissions

The app should function as a keyboard without invasive permissions.

---

# 19. KEYBOARD SETTINGS SCREEN

Create a polished settings screen with:

### Keyora

Keyboard

Appearance

Theme:
[System Default]

App Themes
Customize theme per application

Keyboard Layout

Language

About

Privacy

Open Source

---

# 20. ONBOARDING

When the app is first opened, show a simple onboarding:

Screen 1:

"Meet Keyora"

"An elegant, privacy-first keyboard for Android."

Screen 2:

"Personalize Every App"

"Choose a different keyboard theme for each app."

Screen 3:

"Private by Design"

"Your typed text stays on your device."

Then guide the user to enable Keyora in Android keyboard settings.

Do not automatically attempt to enable the keyboard.

Use the appropriate Android settings Intent.

---

# 21. KEYBOARD ENABLE/SELECT FLOW

Provide buttons:

Enable Keyora

Select Keyora as Keyboard

Check whether Keyora is currently enabled/selected where Android allows this to be determined.

Use appropriate system settings intents.

Do not attempt to change the default keyboard without user interaction.

---

# 22. APP THEME MANAGEMENT UX

Make this feature very easy to understand.

Example:

App Themes

"Choose how Keyora looks in each app."

[ + Add App ]

WhatsApp
🌙 Dark

Chrome
☀️ Light

Telegram
🌙 Dark

Gmail
System

When clicking an app:

Theme

○ System Default
○ Light
○ Dark

[Save]

---

# 23. OPTIONAL AUTOMATIC THEME DETECTION ARCHITECTURE

Do NOT implement unreliable screen scraping.

However, create the architecture so that future versions could support automatic theme detection.

Create an interface conceptually similar to:

ThemeDetector

Possible future implementations:

SystemThemeDetector

AppThemeDetector

ManualThemeDetector

For MVP:

ManualThemeDetector / AppRuleThemeDetector

should be the actual implementation.

Future automatic detection should be clearly separated.

---

# 24. ANIMATION

Add subtle animations:

- key press feedback
- theme transitions
- settings selection
- app rule changes

Do not over-animate.

The keyboard must remain fast and responsive.

Keyboard performance is more important than visual effects.

---

# 25. RESPONSIVE DESIGN

The keyboard must work on:

- small phones
- normal phones
- large phones
- landscape orientation

Do not hard-code one screen size.

Use responsive Compose layouts.

Make key widths dynamic.

Handle different aspect ratios.

---

# 26. PROJECT STRUCTURE

Use a clean architecture similar to:

app/

src/main/java/com/example/keyora/

keyboard/
    KeyoraInputMethodService.kt
    KeyboardController.kt
    KeyboardState.kt
    InputHandler.kt

keyboard/ui/
    KeyboardView.kt
    LetterKeyboard.kt
    NumberKeyboard.kt
    SymbolKeyboard.kt
    KeyButton.kt
    KeyboardTheme.kt

theme/
    ThemeManager.kt
    ThemeRuleManager.kt
    ThemeModels.kt

settings/
    SettingsActivity.kt
    SettingsViewModel.kt
    SettingsRepository.kt
    AppThemeScreen.kt
    AppearanceScreen.kt
    AppPickerScreen.kt

appdetector/
    AppContextDetector.kt

suggestions/
    SuggestionEngine.kt

onboarding/
    OnboardingActivity.kt

util/
    ...

Adjust the structure if a better Android architecture is appropriate.

---

# 27. ANDROID MANIFEST

Configure the InputMethodService correctly.

The IME service must include the appropriate:

android.permission.BIND_INPUT_METHOD

and:

android.view.InputMethod

service metadata.

Follow current Android documentation.

Do not add unnecessary permissions.

---

# 28. ACCESSIBILITY

Do NOT use AccessibilityService for keyboard functionality.

The keyboard must be implemented using the official InputMethodService API.

---

# 29. ERROR HANDLING

The keyboard must never crash because:

- current app cannot be determined
- an app was uninstalled
- theme rule references an uninstalled app
- InputConnection is unavailable
- editor information is unavailable

Use safe fallbacks.

Default fallback:

Global keyboard theme.

---

# 30. TESTING

Create unit tests for:

ThemeRuleManager

Examples:

No rule:

getThemeForPackage("com.test") → global theme

WhatsApp:

getThemeForPackage("com.whatsapp") → DARK

Chrome:

getThemeForPackage("com.android.chrome") → LIGHT

Remove rule:

returns global theme.

Also test:

- shift
- caps lock
- backspace
- enter
- space
- layout switching
- theme switching

---

# 31. README

Create a detailed README.md.

Include:

Project description

Features

Screenshots placeholder section

Architecture

How Android IME works

How to build

How to install on an Android device

How to enable the keyboard

How to configure per-app themes

Privacy

Limitations

Future roadmap

Contribution guide

---

# 32. IMPORTANT LIMITATIONS TO DOCUMENT

Clearly explain:

"Keyora does not currently detect whether another app is using Light Mode or Dark Mode internally."

Instead:

"Keyora allows users to configure the keyboard theme per application."

Example:

WhatsApp → Dark

Chrome → Light

This is intentional because Android does not provide a universal API that allows an IME to reliably read the private theme state of every application.

Do not claim automatic app-theme detection if it is not technically implemented.

---

# 33. FUTURE ROADMAP

Keep these as future features, not MVP requirements:

- automatic app theme detection where technically possible
- AI-powered suggestions
- advanced autocorrection
- multilingual support
- emoji search
- clipboard manager
- customizable key colors
- custom keyboard backgrounds
- haptic feedback controls
- sound controls
- swipe typing
- voice typing
- user-created themes
- theme scheduling

Do not implement these unless necessary for the MVP.

---

# 34. DEVELOPMENT APPROACH

IMPORTANT:

Do not generate a huge amount of code blindly.

Work incrementally.

First:

1. Create the Android project.
2. Configure Gradle.
3. Create the InputMethodService.
4. Make a minimal working keyboard.
5. Verify it can be enabled on a physical Android device/emulator.
6. Implement keyboard layouts.
7. Implement themes.
8. Implement global theme selection.
9. Implement current-app detection.
10. Implement per-app theme rules.
11. Implement the app picker.
12. Implement DataStore persistence.
13. Add polished UI.
14. Add testing.
15. Write documentation.

After each major step, make sure the project compiles.

If an Android API is uncertain, verify the correct modern API before implementing it rather than inventing an API.

---

# 35. CURSOR BEHAVIOR

You are working inside my Cursor workspace.

Before changing anything:

1. Inspect the existing project.
2. Determine whether an Android project already exists.
3. Reuse existing code where appropriate.
4. Do not overwrite useful existing work without checking it first.

If this is an empty directory, create the project from scratch.

After implementing each major feature:

- run Gradle build
- fix compilation errors
- check for deprecated APIs
- check Android manifest configuration
- check Kotlin/Compose errors

Prefer simple maintainable code over over-engineering.

Do not add dependencies unless necessary.

---

# 36. FINAL MVP ACCEPTANCE CRITERIA

The project is considered successful when:

### Keyboard

- Keyora can be installed on Android.
- Keyora appears in Android keyboard settings.
- User can enable Keyora.
- User can select Keyora as the default keyboard.
- Keyboard appears in WhatsApp/Chrome/Telegram/etc.
- Typing works.
- Backspace works.
- Enter works.
- Space works.
- Shift works.
- Number/symbol layouts work.

### Themes

- Global Light theme works.
- Global Dark theme works.
- System Default works.

### Per-App Themes

Example:

Global:
System Default

WhatsApp:
Dark

Chrome:
Light

Telegram:
Dark

Result:

WhatsApp → Dark keyboard

Chrome → Light keyboard

Telegram → Dark keyboard

Other apps → System Default

### Persistence

Close/reopen the app.

Rules remain.

Restart the phone.

Rules remain.

### Privacy

No typed text is sent to a server.

No unnecessary permissions are requested.

No AccessibilityService is required.

---

# MOST IMPORTANT PRODUCT PRINCIPLE

The application should feel like a **real polished keyboard**, not a programming demo.

Prioritize:

1. Reliable typing
2. Fast keyboard response
3. Clean UI
4. Privacy
5. Simple per-app theme customization
6. Maintainable architecture

Do not sacrifice keyboard reliability for visual effects.

Start by inspecting the workspace and then implement the project incrementally, beginning with the real Android InputMethodService.