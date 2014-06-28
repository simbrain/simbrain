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
package org.simbrain.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 
 * A class solely for storing constants used throughout simbrain.
 * 
 * @author Zach Tosi
 * 
 */
public class SimbrainConstants {

    public static final String NULL_STRING = "...";

    public static final NumberFormat LOCAL_FORMATTER = NumberFormat
        .getNumberInstance(Locale.getDefault());

    public static enum Polarity {
        EXCITATORY {
            @Override
            public double value(double val) {
                return Math.abs(val);
            }

            @Override
            public String title() {
                return "Excitatory";
            }
        },
        INHIBITORY {
            @Override
            public double value(double val) {
                return -Math.abs(val);
            }

            @Override
            public String title() {
                return "Inhibitory";
            }
        };
        public abstract double value(double val);

        /** A capitalized version to toString(). */
        public abstract String title();
    }

}
