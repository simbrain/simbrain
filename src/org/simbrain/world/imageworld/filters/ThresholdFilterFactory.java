package org.simbrain.world.imageworld.filters;

import org.simbrain.world.imageworld.ImageSource;

public class ThresholdFilterFactory extends ImageFilterFactory {

    private static class ThresholdFilterSource extends FilteredImageSource {
    
        private double threshold;

        ThresholdFilterSource(ImageSource source, double threshold, int width, int height) {
            super(source, new ThresholdOp(threshold), width, height);
            this.threshold = threshold;
        }

        @Override
        public Object readResolve() {
            super.readResolve();
            setImageOp(new ThresholdOp(threshold));
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
    public FilteredImageSource create(ImageSource source) {
        return createThresholdFilter(source, threshold, getWidth(), getHeight());
    }
}