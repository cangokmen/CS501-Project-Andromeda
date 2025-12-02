# Andromeda - Wellness Tracker

Andromeda is a modern Android application for personal wellness tracking. It lets users log daily metrics such as weight, diet, activity, and sleep, view historical trends, manage their account profile, and interact with an integrated AI chatbot. The app is built entirely with Jetpack Compose and follows current Android development best practices, including an MVVM architecture.

## ✨ Features

- **Daily Logging**: Add daily entries for weight and wellness metrics. Each day contains one entry, and new inputs update that day's record.
- **Account System**: Register and log in using email + password. Each account has its own wellness data and preferences.
- **Profile Management**: Edit last name, age, and target weight. First name is taken from registration.
- **Dynamic Questions**: The Add screen shows only the selected wellness questions (up to 3).
- **Historical View**: A clear history screen that displays past entries, filtered by logged-in user.
- **Customizable Experience**:
  - **Theme**: Light/Dark mode.
  - **Text Size**: Normal or larger text.
  - **Question Management**: Track up to three wellness metrics.
- **Data Management**:
  - **Reset Data**: Clear all stored wellness entries.
  - **Seed Data**: Developer-only tool to add sample data for testing.
- **AI Chatbot**: Integrated Gemini-powered wellness chatbot.
- **Voice Input**: Speech-to-text support in the chatbot input bar (requires a physical Android device).

## 🛠️ Build and Run Instructions

To build and run the Andromeda project, you will need Android Studio.

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/Andromeda.git
    ```
2. Open in Android Studio
   1. **Open Android Studio.**
   2. Select **File > Open** and navigate to the cloned project directory.
   3. Let Android Studio **sync the Gradle files** and download the required dependencies.

3. Run the Application
   1. Select an **Android emulator** or connect a **physical Android device**.
   2. Click the **Run** button in Android Studio.
   3. The app will **build, install, and launch** on the selected device.

---

## 🏗️ Architectural Overview

Andromeda is built with a modern **MVVM (Model–View–ViewModel)** architecture and a fully declarative UI using **Jetpack Compose**.

### 1. UI Layer (View)

- **Jetpack Compose**: The entire UI is written as composable functions.
- **Single-Activity Architecture**: `MainActivity` hosts the composable UI.
- **State-driven UI**: Screens such as `HistoryScreen`, `AddScreen`, and `SettingsScreen` observe state from ViewModels and recompose automatically.

### 2. Navigation

- **Navigation for Compose**: A centralized `NavHost` (for example, `AppNavHost.kt`) manages all screen transitions.
- **Type-safe routes**: A `Screen` sealed class defines all navigation routes, which reduces runtime navigation errors.

### 3. State Management (ViewModel)

- **MainViewModel**: Manages global app state (theme, selected questions, etc.).
- **Screen-scoped ViewModels**: For example, `AddViewModel` manages the state for the Add screen only.
- **StateFlow**: ViewModels expose state via `kotlinx.coroutines.flow.StateFlow`, which works seamlessly with Compose.

### 4. Data Layer (Model)

- **Repository Pattern**: `UserPreferencesRepository` and `WellnessDataRepository` abstract data access from the rest of the app.
- **Jetpack DataStore**: Used for modern, asynchronous data persistence.
  - **Simple data**: Theme, text size, and selected questions are stored as key–value pairs.
  - **Complex data**: The list of `WellnessData` objects is serialized to JSON using **Gson** and stored in DataStore.

---

## 📚 Key Libraries

- **Jetpack Compose**
- **Jetpack Navigation**
- **Jetpack ViewModel**
- **Jetpack DataStore**
- **Kotlin Coroutines & Flow**
- **Gson**
