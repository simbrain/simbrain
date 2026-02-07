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

**Couplings:**
```kotlin
with(couplingManager) {
    neuron couple timeSeries.model.timeSeriesList[0]
    sensor.getProducer(ObjectSensor::currentValue) couple 
        neuron.getConsumer(Neuron::addInputValue)
}
```

## Documentation

```kotlin
addSidebarInfo("""
    # Simulation Title
    Brief description.
    
    # What to Do
    1. Click Run
    2. Observe...
    
    # Details
    Explanation...
""", width = 300, initiallyOpened = true)
```

Supports markdown, LaTeX math, code blocks.

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
- Batch add neurons: `network.addNeurons(List(100) { Neuron() })`
- Get layout coordinates by arranging manually in GUI first
- Wrap GUI code in `withGui { }` for headless compatibility
- Always call `workspace.clearWorkspace()` at start
- Test save/reopen if using custom updates or control panels

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
