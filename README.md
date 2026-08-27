# Recipes

Recipes is a Kotlin Multiplatform (KMP) application for browsing and searching recipes. It demonstrates modern Android and KMP development practices, including the use of Compose Multiplatform, Circuit architecture, and various powerful libraries.

## Features

- **Search**: Quickly find recipes by keyword.
- **Categories**: Browse recipes organized by categories.
- **Favorites**: Save your favorite recipes for quick access.
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
- **Data Source**: Uses the **[TheMealDB API](https://www.themealdb.com/documentation)** (v2) for fetching recipe data. A `MEALDB_API_KEY` is required to build — set it as a Gradle property in `~/.gradle/gradle.properties` or as an environment variable.

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

### Resetting local data

Room stores an identity hash of the schema inside the database file. Change an entity and that hash
stops matching, so the app throws on first database access:

```
IllegalStateException: Room cannot verify the data integrity.
Looks like you've changed schema but forgot to update the version number.
```

This is deliberate and there is **no destructive-migration fallback** — a schema change should make
you decide between writing a migration and throwing the local data away, rather than silently
losing it. Two distinct failures:

- **Schema changed, `version` left alone** — the message above. Delete the local database (below).
- **`version` bumped with no matching `Migration`** — `A migration from 1 to 2 was required but not
  found.` Write the migration, or delete the local database while still in development.

To confirm you're in the first case, run `git diff storage/schemas/`. If `identityHash` changed but
the file is still `1.json`, the schema moved without a version bump and every existing local
database is now invalid.

The database is `recipe.db` on every platform:

| Platform | How to delete it |
| --- | --- |
| **Web** | DevTools → Application → *Clear site data*. To remove only the database: <br>`const root = await navigator.storage.getDirectory();`<br>`await root.removeEntry('recipe.db');`<br>then reload. |
| **Desktop** | `rm -f "$TMPDIR"recipe.db*` on macOS (`$TMPDIR` already ends in `/`), `rm -f /tmp/recipe.db*` on Linux, `del %TEMP%\recipe.db*` on Windows. |
| **iOS** | `rm -f "$(xcrun simctl get_app_container booted com.scottolcott.recipe.Recipes data)/Documents/recipe.db"*` — or just delete the app. |
| **Android** | Clear storage in the app's system settings, or `adb shell pm clear com.scottolcott.recipe`. |

Things that catch people out:

- **Keep the trailing `*`.** The database runs in WAL mode, so `recipe.db-wal` and `recipe.db-shm`
  sit alongside it. Deleting only `recipe.db` leaves a partial database behind.
- **Desktop stores the database in the system temp directory**, so it also disappears on reboot.
  That is why this crash sometimes appears to fix itself.
- **DataStore is stored separately from the database.** On desktop and iOS the fetch-history and
  search-suggestion `.json` files live in the same directory as `recipe.db`; on web they are in
  `localStorage`, a different bucket from OPFS, so the console snippet above does not touch them.
  Clearing only the database does recover — each Store rejects empty results and refetches — but
  *Clear site data* is the real reset on web.
- **Favorites do not come back.** They exist only in the local database and there is no API to
  restore them from.
- The database used to be called `my_room.db` on iOS and desktop. If you ran an older build, delete
  that stale file once.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
