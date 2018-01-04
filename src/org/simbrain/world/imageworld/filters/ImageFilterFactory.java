package org.simbrain.world.imageworld.filters;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JFormattedTextField;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.imageworld.ImageSource;

/**
 * Maintains gui information (name, editor panel) relating to filters. Add
 * custom filter types by extending this factory class, see e.g
 * {@link ThresholdFilterFactory}
 */
public abstract class ImageFilterFactory extends XmlAdapter<String, ImageFilter> {
    private static Map<String, ImageFilterFactory> factories = new HashMap<String, ImageFilterFactory>();

    /**
     * The default filters.
     */
    static {
        putFactory("Color Filter", new ImageFilterFactory() {
            @Override
            public ImageFilter create(ImageSource source) {
                return createColorFilter(source, getWidth(), getHeight());
            }
        });
        putFactory("Gray Filter", new ImageFilterFactory() {
            @Override
            public ImageFilter create(ImageSource source) {
                return createGrayFilter(source, getWidth(), getHeight());
            }
        });
    }

    public static ImageFilter createColorFilter(ImageSource source, int width, int height) {
        return new ImageFilter(source, "Color Filter", ImageFilter.getIdentityOp(), width, height);
    }

    public static ImageFilter createGrayFilter(ImageSource source, int width, int height) {
        return new ImageFilter(source, "Gray Filter", ImageFilter.getGrayOp(), width, height);
    }

    public static void putFactory(String type, ImageFilterFactory factory) {
        factories.put(type, factory);
    }

    public static ImageFilterFactory getFactory(String type) {
        return factories.get(type);
    }

    public static Set<String> getTypes() {
        return factories.keySet();
    }

    private int width;
    private int height;

    public void setDefaultValues() {
        width = 10;
        height = 10;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int value) {
        width = value;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int value) {
        height = value;
    }

    public void getValuesFromFilter(ImageFilter filter) {
        width = filter.getWidth();
        height = filter.getHeight();
    }

    @Override
    public ImageFilter unmarshal(String xml) {
        return null;
    }

    @Override
    public String marshal(ImageFilter filter) {
        return null;
    }

    public LabelledItemPanel getEditorPanel() {
        LabelledItemPanel panel = new LabelledItemPanel();
        JFormattedTextField widthField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        widthField.setValue(width);
        widthField.addPropertyChangeListener("value", (evt) -> {
            width = ((Number) widthField.getValue()).intValue();
            width = Math.max(Math.min(width, 10000), 1);
            widthField.setValue(width);
        });
        panel.addItem("Width", widthField);
        JFormattedTextField heightField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        heightField.setValue(height);
        heightField.addPropertyChangeListener("value", (evt) -> {
            height = ((Number) heightField.getValue()).intValue();
            height = Math.max(Math.min(height, 10000), 1);
            heightField.setValue(height);
        });
        panel.addItem("Height", heightField);
        return panel;
    }

    public abstract ImageFilter create(ImageSource source);
}
