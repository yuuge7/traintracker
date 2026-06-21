# TrainTracker

TrainTracker is an Android application designed to help users track and manage their train trips. It provides a convenient way to log journeys, view historical data, analyze statistics, and manage frequently visited stations.

## 🚀 Features

- **Trip Logging:** Record trip details including origin, destination, and duration.
- **History:** A dedicated activity to view and manage past trips.
- **Statistics:** Analyze your travel habits with built-in statistics.
- **Station Management:** Easily manage and select from a list of stations.
- **Home Screen Widget:** A convenient widget for quick access to your train tracking information directly from your home screen.
- **Local Storage:** Uses Room Persistence Library for efficient data handling.

## 🛠 Tech Stack

- **Language:** Java
- **UI Framework:** Android XML with Material Design 3
- **Build System:** Gradle (Kotlin DSL) with Version Catalogs
- **Database:** Room Persistence Library (SQLite)
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 36

## 📋 Prerequisites

Before you begin, ensure you have the following installed:
- [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended)
- [JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) (included with Android Studio)
- [Git](https://git-scm.com/)

## ⚙️ Setup & Installation

Follow these steps to set up the project on a new machine:

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/yuuge7/TrainTracker.git
    ```
2.  **Open in Android Studio:**
    - Launch Android Studio.
    - Select **Open** and navigate to the `TrainTracker` folder you just cloned.
3.  **Sync Project with Gradle Files:**
    - Android Studio should automatically prompt you to sync. If not, go to **File > Sync Project with Gradle Files**.
    - Wait for the process to download dependencies and index the project.
4.  **Run the Application:**
    - Connect a physical device with **USB Debugging** enabled or start an Android Emulator.
    - Click the **Run** button (green arrow) or press `Shift + F10`.

## 📂 Project Structure

- `app/src/main/java/com/example/traintracker/`: Contains the Java source code.
    - `MainActivity.java`: Main entry point.
    - `AppDatabase.java`: Room database configuration.
    - `TrainDao.java`: Data access object for all tables.
    - `Trip.java`, `Station.java`, `Distance.java`: Room entities.
    - `TrainWidget.java`: Implementation of the App Widget.
- `app/src/main/res/`: Contains UI resources (layouts, drawables, strings, etc.).
- `gradle/libs.versions.toml`: Centralized dependency management.
