package org.l2kserver.game.model.position

/**
 * Heading - integer from 0 to 65535,
 * where 0 is east direction, 16384 - south, 32768 - west, 49152 - north
 */
@JvmInline
value class Heading(val value: UShort) {
    constructor(): this(0u)
    constructor(value: Int): this(value.toUShort())
    constructor(value: Double): this(value.toInt().toUShort())

    operator fun minus(other: Heading) = Heading(this.value.toInt() - other.value.toInt())
    operator fun plus(other: Int) = Heading((this.value.toInt() + other).toUShort())

    fun toShort() = this.value.toShort()
    fun toInt() = this.value.toInt()
}
