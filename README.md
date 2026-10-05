# Memento

Memento is a private, on-device memory assistant for Android. Record things you
want to remember, then ask Memento where something was last recorded or what
you promised to do.

## Download for Android

The installable APK is published on the project's
[GitHub Releases page](https://github.com/blacksmoke39364-coder/memento/releases).
Download `Memento-debug.apk` from the latest release on an Android device, open
it, and approve the Android installation prompt if asked.

The published APK is a debug-signed build intended for direct testing. Android
may require you to allow installations from the browser or file manager used to
open it.

## Features

- Keeps memories in the app's local database.
- Records items, people, promises, waiting items, and places.
- Answers memory questions using recorded evidence.
- Falls back to on-device parsing and retrieval when no Gemini API key is
  configured, so the app remains usable after installation.

## Build locally

Open the project in Android Studio with Android SDK Platform 36 installed, then
run the `app` configuration. To build an APK from the command line:

```powershell
gradle assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`.

## Create a public release

Push a version tag such as `v1.0.0`. GitHub Actions builds the APK and creates
a matching GitHub Release with `Memento-debug.apk` attached. This makes the
download available directly from the Releases page.

## Optional Gemini key

Copy `.env.example` to `.env` and set `GEMINI_API_KEY` to enable Gemini-backed
responses. Do not commit `.env`; it is intentionally ignored by Git.
