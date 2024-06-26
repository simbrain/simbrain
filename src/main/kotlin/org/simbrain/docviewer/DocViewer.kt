package org.simbrain.docviewer

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
        
        # Local image     
        
        ![Flower](//localfiles/simulations/images/Caltech101Sample/image_0036.jpg)

        # Remote image     
        
        ![Bobcat Drawing](https://upload.wikimedia.org/wikipedia/commons/3/37/Ernest_Ingersoll_-_lynx_rufus_%26_lynx_canadensis.png)
        """.trimIndent()

    @Transient
    var renderedText = ""
        private set

    fun render() {
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(text)
        val userDir = System.getProperty("user.dir").replace("\\\\".toRegex(), "\\\\\\\\")
        renderedText = HtmlGenerator(text, parsedTree, flavour)
            .generateHtml()
            .replace("//localfiles/", "file:${userDir}/")
    }

    init {
        render()
    }

}