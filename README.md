<div align="center">
  <img src="https://raw.githubusercontent.com/blabby-cn/UniHub/main/app/src/main/res/mipmap-anydpi-v26/ic_launcher.png" alt="UniHub Logo" width="120" />

  # UniHub

  **Dual-pane File Manager & Web Browser for Android**

  [![License](https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square)](https://github.com/blabby-cn/UniHub/blob/main/LICENSE)
  [![Platform](https://img.shields.io/badge/platform-Android-brightgreen?style=flat-square&logo=android)](https://www.android.com)
  [![Min SDK](https://img.shields.io/badge/minSDK-21-ff69b4?style=flat-square)](https://developer.android.com/about/versions/lollipop)
  [![Target SDK](https://img.shields.io/badge/targetSDK-34-green?style=flat-square)](https://developer.android.com/about/versions/14)
  [![Version](https://img.shields.io/badge/version-1.0.2-orange?style=flat-square)](https://github.com/blabby-cn/UniHub/releases)
  [![Stars](https://img.shields.io/github/stars/blabby-cn/UniHub?style=flat-square)](https://github.com/blabby-cn/UniHub/stargazers)
  [![GitHub release](https://img.shields.io/github/v/release/blabby-cn/UniHub?style=flat-square)](https://github.com/blabby-cn/UniHub/releases)
  [![GitHub last commit](https://img.shields.io/github/last-commit/blabby-cn/UniHub?style=flat-square)](https://github.com/blabby-cn/UniHub/commits/main)
  [![Language](https://img.shields.io/badge/language-Java-ED8B00?style=flat-square&logo=openjdk)](https://www.java.com)
  [![Build](https://img.shields.io/badge/build-Gradle-02303A?style=flat-square&logo=gradle)](https://gradle.org)
  [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](https://github.com/blabby-cn/UniHub/blob/main/CONTRIBUTING.md)

  <hr />
</div>

## Overview

**UniHub** is a multi-purpose file management and browsing tool for Android, combining local file management, remote connectivity, and web browsing in one app.

It features a **dual-pane local file browser**, **FTP/SFTP remote file access**, a **multi-tab WebView browser**, **text & Markdown editors**, a **video player**, **ZIP archive browser**, and supports **14 languages** for the UI.

## Features

<details open>
<summary><b>📂 File Management</b></summary>

- Dual-pane file browsing (master / slave panel)
- File operations: copy, move, rename, delete
- File compression (ZIP / XZ formats)
- Storage space status display
- File search, filter, sort (reserved)
- Toggle hidden files
- Bookmark management (reserved)
- File download via OkHttp
- File sharing
</details>

<details open>
<summary><b>🌐 Remote Connectivity</b></summary>

- **FTP client** — powered by Apache Commons Net
- **SFTP client** — powered by JSch (Java Secure Channel)
- Remote file browsing and navigation
- Account management with persistent login storage
</details>

<details open>
<summary><b>🌍 Web Browsing</b></summary>

- WebView-based browser
- Multi-tab support
- Forward / Back navigation
- Manual URL input in address bar
</details>

<details open>
<summary><b>✏️ Text Tools</b></summary>

- **Text Editor** — plain text file editing
- **Markdown Editor** — formatted Markdown editing
- Code block editing support
</details>

<details open>
<summary><b>🎬 Media & Archives</b></summary>

- **Video Player** — built-in video playback
- **ZIP Browser** — inspect ZIP archive contents
- **Document Viewer** — view user agreement, privacy policy, changelog, third-party licenses
</details>

<details open>
<summary><b>🎨 User Experience</b></summary>

- **Sidebar Navigation** — full-screen drawer menu with slide animation
- **Light & Dark Themes** — toggle between light and dark color schemes
- **Multi-language Support** — 14 UI languages
- **Blabby Account System** — login, logout, profile avatar loading
- Material Design UI
</details>

## Multi-language Support

UniHub ships with built-in internationalization. The following languages are currently available:

| Language | Code | Native Name |
|:--------:|:----:|:-----------:|
| Chinese (Simplified) | `zh_cn` | 简体中文 |
| Chinese (Traditional) | `zh_tw` | 繁體中文 |
| English | `en` | English |
| Japanese | `ja` | 日本語 |
| Korean | `ko` | 한국어 |
| Malay | `ms` | Bahasa Melayu |
| Burmese | `my` | မြန်မာ |
| German | `de` | Deutsch |
| French | `fr` | Français |
| Russian | `ru` | Русский |
| Ukrainian | `uk` | Українська |
| Danish | `da` | Dansk |
| Czech | `cs` | Čeština |
| Greek | `el` | Ελληνικά |

> Language files live in `assets/languages/*.yaml`. PRs adding new languages are welcome!

## Tech Stack

| Category | Technology |
|:---------|:-----------|
| Language | Java |
| Build System | Gradle (AGP 8.11.0) |
| Min SDK | API 21 (Android 5.0) |
| Target SDK | API 34 (Android 14) |
| UI | AndroidX AppCompat, Material Design, ConstraintLayout |
| Networking | OkHttp 4, Apache Commons Net, JSch |
| Image Loading | Glide 4.16 |
| Compression | XZ (Tukaani) |
| Navigation | DrawerLayout, ViewPager2 |
| Testing | JUnit, Espresso |

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 34

### Clone & Build

```bash
git clone https://github.com/blabby-cn/UniHub.git
cd UniHub
./gradlew assembleDebug
```

The debug APK will be generated at `app/build/outputs/apk/debug/`.

> **Note**: Release builds require a valid signing certificate. Configure `signingConfigs.release` in `app/build.gradle` before building.

## Project Structure

```
UniHub/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/Blabby/Co/UniHub/
│   │       │   ├── MainActivity.java        # Main activity
│   │       │   ├── SettingsActivity.java     # Settings
│   │       │   ├── DocumentActivity.java     # Document viewer
│   │       │   ├── VideoSee.java            # Video player
│   │       │   ├── ZipLiulan.java           # ZIP browser
│   │       │   ├── WenbenBianjiqi.java      # Text editor
│   │       │   ├── GeshihuaBianji.java      # Markdown editor
│   │       │   ├── MyApplication.java       # Application class
│   │       │   ├── data/
│   │       │   │   └── model/
│   │       │   │       ├── FileItem.java
│   │       │   │       └── RemoteFileEntry.java
│   │       │   ├── network/
│   │       │   │   ├── FtpClient.java       # FTP client
│   │       │   │   └── SftpClient.java      # SFTP client
│   │       │   ├── ui/
│   │       │   │   ├── adapters/
│   │       │   │   │   ├── FileListAdapter.java
│   │       │   │   │   └── RemoteFileListAdapter.java
│   │       │   │   ├── dialogs/
│   │       │   │   │   └── LoginDialog.java
│   │       │   │   └── fragments/
│   │       │   │       ├── FileBrowserFragment.java
│   │       │   │       ├── RemoteFileBrowserFragment.java
│   │       │   │       └── WebBrowserFragment.java
│   │       │   └── util/
│   │       │       ├── AccountManager.java
│   │       │       ├── FileOperations.java
│   │       │       ├── FileUtils.java
│   │       │       ├── Localization.java    # i18n engine
│   │       │       └── PathParser.java
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle                              # Root build script
├── settings.gradle
├── gradle.properties
└── gradlew
```

## License

This project is licensed under the [GNU General Public License v3.0 (GPL-3.0)](https://github.com/blabby-cn/UniHub/blob/main/LICENSE).

---

<div align="center">
  <sub>Built with ❤️ by <a href="https://github.com/blabby-cn">Blabby.Co Studio</a></sub>
  <br />
  <sub>Copyright ©️ 2026 Blabby.Co Studio</sub>
</div>
