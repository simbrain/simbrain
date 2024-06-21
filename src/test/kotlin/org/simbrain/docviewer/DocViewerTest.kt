package org.simbrain.docviewer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class DocViewerTest {


    @Test
    fun testXStream() {

        // Create a world
        val dvc = DocViewerComponent(name = "Test")
        dvc.docViewer.text = "markdown test"

        val xstream = dvc.xml
        val stream: InputStream = ByteArrayInputStream(xstream.toByteArray(StandardCharsets.UTF_8))
        // println(xstream)

        // Unmarshall from xstream
        val fromXML = DocViewerComponent.open(stream, "test2", "xml")

        assertEquals("markdown test", fromXML.docViewer.text)
    }

}