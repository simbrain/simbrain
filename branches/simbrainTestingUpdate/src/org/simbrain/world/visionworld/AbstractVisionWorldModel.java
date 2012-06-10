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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Abstract implementation of VisionWorldModel which provides
 * VisionWorldModelListener management.
 */
abstract class AbstractVisionWorldModel extends VisionWorldModelListenerSupport
        implements VisionWorldModel {

    /**
     * Create a new abstract vision world model.
     */
    protected AbstractVisionWorldModel() {
        super();
        setSource(this);
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    public static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        // TODO: Too much is stored in pixel matrix and elsewhere
        // Also the pixels don't actually get stored
        // xstream.omitField(MutableVisionWorldModel.class, "pixelMatrix");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        // logger = Logger.getLogger(Network.class);
        // this.updatePriorities = new TreeSet<Integer>();
        return this;
    }
}
