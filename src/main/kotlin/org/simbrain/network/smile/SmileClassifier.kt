//package org.simbrain.network.smile
//
//import org.simbrain.network.core.Network
//import org.simbrain.network.events.NetworkModelEvents
//import org.simbrain.network.matrix.NeuronArray
//import smile.classification.SVM
//import smile.math.kernel.PolynomialKernel
//
//class SmileClassifier(network: Network, size: Int): NeuronArray(network, size) {
//
//    private var kernel = PolynomialKernel(2)
//
//    private var x = arrayOf(0.0)
//
//    private var y = intArrayOf(1)
//
////    private var classifier = SVM.fit(x, y, kernel, 1000.0, 1E-3)
//
//    override fun update() {
//        // TODO
////        val result: Int = classifier.predict(inputs)
////        setOneHot(if (result == -1) 0 else 1)
//    }
//
//}