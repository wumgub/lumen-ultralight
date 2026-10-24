# Lumen Ultralight

A stripped-down, **native** Android build of [Lumen](https://github.com/wumgub/lumen-player)
for older and lower-powered phones — Android 7.1.1 and up.

The regular [Android app](https://github.com/wumgub/lumen-android) is a web interface
in a WebView, which is fine on a recent phone but heavy on an old one. Ultralight is a
ground-up rewrite in Kotlin and Jetpack Compose: no WebView, no visualizer, native
playback. It keeps the library, playlists, listening stats and the full theming — just
light enough to feel instant on a phone that's been around the block.

> **Status: early.** This is being built in the open, phase by phase. The first
> releases are skeletons that prove the build; the player grows from there.

## Installing

Grab the latest `Lumen-Ultralight-*.apk` from the
[releases page](https://github.com/wumgub/lumen-ultralight/releases/latest) and open it
on the phone. Android warns about installing from outside the Play Store, and since it
isn't signed with a paid developer certificate you may get an extra "unknown app"
prompt — both are normal for a sideloaded build. It installs alongside the regular
Lumen app, not over it.

## Building it

No local Android SDK needed. Pushing a version tag builds the APK in GitHub Actions and
attaches it to the release:

```bash
git tag v0.0.1
git push --follow-tags
```

You can also run the **Build APK** workflow by hand from the Actions tab to get an APK
artifact without cutting a release.

## License

GPL-3.0-or-later, same as the rest of Lumen.
