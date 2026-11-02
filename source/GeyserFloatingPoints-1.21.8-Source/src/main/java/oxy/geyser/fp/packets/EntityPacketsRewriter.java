package oxy.geyser.fp.packets;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityPacketsRewriter implements JavaPacketListener {
    @Override public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            event.setPacket(packet.withX(packet.getX() - user.offset().getX())
                    .withZ(packet.getZ() - user.offset().getZ()));
        }
        if (event.getPacket() instanceof ClientboundSetEntityDataPacket packet) {
            ArrayList<EntityMetadata<?, ?>> list = new ArrayList<>();
            for (EntityMetadata<?, ?> meta : packet.getMetadata()) {
                if (meta.getId() == 14 && meta.getType() == MetadataTypes.OPTIONAL_BLOCK_POS) {
                    Object value = meta.getValue();
                    if (value instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof Vector3i pos) {
                        list.add(new ObjectEntityMetadata<>(meta.getId(), MetadataTypes.OPTIONAL_BLOCK_POS,
                                Optional.of(pos.sub(user.offset()))));
                        continue;
                    }
                }
                list.add(meta);
            }
            event.setPacket(new ClientboundSetEntityDataPacket(packet.getEntityId(),
                    list.toArray(EntityMetadata[]::new)));
        }
        if (event.getPacket() instanceof ClientboundEntityPositionSyncPacket packet) {
            // 2.0 regression: null endPosition must not be dereferenced (Issue #40).
            Vector3d endPosition = packet.getEndPosition();
            if (endPosition != null) {
                event.setPacket(packet.withEndPosition(endPosition.sub(user.offset().toDouble())));
            }
            // A null position is passed through unchanged. This prevents NPE,
            // but requires integration testing to confirm upstream packet semantics.
        }
        if (event.getPacket() instanceof ClientboundTeleportEntityPacket packet) {
            List<org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement> relatives = packet.getRelatives();
            double x = packet.getPosition().getX();
            double z = packet.getPosition().getZ();
            if (!relatives.contains(org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement.X)) {
                x -= user.offset().getX();
            }
            if (!relatives.contains(org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement.Z)) {
                z -= user.offset().getZ();
            }
            event.setPacket(packet.withPosition(Vector3d.from(x, packet.getPosition().getY(), z)));
        }
        if (event.getPacket() instanceof ClientboundMoveVehiclePacket packet) {
            if (packet.getPosition() != null) {
                event.setPacket(packet.withPosition(packet.getPosition().sub(user.offset().toDouble())));
            }
        }
    }
}
