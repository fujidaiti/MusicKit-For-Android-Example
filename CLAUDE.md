# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Android MusicKit SDK test application (version 1.1.2), a demo app showcasing Apple's MusicKit SDK for Android. It demonstrates how to build a custom music player that integrates with Apple Music using the MusicKit framework.

## Build and Run

### Building the app
```bash
./gradlew build
```

### Installing to device/emulator
```bash
./gradlew installSdkDebug
```

### Clean build
```bash
./gradlew clean
```

## Configuration Requirements

**IMPORTANT**: Before building, you must configure a developer token:
- Edit `src/main/res/values/strings.xml`
- Replace `"Enter Your JWT Developer Token Here"` in the `developer_token` string with a valid Apple Music JWT developer token
- This token is required for the app to communicate with Apple Music services

## Architecture

### Core Components

**MediaPlaybackService** (`service/MediaPlaybackService.java`)
- Main service extending `MediaBrowserServiceCompat`
- Loads native libraries (`c++_shared`, `appleMusicSDK`) on initialization
- Creates and manages `MediaPlayerController` (from Apple's SDK AAR libraries)
- Bridges Apple Music SDK with Android's MediaSession framework
- Runs on a background `HandlerThread` for performance

**MainActivity** (`activity/MainActivity.java`)
- Single activity with drawer navigation
- Manages fragment navigation (Browse, Now Playing, Up Next, Settings)
- Connects to `MediaPlaybackService` via `MediaBrowserHelper`
- Sets up `MediaControllerCompat` when browser connects

**MediaSessionManager** (`service/MediaSessionManager.java`)
- Implements `MediaSessionCompat.Callback` and `MediaPlayerController.Listener`
- Translates between Android MediaSession API and Apple MusicKit SDK
- Handles playback commands (play, pause, skip, seek)
- Manages metadata updates and artwork loading (using Picasso)
- Converts SDK playback states to MediaSession states

**LocalMediaProvider** (`util/LocalMediaProvider.java`)
- Provides media browsing data from JSON files in `assets/media_data/`
- Loads media items asynchronously using `AsyncTask`
- Parses JSON to create `MediaBrowserCompat.MediaItem` objects
- Supports both browsable containers (albums, playlists) and playable items (songs)

### Fragment Architecture

All fragments extend `BaseFragment` and represent different screens:
- **BrowseFragment**: Media browsing interface
- **NowPlayingFragment**: Current playback UI
- **UpNextFragment**: Queue management
- **SettingsFragment**: User authentication and preferences

### Authentication

**AppleMusicTokenProvider** (`util/AppleMusicTokenProvider.java`)
- Implements SDK's `TokenProvider` interface
- Provides both developer token (from strings.xml) and user token (from SharedPreferences)
- User tokens are obtained through authentication flow and stored via `AppPreferences`

### Native SDK Integration

The app depends on two AAR libraries in `src/main/libs/`:
- `mediaplayback-release-1.1.1.aar`: Core playback functionality
- `musickitauth-release-1.1.2.aar`: Authentication components

Key SDK classes used:
- `MediaPlayerController`: Main playback controller
- `MediaPlayerControllerFactory`: Creates controller instances
- `CatalogPlaybackQueueItemProvider`: Builds playback queues from Apple Music catalog IDs
- `PlayerQueueItem`, `PlayerMediaItem`: Represent queue and media items

### Threading Model

- Service operations run on `HandlerThread` with background priority
- MediaSession callbacks executed on background handler
- UI updates marshaled to main thread via `Handler`
- Artwork loading happens asynchronously via Picasso (callbacks on main thread)

## Media Data Structure

Media browsing data is stored in `src/main/assets/media_data/` as JSON files:
- `root.json`: Top-level navigation items
- Files named after media IDs (e.g., `subscription_content.json`)
- Each JSON contains arrays of media items with: id, title, subtitle, type (song/album/playlist), URIs, browseable/playable flags

## Common Development Patterns

### Playing Media from Catalog
Media is played by creating a `CatalogPlaybackQueueItemProvider` with either:
- Container ID + container type (for albums/playlists)
- Item ID + item type (for individual songs)

Then calling `playerController.prepare(provider, autoPlay)`

### Custom MediaSession Commands
Custom commands are handled in `MediaSessionManager.onCommand()`:
- `COMMAND_REMOVE_QUEUE_ITEM`: Remove item from queue
- `COMMAND_MOVE_QUEUE_ITEM`: Reorder queue items
- `COMMAND_ADD_QUEUE_ITEMS`: Add items to queue with insertion type

### Metadata Mapping
SDK `PlayerMediaItem` fields map to `MediaMetadataCompat` keys:
- `getTitle()` → `METADATA_KEY_TITLE`
- `getAlbumTitle()` → `METADATA_KEY_ALBUM`
- `getArtistName()` → `METADATA_KEY_ARTIST`
- `getDuration()` → `METADATA_KEY_DURATION`
- `getArtworkUrl(w, h)` → `METADATA_KEY_ALBUM_ART_URI`

## SDK Version

Android MusicKit SDK 1.1.2 (November 2021)
- Supports 64-bit ARM architectures
- Android SDK 28 (compileSdkVersion/targetSdkVersion)
- Minimum SDK 21
- Uses deprecated AppCompat v7:28 (final release with this support)
