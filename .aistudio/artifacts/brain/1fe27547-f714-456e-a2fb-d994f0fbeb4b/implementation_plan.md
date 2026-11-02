# Fountain Studio — Screenplay Editor & Live Formatter

A distraction-free Android screenplay studio for writing in the Fountain plain-text markup format, featuring real-time syntax highlighting, an industry-standard formatted screenplay preview, local and Google Drive file management, a quick-insert writing bar, scene outline navigation, title page metadata editing, and PDF screenplay export.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and UX choices were confirmed during discovery and shape the architecture below:

- **Confirmed Decision — Adaptive Editor & Preview Layout**: On compact phone screens, the workspace uses a snappy segmented toggle between **Editor**, **Preview**, and **Split** modes; on wider tablet/landscape screens, it automatically displays a side-by-side live **Split-Pane** view so writers see formatted screenplay output as they type.
- **Confirmed Decision — Local & Google Drive File Management**: Uses a dual-layer persistence strategy: instant automatic local saving in an on-device **Script Library** (Room database) paired with the **Android Storage Access Framework (SAF)** (`OpenDocument` / `CreateDocument`) so users can open and save `.fountain` and `.txt` files directly to **Google Drive** or device folders without complex OAuth barriers.
- **Confirmed Decision — Screenplay Writing Conveniences**: Includes a **Quick-Insert Formatting Bar** (`INT.`, `EXT.`, Character, Parenthetical, Transition, Centered), a **Scene Outline Drawer** with live word and estimated page counts (1 page ≈ 55 formatted lines), a dedicated **Title Page Metadata Editor**, and native **Formatted Screenplay PDF Export** via Android's `PdfDocument` engine.

---

## 1. Overview & Core Concept

- **What It Does**: Lets screenwriters compose plain-text Fountain manuscripts with real-time color-coded syntax highlighting while simultaneously previewing and exporting their script in standard US Letter screenplay layout (12pt Courier Prime, indented Character cues, Dialogue blocks, Parentheticals, Scene Headings, and right-aligned Transitions).
- **Target Audience / Persona**: Screenwriters, filmmakers, playwrights, and storytellers who want a tactile, zero-friction writing environment on phones or tablets that syncs seamlessly with `.fountain` files in Google Drive.
- **Key Value**: Eliminates manual indentation and formatting overhead on mobile devices while preserving 100% plain-text Fountain portability and producing industry-formatted PDFs on the go.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Focused Writing & Syntax Highlighting**:
   - Launches directly into the active screenplay (seeded with a rich sample short film script in Fountain format on first run so the user can immediately explore syntax highlighting and live preview).
   - As the writer types, a custom `VisualTransformation` highlights Scene Headings (`INT. COFFEE SHOP - NIGHT`), Character cues, Parentheticals, Transitions (`CUT TO:`), Centered text, Notes (`[[...]]`), Boneyard comments, and Title Page keys in distinct, high-contrast semantic hues.
   - A floating bottom **Quick-Insert Bar** above the keyboard offers one-tap insertion of `INT. `, `EXT. `, ` - DAY`, ` - NIGHT`, `CHARACTER`, `(parenthetical)`, `CUT TO:`, and Title Page blocks.
2. **Real-Time Formatted Preview**:
   - Switching to the **Preview** tab (or viewing the right pane on tablets/split view) renders the parsed Fountain AST into a true screenplay page layout: capitalized underlined/bold scene slugs, centered character names with proper left-margin offsets, narrow dialogue columns, italicized parentheticals, right-aligned transitions, and a formatted cover page when title metadata is present.
3. **Scene Outline & Script Statistics**:
   - Opening the **Scene Outline Drawer** lists every parsed scene (`1. INT. DINER - NIGHT`, `2. EXT. ALLEY - CONTINUOUS`) along with live **Page Count**, **Scene Count**, **Word Count**, and **Character Roster** chips. Tapping any scene scrolls/jumps the editor cursor directly to that scene.
4. **Title Page & File Management (Local + Google Drive + PDF)**:
   - Tapping the **Title Page** action opens a structured sheet for `Title`, `Credit`, `Author`, `Source`, `Draft date`, and `Contact`, syncing bidirectionally with the Fountain header block.
   - Tapping **Save / Export** lets the user:
     - Save changes to the local Script Library (also autosaved continuously).
     - **Save `.fountain` to Google Drive / Device** via the system document creator (`CreateDocument`).
     - **Open `.fountain` from Google Drive / Device** via the system document picker (`OpenDocument`), with persistent URI write permissions when reopening linked cloud files.
     - **Export Formatted PDF** using Android's native `PdfDocument` renderer with authentic screenplay margins and pagination.

### Visual Identity & Theme

- **Aesthetic Direction**: *Noir Typewriter Studio* — tactile, editorial, distraction-free studio aesthetic inspired by classic literary manuscripts and darkroom film editing suites.
- **Color Palette & Mood**:
  - **Focused Dark Mode (Default for Writing Sessions)**:
    - Background (`surface` / `--background`): Deep Obsidian (`#111315`)
    - Elevated Paper Surface (`surfaceContainer`): Warm Charcoal Slate (`#1A1D21`)
    - Primary Accent (`primary`): Amber Gold (`#E5A93C`) — evokes warm desk-lamp light and brass typewriter accents
    - Secondary Accent (`secondary`): Cinema Cyan (`#68B0AB`) — highlights scene headings and structural markers
    - Text Primary (`onSurface`): Warm Parchment (`#ECE8DF`) for zero eye strain in dark rooms
  - **Daylight Manuscript Mode (Light Theme)**:
    - Background (`surface`): Warm Cream Paper (`#FAF6EE`)
    - Elevated Surface (`surfaceContainer`): Aged Ivory (`#F0EAE1`)
    - Primary Accent (`primary`): Deep Terracotta Ink (`#9E472A`)
    - Secondary Accent (`secondary`): Slate Teal (`#2B5B6C`)
    - Text Primary (`onSurface`): Carbon Ink (`#1C1B19`)
- **Typography & Hierarchy**:
  - **Screenplay & Editor Font**: **Courier Prime** (bundled locally via `font-util`) for authentic 12pt monospace pitch in both the syntax-highlighted editor and the formatted screenplay preview.
  - **UI Chrome & Outline Font**: **Space Grotesk** / **Plus Jakarta Sans** (bundled locally via `font-util`) for crisp top bars, statistics badges, and drawer navigation.
- **Component Styling & Layout**:
  - Full-bleed edge-to-edge layout (`enableEdgeToEdge()`) with proper status bar and navigation/IME insets so the Quick-Insert Bar sits cleanly above the on-screen keyboard.
  - Subtle paper-margin guide lines in the Preview pane and line/page indicator pills in the header.
- **Interactive Feedback & Motion**:
  - Smooth spring transitions when switching between Editor, Split, and Preview modes.
  - Live pulsing "Saved locally" / "Synced to Drive file" status pill in the top bar.
  - Snackbar confirmations with quick actions after exporting `.fountain` or `.pdf` files to Google Drive.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom Fountain Lexer & AST Parser + `VisualTransformation`**
  - *Chosen Approach*: Build a dedicated, deterministic Kotlin Fountain parser that tokenizes lines into `TitleMetadata`, `SceneHeading`, `Action`, `Character`, `Dialogue`, `Parenthetical`, `Transition`, `Centered`, `Lyrics`, `Synopsis`, `Section`, and `Note` elements, driving both Compose `VisualTransformation` syntax highlighting and the Preview/PDF layout engine.
  - *Why*: `VisualTransformation` highlights Fountain syntax in real time without mutating cursor selection or breaking IME composition/autocorrect on Android keyboards.
  - *Alternatives Considered*: WebView-based JavaScript editors (rejected due to sluggish keyboard handling, poor native accessibility, and flickering in dark mode).
- **Decision 2: Storage Access Framework (SAF) + Room Local Library**
  - *Chosen Approach*: Combine a local Room database (for zero-latency autosave, version safety, and managing multiple drafts) with Android's `ActivityResultContracts.OpenDocument` and `CreateDocument` contracts for Google Drive and local storage `.fountain` / `.pdf` I/O.
  - *Why*: SAF integrates natively with the Google Drive app installed on Android devices without requiring restricted Drive OAuth scopes or manual cloud project setup, while Room guarantees no work is ever lost mid-session.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         MainActivity (Edge-to-Edge)                       │
│                    FountainStudioTheme (Dark / Light Mode)                │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         FountainEditorScreen (Scaffold)                   │
│  ┌──────────────────────┐ ┌───────────────────────┐ ┌──────────────────┐  │
│  │ Top Studio Bar       │ │ Adaptive Workspace    │ │ Quick-Insert Bar │  │
│  │ • Script Title       │ │ • Editor Pane         │ │ • INT./EXT.      │  │
│  │ • Mode Toggle        │ │   (Syntax Highlight)  │ │ • Character      │  │
│  │ • Dark Mode Switch   │ │ • Live Preview Pane   │ │ • Parenthetical  │  │
│  │ • Drive / PDF Menu   │ │   (Screenplay Layout) │ │ • CUT TO: / Note │  │
│  └──────────────────────┘ └───────────────────────┘ └──────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ Modal Navigation Drawer & Dialogs                                   │  │
│  │ • Scene Outline & Stats  • Local Script Library  • Title Page Modal │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                        FountainViewModel (StateFlow)                      │
│  • Active Script Content & Cursor Selection State                         │
│  • Real-Time Parsed FountainDocument (Title Metadata, Elements, Scenes)   │
│  • Debounced Room Autosave & Linked Document URI Tracking                 │
└───────────┬─────────────────────────┬─────────────────────────┬───────────┘
            │                         │                         │
            ▼                         ▼                         ▼
┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────────┐
│ FountainParser &      │ │ ScriptRepository      │ │ DocumentExportManager │
│ SyntaxHighlighter     │ │ (Room SQLite DAO)     │ │ (SAF & PdfDocument)   │
│ • Line AST Tokenizer  │ │ • Local Script Drafts │ │ • Google Drive / SAF  │
│ • VisualTransformation│ │ • Dark Mode & Prefs   │ │   Read & Write (.fountain)
│ • Page Count Estimator│ │ • Starter Screenplay  │ │ • Native PDF Renderer │
└───────────────────────┘ └───────────────────────┘ └───────────────────────┘
```

### Data Model & State

- **`ScriptEntity` (Room Table `scripts`)**:
  - `id: Long`, `title: String`, `content: String`, `linkedUri: String?` (persisted SAF/Google Drive URI if linked to an external file), `updatedAt: Long`, `isFavorite: Boolean`.
- **`FountainDocument` (Parsed Domain Model)**:
  - `titlePage: Map<String, String>` (`Title`, `Credit`, `Author`, `Source`, `Draft date`, `Contact`)
  - `elements: List<FountainElement>` (sealed hierarchy with line numbers, raw text, clean display text, and scene numbers)
  - `scenes: List<SceneOutlineItem>` (scene index, slugline, line offset, estimated page number)
  - `stats: ScriptStats` (word count, character count, scene count, estimated page count, speaking character list)
- **`EditorUiState`**:
  - Holds current `TextFieldValue`, `FountainDocument`, `ViewMode` (`EDITOR`, `PREVIEW`, `SPLIT`), `isDarkMode: Boolean`, `showLineNumbers: Boolean`, `saveStatus: SaveStatus`, and active dialogs (`showTitlePageDialog`, `showLibrarySheet`, `showStatsSheet`).

### Interactive Component & State Mapping

- **Syntax-Highlighted Editor (`FountainEditorPane`)**: Binds to `TextFieldValue` in `FountainViewModel`. Every edit immediately updates the text state, triggers synchronous AST parsing for the live preview and outline, and schedules a 600ms debounced autosave to Room.
- **Quick-Insert Formatting Bar (`QuickInsertBar`)**: Reads the current cursor selection in `TextFieldValue`, inserts or wraps the target line with the selected Fountain token (e.g., prefixing `INT. `, uppercasing a `CHARACTER` cue, or inserting `(beat)`), and advances the cursor automatically.
- **Scene Outline & Library Drawer (`StudioDrawer`)**: Displays two tabs—**Scenes** (tapping a scene updates `TextFieldValue.selection` and scrolls the editor/preview to that line) and **Scripts** (create new script, switch between local drafts, rename, or delete).
- **Title Page Editor Dialog (`TitlePageDialog`)**: Parses existing top-of-file key-value metadata into structured fields and rewrites the Fountain header block cleanly on save.
- **Google Drive / Local File & PDF Actions**:
  - *Open File*: Launches `OpenDocument`, reads the `.fountain`/`.txt` stream, takes persistable URI permission when supported, and imports it into the active workspace.
  - *Save to Drive / Device*: If the script already has a writable `linkedUri`, writes directly or offers "Save As..." via `CreateDocument("text/plain")` with a `.fountain` filename so the user can pick their Google Drive folder in the system picker.
  - *Export PDF*: Launches `CreateDocument("application/pdf")` and renders the parsed `FountainDocument` onto paginated US Letter canvases (`612 x 792 pt`) using standard screenplay indentation rules.
