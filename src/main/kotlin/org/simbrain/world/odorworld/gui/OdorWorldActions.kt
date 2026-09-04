/**
 * Holder for the odor world's user actions. Each action is created once and shared by the menu bar, toolbar and
 * context menus, so keyboard shortcuts are registered a single time and enabled state stays consistent everywhere.
 * Selection-dependent actions re-evaluate their enabled state on selection and entity add/remove events.
 */
package org.simbrain.world.odorworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.util.*
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.OdorWorldPreferences
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.layerEditor
import org.simbrain.world.odorworld.showTilePicker
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JRadioButtonMenuItem

class OdorWorldActions(val odorWorldPanel: OdorWorldPanel) {

    val selectAllAction = odorWorldPanel.createConditionallyEnabledAction(
        name = "Select all",
        description = "Select all entities (Cmd/Ctrl-A)",
        keyboardShortcuts = listOf(CmdOrCtrl + 'A'),
        enablingCondition = { world.entityList.isNotEmpty() }
    ) {
        selectionManager.addAll(canvas.layer.allNodes.filterIsInstance<EntityNode>().toMutableSet())
    }

    val addAgentAction = odorWorldPanel.createAction(
        name = "Add agent",
        description = "Add a new agent to the world. A mouse by default. Double click to edit (Cmd/Ctrl-P)",
        iconPath = "menu_icons/mouse_icon.png",
        keyboardShortcut = CmdOrCtrl + 'P'
    ) {
        world.addAgent()
    }

    val addEntityAction = odorWorldPanel.createAction(
        name = "Add entity",
        description = "Add a new entity to the world. Cheese by default. Double click to edit (p)",
        iconPath = "menu_icons/swiss_icon.png",
        keyboardShortcut = 'P'
    ) {
        world.addEntity()
    }

    val deleteSelectedAction = odorWorldPanel.createConditionallyEnabledAction(
        name = "Delete selected entities",
        description = "Delete selected entities (Delete or Backspace)",
        iconPath = "menu_icons/minus.png",
        keyboardShortcuts = listOf(KeyCombination(KeyEvent.VK_DELETE), KeyCombination(KeyEvent.VK_BACK_SPACE)),
        enablingCondition = { selectedEntityNodes.isNotEmpty() }
    ) {
        deleteSelectedEntities()
    }

    val editEntityAction = odorWorldPanel.createConditionallyEnabledAction(
        name = "Edit entity...",
        description = "Edit the first selected entity (Cmd/Ctrl-E)",
        iconPath = "menu_icons/Properties.png",
        keyboardShortcuts = listOf(CmdOrCtrl + 'E'),
        enablingCondition = { selectedEntityNodes.isNotEmpty() }
    ) {
        editSelectedEntities()
    }

    val showWorldPropertiesAction = odorWorldPanel.createAction(
        name = "Properties...",
        description = "Show odor world properties (Cmd/Ctrl-,)",
        iconPath = "menu_icons/Tools.png",
        keyboardShortcut = CmdOrCtrl + ','
    ) {
        world.createEditorDialog(titleName = "World Properties") { it.events.propertiesChanged.fire() }.display()
    }

    val loadTileMapAction = odorWorldPanel.createAction(
        name = "Load tile map...",
        description = "Replace the current tile map with one loaded from a Tiled (tmx) file"
    ) {
        val chooser = SFileChooser(OdorWorldPreferences.tileMapDirectory, "Load TMX Tilemap", null, true)
        chooser.addExtension("tmx")
        chooser.showOpenDialog()?.let { file ->
            world.tileMap = loadTileMap(file)
            OdorWorldPreferences.tileMapDirectory = chooser.currentLocation!!
        }
    }

    val addTileAction = odorWorldPanel.createAction(
        name = "Add tile...",
        description = "Set the tile at the last clicked position on the selected layer"
    ) {
        showTilePicker(world.tileMap.tileSets) {
            val (x, y) = world.tileMap.pixelToGridCoordinate(world.lastClickedPosition)
            world.tileMap.setTile(x, y, it, world.selectedLayer)
        }
    }

    val fillLayerAction = odorWorldPanel.createAction(
        name = "Fill layer...",
        description = "Fill every cell of the selected layer with one tile"
    ) {
        showTilePicker(world.tileMap.tileSets) {
            world.tileMap.fill(it, world.selectedLayer)
        }
    }

    /**
     * Submenu of radio items for choosing the layer that tile editing acts on. Rebuilt each time it opens so it
     * reflects layers added or removed in the layer editor.
     */
    fun createChooseLayerMenu() = JMenu("Choose layer").apply {
        fun populate() {
            removeAll()
            val world = odorWorldPanel.world
            world.tileMap.layers.forEach { layer ->
                add(JRadioButtonMenuItem(layer.name).apply {
                    isSelected = layer == world.selectedLayer
                    addActionListener { world.selectedLayer = layer }
                })
            }
        }
        populate()
        onMenuSelected { populate() }
    }

    val editLayersAction = odorWorldPanel.createAction(
        name = "Edit layers...",
        description = "Add, remove, rename and reorder tile map layers"
    ) {
        world.layerEditor().display()
    }

    val clearTileMapAction = odorWorldPanel.createAction(
        name = "Clear tile map...",
        description = "Replace the tile map with an empty one of the given size in tiles"
    ) {
        val size = object : EditableObject {
            var width by GuiEditable(initValue = canvas.camera.width.toInt() / world.tileMap.tileWidth)
            var height by GuiEditable(initValue = canvas.camera.height.toInt() / world.tileMap.tileHeight)
        }
        AnnotatedPropertyEditor(size).displayInDialog {
            world.tileMap.updateMapSize(size.width, size.height)
        }
    }

    val clearAllTrailsAction = odorWorldPanel.createAction(
        name = "Clear all trails",
        description = "Erase the trail behind every entity"
    ) {
        world.entityList.forEach { it.clearTrail() }
    }

    /**
     * True if at least one entity is showing its trail; the "Show trails" checkbox reflects this.
     */
    val anyTrailShown get() = odorWorldPanel.world.entityList.any { it.isShowTrail }

    /**
     * Shows or hides trails on every entity. From a checkbox menu item the checkbox state wins; from the keyboard
     * shortcut it toggles.
     */
    val showAllTrailsAction = odorWorldPanel.createAction(
        name = "Show trails",
        description = "Show or hide the trail behind every entity (t)",
        keyboardShortcut = 'T'
    ) { e ->
        val show = (e?.source as? JCheckBoxMenuItem)?.isSelected ?: !anyTrailShown
        world.entityList.forEach { it.isShowTrail = show }
    }

    fun createShowTrailMenuItem(entity: OdorWorldEntity) = JCheckBoxMenuItem(
        odorWorldPanel.createAction(name = "Show trail", description = "Show the trail behind ${entity.name}") { e ->
            entity.isShowTrail = (e?.source as? JCheckBoxMenuItem)?.isSelected ?: !entity.isShowTrail
        }
    ).apply { isSelected = entity.isShowTrail }

    val resetZoomAction = odorWorldPanel.createAction(
        name = "Reset zoom",
        description = "Reset the zoom so entities appear at their original size in pixels (Cmd/Ctrl-0)",
        iconPath = "menu_icons/ZoomReset.png",
        keyboardShortcut = CmdOrCtrl + KeyEvent.VK_0
    ) {
        scalingFactor = 1.0
    }

    val zoomInAction = odorWorldPanel.createAction(
        name = "Zoom in",
        description = "Zoom in (Cmd/Ctrl-+ or Cmd/Ctrl-=)",
        iconPath = "menu_icons/ZoomIn.png",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_ADD, CmdOrCtrl + KeyEvent.VK_EQUALS)
    ) {
        scalingFactor *= 1.1
    }

    val zoomOutAction = odorWorldPanel.createAction(
        name = "Zoom out",
        description = "Zoom out (Cmd/Ctrl-- or Cmd/Ctrl-_)",
        iconPath = "menu_icons/ZoomOut.png",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_SUBTRACT, CmdOrCtrl + KeyEvent.VK_MINUS)
    ) {
        scalingFactor /= 1.1
    }

    val zoomToFitAction = odorWorldPanel.createAction(
        name = "Zoom to fit",
        description = "Zoom so the whole world is visible (Cmd/Ctrl-Shift-0)",
        iconPath = "menu_icons/ZoomFitPage.png",
        keyboardShortcut = CmdOrCtrl + KeyEvent.VK_0 + Shift
    ) {
        zoomToFit()
    }

    val showToolbarAction = odorWorldPanel.createAction(
        name = "Toolbar",
        description = "Show or hide the toolbar"
    ) { e ->
        mainToolBar.isVisible = (e?.source as? JCheckBoxMenuItem)?.isSelected ?: !mainToolBar.isVisible
    }

}

/**
 * [createAction] whose enabled state follows [enablingCondition], re-evaluated whenever the selection changes or an
 * entity is added to or removed from the world.
 */
private fun OdorWorldPanel.createConditionallyEnabledAction(
    name: String,
    description: String = name,
    iconPath: String? = null,
    keyboardShortcuts: List<KeyCombination> = listOf(),
    enablingCondition: OdorWorldPanel.() -> Boolean,
    block: suspend OdorWorldPanel.(e: ActionEvent) -> Unit
) = createAction(
    name = name,
    description = description,
    iconPath = iconPath,
    keyboardShortcuts = keyboardShortcuts,
    initBlock = {
        fun updateEnabled() {
            isEnabled = enablingCondition()
        }
        updateEnabled()
        selectionManager.events.selection.on(Dispatchers.Swing) { _, _ -> updateEnabled() }
        world.events.entityAdded.on(Dispatchers.Swing) { updateEnabled() }
        world.events.entityRemoved.on(Dispatchers.Swing) { updateEnabled() }
    },
    block = block
)
