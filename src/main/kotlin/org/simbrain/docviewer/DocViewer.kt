package org.simbrain.docviewer

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.simbrain.util.propertyeditor.EditableObject

class DocViewer: EditableObject {

    // When calling this manually call render() after
    var text: String = """
        # Doc Viewer
        Use markdown to create documentation to go explain simulations. Simple html is supported in markdown 
        so simple html is supported as well, but markdown is suggested. 
        
        A [markdown cheat sheet](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet).
        
        # Some Basic commands
        1. First item 
          - Unordered item with *italics* in one style
          - Unordered sub-item with _italics_ in another style
        2. Second item 
          - Unordered item with **bold** in one style
          - Unordered item with __bold__ in one style
        
        # A local image     
        
        ![Flower](//localfiles/simulations/images/Caltech101Sample/image_0036.jpg)
        """.trimIndent()

    @Transient
    var renderedText = ""
        private set

    fun render() {
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(text)
        renderedText = HtmlGenerator(text, parsedTree, flavour)
            .generateHtml()
            .replace("//localfiles/", "file:")
    }

    init {
        render()
    }

}