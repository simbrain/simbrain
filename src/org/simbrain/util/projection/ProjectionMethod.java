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
package org.simbrain.util.projection;

import org.apache.log4j.Logger;

/**
 * <b>ProjectionMethod</b> is a superclass for all specific projection
 * algorithms used. by the projector. Subclasses correspond to different
 * dimensionality reduction techniques.
 */
public abstract class ProjectionMethod {

    /** Logger. */
    private Logger logger = Logger.getLogger(ProjectionMethod.class);

    /** Reference to parent projector. */
    protected final Projector projector;

    /**
     * Superclass constructor which sets the projector instance.
     *
     * @param projector the parent projector.
     */
    public ProjectionMethod(Projector projector) {
        this.projector = projector;
    }

    /**
     * Perform any initialization necessary for a given projection method.
     */
    public abstract void init();

    /**
     * Perform operations necessary to project the data. For iterable functions
     * this is just a stub.
     */
    public abstract void project();

    /**
     * Convenience method to determine if instances of this are instances of
     * IterableProjectionMethod.
     *
     * @return true if iterable
     */
    public boolean isIterable() {
        return this instanceof IterableProjectionMethod;
    }

}