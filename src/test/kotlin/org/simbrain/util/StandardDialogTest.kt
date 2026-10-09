/**
 * Every way of closing a [StandardDialog] runs its close tasks and ends its view scope, including Escape, which used
 * to only hide the dialog and leave its subscriptions running.
 */
package org.simbrain.util

import kotlinx.coroutines.isActive
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class StandardDialogTest {

    @Test
    fun `escape closes the dialog the way its close button does`() {
        var closeTaskRan = false
        lateinit var dialog: StandardDialog
        SwingUtilities.invokeAndWait {
            dialog = StandardDialog()
            dialog.addCloseTask { closeTaskRan = true }
            val escape = dialog.rootPane.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0))
            escape.actionPerformed(ActionEvent(dialog.rootPane, ActionEvent.ACTION_PERFORMED, null))
        }

        assertTrue(closeTaskRan, "close tasks did not run")
        assertTrue(dialog.hasUserCancelled(), "escape did not count as cancelling")
        assertFalse(dialog.viewScope.isActive, "the dialog's view scope outlived it")
    }
}
