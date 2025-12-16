# Andromeda - Wellness Tracker (Phone + Wear OS)

[Project Report](https://docs.google.com/document/d/1tZ3rl0np883EFl9VOG-Cs6yTW1zFlAtuqc451Dgv1WU/edit?usp=sharing)

<img width="1390" height="735" alt="Screenshot 2025-12-16 at 3 37 48 PM" src="https://github.com/user-attachments/assets/eaf170e2-0707-44cd-8eb5-83c0caf673b9" />



Andromeda is a modern Android wellness tracking application that helps users log daily health metrics, visualize trends, personalize their experience, and receive AI-powered wellness insights. The app is built with **Jetpack Compose**, follows a clean **MVVM** architecture with **Unidirectional Data Flow (UDF)**, and emphasizes **local-first, privacy-focused** data storage.

This repository includes:
- **Phone app**: Home dashboard, Add logging flow, History, Settings, AI suggestions, chatbot, voice-to-text
- **Wear OS companion**: on-watch logging, on-watch question selection, and bidirectional sync with the phone

---

## ✨ Features

### Daily wellness logging
- Log **weight** plus selected wellness metrics (diet, activity, sleep, water, protein)
- Enforces a **single authoritative entry per day** (same-day saves merge/update)
- Add entries for past dates via DatePicker (with careful timezone handling)
- Edit and delete entries with confirmation dialogs

### Dynamic question tracking
- Select up to **three** wellness questions to track
- Add screen dynamically renders only the selected questions
- Unselected metrics are stored as `null` and do not affect UI

### Home dashboard (two views)
- Swipe between:
  - **Calendar view** highlighting days with entries
  - **Custom 14-day weight chart** (Canvas-based) with dynamic scaling
- Uses only the most recent entry per day for visualization clarity
- Supports unit changes (kg ↔ lbs) consistently across UI

### Personalization and settings
- Profile settings (name, age, target weight) with validation
- Light / dark theme support
- Adjustable text size
- Animated multi-layer Settings navigation

### AI assistant (Gemini on Vertex AI)
- **Proactive suggestions**: shows exactly **3 short tips** based on recent data and user profile
- **Conversational chatbot** with multi-turn chat history
- Voice-to-text chatbot input using Android speech recognition APIs (permission-aware)

### Wear OS companion app
- Independent question selection on watch (stored locally on watch)
- Watch ↔ phone bidirectional sync:
  - Watch sends partial metrics safely (missing questions remain `null`)
  - Phone sends an intelligent default weight suggestion based on recent averages
  - Watch requests the default on start for a low-friction experience

### Robust local persistence
- **Jetpack DataStore** for persistence
- JSON serialization (Gson) for complex stored objects
- Reactive updates via Kotlin Flow / StateFlow
- Full historical conversion when switching units (kg ↔ lbs), including target weight

---

## ✅ Features List + Status

| Feature | Status |
|---|---|
| Onboarding | ✔ |
| Daily wellness logging | ✔ |
| Single entry per day enforcement | ✔ |
| Dynamic question selection (up to three) | ✔ |
| Home dashboard (calendar + 14-day chart) | ✔ |
| Calendar-based history view | ✔ |
| Edit and delete entries | ✔ |
| AI-powered suggestions (3 tips) | ✔ |
| Conversational chatbot | ✔ |
| Voice-to-text chatbot input | ✔ |
| Wear OS companion app | ✔ |
| Watch ↔ phone sync | ✔ |
| Theme & accessibility settings | ✔ |
| Local persistence (DataStore + JSON) | ✔ |
| Automatic unit conversion (kg ↔ lbs) | ✔ |

---

## 🛠️ Build and Run Instructions

### Prerequisites
- Android Studio (latest stable)
- Android device or emulator
  - Voice input requires a **physical Android device** (most emulators do not support microphone speech-to-text reliably)

### Steps

1. **Clone the repository**

```bash
git clone https://github.com/cangokmen/CS501-Project-Andromeda.git
cd CS501-Project-Andromeda
```

2. **Add API key**

Create or edit `local.properties` (in the project root) and add:

```text
GEMINI_API_KEY=your_key_here
```

3. **Open in Android Studio**
- Go to **File > Open** and select the project folder
- Let Gradle sync and download dependencies

4. **Run the phone app**
- Select an emulator or physical device
- Press **Run**

5. **Run the Wear OS app (optional)**
- Use a Wear OS emulator or a physical watch
- Run the Wear module configuration (Android Studio will show it as a separate run target)

---

## 🏗️ Architecture

Andromeda uses **MVVM + UDF**:

1. User interaction triggers a UI event  
2. Event is handled by a ViewModel  
3. ViewModel updates `StateFlow` (often via repositories)  
4. UI recomposes based on the new state  

### UI Layer (View)
- Fully **Jetpack Compose**
- Single-activity structure with composable screens:
  - Home
  - Add
  - History
  - Settings
  - Chatbot
  - Login / Register (if enabled in your branch)

### Navigation
- **Navigation Compose**
- Centralized `NavHost` manages screen transitions
- Routes defined with a type-safe structure (commonly a sealed `Screen` pattern)

### ViewModel Layer
- ViewModels expose state via **StateFlow**
- Examples of responsibilities:
  - Home: dashboard state + suggestion loading
  - Add: dynamic form + validation + unit-safe data handling
  - Chatbot: message list, chat history, loading states
  - Watch: question selection, defaults, sync triggers

### Data Layer (Model)
- Repository pattern:
  - Wellness data CRUD, date-based merge, unit conversion, sync helpers
  - User preferences (theme, text size, selected questions)
  - Profile data (name, age, target weight)
- **DataStore + Gson** for persistence
- Flows used as the single source of truth for reactive UI updates

---

## 🔒 Privacy Notes
- Designed to be **local-first**: wellness history and preferences are stored on-device using DataStore.
- AI features use Gemini on Vertex AI. Do not treat this project as medical software or medical advice.

---

## 📚 Tech Stack
- Jetpack Compose
- Navigation Compose
- ViewModel
- Kotlin Coroutines + Flow / StateFlow
- Jetpack DataStore
- Gson (JSON serialization)
- Gemini on Vertex AI (AI suggestions + chatbot)
- Android Speech APIs (voice-to-text)
- Wear OS Data Layer APIs (phone ↔ watch sync)

---

## 👥 Team
CAS CS 501 (Fall 2025) project by **Can Gokmen** and **Jinpeng Huang**.
