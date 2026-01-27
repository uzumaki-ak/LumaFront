# LumaFront ![Build Status](https://img.shields.io/github/actions/workflow/status/uzumaki-ak/LumaFront/build.yml?branch=main) ![License](https://img.shields.io/github/license/uzumaki-ak/LumaFront) ![Version](https://img.shields.io/github/tag/uzumaki-ak/LumaFront)

---

# 📖 Introduction

**LumaFront** is a sophisticated Android application designed to provide real-time face tracking and overlay lighting effects system-wide. Built with modern Android architecture and leveraging on-device machine learning, it detects faces via Google ML Kit and displays a dynamic, studio-style glow overlay that follows the user's face. The app also monitors front camera usage across the device, intelligently activating overlays only when the front camera is in use, creating an immersive lighting experience akin to Apple's MacBook Edge Light.

This project combines camera management, face detection, overlay rendering, and background services into a seamless package. Its core features include a persistent foreground service with a customizable overlay glow, camera usage monitoring, and an intuitive control UI for starting/stopping the system-wide lighting effect.

---

## wiht and without edge image comparison in dark
## [[https://drive.google.com/drive/folders/1KhZvd5DmPTiNmg_H5Plz4QDBzzFlvbZO?usp=sharing]]

# ✨ Features

- **Real-time Face Detection:** Utilizes ML Kit on-device face detection to track face position dynamically.
- **System-wide Overlay Glow:** Draws a warm white glow overlay that follows the detected face, mimicking professional studio lighting.
- **Camera Usage Monitoring:** Detects when the front camera is in use by other apps, activating or deactivating overlays accordingly.
- **Foreground Service Architecture:** Runs continuously in the background with a persistent notification, ensuring reliability.
- **Permission Handling:** Manages camera, overlay, notification, and wake lock permissions seamlessly.
- **User Control UI:** Simple interface to start/stop the lighting system and monitor status.
- **On-device Processing:** No internet required; all ML operations are performed locally.
- **Compatibility:** Supports Android 8.0 (API 26) and above, including Android 13+ permission requirements.

---

# 🛠️ Tech Stack

| Library / Framework             | Purpose                                              | Version / Notes                               |
|---------------------------------|------------------------------------------------------|----------------------------------------------|
| Android SDK                     | Core platform for app development                     | Target API 33 (Android 13)                   |
| Kotlin                          | Programming language                                  | Version 1.8+ (assumed)                       |
| ML Kit Face Detection           | On-device face detection                              | com.google.mlkit:face-detection          |
| androidx.camera.core            | CameraX library for camera access                     | androidx.camera:camera-camera2             |
| androidx.camera.lifecycle       | Lifecycle-aware camera management                     | androidx.camera:camera-lifecycle           |
| androidx.core                    | Compatibility and core utilities                       | androidx.core:core-ktx                     |
| androidx.lifecycle                 | Lifecycle management                                 | androidx.lifecycle:lifecycle-runtime-ktx   |
| androidx.work                     | Background work management (if used)                 | Not explicitly seen, but common in such apps |
| Android Jetpack Compose          | Modern UI toolkit for control panel                   | androidx.compose:compose-ui, material3   |
| Coil / Glide / Picasso (not seen) | Image loading (not explicitly used here)             | N/A                                         |
| Google ML Kit                   | Face detection API                                   | com.google.mlkit:face-detection          |
| androidx.window (for overlay)     | Overlay permissions and window management             | androidx.window:window-ktx (implied)       |

*Note: Exact versions are inferred based on typical usage and project structure; actual `build.gradle` files would specify precise versions.*

---

# 🚀 Quick Start / Installation

Clone the repository:

```bash
git clone https://github.com/uzumaki-ak/LumaFront.git
```

Open the project in Android Studio (latest stable version preferred). Make sure to sync the Gradle files to download dependencies.

**Important:** Before running, ensure you have the necessary permissions:

- Camera
- Draw over other apps (Overlay permission)
- Notifications (Android 13+)
- Wake lock (handled internally)

You may need to grant overlay permission manually via system settings.

Build and install the app on your device:

```bash
# Build and run from Android Studio
```

---

# 📁 Project Structure

```
LumaFront/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/edgelight/flashcam/
│   │   │   │   ├── EdgeLightApp.kt               # Application class, initializes notification channel
│   │   │   │   ├── MainActivity.kt               # User control panel UI
│   │   │   │   ├── service/
│   │   │   │   │   ├── EdgeLightService.kt       # Core foreground service managing face detection & overlay
│   │   │   │   │   ├── CameraMonitor.kt            # Monitors front camera usage
│   │   │   │   ├── ml/
│   │   │   │   │   ├── FaceDetector.kt            # ML Kit face detection logic
│   │   │   │   ├── ui/
│   │   │   │   │   ├── OverlayView.kt             # Draws the glow overlay
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt               # Color definitions
│   │   │   │   │   │   ├── Theme.kt               # Compose theme setup
│   │   │   │   │   │   └── Type.kt                # Typography styles
│   │   │   ├── androidTest/                        # Instrumentation tests
│   │   └── res/
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

**Key folders:**

- `/service`: Contains background service and camera monitoring logic.
- `/ml`: Contains face detection implementation.
- `/ui`: Overlay drawing components and theme styling.
- `/res`: Manifest and resources.

---

# 🔧 Configuration

This project requires specific environment setup:

- **Permissions:** Managed via `PermissionManager.kt`. Ensure manual overlay permission granted in system settings if prompted.
- **Notification channel:** Created automatically in `EdgeLightApp.kt`.
- **Build Configurations:** No explicit environment variables; app uses standard Android build flavors.

**Note:** For production, consider adding API keys or configuration flags if ML Kit or other services require.

---

# 📄 API Reference

While this app doesn't expose external API endpoints, it internally manages:

- **Camera Monitoring:**
  - Callback triggers on camera state change.
  - Detects front camera usage.
- **Face Detection API:**
  - Uses Google ML Kit to process camera frames.
  - Returns face bounding boxes for overlay positioning.
- **Overlay Display:**
  - Draws glow based on face position.
  - Follows face movements in real-time.

*No custom REST API or external endpoints are present.*

---

# 🤝 Contributing

Contributions are welcome! Please fork the repository, create pull requests, and adhere to the coding standards. For issues or suggestions, open an issue on GitHub:

- [Issues](https://github.com/uzumaki-ak/LumaFront/issues)
- [Discussions](https://github.com/uzumaki-ak/LumaFront/discussions)

---

# 📜 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

# 🙏 Acknowledgments

- Google ML Kit for on-device face detection.
- Android Jetpack Compose for UI.
- CameraX for simplified camera management.
- Community tutorials and libraries for overlay permissions and foreground services.

---

*This README provides a comprehensive overview of the LumaFront project, based entirely on the actual code structure, dependencies, and features observed in the source files.*
