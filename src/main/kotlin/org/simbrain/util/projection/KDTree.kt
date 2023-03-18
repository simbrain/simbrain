package org.simbrain.util.projection

import java.util.*
import kotlin.math.abs

class KDTree(private val dimension: Int) : Iterable<DataPoint2> {

    private var pointCount = 0
    val size get() = pointCount

    private data class Node(
        var point: DataPoint2,
        val axis: Int,
        var left: Node? = null,
        var right: Node? = null
    )

    private var root: Node? = null

    private fun buildTree(points: List<DataPoint2>, depth: Int): Node? {
        if (points.isEmpty()) {
            return null
        }

        val axis = depth % dimension
        val sortedPoints = points.sortedBy { it.upstairsPoint[axis] }
        val median = sortedPoints.size / 2

        return Node(
            point = sortedPoints[median],
            axis = axis,
            left = buildTree(sortedPoints.subList(0, median), depth + 1),
            right = buildTree(sortedPoints.subList(median + 1, sortedPoints.size), depth + 1)
        )
    }

    fun insert(point: DataPoint2) {
        root = insert(root, point, 0)
        pointCount++
    }

    private fun insert(node: Node?, point: DataPoint2, depth: Int): Node {
        if (node == null) {
            return Node(point, depth % dimension)
        }

        val axis = node.axis
        val currentNode = node.point.upstairsPoint[axis]
        val newPoint = point.upstairsPoint[axis]

        if (newPoint < currentNode) {
            node.left = insert(node.left, point, depth + 1)
        } else {
            node.right = insert(node.right, point, depth + 1)
        }

        return node
    }

    private data class SearchNode(
        val node: Node,
        val distance: Double
    ) : Comparable<SearchNode> {
        override fun compareTo(other: SearchNode): Int {
            return -distance.compareTo(other.distance)
        }
    }

    fun findClosestNPoint(target: DataPoint2) = findClosestNPoints(target, 1).firstOrNull()

    fun findClosestNPoints(target: DataPoint2, n: Int): List<DataPoint2> {
        val closestNPoints = PriorityQueue<SearchNode>()

        fun searchClosest(node: Node?, depth: Int) {
            if (node == null) {
                return
            }

            val distance = target.euclideanDistance(node.point)
            if (closestNPoints.size < n || distance < closestNPoints.peek().distance) {
                if (closestNPoints.size == n) {
                    closestNPoints.poll()
                }
                closestNPoints.offer(SearchNode(node, distance))
            }

            val axis = node.axis
            val diff = target.upstairsPoint[axis] - node.point.upstairsPoint[axis]
            val (nearNode, farNode) = if (diff < 0) {
                node.left to node.right
            } else {
                node.right to node.left
            }

            searchClosest(nearNode, depth + 1)

            if (abs(diff) < closestNPoints.peek().distance || closestNPoints.size < n) {
                searchClosest(farNode, depth + 1)
            }
        }

        searchClosest(root, 0)

        return closestNPoints.map { it.node.point }
    }

    private fun inOrderTraversal(node: Node?, action: (DataPoint2) -> Unit) {
        if (node == null) return
        inOrderTraversal(node.left, action)
        action(node.point)
        inOrderTraversal(node.right, action)
    }

    override fun iterator(): Iterator<DataPoint2> {
        val stack = Stack<Node>()
        var current: Node? = root

        while (current != null) {
            stack.push(current)
            current = current.left
        }

        return object : Iterator<DataPoint2> {
            override fun hasNext(): Boolean {
                return stack.isNotEmpty()
            }

            override fun next(): DataPoint2 {
                if (!hasNext()) throw NoSuchElementException()

                val nextNode = stack.pop()
                current = nextNode.right

                while (current != null) {
                    stack.push(current)
                    current = current?.left
                }

                return nextNode.point
            }
        }
    }

    private fun findMinimum(node: Node?, targetAxis: Int, searchAxis: Int): Node? {
        if (node == null) return null
        if (searchAxis == targetAxis) {
            return findMinimum(node.left, targetAxis, (searchAxis + 1) % dimension) ?: node
        }

        val leftMin = findMinimum(node.left, targetAxis, (searchAxis + 1) % dimension)
        val rightMin = findMinimum(node.right, targetAxis, (searchAxis + 1) % dimension)

        return listOfNotNull(node, leftMin, rightMin)
            .minByOrNull { it.point.upstairsPoint[targetAxis] }
    }

    fun delete(target: DataPoint2): Boolean {
        return delete(null, root, target, 0) != null
    }

    private fun delete(parent: Node?, node: Node?, target: DataPoint2, depth: Int): Node? {
        if (node == null) {
            return null
        }

        val axis = depth % dimension

        if (node.point.upstairsPoint.contentEquals(target.upstairsPoint)) {
            pointCount--
            if (node.right != null) {
                val minNode = findMinimum(node.right, axis, (depth + 1) % dimension)!!
                node.point = minNode.point
                node.right = delete(node, node.right, minNode.point, depth + 1)
            } else if (node.left != null) {
                if (parent == null) {
                    root = node.left
                } else {
                    if (parent.left == node) {
                        parent.left = node.left
                    } else {
                        parent.right = node.left
                    }
                }
                return node.left
            } else {
                if (parent == null) {
                    root = null
                } else {
                    if (parent.left == node) {
                        parent.left = null
                    } else {
                        parent.right = null
                    }
                }
                return null
            }
        } else {
            if (target.upstairsPoint[axis] < node.point.upstairsPoint[axis]) {
                delete(node, node.left, target, depth + 1)
            } else {
                delete(node, node.right, target, depth + 1)
            }
        }

        return node
    }

}

fun main() {
    val points = listOf(
        DataPoint2(doubleArrayOf(9.0, 9.0)),
        DataPoint2(doubleArrayOf(8.0, 8.0)),
        DataPoint2(doubleArrayOf(11.0, 12.0)),
        DataPoint2(doubleArrayOf(6.0, 12.0)),
        DataPoint2(doubleArrayOf(-9.0, 1.0)),
        DataPoint2(doubleArrayOf(2.0, -7.0))
    )

    val kdTree = KDTree(2)
    for (point in points) {
        kdTree.insert(point)
    }

    val searchPoint = DataPoint2(doubleArrayOf(10.0, 10.0))
    val closestPoints = kdTree.findClosestNPoints(searchPoint, 3)
    closestPoints.forEach { point ->
        println("Closest point: (${point.upstairsPoint[0]}, ${point.upstairsPoint[1]})")
    }

    println("Before deletion:")
    for (point in kdTree) {
        println("Point: (${point.upstairsPoint[0]}, ${point.upstairsPoint[1]})")
    }

    val pointToDelete = DataPoint2(doubleArrayOf(6.0, 12.0))
    if (kdTree.delete(pointToDelete)) {
        println("Deleted point: (${pointToDelete.upstairsPoint[0]}, ${pointToDelete.upstairsPoint[1]})")
    } else {
        println("Point not found: (${pointToDelete.upstairsPoint[0]}, ${pointToDelete.upstairsPoint[1]})")
    }

    println("After deletion:")
    for (point in kdTree) {
        println("Point: (${point.upstairsPoint[0]}, ${point.upstairsPoint[1]})")
    }

}
