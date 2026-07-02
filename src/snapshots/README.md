# UI Snapshots

Off-screen Swing renderer for Simbrain UI screenshots. It builds real dialogs and panels, paints them with FlatLaf, and writes PNGs to `build/ui-snapshots/`.

The snapshot classes support two main workflows:

- Automated or manual UI testing, including visual regression checks and repeatable inspection of specific UI states.
- Generating publication-quality images for documentation, presentations, and papers without running the full desktop app.

The default behavior is non-interactive, which is appropriate for tests and automation. Add `-Popen=true` when working interactively to open the generated PNG in the system image viewer.

## Quick start

From the `simbrain/` directory:

```bash
./gradlew uiSnapshot \
  -PsnapshotDef=org.simbrain.util.uisnapshot.AboutDialogSnapshot \
  -Popen=true
```

This writes `simbrain/build/ui-snapshots/about_dialog.png` and opens it. Omit `-Popen=true` in tests, CI, or batch snapshot generation.

## Gradle options

| Property | Default | Description |
|----------|---------|-------------|
| `snapshotDef` | *(required)* | Fully qualified class name of a `UiSnapshotDef` implementation |
| `theme` | `light` | `light` or `dark` |
| `scale` | `1` | Output scale factor (1–4). `2` doubles pixel dimensions for sharper docs images |
| `open` | `false` | Set to `true` to open the generated PNG in the system image viewer |

Examples:

```bash
# Dark theme
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.ThemeWidgetsSnapshot -Ptheme=dark

# 2× resolution (writes about_dialog@2x.png)
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.AboutDialogSnapshot -Pscale=2

# 2× resolution and open the result for inspection
./gradlew uiSnapshot \
  -PsnapshotDef=org.simbrain.util.uisnapshot.IzhikevichRuleNoiseTabSnapshot \
  -Ptheme=light \
  -Pscale=2 \
  -Popen=true
```

At `scale=2`, the harness paints the component at its normal layout size but writes an image twice as wide and tall. Use `-Pscale=2` or `-Pscale=3` for documentation; default `1` matches on-screen pixel size.

`-Popen=true` is intended for local inspection. It requires a graphical desktop and should normally be omitted from automated tests and headless environments.

## Creating a snapshot

1. Add a class in this package implementing `UiSnapshotDef`.
2. Give it a no-arg constructor (required by the harness).
3. Set `name` — used as the output filename (without `.png`).
4. Implement `build()` to return the component to capture.

`build()` runs **off** the EDT, so you can use `runBlocking { ... }` for suspend network setup. Use `SwingUtilities.invokeAndWait { ... }` when you need realized layout (pack, zoom-to-fit, etc.).

Return either:

- A **`Window`** (`JDialog`, `JFrame`, `StandardDialog`) — the harness snapshots the full root pane, including OK/Cancel bars and menu bars.
- A **`JComponent`** — sized to preferred size and painted directly.

The harness applies the requested theme, re-applies it after `build()` (app code may reset L&F), drains pending EDT work, then paints.

### Property editor only

For a bare parameter panel (no dialog chrome):

```kotlin
class MyRuleEditorSnapshot : UiSnapshotDef {
    override val name = "my_rule_editor"
    override fun build(): Component = AnnotatedPropertyEditor(IzhikevichRule())
}
```

### Tabs and collapsed sections

```kotlin
override fun build(): Component = AnnotatedPropertyEditor(IzhikevichRule()).also {
    it.selectTab("Noise")
    it.expandDetailPanels()
}
```

`expandDetailPanels()` opens every "Settings ▼" disclosure in APE. `selectTab(title)` finds the first matching tab in a nested `JTabbedPane`.

### Full dialog (StandardDialog / NeuronDialog)

Configure model objects first, then wrap in the real dialog class:

```kotlin
class NeuronDialogIzhikevichSnapshot : UiSnapshotDef {
    override val name = "neuron_dialog_izhikevich"

    override fun build(): Component {
        val neuron = Neuron(IzhikevichRule().apply {
            a = 0.02; b = 0.2; c = -65.0; d = 8.0
        }).apply {
            label = "N1"
            bias = 0.5
        }
        return NeuronDialog(listOf(neuron))
    }
}
```

Settings live on the **model** (`Neuron`, update rules, etc.) before the editor is built. The property editor reads those values when it renders.

### Panels and canvases

Build a minimal model, attach a panel, set `preferredSize` if needed:

```kotlin
override fun build(): Component {
    val network = Network()
    val component = NetworkComponent("snapshot", network)
    val panel = NetworkPanel(component).apply {
        preferredSize = Dimension(600, 400)
    }
    runBlocking { /* add neurons, synapses, ... */ }
    SwingUtilities.invokeAndWait {
        JDialog().apply { contentPane = panel; pack() }
        network.events.zoomToFitPage.fire()
    }
    return panel
}
```

See `NetworkPanelSnapshot` for a working example.

### Dialogs that need app context

Some dialogs expect a `NetworkPanel`, desktop component, or pre-built model. Build the smallest valid setup, then call the same factory the app uses:

```kotlin
// CNN trainer — see CnnTrainerDialogSnapshot
dialog = with(panel) { cnn.getCnnTrainingDialog() }

// Image pipeline — see ImagePipelineDialogSnapshot
ImageProcessingPipelineDialog(desktopComponent, pipeline)
```

## Existing snapshots

All classes live in `org.simbrain.util.uisnapshot`. Run any with `-PsnapshotDef=org.simbrain.util.uisnapshot.<ClassName>`.

### Dialogs

```bash
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.AboutDialogSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.StandardDialogSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.TextDialogSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.UndoHistoryDialogSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.WorkspacePreferencesDialogSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.CnnTrainerDialogSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.ImagePipelineDialogSnapshot
```

### Property editors

```bash
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.IzhikevichRuleEditorSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.IzhikevichRuleNoiseTabSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.GaborFilterEditorSnapshot
```

### Network UI

```bash
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.NetworkPanelSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.NetworkToolbarSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.NetworkToolbarWandActiveSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.SynapseAdjustmentPanelSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.WandPalettePanelSnapshot
```

### Chrome, theme, and misc

```bash
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.ThemeWidgetsSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.ChartThemeSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.IconGallerySnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.RoundedInternalFrameSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.RoundedInternalFrameActiveSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.ControlPanelRoundedSnapshot
./gradlew uiSnapshot -PsnapshotDef=org.simbrain.util.uisnapshot.TablePanelSnapshot
```

### Output filenames

| Class | Output (`name`) |
|-------|-------------------|
| `AboutDialogSnapshot` | `about_dialog.png` |
| `ChartThemeSnapshot` | `chart_theme.png` |
| `CnnTrainerDialogSnapshot` | `cnn_trainer_dialog.png` |
| `ControlPanelRoundedSnapshot` | `rounded_control_panel.png` |
| `GaborFilterEditorSnapshot` | `gabor_filter_editor.png` |
| `IconGallerySnapshot` | `icon_gallery.png` |
| `ImagePipelineDialogSnapshot` | `image_pipeline_dialog.png` |
| `IzhikevichRuleEditorSnapshot` | `izhikevich_rule_editor.png` |
| `IzhikevichRuleNoiseTabSnapshot` | `izhikevich_rule_noise_tab.png` |
| `NetworkPanelSnapshot` | `network_panel.png` |
| `NetworkToolbarSnapshot` | `network_toolbar.png` |
| `NetworkToolbarWandActiveSnapshot` | `network_toolbar_wand_active.png` |
| `RoundedInternalFrameSnapshot` | `rounded_internal_frame.png` |
| `RoundedInternalFrameActiveSnapshot` | `rounded_internal_frame_active.png` |
| `StandardDialogSnapshot` | `standard_dialog.png` |
| `SynapseAdjustmentPanelSnapshot` | `synapse_adjustment_panel.png` |
| `TablePanelSnapshot` | `table_panel.png` |
| `TextDialogSnapshot` | `text_dialog.png` |
| `ThemeWidgetsSnapshot` | `theme_widgets.png` |
| `UndoHistoryDialogSnapshot` | `undo_history_dialog.png` |
| `WandPalettePanelSnapshot` | `wand_palette_panel.png` |
| `WorkspacePreferencesDialogSnapshot` | `workspace_preferences_dialog.png` |

With `-Pscale=2`, the suffix `@2x` is appended (e.g. `about_dialog@2x.png`).

## Source layout

- Snapshot definitions: `src/snapshots/kotlin/org/simbrain/util/uisnapshot/*Snapshot.kt`
- Harness entry point: `UiSnapshot.kt` (`main`)
- Gradle task: `uiSnapshot` in `build.gradle.kts`
- Source set: `snapshots` (compile classpath includes `main`)

There is no CLI for arbitrary settings — each shot is a dedicated class (or you extend the harness to read env/system properties). For a one-off doc image, add a snapshot class, run the task, and commit the PNG or copy it wherever you need it.
