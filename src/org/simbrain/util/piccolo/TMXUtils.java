package org.simbrain.util.piccolo;

public class TMXUtils {

    public static int parseIntWithDefaultValue(String string, int defaultValue) {
        int ret;
        try {
            ret = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            ret = defaultValue;
        }
        return ret;
    }
}
