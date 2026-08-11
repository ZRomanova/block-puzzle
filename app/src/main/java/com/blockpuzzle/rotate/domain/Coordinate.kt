package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/** Relative or absolute (row, col) position on the board / within a shape. */
@Serializable
data class Coordinate(val row: Int, val col: Int)
