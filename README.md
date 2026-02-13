<div align="center">
<a id="top"></a>
  
# VEXT_APP - <br> An Advanced Chat & Video Communication Platform

## [App is Live!](https://github.com/kushalvachar2006/VEXT_APP/releases/tag/v1.0)

[About](#about-the-app) •
[Features](#core-features) •
[System Architecture](#system-architecture) •
[Tech Stack](#tech-stack) •
[Project Structure](#project-structure) •
[Installation](#installation--setup) •
[Architecture Details](#architecture-details) •
[Contributing](#-contributing) •
[MIT License](#-license)
</div>
A feature-rich Android chat application with real-time messaging, video/audio calling, and media sharing capabilities. Built with modern Android architecture and powered by Firebase backend services.

---

## <a id="about-the-app"></a> About the App

VEXT_APP is a comprehensive communication platform designed to connect users seamlessly through multiple channels:

### <a id="core-features"></a> Core Features

- **User Authentication**
  - Google Sign-In integration with secure OAuth 2.0
  - Email-based registration and login
  - Phone number verification support
  - Terms and Conditions acceptance flow

- **Real-Time Messaging**
  - Instant text messaging between users
  - Message persistence with Firebase Firestore & Realtime Database
  - Push notifications via Firebase Cloud Messaging (FCM)
  - Notification click handling for seamless user experience

- **Voice & Video Calling**
  - HD video calling capabilities using WebRTC
  - High-quality audio calling
  - Full-screen incoming call notifications (Android 15 compatible)
  - Call state management with proper lifecycle handling
  - Audio recording and microphone access

- **Contact Management**
  - Contact list integration and viewing
  - User profile management with customizable profiles
  - Full-screen profile view with image display
  - Contact synchronization from device

- **Media Handling**
  - Image upload and sharing in conversations
  - Image cropping tool (uCrop) with portrait orientation support
  - Photo view gallery with PhotoView library
  - Media file permissions (images, videos, audio)
  - Efficient image loading with Glide

- **Advanced Notifications**
  - Firebase Cloud Messaging integration
  - Notification channels with chat_notifications default
  - Full-screen intent support for incoming calls
  - Lock screen call notifications
  - Custom notification handling

- **User Experience**
  - Splash screen with Material Design
  - Responsive UI with ConstraintLayout

---

## <a id="system-architecture"></a> System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          VEXT_APP Architecture                          │
└─────────────────────────────────────────────────────────────────────────┘

                          ┌──────────────────┐
                          │   Android Client │
                          │   (VEXT_APP)     │
                          └────────┬─────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
            ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
            │  Firebase   │  │   Storage    │  │   WebRTC     │
            │  Services   │  │   Server     │  │   Signaling  │
            └─────────────┘  └──────────────┘  └──────────────┘
                    │              │              │
            ┌───────┴───────┐      │      ┌───────┴───────┐
            │               │      │      │               │
            ▼               ▼      ▼      ▼               ▼
        ┌─────────┐    ┌──────┐  ┌──────────┐    ┌────────────┐
        │  Auth   │    │FCM   │  │Express.js│    │ Peer-to-   │
        │ Service │    │Push  │  │Multer    │    │ Peer Audio │
        └─────────┘    │Notif │  │CORS      │    │ Video Data │
                       └──────┘  └──────────┘    │ Channels   │
        ┌─────────┐                              └────────────┘
        │Firestore│              │
        │Real-time│         ┌────┴─────┐
        │Database │         │Uploads   │
        └─────────┘         │Directory │
                            │(Temp)    │
                            └──────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                        Communication Flows                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. USER AUTHENTICATION                                                 │
│     Client ──(Google OAuth)──> Firebase Auth ──> Client Session         │
│                                                                         │
│  2. REAL-TIME MESSAGING                                                 │
│     Client A ──(Text Message)──> Firebase Firestore <── Client B        │
│     Sender ──(FCM Notification)──> Firebase Cloud Messaging             │
│     Firebase ──(Push Notification)──> Receiver                          │
│                                                                         │
│  3. MEDIA SHARING                                                       │
│     Client ──(Upload File)──> Storage Server ──> Temporary Upload Dir   |
│     Storage Server ──(Download URL)──> Client                           │
│     Receiver ──(Download)──> Storage Server ──(Auto-Delete)──> Privacy  │
│                                                                         │
│  4. VOICE/VIDEO CALLING                                                 │
│     Client A ──(Signaling)──> Firebase Firestore (Call Signal)          │
│     Firebase ──(FCM Notification)──> Client B (Incoming Call)           │
│     Client B ──(Accept/Reject)──> Firebase Firestore                    │
│     Client A ⟷ (WebRTC P2P Connection) ⟷ Client B                     |
│     ├─ Audio/Video Streams (Direct Peer Connection)                     │
│     ├─ NAT Traversal (STUN/TURN Servers)                                │
│     └─ Real-time Data Channels (Low Latency)                            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## <a id="tech-stack"></a> Tech Stack

### Frontend (Android Native)
- **Language**: Java (99.3% of codebase)
- **Build System**: Gradle with Kotlin DSL
- **Minimum SDK**: Android 7.1 (API 24)
- **Target SDK**: Android 14 (API 34)

### Core Libraries & Dependencies
- **Firebase Suite** (v32.8.1)
  - Firebase Authentication (email, phone, Google Sign-In)
  - Firebase Firestore (real-time database)
  - Firebase Realtime Database
  - Firebase Cloud Messaging (FCM)

- **UI & Material Design**
  - AndroidX AppCompat
  - Material Design Components
  - ConstraintLayout

- **Real-Time Communication**
  - **WebRTC ([Notes on WebRTC](https://www.notion.so/Notes-on-WebRTC-305b84f94df980e088bcf7ae1f2d9323?source=copy_link))** - Advanced peer-to-peer communication framework
    - HD video calling with adaptive bitrate streaming
    - High-quality audio codec support
    - NAT traversal and firewall piercing via STUN/TURN servers
    - Low-latency real-time data channel support
    - Hardware-accelerated video encoding/decoding
    - For more details, refer to

- **Media & Image Processing**
  - Glide 4.16.0 - Image loading and caching
  - uCrop 2.2.8 - Advanced image cropping
  - PhotoView 2.3.0 - Interactive image gallery

- **Authentication & Security**
  - Google Play Services Auth 21.4.0
  - Google ID Tokens 1.1.1

- **API & Networking**
  - OkHttp - HTTP client for REST API calls

### Backend

#### Cloud Infrastructure
- **Render**: Primary backend hosting (configured via RENDER_LINK)
- **API Integration**: RESTful APIs with OkHttp
- **Authentication**: Google OAuth 2.0 integration

#### Temporary Storage Server (Node.js)
- **Purpose**: Ephemeral file transfer service for media sharing between users
- **Technology Stack**:
  - Express.js - RESTful API framework
  - Multer - File upload handling (50MB file size limit)
  - CORS - Cross-origin resource sharing support
- **Key Features**:
  - Upload endpoint (`/upload`) - Users upload files with unique timestamp-based naming
  - Download endpoint (`/download/:filename`) - Receivers download and automatically delete files after transfer
  - Automatic cleanup - Files are automatically deleted after receiver downloads to maintain privacy
  - Health check endpoint (`/`) - Server status monitoring
- **Deployment**: Configured to run on port 10000 (customizable via environment variables)
- **Use Case**: Enables temporary, secure file sharing without permanent storage overhead

### Development & Testing
- JUnit - Unit testing
- AndroidX Test - Instrumented testing
- Espresso - UI testing framework

---

## <a id="project-structure"></a> Project Structure

```
VEXT_APP/
├── app/                          # Main Android application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/            # Application source code (Java)   
│   │   │   ├── res/             # Resources (layouts, drawables, strings)
│   │   │   │   ├── layout/      # XML layout files
│   │   │   │   ├── drawable/    # Image assets
│   │   │   │   ├── values/      # String, color, dimension resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                # Unit tests
│   │   └── androidTest/         # Instrumented tests
│   └── build.gradle.kts         # App-level build configuration
├── storage-server/              # Temporary file transfer server (Node.js)
│   ├── server.js               # Express server implementation
│   ├── package.json            # Node.js dependencies
│   ├── package-lock.json       # Dependency lock file
│   └── uploads/                # Temporary file storage directory
├── build.gradle.kts             # Root build configuration
├── settings.gradle.kts          # Gradle settings
├── gradle.properties            # Gradle properties
├── README.md                    # Project documentation
└── gradle/                      # Gradle wrapper and plugins

```

---

## <a id="installation--setup"></a> Installation & Setup

### Prerequisites
- Android Studio (Arctic Fox or newer)
- Java Development Kit (JDK 8+)
- Android SDK with API 34 installed
- Node.js 18+ (for storage server)
- Git

### Step 1: Clone the Repository
```bash
git clone https://github.com/kushalvachar2006/VEXT_APP.git
cd VEXT_APP
```

### Step 2: Configure Local Properties
Create a `local.properties` file in the project root:

```properties
# Firebase Web Client ID (from Google Cloud Console)
web_client_id=YOUR_WEB_CLIENT_ID

# Google Maps API Key
API_KEY_MAINFEST=YOUR_GOOGLE_MAPS_API_KEY

# Backend API URL
RENDER_LINK=YOUR_RENDER_BACKEND_URL

# Storage Server URL (optional, for local/custom server)
STORAGE_SERVER_URL=http://localhost:10000
```

### Step 3: Set Up Firebase
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or select existing
3. Add Android app with package name: `com.example.chat_application`
4. Download `google-services.json`
5. Place it in the `app/` directory

### Step 4: Configure Google OAuth
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create OAuth 2.0 credentials for Android
3. Add your app's signing certificate SHA-1 fingerprint
4. Update `web_client_id` in local.properties

### Step 5: Set Up Storage Server (Optional - for local development)
```bash
# Navigate to storage server directory
cd storage-server

# Install dependencies
npm install

# Start the server
npm start
# Server will run on port 10000 by default

# For development with auto-reload
npm run dev
```

### Step 6: Build & Run Android App
```bash
# Build the project
./gradlew build

# Run on emulator or device
./gradlew installDebug
```

### Step 7: Grant Permissions
When running for the first time, grant the following permissions:
- Camera
- Microphone
- Read Contacts
- Read Media (Images, Videos, Audio)
- Post Notifications

---

## <a id="architecture-details"></a> Architecture Details

### Data Flow

**Authentication Flow:**
- User initiates Google Sign-In → Firebase Authentication validates credentials → Session stored locally
- App caches authentication token for subsequent requests

**Messaging Flow:**
- Sender types message → Message stored in Firebase Firestore → FCM triggers push notification → Receiver's app opens and fetches message

**Media Sharing Flow:**
- Sender selects image/file → Image cropped (if needed) → Uploaded to Storage Server → Returns shareable URL → Sender sends URL via Firebase → Receiver clicks URL → Storage Server sends file → File deleted after download

**Video Call Flow:**
- Initiator sends call signal via Firebase → FCM notification sent to receiver → Receiver accepts → WebRTC establishes P2P connection → Audio/video streams flow directly between peers → TURN servers used if direct connection fails

---

## <a id="-contributing"></a> 🤝 Contributing

We welcome contributions from the community! Whether it's bug fixes, feature enhancements, or documentation improvements, your input is valued.

### How to Contribute

1. **Fork the Repository**
   ```bash
   Click "Fork" button on GitHub
   ```

2. **Create a Feature Branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```

3. **Make Your Changes**
   - Write clean, well-documented code
   - Follow Android coding conventions
   - Ensure your code doesn't break existing functionality

4. **Commit Your Changes**
   ```bash
   git commit -m 'Add amazing feature: description'
   ```

5. **Push to Your Fork**
   ```bash
   git push origin feature/amazing-feature
   ```

6. **Open a Pull Request**
   - Provide a clear description of changes
   - Reference any related issues
   - Include screenshots for UI changes
   - Wait for code review

### Contribution Guidelines

- Follow the existing code style and architecture
- Add unit tests for new features
- Update documentation as needed
- Ensure minimum SDK compatibility (API 24+)
- Test on multiple Android versions
- Don't introduce breaking changes without discussion
---

## <a id="-license"></a> License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
### Reporting Issues

Found a bug? Have a feature request?
1. Check existing issues first
2. Create detailed issue with reproduction steps
3. Include device info, Android version, and logs

---

## Author
Kushal V Achar

- GitHub: [@kushalvachar2006](https://github.com/kushalvachar2006)
- LinkedIn: [Connect with me](https://www.linkedin.com/in/kushal-v-achar-796049317/)

---

## Acknowledgments

- Firebase for backend infrastructure and real-time capabilities
- Google APIs for authentication and cloud services
- WebRTC community for peer-to-peer communication technology
- Express.js and Node.js ecosystem for temporary storage solution
- Android development community for best practices and libraries
- All contributors and users

---

<div align="center">
  
[⬆ Back to Top](#top)

</div>
