# Block Puzzle 8x8 (rotate) — project notes

Native Android block-puzzle game. Kotlin + Jetpack Compose, MVVM, fully offline
(no INTERNET permission). The hook is the **level constructor**: board size,
color mode, algorithm, rotation, mirroring, and a hand-drawn weighted shape
pool are all per-level configurable — not one fixed game mode. (Rotation and
mirroring — pieces can be freely rotated, and chiral shapes may spawn
mirrored, before placing — were the original headline mechanic before the
constructor existed and are still on by default, but as of 2026-08-11 they're
just two settings among many, not *the* distinguishing feature; see Notable
product decisions before re-emphasizing rotation specifically anywhere in
copy.) Package: `com.blockpuzzle.rotate`.

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
  several same-day follow-up rounds): `assembleDebug` and `testDebugUnitTest`
  both green. Latest APK installed via `adb install -r`, pushed to
  `/sdcard/Download/BlockPuzzle.apk`, and copied to the desktop — all from
  the current round. See Status below for exactly which changes have vs.
  haven't been visually re-confirmed by the user on-device (most haven't).

## Architecture

- **domain/** — pure Kotlin, zero Android deps, unit-tested with JUnit4.
  Immutable models (`Board`, `GameState`, `Piece`); every move produces a new
  `GameState`, so undo is just a stack of prior states (`GameEngine.history`).
  - `LevelDefinition(tag, name, boardSize: Int, colorMode: ScoringMode, algorithm: GameMode, shapes: List<LevelShape>, allowRotation: Boolean = true, allowMirror: Boolean = true)`
    is a full game variant — **user-created via the level constructor**, not
    a fixed enum combination. `tag` is the persisted-record key and is
    assigned once at creation (`LevelDefinition.nextAvailableTag`), never
    regenerated on edit. `boardSize` is a free `Int` in `ALLOWED_BOARD_SIZES`
    (5..8) — there is no more `BoardSize` enum. This replaced the old fixed
    `GameVariant` (8 combinations, deleted) — see Notable product decisions.
    `allowRotation`/`allowMirror` (added 2026-08-11) default `true` so old
    persisted levels decode with the behavior they always had.
  - `PieceShape(id, baseCells)` replaced the old closed `ShapeType` enum —
    it's an open value type now so the constructor's hand-drawn shapes and
    the 15 built-in ones (`PieceShape.LEGACY_CATALOG`) are the same type.
  - `LevelShape(shape: PieceShape, weight: Int, includeMirror: Boolean)` is
    one pool entry inside a level. `weight` only matters for the Случайный
    (EASY) algorithm. `includeMirror` is a **geometric fact** about the
    shape — does it have a mirror image distinct from any of its own
    rotations — decided once, at add-time (`LevelShape.userDrawn` computes
    it via `ShapeSymmetry.isChiral`, always with the full rotation-aware
    definition, never re-derived later), because the legacy catalog has
    both explicit mirror pairs (TETROMINO_L/J, S/Z) and at least one
    intentionally mirror-less chiral shape (PENTOMINO_L); auto-deriving
    mirroring would silently change the legacy piece distribution. See
    `LevelShape.kt`'s doc comment before touching this. Whether that fact
    actually results in a mirrored spawn is a **separate, live** question,
    answered fresh every time a piece is generated: `LevelShape.resolveCells(random, allowMirror)`
    (added 2026-08-11) only mirrors when `includeMirror` is true **and**
    the *level's current* `allowMirror` is true — so flipping a level's
    mirror toggle takes effect immediately for every shape in it, including
    ones added before the flip, with nothing to retroactively recompute or
    re-store.
  - `LevelDefinition.isUnlosable(shapes, boardSize, allowRotation)` (added
    2026-08-11, `allowRotation` param added same day when rotation became
    configurable) — true in exactly two *provable* cases: (1) every shape
    has `cellCount <= 1` (e.g. only `PieceShape.SINGLE`) — a lone cell
    always fits somewhere until a line clears it, true for any board size
    *and regardless of the rotation setting* (a point has no orientation);
    (2) every shape has `cellCount == 2` (domino), `boardSize` is even,
    **and `allowRotation` is true** — checkerboard-color argument: any
    2-cell orthogonally-adjacent placement always covers one black + one
    white cell, and a full line of *even* length always splits evenly by
    color, so the black-filled == white-filled invariant can never break,
    ruling out the classic "scattered isolated single-cell gaps" deadlock.
    **Why `allowRotation` is required for the domino case** (this was a real
    correctness bug caught while generalizing for the rotation toggle, not
    obvious up front): a domino's `baseCells` fix *one* orientation, and
    mirroring a straight 2-cell piece doesn't change it (its mirror image is
    itself) — without rotation, every spawned domino is stuck in that one
    fixed orientation, so a board left with only *cross*-oriented adjacent
    gaps blocks it completely even with plenty of empty cells remaining; the
    color-balance argument doesn't rescue that case. On *odd* board sizes a
    full line splits unevenly, so a line clear *can* break the color
    invariant regardless of rotation — deliberately left unflagged there, no
    proof either way. Does **not** extend to 3+-cell shapes (a 3-cell
    placement already unbalances the color invariant by construction) or to
    mixed-size pools — see the doc comment before generalizing further,
    this is intentionally narrow rather than a general solvability prover.
    `LevelEditorScreen` disables Save and shows a matching inline message.
  - `ShapeSymmetry` (canonicalKey/isChiral/rotate90/mirror — the dihedral-8
    transform group) and `ShapeConnectivity` (8-directional BFS) back the
    level editor's shape-drawing validation: a shape and its rotation/mirror
    can't be added twice, and all cells must be connected diagonally-or-not.
    **`canonicalKey`/`isChiral` always use the full 8-transform definition,
    regardless of a level's `allowRotation`/`allowMirror`** — briefly (same
    day) parameterized by those two settings so duplicate-detection matched
    exactly what a given level's player could reach, but the user asked to
    revert that for simplicity: in the constructor, a shape and any of its
    rotations/reflections are *always* the same shape, full stop, no
    per-level exceptions. `allowRotation`/`allowMirror` instead govern
    spawn-time behavior only (`LevelShape.resolveCells`, `initialRotationSteps`
    below) — not what counts as a duplicate while drawing.
  - Piece generation is behind `PieceGenerator`, now over `List<LevelShape>`
    instead of a fixed shape enum: `EasyPieceGenerator` (weighted random —
    `pickWeighted`) vs `HardModePieceSelector` (lookahead — samples
    candidates, scores by resulting playability, picks the hardest option
    that still leaves a minimum margin so the board stays technically
    solvable; ignores weight, same as before). Both take `allowRotation`/
    `allowMirror` constructor params (from the active `LevelDefinition`) and
    resolve a chosen `LevelShape` to concrete cells via
    `LevelShape.resolveCells(random, allowMirror)`. **`PieceGenerator.kt`'s
    top-level `initialRotationSteps(allowRotation, random)`** (added
    2026-08-11) decides the `rotationSteps` a freshly spawned piece starts
    with: `0` when rotation is allowed (the player fixes orientation
    themselves via the rotate button, so the spawn value is irrelevant), or
    a uniform random pick from `0..3` when it's disallowed — since the
    spawned orientation is then final for that piece's whole life. Picking
    uniformly over the 4 *raw* steps (rather than deduplicating first) still
    yields a uniform distribution over the shape's *distinct* visual
    outcomes for free: [Piece.cells] normalizes the result, so any
    rotational symmetry naturally folds multiple raw steps onto the same
    outcome, evenly (e.g. a straight domino/triomino: steps 0 and 2 always
    coincide, so its 2 distinct orientations still land 50/50). Combined
    with the independent mirror coin-flip, a shape can end up with 1, 2, 4,
    or 8 distinct reachable spawn forms depending on its own symmetry and
    which of the two settings are on — this is the mechanism behind "на 2, 4
    или 8" the user asked for; `HardModePieceSelector` applies the exact
    same per-candidate randomization before evaluating each one, rather than
    restructuring its sampling step to enumerate every orientation as a
    separate candidate ("theoretically every variant should be able to come
    up; the practical implementation is up to you" — this was judged the
    simplest one that satisfies that).
  - `GameEngine.colorProvider: () -> PieceColor` decides piece color
    independently of shape selection — CLASSIC ("Однотонный") is hardcoded to
    `PieceColor.BLUE` (fixed, not user-configurable — see below), COLOR_BONUS
    ("Цветной") uses random. GameEngine always overwrites whatever color the
    generator provisionally assigned.
  - **Tray refill is batched, not one-for-one**: placing a piece just empties
    its slot; a fresh trio of 3 only appears once all three slots are empty.
    This is what makes the game losable (a 1-for-1 refill always bails you
    out with a fresh roll). See `GameEngine.place()`.
  - `GameEngine.rotate(trayIndex)` (guard added 2026-08-11) is a no-op when
    `level.allowRotation` is false — the domain layer is the single source
    of truth for this rather than relying only on the UI hiding the rotate
    button (`TraySlot`'s `showRotateButton` param, wired from
    `uiState.level.allowRotation` in `GameScreen`).
  - `GameEngine.flip(trayIndex)` (added 2026-08-11, same day as the button
    below) is the player-facing counterpart to spawn-time mirroring — a
    manual "flip this piece left-right" control, guarded by
    `level.allowMirror` the same way `rotate` is guarded by `allowRotation`.
    `Piece.flippedHorizontally()` mirrors whatever the piece *currently*
    looks like on screen (`ShapeSymmetry.mirror(cells)`, not
    `shape.baseCells`) and resets `rotationSteps` to 0 against that as the
    new base — deliberately sidesteps reasoning about how mirroring and the
    existing rotation compose (they don't commute in general); this way
    "flip" always does exactly what it looks like it does regardless of
    prior rotates, and pressing it twice is always a no-op overall.
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
    `ui/components/ToggleButton.kt`) → color mode → algorithm → **вращение**
    → **отражение** (both added 2026-08-11, same `LabeledToggleRow<Boolean>`
    pattern, "Включено"/"Выключено") → shape list (weight steppers shown
    only for Случайный) → "Добавить фигуру" opens a
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
    disabled with an inline message when
    `LevelDefinition.isUnlosable(shapes, boardSize, allowRotation)` is true —
    the message text itself distinguishes the single-cell-only case from the
    domino/even-board case. The shape-drawing dialog's duplicate-shape error
    message is fixed regardless of the level's toggles ("с учётом поворота и
    отражения" — see the `ShapeSymmetry` bullet above for why this doesn't
    vary per level). Saving with any *rule* field changed (board size/color
    mode/algorithm/shapes/weights/**вращение/отражение**) resets that
    level's record via `GameViewModel.saveLevel`'s diff — renaming alone
    does not. **Unless the
    edited level currently has a nonzero record**, in which case pressing
    Save opens `RecordAtRiskDialog` (added 2026-08-11) offering a choice:
    overwrite in place (resets the record, old one-path behavior) or "save
    as a copy" (`saveLevel(..., saveAsCopy = true)` — always takes the
    fresh-tag path via `nextAvailableTag` regardless of `editingTag`, so the
    original level and its record are never touched; the copy's name gets
    " (копия)" appended automatically if the user didn't already rename it,
    so the two don't look identical in the list). A record of 0 skips the
    dialog entirely and saves in place exactly as before.
  - Each row in `LevelEditorScreen`'s shape list shows **two** glyphs side by
    side (`ShapeRow`'s `showMirroredPreview`, added 2026-08-11) when that
    shape would actually spawn mirrored in *this* level — i.e.
    `allowMirror && levelShape.includeMirror` — otherwise just the one
    drawn form, same as before. Added after the user noticed the default
    levels' shape lists showed what looked like duplicate, mirror-image
    rows (see `DefaultLevels.kt`/Notable product decisions) and asked for a
    clearer way to see "this row can come out either way" instead.
  - `LevelDefinition.rulesSummary()` (`ui/components/LevelLabels.kt`) builds
    the "Однотонный · Случайный · 6×6"-style one-liner shown on level rows,
    the game top bar, and game-over — it only appends "· без вращения" /
    "· без отражения" (added 2026-08-11) when a level actually deviates from
    the all-enabled default, to avoid cluttering every row with two more
    "· включено" badges nobody needs to see on the common case.
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
  completeness: "Классика 8×8" (`PieceShape.LEGACY_CATALOG`,
  Однотонный/Случайный, rotation+mirror both on — the original, still-default
  experience), "Цветной хитрец 6×6" with rotation turned **off** (Цветной/
  Хитрый — a meaningfully harder "place it exactly as dealt" variant), and
  "Мини-вызов 5×5" with a small hand-picked, non-uniformly weighted shape
  pool and mirror turned **off**. The level *names* briefly had
  "(без вращения)"/"(без отражения)" suffixes to spell this out, but the
  user asked for those back out the same day — "перегружает" (redundant
  clutter): `rulesSummary()` already surfaces the same information via
  "· без вращения"/"· без отражения" wherever the level is shown, so
  baking it into the name too was double-stating it. See `DefaultLevels.kt`.
  Don't go back to seeding one level per mode/scoring/size combination, and
  don't put rule-summary info back into level *names*, without asking again.
- **`classicShapePool()` collapses the legacy catalog's explicit mirror
  pairs before handing shapes to a default level** (added 2026-08-11): the
  15-shape `PieceShape.LEGACY_CATALOG` deliberately keeps TETROMINO_L/J and
  TETROMINO_S/Z as four separate entries for historical reasons (see that
  catalog's own doc comment) — but seeding "Классика 8×8" directly from it
  put both halves of each pair in the shape list as two separate,
  visually-mirror-image rows, which the user flagged as looking like a
  duplication bug rather than a deliberate legacy quirk. `classicShapePool()`
  (`DefaultLevels.kt`) dedupes by `ShapeSymmetry.canonicalKey` and keeps one
  `LevelShape.userDrawn` entry per pair (naturally `includeMirror = true`,
  so it still spawns as either half) — 13 entries instead of 15. This is a
  fresh-content decision, not a change to `PieceShape.LEGACY_CATALOG` itself
  or its historical rationale; don't conflate the two.
- **Rotation and mirror are now per-level configurable, not just fixed
  behavior** (added 2026-08-11): rotation was the game's headline mechanic
  since before the constructor even existed ("Unique mechanic: pieces can be
  freely rotated before placing"), and mirroring was always-on for chiral
  shapes since the constructor shipped. The user asked for both to become
  level-scoped toggles (`LevelDefinition.allowRotation`/`allowMirror`,
  default `true`/`true`), explicitly asking that every combination of the
  two be reasoned through rather than guessed at. That reasoning surfaced a
  real correctness issue in the just-added domino-`isUnlosable` rule (see
  above) — it turned out to silently assume rotation was always available —
  which is exactly the kind of bug this sort of request is meant to catch;
  worth remembering as a data point for how much these "walk through every
  combination" asks are worth taking literally rather than skimming. Design
  choices made along the way, don't re-litigate without asking:
  - **The constructor's duplicate-shape check always treats a shape and any
    of its 8 dihedral transforms as the same shape, regardless of a level's
    toggles** — this was initially made toggle-aware (rotation off → a
    rotated copy is a distinct, addable shape) and then explicitly reverted
    the same day: the user asked for it back for simplicity. `allowRotation`/
    `allowMirror` only affect what happens *at spawn time* now (see the
    `ShapeSymmetry`/`PieceGenerator` bullets above), never what counts as a
    duplicate while drawing.
  - Because mirroring is gated live at spawn time (`resolveCells(random, allowMirror)`)
    rather than baked into stored `includeMirror` values, flipping a level's
    mirror toggle takes effect immediately and uniformly across its whole
    shape list — there's no "toggling doesn't retroactively affect
    already-added shapes" asymmetry to worry about for mirroring. Rotation
    has no stored per-shape state at all (`initialRotationSteps` is computed
    fresh every spawn), so the same is trivially true there too.
  - Both are genuine gameplay rules, on par with board size/color
    mode/algorithm — changing either on an existing level triggers the same
    record-reset (or save-as-copy) flow as any other rule change, no special
    casing.
- **Rotation de-emphasized in the game's own framing** (changed 2026-08-11,
  right after the toggle work above shipped): with rotation now just one
  setting among several, the user asked to stop billing it as *the* hook
  everywhere it was mentioned. `MenuScreen`'s subtitle changed from
  "фигуры можно вращать" to "у каждого уровня свои правила";
  `RulesScreen.kt` dropped "это и есть фишка игры" from the rotation bullet
  and now opens its constructor section with "Самое интересное в этой
  игре — сколько всего можно настроить под себя"; this file's own opening
  line leads with the constructor/configurability instead of rotation. The
  *mechanic itself* is unchanged (still on by default, still the same rotate
  button) — only the marketing/framing moved. Don't re-introduce
  rotation-as-the-headline copy without asking again.
- **Manual flip button, alongside rotate, when a level allows mirroring**
  (added 2026-08-11): mirroring had been spawn-time-only (a coin flip when
  the piece is generated) — the user asked for a player-facing control to
  match, so it's not purely luck-of-the-draw whether you get the chirality
  you need. `TraySlot` gained a second icon button (`Icons.Default.Flip`)
  next to rotate, shown whenever `level.allowMirror` is true (independent of
  whether `showRotateButton` is also true — a level could in principle allow
  one but not the other). See the `GameEngine.flip`/`Piece.flippedHorizontally`
  bullets above for the domain side.
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
  per-level records/pausing. Updated same day when rotation/mirror became
  configurable, so "controls" no longer states rotation as an unconditional
  fact and "what the level settings mean" covers both new toggles — then
  updated again the same day to drop the "rotation is the hook" framing
  entirely, see the next bullet.
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

Most recently: rotation and mirror became per-level configurable toggles
(`LevelDefinition.allowRotation`/`allowMirror`) — new `LabeledToggleRow`s in
`LevelEditorScreen`, `ShapeSymmetry.canonicalKey`/`isChiral` parameterized to
match, `GameEngine.rotate` guarded, `TraySlot`'s rotate button hidden when
disabled, `rulesSummary()` calling out non-default settings, `RulesScreen`
and this file updated, and the 3 default levels reworked so each toggle gets
demonstrated by one of them. Working through every combination of the two
new toggles (as asked) surfaced a real bug in the just-shipped domino
`isUnlosable` rule — it implicitly assumed rotation was always available —
fixed by requiring `allowRotation` for that specific case; see Notable
product decisions for the full reasoning.

Immediately after (same session, phone briefly unreachable over `adb` in
between): the constructor's duplicate-shape logic was simplified back to
always treating a shape and its rotations/reflections as one, mirroring
moved from a baked-in per-shape flag to a live spawn-time gate
(`LevelShape.resolveCells(random, allowMirror)`), and rotation gained the
same live treatment via the new top-level `initialRotationSteps` helper in
`PieceGenerator.kt` — a uniform-random starting rotation when a level
disallows rotation, so a shape's distinct reachable orientations (1, 2, 4,
or 8 depending on its own symmetry) all get a real chance to spawn even
though the player can't rotate them into being. `ShapeSymmetry.canonicalKey`/
`isChiral` lost the `allowRotation`/`allowMirror` parameters they'd gained
earlier the same session — reverted at the user's request for simplicity.
Also: rotation's billing as "the" hook was walked back everywhere (menu
subtitle, Rules screen, this file's own opening line) in favor of "lots of
configurable settings" — see Notable product decisions for the full
narrative and reasoning on both changes.

One more round right after: the user tried the app, then asked for three
fixes — drop the "(без вращения)"/"(без отражения)" name suffixes (redundant
with `rulesSummary()`), add a manual flip button for symmetry with rotate,
and fix the default levels' shape lists showing what looked like duplicate
mirror-image rows. All three shipped: `GameEngine.flip`/`Piece.flippedHorizontally`
+ `TraySlot`'s new flip button, `DefaultLevels.kt`'s `classicShapePool()`
dedup, and `ShapeRow`'s two-glyph preview for shapes that can actually spawn
mirrored in the level being edited — see Notable product decisions for all
three.

`assembleDebug` and `testDebugUnitTest` both green throughout, including
`ShapeSymmetryTest`/`ShapeConnectivityTest` (chirality edge cases: S/Z, L/J
mirror pairs; the lone chiral PENTOMINO_L with no legacy mirror — the
briefly-added `allowRotation`/`allowMirror` parameterized cases were removed
along with the revert), `LevelDefinitionTest` for `isUnlosable` (single-cell,
domino/even-board, and the domino-needs-rotation cases, all still valid —
that reasoning didn't change this round), `EasyPieceGeneratorTest`/
`HardModePieceSelectorTest` cases confirming varied starting rotations when
`allowRotation` is false and that a chiral shape never spawns mirrored when
`allowMirror` is false, a new `PieceTest` for `flippedHorizontally` (chiral
shapes change, flipping twice returns to the original, rotation resets to 0),
new `GameEngineTest` cases for `flip`'s no-op-when-disallowed guard, and a
new `DefaultLevelsTest` asserting `classicShapePool()` has no two entries
sharing a canonical key and is exactly 13 shapes (15 legacy shapes minus one
per merged mirror pair). The user visually confirmed the constructor works
("Проверила, пока всё хорошо") after the *first* round of constructor work;
none of the follow-up rounds since (board-size shape validation, full-width
drawing grid, 3 curated defaults, the unlosable checks, save-as-copy, the
rotation/mirror toggles, the simplification-and-reframing round, or this
flip-button-and-defaults-cleanup round) have been visually re-confirmed by
her yet as of this write-up — worth an actual walkthrough before assuming
the UI/UX details are all correct, only the domain logic underneath has
real test coverage. The phone was reachable throughout this round — latest
APK installed via `adb install -r`, pushed to `/sdcard/Download/BlockPuzzle.apk`,
and copied to the desktop, all from *this* round's build.

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
