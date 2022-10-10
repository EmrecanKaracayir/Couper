package algorithm.data

import algorithm.model.SidedFixture

class Node(
    val sidedFixture: SidedFixture,
    var downNode: Node? = null,
    val nodeOrder: Int = -1
) {
    fun hasDownNode(): Boolean = downNode != null
}