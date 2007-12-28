/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceComponentListener;

/**
 * Vision world frame.
 */
public final class VisionWorldComponent extends WorkspaceComponent<WorkspaceComponentListener> {//implements CouplingContainer {

    /**
     * Create a new vision world frame with the specified name.
     *
     * @param name name
     */
    public VisionWorldComponent(final String name) {
        super(name);
    }

    @Override
    public void close() {
        // empty
    }

    @Override
    public String getFileExtension() {
        return null;
    }

    @Override
    public void save(final File saveFile) {
        // empty
    }

    /** {@inheritDoc} */
    public List<Consumer> getConsumers() {
        return Collections.<Consumer>emptyList();
    }
    
    /** {@inheritDoc} */
    public void update() {
        // empty
    }

    @Override
    public void open(File openFile) {
        // empty
    }
}
