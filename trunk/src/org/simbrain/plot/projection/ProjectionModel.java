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

import org.simbrain.util.projection.Dataset;
import org.simbrain.util.projection.ProjectionMethod;
import org.simbrain.util.projection.Projector;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Main data for a projection chart.
 */
public class ProjectionModel {

    /** High Dimensional Projection. */
    private Projector projector = new Projector();
    
    /**
     * Default constructor.
     *
     * @param numDataSources dimension of the data.
     */
    public ProjectionModel(final int numDataSources) {
        projector.init(numDataSources);
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
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(Projector.class, "logger");
        xstream.omitField(Projector.class, "currentState");
        xstream.omitField(ProjectionMethod.class, "logger");
        xstream.omitField(Dataset.class, "logger");
        xstream.omitField(Dataset.class, "distances");
        xstream.omitField(Dataset.class, "dataset");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized.
     * See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     * 
     * @return Initialized object.
     */
    private Object readResolve() {
        projector.getUpstairs().postOpenInit();
        projector.getDownstairs().postOpenInit();
        return this;
    }

}
