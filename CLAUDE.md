# Block Puzzle 8x8 (rotate) — project notes

Native Android block-puzzle game. Kotlin + Jetpack Compose, MVVM, fully offline
(no INTERNET permission). Unique mechanic: pieces can be freely rotated before
placing. Package: `com.blockpuzzle.rotate`.

This file exists so a future Claude Code session (possibly after this folder
is moved elsewhere) has full context without re-deriving it. Read this before
making changes.

## Build / run

- No Android Studio required — SDK was installed manually via `sdkmanager`
  into `%LOCALAPPDATA%\Android\Sdk` (`local.properties` → `sdk.dir`).
- Build debug APK: `./gradlew.bat assembleDebug`
- Run unit tests: `./gradlew.bat testDebugUnitTest`
- `adb` lives at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (not on
  PATH by default in a fresh shell — add it, e.g.
  `export PATH="/c/Users/<user>/AppData/Local/Android/Sdk/platform-tools:$PATH"`
  in Git Bash).
- Installing to a phone over USB: `adb install -r app/build/outputs/apk/debug/app-debug.apk`,
  or push to Downloads with `adb push app-debug.apk /sdcard/Download/BlockPuzzle.apk`.
  **Gotcha:** Git Bash/MSYS auto-converts `/sdcard/...`-style destination paths
  into Windows paths, breaking `adb push`. Prefix the command with
  `MSYS_NO_PATHCONV=1` to stop that.
- Last known-good state (2026-08-11, after the level constructor rewrite):
  `assembleDebug` and `testDebugUnitTest` both green. Latest APK installed to
  the test phone via `adb install -r` and copied to
  `C:\Users\roman\Desktop\BlockPuzzle.apk`. App launches without crashing
  (verified via `adb logcat`), but the phone's lockscreen (PIN-protected)
  meant the constructor UI itself wasn't visually walked through by the
  agent that session — worth an actual play-test before trusting the UI
  polish, only the domain logic has real test coverage.

## Architecture

- **domain/** — pure Kotlin, zero Android deps, unit-tested with JUnit4.
  Immutable models (`Board`, `GameState`, `Piece`); every move produces a new
  `GameState`, so undo is just a stack of prior states (`GameEngine.history`).
  - `LevelDefinition(tag, name, boardSize: Int, colorMode: ScoringMode, algorithm: GameMode, shapes: List<LevelShape>)`
    is a full game variant — **user-created via the level constructor**, not
    a fixed enum combination. `tag` is the persisted-record key and is
    assigned once at creation (`LevelDefinition.nextAvailableTag`), never
    regenerated on edit. `boardSize` is a free `Int` in `ALLOWED_BOARD_SIZES`
    (5..8) — there is no more `BoardSize` enum. This replaced the old fixed
    `GameVariant` (8 combinations, deleted) — see Notable product decisions.
  - `PieceShape(id, baseCells)` replaced the old closed `ShapeType` enum —
    it's an open value type now so the constructor's hand-drawn shapes and
    the 15 built-in ones (`PieceShape.LEGACY_CATALOG`) are the same type.
  - `LevelShape(shape: PieceShape, weight: Int, includeMirror: Boolean)` is
    one pool entry inside a level. `weight` only matters for the Случайный
    (EASY) algorithm. `includeMirror` is set **once, at add-time**
    (`LevelShape.userDrawn` computes it via `ShapeSymmetry.isChiral`) —
    it is deliberately *not* re-derived from geometry at spawn time, because
    the legacy catalog has both explicit mirror pairs (TETROMINO_L/J, S/Z)
    and at least one intentionally mirror-less chiral shape (PENTOMINO_L);
    auto-deriving mirroring would silently change the legacy piece
    distribution. See `LevelShape.kt`'s doc comment before touching this.
  - `ShapeSymmetry` (canonicalKey/isChiral/rotate90/mirror — the dihedral-8
    transform group) and `ShapeConnectivity` (8-directional BFS) back the
    level editor's shape-drawing validation: a shape and its rotation/mirror
    can't be added twice, and all cells must be connected diagonally-or-not.
  - Piece generation is behind `PieceGenerator`, now over `List<LevelShape>`
    instead of a fixed shape enum: `EasyPieceGenerator` (weighted random —
    `pickWeighted`) vs `HardModePieceSelector` (lookahead — samples
    candidates, scores by resulting playability, picks the hardest option
    that still leaves a minimum margin so the board stays technically
    solvable; ignores weight, same as before). Both resolve a chosen
    `LevelShape` to concrete cells via `LevelShape.resolveCells` (coin-flips
    the mirror when `includeMirror` is set).
  - `GameEngine.colorProvider: () -> PieceColor` decides piece color
    independently of shape selection — CLASSIC ("Однотонный") is hardcoded to
    `PieceColor.BLUE` (fixed, not user-configurable — see below), COLOR_BONUS
    ("Цветной") uses random. GameEngine always overwrites whatever color the
    generator provisionally assigned.
  - **Tray refill is batched, not one-for-one**: placing a piece just empties
    its slot; a fresh trio of 3 only appears once all three slots are empty.
    This is what makes the game losable (a 1-for-1 refill always bails you
    out with a fresh roll). See `GameEngine.place()`.
  - `PieceColor` enum currently has **6** colors (RED, ORANGE, YELLOW, GREEN,
    BLUE, PURPLE) — deliberately cut down from an original 12 because too
    many close hues made monochrome-line color-bonus play impractical.
- **data/** — DataStore Preferences repos, no network:
  - `LevelsRepository` — the full level list (built-in-seeded + custom) as
    one JSON blob (`kotlinx.serialization`) in a `levels` DataStore. `save`/
    `delete` decode-mutate-encode inside `edit {}` (transactional).
  - `RecordsRepository` — one high score per level **tag** (plain `String`
    now, not `GameVariant`).
  - `LegacyMigration.kt` — one-shot, gated by `LevelsRepository.defaultsSeeded`:
    seeds the 8 old Easy/Hard × Classic/ColorBonus × 8×8/6×6 combinations as
    regular (fully editable/deletable) levels using the full legacy shape
    catalog, and copies each old high score (read via the old
    `"${mode.name}_${scoring.name}_${boardSize.name}"` key format) onto its
    new tag so nothing already earned was lost in the migration. Kept in its
    own file, isolated from both repositories, so it's easy to find/delete
    later.
  - (No `SettingsRepository` anymore — the monotone-color picker and Settings
    screen were removed 2026-08-10; see Notable product decisions.)
- **ui/** — Compose, MVVM. `GameViewModel` holds one `activeEngine` plus a
  `pausedEngines: Map<String /* level tag */, GameEngine>` so **any number of
  levels can each have their own paused/unfinished game simultaneously**
  (exposed as `resumableLevelTags`). Runs `migrateLegacyIfNeeded` once from
  `init`. Screens: `Menu` (just 2 buttons: "Играть" → `LevelList`,
  "Конструктор" → `Constructor`), `LevelList` (pick a level, tap resumes if
  paused else starts fresh), `Constructor` (list + edit/delete + create),
  `LevelEditor` (the actual constructor form), `Game`, `GameOver`
  (`ui/navigation/Screen.kt` sealed interface, driven by `AnimatedContent` in
  `MainActivity.kt`). There is no more mode/scoring/board-size toggle on the
  menu and no standalone records card — all of that moved into
  `LevelEditorScreen` (per-level rule editing) and per-level rows/top bars.
  - `LevelEditorScreen` is one scrollable screen, no wizard: name → board
    size (5/6/7/8, generalized `ToggleButton`/`LabeledToggleRow` from
    `ui/components/ToggleButton.kt`) → color mode → algorithm → shape list
    (weight steppers shown only for Случайный) → "Добавить фигуру" opens a
    tap-grid `Dialog` (`Trunc(boardSize*0.8)` square) with live
    `ShapeConnectivity`/`ShapeSymmetry` validation. Draft fields use
    `rememberSaveable` (shapes as a JSON string, since `LevelShape` is
    already `@Serializable`) so an in-progress draft survives rotation.
    Saving with any *rule* field changed (board size/color mode/algorithm/
    shapes/weights) resets that level's record via
    `GameViewModel.saveLevel`'s diff — renaming alone does not.
  - `GameScreen`'s top bar shows **score and record side by side** (labeled
    "счёт" / "рекорд"), passed in as `record: Int` from
    `MainActivity`'s `records[state.level.tag]`. The displayed record is
    `maxOf(record, score)`, so once the live score overtakes the stored
    record both numbers animate upward together — the record readout isn't
    just the persisted value. Actual persistence still only happens at game
    end via `RecordsRepository.submitScore` in `GameViewModel.finishGame`.
  - Drag-and-drop is hand-rolled via `pointerInput` + `detectDragGestures` in
    `TraySlot.kt`, tracking root coordinates via `positionInRoot()`. Drag
    callbacks are wrapped in `rememberUpdatedState` — required because
    `pointerInput(piece?.id)` does NOT restart on rotation (id is stable
    across rotation), so without this the gesture coroutine would use stale
    pre-rotation closures.
  - The floating drag "ghost" and the placement-highlight/anchor computation
    share one `ghostTopLeftInRoot()` helper in `GameScreen.kt`, so the piece's
    bottom edge always sits a fixed small gap above the finger regardless of
    piece height/orientation (both computations must agree exactly).

## Notable product decisions (why, not just what)

- No INTERNET permission anywhere — offline-only was a hard requirement from
  the original spec.
- Scoring constants were deliberately reduced ~10x from the first pass
  (`ScoringConfig`) because raw numbers felt too large/arbitrary to the user.
- Color-bonus scoring rewards monochrome line clears
  (`ScoringConfig.colorBonusForLine` / `colorBonusForMove`), giving the piece
  colors actual gameplay meaning instead of being purely cosmetic.
- Independent high scores × independent paused games per level are still true
  and load-bearing — don't collapse them back down without asking. The old
  fixed *8 variants* themselves were superseded 2026-08-11 by the level
  constructor (below): "8 variants" is now just how many levels happen to be
  seeded on first launch, not a hardcoded ceiling.
- **Level constructor** (added 2026-08-11, replacing the fixed 8-variant
  system entirely — this was an explicit, deliberated choice, not a default):
  the user wanted to "feel like a creator." A level = board size (any square
  5–8, not just 6/8) × color mode (Однотонный/Цветной) × algorithm
  (Случайный/Хитрый — renamed from Простой/Сложный, same underlying
  `GameMode.EASY`/`HARD`, just reframed as "which piece-selection algorithm"
  rather than "difficulty") × a hand-drawn shape pool with weights. Key
  decisions locked in via user Q&A, don't re-litigate without asking again:
  - Unify into one system rather than keep the old 8 variants as a separate,
    protected set alongside custom levels.
  - Editing a level's *rules* resets its record; renaming does not.
  - Shape probabilities are plain positive-integer **weights**, not
    percentages — the user explicitly rejected "you type percents and the
    last one is auto-computed" as more mental math than typing a weight.
  - Main menu is exactly 2 buttons ("Играть" / "Конструктор"); no
    mode/scoring/board-size toggles or a standalone records card anywhere on
    the menu — everything moved into level-scoped screens.
  - A shape and its mirror image always count as one shape (shared,
    50/50-split weight at spawn time) — validation blocks adding a
    rotation/reflection duplicate outright.
- The monotone-color setting (Settings screen, `SettingsRepository`) was
  **removed** 2026-08-10 at the user's request — she found blue (the
  original default) the most readable/contrasty option and didn't want the
  choice at all. CLASSIC-scoring pieces are now hardcoded blue. Don't
  reintroduce a color picker without asking.
- Score + record shown together during play (top bar, side by side) was
  added 2026-08-10 so the user can gauge how close she is to her best.
  Placement was chosen from several ASCII-mockup options presented to her
  (stacked / side-by-side / progress bar / flanking cards) — she picked
  side-by-side. If asked to change this layout again, it's worth offering
  options the same way rather than just picking one.

## Status as of last session (2026-08-11)

Level constructor implemented per the plan agreed with the user: domain model
(`LevelDefinition`/`LevelShape`/`PieceShape`/`ShapeSymmetry`/`ShapeConnectivity`),
data layer (`LevelsRepository`, re-keyed `RecordsRepository`, `LegacyMigration`),
and full UI (gutted `MenuScreen`, new `LevelListScreen`/`ConstructorScreen`/
`LevelEditorScreen` with a tap-to-draw shape dialog). `assembleDebug` and
`testDebugUnitTest` both green, including new `ShapeSymmetryTest`/
`ShapeConnectivityTest` covering the chirality edge cases (S/Z, L/J mirror
pairs; the lone chiral PENTOMINO_L with no legacy mirror). APK installed on
the test phone and launches without crashing per `adb logcat`, but the phone
was locked (PIN) so the constructor UI itself has **not been visually
play-tested yet** — do that first if continuing here, especially the
shape-drawing dialog and weight steppers, before treating the UI as done.

Google Play publishing was explicitly **paused** by the user 2026-08-11 (she
said she changed her mind about it "for now") in favor of this feature —
don't steer conversations back toward it unprompted. If it resurfaces, note
that essentially no publishing prep exists yet (no release signing config,
no store listing, no privacy policy) — see the Play-publishing prompt from
that earlier conversation if one exists, don't re-derive the checklist from
scratch, but don't assume progress was made either.
