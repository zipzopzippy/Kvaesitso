# Modified version

This is a **modified version of Kvaesitso** for personal use. It is not made or endorsed by the
Kvaesitso developers. The original is at https://github.com/MM2-0/Kvaesitso and is licensed under
the [GNU General Public License v3](LICENSE.txt), which also covers this version.

## Changes from Kvaesitso

- Personal strings live in `app/ui/src/main/res/values/strings_personal.xml`, not upstream's
  `strings.xml`, so translations and upstream string changes merge cleanly.
- A `personal` build flavor (`assemblePersonalRelease`): app ID `de.mm20.launcher2.personal`,
  named "Kvaesitso Personal", version suffix `-personal-<commit>`. Installs next to the official
  app. Release builds are signed with the local debug key.
