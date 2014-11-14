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
package org.simbrain.docviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Component corresponding to a Document Viewer.
 */
public class DocViewerComponent extends WorkspaceComponent {

    /** Default string. */
    private String text = "<html>\n<body>\nUse this text to explain how a simulation works,\n"
            + "and save it with the workspace so that\n"
            + "when it is re-opened other users will know how to use it.\n<br><br>\n"
            + "Uses simple html for formatting, e.g. <b>bold text</b>.\n Click on the "
            + "Edit tab to edit the html \n or import from pre-edited html "
            + "using the File menu.<br><br>"
            + "Example of a local image: <img src = \"file:docs/Images/simbrainlogo.gif\"><br><br>\n"
            + "Example of a local link: <a href = \"file:docs/SimbrainDocs.html\">docs</a>\n"
            + " \n</body>\n</html>\n";

    /**
     * Construct a new document viewer component.
     */
    public DocViewerComponent() {
        super("");
    }

    /**
     * Construct a new document viewer component with a specified title.
     *
     * @param name title for frame this is displayed in
     */
    public DocViewerComponent(String name) {
        super(name);
    }

    /**
     * Opens a saved component. There isn't much to do here since currently
     * there is nothing to persist with a console. This just ensures that a
     * component is created and (in the gui) presented.
     *
     * @param input stream
     * @param name name of file
     * @param format format
     * @return component to be opened
     */
    public static DocViewerComponent open(InputStream input, final String name,
            final String format) {
        DocViewerComponent comp = new DocViewerComponent(name);
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(input,
                Charset.forName("UTF-8")));
        String line;
        String text = new String();
        try {
            while ((line = br.readLine()) != null) {
                text = text.concat(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        comp.setText(text);
        return comp;
    }

    @Override
    public void save(OutputStream output, final String format) {
        try {
            output.write(getText().getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void closing() {
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public List<? extends String> getFormats() {
        return Collections.singletonList("html");
    }
}
