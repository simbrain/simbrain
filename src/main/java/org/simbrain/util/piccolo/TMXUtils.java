package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.XStream;
import org.simbrain.util.Utils;

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

    public static XStream getXStream() {
        XStream xstream = Utils.getSimbrainXStream();
        xstream.processAnnotations(TileMap.class);
        return xstream;
    }
}
