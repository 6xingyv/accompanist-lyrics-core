# Accompanist Lyrics

A general-purpose, extensible lyrics library for Kotlin/JVM. Accompanist Lyrics supports multiple popular lyrics formats with a unified data model for easy integration into music players, karaoke apps, and more.

---

## Features

- **Multi-format support:** Parse LRC, Enhanced LRC (with voice separation and accompaniment), TTML (Apple Syllable), and Lyricify Syllable formats.
- **Karaoke-ready:** Provides syllable-level timing and alignment for karaoke-style highlighting.
- **Translation support:** Handles dual-language/translation lines natively.
- **Metadata extraction:** Reads standard tags (artist, album, title, offset, duration) from lyrics files.
- **Extensible:** Easy to add new formats by implementing the `ILyricsParser` interface.
- **Pure Kotlin/JVM:** No Android dependencies, suitable for desktop, server, and multiplatform projects.

---

## Installation

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.mocharealm.accompanist:accompanist-lyrics:0.0.1")
}
```

---

## Usage

```kotlin
import com.mocharealm.accompanist.lyrics.parser.LrcParser

val lrcLines = listOf(
    "[00:39.96]I lean in and you move away",
    "[00:39.96]我靠在里面，你就离开"
)
val lyrics = LrcParser.parse(lrcLines)
println(lyrics.lines)
```

You can also use `EnhancedLrcParser`, `TTMLParser`, or `LyricifySyllableParser` for other formats. All parsers return a unified `SyncedLyrics` data structure.

---

## Supported Formats

- **LRC**: Standard and dual-language LRC files
- **Enhanced LRC**: Syllable-level timing, voice separation, accompaniment
- **TTML (Apple Syllable)**: Used by Apple Music and others
- **Lyricify Syllable**: [Lyricify App format](https://github.com/WXRIW/Lyricify-App/blob/main/docs/Lyricify%204/Lyrics.md#lyricify-syllable-%E6%A0%BC%E5%BC%8F%E8%A7%84%E8%8C%83)

---

## Extending

To add a new format, implement the `ILyricsParser` interface:

```kotlin
interface ILyricsParser {
    fun parse(lines: List<String>): SyncedLyrics
}
```

---

## Contributing

Pull requests and issues are welcome! For major changes, please open an issue first to discuss what you would like to change.

---

## License

This project is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).
