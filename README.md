# TextCraft

A lightweight Google Docs-style desktop editor in **Java 11 + Swing** (University project).
Unlike Google Docs it edits many formats (DOCX, PDF text, MD, RTF, HTML, TXT and code files) and adds Gemini text
polishing, a Java/C++/Python code runner, voice typing, read aloud, Study mode (flashcards and quizzes),
Google sign-in with MySQL cloud storage, light/dark themes, and runs on old PCs (Lite mode).
The full requirements are in `SPEC.md`.

## Run
Needs JDK 11+ and Maven.
```
mvn package                      # builds target/textcraft-1.0.0.jar and runs the unit tests
java -Xmx512m -jar target/textcraft-1.0.0.jar
```
With **no configuration** the app starts in *offline mode*: editing, files, themes, code runner (online or local tools),
find/replace all work. Cloud, AI, voice and Google sign-in need the setup below.
Native installer: `mvn package`, then `scripts/package.sh` (Windows: `scripts\package.bat`); it builds a small runtime
with `jlink` from the same JDK and packages it with `jpackage` (`-Xmx512m`).

## Setup (all optional, all in `config/app.properties`, copy from `config/app.properties.example`)
The file is git-ignored. Secrets never go anywhere else; Google tokens live only in `~/.textcraft/`.

1. **MySQL 8** - create a database and a *restricted* user:
   ```sql
   CREATE DATABASE textcraft CHARACTER SET utf8mb4;
   CREATE USER 'textcraft_app'@'%' IDENTIFIED BY 'a-strong-password';
   GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, INDEX, REFERENCES ON textcraft.* TO 'textcraft_app'@'%';
   ```
   (`CREATE` is only needed once: the app runs `schema.sql` when tables are missing. After that you can `REVOKE CREATE, INDEX, REFERENCES`.)
   Only `db.url` differs between **local**, **Cloud SQL** (public IP + authorized network, or the Cloud SQL Auth Proxy on `127.0.0.1`)
   and a **VM**. Note: Cloud SQL is *not* always-free; set a **billing budget alert** in Google Cloud.
2. **Gemini** - create a key in Google AI Studio -> `gemini.apiKey`. Default model `gemini-2.0-flash` (change in Settings or `gemini.model`).
3. **Google sign-in** - Google Cloud Console -> APIs & Services -> OAuth consent screen (External, *Testing*, add your **test users**) ->
   Credentials -> OAuth client ID of type **Desktop app** -> `google.clientId` / `google.clientSecret`.
   Scopes used: `openid email profile`. Fallback: email + password accounts (PBKDF2 with salt).
4. **Voice typing** - enable *Cloud Speech-to-Text API*, create an API key and **restrict it to the Speech-to-Text API** -> `speech.apiKey`.
   The status bar shows *Voice minutes x/60*, warns at 50 and blocks at `voice.monthlyCapMinutes` (free tier is 60 minutes/month).
5. **Code runner** - default Judge0 CE at `https://ce.judge0.com` (`judge0.url`). `code.runner=local` uses `python3`/`g++`/`javac` on the PC;
   `auto` (default) tries Judge0 first and falls back to local tools when offline.

## How to use
Menus follow Google Docs: File, Edit, View, Insert, Format, Tools, AI, Study, Code, Help (Help > Shortcuts lists all keys).
Select text, choose an AI action; the result appears in the side panel and **nothing is replaced until you click Replace**.
Code > Code mode switches to the code editor with a green Run button (F5).
Rich documents are stored in the cloud as HTML, code documents as raw text; each cloud save adds a version.

## Project layout (`com.textcraft`)
| Package | Role |
|---|---|
| `app` | `Main`, `AppConfig` (config loaded once), `AppContext` (creates services lazily) |
| `model` | plain data classes (encapsulation) |
| `db`, `dao` | `DatabaseConnection` (Singleton), `SchemaInitializer`, `GenericDAO<T,ID>` and one DAO per table (PreparedStatement only) |
| `auth` | `GoogleAuthService` (loopback OAuth + PKCE), `SessionManager` (Observer) |
| `service` | facades used by the UI: documents (transactions), auth, AI, code run, voice, autosave, settings, recent files |
| `ai`, `study` | `AIProvider` (Gemini / Mock), `PromptBuilder`, `StudyService` (strict JSON, retry once) |
| `compiler` | `CodeRunner` (Judge0 / local process), `Language` enum |
| `voice` | `SpeechRecognizer` (Google REST), `QuotaGuard`, `TextToSpeechService` (OS voices), `MicRecorder` |
| `io` | `FileFormatHandler` per format, `FileHandlerFactory`, `HtmlCodec` |
| `ui` | `MainFrame`, menus, toolbar, editors, panels, dialogs, `ThemeManager`, `ErrorHandler` |
| `exception`, `util` | custom checked exceptions; password, text, threading and HTTP helpers |

Rules the code follows: the UI never touches SQL or HTTP; all network/DB/file/audio work runs on a shared 4-thread pool and returns with `invokeLater`;
every layer throws its own exception and one `ErrorHandler` shows it. Key `// OOP:` comments mark encapsulation, inheritance, interfaces/polymorphism,
generics, collections, enums, Singleton/Factory/Strategy/DAO/Observer/Facade, try-with-resources, multithreading and JDBC transactions.

## Documentation
`docs/` has PlantUML diagrams (class, use case, ER, architecture, sequences for sign-in, save to cloud, AI refine, run code, flashcards, voice)
and `docs/TEST_CASES.md` (68 manual cases covering both themes and Lite mode). Render the diagrams with any PlantUML tool.

## Defaults chosen (no questions asked, as the spec requires)
- **System theme** = light unless started with `-Dtextcraft.systemDark=true` (the JDK has no portable dark-mode detection).
- **Toolbar icons** are Unicode glyphs that take the theme text colour automatically (instead of SVG files).
- **Google sign-in** uses the JDK `HttpServer` + `HttpClient` + Gson with PKCE rather than `google-oauth-client`: fewer dependencies for old PCs, same loopback flow.
- **Lists** are stored as text prefixes ("• " / "1. ") so every export format keeps them.
- **Unsaved-changes "Save"** waits for the save to finish (the work runs off the UI thread, the dialog flow waits).
- **Default font/size** from Settings applies to text without its own formatting; DOCX/PDF export of unformatted text uses Arial 12.
- **PDF**: opening extracts text only (the UI says layout is not preserved); exporting writes a plain white A4 page.
- **Pause** while reading aloud works on macOS/Linux; on Windows Pause stops (PowerShell speech cannot be paused portably).
- Optional extras (runnable code blocks in documents, focus mode, templates, spell check) were left out; the "Explain error with AI" button for failed runs is included.
