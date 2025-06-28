package org.simbrain.docviewer

import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

/**
 * Component corresponding to a Document Viewer.
 */
class DocViewerComponent(val docViewer: DocViewer = DocViewer(), name: String = "") : WorkspaceComponent(name) {

    override fun save(output: OutputStream, format: String?) {
        getSimbrainXStream().toXML(docViewer, output)
    }

    override val xml: String
        get() = getSimbrainXStream().toXML(docViewer)


    companion object {
        fun open(input: InputStream, name: String, format: String?): DocViewerComponent {
            val docViewer = getSimbrainXStream().fromXML(input) as DocViewer
            docViewer.render()
            return DocViewerComponent(docViewer, name)
        }
    }
}
