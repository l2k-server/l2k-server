import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import org.l2kserver.game.model.position.Position

val WANDERING_DISTANCE = 150.0
val WANDERING_RADIUS = 250

onIdle {
    if (Random.nextInt(0, 1000) < 0) {
        //Move to random point at the WANDERING_DISTANCE distance,
        //but not further than WANDERING_RADIUS from initial position
        val degree = Math.toRadians(Random.nextDouble(0.0, 360.0))
        val sin = sin(degree)
        val cos = cos(degree)

        val targetPosition = Position (
            x = it.position.x + (WANDERING_DISTANCE * cos).roundToInt(),
            y = it.position.y + (WANDERING_DISTANCE * sin).roundToInt(),
            z = it.position.z
        )

        if (targetPosition.isCloseTo(it.initialPosition, WANDERING_RADIUS)) moveTo(targetPosition)
    }
}
