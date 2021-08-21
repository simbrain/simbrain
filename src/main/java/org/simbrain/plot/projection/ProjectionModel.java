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
package org.simbrain.plot.projection;

import org.simbrain.util.projection.Projector;
import org.simbrain.workspace.AttributeContainer;

/**
 * Main data for a projection chart.
 */
public class ProjectionModel implements AttributeContainer {

    /**
     * The underlying projector object.
     */
    private Projector projector = new Projector();


    /**
     * Flag which allows the user to start and stop iterative projection
     * techniques..
     */
    private transient volatile boolean isRunning = true;

    /**
     * Flag for checking that GUI update is completed.
     */
    private transient volatile boolean isUpdateCompleted;

    // TODO: User parameter
    private int storagePrecision = 10;

    /**
     * Default constructor.
     */
    public ProjectionModel() {
    }


    /**
     * Returns the projector.
     *
     * @return the projector.
     */
    public Projector getProjector() {
        return projector;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        projector.postOpenInit();
        return this;
    }

    /**
     * @return whether this component being updated by a thread or not.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * This flag allows the user to start and stop iterative projection
     * techniques.
     *
     * @param b whether this component being updated by a thread or not.
     */
    public void setRunning(boolean b) {
        isRunning = b;
    }

    /**
     * Swing update flag.
     *
     * @param b whether updated is completed
     */
    public void setUpdateCompleted(final boolean b) {
        isUpdateCompleted = b;
    }

    /**
     * Swing update flag.
     *
     * @return whether update is completed or not
     */
    public boolean isUpdateCompleted() {
        return isUpdateCompleted;
    }


    @Override
    public String getId() {
        return "Projection";
    }

}
