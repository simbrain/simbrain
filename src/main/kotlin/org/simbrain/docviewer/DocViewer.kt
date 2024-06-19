package org.simbrain.docviewer

import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable

class DocViewer: EditableObject {

    enum class Mode {
        HTML, MARKDOWN
    }

    var mode: Mode by GuiEditable(
        initValue = Mode.MARKDOWN
    )

    var text: String = """
            Use this text to explain how a simulation works,
            and save it with the workspace so that
            when it is re-opened other users will know how to use it.


            Uses simple html for formatting, e.g. **bold text**.
            Click on the Edit tab to edit the html 
            or import from pre-edited html using the File menu. Example of a local image: ![Flower](//localfiles/simulations/images/Caltech101Sample/image_0036.jpg)
            
            Example of a link: [google](https://google.com)
            """.trimIndent()

    var renderedText = ""
        private set

    fun render() {
        renderedText = when (mode) {
            Mode.MARKDOWN -> {
                val flavour = CommonMarkFlavourDescriptor()
                val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(text)
                HtmlGenerator(text, parsedTree, flavour).generateHtml()
            }
            Mode.HTML -> {
                text
            }
        }.replace("//localfiles/", "file:")
    }

    init {
        render()
    }

}