package com.blockpuzzle.rotate.domain

/** Connectivity check for hand-drawn shapes in the level constructor: cells count as touching if they share an edge OR a corner. */
object ShapeConnectivity {
    private val NEIGHBOR_OFFSETS = listOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1, 0 to 1,
        1 to -1, 1 to 0, 1 to 1
    )

    fun isConnected(cells: Set<Coordinate>): Boolean {
        if (cells.isEmpty()) return false
        val start = cells.first()
        val visited = mutableSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for ((dr, dc) in NEIGHBOR_OFFSETS) {
                val neighbor = Coordinate(current.row + dr, current.col + dc)
                if (neighbor in cells && visited.add(neighbor)) {
                    queue.add(neighbor)
                }
            }
        }
        return visited.size == cells.size
    }
}
