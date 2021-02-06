package org.simbrain.network

import org.junit.Test
import smile.math.MathEx.c
import smile.math.MathEx.norm
import smile.math.matrix.Matrix

class SmileTestKt {

    @Test
    fun `messing with matrices`() {
        val x = c(1.0, 2.0, 3.0, 4.0)
        val y = c(4.0, 3.0, 2.0, 1.0)

        println(norm(x))

        val A = arrayOf(
            doubleArrayOf(0.7220180, 0.07121225, 0.6881997),
            doubleArrayOf(-0.2648886, -0.89044952, 0.3700456),
            doubleArrayOf(-0.6391588, 0.44947578, 0.6240573)
        )
        println(A)
        var a = Matrix(A)

        var test1 = Matrix.eye(2)
        var test2 = Matrix.eye(2)

        println(test1.add(test2.mul(2.0)))

        println(test1)


        // println(Matrix.randn(2,2))
    }
}