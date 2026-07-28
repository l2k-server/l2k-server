package org.l2kserver.game.model.actor.npc.ai.standard;

import org.jetbrains.annotations.NotNull;
import org.l2kserver.game.model.actor.npc.ai.AiDesire;
import org.l2kserver.game.model.actor.npc.ai.NpcAi;
import org.l2kserver.game.model.actor.npc.NpcInstance;
import org.l2kserver.game.model.actor.position.Position;
import org.l2kserver.game.model.utils.GeomUtils;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple AI, that makes NPC wandering around if it is idle, or attack actor, who attacked him
 */
public class WarriorAi implements NpcAi {

    private final NpcInstance self;
    private final int wanderingDistance;

    public WarriorAi(NpcInstance self, int wanderingDistance) {
        this.self = self;
        this.wanderingDistance = wanderingDistance;
    }

    @NotNull
    @Override
    public List<AiDesire> onTick() {
        final var desires = new ArrayList<AiDesire>();

        // 5% chance to start wandering
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            final var spawnPosition = self.getSpawnedAt().getSpawnPosition();
            final var spawnZone = self.getSpawnedAt().getSpawnZone();
            Shape wanderingArea;
            if (spawnPosition != null) {
                wanderingArea = new Ellipse2D.Float(
                    spawnPosition.getX() - wanderingDistance,
                    spawnPosition.getY() - wanderingDistance,
                    wanderingDistance * 2,
                    wanderingDistance * 2
                );
            }
            else if (spawnZone != null) {
                wanderingArea = spawnZone.getShape();
            }
            else return desires;

            final var targetPoint = GeomUtils.getRandomPoint(wanderingArea);
            final var targetPosition = new Position(
                (int) targetPoint.getX(),
                (int) targetPoint.getY(),
                self.getPosition().getZ()
            );

            desires.add(new AiDesire.Move(targetPosition));
        }

        return desires;
    }
}
