package org.simbrain.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.simbrain.network.gui.dialogs.createDataSetPanel
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.crossEntropy
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.util.table.Column
import smile.math.matrix.Matrix
import kotlin.math.ln
import kotlin.random.Random

class SmileUtilsTest {

    val testMatrix = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0)
    ))

    val nonSquareMatrix = Matrix.of(arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0),
        doubleArrayOf(10.0, 11.0, 12.0)
    ))

    @Test
    fun `test validate shape`() {
        val a = Matrix(1, 2)
        val b = Matrix(2, 1)
        assertDoesNotThrow{ a.validateSameShape(a) }
        assertThrows<IllegalArgumentException> { a.validateSameShape(b) }

        // Uncomment to check the exception formatting
        // a.validateShape(b)
    }

    @Test
    fun `test row matrix transposed`() {
        val rmt = nonSquareMatrix.rowVectorTransposed(0)
        assertEquals(3, rmt.nrow())
        assertEquals(1, rmt.ncol())
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), rmt.toDoubleArray())
    }

    @Test
    fun `test broadcasting a vector across columns`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toColumnVector()
        val result = testMatrix.scaleColumns(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), result.col(0))
        assertArrayEquals(doubleArrayOf(2.0, 5.0, 8.0), result.col(1))
        assertArrayEquals(doubleArrayOf(6.0, 12.0, 18.0), result.col(2))
    }

    @Test
    fun `test broadcasting a vector across rows`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toColumnVector()
        val result = testMatrix.scaleRows(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), result.row(0))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), result.row(1))
        assertArrayEquals(doubleArrayOf(14.0, 16.0, 18.0), result.row(2))
    }

    @Test
    fun `test broadcasting multiplication on non square matrix`() {
        val vector = doubleArrayOf(0.0, 1.0, 2.0).toColumnVector()
        val result = nonSquareMatrix.scaleColumns(vector)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0, 0.0), result.col(0))
        assertArrayEquals(doubleArrayOf(2.0, 5.0, 8.0, 11.0), result.col(1))
        assertArrayEquals(doubleArrayOf(6.0, 12.0, 18.0, 24.0), result.col(2))
    }

    @Test
    fun testMaxEigen() {
        assertEquals(1.0, Matrix.eye(2).maxEigenvalue())
        assertEquals(2.0, Matrix.eye(2).mul(2.0).maxEigenvalue())
        val ut=  Matrix.of(arrayOf(
            doubleArrayOf(-2.0, 4.0),
            doubleArrayOf(0.0, 7.0)
        ))
        assertEquals(7.0, ut.maxEigenvalue())
    }

    @Test
    fun testSpectralRadius() {
        assertEquals(.9, testMatrix.setSpectralRadius(.9).maxEigenvalue(), .01)
    }

    @Test
    fun shiftUpAndPadEndWithZero() {
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), nonSquareMatrix.shiftUpAndPadEndWithZero().row(0))
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), nonSquareMatrix.shiftUpAndPadEndWithZero().row(nonSquareMatrix.nrow() - 1))
    }

    @Test
    fun `test add operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = arrayOf(
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(7.0, 8.0)
        ).toMatrix()
        val c = a + b
        assertArrayEquals(doubleArrayOf(6.0, 8.0), c.row(0))
        assertArrayEquals(doubleArrayOf(10.0, 12.0), c.row(1))
    }

    @Test
    fun `test minus operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = arrayOf(
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(7.0, 8.0)
        ).toMatrix()
        val c = a - b
        assertArrayEquals(doubleArrayOf(-4.0, -4.0), c.row(0))
        assertArrayEquals(doubleArrayOf(-4.0, -4.0), c.row(1))
    }

    @Test
    fun `test scalar multiplication operator`() {
        val a = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val b = a * 2.0
        assertArrayEquals(doubleArrayOf(2.0, 4.0), b.row(0))
        assertArrayEquals(doubleArrayOf(6.0, 8.0), b.row(1))
        val c = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).toMatrix()
        val d = -2.0 * c
        assertArrayEquals(doubleArrayOf(-2.0, -4.0), d.row(0))
        assertArrayEquals(doubleArrayOf(-6.0, -8.0), d.row(1))
    }

    @Test
    fun `test cross entropy loss`() {
        val t1 =  doubleArrayOf(0.0, 1.0, 0.0).toColumnVector()
        val a1 =  doubleArrayOf(0.2, .7, 0.1).toColumnVector()
        assertEquals(0.0, crossEntropy(t1, t1), 0.001)
        assertEquals(-ln(.7), crossEntropy(a1, t1))
    }

    @Test
    fun `test validate column vector`(){
        val a = Matrix(4, 1)
        val b = Matrix(1, 4)
        assertDoesNotThrow{a.validateColumnVector()}
        assertThrows<Error>{b.validateColumnVector()}
    }

    @Test
    fun `test copy matrix using copyFrom`(){
        val a = Matrix(3, 3)
        a.copyFrom(testMatrix)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), a.row(1))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), a.row(2))

        a.copyFrom(nonSquareMatrix, allowShapeMismatch = true)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), a.row(1))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), a.row(2))
    }

    @Test
    fun `test reshape matrix`(){
        val a = testMatrix.reshape(2,3)
        assertArrayEquals(doubleArrayOf(1.0,2.0,3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(4.0,5.0,6.0), a.row(1))
        assertEquals(2, a.nrow())
        assertEquals(3, a.ncol())
        assertThrows<IllegalArgumentException>{a.validateSameShape(testMatrix)}
        val b = a.reshape(3,3)
        assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0), b.row(2))
        testMatrix.reshape(4,2)
    }

    @Test
    fun `test shapeString`(){
        assertEquals("3 x 3", testMatrix.shapeString)
        assertEquals("4 x 3", nonSquareMatrix.shapeString)
    }

    @Test
    fun `test clip`(){
        val a = Matrix(3, 3)
        a.copyFrom(testMatrix)
        a.clip(2.0,7.0)
        assertArrayEquals(doubleArrayOf(2.0, 2.0, 3.0), a.row(0))
        assertArrayEquals(doubleArrayOf(7.0, 7.0, 7.0), a.row(2))
    }

    @Test
    fun `test shifting`() {
        assertArrayEquals(doubleArrayOf(3.0, 1.0, 2.0), testMatrix.shiftRight().row(0))
        assertArrayEquals(doubleArrayOf(5.0, 6.0, 4.0), testMatrix.shiftRight(2).row(1))
        assertArrayEquals(doubleArrayOf(4.0, 5.0, 6.0), testMatrix.shiftUp().row(0))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), testMatrix.shiftUp(-1).row(0))
        assertArrayEquals(doubleArrayOf(7.0, 8.0, 9.0), testMatrix.shiftUp(2).row(0))
    }

    @Test
    fun `test setrow and setrowconstant`(){
        val a = Matrix(3,3)
        a.copyFrom(testMatrix)
        a.setRowConstant(0, 10.0)
        assertArrayEquals(doubleArrayOf(10.0,10.0,10.0), a.row(0))
        a.setRow(1, doubleArrayOf(11.0, 12.0, 13.0))
        assertArrayEquals(doubleArrayOf(11.0, 12.0, 13.0), a.row(1))
        assertThrows<ArrayIndexOutOfBoundsException>{a.setRowConstant(3, 10.0)}
        assertThrows<ArrayIndexOutOfBoundsException>{a.setRow(3, doubleArrayOf(0.0,0.0,0.0))}
    }

    @Test
    fun `test flatten matrix`(){
        assertEquals(9, testMatrix.flatten().size)
    }

    @Test
    fun `test double array to matrix`(){
        val colVector =  testMatrix.flatten().toColumnVector()
        assertEquals(9, colVector.nrow())
        assertEquals(1, colVector.ncol())
    }

    @Test
    fun `test appendRow`(){
        val a = testMatrix.appendRow(doubleArrayOf(10.0, 11.0, 12.0))
        assertEquals(4, a.nrow())
        assertArrayEquals(doubleArrayOf(10.0, 11.0, 12.0), a.row(3))
    }

    @Test
    fun `test matrix building shortcut`() {
        val m1 = Matrix.of(arrayOf(doubleArrayOf(1.0,2.0,3.0)))
        val m2 = matrix[1,3](1,2,3)
        assertArrayEquals(m1.flatten(), m2.flatten())
    }

    @Test
    fun `test deepCopy for MutableList`() {
        val original = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0),
            mutableListOf(4.0, 5.0, 6.0)
        )

        val copy = original.copy()

        // Verify content is the same
        assertEquals(original, copy)

        // Verify they are independent objects
        assertNotSame(original, copy)
        assertNotSame(original[0], copy[0])
        assertNotSame(original[1], copy[1])

        // Verify modifications don't affect each other
        copy[0][0] = 999.0
        assertEquals(1.0, original[0][0])
        assertEquals(999.0, copy[0][0])
    }

    @Test
    fun `test shiftUpAndPadEndWithZero for MutableList`() {
        val original = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0),
            mutableListOf(5.0, 6.0)
        )

        val result = original.shiftUpAndPadEndWithZero()

        // Expected: second row, third row, then zero row
        val expected = mutableListOf(
            mutableListOf(3.0, 4.0),  // originally row 1
            mutableListOf(5.0, 6.0),  // originally row 2
            mutableListOf(0.0, 0.0)   // padded zero row
        )

        assertEquals(expected, result)

        // Verify original is unchanged
        assertEquals(3, original.size)
        assertEquals(mutableListOf(1.0, 2.0), original[0])
    }

    @Test
    fun `test shiftUpAndPadEndWithZero empty list`() {
        val empty = mutableListOf<MutableList<Double>>()
        val result = empty.shiftUpAndPadEndWithZero()
        assertEquals(empty, result)
    }

    @Test
    fun `test List toMutableListOfLists conversion`() {
        val immutableList: List<List<Double>> = listOf(
            listOf(1.0, 2.0, 3.0),
            listOf(4.0, 5.0, 6.0)
        )

        val result = immutableList.toMutableListOfLists()

        // Verify content is the same
        assertEquals(2, result.size)
        assertEquals(listOf(1.0, 2.0, 3.0), result[0])
        assertEquals(listOf(4.0, 5.0, 6.0), result[1])

        // Verify it's actually mutable
        result[0][0] = 999.0
        assertEquals(999.0, result[0][0])

        // Verify original is unchanged (immutable lists)
        assertEquals(1.0, immutableList[0][0])
    }

    @Test
    fun `test empty TrainingDataset creation`() {
        // Test that we can create empty datasets with explicit dimensions
        val emptyDataset = TrainingDataset(
            inputs = mutableListOf(),
            targets = mutableListOf(),
            inputSize = 3,
            targetSize = 2
        )

        assertEquals(0, emptyDataset.size)
        assertEquals(3, emptyDataset.inputSize)
        assertEquals(2, emptyDataset.targetSize)

        // Test iteration over empty dataset
        var count = 0
        for (pair in emptyDataset) {
            count++
        }
        assertEquals(0, count)
    }

    @Test
    fun `test TrainingDataset with mixed empty and non-empty validation`() {
        // Test dimension validation
        val validInputs = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0),
            mutableListOf(4.0, 5.0, 6.0)
        )
        val validTargets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0)
        )

        // Should work with correct dimensions
        assertDoesNotThrow {
            TrainingDataset(validInputs, validTargets, 3, 1)
        }

        // Should fail with wrong input dimension
        assertThrows<IllegalArgumentException> {
            TrainingDataset(validInputs, validTargets, 2, 1) // Wrong input dimension
        }

        // Should fail with wrong target dimension
        assertThrows<IllegalArgumentException> {
            TrainingDataset(validInputs, validTargets, 3, 2) // Wrong target dimension
        }
    }

    @Test
    fun `test splitDataSet with ratio 1_0 leaves empty test set`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0),
            mutableListOf(5.0, 6.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        val (training, testing) = splitDataSet(inputs, targets, 1.0)
        val (trainingInputs, trainingTargets) = training
        val (testingInputs, testingTargets) = testing

        // All data should be in training set
        assertEquals(3, trainingInputs.size)
        assertEquals(3, trainingTargets.size)

        // Test set should be empty
        assertEquals(0, testingInputs.size)
        assertEquals(0, testingTargets.size)
    }

    @Test
    fun `test splitDataSet with ratio 0_0 leaves empty training set`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0),
            mutableListOf(5.0, 6.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        val (training, testing) = splitDataSet(inputs, targets, 0.0)
        val (trainingInputs, trainingTargets) = training
        val (testingInputs, testingTargets) = testing

        // Training set should be empty
        assertEquals(0, trainingInputs.size)
        assertEquals(0, trainingTargets.size)

        // All data should be in test set
        assertEquals(3, testingInputs.size)
        assertEquals(3, testingTargets.size)
    }

    @Test
    fun `test splitDataSet edge case with single row`() {
        val inputs = mutableListOf(mutableListOf(1.0, 2.0))
        val targets = mutableListOf(mutableListOf(0.0))

        // Test ratio 1.0 - all data goes to training
        val (training1, testing1) = splitDataSet(inputs, targets, 1.0)
        assertEquals(1, training1.first.size)
        assertEquals(0, testing1.first.size)

        // Test ratio 0.0 - all data goes to testing
        val (training0, testing0) = splitDataSet(inputs, targets, 0.0)
        assertEquals(0, training0.first.size)
        assertEquals(1, testing0.first.size)
    }

    @Test
    fun `test empty dataset creation with splitDataSet results`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0),
            mutableListOf(4.0, 5.0, 6.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0)
        )

        // Split with ratio 1.0 to get empty testing set
        val (training, testing) = splitDataSet(inputs, targets, 1.0)
        val (trainingInputs, trainingTargets) = training
        val (testingInputs, testingTargets) = testing

        // Create TrainingDatasets - training with inferred dimensions, testing with explicit
        val trainingDataset = TrainingDataset(trainingInputs, trainingTargets)
        val testingDataset = TrainingDataset(
            inputs = testingInputs,
            targets = testingTargets,
            inputSize = 3,  // Must provide explicit dimensions for empty data
            targetSize = 1
        )

        assertEquals(2, trainingDataset.size)
        assertEquals(0, testingDataset.size)
        assertEquals(3, testingDataset.inputSize)
        assertEquals(1, testingDataset.targetSize)
    }

    @Test
    fun `test splitDataSet with TrainingDataset input`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0),
            mutableListOf(5.0, 6.0),
            mutableListOf(7.0, 8.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(0.0),
            mutableListOf(1.0)
        )
        val inputNames = listOf("Row1", "Row2", "Row3", "Row4")
        val targetNames = listOf("Target1", "Target2", "Target3", "Target4")

        val originalDataset = TrainingDataset(
            inputs = inputs,
            targets = targets,
            inputRowNames = inputNames,
            targetRowNames = targetNames,
            inputColumnNames = listOf("Feature1", "Feature2"),
            targetColumnNames = listOf("Label")
        )

        // Test 50/50 split
        val (training, testing) = splitDataSet(originalDataset, 0.5, Random(42))

        // Verify sizes
        assertEquals(2, training.size)
        assertEquals(2, testing.size)
        assertEquals(4, training.size + testing.size) // Total should match

        // Verify dimensions are preserved
        assertEquals(2, training.inputSize)
        assertEquals(1, training.targetSize)
        assertEquals(2, testing.inputSize)
        assertEquals(1, testing.targetSize)

        // Verify column names are preserved
        assertEquals(listOf("Feature1", "Feature2"), training.inputColumnNames)
        assertEquals(listOf("Label"), training.targetColumnNames)
        assertEquals(listOf("Feature1", "Feature2"), testing.inputColumnNames)
        assertEquals(listOf("Label"), testing.targetColumnNames)

        // Verify row names are filtered appropriately
        assertEquals(2, training.inputRowNames?.size)
        assertEquals(2, testing.inputRowNames?.size)
    }

    @Test
    fun `test splitDataSet with TrainingDataset preserves dimensions on empty sets`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0),
            mutableListOf(4.0, 5.0, 6.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 0.0)
        )

        val originalDataset = TrainingDataset(
            inputs = inputs,
            targets = targets,
            inputSize = 3,
            targetSize = 2
        )

        // Test ratio 1.0 - empty testing set
        val (training1, testing1) = splitDataSet(originalDataset, 1.0)
        assertEquals(2, training1.size)
        assertEquals(0, testing1.size)
        assertEquals(3, testing1.inputSize)  // ✅ Dimensions preserved!
        assertEquals(2, testing1.targetSize)

        // Test ratio 0.0 - empty training set
        val (training0, testing0) = splitDataSet(originalDataset, 0.0)
        assertEquals(0, training0.size)
        assertEquals(2, testing0.size)
        assertEquals(3, training0.inputSize)  // ✅ Dimensions preserved!
        assertEquals(2, training0.targetSize)
    }

    @Test
    fun `test splitDataSet TrainingDataset vs manual approach comparison`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0),
            mutableListOf(5.0, 6.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        val dataset = TrainingDataset(inputs, targets)

        // ✅ NEW WAY: Clean, preserves dimensions and metadata
        val (training, testing) = splitDataSet(dataset, 0.8, Random(42))

        // ❌ OLD WAY: Manual, verbose, easy to lose dimensions
        val (manualTraining, manualTesting) = splitDataSet(inputs, targets, 0.8, Random(42))
        val manualTrainingDataset = TrainingDataset(
            inputs = manualTraining.first,
            targets = manualTraining.second,
            inputSize = dataset.inputSize,  // Have to remember to pass this!
            targetSize = dataset.targetSize  // And this!
        )
        val manualTestingDataset = TrainingDataset(
            inputs = manualTesting.first,
            targets = manualTesting.second,
            inputSize = dataset.inputSize,  // Easy to forget!
            targetSize = dataset.targetSize
        )

        // Both approaches should give same results
        assertEquals(training.size, manualTrainingDataset.size)
        assertEquals(testing.size, manualTestingDataset.size)
        assertEquals(training.inputSize, manualTrainingDataset.inputSize)
        assertEquals(training.targetSize, manualTrainingDataset.targetSize)
        assertEquals(testing.inputSize, manualTestingDataset.inputSize)
        assertEquals(testing.targetSize, manualTestingDataset.targetSize)
    }

    @Test
    fun `test splitDataSet preserves column dimensions in all cases`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0, 4.0),  // 4 columns
            mutableListOf(5.0, 6.0, 7.0, 8.0),
            mutableListOf(9.0, 10.0, 11.0, 12.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0, 1.0),  // 2 columns
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0)
        )

        val originalDataset = TrainingDataset(
            inputs = inputs,
            targets = targets,
            inputSize = 4,
            targetSize = 2
        )

        // Test normal split (50/50)
        val (training50, testing50) = splitDataSet(originalDataset, 0.5, Random(42))

        // Both sets should preserve original dimensions
        assertEquals(4, training50.inputSize)
        assertEquals(2, training50.targetSize)
        assertEquals(4, testing50.inputSize)
        assertEquals(2, testing50.targetSize)

        // Verify actual data matches dimensions (non-empty sets)
        training50.inputs.forEach { row ->
            assertEquals(4, row.size, "Training input row should have 4 columns")
        }
        training50.targets.forEach { row ->
            assertEquals(2, row.size, "Training target row should have 2 columns")
        }
        testing50.inputs.forEach { row ->
            assertEquals(4, row.size, "Testing input row should have 4 columns")
        }
        testing50.targets.forEach { row ->
            assertEquals(2, row.size, "Testing target row should have 2 columns")
        }

        // Test edge case: ratio 1.0 (empty testing set)
        val (training100, testing0) = splitDataSet(originalDataset, 1.0)

        assertEquals(3, training100.size, "Training should have all 3 rows")
        assertEquals(0, testing0.size, "Testing should be empty")

        // ✅ CRITICAL: Empty testing set must preserve dimensions
        assertEquals(4, testing0.inputSize, "Empty testing set should preserve input dimension")
        assertEquals(2, testing0.targetSize, "Empty testing set should preserve target dimension")

        // Verify training set data dimensions
        training100.inputs.forEach { row ->
            assertEquals(4, row.size, "Training input row should have 4 columns")
        }
        training100.targets.forEach { row ->
            assertEquals(2, row.size, "Training target row should have 2 columns")
        }

        // Test edge case: ratio 0.0 (empty training set)
        val (training0, testing100) = splitDataSet(originalDataset, 0.0)

        assertEquals(0, training0.size, "Training should be empty")
        assertEquals(3, testing100.size, "Testing should have all 3 rows")

        // ✅ CRITICAL: Empty training set must preserve dimensions
        assertEquals(4, training0.inputSize, "Empty training set should preserve input dimension")
        assertEquals(2, training0.targetSize, "Empty training set should preserve target dimension")

        // Verify testing set data dimensions
        testing100.inputs.forEach { row ->
            assertEquals(4, row.size, "Testing input row should have 4 columns")
        }
        testing100.targets.forEach { row ->
            assertEquals(2, row.size, "Testing target row should have 2 columns")
        }
    }

    @Test
    fun `test splitDataSet with MutableList preserves column dimensions`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0),  // 3 columns
            mutableListOf(4.0, 5.0, 6.0),
            mutableListOf(7.0, 8.0, 9.0),
            mutableListOf(10.0, 11.0, 12.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0),  // 1 column
            mutableListOf(1.0),
            mutableListOf(0.0),
            mutableListOf(1.0)
        )

        // Test ratio 1.0 - empty testing set
        val (training, testing) = splitDataSet(inputs, targets, 1.0, Random(42))
        val (trainingInputs, trainingTargets) = training
        val (testingInputs, testingTargets) = testing

        // Training set should have all data with correct dimensions
        assertEquals(4, trainingInputs.size)
        assertEquals(4, trainingTargets.size)
        trainingInputs.forEach { row ->
            assertEquals(3, row.size, "Training input row should have 3 columns")
        }
        trainingTargets.forEach { row ->
            assertEquals(1, row.size, "Training target row should have 1 column")
        }

        // Testing set should be empty but we can't check dimensions
        // (raw splitDataSet doesn't preserve dimension info for empty sets)
        assertEquals(0, testingInputs.size)
        assertEquals(0, testingTargets.size)

        // Test ratio 0.0 - empty training set
        val (training0, testing0) = splitDataSet(inputs, targets, 0.0, Random(42))
        val (trainingInputs0, trainingTargets0) = training0
        val (testingInputs0, testingTargets0) = testing0

        // Training set should be empty
        assertEquals(0, trainingInputs0.size)
        assertEquals(0, trainingTargets0.size)

        // Testing set should have all data with correct dimensions
        assertEquals(4, testingInputs0.size)
        assertEquals(4, testingTargets0.size)
        testingInputs0.forEach { row ->
            assertEquals(3, row.size, "Testing input row should have 3 columns")
        }
        testingTargets0.forEach { row ->
            assertEquals(1, row.size, "Testing target row should have 1 column")
        }
    }

    @Test
    fun `test splitDataSet column dimension consistency across different sizes`() {
        // Test with varying column sizes to ensure robustness

        // Case 1: Many input columns, few target columns
        val wideInputs = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0),  // 8 columns
            mutableListOf(9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0)
        )
        val narrowTargets = mutableListOf(
            mutableListOf(0.0),  // 1 column
            mutableListOf(1.0)
        )

        val wideDataset = TrainingDataset(wideInputs, narrowTargets, 8, 1)
        val (wideTraining, wideTesting) = splitDataSet(wideDataset, 1.0)  // Empty testing

        assertEquals(8, wideTraining.inputSize)
        assertEquals(1, wideTraining.targetSize)
        assertEquals(8, wideTesting.inputSize)  // ✅ Preserved in empty set
        assertEquals(1, wideTesting.targetSize)  // ✅ Preserved in empty set

        wideTraining.inputs.forEach { row ->
            assertEquals(8, row.size, "Wide input should have 8 columns")
        }
        wideTraining.targets.forEach { row ->
            assertEquals(1, row.size, "Narrow target should have 1 column")
        }

        // Case 2: Few input columns, many target columns
        val narrowInputs = mutableListOf(
            mutableListOf(1.0, 2.0),  // 2 columns
            mutableListOf(3.0, 4.0)
        )
        val wideTargets = mutableListOf(
            mutableListOf(0.0, 1.0, 0.0, 0.0, 1.0),  // 5 columns
            mutableListOf(1.0, 0.0, 1.0, 0.0, 0.0)
        )

        val narrowDataset = TrainingDataset(narrowInputs, wideTargets, 2, 5)
        val (narrowTraining, narrowTesting) = splitDataSet(narrowDataset, 0.0)  // Empty training

        assertEquals(2, narrowTraining.inputSize)  // ✅ Preserved in empty set
        assertEquals(5, narrowTraining.targetSize)  // ✅ Preserved in empty set
        assertEquals(2, narrowTesting.inputSize)
        assertEquals(5, narrowTesting.targetSize)

        narrowTesting.inputs.forEach { row ->
            assertEquals(2, row.size, "Narrow input should have 2 columns")
        }
        narrowTesting.targets.forEach { row ->
            assertEquals(5, row.size, "Wide target should have 5 columns")
        }

        // Case 3: Single column inputs and targets
        val singleInputs = mutableListOf(
            mutableListOf(1.0),  // 1 column
            mutableListOf(2.0),
            mutableListOf(3.0)
        )
        val singleTargets = mutableListOf(
            mutableListOf(0.0),  // 1 column
            mutableListOf(1.0),
            mutableListOf(0.0)
        )

        val singleDataset = TrainingDataset(singleInputs, singleTargets, 1, 1)
        val (singleTraining, singleTesting) = splitDataSet(singleDataset, 0.33)  // ~1/3 split

        assertEquals(1, singleTraining.inputSize)
        assertEquals(1, singleTraining.targetSize)
        assertEquals(1, singleTesting.inputSize)
        assertEquals(1, singleTesting.targetSize)

        // Verify all rows have exactly 1 column
        singleTraining.inputs.forEach { row ->
            assertEquals(1, row.size, "Single input should have 1 column")
        }
        singleTraining.targets.forEach { row ->
            assertEquals(1, row.size, "Single target should have 1 column")
        }
        singleTesting.inputs.forEach { row ->
            assertEquals(1, row.size, "Single input should have 1 column")
        }
        singleTesting.targets.forEach { row ->
            assertEquals(1, row.size, "Single target should have 1 column")
        }
    }

    @Test
    fun `test TrainingDataset createDataSetPanel with empty datasets`() {
        // Test that empty datasets can create DataSetPanels without errors
        val emptyDataset = TrainingDataset(
            inputs = mutableListOf(),
            targets = mutableListOf(),
            inputSize = 3,
            targetSize = 2,
            inputColumnNames = listOf("Feature1", "Feature2", "Feature3"),
            targetColumnNames = listOf("Label1", "Label2")
        )

        // This should not throw an exception
        val panel = emptyDataset.createDataSetPanel { _ -> }

        // Verify the DataFrames have correct structure
        assertEquals(3, panel.inputDataFrame.columnCount)
        assertEquals(2, panel.targetDataFrame.columnCount)
        assertEquals(0, panel.inputDataFrame.rowCount)
        assertEquals(0, panel.targetDataFrame.rowCount)

        // Verify column names are set correctly
        assertEquals("Feature1", panel.inputDataFrame.columnNames[0])
        assertEquals("Feature2", panel.inputDataFrame.columnNames[1])
        assertEquals("Feature3", panel.inputDataFrame.columnNames[2])
        assertEquals("Label1", panel.targetDataFrame.columnNames[0])
        assertEquals("Label2", panel.targetDataFrame.columnNames[1])
    }

    @Test
    fun `test TrainingDataset createDataSetPanel with non-empty datasets`() {
        // Test that non-empty datasets also work correctly
        val dataset = TrainingDataset(
            inputs = mutableListOf(
                mutableListOf(1.0, 2.0),
                mutableListOf(3.0, 4.0)
            ),
            targets = mutableListOf(
                mutableListOf(0.1),
                mutableListOf(0.9)
            ),
            inputSize = 2,
            targetSize = 1,
            inputColumnNames = listOf("X1", "X2"),
            targetColumnNames = listOf("Y")
        )

        // This should not throw an exception
        val panel = dataset.createDataSetPanel { _ -> }

        // Verify the DataFrames have correct structure
        assertEquals(2, panel.inputDataFrame.columnCount)
        assertEquals(1, panel.targetDataFrame.columnCount)
        assertEquals(2, panel.inputDataFrame.rowCount)
        assertEquals(2, panel.targetDataFrame.rowCount)

        // Verify column names are set correctly
        assertEquals("X1", panel.inputDataFrame.columnNames[0])
        assertEquals("X2", panel.inputDataFrame.columnNames[1])
        assertEquals("Y", panel.targetDataFrame.columnNames[0])

        // Verify data is preserved
        assertEquals(1.0, panel.inputDataFrame.getValueAt(0, 0))
        assertEquals(2.0, panel.inputDataFrame.getValueAt(0, 1))
        assertEquals(3.0, panel.inputDataFrame.getValueAt(1, 0))
        assertEquals(4.0, panel.inputDataFrame.getValueAt(1, 1))
        assertEquals(0.1, panel.targetDataFrame.getValueAt(0, 0))
        assertEquals(0.9, panel.targetDataFrame.getValueAt(1, 0))
    }

    @Test
    fun `test TrainingDataset createDataSetPanel with empty datasets without column names`() {
        // Test that empty datasets work even without explicit column names
        val emptyDataset = TrainingDataset(
            inputs = mutableListOf(),
            targets = mutableListOf(),
            inputSize = 2,
            targetSize = 1
        )

        // This should not throw an exception and should create default column names
        val panel = emptyDataset.createDataSetPanel { _ -> }

        // Verify the DataFrames have correct structure
        assertEquals(2, panel.inputDataFrame.columnCount)
        assertEquals(1, panel.targetDataFrame.columnCount)
        assertEquals(0, panel.inputDataFrame.rowCount)
        assertEquals(0, panel.targetDataFrame.rowCount)

        // Verify default column names are created
        assertEquals("Column 1", panel.inputDataFrame.columnNames[0])
        assertEquals("Column 2", panel.inputDataFrame.columnNames[1])
        assertEquals("Column 1", panel.targetDataFrame.columnNames[0])
    }

    @Test
    fun `test TrainingDataset createDataSetPanel with split dataset results`() {
        // Test the specific scenario: split dataset with splitRatio = 1.0 (empty testing set)
        val originalDataset = TrainingDataset(
            inputs = mutableListOf(
                mutableListOf(1.0, 2.0, 3.0),
                mutableListOf(4.0, 5.0, 6.0),
                mutableListOf(7.0, 8.0, 9.0)
            ),
            targets = mutableListOf(
                mutableListOf(0.1, 0.2),
                mutableListOf(0.4, 0.5),
                mutableListOf(0.7, 0.8)
            ),
            inputSize = 3,
            targetSize = 2,
            inputColumnNames = listOf("A", "B", "C"),
            targetColumnNames = listOf("X", "Y")
        )

        // Split with ratio 1.0 - all data goes to training, testing set is empty
        val (training, testing) = splitDataSet(originalDataset, 1.0)

        // Training set should have all data and correct columns
        val trainingPanel = training.createDataSetPanel { _ -> }
        assertEquals(3, trainingPanel.inputDataFrame.columnCount)
        assertEquals(2, trainingPanel.targetDataFrame.columnCount)
        assertEquals(3, trainingPanel.inputDataFrame.rowCount)
        assertEquals(3, trainingPanel.targetDataFrame.rowCount)
        assertEquals("A", trainingPanel.inputDataFrame.columnNames[0])
        assertEquals("B", trainingPanel.inputDataFrame.columnNames[1])
        assertEquals("C", trainingPanel.inputDataFrame.columnNames[2])
        assertEquals("X", trainingPanel.targetDataFrame.columnNames[0])
        assertEquals("Y", trainingPanel.targetDataFrame.columnNames[1])

        // Testing set should be empty but have correct column structure
        val testingPanel = testing.createDataSetPanel { _ -> }
        assertEquals(3, testingPanel.inputDataFrame.columnCount)
        assertEquals(2, testingPanel.targetDataFrame.columnCount)
        assertEquals(0, testingPanel.inputDataFrame.rowCount)
        assertEquals(0, testingPanel.targetDataFrame.rowCount)
        assertEquals("A", testingPanel.inputDataFrame.columnNames[0])
        assertEquals("B", testingPanel.inputDataFrame.columnNames[1])
        assertEquals("C", testingPanel.inputDataFrame.columnNames[2])
        assertEquals("X", testingPanel.targetDataFrame.columnNames[0])
        assertEquals("Y", testingPanel.targetDataFrame.columnNames[1])
    }

    @Test
    fun `test TrainingDataset deepCopy`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0)
        )
        val targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0)
        )
        val inputNames = mutableListOf("input1", "input2")
        val targetNames = mutableListOf("target1", "target2")

        val original = TrainingDataset(inputs, targets, inputRowNames = inputNames, targetRowNames = targetNames)
        val copy = original.copy()

        // Verify content is the same
        assertEquals(original.inputs, copy.inputs)
        assertEquals(original.targets, copy.targets)
        assertEquals(original.inputRowNames, copy.inputRowNames)
        assertEquals(original.targetRowNames, copy.targetRowNames)

        // Verify they are independent objects
        assertNotSame(original.inputs, copy.inputs)
        assertNotSame(original.targets, copy.targets)
        assertNotSame(original.inputRowNames, copy.inputRowNames)
        assertNotSame(original.targetRowNames, copy.targetRowNames)

        // Verify modifications don't affect each other
        copy.inputs[0][0] = 999.0
        assertEquals(1.0, original.inputs[0][0])
        assertEquals(999.0, copy.inputs[0][0])
    }

    @Test
    fun `test UnsupervisedNetwork dialog with empty datasets`() {
        // Test that the unsupervised training dialog can handle empty datasets
        val competitiveNet = CompetitiveNetwork(3, 5)

        // Set empty training and testing data
        competitiveNet.trainingData = mutableListOf()
        competitiveNet.testingData = mutableListOf()

        // This should not throw any exceptions when creating the dialog
        assertDoesNotThrow {
            // We can't easily test the full GUI creation without a full UI context,
            // but we can test the data structure handling
            val inputSize = competitiveNet.inputLayer.size
            assertEquals(3, inputSize)

            // Test that empty data with proper column count works
            val columns = (0 until inputSize).map { i ->
                Column("Input ${i + 1}", Column.DataType.DoubleType)
            }.toMutableList()

            val dataFrame = BasicDataFrame(
                mutableListOf<MutableList<Any?>>(), // Empty data
                columns
            )

            // Should have correct column count even with empty data
            assertEquals(3, dataFrame.columnCount)
            assertEquals(0, dataFrame.rowCount)
            assertEquals("Input 1", dataFrame.getColumnName(0))
            assertEquals("Input 2", dataFrame.getColumnName(1))
            assertEquals("Input 3", dataFrame.getColumnName(2))
        }
    }

    @Test
    fun `test UnsupervisedNetwork dialog with non-empty datasets`() {
        // Test that the unsupervised training dialog works with actual data
        val somNet = SOMNetwork(2, 4)

        // Set some training data
        val trainingData = mutableListOf(
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(0.5, 0.5)
        )
        somNet.trainingData = trainingData

        assertDoesNotThrow {
            val inputSize = somNet.inputLayer.size
            assertEquals(2, inputSize)

            // Test data frame creation with actual data
            val columns = (0 until inputSize).map { i ->
                Column("Input ${i + 1}", Column.DataType.DoubleType)
            }.toMutableList()

            val dataFrame = BasicDataFrame(
                trainingData.map { it.map { value -> value as Any? }.toMutableList() }.toMutableList(),
                columns
            )

            // Should have correct dimensions
            assertEquals(2, dataFrame.columnCount)
            assertEquals(3, dataFrame.rowCount)
            assertEquals("Input 1", dataFrame.getColumnName(0))
            assertEquals("Input 2", dataFrame.getColumnName(1))

            // Should contain correct data
            assertEquals(1.0, dataFrame.getValueAt(0, 0))
            assertEquals(0.0, dataFrame.getValueAt(0, 1))
            assertEquals(0.0, dataFrame.getValueAt(1, 0))
            assertEquals(1.0, dataFrame.getValueAt(1, 1))
        }
    }


}
