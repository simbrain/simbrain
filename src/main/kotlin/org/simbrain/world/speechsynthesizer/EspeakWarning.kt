package org.simbrain.world.speechsynthesizer

import org.simbrain.util.showCopyableWarningDialog
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

private val espeakWarningShown = AtomicBoolean(false)

fun warnIfEspeakUnavailable() {
    if (EspeakRuntime.ensureInitialized()) return
    if (!espeakWarningShown.compareAndSet(false, true)) return
    val message = EspeakRuntime.errorMessage ?: "Speech audio is not available."
    SwingUtilities.invokeLater { showCopyableWarningDialog(message) }
}
