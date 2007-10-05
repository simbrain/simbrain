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
package org.simbrain.world.textworld;

import org.simbrain.util.Utils;

/**
 * <b>Entry</b>.
 *
 */
public class Entry {

    /** Text string. */
    private String textString;
    /** Text vector. */
    private double[] textVector;

    /**
     * Entry.
     * @param string String
     * @param vector Vector
     */
    public Entry(final String string, final String vector) {
        textString = string;
        textVector = Utils.getVectorString(vector, ",");

    }
    /**
     * @return Returns the textString.
     */
    public String getTextString() {
        return textString;
    }
    /**
     * @param textString The textString to set.
     */
    public void setTextString(final String textString) {
        this.textString = textString;
    }
    /**
     * @return Returns the textVector.
     */
    public double[] getTextVector() {
        return textVector;
    }
    /**
     * @param textVector The textVector to set.
     */
    public void setTextVector(final double[] textVector) {
        this.textVector = textVector;
    }

    /**
     * @see java.lang.Object.toString
     * @return new string using the textVector.
     */
    public String toString() {
//        return new String(textString + ' ' + textVector);
        String ret = new String();
        ret += (textString + ' ');
        for (int i = 0; i < textVector.length; i++) {
          ret += ("<" + textVector[i] + ">");
        }
        return ret;
        //String ret = new String();
        // ret += (textString + " ")
        // for ....
        ///   ret += ("<" + textVector[i] + ">

                ///  Jeff <1><21><0>


    }
}
