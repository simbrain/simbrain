package org.simbrain.network.gui;

import java.awt.Color;

public class NetworkGuiSettings {

    /** Color of all lines in rootNetwork panel. */
    private static Color lineColor =  Color.black;
    
    /**
     * @return Returns the lineColor.
     */
    public static Color getLineColor() {
        return lineColor;
    }

    /**
     * @param lineColor The lineColor to set.
     */
    public static void setLineColor(final Color lc) {
        lineColor = lc;
    }

}
