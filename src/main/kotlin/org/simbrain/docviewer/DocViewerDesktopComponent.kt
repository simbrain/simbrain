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

        val viewPanel = DocViewerViewPanel().apply {
            docViewer.render()
            text = docViewer.renderedText
        }

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
                viewPanel.text = docViewer.renderedText
            }
            docViewer.render()
            viewPanel.renderedTextPanel.caretPosition = 0
            codeEditor.caretPosition = 0
        }
        tabs.addChangeListener(changeListener)

        codeEditor.text = docViewer.text
        viewPanel.renderedTextPanel.caretPosition = 0
    }

}
