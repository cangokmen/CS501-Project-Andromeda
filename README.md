# Andromeda - Wellness Tracker

Andromeda is a modern Android wellness tracking application that helps users log daily health metrics, visualize trends, personalize their experience, and receive AI-powered wellness insights. The app is built entirely with **Jetpack Compose**, follows a clean **MVVM architecture**, and emphasizes **local-first, privacy-focused data storage**.

---

## ✨ Features

- **Daily Wellness Logging**  
  Log weight and selected wellness metrics (diet, activity, sleep, water, protein). Each day maintains a single authoritative entry.

- **Dynamic Question Tracking**  
  Users can select up to **three wellness questions** to track. The Add screen dynamically renders only selected metrics.

- **Advanced Data Visualization**  
  - Custom **14-day weight trend line chart** with dynamic scaling  
  - Interactive **calendar view** highlighting logged days  

- **Personalized AI Assistance (Google Gemini on Vertex AI)**  
  - Proactive daily suggestions on the Home screen  
  - Conversational chatbot with historical context awareness  
  - Supports **voice-to-text input** for hands-free interaction  

- **Historical Management**  
  View, edit, and safely delete past entries with confirmation dialogs.

- **Wear OS Companion App**  
  - Independent question selection on watch  
  - Bidirectional watch ↔ phone data synchronization  
  - Intelligent default weight suggestions based on recent averages  

- **Customization & Accessibility**  
  - Light / Dark theme support  
  - Adjustable text size  
  - Animated multi-layer Settings navigation  

- **Robust Local Persistence**  
  - Jetpack DataStore with JSON serialization  
  - Reactive updates using Kotlin Flow  
  - Full historical weight conversion when switching units (kg ↔ lbs)  

---

## ✨ Features List + Status

| Feature                               | Status |
|--------------------------------------|--------|
| Daily wellness logging               | ✔      |
| Single entry per day enforcement     | ✔      |
| Dynamic question selection           | ✔      |
| 14-day custom weight graph           | ✔      |
| Calendar-based history view          | ✔      |
| AI-powered suggestions               | ✔      |
| Conversational Gemini chatbot        | ✔      |
| Voice-to-text chatbot input          | ✔      |
| Wear OS companion app                | ✔      |
| Theme & accessibility settings       | ✔      |
| Local data persistence (DataStore)   | ✔      |
| Automatic unit conversion            | ✔      |
| Cloud sync across devices            | ✘ descoped |
| PDF / analytics export               | ✘ descoped |

---

## 🛠️ Build and Run Instructions

### Prerequisites
- Android Studio (latest stable)
- Android device or emulator  
  > ⚠️ Voice input requires a **physical Android device**

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/Andromeda.git

2. **Add API Key**

   Create or edit `local.properties` and add:

   ```text
   GEMINI_API_KEY=your_key_here
   ```

3. **Open in Android Studio**
   1. Open Android Studio.
   2. Select **File > Open** and navigate to the cloned project directory.
   3. Let Android Studio sync the Gradle files and download the required dependencies.

4. **Run the Application**
   1. Select an **Android emulator** or connect a **physical Android device**.
   2. Click the **Run** button in Android Studio.
   3. The app will build, install, and launch on the selected device.

> ⚠️ Voice input (microphone to text) requires a physical Android device and will not work on most emulators.

---

## 🏗️ Architectural Overview

Andromeda is built using a modern **MVVM (Model–View–ViewModel)** architecture combined with **Unidirectional Data Flow (UDF)** and a fully declarative UI using **Jetpack Compose**. This structure ensures clear separation of concerns, high maintainability, and reactive UI updates.

### 1. UI Layer (View)

- Implemented entirely with **Jetpack Compose**
- Uses a **Single-Activity architecture** (`MainActivity`)
- Core screens:
  - Home
  - Add
  - History
  - Settings
  - Chatbot
- UI components are **stateless** and render purely from ViewModel state
- UI observes state via `collectAsState()` and recomposes automatically on data changes

### 2. Navigation

- Built with **Navigation Compose**
- Centralized `NavHost` (e.g., `AppNavHost.kt`) manages all screen transitions
- Routes are defined using a sealed `Screen` class for type safety
- Navigation is deterministic and lifecycle-aware

### 3. State Management (ViewModel)

- **MainViewModel**
  - Manages global UI preferences (theme, text size, selected questions)
- **AddViewModel**
  - Handles dynamic form rendering and validation logic
- **ChatbotViewModel**
  - Manages AI conversation state, message history, and loading indicators
- **WatchViewModel**
  - Manages Wear OS UI state and sync communication
- ViewModels expose state via **`StateFlow`**
- Business logic is isolated from UI rendering

### 4. Data Layer (Model)

- Follows the **Repository pattern**
- Core repositories:
  - `WellnessDataRepository`: CRUD operations, date-based merging, unit conversion, watch ↔ phone sync
  - `UserPreferencesRepository`: theme, text size, selected questions, and other UI preferences
- Uses **Jetpack DataStore** for persistence:
  - Key–value storage for simple preferences
  - JSON serialization (via **Gson**) for complex data such as wellness history
- Data layer acts as the **single source of truth** for the application

### 5. Unidirectional Data Flow (UDF)

1. User interaction triggers a UI event  
2. Event is sent to the ViewModel  
3. ViewModel updates its `StateFlow` (optionally via repositories)  
4. UI recomposes automatically with the new state  

This predictable flow makes the application easier to debug, test, and reason about.

---

## 📚 Key Libraries

- **Jetpack Compose** — Declarative UI toolkit for building modern Android interfaces  
- **Navigation Compose** — Type-safe navigation between composable screens  
- **Jetpack ViewModel** — Lifecycle-aware state management  
- **Jetpack DataStore** — Asynchronous, transaction-safe local persistence  
- **Kotlin Coroutines & Flow** — Reactive and asynchronous programming  
- **Gson** — JSON serialization/deserialization for complex data models  
- **Google Gemini API (Vertex AI)** — AI-powered wellness suggestions and conversational chatbot  
- **Android SpeechRecognizer API** — Voice-to-text input for chatbot interactions  
- **Wear OS Data Layer API** — Bidirectional communication between phone and watch modules  
