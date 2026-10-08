<img src="./artwork/banner.png" style="border-radius: 6px; margin-bottom: 8px">

[![Tests](https://img.shields.io/github/actions/workflow/status/Mocha-Realm/Accompanist-Lyrics/test.yml?branch=main&label=Tests)](https://github.com/Mocha-Realm/Accompanist-Lyrics/actions/workflows/test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.mocharealm.accompanist/lyrics-core)](https://central.sonatype.com/artifact/com.mocharealm.accompanist/lyrics-core)
[![Telegram](https://img.shields.io/badge/Telegram-Community-blue?logo=telegram)](https://t.me/mocha_pot)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

## 📦 Repository

Accompanist released a group of artifacts, including: 

- [`lyrics-core`](https://github.com/6xingyv/accompanist-lyrics-core) - Parsing lyrics file, holding data and exporting to other formats.

- [`lyrics-ui`](https://github.com/6xingyv/accompanist-lyrics-ui) - Standard lyrics interface built on Jetpack Compose

This repository hosts the `lyrics-core` code.

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
    implementation("com.mocharealm.accompanist:lyrics-core:VERSION")
}
```

*Replace `VERSION` with the latest version from Maven Central.*

-----

## ▶️ Usage

### Quick Start: Auto-Parsing (Recommended)

For most use cases, `AutoParser` is the easiest way to parse lyrics without needing to know the format beforehand.

```kotlin
// 1. Get your lyrics content from a file or network
val lyricsContent: String = fetchLyrics()

// 2. Create a default AutoParser instance
val autoParser = AutoParser()

// 3. Parse the content
val lyrics = autoParser.parse(lyricsContent)

// Now you have a unified SyncedLyrics object!
println(lyrics.metadata.title)
println(lyrics.lines.first().text)
```

### Contextual pronunciation captions (0.5.0)

Pass `fallbackPhoneticProvider` to `AutoParser` to enrich every supported format, including ordinary LRC `SyncedLine` captions. Standalone TTML parsing supports the same provider. `SyncedLine` now has optional `phonetic` and `languageTag` fields; enrichment preserves source timing, supplied captions, nested accompaniment and lyrics metadata.

Contextual providers override `PhoneticProvider.resolve(PhoneticRequest)`. The request contains the complete original text, UTF-16 language hint ranges and requested projection ranges. Resolve the complete context once; return a line caption and ordered `PhoneticProjection` values. `phonetic=null, aligned=true` deliberately omits a caption (for example Latin text), while `aligned=false` means word-to-fragment alignment is unknown and requires a line caption fallback. Projection `separatorBefore` is formatter-owned boundary metadata, propagated to `KaraokeSyllable.phoneticSeparatorBefore`; renderers preserve it when merging captions. Legacy string providers remain supported. `SyncedLyrics.withPhonetics(provider)` exposes the same enrichment outside parsers.

### Parsing a Specific Format

If you know the exact format, you can use a specific parser directly.

```kotlin

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
class MyCustomParser : ILyricsParser {
    override fun canParse(content: String): Boolean {
        // Check if your parser can parse or not
        // Example: check for a unique tag
        return content.startsWith("##MY_COOL_LYRICS##")
    }

    override fun parse(lines: List<String>): SyncedLyrics {
        // Your parsing logic here...
    }

    override fun parse(content: String): SyncedLyrics {
        // Your parsing logic here...
    }
}
```

### Step 2: Register with `AutoParser`

Pass your custom parser to the `AutoParser` constructor. Custom formats are checked in the order they are provided in the list, ensuring they are prioritized over built-in ones if placed first.

```kotlin
// Build an AutoParser instance with your custom parser alongside built-in ones
val autoParser = AutoParser(
    listOf(
        MyCustomParser(), // Checked first
        KugouKrcParser,
        TTMLParser,
        LyricifySyllableParser,
        EnhancedLrcParser,
        LrcParser
    )
)

// This parser now understands both built-in and your custom format!
val lyrics = autoParser.parse(myCustomLyricsContent)
```
## 💬 Community & Support

Join the community, ask questions, and share your projects!

- **Telegram:** [**mocha\_pot**](https://t.me/mocha_pot)
- **GitHub Issues:** [**Create an Issue**](https://www.google.com/search?q=https://github.com/6xingyv/Accompanist-Lyrics/issues)

## 🤝 Contributing

Contributions are welcome\! Please feel free to submit a pull request or open an issue to discuss your ideas. For major changes, please open an issue first.


## 📜 License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](http://www.apache.org/licenses/LICENSE-2.0.txt) file for details.

TTML supplied transliterations take precedence over generated captions. Supplied caption fragments attach to the original `KaraokeSyllable.phonetic` and inherit its timing. The importer groups finer metadata fragments using association hints without requiring identical timing boundaries, changing the source syllables, or exposing a separate pronunciation clock. Untimed one-to-one fragments remain supported; plain line metadata remains a line caption.
