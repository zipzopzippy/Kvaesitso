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
