/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.workspace.gui

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.pmw.tinylog.Logger
import org.simbrain.network.NetworkComponent
import org.simbrain.util.SFileChooser
import org.simbrain.util.SimbrainPreferences
import org.simbrain.util.Utils
import org.simbrain.util.createAction
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop.createDesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.getDesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.registerComponentInstance
import org.simbrain.workspace.gui.SimbrainDesktop.workspace
import org.simbrain.workspace.serialization.WorkspaceComponentDeserializer
import org.simbrain.world.dataworld.DataWorldComponent
import org.simbrain.world.odorworld.OdorWorldComponent
import java.awt.Dimension
import java.awt.Rectangle
import java.beans.PropertyVetoException
import java.io.*
import javax.swing.JOptionPane
import javax.swing.JPanel

/**
 * A gui view on a [org.simbrain.workspace.WorkspaceComponent].
 *
 * All desktop component graphical updates should happen via events.
 *
 * For custom size overrides [.getPreferredSize]
 *
 * @param <E> the type of the workspace component.
</E> */
abstract class DesktopComponent<E : WorkspaceComponent>(
    /**
     * Reference to parent frame.
     */
    @JvmField var parentFrame: GenericFrame, workspaceComponent: E
) : JPanel() {

    /**
     * Default size for new components.
     */
    private val DEFAULT_SIZE = Dimension(500, 500)

    var workspaceComponent: E
        private set

    private val chooser: SFileChooser

    @JvmField
    var desktop: SimbrainDesktop? = null

    val exportAction = createAction(
        iconPath = "menu_icons/Save.png",
        name = "Export to xml...",
        description = "Export component to .xml",
        coroutineScope = workspace
    ) {
        showExportDialog()
    }

    val importAction = createAction(
        iconPath = "menu_icons/Open.png",
        name = "Import from xml...",
        description = "Import component from .xml",
        coroutineScope = workspace
    ) {
        showImportDialog()
    }

    val closeAction = createAction(
        name = "Close component",
        coroutineScope = workspace
    ) {
        close()
    }

    /**
     * Construct a workspace component.
     *
     * @param frame              the parent frame.
     * @param workspaceComponent the component to wrap.
     */
    init {
        this.workspaceComponent = workspaceComponent
        val defaultDirectory = getDefaultDirectory(workspaceComponent::class.java)
        chooser = SFileChooser(defaultDirectory, null)
        for (format in workspaceComponent.formats) {
            chooser.addExtension(format)
        }

        // Add a default update listener
        val events = workspaceComponent.events
        events.guiToggled.on { parentFrame.setVisible(workspaceComponent.isGuiOn) }
        events.componentClosing.on { close() }
        events.componentMinimized.on { minimized ->
            try {
                parentFrame.setIcon(minimized)
            } catch (e: PropertyVetoException) {
                throw RuntimeException(e)
            }
        }
        Logger.trace(this.javaClass.canonicalName + " created")
    }

    fun close() {
        if (workspaceComponent.hasChangedSinceLastSave()) {
            val hasCancelled = showHasChangedDialog()
            if (hasCancelled) {
                return
            }
        }
        workspaceComponent.close()
    }

    /**
     * Dialog for importing a workspace component.
     */
    suspend fun showImportDialog() {
        val chooser = SFileChooser(getDefaultDirectory(workspaceComponent.javaClass), null)
        for (format in workspaceComponent.formats) {
            chooser.addExtension(format)
        }
        val file = chooser.showOpenDialog()
        val dir = file.parentFile.absolutePath
        val name = file.name
        val ext = SFileChooser.getExtension(file)
        val inputStream: FileInputStream = try {
            withContext(Dispatchers.IO) {
                FileInputStream(file)
            }
        } catch (ex: FileNotFoundException) {
            JOptionPane.showMessageDialog(null, String.format("File %s was not found.", file))
            return
        }
        val newComponent: E = try {
            WorkspaceComponentDeserializer.deserializeWorkspaceComponent(
                workspaceComponent::class.java,
                name,
                inputStream,
                ext
            ) as E
        } catch (ex: ReflectiveOperationException) {
            val message = String.format(
                "Failed to deserialize workspace component %s\nCould not execute open method in class %s.",
                name,
                workspaceComponent.javaClass.simpleName
            )
            JOptionPane.showMessageDialog(null, message)
            ex.printStackTrace()
            return
        }
        val bounds = parentFrame.bounds
        val workspace = workspaceComponent.workspace
        workspace.removeWorkspaceComponent(workspaceComponent)
        workspaceComponent = newComponent
        workspace.addWorkspaceComponent(workspaceComponent)
        workspaceComponent.currentFile = file
        setDefaultDirectory(workspaceComponent.javaClass, dir)
        val desktopComponent = getDesktopComponent(workspaceComponent)
        registerComponentInstance(workspaceComponent, desktopComponent)
        desktopComponent.parentFrame.bounds = bounds
        workspaceComponent.name = name
        parentFrame.title = name
    }

    /**
     * Dialog for explorting a workspace component to xml.
     */
    open fun showExportDialog() {
        var theFile = workspaceComponent.currentFile
        if (theFile == null) {
            theFile = File(name)
        }
        theFile = chooser.showSaveDialog(theFile)
        if (theFile != null) {
            workspaceComponent.currentFile = theFile
            try {
                val stream = FileOutputStream(theFile)
                // TODO format?
                workspaceComponent.save(stream, null)
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }

            // workspaceComponent.setCurrentDirectory(theFile.getParentFile()
            // .getAbsolutePath());
            setDefaultDirectory(workspaceComponent.javaClass, theFile.parentFile.absolutePath)
            workspaceComponent.name = theFile.name
            parentFrame.title = workspaceComponent.name
        }
    }

    /**
     * Save vs. save-as. Saves the currentfile.
     */
    open fun save() {
        // System.out.println("Network save:" +
        // workspaceComponent.getCurrentFile());
        if (workspaceComponent.currentFile == null) {
            showExportDialog()
        } else {
            try {
                val stream = FileOutputStream(workspaceComponent.currentFile)
                workspaceComponent.save(stream, null)
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }

    /**
     * Writes the bounds of this desktop component to the provided stream.
     *
     * @param ostream the stream to write to
     * @throws IOException if an IO error occurs
     */
    @Throws(IOException::class)
    fun save(ostream: OutputStream?) {
        XStream(DomDriver()).toXML(parentFrame.bounds, ostream)
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     *
     * @return true if user cancels
     */
    fun showHasChangedDialog(): Boolean {
        val options = arrayOf<Any>("Save", "Don't Save", "Cancel")
        val s = JOptionPane.showInternalOptionDialog(
            this,
            """
     This component has changed since last save,
     Would you like to save these changes?
     """.trimIndent(),
            "Component Has Changed",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]
        )
        if (s == JOptionPane.OK_OPTION) {
            this.save()
            workspaceComponent.close()
            return false
        } else if (s == JOptionPane.NO_OPTION) {
            workspaceComponent.close()
            return false
        } else if (s == JOptionPane.CANCEL_OPTION) {
            return true
        }
        return false
    }

    /**
     * Return name of underlying component.
     *
     * @return the name of underlying component.
     */
    override fun getName(): String {
        return if (workspaceComponent == null) "null" else workspaceComponent.name
    }

    var title: String?
        get() = parentFrame.title
        set(name) {
            parentFrame.title = name
        }

    val simpleName: String
        /**
         * Retrieves a simple version of a component name from its class, e.g.
         * "Network" from "org.simbrain.network.NetworkComponent"/
         *
         * @return the simple name.
         */
        get() {
            var simpleName = javaClass.simpleName
            if (simpleName.endsWith("Component")) {
                simpleName = simpleName.replaceFirst("Component".toRegex(), "")
            }
            return simpleName
        }

    /**
     * Returns the default directory for specific component types.
     *
     * @param componentType the component type
     * @return the directory
     */
    private fun getDefaultDirectory(
        componentType: Class<out WorkspaceComponent?>
    ): String {
        val defaultDirectory: String = if (componentType == OdorWorldComponent::class.java) {
            Utils.USER_DIR + Utils.FS + "simulations" + Utils.FS + "worlds"
        } else if (componentType == DataWorldComponent::class.java) {
            Utils.USER_DIR + Utils.FS + "simulations" + Utils.FS + "tables"
        } else if (componentType == NetworkComponent::class.java) {
            Utils.USER_DIR + Utils.FS + "simulations" + Utils.FS + "networks"
        } else {
            Utils.USER_DIR + Utils.FS + "simulations"
        }
        return defaultDirectory
    }

    /**
     * Set the default directory for specific component types.
     *
     * @param componentType the component type
     * @param dir           the directory to set
     */
    private fun setDefaultDirectory(componentType: Class<out WorkspaceComponent>, dir: String) {
        if (componentType == OdorWorldComponent::class.java) {
            SimbrainPreferences.putString("workspaceOdorWorldDirectory", dir)
        } else if (componentType == DataWorldComponent::class.java) {
            SimbrainPreferences.putString("workspaceTableDirectory", dir)
        } else if (componentType == NetworkComponent::class.java) {
            SimbrainPreferences.putString("workspaceNetworkDirectory", dir)
        } else {
            SimbrainPreferences.putString("workspaceBaseDirectory", dir)
        }
    }

    override fun getPreferredSize(): Dimension {
        return DEFAULT_SIZE
    }

    companion object {
        /**
         * Creates a new desktop component from the provided stream.
         *
         * @param component the component to create the desktop component for.
         * @param istream   the inputstream containing the serialized data.
         * @param name      the name of the desktop component.
         * @return a new component.
         */
        fun open(component: WorkspaceComponent?, istream: InputStream?, name: String?): DesktopComponent<*> {
            // SimbrainDesktop desktop =
            // SimbrainDesktop.getDesktop(component.getWorkspace());
            val dc = createDesktopComponent(null, component!!)
            val bounds = XStream(DomDriver()).fromXML(istream) as Rectangle
            dc.title = name
            dc.bounds = bounds
            return dc
        }
    }
}