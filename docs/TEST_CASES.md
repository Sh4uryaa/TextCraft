# TextCraft manual test cases

Run each case in **Light**, then repeat the starred (★) cases in **Dark** and with **Lite mode** on.
Cases needing keys/database say so; without them the app must stay usable (offline mode).

| # | Area | Steps | Expected |
|---|------|-------|----------|
| 1 | Start | Run the app with no `config/app.properties` | Window opens in a few seconds, welcome screen, toast "Offline mode" |
| 2 | Welcome ★ | Look at welcome screen in both themes | New, Open, Open from cloud, Sign in, Recent list readable (contrast) |
| 3 | New ★ | File > New | A4 page centred on grey/dark workspace, cursor in page |
| 4 | Typing | Type 3 lines | Words/Chars in status bar update; "Unsaved changes" appears |
| 5 | Bold/Italic/Underline/Strike ★ | Select text, press B, I, U, S (toolbar and Ctrl+B/I/U) | Format applied; buttons show pressed state when caret is in formatted text |
| 6 | Fonts and size | Pick font and size from toolbar, use − / + | Selection changes; combos follow the caret |
| 7 | Colours ★ | Text colour > Choose; highlight > Choose; Text colour > Automatic | Colour/highlight applied; Automatic removes the stored colour |
| 8 | Automatic colour ★ | Type plain text, switch theme (Ctrl+Alt+D) | Text is dark on light page, light on dark page, no restart |
| 9 | Chosen colour | Make text red, switch theme | Red stays red in both themes |
| 10 | Headings | Ctrl+Alt+1/2/3 and Format > Paragraph styles > Title/Normal | Size/weight change; toolbar style box follows caret |
| 11 | Align and spacing | Format > Align, Line spacing 1.5 / 2.0 | Paragraph changes |
| 12 | Lists | Toolbar • and 1. on three lines; press again | Prefixes added; second press removes them |
| 13 | Clear formatting | Format > Clear formatting on styled text | Plain text remains |
| 14 | Zoom | Toolbar zoom 150%, View > Zoom 75% | Text and page scale; text unchanged |
| 15 | Undo/Redo | Type, format, Ctrl+Z / Ctrl+Y | Steps undone/redone |
| 16 | Find | Ctrl+F, search a word, Find next | Match selected, wraps around |
| 17 | Replace all | Replace all with Match case, Whole word, Regex `\d+` | Counts shown; one Ctrl+Z per replacement |
| 18 | Word count | View > Word count | Words, characters, no-spaces, paragraphs correct |
| 19 | Insert | Insert > Date/time, Horizontal line, Special characters | Inserted at caret |
| 20 | Save TXT | Save As > Plain text | File saved; title bar loses `*`; "Saved" label |
| 21 | Save DOCX | Save As > Word; open in Word | Bold/italic/size/headings preserved |
| 22 | Save HTML / MD / RTF | Save As each format and reopen in TextCraft | Text and bold survive |
| 23 | Export PDF | File > Export > PDF | White page, black text even in Dark theme; toast about layout |
| 24 | Open PDF | Open any text PDF | Text appears; toast says layout is not preserved |
| 25 | Open DOCX/MD/RTF/HTML | Open one of each | Content and basic formatting shown |
| 26 | Unknown file | Open a `.bin` or odd extension | Binary warning; opens as plain text if confirmed |
| 27 | Unsaved prompt | Edit, then File > New | Dialog Save / Don't save / Cancel; Cancel keeps document |
| 28 | Autosave draft | Edit, wait 30 s, kill the app, restart | "Restore draft?" appears; Yes restores text |
| 29 | Recent files | Open 3 files; File > Open recent; welcome screen | Newest first, deleted files hidden |
| 30 | Print ★ | File > Print in Dark theme | Print preview/paper is white with black text |
| 31 | Theme ★ | View > Theme Light/Dark/System; theme button | Live switch; choice remembered after restart |
| 32 | Lite mode | View > Lite mode, restart | Setting remembered; no smooth scrolling; huge file (>200k chars) skips live word count |
| 33 | Side panel | Ctrl+Shift+A twice | Opens and closes; page keeps its position |
| 34 | Code mode ★ | Code > Code mode | Code editor with syntax colours (dark/light follow theme), formatting greyed, green Run, Format menu disabled |
| 35 | Language | Code > Language > C++ / Java on untouched template | Template switches, syntax colours change |
| 36 | Run (online) ★ | Python Hello World, F5 | Output, status and time shown within 10 s (needs internet) |
| 37 | Run stdin | `print(input())` with text in the Input box | Echoed |
| 38 | Run errors | Syntax error in code | Compile/stderr shown in red; "Explain error with AI" enabled |
| 39 | Stop / timeout | `while True: pass`, F5 then Stop; again without Stop | Run stops; otherwise timeout message after 10 s |
| 40 | Local runner | Set `code.runner=local`, turn off internet | Runs using installed python/g++/javac, or a clear "not installed" message |
| 41 | Offline code run | Internet off, `code.runner=judge0` | Friendly "You are offline" message |
| 42 | AI (no key) | AI > Improve with no key | Error dialog explaining gemini.apiKey |
| 43 | AI (mock) ★ | `ai.provider=mock`; select text; AI > Fix grammar | Result in panel; Replace changes selection; Ctrl+Z restores |
| 44 | AI actions | Tone, Shorter, Longer, Summarize, Explain, Translate, Ask | Each returns in the panel; Summarize/Explain/Ask cannot Replace |
| 45 | AI no selection | Run an action with nothing selected | Uses whole document; Replace asks for confirmation |
| 46 | AI bad key / 429 / offline | Wrong key; many rapid calls; internet off | Friendly message each time, app keeps working |
| 47 | AI insert/copy/discard | Use Insert below, Copy, Discard | Text inserted below selection / copied / panel cleared |
| 48 | Sign in (needs OAuth + DB) | Avatar > Sign in with Google | Browser opens, avatar/initial shown, "Signed in as ..." |
| 49 | Email account | Create account, sign out, sign in; wrong password | Success; "Wrong email or password." |
| 50 | Auto-login | Restart after Google sign-in | Signed in without prompt |
| 51 | Save to cloud | File > Save to cloud | "Saved to cloud" label |
| 52 | Open from cloud | File > Open from cloud, search, sort | Title search works; double-click opens |
| 53 | Version history | Save twice with changes; File > Version history; Restore | Two versions listed with preview; restore marks document unsaved |
| 54 | Rename / delete | Rename title; File > Delete cloud copy | Title changes in cloud list; document removed after confirm |
| 55 | Per-user data | Sign in as user B | User A's documents not listed |
| 56 | DB down | Stop MySQL, use cloud commands | Message "Cloud storage is not available", local files still work |
| 57 | Voice (needs key, mic) | Ctrl+Alt+M, speak, Ctrl+Alt+M | Text inserted; "Voice minutes x/60" grows |
| 58 | Voice commands | Say "hello comma world full stop new line" | `Hello, world.` then a line break |
| 59 | Voice errors | No mic / no key / internet off / quota reached | Clear message each time, no crash |
| 60 | Voice warning | Pretend 50 minutes used (edit `~/.textcraft/voice_usage.properties`) | Warning toast after next recording; blocked at the cap |
| 61 | Read aloud | Select text, Ctrl+Alt+R; Pause; Stop | OS voice reads selection or document |
| 62 | Study flashcards | Study > Flashcards on 200+ words | Cards flip, next/prev, Got it/Review, shuffle, progress |
| 63 | Study quiz | Study > Quiz (5/10/15) | One question at a time, instant feedback, score, review of wrong answers |
| 64 | My study sets | Signed in: generate, then Study > My study sets; open, export CSV, delete | Saved sets listed; CSV opens in a spreadsheet |
| 65 | Study bad JSON | Use mock reply with invalid JSON (test) | Retried once, then a friendly error |
| 66 | Shortcuts | Press every shortcut in Help > Shortcuts | Each does what it says |
| 67 | Small screen | Resize window to 1366x768 and 900x520 | Nothing important cut off |
| 68 | Old PC | Run with `-Xmx512m` on 2 GB machine, Lite mode | Usable; startup under about 5 s |
