package oxy.geyser.fp.session;

import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityCache;
import org.geysermc.geyser.session.cache.TeleportCache;
import org.geysermc.geyser.session.cache.WorldBorder;
import oxy.geyser.fp.GeyserFloatingPoints;
import oxy.geyser.fp.session.cache.ChunkCache;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

/** Per-Bedrock-session local origin. All non-zero offsets are chunk-aligned. */
public class GeyserFPUser {
    public static final String SHOW_COORDINATES_GAME_RULE = "showcoordinates";
    private final GeyserSession session;
    private boolean positionShown = GeyserFloatingPoints.config().showPositionByDefault();
    private final ChunkCache chunkCache = new ChunkCache(this);
    private Vector3i offset = Vector3i.ZERO;
    public Vector3i prevPosition = Vector3i.ZERO;
    private boolean worldBorderReflectionWarningLogged;
    private boolean entityReflectionWarningLogged;
    public GeyserFPUser(GeyserSession session) { this.session = session; }
    public GeyserSession session() { return session; }
    public boolean shouldShowPosition() { return positionShown; }
    public void setPositionShown(boolean shown) { this.positionShown = shown; }
    public ChunkCache chunkCache() { return chunkCache; }
    public Vector3i offset() { return offset; }

    public void offset(Vector3i target, boolean teleport) {
        if ((target.getX() & 15) != 0 || (target.getZ() & 15) != 0 || target.getY() != 0) {
            throw new IllegalArgumentException("Offset must have 16-block-aligned X/Z and Y=0");
        }
        if (target.equals(offset)) return;
        SessionPlayerEntity entity = session.getPlayerEntity();
        entity.setPosition(entity.position().add(offset.toFloat()).sub(target.toFloat()));
        if (teleport) {
            entity.moveAbsolute(entity.position(), entity.getYaw(), entity.getPitch(), entity.isOnGround(), true);
            session.setUnconfirmedTeleport(new TeleportCache(session,
                    entity.position(), entity.getPitch(), entity.getYaw(), 0));
        }
        try {
            Field centerField = WorldBorder.class.getDeclaredField("center");
            centerField.setAccessible(true);
            Vector2d center = (Vector2d) centerField.get(session.getWorldBorder());
            WorldBorder border = session.getWorldBorder();
            border.setCenter(center.add(offset.getX(), offset.getZ()).sub(target.getX(), target.getZ()));
            border.update();
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!worldBorderReflectionWarningLogged) {
                worldBorderReflectionWarningLogged = true;
                session.getGeyser().getLogger().warning("GeyserFloatingPoints could not shift the world border for "
                        + session.bedrockUsername() + ": " + e.getClass().getSimpleName());
            }
        }
        try {
            Field entitiesField = EntityCache.class.getDeclaredField("entities");
            entitiesField.setAccessible(true);
            Object entities = entitiesField.get(session.getEntityCache());
            Method valuesMethod = entities.getClass().getDeclaredMethod("values");
            valuesMethod.setAccessible(true);
            @SuppressWarnings("unchecked")
            Collection<Entity> values = (Collection<Entity>) valuesMethod.invoke(entities);
            for (Entity other : values) {
                if (other == entity) continue;
                other.setPosition(other.position().add(offset.toFloat()).sub(target.toFloat()));
                other.despawnEntity();
                other.spawnEntity();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!entityReflectionWarningLogged) {
                entityReflectionWarningLogged = true;
                session.getGeyser().getLogger().warning("GeyserFloatingPoints could not shift cached entities for "
                        + session.bedrockUsername() + ": " + e.getClass().getSimpleName());
            }
        }
        boolean wasHiding = !offset.equals(Vector3i.ZERO);
        offset = target;
        if (session.isSpawned()) {
            chunkCache.sendChunksWithOffset();
        }
        boolean isHiding = !offset.equals(Vector3i.ZERO);
        if (wasHiding != isHiding) session.sendGameRule(SHOW_COORDINATES_GAME_RULE, shouldShowCoordinates());
    }

    private boolean shouldShowCoordinates() {
        return offset.equals(Vector3i.ZERO)
                && !session.isReducedDebugInfo()
                && session.getGeyser().config().gameplay().showCoordinates()
                && session.getPreferencesCache().isPrefersShowCoordinates();
    }
}
