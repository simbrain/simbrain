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
package org.simbrain.docviewer

import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.util.widgets.SimbrainTextArea
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

/**
 * Component for editing documents in markdown and rendering them in html.
 * Used to document Simbrain sims.
 */
class DocViewerDesktopComponent(frame: GenericFrame, component: DocViewerComponent)
    : DesktopComponent<DocViewerComponent>(frame, component) {

    private val renderedText = JEditorPane()

    private val menuBar = JMenuBar()

    private val file = JMenu("File")

    private val codeEditor = SimbrainTextArea()
    
    private val docViewer = component.docViewer

    init {
        preferredSize = Dimension(500, 400)
        layout = BorderLayout()

        // File Menu
        menuBar.add(file)
        file.add(actionManager.createImportAction(this))
        file.add(actionManager.createExportAction(this))
        file.addSeparator()
        file.add(actionManager.createRenameAction(this))
        file.addSeparator()
        file.add(actionManager.createCloseAction(this))

        val helpMenu = JMenu("Help")
        val helpItem = JMenuItem("Help")
        val helpAction: Action = ShowHelpAction("https://docs.simbrain.net/docs/utilities/docviewer.html")
        helpItem.action = helpAction
        helpMenu.add(helpItem)
        menuBar.add(helpMenu)

        parentFrame.jMenuBar = menuBar

        renderedText.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
        renderedText.contentType = "text/html"
        renderedText.isEditable = false
        renderedText.text = docViewer.renderedText

        val viewPanel = JScrollPane(renderedText)

        val tabs = JTabbedPane()

        codeEditor.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_MARKDOWN
        val editPanel = JPanel().apply {
            layout = BorderLayout()
            codeEditor.lineWrap = true
            add("Center", RTextScrollPane(codeEditor).apply {
                isFoldIndicatorEnabled = true
            })
        }
        add(editPanel)

        tabs.addTab("View", viewPanel)
        tabs.addTab("Edit", editPanel)

        add("Center", tabs)

        // Tab changed events
        val changeListener = ChangeListener { changeEvent ->
            val sourceTabbedPane = changeEvent.source as JTabbedPane
            val index = sourceTabbedPane.selectedIndex
            // Assumes index of view tab is 0
            if (index == 0) {
                docViewer.text = codeEditor.text
                docViewer.render()
                renderedText.text = docViewer.renderedText
            }
            docViewer.render()
            renderedText.caretPosition = 0
            codeEditor.caretPosition = 0
        }
        tabs.addChangeListener(changeListener)

        val l = HyperlinkListener { e ->
            if (HyperlinkEvent.EventType.ACTIVATED == e.eventType) {
                try {
                    if (e.url != null) {
                        // System.out.println(e.getURL().toURI());
                        Desktop.getDesktop().browse(processLocalFiles(e.url.toURI()))
                    }
                } catch (e1: IOException) {
                    e1.printStackTrace()
                } catch (e1: URISyntaxException) {
                    e1.printStackTrace()
                }
            }
        }
        renderedText.addHyperlinkListener(l)
        codeEditor.text = docViewer.text
        renderedText.caretPosition = 0
    }

    /**
     * Convert local paths into absolute paths for links based on the local file
     * system.
     */
    private fun processLocalFiles(uri: URI): URI {
        var uriStr = uri.toString()
        if (uriStr.startsWith("//localfiles/")) {
            uriStr = "file:" + System.getProperty("user.dir") + "/" + uriStr.substring(5)
            val url: URL
            try {
                url = URL(uriStr)
                return url.toURI()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uri
    }

}
