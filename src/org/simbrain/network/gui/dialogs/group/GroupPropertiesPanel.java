/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.group;

import org.simbrain.network.groups.Group;


/**
 * Interface for property panels that have fill field and commit change methods.
 * These must often be shared between dialogs for creation and editing of a
 * group, hence the interface.
 *
 * @author Jeff Yoshimi
 *
 */
public interface GroupPropertiesPanel {

    /** Initialize the panel with field value. */
    void fillFieldValues();

    /**
     * Take all field values from the panel and use it to create or edit
     * relevant object.
     *
     * @return whether or not the committ was successful.
     */
    boolean commitChanges();

    /**
     * @return the group associated with this panel.
     */
    Group getGroup();

    /**
     * Returns a string path to the documentation page for this group, relative
     * to the top level Simbrain/doc directory.
     *
     * @return the path to the docs
     */
    String getHelpPath();

}
