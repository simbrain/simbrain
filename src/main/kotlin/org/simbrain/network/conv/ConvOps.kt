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
        require(outputShape.channels == numFilters) { "Output has ${outputShape.channels} channels for $numFilters filters" }
        val inC = inputShape.channels
        val inH = inputShape.height
        val inW = inputShape.width
        val byPosition = kernelsByPosition(kernels, numFilters, inC, kernelSize)
        val sums = DoubleArray(numFilters)

        for (oh in 0 until outputShape.height) {
            val inHBase = oh * stride - padH
            for (ow in 0 until outputShape.width) {
                val inWBase = ow * stride - padW
                System.arraycopy(biases, 0, sums, 0, numFilters)
                for (kh in 0 until kernelSize) {
                    val ih = inHBase + kh
                    if (ih < 0 || ih >= inH) continue
                    for (kw in 0 until kernelSize) {
                        val iw = inWBase + kw
                        if (iw < 0 || iw >= inW) continue
                        val inBase = inputShape.index(ih, iw, 0)
                        val kernelBase = (kh * kernelSize + kw) * inC * numFilters
                        for (c in 0 until inC) {
                            val x = input[inBase + c]
                            if (x == 0.0) continue
                            val k = kernelBase + c * numFilters
                            for (f in 0 until numFilters) sums[f] += x * byPosition[k + f]
                        }
                    }
                }
                val outBase = outputShape.index(oh, ow, 0)
                for (f in 0 until numFilters) output[outBase + f] += sums[f]
            }
        }
    }

    /**
     * [kernels], stored [filter][channel][kh][kw], rearranged to [kh][kw][channel][filter]. With HWC tensors the
     * channels at one position are contiguous, and so are the filters for one input value, so the convolution loops
     * run over contiguous memory with the padding checks hoisted out of them.
     */
    private fun kernelsByPosition(kernels: DoubleArray, numFilters: Int, inC: Int, kernelSize: Int): DoubleArray =
        rearrangeKernels(kernels, numFilters, inC, kernelSize) { k, c, f -> positionChannelFilter(k, c, f, inC, numFilters) }

    /** Index of kernel position [k] (kh * kernelSize + kw), channel [c] and filter [f] in [kh][kw][channel][filter] order. */
    private fun positionChannelFilter(k: Int, c: Int, f: Int, inC: Int, numFilters: Int) = (k * inC + c) * numFilters + f

    /** Index of kernel position [k], channel [c] and filter [f] in [kh][kw][filter][channel] order. */
    private fun positionFilterChannel(k: Int, c: Int, f: Int, inC: Int, numFilters: Int) = (k * numFilters + f) * inC + c

    /** [kernels], stored [filter][channel][kh][kw], copied to the position [index] gives each weight. */
    private inline fun rearrangeKernels(
        kernels: DoubleArray, numFilters: Int, inC: Int, kernelSize: Int,
        index: (k: Int, c: Int, f: Int) -> Int
    ): DoubleArray {
        val area = kernelSize * kernelSize
        val rearranged = DoubleArray(numFilters * inC * area)
        for (f in 0 until numFilters) {
            for (c in 0 until inC) {
                val from = (f * inC + c) * area
                for (k in 0 until area) rearranged[index(k, c, f)] = kernels[from + k]
            }
        }
        return rearranged
    }

    /** True when any of the [count] gradients from [start] is nonzero; ReLU zeroes many whole output positions. */
    private fun anyNonZero(values: DoubleArray, start: Int, count: Int): Boolean {
        for (i in start until start + count) if (values[i] != 0.0) return true
        return false
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
        require(outputShape.channels == numFilters) { "Output has ${outputShape.channels} channels for $numFilters filters" }
        val inC = inputShape.channels
        val inH = inputShape.height
        val inW = inputShape.width
        // [kh][kw][filter][channel]: each output gradient scales a contiguous run of channel weights, so the inner loop
        // is independent multiply-adds rather than a sum whose every add waits on the last
        val byPosition = rearrangeKernels(kernels, numFilters, inC, kernelSize) { k, c, f ->
            positionFilterChannel(k, c, f, inC, numFilters)
        }

        for (oh in 0 until outputShape.height) {
            val inHBase = oh * stride - padH
            for (ow in 0 until outputShape.width) {
                val outBase = outputShape.index(oh, ow, 0)
                if (!anyNonZero(outputGrad, outBase, numFilters)) continue
                val inWBase = ow * stride - padW
                for (kh in 0 until kernelSize) {
                    val ih = inHBase + kh
                    if (ih < 0 || ih >= inH) continue
                    for (kw in 0 until kernelSize) {
                        val iw = inWBase + kw
                        if (iw < 0 || iw >= inW) continue
                        val inBase = inputShape.index(ih, iw, 0)
                        val kernelBase = (kh * kernelSize + kw) * numFilters * inC
                        for (f in 0 until numFilters) {
                            val g = outputGrad[outBase + f]
                            if (g == 0.0) continue
                            val k = kernelBase + f * inC
                            for (c in 0 until inC) inputGrad[inBase + c] += g * byPosition[k + c]
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
        require(outputShape.channels == numFilters) { "Output has ${outputShape.channels} channels for $numFilters filters" }
        val inC = inputShape.channels
        val inH = inputShape.height
        val inW = inputShape.width
        val area = kernelSize * kernelSize
        // Accumulated in [kh][kw][channel][filter] order, like the forward pass, then added in kernelGrad's order
        val gradByPosition = DoubleArray(numFilters * inC * area)

        for (oh in 0 until outputShape.height) {
            val inHBase = oh * stride - padH
            for (ow in 0 until outputShape.width) {
                val outBase = outputShape.index(oh, ow, 0)
                if (!anyNonZero(outputGrad, outBase, numFilters)) continue
                for (f in 0 until numFilters) biasGrad[f] += outputGrad[outBase + f]
                val inWBase = ow * stride - padW
                for (kh in 0 until kernelSize) {
                    val ih = inHBase + kh
                    if (ih < 0 || ih >= inH) continue
                    for (kw in 0 until kernelSize) {
                        val iw = inWBase + kw
                        if (iw < 0 || iw >= inW) continue
                        val inBase = inputShape.index(ih, iw, 0)
                        val kernelBase = (kh * kernelSize + kw) * inC * numFilters
                        for (c in 0 until inC) {
                            val x = input[inBase + c]
                            if (x == 0.0) continue
                            val k = kernelBase + c * numFilters
                            for (f in 0 until numFilters) gradByPosition[k + f] += x * outputGrad[outBase + f]
                        }
                    }
                }
            }
        }
        // The inverse of kernelsByPosition, accumulating
        for (f in 0 until numFilters) {
            for (c in 0 until inC) {
                val to = (f * inC + c) * area
                for (k in 0 until area) kernelGrad[to + k] += gradByPosition[positionChannelFilter(k, c, f, inC, numFilters)]
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
