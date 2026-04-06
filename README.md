# Vinyl Android Native

This sibling project is the native Android rewrite target for the current Expo app.

## Directory structure

```text
vinyl-android/
  app/
    src/main/java/com/tyxu4459/expovinyl/
      data/
        local/
          dao/
          entity/
        repository/
      di/
      feature/
        library/
        player/
        playlist/
      media/
      model/
      navigation/
      ui/theme/
```

## Room tables

- `playlists`
  - `id TEXT PRIMARY KEY`
  - `name TEXT NOT NULL`
  - `created_at INTEGER NOT NULL`
- `tracks`
  - `id INTEGER PRIMARY KEY AUTOINCREMENT`
  - `uri TEXT NOT NULL UNIQUE`
  - `display_name TEXT NOT NULL`
  - `artist TEXT`
  - `album TEXT`
  - `duration_ms INTEGER`
  - `imported_at INTEGER NOT NULL`
- `playlist_tracks`
  - `playlist_id TEXT NOT NULL`
  - `track_id INTEGER NOT NULL`
  - `sort_order INTEGER NOT NULL`
  - composite primary key: `(playlist_id, track_id)`
- `playback_snapshot`
  - singleton row keyed by `id = 0`
  - stores current `playlist_id`, `track_id`, `queue_index`, `position_ms`, `is_playing`, `play_mode`, `sleep_timer_ends_at`

## Initial migration path

1. Replace Expo local storage with Room repositories.
2. Replace `expo-audio` and the custom lock screen bridge with `Media3 + MediaSessionService`.
3. Move mini player and page state into `ViewModel + StateFlow`.
4. Add file import flow with `OpenMultipleDocuments`.
