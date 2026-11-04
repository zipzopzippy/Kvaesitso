# Modified version

This is a **modified version of Kvaesitso** for personal use. It is not made or endorsed by the
Kvaesitso developers. The original is at https://github.com/MM2-0/Kvaesitso and is licensed under
the [GNU General Public License v3](LICENSE.txt), which also covers this version.

## Changes from Kvaesitso

- **Keyboard filter bar moves with the keyboard.** Upstream only enables it once the drawer has fully
  settled and animates its space on a separate spring, so the search UI jumps when the keyboard
  opens or closes. Here it follows the keyboard's position every frame (`LauncherScaffold.kt`).
- **Search bar menu keeps the keyboard open.** Upstream's menu popup takes input focus, so Android
  drops the keyboard without animating and the search UI jumps. Here the popup isn't focusable
  (`SearchBarMenu.kt`).
- **Filters for disabled search types are hidden** in the filter panel, the keyboard filter bar and
  the default filter settings (`AvailableSearchFilters.kt`).
- **Dock position** setting (Settings > Home screen, below "Dock rows"):
  - *Middle of the screen* (default): the dock sits below the clock. In clock fill-height mode
    it is centered vertically.
  - *Above search bar*: the dock is pinned just above the search bar and home screen widgets
    scroll above it (`ClockAndWidgetsHomeComponent.kt`).
- **Themed icons on the home screen only** (Settings > Icons > "Home screen only"): the dock and
  home screen widgets use themed icons, the app drawer, search and settings don't. `IconService`
  keeps a second, unthemed provider set; grid/list items on the home page (`LocalScaffoldPage`)
  request the themed set. Long-press popups of dock icons and the separate widgets page count as
  "not home" and show unthemed icons.
- **Discreet profile switcher** (Settings > Search > Apps): the Personal / Work / Private space
  tabs become small right-aligned icon buttons without labels, and the "Lock" button becomes a
  lock icon in the same row (`ui/launcher/search/apps/AppResults.kt`).
- **Device settings in search**: system settings pages (Wi-Fi, Bluetooth, Display, Battery,
  Developer options, ...) show up in the Shortcuts search results with the Settings app's icon
  and can be pinned. Added as a new shortcut type in
  `data/appshortcuts/.../DeviceSettingsShortcut.kt` with its own strings file
  (`device_settings_strings.xml`); `AppShortcutRepository.search` and the module's DI setup
  gained a few lines. Pages that don't exist on the device are left out, and turning off the
  shortcut search turns these off too.
- **Clock theme colour in dark mode**: with dark clock colours, "Use theme colors" now uses the
  theme colour at tone 30 (dark navy for a blue theme) in dark mode too (upstream used
  `primaryContainer`, which barely differed from the default clock colour). One line in
  each of the Analog, Binary, Digital 1/2 and Segment clocks.
- **More currencies**: the ECB only publishes ~30 exchange rates, so the rest (e.g. TWD) are
  fetched from open.er-api.com (free, no key) when currency conversion is on. ECB rates always
  take priority. New file `data/currencies/.../SupplementaryExchangeRates.kt`, one line in
  `ExchangeRateWorker`. Note: this is one extra daily network request to a third party.
- **Clock text shadow** (clock widget settings, on by default): the clock digits get a shadowed
  copy of the typography (`ClockTextShadow.kt`), so every clock style gets the shadow without
  touching each clock. Dark clock colours get a light glow instead. Text below the clock is not
  shadowed.
- **Compact clock parts**: in the vertical layout, the date, music, alarm and battery parts use two
  shared styles (`ClockPartText.kt`): a slightly heavier main style and a lighter accent style,
  with less padding. Music shows a small music icon (a play icon for apps that aren't music players) and "Title · Artist" on one line, scrolling
  (marquee) when it doesn't fit. The date and alarm replace
  upstream's `TextButton` with a plain clickable row (upstream code left in place, unused).
- **Plain black / white clock** without "Use theme colors": the clock and its text are black (dark
  clock colours) or white instead of a dark / light shade of the theme colour, so the toggle makes
  a visible difference.
- **No music playback controls** below the clock in the vertical layout; tapping the title still
  opens the player (`ShowMusicControls` in `MusicPartProvider.kt`).
- **Music part only shows while playing**: upstream also keeps it up while paused; here a paused
  player falls back to the next part, e.g. the date (`getRanking` in `MusicPartProvider.kt`).
- **Media apps are selected by hand**: upstream includes every music player unless you untick it;
  here only the apps ticked in Media control count, whatever kind of app they are (`MusicService.kt`,
  `MediaIntegrationSettingsScreenVM.kt`). If several have a session, whichever is playing wins, else
  the newest.
- **Next event under the date**: in the vertical layout, the date part also shows today's next
  timed event ("Name · 3:00 pm"), or an all-day event ("Name") when no timed event is left. Uses
  the calendars enabled for search, without tasks (`TodayEvent` in `DatePartProvider.kt`).
- **Tapping the battery part** under the clock opens the battery settings.
- **Fix: clock and its parts (date, alarm, music) couldn't be tapped** with the dock pinned above
  the search bar, because the dock's full-screen layer swallowed the taps. The column and the
  dock now share one container that owns the home screen's tap handling.
- Personal strings live in `app/ui/src/main/res/values/strings_personal.xml`, not upstream's
  `strings.xml`, so translations and upstream string changes merge cleanly.
- **GitHub Actions release workflow** (`.github/workflows/personal-release.yml`): every push to
  `personal` builds `assemblePersonalRelease` and publishes it as a release numbered
  incrementally (`personal-004`, version `1.41.0-personal.004`), with the commit subjects since
  the previous release as notes. Local builds use the commit (`-personal-<commit>`) instead. Needs the repository secret `DEBUG_KEYSTORE`
  (base64 of the PC's `~/.android/debug.keystore`) so builds keep updating existing installs.
- A `personal` build flavor (`assemblePersonalRelease`): app ID `de.mm20.launcher2.personal`,
  named "Kvaesitso Personal", version suffix `-personal-<commit>`. Installs next to the official
  app. Release builds are signed with the local debug key.
