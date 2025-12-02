# Andromeda - Wellness Tracker

Andromeda is a modern Android application for personal wellness tracking. It lets users log daily metrics such as weight, diet, activity, and sleep, view historical trends, manage their account profile, and interact with an integrated AI chatbot. The app is built entirely with Jetpack Compose and follows current Android development best practices, including an MVVM architecture.

## ✨ Features

- **Daily Logging**: Easily add daily entries for weight and other wellness metrics. Each day contains a single entry; new inputs for the same day update that day's data.
- **Account System**: Register and log in with email and password. Each account has its own wellness history and preferences.
- **Profile Management**: View and edit last name, age, and target weight. First name is taken from registration.
- **Dynamic Questions**: The Add screen dynamically updates to show only the questions the user has selected to track (up to three).
- **Historical View**: A scrollable history screen that displays past entries for the currently logged-in user.
- **Customizable Experience**:
  - **Theme**: Switch between light and dark themes.
  - **Text Size**: Choose between normal and larger text sizes for better accessibility.
  - **Question Management**: Select up to three wellness questions to track from a list of options.
- **Data Management**:
  - **Reset Data**: Clear all saved wellness history from the settings screen.
  - **Seed Data**: A developer-focused feature to populate the app with predefined historical data for testing and demos.
- **AI Chatbot**: Ask wellness-related questions through a chatbot powered by the Gemini API.
- **Voice Input**: Optional speech-to-text support for the chatbot input (requires a physical Android device).


## ✨ Features List + Status
  | Feature                     | Status     |
| --------------------------- | ---------- |
| Login / Logout              | ✔          |
| Store wellness data per day | ✔          |
| Merge same-day entries      | ✔          |
| 14-day dynamic graph        | ✔          |
| Settings (theme, questions) | ✔          |
| Speech-to-text              | ✔          |
| Gemini chatbot              | ✔          |
| Multi-user data isolation   | ✔          |
| Data reset                  | ✔          |
| Seed demo data              | ✔          |
| Cloud sync                  | ✘ descoped |


## 🛠️ Build and Run Instructions

To build and run the Andromeda project, you will need Android Studio.

1. **Clone the repository**

    ```bash
    git clone https://github.com/your-username/Andromeda.git
    ```

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

Andromeda is built with a modern **MVVM (Model–View–ViewModel)** architecture and a fully declarative UI using **Jetpack Compose**.

### 1. UI Layer (View)

- **Jetpack Compose**: The entire UI is written as composable functions.
- **Single-Activity Architecture**: `MainActivity` hosts the composable UI.
- **Screens**: `LoginScreen`, `HomeScreen`, `AddScreen`, `HistoryScreen`, `SettingsScreen`, `ChatbotScreen`.
- **State-driven UI**: Each screen observes state from ViewModels and recomposes automatically when data changes.

### 2. Navigation

- **Navigation for Compose**: A centralized `NavHost` (for example, `AppNavHost.kt`) manages all screen transitions.
- **Type-safe routes**: A `Screen` sealed class defines all navigation routes, which reduces runtime navigation errors.
- **Auth-aware start destination**: The app starts on `Login` or `Home` based on the stored login state.

### 3. State Management (ViewModel)

- **MainViewModel**: Manages global app state such as theme, selected questions, text size, and login status.
- **AddViewModel**: Manages the state and logic for the Add screen, including enforcing one entry per day.
- **ChatbotViewModel**: Manages chatbot conversation state, user messages, AI responses, and loading indicators.
- **StateFlow**: ViewModels expose state via `kotlinx.coroutines.flow.StateFlow`, which works seamlessly with Compose.

### 4. Data Layer (Model)

- **Repository Pattern**:
  - `UserPreferencesRepository`: Handles theme, text size, selected questions, login state, user email, and profile fields (name, last name, age, target weight).
  - `WellnessDataRepository`: Handles reading and writing wellness entries, per-user filtering, date-based merging, and seeding sample data.
- **Jetpack DataStore**: Used for modern, asynchronous data persistence.
  - **Simple data**: Theme, text size, selected questions, and login info are stored as key–value pairs.
  - **Complex data**: The list of `WellnessData` objects is serialized to JSON using **Gson** and stored in DataStore.

---

## 📚 Key Libraries

- **Jetpack Compose**
- **Navigation Compose**
- **Jetpack ViewModel**
- **Jetpack DataStore**
- **Kotlin Coroutines & Flow**
- **Gson**
- **Google Gemini API (Generative AI)**
- **Android SpeechRecognizer API**
