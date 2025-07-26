# Accompanist Lyrics

[![Tests](https://img.shields.io/github/actions/workflow/status/Mocha-Realm/Accompanist-Lyrics/test.yml?branch=main&label=Tests)](https://github.com/Mocha-Realm/Accompanist-Lyrics/actions/workflows/test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.mocharealm.accompanist/accompanist-lyrics)](https://central.sonatype.com/artifact/com.mocharealm.accompanist/accompanist-lyrics)
[![Telegram](https://img.shields.io/badge/Telegram-Community-blue?logo=telegram)](https://t.me/mocha_pot)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

A general-purpose, extensible lyrics parsing library for Kotlin/JVM.

## ✨ Features

- **🤖 Smart Auto-Detection**: Automatically detects and parses various lyrics formats out of the box.
- **🎤 Karaoke-Ready**: Provides syllable-level timing for precise karaoke-style highlighting.
- **🌐 Translation Support**: Natively handles dual-language or translated lyric lines.
- **🧩 Highly Extensible**: Easily add support for new or custom formats.
- **🏷️ Metadata Extraction**: Reads standard tags like artist, album, title, and offset.
- **🚀 Pure Kotlin/JVM**: No Android dependencies, suitable for any Kotlin project.

## 💿 Supported Formats

- **LRC**: Standard and dual-language `.lrc` files.
- **Enhanced LRC**: Syllable-level timing, voice separation, and accompaniment tags.
- **TTML (Apple Syllable)**: The format used by Apple Music.
- **Lyricify Syllable**: Custom format from the [Lyricify App](https://github.com/WXRIW/Lyricify-App).

## 🚀 Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.mocharealm.accompanist:accompanist-lyrics:VERSION")
}
```

*Replace `VERSION` with the latest version from Maven Central.*

-----

## ▶️ Usage

### Quick Start: Auto-Parsing (Recommended)

For most use cases, `AutoParser` is the easiest way to parse lyrics without needing to know the format beforehand.

```kotlin
import com.mocharealm.accompanist.lyrics.parser.AutoParser

// 1. Get your lyrics content from a file or network
val lyricsContent: String = fetchLyrics()

// 2. Create a default AutoParser instance using its builder
val autoParser = AutoParser.Builder().build()

// 3. Parse the content
val lyrics = autoParser.parse(lyricsContent)

// Now you have a unified SyncedLyrics object!
println(lyrics.metadata.title)
println(lyrics.lines.first().text)
```

### Parsing a Specific Format

If you know the exact format, you can use a specific parser directly.

```kotlin
import com.mocharealm.accompanist.lyrics.parser.LrcParser

val lrcLines = listOf(
    "[00:39.96]I lean in and you move away",
    "[00:39.96]我靠在里面，你就离开"
)

val lyrics = LrcParser.parse(lrcLines)
println(lyrics.lines)
```

*You can also use `EnhancedLrcParser`, `TTMLParser`, or `LyricifySyllableParser`.*

-----

## 🛠️ Extending with Custom Formats

Accompanist Lyrics is designed to be extensible. You can add support for any custom format by implementing the `ILyricsParser` interface and registering it with the `AutoParser`.

### Step 1: Implement `ILyricsParser`

Create a class that implements the parsing logic for your custom format.

```kotlin
import com.mocharealm.accompanist.lyrics.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.parser.ILyricsParser

class MyCustomParser : ILyricsParser {
    override fun parse(lines: List<String>): SyncedLyrics {
        // Your parsing logic here...
    }
    
    override fun parse(content: String): SyncedLyrics {
        // Your parsing logic here...
    }
}
```

### Step 2: Define a Format Detector

Create a `LyricsFormat` that contains a detector function. This function returns `true` if the given content matches your custom format.

```kotlin
import com.mocharealm.accompanist.lyrics.utils.LyricsFormatGuesser

val myCustomFormat = LyricsFormatGuesser.LyricsFormat(
    name = "MY_CUSTOM_FORMAT",
    detector = { content -> 
        // Example: check for a unique tag
        content.startsWith("##MY_COOL_LYRICS##")
    }
)
```

### Step 3: Register with `AutoParser.Builder`

Use the `withFormat` method on the builder to register your new format and its parser. Custom formats are checked first, ensuring they are prioritized over built-in ones.

```kotlin
import com.mocharealm.accompanist.lyrics.parser.AutoParser

// Build an AutoParser instance with your custom format
val autoParser = AutoParser.Builder()
    .withFormat(myCustomFormat, MyCustomParser())
    .build()

// This parser now understands both built-in and your custom format!
val lyrics = autoParser.parse(myCustomLyricsContent)
```

## 💬 Community & Support

Join the community, ask questions, and share your projects\!

- **Telegram:** [**mocha\_pot**](https://t.me/mocha_pot)
- **GitHub Issues:** [**Create an Issue**](https://www.google.com/search?q=https://github.com/6xingyv/Accompanist-Lyrics/issues)

## 🤝 Contributing

Contributions are welcome\! Please feel free to submit a pull request or open an issue to discuss your ideas. For major changes, please open an issue first.


## 📜 License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](http://www.apache.org/licenses/LICENSE-2.0.txt) file for details.