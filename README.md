# Recipes

Recipes is a Kotlin Multiplatform (KMP) application for browsing and searching recipes. It demonstrates modern Android and KMP development practices, including the use of Compose Multiplatform, Circuit architecture, and various powerful libraries.

## Features

- **Search**: Quickly find recipes by keyword.
- **Categories**: Browse recipes organized by categories.
- **Favorites (WIP)**: Save your favorite recipes for quick access.
- **Recipe Details**: View detailed instructions and ingredients for each recipe.

## Technology Stack

The project leverages a modern and robust tech stack for multiplatform development:

- **[Kotlin Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html)**: Shared business logic and data layers across Android, iOS, Desktop, and Web.
- **[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)**: Shared UI framework for building beautiful and consistent interfaces on all platforms.
- **[Circuit](https://github.com/slackhq/circuit)**: A simple, reactive framework for building UI on Android and multiplatform.
- **[Metro](https://github.com/ZacSweers/metro)**: A multiplatform, compile-time dependency injection (DI) framework for Kotlin.
- **[Ktor](https://ktor.io/)**: For networking and API interactions.
- **[Coil 3](https://coil-kt.github.io/coil/)**: Image loading for Kotlin Multiplatform.
- **[Room 3](https://developer.android.com/training/data-storage/room)**: SQLite object mapping library, now supporting KMP.
- **[Store 5](https://github.com/MobileNativeFoundation/Store)**: A library for managing data loading and caching.
- **Data Source**: Uses **[TheMealDB API](https://www.themealdb.com/api.php)** for fetching recipe data.

## Project Structure

The project is organized into several modules to ensure a clean separation of concerns:

- `app`: Android-specific application code.
- `ui`: Shared UI components using Compose Multiplatform.
- `domain`: Shared business logic and presenters (Circuit).
- `model`: Shared data models.
- `repository`: Shared data repository layer.
- `network`: Shared networking layer using Ktor.
- `storage`: Shared persistence layer using Room.
- `shared`: Dependency injection (DI) and application-level shared logic.
- `iosApp`: iOS-specific application code (Swift).
- `webApp`: Web-specific application code using Compose for Web.
- `desktopApp`: Desktop-specific application code using Compose for Desktop.
- `sqliteWasmWorker`: Helper for SQLite WASM on Web.

## Getting Started

### Prerequisites

- [Android Studio Panda](https://developer.android.com/studio) or newer.
- [Xcode](https://developer.apple.com/xcode/) (for iOS development).

### Running the Application

- **Android**: Open the project in Android Studio and run the `app` configuration.
- **iOS**: Open `iosApp/iosApp.xcworkspace` in Xcode and run on a simulator or device.
- **Desktop**: Run `./gradlew :desktopApp:run`.
- **Web (JS)**: Run `./gradlew :webApp:jsBrowserDevelopmentRun`.
- **Web (wasmJS)**: Run `./gradlew :webApp:wasmJsBrowserDevelopmentRun`.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
