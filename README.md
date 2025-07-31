# Notes App with Couchbase Sync

Note-taking application built with **Jetpack Compose** and **Couchbase Lite**, with real-time cloud synchronization using **Sync Gateway** and **Couchbase Capella**.

## NOTE:

For this assignment I pushed my capella credentials to Github in public repo for testing purpose (credentials should never be uploaded to github)

## Features

- **Note Management**: CRUD Notes app
- **Real-time Sync**: Automatic Sync with Couchbase Capella
- **Offline Support**: Local storage with Couchbase Lite

## Architecture : MVVM

- **Target SDK**: Android API 35
- **Min SDK**: Android API 24 (Android 7.0+)

## Setup & Installation

### 3. Build and Run

```bash
# Open in Android Studio or use command line:
./gradlew assembleDebug
./gradlew installDebug
```

## Usage

### Creating Notes

1. Tap the **"New"** button on the main screen
2. Enter a title and content for your note
3. Tap **"Save"** to create the note

### Editing Notes

1. Tap on any existing note from the list
2. Modify the title or content
3. Tap **"Save"** to update the note

### Deleting Notes

1. Tap the delete icon on any note card
2. The note will be deleted from both local and cloud storage

### Synchronization

- Notes automatically sync with Couchbase Capella when connected
- Works offline with local Couchbase Lite storage
- Real-time updates across all connected devices

## Configuration Options

The app uses `ConfigurationManager` to handle various settings:

```kotlin
// Initialize configuration
ConfigurationManager.initialize(context)

// Get current configuration
val config = ConfigurationManager.getConfiguration()
```

### Network Monitoring

The app includes built-in network monitoring:

```kotlin
// Monitor network status
val isConnected by networkMonitor.isConnected.collectAsState()
```

## Database Schema

### Note Model

```kotlin
data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: Instant
)
```

### Couchbase Document Structure

```json
{
  "type": "note",
  "id": 12345,
  "title": "My Note Title",
  "content": "Note content",
  "createdAt": 1672531200.0
}
```
