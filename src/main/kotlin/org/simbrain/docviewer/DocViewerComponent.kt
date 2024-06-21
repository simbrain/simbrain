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

import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

/**
 * Component corresponding to a Document Viewer.
 */
class DocViewerComponent(val docViewer: DocViewer = DocViewer(), name: String = "") : WorkspaceComponent(name) {

    override fun save(output: OutputStream, format: String?) {
        getSimbrainXStream().toXML(docViewer)
    }

    override val xml: String
        get() = getSimbrainXStream().toXML(docViewer)


    companion object {
        fun open(input: InputStream, name: String, format: String?): DocViewerComponent {
            val docViewer = getSimbrainXStream().fromXML(input) as DocViewer
            return DocViewerComponent(docViewer, name)
        }
    }
}
