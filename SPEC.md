# PART A: TextCraft Build Prompt (compact)

Build **TextCraft**, a lightweight Google Docs-style desktop editor in Java Swing. Unlike Google Docs it edits many formats (DOCX, PDF text, MD, RTF, HTML, TXT, code files), and adds Gemini text polishing, a Java/C++/Python code runner, Google voice typing, read aloud, Study mode (flashcards/quizzes), Google sign-in with JDBC cloud storage, light/dark themes, and runs on old PCs. University project: clean, commented, easy to explain in a viva. Choose the simplest solution.

## Working rules (save tokens)
- Save this spec as `SPEC.md`; implement ONE build step per request; do not reread or reprint it.
- Edit files instead of rewriting them; never reprint whole files in chat.
- After each step reply in at most 5 lines: done / how to run / next. No explanations.
- Run build and tests after each step and fix errors yourself.
- Never ask questions: pick a sensible default and note it in the README.

## 1. Stack and compatibility
- **Java 11 baseline** (`--release 11`, no newer APIs), Maven; choose library versions that support Java 11.
- Swing + FlatLaf (light/dark), `JTextPane`/`StyledDocument`, RSyntaxTextArea (use its light/dark themes).
- MySQL 8 via JDBC; URL/user/password in config (local, Cloud SQL or VM must all work unchanged).
- Gemini REST (`java.net.http.HttpClient` + Gson); model name in config, default a Flash-class model.
- Voice: Google Speech-to-Text **V1 REST** (`speech:recognize`) over HttpClient, mic via `javax.sound.sampled`. No gRPC client (too heavy for old PCs).
- Google sign-in: `google-oauth-client`, desktop loopback flow, scopes `openid email profile`.
- Files: Apache POI (docx), PDFBox (read PDF), OpenPDF (write PDF), flexmark (MD), JDK RTF/HTML kits.
- Code runner: Judge0 CE HTTP (configurable URL, language IDs from `/languages`); optional local `python`/`g++`/`javac`.
- Read aloud: OS voices via `ProcessBuilder` (PowerShell / `say` / `espeak`).
- JUnit 5. Package with jpackage/jlink bundling a runtime built by the same JDK.
- Secrets only in git-ignored `config/app.properties` (+ `app.properties.example`). TLS 1.2+ only.

## 2. Old-hardware (Lite) requirements
Target 2 GB RAM, dual-core, 1366x768. Lazy-load AI, voice, code runner and study panels on first use. Default `-Xmx512m`. Settings toggle **Lite mode**: no shadows/animations, undo history capped at 100, no live word-count on huge files. Startup window within about 5 s on an old HDD.

## 3. UI (Docs-style, professional)
Menu bar + ONE toolbar row; everything else in dropdowns. Centered A4-style page on a workspace background, collapsible right side panel (AI/Study), bottom output panel (Code mode only), status bar (words, chars, autosave time, voice minutes x/60, mode), top-right theme button and Google avatar menu, editable title with "Saved to cloud".

Menus:
- **File**: New, Open, Open from cloud, Save to cloud, Save As (format), Make a copy, Version history, Export (PDF/DOCX/HTML/MD/TXT/RTF), Rename, Delete cloud copy, Print, Sign in/out, Exit
- **Edit**: Undo, Redo, Cut, Copy, Paste, Select all, Find & replace
- **View**: Zoom, Theme (Light/Dark/System), Lite mode, Word count, Side panel
- **Insert**: Horizontal line, Date/time, Special characters
- **Format**: Bold, Italic, Underline, Strike, Paragraph styles (Normal, Title, H1-H3), Align, Line spacing, Lists, Clear formatting
- **Tools**: Voice typing, Voice language, Read aloud, Stop, Settings
- **AI**: Fix grammar, Improve, Tone (Formal/Casual/Friendly/Professional), Shorter, Longer, Summarize, Explain, Translate, Ask AI
- **Study**: Flashcards, Quiz, Key points, My study sets
- **Code**: Code mode, Language (Python/C++/Java), Run (F5), Stop, Clear
- **Help**: Shortcuts, About

Toolbar: undo, redo, print, zoom, paragraph style, font, size -/+, B I U S, text/highlight color, align, lists, AI, mic, read aloud. SVG icons recolored per theme, tooltips with shortcuts, pressed state follows selection. In Code mode formatting buttons are greyed (not hidden) and a green Run button appears.

Shortcuts: Ctrl+N/O/S/Shift+S/Z/Y/B/I/U/F/P, F5 run, Ctrl+Alt+1/2/3 headings, Ctrl+Shift+A side panel, Ctrl+Alt+M voice, Ctrl+Alt+R read aloud, Ctrl+Alt+F flashcards, Ctrl+Alt+D theme.

### Themes
`ThemeManager` + `Theme` enum (LIGHT, DARK, SYSTEM), live switch without restart, saved per user.
- Light: workspace `#F1F3F4`, surface `#FFFFFF`, page `#FFFFFF`, border `#DADCE0`, text `#202124`, text2 `#5F6368`, accent `#1A73E8`, selection `#C2DBFF`, ok/warn/error `#188038/#F9AB00/#D93025`
- Dark: workspace `#121316`, surface `#1E1F22`, page `#1F2023`, border `#3C4043`, text `#E8EAED`, text2 `#9AA0A6`, accent `#8AB4F8`, selection `#2F4F7F`, ok/warn/error `#81C995/#FDD663/#F28B82`
- UI font Inter/Segoe UI 13px, page font Arial 12, code font Consolas/JetBrains Mono 13px; 8px grid, rounded corners, hover/pressed/focus states, WCAG AA contrast.
- **Default text color is "automatic"** (not stored as black): follows the theme. Only user-picked colors are stored. Export/print/PDF always use a white page with black automatic text. Code editor syntax theme switches too.
- Welcome screen: New, Open, Open from cloud, Recent, Sign in with Google. Toasts bottom-right.

## 4. Features
1. **Editing**: fonts, sizes, styles, colors, alignment, headings, lists, spacing, zoom, find/replace (case, word, regex), undo/redo, word/char count, unsaved-changes prompt (Save/Don't save/Cancel), discard, autosave draft every 30 s, version history with restore.
2. **Formats** (`FileFormatHandler` per type + `FileHandlerFactory`): txt/code/json/xml/csv/log (plain), md, rtf, html, docx (paragraphs, bold, italic, size, headings), pdf (text extraction on open, export on save; say in UI that layout is not preserved), unknown extension = plain text with binary warning.
3. **Gemini AI** (`AIProvider` interface, `GeminiProvider`, `MockAIProvider` for tests/offline): `generateContent`, key in `x-goog-api-key` header. Select text, choose an action, result appears in the side panel with Replace / Insert below / Copy / Discard; never overwrite without confirmation. Prompts: "return only the rewritten text, keep meaning and language". Background thread; handle timeout, bad key, HTTP 429, offline. Log to `ai_history`.
4. **Code runner**: Code mode = RSyntaxTextArea; `CodeRunner` interface, `Judge0Runner`, optional `LocalProcessRunner`; language dropdown, Run, Stop, stdin box, output panel (stdout/stderr/compile errors/exit status/time), 10 s timeout, Hello World templates. Log to `compile_history`.
5. **Google sign-in + cloud**: OAuth on first login creates `users` row (google_id = `sub`, email, name, avatar); refresh token in `~/.textcraft/` for auto-login. Fallback email+password (PBKDF2 + salt). Per-user data only. **JDBC CRUD, `PreparedStatement` only**: create, list (search by title, sort by date), open, update (adds version row, in a transaction), delete (confirm).
6. **Voice typing**: `SpeechRecognizer` interface, `GoogleSpeechRecognizer`. Push-to-talk: record up to 60 s (16 kHz 16-bit mono), call REST, insert text at caret. Language dropdown (en-US, en-IN, hi-IN). Spoken commands: new line, new paragraph, full stop, comma, question mark. **Quota guard**: save seconds per month in `voice_usage`, show "Voice minutes x/60", warn at 50, block above a configurable cap. Handle no mic / offline / missing key / quota.
7. **Read aloud**: `TextToSpeechService` interface, `SystemTtsService`; selection or whole document, pause/stop, speed.
8. **Study mode**: `StudyService` uses `AIProvider`; prompts demand strict JSON (flashcards: question/answer; quiz: question, 4 options, correct letter, explanation); Gson parse, validate, retry once. Choose 5/10/15 items. Flashcard view (flip, next/prev, got it/review, shuffle, progress); quiz view (one question, instant feedback, score, review wrong). Save sets and attempts linked to the document; "My study sets" dialog (open, delete, export CSV).
9. **Offline mode**: if DB or internet is unavailable, show a message and continue with local files.
10. **Optional extras (only after the core works)**: runnable code blocks inside documents, AI error explainer for failed runs, focus mode, templates, spell check.

## 5. Database (`schema.sql`, MySQL; FKs ON DELETE CASCADE, document_id SET NULL)
- users(id, google_id UNIQUE null, display_name, avatar_url, username UNIQUE null, email UNIQUE not null, password_hash null, salt null, created_at)
- documents(id, user_id, title, doc_type RICH|CODE, language, content LONGTEXT [HTML for rich, raw for code], created_at, updated_at)
- document_versions(id, document_id, content LONGTEXT, saved_at)
- ai_history(id, user_id, action, input_text, output_text, model, created_at)
- compile_history(id, user_id, language, source_code, stdin_text, output_text, status, run_ms, created_at)
- user_settings(user_id PK, theme LIGHT|DARK|SYSTEM, default_font, default_font_size, gemini_model, voice_language, voice_speed, lite_mode)
- voice_usage(user_id, month 'YYYY-MM', seconds_used; PK user_id+month)
- flashcard_sets(id, user_id, document_id, title, created_at); flashcards(id, set_id, question, answer)
- quizzes(id, user_id, document_id, title, created_at); quiz_questions(id, quiz_id, question, option_a..option_d, correct_option, explanation); quiz_attempts(id, quiz_id, user_id, score, total, taken_at)

## 6. Architecture (`com.textcraft`, MVC + DAO; the UI never touches SQL or HTTP)

### 6.1 Layers (calls go downward only)
```
UI (Swing)         MainFrame, panels, dialogs, ThemeManager
  | calls
Services           AuthService, DocumentService, AIService, StudyService,
  |                CodeRunService, VoiceService, AutoSaveService
  | uses
Providers / DAOs   AIProvider, CodeRunner, SpeechRecognizer, TextToSpeechService,
  |                FileFormatHandler  |  GenericDAO<T,ID> + per-table DAOs
  | talks to
External           Gemini, Google OAuth, Speech-to-Text REST, Judge0, MySQL,
                   local files, OS voices
```

### 6.2 Deployment view
```
TextCraft desktop app (Java 11 + bundled runtime)
  |-- JDBC  --> MySQL (local / Cloud SQL / VM)
  |-- HTTPS --> Google OAuth (sign-in)
  |-- HTTPS --> Gemini API (AI + Study)
  |-- HTTPS --> Speech-to-Text REST (voice)
  |-- HTTPS --> Judge0 (code run)
  |-- local --> files, ~/.textcraft/ (refresh token), OS text-to-speech
```

### 6.3 Packages and responsibilities
- `app`: Main (startup), AppConfig (loads `config/app.properties` once)
- `model`: User, Document, DocumentVersion, CompileResult, Flashcard, QuizQuestion (private fields, getters/setters)
- `db`: DatabaseConnection (Singleton, `getConnection()`), SchemaInitializer (runs `schema.sql` if tables are missing)
- `dao`: GenericDAO<T,ID> + UserDAO, DocumentDAO, VersionDAO, SettingsDAO, AIHistoryDAO, CompileHistoryDAO, StudyDAO, VoiceUsageDAO (PreparedStatement only)
- `auth`: GoogleAuthService (OAuth loopback, token storage), SessionManager (current user + SessionListener)
- `service`: facades used by the UI. DocumentService (save/open/versions/transactions), AIService, CodeRunService, VoiceService, AutoSaveService
- `ai`: AIProvider, AbstractAIProvider, GeminiProvider, MockAIProvider, PromptBuilder (one prompt per AIAction/Tone)
- `study`: StudyService, StudyPromptBuilder (JSON prompts, parse, validate, retry once)
- `compiler`: CodeRunner, Judge0Runner, LocalProcessRunner, Language (enum)
- `voice`: SpeechRecognizer, GoogleSpeechRecognizer, QuotaGuard, TextToSpeechService, SystemTtsService
- `io`: FileFormatHandler, AbstractFormatHandler, Txt/Md/Rtf/Html/Docx/PdfHandler, FileHandlerFactory
- `ui`: MainFrame, MenuBarBuilder, ToolBarBuilder, RichEditorPanel, CodeEditorPanel, AIPanel, StudyPanel, FlashcardView, QuizView, OutputPanel, StatusBar, dialogs, ThemeManager, Toast, ErrorHandler
- `exception`: DocumentException, AIServiceException, CompilerException, DatabaseException, VoiceException
- `util`: PasswordUtil (PBKDF2), TextUtils, Debouncer

### 6.4 Key interfaces
```java
interface FileFormatHandler { boolean supports(String ext);
  StyledDocument read(File f) throws DocumentException;
  void write(StyledDocument d, File f) throws DocumentException; }
interface AIProvider { String generate(String prompt) throws AIServiceException; }
interface CodeRunner { CompileResult run(Language l, String code, String stdin) throws CompilerException;
  void cancel(); }
interface SpeechRecognizer { String recognize(byte[] pcm16kMono, String langCode) throws VoiceException; }
interface TextToSpeechService { void speak(String text, double speed); void pause(); void stop(); }
interface GenericDAO<T,ID> { T save(T t); Optional<T> findById(ID id); List<T> findAll(); void delete(ID id); }
```

### 6.5 Runtime flows
- **Startup**: Main, load config, ThemeManager applies saved or system theme, show MainFrame with the welcome screen. In the background: connect to DB and try silent Google login with the refresh token. On failure continue in offline mode. Heavy panels (AI, Study, Output, voice) are created only on first use.
- **Google sign-in**: Profile menu, GoogleAuthService (worker thread) opens the browser and receives the code on localhost, gets tokens and profile, UserDAO finds or creates the user by `google_id`, SessionManager sets the user, UI shows the avatar and loads settings (theme, Lite mode).
- **Open file**: FileHandlerFactory picks the handler by extension, handler reads in a worker, the editor shows the StyledDocument.
- **Save to cloud**: DocumentService converts the document to HTML (or raw text in Code mode), then one transaction: DocumentDAO upsert + VersionDAO insert, commit or rollback, toast.
- **AI refine**: selection, AIService (worker), PromptBuilder, `AIProvider.generate`, AIPanel shows the result, user clicks Replace (one undoable edit), AIHistoryDAO insert.
- **Run code**: CodeRunService (worker), Judge0Runner submits and polls with a 10 s timeout, OutputPanel shows the result, CompileHistoryDAO insert.
- **Voice typing**: QuotaGuard check, record up to 60 s on a thread, `recognize` over REST, insert text at the caret, VoiceUsageDAO adds the seconds.
- **Study**: StudyService, JSON prompt, AIProvider, Gson parse and validate (retry once), StudyDAO save, FlashcardView or QuizView.
- **Theme switch**: ThemeManager sets the theme, FlatLaf updates the UI, ThemeChangeListeners switch the code-editor syntax theme, SettingsDAO saves the choice.

### 6.6 Cross-cutting rules
- **Threads**: the Swing EDT only draws UI. All network, DB, file and audio work runs on SwingWorker or a shared ExecutorService (4 threads) and returns results with `invokeLater`. Autosave uses a ScheduledExecutorService (30 s).
- **Errors**: each layer throws its own custom exception; services translate low-level errors; one `ErrorHandler` shows a toast or dialog; log with `java.util.logging`.
- **Observer**: DocumentChangeListener, ThemeChangeListener, SessionListener (login/logout refreshes menus and avatar).
- **Patterns**: Singleton (DatabaseConnection), Factory (FileHandlerFactory), Strategy (AI, code runner, speech, TTS), DAO, Facade (services), Observer.
- **Data**: rich documents stored as HTML, code documents as raw text; secrets only in config; tokens only in `~/.textcraft/`.

### 6.7 Class relationships (for the UML)
MainFrame has one editor panel at a time (RichEditorPanel or CodeEditorPanel) plus lazy AIPanel, StudyPanel, OutputPanel. Services depend on interfaces, never on concrete classes. GeminiProvider and MockAIProvider extend AbstractAIProvider, which implements AIProvider. Each format handler extends AbstractFormatHandler, which implements FileFormatHandler. Each DAO implements GenericDAO. User 1-* Document, Document 1-* DocumentVersion, User 1-* FlashcardSet/Quiz, FlashcardSet 1-* Flashcard, Quiz 1-* QuizQuestion.

## 7. Java concepts that must be visible (tag with short `// OOP:` comments)
Encapsulation (model POJOs); inheritance (AbstractFormatHandler, AbstractAIProvider); polymorphism and interfaces (FileFormatHandler, AIProvider, CodeRunner, SpeechRecognizer, TextToSpeechService, GenericDAO); custom checked exceptions + try-with-resources + global error dialog; multithreading (SwingWorker/ExecutorService for AI, code runs, OAuth, mic, speech, TTS, autosave via ScheduledExecutorService); collections (handler registry Map, List<Document>, Deque recent files, Set extensions); generics (GenericDAO<T,ID>, Result<T>); streams/lambdas (filter/sort/search documents, word stats, menu building); enums (Language, DocType, AIAction, Tone, Theme); patterns (Singleton, Factory, Strategy, DAO, Observer); JDBC transactions.

## 8. Quality, tests, deliverables
- Never crash: friendly dialog/toast for DB down, bad login, offline, bad API key, missing credentials, quota, unreadable file. Close all JDBC resources; restricted DB user.
- Unit tests: PasswordUtil, each file handler round-trip, PromptBuilder, Study JSON parsing (MockAIProvider), QuotaGuard, DocumentDAO CRUD (SQLite or test schema), word count, theme color mapping. `docs/TEST_CASES.md` with 30+ manual cases covering both themes and Lite mode.
- Deliver: Maven project, `schema.sql`, `app.properties.example`, README (setup: Google Cloud OAuth client with test users, Speech API key restricted to the Speech API, Gemini key from AI Studio, MySQL local/Cloud SQL/VM with restricted user, billing budget alert; note Cloud SQL is not always-free), PlantUML in `docs/` (class, use case, ER, architecture, sequences for sign-in, save to cloud, AI refine, run code, flashcards, voice), runnable jar and jpackage profile.

## 9. Build steps (one per request)
1. Skeleton, FlatLaf, ThemeManager (light/dark), Docs layout with menus/toolbar stubs
2. Rich editor (formatting, automatic color, zoom, word count)
3. File handlers and open/save/save as
4. Database, email login, document CRUD, open from cloud, then Google sign-in and auto-login
5. Version history, autosave, recents, settings (theme, Lite mode)
6. Gemini panel, AI history, mock provider, tests
7. Code mode and Judge0 runner
8. Study mode
9. Voice typing (REST + quota guard) and read aloud
10. Polish, lazy loading, startup check, both themes on every screen, tests, docs
