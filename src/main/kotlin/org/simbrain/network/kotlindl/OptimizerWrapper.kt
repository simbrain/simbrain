// package org.simbrain.network.kotlindl
//
// import org.jetbrains.kotlinx.dl.api.core.optimizer.*
// import org.simbrain.util.UserParameter
// import org.simbrain.util.propertyeditor.CopyableObject
//
// /**
//  * Wrapper for tensor flow optimizers
//  */
// abstract class OptimizerWrapper() : CopyableObject {
//
//     lateinit var optimizer: Optimizer
//
//     /**
//      * For use with object type editor.
//      */
//     abstract override fun copy(): OptimizerWrapper
//
//     override fun getTypeList() = optimizerTypes
//
// }
//
// val optimizerTypes = listOf(AdaDeltaWrapper::class.java, AdaGradWrapper::class.java, AdaGradaWrapper::class.java, AdamWrapper::class.java, AdamaxWrapper::class.java, FtrlWrapper::class.java,
//     MomentumWrapper::class.java, RMSPropWrapper::class.java, SGDWrapper::class.java)
//
// class AdaDeltaWrapper() : OptimizerWrapper() {
//     @UserParameter(label = "LearningRate", order = 10)
//     private var learningRate: Float = 0.1f
//
//     @UserParameter(label = "Rho", order = 20)
//     private var rho: Float = 0.95f
//
//     @UserParameter(label = "Epsilon", order = 30)
//     private var epsilon: Float = 1e-8f
//
//     init {
//         optimizer = AdaDelta()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return AdaDeltaWrapper()
//     }
//
//     override val name = "AdaDelta"
//
// }
//
// class AdaGradWrapper() : OptimizerWrapper() {
//     @UserParameter(label = "LearningRate", order = 10)
//     private var learningRate: Float = 0.1f
//
//     @UserParameter(label = "InitialAccumulatorValue", order = 20)
//     private var initialAccumulatorValue: Float = 0.01f
//
//     init {
//         optimizer = AdaGrad()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return AdaGradWrapper()
//     }
//
//     override val name = "AdaGrad"
// }
//
//
// class AdaGradaWrapper() : OptimizerWrapper() {
//     @UserParameter(label = "LearningRate", order = 10)
//     private var learningRate: Float = 0.1f
//
//     @UserParameter(label = "InitialAccumulatorValue", order = 20)
//     private var initialAccumulatorValue: Float = 0.01f
//
//     @UserParameter(label = "L1Strength", order = 30)
//     private var l1Strength: Float = 0.01f
//
//     @UserParameter(label = "L2Strength", order = 40)
//     private var l2Strength: Float = 0.01f
//
//
//     init {
//         optimizer = AdaGradDA()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return AdaGradaWrapper()
//     }
//
//     override val name = "AdaGradDA"
// }
//
//
// class AdamWrapper() : OptimizerWrapper() {
//
//     @UserParameter(label = "LearningRate",  minimumValue = 0.0, increment = .01, order = 10)
//     private var learningRate: Float = 0.001f
//
//     @UserParameter(label = "Beta1", minimumValue = 0.0, maximumValue = 1.0, order = 20)
//     private var beta1: Float = 0.9f
//
//     @UserParameter(label = "Beta2", minimumValue = 0.0, maximumValue = 1.0, increment = .01, order = 30)
//     private var beta2: Float = 0.999f
//
//     @UserParameter(label = "Epsilon", minimumValue = 0.0, increment = .01, order = 40)
//     private var epsilon: Float = 1e-07f
//
//     @UserParameter(label = "UseNesterov", order = 50)
//     private var useNesterov: Boolean = false
//
//     init {
//         optimizer = Adam()
//     }
//
//     override fun onCommit() {
//         optimizer = Adam(learningRate = learningRate, beta1 = beta1, beta2=beta2, epsilon=epsilon, useNesterov = useNesterov)
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return AdamWrapper()
//     }
//
//     override val name = "Adam"
//
// }
//
//
// class AdamaxWrapper() : OptimizerWrapper() {
//
//     @UserParameter(label = "LearningRate", order = 10)
//     private val learningRate: Float = 0.001f
//
//     @UserParameter(label = "Beta1", order = 20)
//     private val beta1: Float = 0.9f
//
//     @UserParameter(label = "Beta2", order = 30)
//     private val beta2: Float = 0.999f
//
//     @UserParameter(label = "Epsilon", order = 40)
//     private val epsilon: Float = 1e-07f
//
//     init {
//         optimizer = Adamax()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return AdamaxWrapper()
//     }
//
//     override val name = "Adamax"
// }
//
//
// class FtrlWrapper() : OptimizerWrapper() {
//     @UserParameter(label = "LearningRate", order = 10)
//     private var learningRate: Float = 0.001f
//
//     @UserParameter(label = "L1RegularizationStrength", order = 20)
//     private var l1RegularizationStrength: Float = 0.0f
//
//     @UserParameter(label = "LearningRatePower", order = 30)
//     private var learningRatePower: Float = -0.5f
//
//     @UserParameter(label = "L2ShrinkageRegularizationStrength", order = 40)
//     private var l2ShrinkageRegularizationStrength: Float = 0.0f
//
//     @UserParameter(label = "InitialAccumulatorValue", order = 50)
//     private var initialAccumulatorValue: Float = 0.0f
//
//
//     init {
//         optimizer = Ftrl()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return FtrlWrapper()
//     }
//
//     override val name = "Ftrl"
// }
//
// class MomentumWrapper() : OptimizerWrapper() {
//
//     @UserParameter(label = "LearningRate", order = 10)
//     private var learningRate: Float = 0.001f
//
//     @UserParameter(label = "Momentum", order = 20)
//     private var momentum: Float = 0.99f
//
//     @UserParameter(label = "UseNesterov", order = 30)
//     private var useNesterov: Boolean = true
//
//     init {
//         optimizer = Momentum()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return MomentumWrapper()
//     }
//
//     override val name = "Momentum"
// }
//
// class RMSPropWrapper() : OptimizerWrapper() {
//
//     @UserParameter(label = "LearningRate", order = 10)
//     private var learningRate: Float = 0.001f
//
//     @UserParameter(label = "Momentum", order = 20)
//     private var momentum: Float = 0.99f
//
//     @UserParameter(label = "UseNesterov", order = 30)
//     private var useNesterov: Boolean = true
//
//     init {
//         optimizer = RMSProp()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return RMSPropWrapper()
//     }
//
//     override val name = "RMSProp"
// }
//
// class SGDWrapper() : OptimizerWrapper() {
//
//     @UserParameter(label = "LearningRate", order = 10)
//     private var learningRate: Float = 0.2f
//
//     init {
//         optimizer = SGD()
//     }
//
//     override fun copy(): OptimizerWrapper {
//         return SGDWrapper()
//     }
//
//     override val name = "SGD"
//
// }
//
