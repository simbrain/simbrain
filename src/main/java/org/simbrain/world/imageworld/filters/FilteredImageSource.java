// package org.simbrain.world.imageworld.filters;
//
// import org.simbrain.util.UserParameter;
// import org.simbrain.util.propertyeditor.EditableObject;
// import org.simbrain.world.imageworld.ImageSource;
// import org.simbrain.world.imageworld.ImageSourceAdapter;
//
// import java.awt.image.BufferedImage;
// import java.awt.image.BufferedImageOp;
//
// /**
//  * FilteredImageSource decorates an ImageSource with a color and size transform.
//  * <p>
//  * Based on a similar pattern in awt.
//  *
//  * @author Tim Shea
//  * @author Jeff Yoshimi
//  */
// public class FilteredImageSource extends ImageSourceAdapter implements EditableObject {
//
//     /**
//      * The parent image source being filtered.
//      */
//     private ImageSource wrappedSource;
//
//     @UserParameter(
//             label = "Width",
//             order = 1
//     )
//     private int width = 10;
//
//     @UserParameter(
//             label = "Height",
//             order = 2
//     )
//     private int height;
//
//     @UserParameter(
//             label = "Filter",
//             order = 3,
//             isObjectType = true
//     )
//     private ImageOperation imageOp;
//
//     private transient BufferedImageOp scaleOp;
//
//     /**
//      * Construct a new FilteredImageSource.
//      *
//      * @param source  the ImageSource to be filtered
//      * @param colorOp the color filter to apply
//      * @param width   the width of the output image
//      * @param height  the height of the output image
//      */
//     public FilteredImageSource(ImageSource source,  ImageOperation colorOp, int width, int height) {
//         wrappedSource = source;
//         this.imageOp = colorOp;
//         this.width = width;
//         this.height = height;
//         notifyResize();
//         scaleToFit(wrappedSource);
//         wrappedSource.getEvents().onResize(this::notifyResize);
//         wrappedSource.getEvents().onImageUpdate(this::onImageUpdate);
//     }
//
//     public Object readResolve() {
//         super.readResolve();
//         notifyResize();
//         scaleToFit(wrappedSource);
//         wrappedSource.getEvents().onResize(this::notifyResize);
//         wrappedSource.getEvents().onImageUpdate(this::onImageUpdate);
//         return this;
//     }
//
//     /**
//      * @return the current unfiltered image
//      */
//     public BufferedImage getUnfilteredImage() {
//         return wrappedSource.getCurrentImage();
//     }
//
//     /**
//      * @param value The BufferedImageOp to assign to the color op.
//      */
//     protected void setImageOp(ImageOperation value) {
//         imageOp = value;
//     }
//
//     /**
//      * @param value the BufferedImageOp to assign to the scale op.
//      */
//     protected void setScaleOp(BufferedImageOp value) {
//         scaleOp = value;
//     }
//
//     @Override
//     public String getName() {
//         return imageOp.getName();
//     }
//
//     @Override
//     public int getWidth() {
//         return width;
//     }
//
//     @Override
//     public int getHeight() {
//         return height;
//     }
//
//     public void onImageUpdate() {
//         BufferedImage image = wrappedSource.getCurrentImage();
//         image = scaleOp.filter(image, null);
//         image = imageOp.getOp().filter(image, null);
//         setCurrentImage(image,false);
//     }
//
//     protected void scaleToFit(ImageSource source) {
//         // Subtract 0.1 from width and height to avoid exceeding the specified dimension due to floating point error.
//         float scaleX = (width - 0.1f) / source.getWidth();
//         float scaleY = (height - 0.1f) / source.getHeight();
//         scaleOp = ImageFilterFactory.createScaleOp(scaleX, scaleY, true);
//     }
// }
