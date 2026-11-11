package org.simbrain.docviewer

import org.intellij.lang.annotations.Language
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.simbrain.util.propertyeditor.EditableObject

class DocViewer: EditableObject {

    // When calling this manually call render() after
    var text: String = """
        # Doc Viewer
        Use markdown to create documentation and to explain simulations. Simple html is supported in markdown 
        so simple html is supported as well, but markdown is suggested. 
        
        A [markdown cheat sheet](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet).
        
        # Some Basic commands
        1. First item 
          - Unordered item with *italics* in one style
          - Unordered sub-item with _italics_ in another style
        2. Second item 
          - Unordered item with **bold** in one style
          - Unordered item with __bold__ in one style

        # Remote image     
        
        ![Simbrain logo](https://simbrain.net/images/simbrain_logo.png)
        


               
        # Local image     
        
        ![Sample figure](//localfiles/simulations/images/visualWorld/spiveyActivationDynamics.png)

        """.trimIndent()
        set(value) {
            field = value
            events.textChanged.fire(value)
        }

    @Transient
    var renderedText = ""
        private set(value) {
            field = value
            events.renderedTextChanged.fire(value)
        }

    @Transient
    val events = DocViewerEvents()

    fun render() {
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(text)
        val userDir = System.getProperty("user.dir").replace("\\\\".toRegex(), "\\\\\\\\")
        @Language("HTML")
        val head = """
            <head>
                <style>
                    body {
                        margin-left: 12px;
                        margin-right: 12px;
                        font-family: SansSerif;
                        font-size: 12pt;
                    }
                    ul, ol {
                        margin-left: 16px;
                    }
                    h1 {
                        font-size: 18pt;
                        font-weight: normal;
                        margin-top: 18px;
                        margin-bottom: 4px;
                        padding-bottom: 3px;
                    }
                    h2 {
                        font-size: 13pt;
                        margin-top: 14px;
                        margin-bottom: 2px;
                        padding-left: 6px;
                    }
                    h3 {
                        font-size: 12pt;
                        margin-top: 10px;
                        margin-bottom: 0px;
                    }
                    p {
                        margin-top: 4px;
                        margin-bottom: 6px;
                    }
                    code {
                        font-size: 11pt;
                        font-weight: bold;
                    }
                </style>
            </head>
        """.trimIndent()
        val body = HtmlGenerator(text, parsedTree, flavour)
            .generateHtml()
            .replace("//localfiles/", "file:${userDir}/")
        renderedText = "<html>${head}<body>${body}</body></html>"
    }

    init {
        render()
    }

}