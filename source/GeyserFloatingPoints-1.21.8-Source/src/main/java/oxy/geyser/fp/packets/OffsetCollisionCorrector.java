package oxy.geyser.fp.packets;

import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;

/**
 * Re-applies Geyser's collision correction at the real Java position after GFP
 * has restored the session offset. Without this, piston pushes can be checked
 * against the spoofed local origin and the server corrects the player there.
 */
final class OffsetCollisionCorrector {
    private OffsetCollisionCorrector() {
    }

    static Vector3d correct(GeyserSession session, Vector3d realPosition) {
        if (!session.getGeyser().getWorldManager().hasOwnChunkCache()) {
            return realPosition;
        }

        BoundingBox box = session.getCollisionManager().getPlayerBoundingBox().clone();
        box.setMiddleX(realPosition.getX());
        box.setMiddleY(realPosition.getY() + box.getSizeY() / 2.0d);
        box.setMiddleZ(realPosition.getZ());

        BlockPositionIterator iterator = CollisionManager.collidableBlocksIterator(session, box);
        int[] blocks = session.getGeyser().getWorldManager().getBlocksAt(session, iterator);
        for (iterator.reset(); iterator.hasNext(); iterator.next()) {
            BlockCollision collision = BlockUtils.getCollision(blocks[iterator.getIteration()]);
            if (collision != null) {
                collision.correctPosition(session, iterator.getX(), iterator.getY(), iterator.getZ(), box);
            }
        }
        return box.getBottomCenter();
    }
}
