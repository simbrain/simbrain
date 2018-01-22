package org.simbrain.world.imageworld.filters;

import java.text.NumberFormat;

import javax.swing.JFormattedTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.imageworld.ImageSource;

public class ThresholdFilterFactory extends ImageFilterFactory {
    static {
        ImageFilterFactory.putFactory("Threshold Filter", new ThresholdFilterFactory());
    }

    public static ImageFilter createThresholdFilter(ImageSource source, double threshold, int width, int height) {
        return new ImageFilter(source, "Threshold Filter", new ThresholdOp(threshold), width, height); 
    }

    private double threshold;

    @Override
    public void setDefaultValues() {
        super.setDefaultValues();
        threshold = 0.5;
    }

    @Override
    public void getValuesFromFilter(ImageFilter filter) {
        super.getValuesFromFilter(filter);
        threshold = ((ThresholdOp) filter.getColorOp()).getThreshold();
    }

    @Override
    public LabelledItemPanel getEditorPanel() {
        LabelledItemPanel panel = super.getEditorPanel();
        JFormattedTextField thresholdField = new JFormattedTextField(NumberFormat.getNumberInstance());
        thresholdField.setValue(threshold);
        thresholdField.addPropertyChangeListener("value", (evt) -> {
            threshold = ((Number) thresholdField.getValue()).doubleValue();
            threshold = Math.max(Math.min(threshold, 1.0), 0.0);
            thresholdField.setValue(threshold);
        });
        panel.addItem("Threshold", thresholdField);
        return panel;
    }

    @Override
    public ImageFilter create(ImageSource source) {
        return createThresholdFilter(source, threshold, getWidth(), getHeight());
    }
}