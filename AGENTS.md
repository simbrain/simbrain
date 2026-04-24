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
- Avoid redundant documentation
- Prefer dialog helpers in `org.simbrain.util.SwingUtils.kt` (e.g. `showWarningDialog`, `showWarningConfirmDialog`, `showInputDialog`) instead of creating new raw `JOptionPane` dialogs
- If a needed dialog helper does not exist, add a reusable utility in `SwingUtils.kt` rather than duplicating dialog setup in feature code
- Do not use code comment separators of any kind (for example `// ----- Section -----`, `// --- Section ---`, `// ========`, or `// ── Section ─────────────────`)
- Use minimal plain comments only when they add clarity
- Test initialization in each test (not `@BeforeEach`)
- Use backtick test names: `` `test that something works`() ``

## Common Tasks

**Adding a simulation:** See `custom_sims/AGENTS_SIMULATIONS.md`

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
    label = "Learning Rate",
    description = "Rate at which weights change"
)

// Conditional visibility
var useAdvanced by GuiEditable(initValue = false, label = "Use Advanced")
var advancedParam by GuiEditable(
    initValue = 0.5,
    label = "Advanced Parameter",
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
./gradlew runSim -PsimName="Simulation Name" # Run simulation
./gradlew test                               # Run tests
```

## Verification Checklist

- [ ] Code follows Kotlin conventions
- [ ] No author comments
- [ ] No code comment separators of any kind
- [ ] User-facing properties use `@UserParameter` or `GuiEditable`
- [ ] Added to appropriate registry (`RULE_LIST`, etc.)
- [ ] Tests pass: `./gradlew test`

## Common Pitfalls

- Don't memorize lists - use discovery tools
- Check registries (`RULE_LIST`) - not all features are in UI
- Use `withGui { }` for GUI-only code in simulations
- Custom classes need XStream serialization configuration

## Resources

- `custom_sims/AGENTS_SIMULATIONS.md` - Simulation development guide
- [Running From Source](https://github.com/simbrain/simbrain/wiki/Running-From-Source)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
