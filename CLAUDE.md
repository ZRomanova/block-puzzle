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
- Last known-good state (2026-08-11, after the level constructor rewrite +
  same-day follow-ups): `assembleDebug` and `testDebugUnitTest` both green.
  Latest APK installed to the test phone via `adb install -r`, pushed to
  `/sdcard/Download/BlockPuzzle.apk`, and copied to
  `C:\Users\roman\Desktop\BlockPuzzle.apk`. App launches without crashing
  (verified via `adb logcat`) and the user has visually confirmed the
  constructor works on-device — see Status below for exactly which changes
  that covers and which are still pending her re-confirmation.

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
  - `LevelDefinition.isUnlosable(shapes, boardSize)` (added 2026-08-11, board
    size added same day) — true in exactly two *provable* cases: (1) every
    shape has `cellCount <= 1` (e.g. only `PieceShape.SINGLE`) — a lone cell
    always fits somewhere until a line clears it, true for any board size;
    (2) every shape has `cellCount == 2` (domino) **and boardSize is even**
    — checkerboard-color argument: a domino always covers one black + one
    white cell, and a full line of *even* length always splits evenly by
    color, so the black-filled == white-filled invariant can never break,
    which rules out the classic "scattered isolated single-cell gaps"
    deadlock. On *odd* board sizes a full line splits unevenly, so a line
    clear *can* break that invariant — deliberately left unflagged there,
    no proof either way. Does **not** extend to 3+-cell shapes (a 3-cell
    placement already unbalances the color invariant by construction) or to
    mixed-size pools — see the doc comment before generalizing further,
    this is intentionally narrow rather than a general solvability prover.
    `LevelEditorScreen` disables Save and shows a matching inline message.
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
  - `DefaultLevels.kt` (`seedDefaultLevelsIfNeeded`, renamed 2026-08-11 from
    `LegacyMigration.kt`/`migrateLegacyIfNeeded`) — one-shot, gated by
    `LevelsRepository.defaultsSeeded`: seeds **3** curated example levels
    (not the original 8 Easy/Hard × Classic/ColorBonus × 8×8/6×6 combos —
    that many similarly-named defaults was confusing to pick from, see
    Notable product decisions). The old key-format score-copying logic from
    the pre-constructor `GameVariant` era was removed along with it — it
    only ever mattered for the one device that had gone through that
    original migration, and that device's local app data was cleared
    2026-08-11 when this change shipped (user's explicit choice, offered
    the alternative of a non-destructive migration and she picked the
    simple wipe). Kept in its own file, isolated from both repositories, so
    it's easy to find/replace again later.
  - (No `SettingsRepository` anymore — the monotone-color picker and Settings
    screen were removed 2026-08-10; see Notable product decisions.)
- **ui/** — Compose, MVVM. `GameViewModel` holds one `activeEngine` plus a
  `pausedEngines: Map<String /* level tag */, GameEngine>` so **any number of
  levels can each have their own paused/unfinished game simultaneously**
  (exposed as `resumableLevelTags`). Runs `seedDefaultLevelsIfNeeded` once
  from `init`. Screens: `Menu` (just 2 buttons: "Играть" → `LevelList`,
  "Конструктор" → `Constructor`, plus a "?" icon top-right → `Rules`),
  `Rules` (static plain-language walkthrough for a new player, see
  `RulesScreen.kt`), `LevelList` (pick a level, tap resumes if paused else
  starts fresh), `Constructor` (list + edit/delete + create), `LevelEditor`
  (the actual constructor form), `Game`, `GameOver` (`ui/navigation/Screen.kt`
  sealed interface, driven by `AnimatedContent` in `MainActivity.kt`). There
  is no more mode/scoring/board-size toggle on the menu and no standalone
  records card — all of that moved into `LevelEditorScreen` (per-level rule
  editing) and per-level rows/top bars. `MenuScreen` also shows a "Топ-5 по
  рекорду" card (only when at least one level has a nonzero record) with a
  tap-to-quick-play row per level, sharing the same resume-if-paused logic
  as `LevelListScreen` via a `startOrResume` lambda built once in
  `MainActivity`.
  - `LevelEditorScreen` is one scrollable screen, no wizard: name → board
    size (5/6/7/8, generalized `ToggleButton`/`LabeledToggleRow` from
    `ui/components/ToggleButton.kt`) → color mode → algorithm → shape list
    (weight steppers shown only for Случайный) → "Добавить фигуру" opens a
    tap-grid `Dialog` (`Trunc(boardSize*0.8)` square, `BoxWithConstraints`-sized
    to always span the dialog's full width — cell size scales to fit, not a
    fixed dp) with live `ShapeConnectivity`/`ShapeSymmetry` validation.
    Draft fields use `rememberSaveable` (shapes as a JSON string, since
    `LevelShape` is already `@Serializable`) so an in-progress draft
    survives rotation. **Shrinking the board size drops any already-added
    shape that no longer fits** the new `Trunc(boardSize*0.8)` bound
    (`changeBoardSize` filters `shapes` and shows an inline count of how
    many were removed) — added 2026-08-11 after the user found she could
    shrink the board out from under shapes that used to fit. Save is also
    disabled with an inline message when `LevelDefinition.isUnlosable(shapes, boardSize)`
    is true. Saving with any *rule* field changed (board size/color
    mode/algorithm/shapes/weights) resets that level's record via
    `GameViewModel.saveLevel`'s diff — renaming alone does not. **Unless the
    edited level currently has a nonzero record**, in which case pressing
    Save opens `RecordAtRiskDialog` (added 2026-08-11) offering a choice:
    overwrite in place (resets the record, old one-path behavior) or "save
    as a copy" (`saveLevel(..., saveAsCopy = true)` — always takes the
    fresh-tag path via `nextAvailableTag` regardless of `editingTag`, so the
    original level and its record are never touched; the copy's name gets
    " (копия)" appended automatically if the user didn't already rename it,
    so the two don't look identical in the list). A record of 0 skips the
    dialog entirely and saves in place exactly as before.
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
- **Only 3 default levels, not 8** (changed 2026-08-11): the original
  migration seeded all 8 old Easy/Hard × Classic/ColorBonus × 8×8/6×6
  combos as a safety net so nothing from the pre-constructor game was lost.
  The user found 8 similarly-named defaults "easy to get confused by" and
  asked for exactly 3, chosen to demonstrate breadth rather than
  completeness: a familiar 8×8 classic (`PieceShape.LEGACY_CATALOG`,
  Однотонный/Случайный), a 6×6 showing off Цветной scoring + the Хитрый
  algorithm together, and a 5×5 with a small hand-picked, non-uniformly
  weighted shape pool to demonstrate that the constructor lets you curate
  *which* shapes appear and how often — not just accept the full legacy
  set. See `DefaultLevels.kt`. Don't go back to seeding one level per
  mode/scoring/size combination without asking again.
- **A level's shape pool can't be all-single-cell, or (on an even board)
  all-domino** (added 2026-08-11, `LevelDefinition.isUnlosable`, extended
  same day): started from the user's own example ("only a block of 1
  cell") — provably always placeable, any board size. She then asked
  whether an all-domino pool on 8×8 had the same problem and asked for the
  conditions to be *calculated* per board size rather than guessed. The
  domino case turned out to only be provable for **even** board sizes (a
  checkerboard-coloring argument — see the domain doc comment); odd sizes
  (5, 7) are deliberately left unflagged since there's no proof either way
  there, not because they're known-safe. This does not generalize to any
  shape with 3+ cells — that's a genuinely harder, unproven question, and
  the rule stays a narrow "provably always safe" check, not a general
  solvability prover. Don't expand it further without doing the same kind
  of actual derivation (or asking) — a plausible-sounding formula that
  isn't actually proven is worse than no rule at all here, since a false
  positive would block a level that's actually fine.
- **Rules screen** (added 2026-08-11, `RulesScreen.kt`, reached via a "?"
  icon top-right of the main menu): the user wanted to share the app with a
  friend and needed something to point them at instead of walking them
  through the mechanics in person. Plain-language sections: goal, controls
  (drag, rotate, undo), what the level settings mean, the constructor, and
  per-level records/pausing.
- **Топ-5 leaderboard on the main menu** (added 2026-08-11): quick
  "beat my record" access — tapping a row starts or resumes that level
  exactly like a `LevelListScreen` row does (shared `startOrResume` lambda
  in `MainActivity`). Hidden entirely when no level has a nonzero record yet
  (a fresh install has nothing worth calling "top" until something's been
  played).
- **Editing a leveled-up level offers "save as copy" instead of only
  overwrite** (added 2026-08-11): previously any rule change on a level
  with an existing record silently reset it, full stop. The user wanted
  records to stop being casualties of experimentation — a record of 0
  still just saves in place (nothing to lose), but a nonzero record now
  triggers a choice (`RecordAtRiskDialog`): overwrite (old behavior,
  record resets) or save as a new, separate copy (original level and its
  record left completely untouched). Don't collapse this back down to
  silent-overwrite-only without asking.
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
data layer (`LevelsRepository`, re-keyed `RecordsRepository`,
`DefaultLevels.kt`), and full UI (gutted `MenuScreen`, new
`LevelListScreen`/`ConstructorScreen`/`LevelEditorScreen` with a tap-to-draw
shape dialog, plus `RulesScreen` and a Топ-5 card on the menu, added later
the same session). The user then play-tested for real and reported two
follow-ups, both implemented and shipped this session: shrinking a level's
board size now drops shapes that no longer fit (with an inline count), and
the shape-drawing dialog now spans the full dialog width instead of a fixed
32dp cell size. Then: default levels cut from 8 to 3 curated ones
(`DefaultLevels.kt` replaced `LegacyMigration.kt` — the old key-format
score-copying logic was dropped, it was only ever relevant to the
pre-constructor `GameVariant` era), and a new `LevelDefinition.isUnlosable`
check blocks saving a level whose shape pool is entirely single-cell shapes.

After that: `isUnlosable` extended from single-cell-only to also cover
domino-only pools on even board sizes (real derivation, not a guess — see
Notable product decisions), and the record-loss-on-edit problem was fixed
with the save-as-copy dialog described above.

`assembleDebug` and `testDebugUnitTest` both green throughout, including
`ShapeSymmetryTest`/`ShapeConnectivityTest` (chirality edge cases: S/Z, L/J
mirror pairs; the lone chiral PENTOMINO_L with no legacy mirror) and
`LevelDefinitionTest` for `isUnlosable` (now covering both the single-cell
and domino/even-board cases). APK installed on the test phone and pushed to
its Downloads folder after every round; launches without crashing per
`adb logcat` each time. The user visually confirmed the constructor works
("Проверила, пока всё хорошо") after the *first* round of constructor work;
none of the follow-up rounds since (board-size shape validation, full-width
drawing grid, 3 curated defaults, the unlosable checks, or the save-as-copy
dialog) have been visually re-confirmed by her yet as of this write-up —
worth an actual walkthrough before assuming the UI/UX details are all
correct, only the domain logic underneath has real test coverage.

**The test phone's app data was cleared 2026-08-11** (`adb shell pm clear
com.blockpuzzle.rotate`, the user's explicit choice when asked, over writing
a non-destructive migration) so the new 3-default-level set would actually
appear — any records/paused games that existed on that specific device
before this point are gone. This was scoped to that one physical device,
nothing about the app's normal behavior for other installs.

Google Play publishing was explicitly **paused** by the user 2026-08-11 (she
said she changed her mind about it "for now") in favor of this feature —
don't steer conversations back toward it unprompted. If it resurfaces, note
that essentially no publishing prep exists yet (no release signing config,
no store listing, no privacy policy) — see the Play-publishing prompt from
that earlier conversation if one exists, don't re-derive the checklist from
scratch, but don't assume progress was made either.
