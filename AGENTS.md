# Simbrain Source Code - Guide for AI Assistants

Simbrain is a visual neural network simulator written in Kotlin. This guide helps AI coding assistants work effectively on the codebase.

## Project Overview

- Visual neural network simulator with GUI workspace
- Supports networks, worlds, plots, and data tables
- Extensible through custom simulations
- **Tech**: Kotlin (primary), Java (legacy), Gradle, Swing + Piccolo2D, XStream

**For simulation development**, see `custom_sims/AGENTS_SIMULATIONS.md`

## Architecture

**4-Layer Pattern:**
```
Core Model → Component → Panel → DesktopComponent
```

Example: `Network.kt` → `NetworkComponent.kt` → `NetworkPanel.kt` → `NetworkDesktopComponent.kt`

**Key Directories:**
- `network/` - Neural networks (neurons, synapses, groups, training)
- `world/` - Environments (odorworld, imageworld, textworld, dataworld)
- `workspace/` - Component management, couplings, serialization
- `custom_sims/` - Simulations (see `custom_sims/AGENTS_SIMULATIONS.md`)
- `plot/` - Visualizations
- `util/` - Utilities

## Discovery Process

Don't memorize - discover what exists NOW using these tools:

```bash
# Explore structure
list_dir: "src/main/kotlin/org/simbrain/network/"
glob_file_search: "**/*Component.kt"

# Find features
codebase_search: "How does network update sequencing work?" ["network"]
grep: "@UserParameter" "network/updaterules/"
grep: "RULE_LIST" "network/updaterules/"  # Check UI availability
```

**Key patterns:**
- `@UserParameter` / `GuiEditable` - User-editable properties
- `enum class` - Dropdown options
- `RULE_LIST` - Features available in UI (only add features in this list)
- `createAction(...)` - Menu/toolbar actions

## Code Conventions

- Prefer Kotlin to Java
- No author comments
- Use a brief file-level KDoc for new Kotlin files and Kotlin files receiving substantive edits; do not do retrofit-only documentation passes
- Put file-level KDoc at the very top of the `.kt` file, before `package`, and use it to explain the file's role and important collaboration or ownership boundaries
- Do not add class-level documentation just to repeat a file header; retain declaration documentation only when it independently clarifies a public API or non-obvious behavior
- Skip a file header when it would be purely redundant, such as for a trivial file whose purpose is fully clear from its name and sole declaration
- Apply this convention only to Kotlin source and test files, not Java files
- Prefer dialog helpers in `org.simbrain.util.SwingUtils.kt` (`showMessage`, `showConfirm`, `showChoice`, `showResults`, `showText`, `showInputDialog`) instead of creating new raw `JOptionPane` dialogs
- If a needed dialog helper does not exist, add a reusable utility in `SwingUtils.kt` rather than duplicating dialog setup in feature code
- Do not use code comment separators of any kind (for example `// ----- Section -----`, `// --- Section ---`, `// ========`, or `// ── Section ─────────────────`)
- Use minimal plain comments only when they add clarity
- Write new comments and docs about the code as it now is: do not mention the discussion that produced a change, previous names, or what was renamed or replaced
- Keep hand-written comments: explanations of intent, rationale, domain meaning, caveats, or decision history are valuable even if long or informal; never delete or rewrite them as part of a cleanup
- Short `@param` / `@return` tags are fine (doc builders use them); trim one only when it is fully redundant with the summary sentence above it and adds no detail
- When converting Java to Kotlin, prefer `?` over prose for nullability, but do not strip other Javadoc
- Comment cleanups may auto-fix only exact-pattern redundancy (e.g. `@param foo the foo`); list anything judgment-based for the user instead of changing it. When in doubt, keep the comment
- Test initialization in each test (not `@BeforeEach`)
- Use backtick test names: `` `test that something works`() ``
- Avoid class-level doc comments that frame ordinary unit tests as regressions or describe specific historical bugs; put scenario intent in the test name and only use local comments when they clarify a non-obvious setup or assertion.

## UI Text Capitalization

Use sentence case for all user-facing UI text: menus and submenus, `createAction` names, buttons, dialog and window titles, tabs, checkboxes, `GuiEditable` and `@UserParameter` labels, enum display names, and tooltips.

- Capitalize the first word only: "Add neuron group...", "Learning rate", "Reset weights"
- Keep proper nouns and acronyms as written: Hebbian, Hopfield, Leabra, SOM, PCA, LSTM, CNN
- Treat world names as proper nouns: Odor World, Text World
- Keep status markers such as `(Beta)` capitalized
- When changing a label, update docs in `simbraindocs/` that quote it, since docs match UI labels exactly

## Shape Captions

Give a network node a `ShapeCaption` only when its drawing hides its shape, as with tensors (hidden channels), convolution kernels, and weight matrices. Nodes that draw one cell per unit, such as neuron arrays, get no caption; their size goes in the tooltip. Update the `showShapeCaptions` preference description when this list changes.

## Common Tasks

**Adding a simulation:** See `custom_sims/AGENTS_SIMULATIONS.md` (includes neuron layout spacing pitfalls and `SimulationLayoutUtils.kt` for multi-layer subnetworks)

**Adding a neuron type:**
1. Create class in `network/updaterules/` extending `NeuronUpdateRule`
2. Add `@UserParameter` or `GuiEditable` for properties
3. Add to `RULE_LIST` in `NeuronUpdateRule.kt`
4. Test in GUI

**Adding a synapse type:**
1. Create class in `network/synapse_update_rules/` extending `SynapseUpdateRule`
2. Add parameters with `@UserParameter` or `GuiEditable`
3. Add to `RULE_LIST` in `SynapseUpdateRule.kt`
4. Test in GUI

## User-Editable Properties

**Kotlin (preferred):**
```kotlin
var learningRate by GuiEditable(
    initValue = 0.1,
    label = "Learning rate",
    description = "Rate at which weights change"
)

// Conditional visibility
var useAdvanced by GuiEditable(initValue = false, label = "Use advanced")
var advancedParam by GuiEditable(
    initValue = 0.5,
    label = "Advanced parameter",
    conditionallyEnabledBy = MyClass::useAdvanced
)

// Enum options
enum class ActivationFunction { LINEAR, SIGMOID, TANH, RELU }
var function by GuiEditable(initValue = ActivationFunction.SIGMOID)
```

## Build Commands

```bash
./gradlew build                              # Build project
./gradlew run                                # Run Simbrain
./gradlew runSim -PsimName="Simulation name" # Run simulation
./gradlew test                               # Run tests
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.KitchenSinkSnapshot # Render a UI snapshot
./gradlew compileSnapshotsKotlin             # Compile the snapshot definitions
```

## UI Snapshots

UI snapshot definitions live in `src/snapshots/kotlin/org/simbrain/util/uisnapshot/`; `src/snapshots/README.md` documents the harness. Use them to verify Swing/Piccolo layout, dialog sizing, and visual overlap without manually running the full app. Generated images are written to `build/ui-snapshots/`.

**A snapshot is temporary by default.** Write the `UiSnapshotDef`, run it, look at the image, and delete the file before committing. Keep it temporary when it:

- Checks your own change once: a dialog's sizing, a label overlap, a fix before and after, a look in dark mode
- Renders one simulation's layout (recipe in `custom_sims/AGENTS_SIMULATIONS.md`)
- Wraps a single constructor call, such as an `AnnotatedPropertyEditor` on one object or a dialog in its default state
- Checks something a test can assert: which menu items exist and what they say, whether something is present, enabled, or inside the view, what survives a save and restore. Write the test instead
- Produces a figure for a paper, talk, or documentation. Keep that definition with the figure's source, not in this repository

**Commit a snapshot only when the next person to change that code would re-run it and could not rebuild it in a few minutes.** It should be one of:

- A reference sheet for a shared visual primitive that many screens depend on: theme widgets, icons, chart chrome, message dialogs, frame chrome, one of every canvas node
- A deliberate set of cases for a drawing or layout rule, side by side: a range of spacings, every display mode, the edge cases
- A state that takes real setup to reach by hand (a trained model, a drag in progress, a hand-built dialog showing its error state) in a feature whose appearance is still likely to change

A committed snapshot must:

- Open with a KDoc saying what the image shows and what correct looks like
- Build its scene from production code rather than a copy of it, and render the same way every run (seed any randomness)
- Say in its KDoc if it needs anything outside a clean checkout, such as downloaded model weights
- Use `-Ptheme=dark` for dark mode rather than a separate dark class

Before adding one, list the directory and extend an existing snapshot of the same surface with a new case where that works. Delete a committed snapshot once it stops meeting the bar, for example when a test now covers what it showed.

`./gradlew build` and `./gradlew test` do not compile this source set. After changing an API that snapshots use, run `./gradlew compileSnapshotsKotlin`.

## Verification Checklist

- [ ] Code follows Kotlin conventions
- [ ] No author comments
- [ ] No code comment separators of any kind
- [ ] User-facing properties use `@UserParameter` or `GuiEditable`
- [ ] Added to appropriate registry (`RULE_LIST`, etc.)
- [ ] Tests pass: `./gradlew test`
- [ ] Temporary UI snapshots deleted; any committed one meets the bar in "UI Snapshots"

## Common Pitfalls

- Don't memorize lists - use discovery tools
- Check registries (`RULE_LIST`) - not all features are in UI
- Use `withGui { }` for GUI-only code in simulations
- Custom classes need XStream serialization configuration

## Resources

- `custom_sims/AGENTS_SIMULATIONS.md` - Simulation development guide
- [Running From Source](https://github.com/simbrain/simbrain/wiki/Running-From-Source)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
