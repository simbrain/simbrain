package org.simbrain.util.uisnapshot

import org.simbrain.util.getPreferenceDialog
import org.simbrain.workspace.WorkspacePreferences
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * The Workspace Preferences dialog, where the Theme (System/Light/Dark) control now lives. Confirms
 * the enum preference renders as a combo box and that a real AnnotatedPropertyEditor dialog renders
 * cleanly under dark.
 */
class WorkspacePreferencesDialogSnapshot : UiSnapshotDef {
    override val name = "workspace_preferences_dialog"

    override fun build(): Component {
        lateinit var dialog: Component
        SwingUtilities.invokeAndWait {
            dialog = getPreferenceDialog(WorkspacePreferences)
        }
        return dialog
    }
}
