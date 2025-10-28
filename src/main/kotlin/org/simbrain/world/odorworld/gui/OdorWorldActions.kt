package org.simbrain.world.odorworld.gui

import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.OdorWorldPanel
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.layerEditor
import org.simbrain.world.odorworld.showTilePicker
import java.awt.event.KeyEvent
import javax.swing.JMenu
import javax.swing.JMenuItem

class OdorWorldActions(val odorWorldPanel: OdorWorldPanel) {

    fun createSelectAllAction() = odorWorldPanel.createAction(
        name = "Select all",
        description = "Select all entities (Cmd/Ctrl-A)",
        keyboardShortcut = CmdOrCtrl + 'A'
    ) {
        odorWorldPanel.selectionManager.addAll(odorWorldPanel.canvas.layer.allNodes.filterIsInstance<EntityNode>().toMutableSet())
    }

    fun addAgentAction() = odorWorldPanel.createAction(
        name = "Add agent",
        description = "Add a new agent to the world. A mouse by default. Double click to edit (Cmd/Ctrl-P)",
        iconPath = "menu_icons/mouse_icon.png",
        keyboardShortcut = CmdOrCtrl + 'P'
    ) {
        world.addAgent()
    }

    fun addEntityAction() = odorWorldPanel.createAction(
        name = "Add entity",
        description = "Add a new entity to the world. Cheese by default. Double click to edit (p)",
        iconPath = "menu_icons/swiss_icon.png",
        keyboardShortcut = 'P'
    ) {
        world.addEntity()
    }

    fun deleteSelectedAction() = odorWorldPanel.createAction(
        name = "Delete selected entities",
        description = "Delete selected entities (Delete or Backspace)",
        keyboardShortcuts = listOf(KeyCombination(KeyEvent.VK_DELETE), KeyCombination(KeyEvent.VK_BACK_SPACE))
    ) {
        odorWorldPanel.deleteSelectedEntities()
    }

    fun showWorldPrefsAction() = odorWorldPanel.createAction(
        name = "Preferences...",
        description = "Show odor world preferences (Cmd/Ctrl-,)",
        iconPath = "menu_icons/Tools.png",
        keyboardShortcut = CmdOrCtrl + ','
    ) {
        world.createEditorDialog().apply { title = "World Preferences" }.display()
    }

    val showPropertyDialogAction = odorWorldPanel.createAction(
        name = "Edit entity...",
        description = "Edit selected entity (Cmd/Ctrl-E)",
        iconPath = "menu_icons/Properties.png",
        keyboardShortcut = CmdOrCtrl + 'E'
    ) {
        odorWorldPanel.editSelectedEntities()
    }

    // TODO: Add images and to toolbar
    val addTileAction
        get() = odorWorldPanel.createAction("Add tile to ${odorWorldPanel.world.selectedLayer.name}...") {
            showTilePicker(world.tileMap.tileSets) {
                val (x,y) = world.tileMap.pixelToGridCoordinate(world.lastClickedPosition)
                world.tileMap.setTile(x, y, it, world.selectedLayer)
            }
        }

    val fillLayerAction
        get() = odorWorldPanel.createAction("Fill layer ${odorWorldPanel.world.selectedLayer.name}...") {
            showTilePicker(world.tileMap.tileSets) {
                world.tileMap.fill(it, world.selectedLayer)
            }
        }

    fun createChooseLayerMenu(world: OdorWorld) = JMenu("Choose layer").apply {
        world.tileMap.layers.forEach { layer ->
            add(JMenuItem(layer.name).apply {
                addActionListener {
                    world.selectedLayer = layer
                }
            })
        }
    }

    val editLayersAction
        get() = odorWorldPanel.createAction("Edit layers...") {
            world.layerEditor().display()
        }

    val clearAllTrails = odorWorldPanel.createAction("Clear all trails") {
        world.entityList.map {
            it.clearTrail()
        }
    }

    val toggleAllTrails = odorWorldPanel.createAction(name = "Toggle all trails", description = "Toggle all trails (t)", keyboardShortcut = 'T') {
        val firstEntityHasTrail = world.entityList.firstOrNull()?.isShowTrail ?: false
        world.entityList.map {
            it.isShowTrail = !firstEntityHasTrail
        }
    }

    val turnOffTrails = odorWorldPanel.createAction("Turn off trails") {
        world.entityList.map {
            it.isShowTrail = false
        }
    }

    @JvmOverloads
    fun toggleTrailAction(entity: OdorWorldEntity) = odorWorldPanel.createAction("Toggle show trails") {
        entity.isShowTrail = !entity.isShowTrail
    }

    fun resetZoomAction() = odorWorldPanel.createAction(
        "Reset zoom",
        description = "Resetting the zoom. Entities will appear at their original size (in pixels) (Cmd/Ctrl-0)",
        iconPath = "menu_icons/ZoomReset.png",
        keyboardShortcut = CmdOrCtrl + KeyEvent.VK_0
    ) {
        scalingFactor = 1.0
    }

    fun zoomInAction() = odorWorldPanel.createAction(
        "Zoom in",
        description = "Zoom in (Cmd/Ctrl-+ or Cmd/Ctrl-=)",
        iconPath = "menu_icons/ZoomIn.png",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_ADD, CmdOrCtrl + KeyEvent.VK_EQUALS)
    ) {
        scalingFactor *= 1.1
    }

    fun zoomOutAction() = odorWorldPanel.createAction(
        "Zoom out",
        description = "Zoom out (Cmd/Ctrl-- or Cmd/Ctrl-_)",
        iconPath = "menu_icons/ZoomOut.png",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_SUBTRACT, CmdOrCtrl + KeyEvent.VK_MINUS)
    ) {
        scalingFactor /= 1.1
    }

    fun clearTileMapAction() = odorWorldPanel.createAction("Clear tile map...") {
        val size = object : EditableObject {
            var width by GuiEditable(
                initValue = odorWorldPanel.canvas.camera.width.toInt() / world.tileMap.tileWidth
            )

            var height by GuiEditable(
                initValue = odorWorldPanel.canvas.camera.height.toInt() / world.tileMap.tileHeight
            )
        }

        val editorPanel = AnnotatedPropertyEditor(size)

        editorPanel.displayInDialog {
            world.tileMap.updateMapSize(size.width, size.height)
        }

    }

}