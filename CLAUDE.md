# Block Puzzle 8x8 (rotate) — project notes

Native Android block-puzzle game. Kotlin + Jetpack Compose, MVVM, fully offline
(no INTERNET permission). The hook is the **level constructor**: board size,
color mode, algorithm, rotation, and a hand-drawn weighted shape pool are all
per-level configurable — not one fixed game mode. (Rotation — pieces can be
freely rotated before placing — was the original headline mechanic before the
constructor existed and is still on by default, but as of 2026-08-11 it's one
setting among several, not *the* distinguishing feature; see Notable product
decisions before re-emphasizing rotation specifically anywhere in copy.)
Package: `com.blockpuzzle.rotate`.

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
  `MSYS_NO_PATHCONV=1` to stop that. The same prefix is needed for
  `adb shell screencap` + `adb pull` of the result, for the same reason.
- Launching the app from a shell: the launcher activity is
  `com.blockpuzzle.rotate/.ui.MainActivity` — **not** `.../.MainActivity`.
  `MainActivity` lives in the `ui` package, so the shorthand component name
  needs that segment (`adb shell am start -n com.blockpuzzle.rotate/.ui.MainActivity`);
  omitting it fails with "Activity class ... does not exist."
- Last known-good state (2026-08-11, after the level constructor rewrite and
  several same-day follow-up rounds — most recently an undo-penalty feature,
  a game-top-bar overflow fix, and before that reverting mirroring — see
  Notable product decisions — and a paused-game bugfix): `assembleDebug` and
  `testDebugUnitTest` both green. Latest APK installed via `adb install -r`,
  pushed to `/sdcard/Download/BlockPuzzle.apk`, and copied to the desktop —
  all from the current round. Both of this round's changes were visually
  re-confirmed by the user on-device (see Status below); most earlier rounds'
  changes still haven't been.

## Architecture

- **domain/** — pure Kotlin, zero Android deps, unit-tested with JUnit4.
  Immutable models (`Board`, `GameState`, `Piece`); every move produces a new
  `GameState`, so undo is just a stack of prior states (`GameEngine.history`).
  - `LevelDefinition(tag, name, boardSize: Int, colorMode: ScoringMode, algorithm: GameMode, shapes: List<LevelShape>, allowRotation: Boolean = true)`
    is a full game variant — **user-created via the level constructor**, not
    a fixed enum combination. `tag` is the persisted-record key and is
    assigned once at creation (`LevelDefinition.nextAvailableTag`), never
    regenerated on edit. `boardSize` is a free `Int` in `ALLOWED_BOARD_SIZES`
    (5..8) — there is no more `BoardSize` enum. This replaced the old fixed
    `GameVariant` (8 combinations, deleted) — see Notable product decisions.
    `allowRotation` (added 2026-08-11) defaults `true` so old persisted
    levels decode with the behavior they always had. There is deliberately
    **no `allowMirror` setting** — see Notable product decisions for why.
    `undoPenaltyPercent` (added 2026-08-11) defaults to
    `ScoringConfig.DEFAULT_UNDO_PENALTY_PERCENT` (20) for the same
    old-levels-decode-unchanged reason; see `GameEngine.undo()` above and
    Notable product decisions below.
  - `PieceShape(id, baseCells)` replaced the old closed `ShapeType` enum —
    it's an open value type now so the constructor's hand-drawn shapes and
    the 15 built-in ones (`PieceShape.LEGACY_CATALOG`) are the same type.
    Mirror pairs (TETROMINO_L/J, TETROMINO_S/Z) are listed as fully
    independent constants — a shape's mirror image is just a different
    shape, not a special case (see `ShapeSymmetry`'s doc comment).
  - `LevelShape(shape: PieceShape, weight: Int)` is one pool entry inside a
    level — just a shape and its weight, nothing else. `weight` only matters
    for the Случайный (EASY) algorithm; Хитрый (HARD) treats every entry as
    equally eligible regardless of weight.
  - `LevelDefinition.isUnlosable(shapes, boardSize, allowRotation)` — true in
    exactly two *provable* cases: (1) every shape has `cellCount <= 1` (e.g.
    only `PieceShape.SINGLE`) — a lone cell always fits somewhere until a
    line clears it, true for any board size and regardless of rotation (a
    point has no orientation); (2) every shape has `cellCount == 2`
    (domino), `boardSize` is even, **and `allowRotation` is true** —
    checkerboard-color argument: any 2-cell orthogonally-adjacent placement
    always covers one black + one white cell, and a full line of *even*
    length always splits evenly by color, so the black-filled ==
    white-filled invariant can never break, ruling out the classic
    "scattered isolated single-cell gaps" deadlock. **Why `allowRotation` is
    required for the domino case**: a domino's `baseCells` fix *one*
    orientation, and reflecting a straight 2-cell piece doesn't change it
    (its mirror image is itself) — without rotation, every spawned domino is
    stuck in that one fixed orientation, so a board left with only
    *cross*-oriented adjacent gaps blocks it completely even with plenty of
    empty cells remaining; the color-balance argument doesn't rescue that
    case. On *odd* board sizes a full line splits unevenly, so a line clear
    *can* break the color invariant regardless of rotation — deliberately
    left unflagged there, no proof either way. Does **not** extend to
    3+-cell shapes or mixed-size pools — see the doc comment before
    generalizing further, this is intentionally narrow rather than a
    general solvability prover. `LevelEditorScreen` disables Save and shows
    a matching inline message.
  - `ShapeSymmetry` (`canonicalKey`/`rotate90`/`normalize`) backs the level
    editor's shape-drawing validation: a shape and its rotation can't be
    added twice. **A shape and its mirror image are deliberately NOT
    treated as the same shape** — an earlier version merged mirror pairs
    into one pool entry with a spawn-time coin flip and a manual flip
    button; the user found that confusing/annoying in practice and asked
    for it to be fully reverted (see Notable product decisions). Don't
    reintroduce mirror-awareness anywhere without asking again.
  - `ShapeConnectivity` (8-directional BFS) checks that all of a drawn
    shape's cells are connected diagonally-or-not.
  - Piece generation is behind `PieceGenerator`, over `List<LevelShape>`
    instead of a fixed shape enum: `EasyPieceGenerator` (weighted random —
    `pickWeighted`) vs `HardModePieceSelector` (lookahead — samples
    candidates, scores by resulting playability, picks the hardest option
    that still leaves a minimum margin so the board stays technically
    solvable; ignores weight, same as before). Both take an `allowRotation`
    constructor param (from the active `LevelDefinition`).
    `PieceGenerator.kt`'s top-level `initialRotationSteps(allowRotation, random)`
    decides the `rotationSteps` a freshly spawned piece starts with: `0`
    when rotation is allowed (the player fixes orientation themselves via
    the rotate button, so the spawn value is irrelevant), or a uniform
    random pick from `0..3` when it's disallowed — since the spawned
    orientation is then final for that piece's whole life. Picking uniformly
    over the 4 *raw* steps (rather than deduplicating first) still yields a
    uniform distribution over the shape's *distinct* visual outcomes for
    free: `Piece.cells` normalizes the result, so any rotational symmetry
    naturally folds multiple raw steps onto the same outcome, evenly (e.g. a
    straight domino/triomino: steps 0 and 2 always coincide, so its 2
    distinct orientations still land 50/50). `HardModePieceSelector` applies
    the exact same per-candidate randomization before evaluating each one,
    rather than restructuring its sampling step to enumerate every
    orientation as a separate candidate.
  - `GameEngine.colorProvider: () -> PieceColor` decides piece color
    independently of shape selection — CLASSIC ("Однотонный") is hardcoded to
    `PieceColor.BLUE` (fixed, not user-configurable — see below), COLOR_BONUS
    ("Цветной") uses random. GameEngine always overwrites whatever color the
    generator provisionally assigned.
  - **Tray refill is batched, not one-for-one**: placing a piece just empties
    its slot; a fresh trio of 3 only appears once all three slots are empty.
    This is what makes the game losable (a 1-for-1 refill always bails you
    out with a fresh roll). See `GameEngine.place()`.
  - `GameEngine.rotate(trayIndex)` is a no-op when `level.allowRotation` is
    false — the domain layer is the single source of truth for this rather
    than relying only on the UI hiding the rotate button (`TraySlot`'s
    `showRotateButton` param, wired from `uiState.level.allowRotation` in
    `GameScreen`).
  - `GameEngine.undo()` (added 2026-08-11) deducts
    `LevelDefinition.undoPenaltyPercent` percent — floored, via
    `ScoringConfig.undoPenalty(scoreBeforeUndo, undoPenaltyPercent)` — from
    the score, so retrying a placement isn't free. The percent is applied to
    the score **at the moment undo is pressed** (i.e. `state.score`, the
    higher post-move score), not the destination state's own (lower,
    pre-move) score — the two differ whenever the undone move scored
    anything, so this is a real deduction on top of losing the move's
    points, not just "restore the old score." The result is clamped with
    `.coerceAtLeast(0)`, so the score can never go negative. See Notable
    product decisions for why this shape (percent-of-current-score, per-level
    configurable, no separate undo-count cap) was chosen.
  - `PieceColor` enum currently has **6** colors (RED, ORANGE, YELLOW, GREEN,
    BLUE, PURPLE) — deliberately cut down from an original 12 because too
    many close hues made monochrome-line color-bonus play impractical.
- **data/** — DataStore Preferences repos, no network:
  - `LevelsRepository` — the full level list (built-in-seeded + custom) as
    one JSON blob (`kotlinx.serialization`, `ignoreUnknownKeys = true` so
    removed fields like the old `allowMirror`/`includeMirror` in
    already-persisted levels just get dropped harmlessly on decode) in a
    `levels` DataStore. `save`/`delete` decode-mutate-encode inside `edit {}`
    (transactional).
  - `RecordsRepository` — one high score per level **tag** (plain `String`
    now, not `GameVariant`).
  - `DefaultLevels.kt` (`seedDefaultLevelsIfNeeded`) — one-shot, gated by
    `LevelsRepository.defaultsSeeded`: seeds **3** curated example levels
    (not the original 8 Easy/Hard × Classic/ColorBonus × 8×8/6×6 combos —
    that many similarly-named defaults was confusing to pick from, see
    Notable product decisions). Kept in its own file, isolated from both
    repositories, so it's easy to find/replace again later.
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
  - `GameViewModel.exitToLevelList()` only parks the current game into
    `pausedEngines` when its score is **greater than 0** (added 2026-08-11,
    bugfix) — a score of 0 is treated as an accidental tap-and-leave rather
    than a real in-progress game. Before this, starting a level and
    immediately backing out left a stale paused engine sitting in
    `pausedEngines` under that level's tag; if the level was later edited in
    the constructor, tapping it again in "Играть" would resume that stale
    engine (built from the *old* shapes/board size, captured at
    `GameEngine` construction time) instead of starting a fresh game against
    the edited rules. This fixes exactly that reported case. It does not
    fully solve the general "paused engine goes stale if its level is
    edited while paused" problem for a *nonzero*-score paused game — that's
    a separate, harder question (would need `saveLevel` to know about and
    evict any paused engine for the tag it's editing) that wasn't asked for
    and hasn't been addressed.
  - `LevelEditorScreen` is one scrollable screen, no wizard: name → board
    size (5/6/7/8, generalized `ToggleButton`/`LabeledToggleRow` from
    `ui/components/ToggleButton.kt`) → color mode → algorithm → **вращение**
    (`LabeledToggleRow<Boolean>`, "Включено"/"Выключено") → **штраф за отмену
    хода (undo)** (added 2026-08-11: a `−`/`+` stepper, step 5, range 0–100,
    reusing the same `IconButton` row pattern as the shape weight steppers
    further down, editing `undoPenaltyPercent`) → shape list
    (weight steppers shown only for Случайный) → "Добавить фигуру" opens a
    tap-grid `Dialog` (`Trunc(boardSize*0.8)` square, `BoxWithConstraints`-sized
    to always span the dialog's full width — cell size scales to fit, not a
    fixed dp) with live `ShapeConnectivity`/`ShapeSymmetry` validation.
    Draft fields use `rememberSaveable` (shapes as a JSON string, since
    `LevelShape` is already `@Serializable`) so an in-progress draft
    survives rotation. **Shrinking the board size drops any already-added
    shape that no longer fits** the new `Trunc(boardSize*0.8)` bound
    (`changeBoardSize` filters `shapes` and shows an inline count of how
    many were removed). Save is also disabled with an inline message when
    `LevelDefinition.isUnlosable(shapes, boardSize, allowRotation)` is true —
    the message text itself distinguishes the single-cell-only case from the
    domino/even-board case. The shape-drawing dialog's duplicate-shape error
    message says "с учётом поворота" — rotation-only, no mirror mention.
    Saving with any *rule* field changed (board size/color
    mode/algorithm/shapes/weights/вращение/undo-penalty percent) resets that
    level's record via `GameViewModel.saveLevel`'s diff — renaming alone
    does not. **Unless the
    edited level currently has a nonzero record**, in which case pressing
    Save opens `RecordAtRiskDialog` offering a choice: overwrite in place
    (resets the record, old one-path behavior) or "save as a copy"
    (`saveLevel(..., saveAsCopy = true)` — always takes the fresh-tag path
    via `nextAvailableTag` regardless of `editingTag`, so the original level
    and its record are never touched; the copy's name gets " (копия)"
    appended automatically if the user didn't already rename it, so the two
    don't look identical in the list). A record of 0 skips the dialog
    entirely and saves in place exactly as before.
  - `LevelDefinition.rulesSummary()` (`ui/components/LevelLabels.kt`) builds
    the "Однотонный · Случайный · 6×6"-style one-liner shown on level rows,
    the game top bar, and game-over — it only appends "· без вращения" (or,
    added 2026-08-11, "· undo -X%"/"· undo бесплатно") when a level actually
    deviates from its default (rotation enabled / 20% undo penalty), to avoid
    cluttering every row with a badge nobody needs to see on the common case.
  - **Game top bar overflow fix** (2026-08-11): `GameTopBar` in
    `GameScreen.kt` lays out the home icon, `rulesSummary()` text, and undo
    icon in one `Arrangement.SpaceBetween` `Row`. On a level whose summary
    is long enough to include the "· без вращения" suffix (e.g. "Цветной
    хитрец 6×6"), the text used to be wide enough to push the undo icon off
    the edge of the screen entirely. Fixed by giving the `Text` itself
    `Modifier.weight(1f)` (so it's now the only flexible element between the
    two fixed-size icons) plus `maxLines = 2` and
    `overflow = TextOverflow.Ellipsis` so a still-too-long summary wraps
    instead of overflowing. Reproduced and visually re-confirmed fixed on
    "Цветной хитрец 6×6" specifically, per the original bug report.
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
- **Only 3 default levels, not 8** (changed 2026-08-11): the original
  migration seeded all 8 old Easy/Hard × Classic/ColorBonus × 8×8/6×6
  combos as a safety net so nothing from the pre-constructor game was lost.
  The user found 8 similarly-named defaults "easy to get confused by" and
  asked for exactly 3, chosen to demonstrate breadth rather than
  completeness: "Классика 8×8" (`PieceShape.LEGACY_CATALOG`,
  Однотонный/Случайный — the original, still-default experience), "Цветной
  хитрец 6×6" with rotation turned **off** (Цветной/Хитрый — a meaningfully
  harder "place it exactly as dealt" variant), and "Мини-вызов 5×5" with a
  small hand-picked, non-uniformly weighted shape pool. See
  `DefaultLevels.kt`. Don't go back to seeding one level per
  mode/scoring/size combination without asking again.
- **Rotation is per-level configurable, not just fixed behavior** (added
  2026-08-11): rotation was the game's headline mechanic since before the
  constructor even existed ("Unique mechanic: pieces can be freely rotated
  before placing"). The user asked for it to become a level-scoped toggle
  (`LevelDefinition.allowRotation`, default `true`). It's a genuine gameplay
  rule, on par with board size/color mode/algorithm — changing it on an
  existing level triggers the same record-reset (or save-as-copy) flow as
  any other rule change, no special casing.
- **Rotation de-emphasized in the game's own framing** (changed 2026-08-11,
  right after the toggle work above shipped): with rotation now just one
  setting among several, the user asked to stop billing it as *the* hook
  everywhere it was mentioned. `MenuScreen`'s subtitle changed from
  "фигуры можно вращать" to "у каждого уровня свои правила"; `RulesScreen.kt`
  opens its constructor section with "Самое интересное в этой игре —
  сколько всего можно настроить под себя"; this file's own opening line
  leads with the constructor/configurability instead of rotation. The
  *mechanic itself* is unchanged (still on by default, still the same
  rotate button) — only the marketing/framing moved. Don't re-introduce
  rotation-as-the-headline copy without asking again.
- **Mirroring was tried as a whole feature axis, then fully reverted the
  same day** (2026-08-11): for one round of the session, a shape and its
  mirror image counted as "the same shape" for constructor-dedup purposes,
  with a per-level `allowMirror` toggle controlling a spawn-time coin flip
  between chiralities, a manual player-facing flip button in `TraySlot`
  next to rotate, and a two-glyph preview in the shape-editing list to show
  both forms. The user tried it and reported the experience was
  "раздражает, вызывает желание от него избавиться" (annoying, makes you
  want to get rid of it) and asked for it to be reverted **everywhere,
  including documentation** — explicit instruction not to just patch
  around it. A shape's mirror image is now, once again, simply a second,
  independent shape the player draws separately if they want it — exactly
  how the pre-constructor game always worked (TETROMINO_L/J and S/Z were
  always separate catalog entries). This touched a lot of surface area:
  `LevelDefinition.allowMirror`, `LevelShape.includeMirror`/`resolveCells`,
  `ShapeSymmetry.mirror`/`isChiral`, `GameEngine.flip`/
  `Piece.flippedHorizontally`, `TraySlot`'s second icon button, the
  two-glyph `ShapeRow` preview, `DefaultLevels.kt`'s `classicShapePool()`
  dedup helper, and the corresponding `rulesSummary()`/`RulesScreen.kt`
  copy — all removed. `ShapeSymmetry.canonicalKey` went back to a plain
  rotation-only (4-transform) definition. **Don't reintroduce any
  mirror/reflection-awareness feature — spawn-time, player-controlled, or
  constructor-dedup — without asking again first**, and if asked, mention
  this history so it isn't repeated blind.
- **A level's shape pool can't be all-single-cell, or (on an even board)
  all-domino** (added 2026-08-11, `LevelDefinition.isUnlosable`): started
  from the user's own example ("only a block of 1 cell") — provably always
  placeable, any board size. She then asked whether an all-domino pool on
  8×8 had the same problem and asked for the conditions to be *calculated*
  per board size rather than guessed. The domino case turned out to only be
  provable for **even** board sizes (a checkerboard-coloring argument — see
  the domain doc comment); odd sizes (5, 7) are deliberately left unflagged
  since there's no proof either way there, not because they're known-safe.
  This does not generalize to any shape with 3+ cells — that's a genuinely
  harder, unproven question, and the rule stays a narrow "provably always
  safe" check, not a general solvability prover. Don't expand it further
  without doing the same kind of actual derivation (or asking) — a
  plausible-sounding formula that isn't actually proven is worse than no
  rule at all here, since a false positive would block a level that's
  actually fine.
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
- **Paused games with a score of 0 aren't kept, and a paused game with real
  progress now counts toward "is this level's record at risk?"** (added
  2026-08-11, two-part bugfix, second part generalizing the first the same
  day): the user found that starting a level and immediately backing out,
  then later editing that level in the constructor, could resume the stale
  pre-edit game instead of reflecting the edit. First fix, her proposed
  approach implemented as-is: treat a 0-score exit as an accidental tap,
  don't park it (`GameViewModel.exitToLevelList()` only calls
  `pausedEngines[tag] = e` when `e.state.score > 0`). She then asked for
  the *general* case: a paused game's live score should count the same way
  a persisted record does when deciding whether editing a level risks
  losing progress. `GameViewModel.pausedScores: StateFlow<Map<String, Int>>`
  (populated in `updateResumable()` alongside `resumableLevelTags`, same
  idea as `GameScreen`'s live `maxOf(record, score)` top-bar display) is
  folded into the *effective* record `MainActivity` passes to
  `LevelEditorScreen`: `maxOf(records[tag] ?: 0, pausedScores[tag] ?: 0)`.
  That's the only change needed on the "detect risk" side — `hasRecordAtRisk`/
  `RecordAtRiskDialog` in `LevelEditorScreen` already just consume whatever
  `record` they're given, no changes there. On the save side,
  `GameViewModel.saveLevel()`'s existing `rulesChanged` branch now also does
  `pausedEngines.remove(tag)` right alongside `recordsRepository.resetScore(tag)`
  — so choosing "overwrite" (`saveAsCopy = false`) discards the stale paused
  game along with the record, while choosing "save as copy" leaves it
  completely untouched for free: `rulesChanged` is defined as
  `!saveAsCopy && (...)`, so it's always `false` on the copy path and the
  `pausedEngines.remove` line is never reached — the unfinished game simply
  stays parked under the *original* tag, exactly where the user asked for
  it to remain. No special-casing needed for "effective record is 0 but a
  paused game still exists" either — `exitToLevelList` never parks a
  0-score game in the first place, so that combination can't arise.
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
- **Undo now costs points, per-level configurable** (added 2026-08-11,
  `LevelDefinition.undoPenaltyPercent`): before this, `GameEngine.undo()`
  was a free, unlimited retry — press undo, try a different placement, no
  cost — which the user flagged as an easy way to cheat. Design was decided
  via Q&A rather than guessed:
  - **Proportional to the current score at the moment undo is pressed**, not
    a flat constant and not proportional to the last move's own points —
    she picked this explicitly over the flat-penalty and
    proportional-to-last-move alternatives that were offered. Concretely,
    the deduction is `undoPenaltyPercent`% of `state.score` (the score
    *before* undoing, i.e. after the move being undone), subtracted from
    the destination state's own (lower) score — so undoing a move that
    scored points costs strictly more than just losing those points would.
    See `GameEngine.undo()`'s doc comment for the exact arithmetic.
  - **Clamped at 0** — she chose this over letting the score go negative.
  - **No separate cap on undo count** — the percentage penalty alone is the
    deterrent; she explicitly declined a "max N undos per game" limit when
    offered as an option.
  - **Per-level, not a single global constant** — her own follow-up ask,
    after initially being offered flat percent choices (10/20/25%): "add it
    as a level setting" instead. This is why it's
    `LevelDefinition.undoPenaltyPercent`, not a plain `ScoringConfig`
    constant, and why it has a `LevelEditorScreen` stepper and participates
    in the `rulesChanged`/record-reset-or-save-as-copy flow like every other
    rule field.
  - **Default 20%, editable in steps of 5, range 0–100** — also decided via
    Q&A; 0% is an explicitly valid "no penalty" choice for a level author
    who wants free undos on a specific level, not a special-cased "off"
    state.
  Don't change this shape (e.g. switch to flat-penalty, add an undo-count
  cap, or move it back to a global constant) without asking again — each of
  those was a real alternative the user considered and declined.

## Status as of last session (2026-08-11)

Level constructor implemented per the plan agreed with the user: domain model
(`LevelDefinition`/`LevelShape`/`PieceShape`/`ShapeSymmetry`/`ShapeConnectivity`),
data layer (`LevelsRepository`, re-keyed `RecordsRepository`,
`DefaultLevels.kt`), and full UI (gutted `MenuScreen`, new
`LevelListScreen`/`ConstructorScreen`/`LevelEditorScreen` with a tap-to-draw
shape dialog, plus `RulesScreen` and a Топ-5 card on the menu). Several
same-day follow-up rounds refined it: board-size shape validation, a
full-width shape-drawing grid, cutting the default levels from 8 to 3, the
`isUnlosable` single-cell and domino/even-board checks, and a save-as-copy
flow so editing a leveled-up level doesn't silently reset its record.

One round added rotation and mirroring as per-level toggles, including a
manual flip button and mirror-aware shape deduplication/preview in the
constructor. The user tried it, found the mirroring half of that
"раздражает, вызывает желание от него избавиться," and asked for it to be
fully reverted everywhere (game and docs) while keeping rotation exactly as
it was — see the "Mirroring was tried..." bullet in Notable product
decisions for the full account and everything that got removed. Rotation's
own toggle/behavior is untouched by this revert.

Same round: a bug where starting a level and immediately leaving with a
score of 0 could later resume a stale pre-edit game after the level was
edited in the constructor — fixed by not parking 0-score games at all (the
user's own proposed fix, implemented as specified; see the
`GameViewModel.exitToLevelList()` bullet above).

Immediately after: the user asked for that fix's *general* case — a paused
game with real (nonzero) progress should count the same as a persisted
record when deciding whether editing a level risks losing something, with
the existing overwrite-vs-save-as-copy choice applying either way, and a
paused game preserved exactly where it was on the copy path / discarded on
the overwrite path. Implemented via `GameViewModel.pausedScores` (a new
`StateFlow<Map<String, Int>>` populated in `updateResumable()`), folded into
an effective record `MainActivity` computes as
`maxOf(records[tag] ?: 0, pausedScores[tag] ?: 0)` before passing it to
`LevelEditorScreen` — no changes needed to `LevelEditorScreen`'s own
risk-detection/dialog logic, it already just consumes whatever `record` it's
given. `saveLevel()`'s existing `rulesChanged` branch now also evicts
`pausedEngines[tag]` alongside the record reset; since `rulesChanged` is
always `false` on the save-as-copy path, that eviction is naturally skipped
there, leaving the paused game parked under the original tag. See the
"Paused games with a score of 0 aren't kept..." bullet in Notable product
decisions for the full account.

`assembleDebug` and `testDebugUnitTest` both green throughout. Current
domain test coverage: `ShapeSymmetryTest` (rotation-only canonicalKey
invariance, mirror pairs no longer sharing a key, every legacy-catalog shape
having a distinct key), `ShapeConnectivityTest`, `LevelDefinitionTest` for
`isUnlosable` (single-cell, domino/even-board, domino-needs-rotation),
`EasyPieceGeneratorTest`/`HardModePieceSelectorTest` (weighted pool, varied
starting rotation when `allowRotation` is false), `GameEngineTest`
(including the rotate-disabled no-op guard and, as of this round, the
undo-penalty tests below), `BoardTest`. No dedicated tests exist for the
`ui`/`data` layers (project convention — see Verification notes in prior
sessions). The user visually confirmed the constructor works ("Проверила,
пока всё хорошо") after the *first* round of constructor work; none of the
many follow-up rounds between then and this one were visually re-confirmed
by her — this round is the exception, see below. The phone was reachable
throughout this round — latest APK installed via `adb install -r`, pushed
to `/sdcard/Download/BlockPuzzle.apk`, and copied to the desktop, all from
*this* round's build.

**This round** (same day, two independent changes): (1) the undo-penalty
feature described above under Notable product decisions —
`LevelDefinition.undoPenaltyPercent`, `GameEngine.undo()`'s deduction logic,
the `LevelEditorScreen` stepper, and the `rulesSummary()`/`rulesChanged`
plumbing, with three new `GameEngineTest` cases (proportional deduction,
clamp-at-zero, 0%-means-no-penalty); (2) the `GameTopBar` overflow fix
described above. Both were visually re-confirmed on-device by the user this
round — screenshotted via `adb shell screencap` after she unlocked the
phone (it locks/sleeps aggressively, so a couple of screencap attempts
during this round came back solid black before that): the top bar on
"Цветной хитрец 6×6" now wraps its two-line rules summary cleanly with both
icons fully visible at the edges, and the new undo-penalty stepper in that
level's editor renders correctly showing its 20% default. Programmatic
drag-and-drop placement via `adb shell input swipe` did not register in
Compose's `detectDragGestures` (single linear swipes don't produce enough
intermediate move events) — that's an automation limitation, not verified
game behavior, so the actual point deduction on a real undo was confirmed
via the new unit tests rather than an on-device play-through.

Google Play publishing was explicitly **paused** by the user 2026-08-11 (she
said she changed her mind about it "for now") in favor of this feature —
don't steer conversations back toward it unprompted. If it resurfaces, note
that essentially no publishing prep exists yet (no release signing config,
no store listing, no privacy policy) — see the Play-publishing prompt from
that earlier conversation if one exists, don't re-derive the checklist from
scratch, but don't assume progress was made either.
