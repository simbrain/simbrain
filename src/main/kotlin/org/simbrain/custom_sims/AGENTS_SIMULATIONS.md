# Simulation Creation Guide for AI Assistants

This guide helps AI coding assistants create and modify custom simulations in Simbrain.

## Quick Start

1. Create `.kt` file in `simulations/{category}/`
2. Register in `RegisteredSimulations.kt`
3. Test: `./gradlew runSim -PsimName="Your Sim Name"`

## Basic Structure

```kotlin
val mySimulation = newSim {
    workspace.clearWorkspace()
    
    // Add components
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    
    // Build network, add neurons, connections...
    
    // Position in GUI
    withGui {
        place(networkComponent, 0, 0, 400, 400)
    }
    
    // Create couplings
    with(couplingManager) {
        neuron couple timeSeries
    }
    
    // Add documentation
    addSidebarInfo("""
        # My Simulation
        Description...
    """)
}
```

## Registration

In `RegisteredSimulations.kt`:

```kotlin
val simulations = dir("Simulations", alphabetical = true) {
    dir("Category Name") {
        item("Menu Item Name") { mySimulation }
    }
}
```

- `dir()` creates submenu
- `item()` creates menu item
- Labels used for menu and command-line
- Avoid duplicate labels

## Finding Templates

Don't memorize - discover existing simulations:

```bash
# See all simulations
read_file: "custom_sims/RegisteredSimulations.kt"
list_dir: "custom_sims/simulations/neuroscience/"

# Find similar patterns
grep: "addOdorWorldComponent" "custom_sims/simulations/"
grep: "SupervisedModel" "custom_sims/simulations/"
```

**Simple templates to study:**
- `neuroscience/SpikingNeuronSim.kt` - Basic network + plots
- `backprop/xorSim.kt` - Supervised learning
- `braitenberg/pursuer.kt` - Embodied agent
- `behaviorism/ClassicalConditioning.kt` - Custom updates

## Common Patterns

**Network + Plots:**
```kotlin
val neuron = network.addNeuron { label = "Output" }
val timeSeries = addTimeSeriesComponent("Activity", "Series")
with(couplingManager) {
    neuron couple timeSeries.model.timeSeriesList[0]
}
```

**Supervised Learning:**
```kotlin
val inputLayer = NeuronGroup(2).apply { isClamped = true }
val outputLayer = NeuronGroup(1).apply { updateRule = SigmoidalRule() }
val sm = SupervisedModel(inputLayer, outputLayer)
sm.trainingSet = TrainingDataset(inputs, targets)
```

**Embodied Agent:**
```kotlin
val odorWorld = addOdorWorldComponent("Environment")
val agent = odorWorld.world.addEntity(100, 100, EntityType.Mouse)
val sensor = agent.addObjectSensor(EntityType.Swiss, 10.0, 0.0, 45.0)
// Couple sensor to network neurons
```

**Custom Updates:**
```kotlin
network.updateManager.addAction(updateAction("Custom Learning") {
    // Runs each iteration
})
```

**Neuron collection layout (spacing and multi-layer subnetworks):**

Changing `betweenNeuronInterval` or other layout spacing after a `NeuronCollection` or subnetwork is built requires more than assigning the property. `setLayoutBasedOnSize()` configures the layout object and lays out neurons once; you still need `applyLayout()` so neuron positions update at the collection's current location.

For subnetworks with multiple layers (`CompetitiveNetwork`, `SOMNetwork`, etc.), also re-run alignment and offset after relayouting every layer. Otherwise layers overlap or drift apart.

```kotlin
collection.betweenNeuronInterval = 30
collection.setLayoutBasedOnSize()
collection.applyLayout()
```

```kotlin
alignNetworkModels(inputLayer, outputLayer, Alignment.VERTICAL)
offsetNeuronCollections(inputLayer, outputLayer, Direction.NORTH, gap)
```

**Kotlin shadowing:** if a local variable has the same name as a subnetwork field (for example `val competitive = CompetitiveNetwork(...)` and the `competitive` neuron group inside it), `competitive.betweenNeuronInterval` inside `apply { }` refers to the network, not the neuron collection. Use `this.competitive`, rename the local variable, or call an extension on the subnetwork type.

Shared helpers and defaults for competitive/SOM menu sims live in `SimulationLayoutUtils.kt` (`applySimulationLayout()`, `SIM_NEURON_INTERVAL`, `COMPETITIVE_LAYER_GAP`). For SOM sims, pass separate `inputNeuronInterval` and `somNeuronInterval` when the map needs more room for labels; layer gap is half the laid-out SOM group height plus `SOM_LAYER_GAP_EXTRA` (100px). Good single-layer examples: `hebb/HopfieldPatterns.kt`, `hebb/DiscreteHopfieldSim.kt`.

**Reopenable (with custom updates/panels):**
```kotlin
val mySim = newSim("unique_id") {
    // Create components
    setupDynamicElements(workspace)
}.registerReopenFunction { workspace ->
    setupDynamicElements(workspace)
}
```

## Component API

**Adding components:**
```kotlin
addNetworkComponent("Name")
addOdorWorldComponent("Name")
addTimeSeriesComponent("Name", "Series")
addProjectionPlot("Name")
addImageWorld("Name")
addDataWorldComponent("Name", rows, cols)
```

**Positioning:**
```kotlin
withGui {
    place(component, x, y, width, height)
    // Get coordinates by arranging manually in GUI
    // Hover over borders to see coordinates
}
```

Use the shared simulation layout helpers in `SimulationLayoutUtils.kt` for desktop component spacing. The standard gap between internal frames is `SIM_WINDOW_GAP` (10px). Prefer formulas using that constant over literal `0`, `5`, or `10` coordinates so future spacing changes are centralized.

```kotlin
withGui {
    place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 400)
    place(plotComponent, SIM_WINDOW_GAP + 400 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 400)
}
```

When a control panel determines the location of another window, pack it before reading its dimensions. `createControlPanel` contents are added on the Swing dispatcher, so use `awaitLayout()` before `rightEdgeWithGap()` or `bottomEdgeWithGap()`.

```kotlin
withGui {
    val controlPanel = createControlPanel("Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
        addButton("Run") { /* ... */ }
    }.awaitLayout()
    place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 600, 600)
}
```

If the left column contains a control panel above a wider network or plot, anchor the right column to the wider of the two. This prevents overlap when the control panel is wider on one platform and prevents oversized gaps when the visual panel is wider.

```kotlin
withGui {
    val controlPanel = createControlPanel("Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
        addButton("Run") { /* ... */ }
    }.awaitLayout()
    val networkWidth = 400
    controlPanel.setLocation(controlPanel.centeredXInColumn(SIM_WINDOW_GAP, networkWidth), SIM_WINDOW_GAP)
    val rightColumnX = max(controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP + networkWidth + SIM_WINDOW_GAP)
    place(networkComponent, SIM_WINDOW_GAP, controlPanel.bottomEdgeWithGap(), networkWidth, 400)
    place(worldComponent, rightColumnX, SIM_WINDOW_GAP, 600, 600)
}
```

For custom internal frames created directly with `GenericJInternalFrame.setBounds(...)`, apply the same `SIM_WINDOW_GAP` formulas as with `place(...)`; those frames are easy to miss in broad layout passes.

For network-internal layouts, remember that `setLocation(x, y)` uses model coordinates and screen elements are centered on those coordinates. If labels or connector widgets overlap, use debug output from the network panel or a UI snapshot to tune model coordinates, then commit the resulting explicit positions. CNN examples often need more clearance than raw layer centers suggest because convolution connectors draw kernel previews and labels at the midpoint of the connector.

### UI Snapshots

The UI snapshot harness can render Swing/Piccolo layouts without manually opening the full application. Snapshot definitions live in `src/snapshots/kotlin/org/simbrain/util/uisnapshot/` and implement `UiSnapshotDef`.

Run a snapshot with:

```bash
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.NetworkPanelSnapshot
```

Useful options:

```bash
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.SomeSnapshot -Pscale=2 -Popen=true
```

For simulation layout work, add a small targeted snapshot definition that builds just the relevant `NetworkPanel`, dialog, or component. Use it to check clearance, label overlap, and frame spacing before asking for manual screenshots. Generated images are written to `build/ui-snapshots/`.

**Couplings:**
```kotlin
with(couplingManager) {
    neuron couple timeSeries.model.timeSeriesList[0]
    sensor.getProducer(ObjectSensor::currentValue) couple 
        neuron.getConsumer(Neuron::addInputValue)
}
```

**Control Panels:**
```kotlin
withGui {
    createControlPanel("Control Panel", x, y) {
        addCheckBox("Label", initialValue) { checked -> /* handler */ }
        addFormattedNumericTextField("Label", initValue) { value -> /* handler */ }
        addSlider("Label", min, max, initial, tickSpacing) { value -> /* handler */ }
        addButton("Label") { /* handler */ }
        addComboBox("Label", options, selected) { value -> /* handler */ }
        addComponent(customSwingComponent)
        addSeparator()
        addLabel("Text")
    }
}
```

Good examples: `braitenberg/Braitenberg.kt`, `demos/view3dDemo.kt`

## Documentation

Simulation documentation may be added with `addSidebarInfo(...)` or a doc viewer when appropriate. Use one coherent documentation block per simulation and follow the standard template below unless there is a clear reason to deviate.

```kotlin
addSidebarInfo("""
    # Simulation Title
    Brief description.
    
    # Simulation Details
    Technical details...
    
    # What to Do
    1. Click `Run`
    2. Observe...
""", width = 300)
```

Supports markdown, LaTeX math, code blocks.

Do not set `initiallyOpened` on `addSidebarInfo(...)` unless a simulation has a specific reason to override the application default. The default comes from `WorkspacePreferences.showSimulationInfoByDefault`, so ordinary simulations should leave the info dock open/closed according to the user's workspace preference.

Good examples to study:
- `psychology/spiveyNet.kt`
- the reservoir simulations

Standard template and intent:

- `# Simulation Name`
  Brief explanation of the simulation. Be clear and concise. Length depends on the simulation: paper-based models may need more context; simpler sims may only need a few sentences.
- `# Simulation Details`
  Technical background and specifics of the simulation. Use `##` subsections when needed.
- `## Control Panel Settings`
  Optional subsection for simulations with control panels. Place it near the top of the documentation, usually under `# Simulation Details`, when the controls are important for interpreting or running the simulation. Briefly explain what each setting changes, or point users to tooltips when the UI already explains the controls well.
- `# What to Do`
  Explain how to run the simulation and what the user should try. Use `##` subsections for demos, experiments, or exploration paths when helpful.
- `# Links`
  Optional. Include links to demos or related material, with one or two sentences explaining what each link shows.
- `# References`
  Optional. Include when relevant. Use APA style, and embed the link in the paper title.
- `# Credits`
  Optional. List contributors one per line, in alphabetical order by name when contribution order is otherwise equal. Embed links to personal sites when available.

Guidance for section content:
- Background can go in the introduction or in a `##` subsection if it is needed to understand the simulation.
- Shared background does not need to be repeated in every simulation. If one simulation in a directory establishes the context, later simulations can refer back to it.
- Not all background belongs in the intro; some background fits better in `# Simulation Details`.

Do **not** add a separate `Architecture`, `Pipeline`, or component-by-component structure section by default. In most simulations the network or processing structure is already visually clear in the workspace, so listing it again is usually redundant.

Only include a structure/architecture explanation if it is genuinely important for operating or interpreting the simulation and is not already obvious from the visual layout. Prefer conceptual descriptions of what to observe over layer-by-layer or component-by-component lists. Implementation details are appropriate when they are the point of the simulation, for example a pure-network implementation of a dynamical system or an analysis that depends on a specific hidden representation. If you think such a section would materially help, explain why and check with the user before adding it.

Style notes:
- When discussing a specific configuration, button, neuron, number, simulation, or section, use inline code.
- Italicize paper titles.
- Use APA in-text citations and references.
- Prefer linking first mentions to relevant docs pages when useful, for example `[ImageWorld](https://docs.simbrain.net/docs/worlds/imageworld.html)`.

Markdown reminders:
- `_word_` or `*word*` for italics
- `__word__` or `**word**` for bold
- `` `word` `` for inline code

### Adding Images to Documentation

Images can be embedded in sidebar documentation using the `//localfiles/` path prefix:

1. Create a subdirectory in `simulations/images/` for your simulation (e.g., `simulations/images/mySimulation/`)
2. Place your image files in this directory
3. Reference images in markdown using either:
   - Standard markdown: `![Alt text](//localfiles/simulations/images/mySimulation/image.png)`
   - HTML with size control: `<img src="//localfiles/simulations/images/mySimulation/image.png" width="400" alt="Alt text" />`

**Example:**

```kotlin
addSidebarInfo("""
    # My Simulation
    
    Here's a diagram showing the structure:
    
    <img src="//localfiles/simulations/images/mySimulation/diagram.png" width="400" alt="Network structure" />
    
    The image above illustrates...
""")
```

Use HTML `<img>` tags with `width` attribute to control display size without modifying the original image files.

See `irisSim.kt` or `CorticalLayers.kt` for complete examples.

## Testing

**From GUI:** Run Simbrain, navigate to `Simulations` menu

**From command line:**
```bash
./gradlew runSim -PsimName="My Simulation Name"

# With options
./gradlew runSim -PsimName="Name" -PoptionString="param1:param2"
```

For headless options, parse `optionString` parameter in `newSim` (see `evolution/CowGrazing.kt`).

## Best Practices

- Start with a template - find similar simulation and copy
- Do not use code comment separators of any kind (for example `// ----- Section -----`, `// --- Section ---`, `// ========`, or `// ── Section ─────────────────`)
- Batch add neurons: `network.addNeurons(List(100) { Neuron() })`
- Get layout coordinates by arranging manually in GUI first
- Wrap GUI code in `withGui { }` for headless compatibility
- Always call `workspace.clearWorkspace()` at start
- Test save/reopen if using custom updates or control panels
- Control panels should not include a title label or top separator — the panel title is set via `createControlPanel("Title", ...)` and shown in the window frame
- Use `showMessageDialog(text, title)` from `org.simbrain.util` to display analysis results rather than printing to console or embedding text in the control panel

## Custom Overlays

Simulations can add custom graphics overlays to component canvases using Piccolo2D's `PNode`. This allows flexible visualization of simulation-specific data like learned weights, activation patterns, or annotations.

**OdorWorld overlay example** (from `rl/actorCritic.kt`):
```kotlin
val overlay = object : PNode() {
    override fun paint(paintContext: PPaintContext) {
        val graphics = paintContext.graphics
        // Custom drawing code using graphics
    }
}.apply {
    pickable = false
    setBounds(0.0, 0.0, world.width, world.height)
}

withGui {
    (getDesktopComponent(odorWorldComponent) as OdorWorldDesktopComponent).apply {
        worldPanel.canvas.layer.addChild(world.tileMap.layers.size, overlay)
    }
}
```

**Network overlay** (similar pattern):
```kotlin
withGui {
    val networkPanel = getNetworkPanel(networkComponent)
    networkPanel.canvas.layer.addChild(overlay)
}
```

The overlay's `paint` method is called each time the canvas repaints, allowing dynamic visualization that updates with the simulation.

## Tips

**Image Worlds:** Album starts with blank canvas at index 0, delete if unwanted
**Odor Worlds:** `place()` positions window not world; set world size separately
**Performance:** Use batch operations for many neurons (see `spikingNetworkSimulation.kt`)

## Verification Checklist

- [ ] File in `simulations/{category}/` directory
- [ ] Registered in `RegisteredSimulations.kt` with import
- [ ] Runs from command line: `./gradlew runSim -PsimName="Label"`
- [ ] Documentation included (sidebar or doc viewer)

## Resources

- [Simulations Documentation](https://docs.simbrain.net/docs/simulations/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- Example simulations: `neuroscience/SpikingNeuronSim.kt`, `backprop/xorSim.kt`, `braitenberg/pursuer.kt`
