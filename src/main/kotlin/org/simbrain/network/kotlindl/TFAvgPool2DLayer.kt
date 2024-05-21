// package org.simbrain.network.kotlindl
//
// import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
// import org.jetbrains.kotlinx.dl.api.core.layer.pooling.AvgPool2D
// import org.simbrain.util.propertyeditor.GuiEditable
//
// /**
//  * Wrapper for kotlin dl Average pooling layer
//  */
// class TFAvgPool2DLayer : TFLayer<AvgPool2D>() {
//
//     var poolSize by GuiEditable(
//         initValue = intArrayOf(3,3),
//         order = 20,
//         onUpdate = {
//             enableWidget(layer == null)
//         },
//     )
//
//     var strides by GuiEditable(
//         initValue = intArrayOf(1,1,1,1),
//         order = 30,
//         onUpdate = {
//             enableWidget(layer == null)
//         },
//     )
//
//     var padding by GuiEditable(
//         initValue = ConvPadding.SAME,
//         order = 80,
//         onUpdate = {
//             enableWidget(layer == null)
//         },
//     )
//
//     override var layer: AvgPool2D? = null
//
//     override fun create() : AvgPool2D {
//         return AvgPool2D(
//             poolSize = poolSize,
//             strides = strides,
//             padding = ConvPadding.SAME,
//         ).also {
//             layer = it
//         }
//     }
//
//     override val name = "Average Pool 2d"
//
// }