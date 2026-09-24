# Gothwad Manager

**Gothwad Manager** is a fast, powerful, and lightweight all-in-one File Explorer and Manager built for Android devices, tablets, foldables, and Android TV.

---

## 🌟 Key Features

- **📁 Complete File Operations**: Cut, copy, paste, delete, rename, compress, extract, and search files across internal storage, SD cards, and USB OTG.
- **⚡ Super Lightweight & Fast**: Designed to deliver maximum performance with minimal system resource consumption.
- **🖥️ Android TV & Leanback Support**: Fully optimized for TV screens with clean directional-pad (D-pad) navigation and custom banners.
- **🌐 Network & FTP Server**: Transfer files seamlessly between your phone and PC over Wi-Fi without cables using the built-in FTP/FTPS server.
- **📱 App & Process Management**: Inspect installed applications, back up APKs, manage storage, and uninstall apps in batch.
- **🔒 Security & Privacy**: Secure your sensitive files with built-in PIN protection and granular access controls.
- **💾 Root & USB Storage**: Advanced explorer for rooted devices and direct support for USB mass storage devices (FAT/FAT32/exFAT).
- **🎨 Modern Material Theme**: Customizable primary and accent colors with full Dark Mode and RTL (Right-to-Left) layout support.

---

## 🛠️ Project Structure & Architecture

```
├── app/
│   ├── src/main/
│   │   ├── java/com/gothwad/manager/     # Application source code
│   │   │   ├── adapter/                  # RecyclerView adapters
│   │   │   ├── fragment/                 # UI fragments (Home, Directory, Server, etc.)
│   │   │   ├── model/                    # Data models and DocumentsContract
│   │   │   ├── provider/                 # Document & Storage content providers
│   │   │   ├── receiver/                 # Broadcast receivers
│   │   │   ├── service/                  # FTP server & background services
│   │   │   ├── setting/                  # Settings and preferences
│   │   │   └── ui/                       # Custom views and Material widgets
│   │   ├── res/                          # Resources (layouts, drawables, English values, mipmaps)
│   │   └── AndroidManifest.xml           # App manifest with modern storage & TV permissions
│   └── build.gradle                      # App-level build script (com.gothwad.manager)
├── .github/
│   └── workflows/
│       ├── build_apk.yml                 # Automated workflow for APK and AAB builds
│       └── release.yml                   # Automated GitHub Releases on version tags
├── gradle/                               # Gradle wrapper and configurations
├── build.gradle                          # Root Gradle configuration
├── settings.gradle                       # Root project settings
└── metadata.json                         # Project metadata
```

---

## 🚀 Building & Running

### Prerequisites
- JDK 17
- Android SDK 36 (Build Tools 36.0.0, Min SDK 24)

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Build Play Store Android App Bundle (AAB)
./gradlew bundleRelease
```

---

## 📦 CI/CD Workflows

The repository includes pre-configured GitHub Actions workflows in `.github/workflows/`:
- **`build_apk.yml`**: Triggers on push or manual dispatch to compile signed Debug and Release APKs and Play Store AAB bundles.
- **`release.yml`**: Generates automated releases with version tagging and artifact uploads.

---

## 📄 Package Identity

- **Application ID**: `com.gothwad.manager`
- **Default Language**: English
- **Supported Formats**: ZIP, TAR, RAR, APK, Images, Audio, Video, Documents
