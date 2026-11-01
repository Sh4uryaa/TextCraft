# TextCraft (NovaEdit)

[![GitHub](https://img.shields.io/badge/GitHub-Sh4uryaa%2FTextCraft-blue?logo=github)](https://github.com/Sh4uryaa/TextCraft)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21-green.svg)](https://openjfx.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **TextCraft (NovaEdit)** is a modern, high-performance desktop text and code editor built with **Java 21**, **JavaFX 21**, and **RichTextFX**. Developed by [@Sh4uryaa](https://github.com/Sh4uryaa). Engineered with a UI-independent core, virtualized text rendering, and a polished visual design system comparable to modern IDEs (VS Code, IntelliJ).

---

## Key Highlights

- **Every Feature Visible**: Every single feature is accessible through dedicated buttons on the grouped, responsive two-row toolbar, sidebar, status bar, or panels. No feature is hidden solely behind a shortcut or obscure menu.
- **High Performance PieceTable Core**: Implements a pure-Java `PieceTable` text buffer with a fast binary-search `LineIndex`, handling files up to 50 MB smoothly.
- **Grouped Typing Bursts & Compound Undo**: Intelligent `UndoManager` automatically groups consecutive typing bursts and merges compound actions (e.g., Replace All).
- **Dual Visual Theme System**: Live, instantaneous switching between Dark Theme (`#1e1f22`) and Light Theme (`#ffffff`) without application restarts.
- **Rich Syntax Highlighting**: Incremental, debounced tokenizers for Java 21, JSON, XML/HTML, and Markdown with automatic extension detection.
- **Integrated Source Control (JGit)**: Built-in Git branch tracking, status inspector (Modified, Untracked, Staged), commit message composer, and file staging.
- **Project Search & Side-by-Side Diff**: Fast virtual-thread workspace search with regex and glob filters; file comparison using `java-diff-utils`.
- **Integrated Terminal & Output Console**: Built-in shell runner for executing commands directly inside NovaEdit.
- **Fuzzy Command Palette (`Ctrl+Shift+P`)**: Search and execute any command with keybindings and categories.
- **Session Restore & Auto-Save**: Automatically recovers open tabs, cursor locations, and scroll positions across restarts with crash backups stored in `~/.novaedit/backup/`.
- **Extensible Plugin SPI**: Modular plugin architecture powered by `java.util.ServiceLoader`.

---

## UI Layout Overview

```
+----------------------------------------------------------------------------+
| Menu Bar:  File   Edit   Selection   View   Go   Tools   Git   Help        |
+----------------------------------------------------------------------------+
| TOOLBAR ROW 1: [New] [Open] [Save] | [Undo] [Redo] [Cut] [Copy] | [Find] ..|
| TOOLBAR ROW 2: [Fold] [Match] [Wrap] [Minimap] [Split] | [Palette] [Git] ..|
+---+---------------------+--------------------------------------------------+
| A | SIDE PANEL          | [Tab1.java x] [notes.md x] [+]     [Split] [..]  |
| C | (Explorer / Search  | Breadcrumb: src > main > java > App.java         |
| T |  / Git / Outline)   +--------------------------------------+-----------+
| I |                     | 1  | public class App {              | MINIMAP   |
| V |                     | 2  |     public static void main() { |           |
| I |                     | 3  |         System.out.println();   |           |
| T |                     +--------------------------------------+-----------+
| Y |                     | Find/Replace Bar (Match Case, Whole Word, Regex) |
|   |                     +--------------------------------------------------+
| B |                     | BOTTOM PANEL: Output | Terminal | Problems       |
| A |                     |                                                  |
| R |                     |                                                  |
+---+---------------------+--------------------------------------------------+
| STATUS BAR: Ln 12, Col 8 | Sel 0 | Spaces: 4 | UTF-8 | LF | Java | Git:main|
|             Zoom 100% | Theme Toggle | Notifications                       |
+----------------------------------------------------------------------------+
```

---

## Build and Run

### Prerequisites
- **JDK 21** or later
- **Apache Maven 3.8+**

### Quick Launch (Zero Configuration)
- **PowerShell**:
  ```powershell
  .\run.ps1
  ```
- **Command Prompt (CMD)**:
  ```cmd
  run.bat
  ```

### Running with Maven Wrapper
```powershell
.\mvnw.cmd clean javafx:run
```

### Running with Global Maven
```bash
mvn clean javafx:run
```
*(Note: If you have just installed Java/Maven, restart your terminal or run `$env:Path = [System.Environment]::GetEnvironmentVariable("Path","User") + ";" + [System.Environment]::GetEnvironmentVariable("Path","Machine")` to reload environment variables).*

### Running the Test Suite
Unit tests for `PieceTable` (including a 5,000-iteration random-edit fuzz test against a `StringBuilder` oracle), `LineIndex`, `UndoManager`, `FindReplaceService`, `IndentService`, `BracketMatcher`, and `SessionManager`:
```bash
mvn clean test
```

### Packaging an Executable Fat JAR
Produces a self-contained executable JAR in `target/novaedit-1.0.0-SNAPSHOT.jar`:
```bash
mvn clean package -DskipTests
java -jar target/novaedit-1.0.0-SNAPSHOT.jar
```

### Native Image Packaging (`jpackage`)
```bash
mvn package -Pjpackage
```

---

## Keyboard Shortcuts

| Action | Shortcut | Description |
|---|---|---|
| **New File** | `Ctrl+N` | Open a new blank untitled document |
| **Open File** | `Ctrl+O` | Open file from local filesystem |
| **Open Folder** | `Ctrl+K Ctrl+O` | Open folder into File Explorer and workspace |
| **Save** | `Ctrl+S` | Save current file |
| **Save As** | `Ctrl+Shift+S` | Save current file with a new path |
| **Save All** | `Ctrl+Alt+S` | Save all open dirty tabs |
| **Close Tab** | `Ctrl+W` | Close current tab (prompts if unsaved) |
| **Reopen Tab** | `Ctrl+Shift+T` | Reopen recently closed tab |
| **Undo / Redo** | `Ctrl+Z` / `Ctrl+Y` | Undo or redo edit commands |
| **Cut / Copy / Paste** | `Ctrl+X` / `Ctrl+C` / `Ctrl+V` | Standard clipboard operations |
| **Select All** | `Ctrl+A` | Select entire document |
| **Select Next Match** | `Ctrl+D` | Add selection to next occurrence |
| **Find** | `Ctrl+F` | Open Find/Replace slide-in bar |
| **Replace** | `Ctrl+H` | Open Find/Replace bar with replace field |
| **Find in Files** | `Ctrl+Shift+F` | Open Search side panel |
| **Go to Line** | `Ctrl+G` | Jump to specific line and column |
| **Toggle Comment** | `Ctrl+/` | Comment or uncomment current lines |
| **Duplicate Line** | `Ctrl+Shift+D` | Duplicate current line downward |
| **Move Line Up/Down**| `Alt+Up` / `Alt+Down` | Move active line up or down |
| **Delete Line** | `Ctrl+Shift+K` | Delete current line |
| **Match Bracket** | `Ctrl+M` | Jump to matching brace, bracket, or paren |
| **Insert Snippet** | `Ctrl+J` | Expand snippet template |
| **Autocomplete** | `Ctrl+Space` | Trigger autocompletion popup |
| **Toggle Word Wrap** | `Alt+Z` | Enable or disable text wrapping |
| **Command Palette** | `Ctrl+Shift+P` | Open fuzzy command palette |
| **Toggle Terminal** | `Ctrl+` ` | Show or hide bottom terminal panel |
| **Toggle Theme** | `Alt+T` | Switch live between Dark and Light mode |
| **Zoom In / Out / Reset**| `Ctrl++` / `Ctrl+-` / `Ctrl+0` | Adjust font size (also Ctrl+Scroll) |
| **Zen Mode** | `F11` | Fullscreen distraction-free mode |
| **Settings** | `Ctrl+,` | Open preferences and keymap editor |

---

## Plugin Authoring Guide

NovaEdit provides a plugin API using standard Java SPI (`java.util.ServiceLoader`).

### 1. Implement the `Plugin` Interface
```java
package com.myplugin;

import com.novaedit.plugin.Plugin;
import com.novaedit.plugin.PluginContext;
import com.novaedit.commands.Command;

public class MyCustomPlugin implements Plugin {
    @Override
    public String getId() {
        return "com.example.customplugin";
    }

    @Override
    public String getName() {
        return "Custom Utility Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad(PluginContext context) {
        context.registerCommand(new Command(
            "plugin.myaction",
            "Say Hello",
            "Tools",
            "Ctrl+Alt+H",
            "spell",
            () -> context.showToast("Hello from Custom Plugin!")
        ));
    }
}
```

### 2. Register via `META-INF/services`
Add a file named `META-INF/services/com.novaedit.plugin.Plugin` containing:
```
com.myplugin.MyCustomPlugin
```
NovaEdit automatically discovers and initializes the plugin on startup.

---

## Architecture Summary

See [ARCHITECTURE.md](file:///c:/Users/krish/Desktop/MyProjects/TextCraft/ARCHITECTURE.md) for full details on:
- Piece table data structures and line-indexing mechanics
- Concurrency and virtual-thread worker dispatching
- Debounced incremental syntax tokenization
- Multi-cursor, undo grouping, and session persistence models

---

## Author & License

- **Author**: [Sh4uryaa](https://github.com/Sh4uryaa)
- **Repository**: [https://github.com/Sh4uryaa/TextCraft](https://github.com/Sh4uryaa/TextCraft)
- **License**: MIT License. Copyright (c) 2026 Shaurya.