package org.simbrain.network.conv

import org.simbrain.network.core.TensorShape

/**
 * Pure stateless convolution and pooling operations on flat DoubleArrays in HWC layout.
 */
object ConvOps {

    /**
     * 2D convolution. Accumulates results into [output] (caller must zero it first if needed).
     *
     * @param input       flat HWC input array
     * @param inputShape  shape of input
     * @param kernels     flat kernel weights: [numFilters][inputChannels][kH][kW] stored as
     *                    filter-major, then channel, then row, then col
     * @param numFilters  number of output filters
     * @param kernelSize  spatial kernel size (square: kH = kW = kernelSize)
     * @param biases      per-filter biases (length = numFilters)
     * @param output      pre-allocated flat HWC output array
     * @param outputShape shape of output
     * @param stride      convolution stride
     * @param padH        vertical padding
     * @param padW        horizontal padding
     */
    fun conv2d(
        input: DoubleArray, inputShape: TensorShape,
        kernels: DoubleArray, numFilters: Int, kernelSize: Int,
        biases: DoubleArray,
        output: DoubleArray, outputShape: TensorShape,
        stride: Int, padH: Int, padW: Int
    ) {
        val inC = inputShape.channels
        val inH = inputShape.height
        val inW = inputShape.width
        val outH = outputShape.height
        val outW = outputShape.width
        val kernelArea = kernelSize * kernelSize

        for (f in 0 until numFilters) {
            val filterOffset = f * inC * kernelArea
            val bias = biases[f]
            for (oh in 0 until outH) {
                for (ow in 0 until outW) {
                    var sum = bias
                    val inHBase = oh * stride - padH
                    val inWBase = ow * stride - padW
                    for (c in 0 until inC) {
                        val channelOffset = filterOffset + c * kernelArea
                        for (kh in 0 until kernelSize) {
                            val ih = inHBase + kh
                            if (ih !in 0..<inH) continue
                            for (kw in 0 until kernelSize) {
                                val iw = inWBase + kw
                                if (iw !in 0..<inW) continue
                                val inputVal = input[inputShape.index(ih, iw, c)]
                                val kernelVal = kernels[channelOffset + kh * kernelSize + kw]
                                sum += inputVal * kernelVal
                            }
                        }
                    }
                    output[outputShape.index(oh, ow, f)] += sum
                }
            }
        }
    }

    /**
     * 2D max pooling. Accumulates results into [output].
     *
     * @param maxIndices optional array to store the index of the max element (for future backprop)
     */
    fun maxPool2d(
        input: DoubleArray, inputShape: TensorShape,
        output: DoubleArray, outputShape: TensorShape,
        poolSize: Int, stride: Int,
        maxIndices: IntArray? = null
    ) {
        val inH = inputShape.height
        val inW = inputShape.width
        val channels = inputShape.channels
        val outH = outputShape.height
        val outW = outputShape.width

        for (c in 0 until channels) {
            for (oh in 0 until outH) {
                for (ow in 0 until outW) {
                    var maxVal = Double.NEGATIVE_INFINITY
                    var maxIdx = -1
                    val inHBase = oh * stride
                    val inWBase = ow * stride
                    for (ph in 0 until poolSize) {
                        val ih = inHBase + ph
                        if (ih >= inH) continue
                        for (pw in 0 until poolSize) {
                            val iw = inWBase + pw
                            if (iw >= inW) continue
                            val idx = inputShape.index(ih, iw, c)
                            val v = input[idx]
                            if (v > maxVal) {
                                maxVal = v
                                maxIdx = idx
                            }
                        }
                    }
                    val outIdx = outputShape.index(oh, ow, c)
                    output[outIdx] += maxVal
                    maxIndices?.set(outIdx, maxIdx)
                }
            }
        }
    }

    /**
     * 2D average pooling. Accumulates results into [output].
     */
    fun avgPool2d(
        input: DoubleArray, inputShape: TensorShape,
        output: DoubleArray, outputShape: TensorShape,
        poolSize: Int, stride: Int
    ) {
        val inH = inputShape.height
        val inW = inputShape.width
        val channels = inputShape.channels
        val outH = outputShape.height
        val outW = outputShape.width
        val poolArea = poolSize * poolSize

        for (c in 0 until channels) {
            for (oh in 0 until outH) {
                for (ow in 0 until outW) {
                    var sum = 0.0
                    val inHBase = oh * stride
                    val inWBase = ow * stride
                    for (ph in 0 until poolSize) {
                        val ih = inHBase + ph
                        if (ih >= inH) continue
                        for (pw in 0 until poolSize) {
                            val iw = inWBase + pw
                            if (iw >= inW) continue
                            sum += input[inputShape.index(ih, iw, c)]
                        }
                    }
                    output[outputShape.index(oh, ow, c)] += sum / poolArea
                }
            }
        }
    }

    // Backward functions for backpropagation

    /**
     * Backward pass for conv2d: computes gradient w.r.t. input.
     * For each output gradient element, distributes it back to the input positions
     * that contributed to it, weighted by the corresponding kernel values.
     *
     * Accumulates into [inputGrad] (caller must zero it first if needed).
     */
    fun conv2dBackwardInput(
        outputGrad: DoubleArray, outputShape: TensorShape,
        kernels: DoubleArray, numFilters: Int, kernelSize: Int,
        inputGrad: DoubleArray, inputShape: TensorShape,
        stride: Int, padH: Int, padW: Int
    ) {
        val inC = inputShape.channels
        val inH = inputShape.height
        val inW = inputShape.width
        val outH = outputShape.height
        val outW = outputShape.width
        val kernelArea = kernelSize * kernelSize

        for (f in 0 until numFilters) {
            val filterOffset = f * inC * kernelArea
            for (oh in 0 until outH) {
                for (ow in 0 until outW) {
                    val dOut = outputGrad[outputShape.index(oh, ow, f)]
                    if (dOut == 0.0) continue
                    val inHBase = oh * stride - padH
                    val inWBase = ow * stride - padW
                    for (c in 0 until inC) {
                        val channelOffset = filterOffset + c * kernelArea
                        for (kh in 0 until kernelSize) {
                            val ih = inHBase + kh
                            if (ih !in 0..<inH) continue
                            for (kw in 0 until kernelSize) {
                                val iw = inWBase + kw
                                if (iw !in 0..<inW) continue
                                inputGrad[inputShape.index(ih, iw, c)] +=
                                    dOut * kernels[channelOffset + kh * kernelSize + kw]
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Backward pass for conv2d: computes gradient w.r.t. kernels and biases.
     * For each output gradient element, accumulates the product of output gradient
     * and input value into the corresponding kernel gradient position.
     *
     * Accumulates into [kernelGrad] and [biasGrad] (caller must zero them first if needed).
     */
    fun conv2dBackwardKernels(
        outputGrad: DoubleArray, outputShape: TensorShape,
        input: DoubleArray, inputShape: TensorShape,
        kernelGrad: DoubleArray, numFilters: Int, kernelSize: Int,
        biasGrad: DoubleArray,
        stride: Int, padH: Int, padW: Int
    ) {
        val inC = inputShape.channels
        val inH = inputShape.height
        val inW = inputShape.width
        val outH = outputShape.height
        val outW = outputShape.width
        val kernelArea = kernelSize * kernelSize

        for (f in 0 until numFilters) {
            val filterOffset = f * inC * kernelArea
            for (oh in 0 until outH) {
                for (ow in 0 until outW) {
                    val dOut = outputGrad[outputShape.index(oh, ow, f)]
                    if (dOut == 0.0) continue
                    biasGrad[f] += dOut
                    val inHBase = oh * stride - padH
                    val inWBase = ow * stride - padW
                    for (c in 0 until inC) {
                        val channelOffset = filterOffset + c * kernelArea
                        for (kh in 0 until kernelSize) {
                            val ih = inHBase + kh
                            if (ih !in 0..<inH) continue
                            for (kw in 0 until kernelSize) {
                                val iw = inWBase + kw
                                if (iw !in 0..<inW) continue
                                kernelGrad[channelOffset + kh * kernelSize + kw] +=
                                    dOut * input[inputShape.index(ih, iw, c)]
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Backward pass for max pooling. Routes each output gradient to the position
     * of the max element that was selected during the forward pass.
     *
     * Accumulates into [inputGrad] (caller must zero it first if needed).
     */
    fun maxPool2dBackward(
        outputGrad: DoubleArray, outputShape: TensorShape,
        maxIndices: IntArray,
        inputGrad: DoubleArray
    ) {
        for (i in 0 until outputShape.size) {
            inputGrad[maxIndices[i]] += outputGrad[i]
        }
    }

    /**
     * Backward pass for average pooling. Distributes each output gradient equally
     * across all positions in the corresponding pool window.
     *
     * Accumulates into [inputGrad] (caller must zero it first if needed).
     */
    fun avgPool2dBackward(
        outputGrad: DoubleArray, outputShape: TensorShape,
        inputGrad: DoubleArray, inputShape: TensorShape,
        poolSize: Int, stride: Int
    ) {
        val inH = inputShape.height
        val inW = inputShape.width
        val channels = inputShape.channels
        val outH = outputShape.height
        val outW = outputShape.width
        val poolArea = poolSize * poolSize

        for (c in 0 until channels) {
            for (oh in 0 until outH) {
                for (ow in 0 until outW) {
                    val dOut = outputGrad[outputShape.index(oh, ow, c)] / poolArea
                    val inHBase = oh * stride
                    val inWBase = ow * stride
                    for (ph in 0 until poolSize) {
                        val ih = inHBase + ph
                        if (ih >= inH) continue
                        for (pw in 0 until poolSize) {
                            val iw = inWBase + pw
                            if (iw >= inW) continue
                            inputGrad[inputShape.index(ih, iw, c)] += dOut
                        }
                    }
                }
            }
        }
    }
}
