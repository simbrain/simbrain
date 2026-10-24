/**
 * A desktop component's view scope, and the scope of a panel tied to it, end when the component closes, taking the
 * view's event subscriptions with them.
 */
package org.simbrain.workspace.gui

import kotlinx.coroutines.isActive
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.workspace.Workspace
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldDesktopComponent
import javax.swing.SwingUtilities

class DesktopComponentScopeTest {

    @Test
    fun `closing a component ends its view scope and its panel's`() {
        val workspace = Workspace()
        val component = OdorWorldComponent("Odor world")
        workspace.addWorkspaceComponent(component)
        lateinit var desktop: OdorWorldDesktopComponent
        SwingUtilities.invokeAndWait {
            desktop = OdorWorldDesktopComponent(GenericJInternalFrame("Odor world", true, true, true, true), component)
        }
        assertTrue(desktop.viewScope.isActive)

        SwingUtilities.invokeAndWait { desktop.close() }

        assertFalse(desktop.viewScope.isActive, "the view scope outlived its component")
        assertFalse(desktop.worldPanel.viewScope.isActive, "the panel's scope outlived its window")
    }
}
