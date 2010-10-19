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
 * Defines the base API for consumers and producers.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
public interface Attribute {

    /**
     * Returns a reference to the parent component.
     *
     * @return parent parent component
     */
    public WorkspaceComponent getParentComponent();

    /*
     * Returns the base object associated with this attribute.
     *
     * @return the base object.
     */
    public Object getBaseObject();

    /*
     * Returns the type of this attribute.
     *
     * @return the data type, as a class.
     */
    public Class<?> getDataType();

    /**
     * Returns the base name of the method associated with this attribute (that
     * is, the method name without "get" or "set")
     *
     * @return base method name
     */
    public String getMethodBaseName();

    /**
     * Returns the descriptive name of this attribute.
     *
     * @return the name of this attribute.
     */
    public String getDescription();
}
