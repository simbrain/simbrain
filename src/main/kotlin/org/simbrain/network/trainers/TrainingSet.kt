package org.simbrain.network.trainers

import org.simbrain.util.clip

class TrainingSet {

    private val _allSet: MutableList<Pair<MutableList<Double>, MutableList<Double>>> = ArrayList()

    val allSet: List<Pair<List<Double>, List<Double>>> = _allSet

    private var shouldResampleIndices = true
        set(value) {
            field = value
            shouldRefreshList = shouldRefreshList || value
        }

    var percentValidation: Double = 0.25
        set(value) {
            field = value.clip(0.0..1.0)
            shouldResampleIndices = true
        }

    private var shouldRefreshTrainingSetList = true
    private var shouldRefreshTestingSetList = true
    private var shouldRefreshList
        get() = shouldRefreshTrainingSetList && shouldRefreshTestingSetList
        set(value) {
            shouldRefreshTrainingSetList = value
            shouldRefreshTestingSetList = value
        }

    private lateinit var trainingSetIndices: List<Int>
    private lateinit var _trainingSet: List<Pair<List<Double>, List<Double>>>
    val trainingSet: List<Pair<List<Double>, List<Double>>>
        get() {
            if (shouldResampleIndices) {
                resampleIndices()
            }
            if (shouldRefreshTrainingSetList) {
                _trainingSet = trainingSetIndices.map { allSet[it] }
                shouldRefreshTestingSetList = false
            }
            return _trainingSet
        }

    private lateinit var testingSetIndices: List<Int>
    private lateinit var _testingSet: List<Pair<List<Double>, List<Double>>>
    val testingSet: List<Pair<List<Double>, List<Double>>>
        get() {
            if (shouldResampleIndices) {
                resampleIndices()
            }
            if (shouldRefreshTestingSetList) {
                _testingSet = testingSetIndices.map { allSet[it] }
                shouldRefreshTestingSetList = false
            }
            return _testingSet
        }

    private fun resampleIndices() {
        val indices = (0..allSet.lastIndex).shuffled()
        val count = (percentValidation * indices.size).toInt()
        trainingSetIndices = indices.take(count)
        testingSetIndices = indices.takeLast(_allSet.size - count)
        shouldRefreshList = true
    }

    operator fun set(index: Int, values: Pair<List<Double>, List<Double>>) {
        val (input, target) = values
        _allSet[index] = Pair(ArrayList(input), ArrayList(target))
        shouldRefreshList = true
    }

    operator fun set(type: Type, index: Int, values: List<Double>) {
        val (input, target) = _allSet[index]
        _allSet[index] = when (type) {
            Type.Input -> Pair(ArrayList(values), target)
            Type.Target -> Pair(input, ArrayList(values))
        }
        shouldRefreshList = true
    }

    fun add(pair: Pair<List<Double>, List<Double>>) {
        val (input, target) = pair
        _allSet.add(Pair(ArrayList(input), ArrayList(target)))
    }

    fun add(index: Int, pair: Pair<List<Double>, List<Double>>) {
        val (input, target) = pair
        _allSet.add(index, Pair(ArrayList(input), ArrayList(target)))
    }

    fun addAll(values: List<Pair<List<Double>, List<Double>>>) {
        values.forEach { add(it) }
    }

    fun addAll(inputs: List<List<Double>>, targets: List<List<Double>>) {
        (inputs zip targets).forEach { add(it) }
    }

    enum class Type {
        Input, Target
    }


}