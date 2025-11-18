# Andromeda - Wellness Tracker

Andromeda is a modern Android application for personal wellness tracking. It lets users log daily metrics such as weight, diet, activity, and sleep, and view their progress over time. The app is built entirely with Jetpack Compose and follows current Android development best practices, including an MVVM architecture.

## ✨ Features

- **Daily Logging**: Easily add daily entries for weight and other wellness metrics.
- **Dynamic Questions**: The Add screen dynamically updates to show only the questions the user has selected to track.
- **Historical View**: A scrollable history screen that displays all past entries, showing only the data that was recorded on that day.
- **Customizable Experience**:
  - **Theme**: Switch between light and dark themes.
  - **Text Size**: Choose between normal and larger text sizes for better accessibility.
  - **Question Management**: Select up to three wellness questions to track from a list of options.
- **Data Management**:
  - **Reset Data**: Clear all saved wellness history from the settings screen.
  - **Seed Data**: A developer focused feature to populate the app with predefined historical data for testing and demos.

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
