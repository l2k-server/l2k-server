package org.l2kserver.game.model.extensions

/** Transforms Boolean to Byte. True = 1, False = 0 */
fun Boolean.toByte(): Byte = if (this) 1 else 0

/** Transforms Boolean to Int. True = 1, False = 0 */
fun Boolean.toInt() = if (this) 1 else 0

/** Performs given [action] on each element if it is not null */
inline fun <T> Array<T>.forEachNotNull(action: (T) -> Unit) = forEach {
    if (it != null) action(it)
}

/** Performs given [action] on each element matching given [predicate] */
inline fun <T> Iterable<T>.forEachMatching(predicate: (T) -> Boolean, action: (T) -> Unit) = forEach {
    if (predicate(it)) action(it)
}

/** Performs given [action] on each element, which is instance of [R] */
inline fun <reified R> Iterable<*>.forEachInstance(action: (R) -> Unit) = forEach {
    if (it is R) action(it)
}

/** Performs given [action] on each element, which is instance of [R] matching given [predicate] */
inline fun <reified R> Iterable<*>.forEachInstanceMatching(
    predicate: (R) -> Boolean,
    action: (R) -> Unit
) = forEach { if (it is R && predicate(it)) action(it) }

/** Returns a list containing only elements of type [R] matching the given [predicate]. */
@Suppress("UNCHECKED_CAST")
inline fun <reified R> Iterable<*>.filterIsInstanceAnd(predicate: (R) -> Boolean): List<R> =
    this.filter { it is R && predicate(it) } as List<R>

/** Checks if all the items in this iterable are unique by some property */
inline fun <reified T, reified R> Iterable<T>.allUniqueBy(transform: (T) -> R): Boolean {
    val hashset = mutableSetOf<R>()
    return this.all { hashset.add(transform(it)) }
}
