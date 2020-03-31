package org.simbrain.util

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun StandardDialog.present() = apply { isVisible = true }

inline fun StandardDialog.onClosed(crossinline block: (WindowEvent?) -> Unit) = apply {
    addWindowListener(object : WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
            block(e)
        }
    })
}