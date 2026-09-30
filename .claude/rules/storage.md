---
paths:
  - "storage/**"
---

# Local storage (Room 3 + DataStore)

**Room has no destructive-migration fallback, deliberately.** Changing an entity without bumping
`AppDatabase.version` throws "Room cannot verify the data integrity" on first db access; bumping
the version without writing a `Migration` throws "A migration from N to M was required but not
found." Both are intentional — the crash is the reminder to write the migration. Don't "fix" one
by adding `fallbackToDestructiveMigration`; note that it would not even cover the first case,
since `checkIdentity` runs in `onOpen`, before any migration path.

The db is `recipe.db` on every platform (one `DATABASE_NAME` constant in `StorageFactory.kt`).
Per-platform delete commands: `README.md` → *Resetting local data*.

Serialize stored JSON with the `@StorageJson` qualifier, not `@NetworkJson`
(`core/.../serialization/JsonQualifiers.kt`).
