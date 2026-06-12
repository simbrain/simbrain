package org.simbrain.docviewer

import org.simbrain.util.FlowEvents

class DocViewerEvents: FlowEvents() {

    val textChanged = OneArgEvent<String>()
    val renderedTextChanged = OneArgEvent<String>()

}