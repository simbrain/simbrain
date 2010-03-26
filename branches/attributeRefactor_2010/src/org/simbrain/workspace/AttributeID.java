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
package org.simbrain.workspace;

/**
 * TODO...
 *
 * @author jyoshimi
 *
 */
public class AttributeID {

    /** The ID. */
    private String id;

    /** Subtype id. Optional. Not always relevant. */
    private String subtype = null;

    /** Parent workspace component. */
    private WorkspaceComponent parent;

    /**
     * Returns a description for use in GUIs...
     *
     * @return description
     */
    public String getDescription() {
        if (subtype == null) {
            return id;
        } else {
            return id + " (" + subtype + ")";
        }
    }

    /**
     * Construct an attribute id.
     *
     * @param parent the parent workspace component
     * @param id the attribute id
     */
    public AttributeID(WorkspaceComponent parent, String id) {
        this.parent = parent;
        this.id = id;
    }


    /**
     * @return the name
     */
    public String getID() {
        return id;
    }


    /**
     * @return the subtype
     */
    public String getSubtype() {
        return subtype;
    }


    /**
     * @param subtype the subtype to set
     */
    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    /**
     * @return the parent
     */
    public WorkspaceComponent getParent() {
        return parent;
    }
}
