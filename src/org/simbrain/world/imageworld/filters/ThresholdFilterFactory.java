package org.simbrain.world.imageworld.filters;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.imageworld.ImageSource;

import javax.swing.*;
import java.text.NumberFormat;

public class ThresholdFilterFactory extends ImageFilterFactory {
    private static class ThresholdFilterSource extends FilteredImageSource {
        private double threshold;

        ThresholdFilterSource(ImageSource source, double threshold, int width, int height) {
            super(source, "Threshold Filter", new ThresholdOp(threshold), width, height);
            this.threshold = threshold;
        }

        @Override
        public Object readResolve() {
            super.readResolve();
            setColorOp(new ThresholdOp(threshold));
            return this;
        }
    }

    static {
        ImageFilterFactory.putFactory("Threshold Filter", new ThresholdFilterFactory());
    }

    public static FilteredImageSource createThresholdFilter(ImageSource source, double threshold, int width, int height) {
        return new ThresholdFilterSource(source, threshold, width, height);
    }

    private double threshold;

    @Override
    public void setDefaultValues() {
        super.setDefaultValues();
        threshold = 0.5;
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
    public FilteredImageSource create(ImageSource source) {
        return createThresholdFilter(source, threshold, getWidth(), getHeight());
    }
}