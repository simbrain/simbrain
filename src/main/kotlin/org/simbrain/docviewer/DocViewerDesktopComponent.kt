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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.util.widgets.SimbrainTextArea
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.io.*
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.Charset
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

/**
 * A very simple component which displays html and allows it to be edited. Uses
 * a JEditorPane to display html and an RSSyntaxTextArea to edit it.
 *
 *
 * Examples of html code for local links and images:
 *
 *
 * <img src = "file:docs/Images/World.gif" alt="world"></img>
 * [Local link](file:docs/SimbrainDocs.html).
 */
class DocViewerDesktopComponent(frame: GenericFrame, component: DocViewerComponent)
    : DesktopComponent<DocViewerComponent>(frame, component) {

    private val textArea = JEditorPane()

    private val menuBar = JMenuBar()

    private val file = JMenu("File")

    private val htmlEditor = SimbrainTextArea()
    
    val docViewer = component.docViewer

    val modeSelector = AnnotatedPropertyEditor(docViewer)

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
        val item = JMenuItem("Import html...")
        item.addActionListener { e ->
            val _fileChooser = JFileChooser()
            val retval = _fileChooser.showOpenDialog(textArea)
            if (retval == JFileChooser.APPROVE_OPTION) {
                val f = _fileChooser.selectedFile
                try {
                    val br: BufferedReader
                    val fis: InputStream = FileInputStream(f)
                    br = BufferedReader(InputStreamReader(fis, Charset.forName("UTF-8")))
                    htmlEditor.read(br, null)
                    textArea.text = htmlEditor.text
                    docViewer.text = htmlEditor.text
                    htmlEditor.caretPosition = 0
                } catch (ioex: IOException) {
                    println(e)
                }
            }
        }
        file.add(item)
        file.addSeparator()
        file.add(actionManager.createCloseAction(this))

        val helpMenu = JMenu("Help")
        val helpItem = JMenuItem("Help")
        val helpAction: Action = ShowHelpAction("Pages/DocEditor.html")
        helpItem.action = helpAction
        helpMenu.add(helpItem)
        menuBar.add(helpMenu)

        parentFrame.jMenuBar = menuBar

        textArea.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
        textArea.contentType = "text/html"
        textArea.isEditable = false
        textArea.text = docViewer.renderedText

        val sp = JScrollPane(textArea)

        val tabs = JTabbedPane()
        modeSelector.getWidgetEventsByLabel("Mode").valueChanged.on(Dispatchers.Swing) {
            val mode = modeSelector.getWidgetValueByLabel("Mode") as DocViewer.Mode
            updateSyntaxHighlighting(mode)
            if (mode == DocViewer.Mode.HTML) {
                val option = JOptionPane.showOptionDialog(
                    null,
                    "Do you want to convert the current text content from markdown to HTML?",
                    "Convert to HTML?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    arrayOf("Convert to HTML", "Keep text the same"),
                    null
                )
                if (option == JOptionPane.YES_OPTION) {
                    docViewer.text = docViewer.renderedText
                    htmlEditor.text = docViewer.text
                }
            }
        }
        updateSyntaxHighlighting(docViewer.mode)
        htmlEditor.isCodeFoldingEnabled = true
        htmlEditor.antiAliasingEnabled = true
        val sp2 = JPanel().apply {
            layout = BorderLayout()
            add("North", modeSelector)
            add("Center", RTextScrollPane(htmlEditor).apply {
                isFoldIndicatorEnabled = true
            })
        }
        add(sp2)

        tabs.addTab("View", sp)
        tabs.addTab("Edit", sp2)

        add("Center", tabs)

        // Listen for tab changed events. Synchronize the editor and the
        // display tabs on these events.
        // TODO: listen for changes in the editor and only update the display
        // when changes occur
        val changeListener = ChangeListener { changeEvent ->
            val sourceTabbedPane = changeEvent.source as JTabbedPane
            val index = sourceTabbedPane.selectedIndex
            // Assumes index of view tab is 0
            if (index == 0) {
                modeSelector.commitChanges()

                docViewer.text = htmlEditor.text
                docViewer.render()
                textArea.text = docViewer.renderedText
            }
            docViewer.render()
        }
        tabs.addChangeListener(changeListener)

        // Respond to clicks on hyper-links by opening a web page in the default
        // browser
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
        textArea.addHyperlinkListener(l)
        htmlEditor.text = docViewer.text
        textArea.caretPosition = 0
    }

    /**
     * Convert local paths into absolute paths for links based on the local file
     * system.
     *
     * @param uri the uri to process
     * @return an update uri if it is a file link
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

    private fun updateSyntaxHighlighting(mode: DocViewer.Mode) {
        htmlEditor.syntaxEditingStyle = when(mode) {
            DocViewer.Mode.HTML -> SyntaxConstants.SYNTAX_STYLE_HTML
            DocViewer.Mode.MARKDOWN -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN
        }
    }

}
