package org.simbrain.docviewer

import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Paths
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.event.HyperlinkEvent

class DocViewerViewPanel: JScrollPane() {

    var text: String
        get() = renderedTextPanel.text
        set(value) {
            renderedTextPanel.text = value
        }

    val renderedTextPanel = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        addHyperlinkListener { e ->
            if (HyperlinkEvent.EventType.ACTIVATED == e.eventType) {
                try {
                    if (e.url != null) {
                        Desktop.getDesktop().browse(processLocalFiles(e.url.toURI()))
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            }
        }
    }.also {
        setViewportView(it)
    }

    /**
     * Convert local paths into absolute paths for links based on the local file
     * system.
     */
    private fun processLocalFiles(uri: URI): URI {
        val uriStr = uri.toString()
        if (uriStr.startsWith("//localfiles/")) {
            try {
                return Paths.get(System.getProperty("user.dir"), uriStr.substring(5)).toUri()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uri
    }



}