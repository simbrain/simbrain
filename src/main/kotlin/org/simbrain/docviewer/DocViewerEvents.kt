package org.simbrain.docviewer

import org.simbrain.util.Events

class DocViewerEvents: Events() {

    val textChanged = OneArgEvent<String>()
    val renderedTextChanged = OneArgEvent<String>()

}