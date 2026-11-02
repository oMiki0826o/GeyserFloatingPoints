package oxy.geyser.fp.packets;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import oxy.geyser.fp.GeyserFloatingPoints;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.util.MathUtil;
import java.util.EnumSet;

public class PositionPacketsRewriter implements JavaPacketListener {
    @Override public void onSend(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPosPacket packet) {
            if (checkForReOffset(user, packet.getX(), packet.getY(), packet.getZ())) {
                event.cancel(); return;
            }
            Vector3d v = OffsetCollisionCorrector.correct(user.session(), Vector3d.from(
                    packet.getX() + user.offset().getX(), packet.getY() + user.offset().getY(),
                    packet.getZ() + user.offset().getZ()));
            event.setPacket(new ServerboundMovePlayerPosPacket(packet.isOnGround(),
                    packet.isHorizontalCollision(), v.getX(), v.getY(), v.getZ()));
        }
        if (event.getPacket() instanceof ServerboundMovePlayerPosRotPacket packet) {
            if (checkForReOffset(user, packet.getX(), packet.getY(), packet.getZ())) {
                event.cancel(); return;
            }
            Vector3d v = OffsetCollisionCorrector.correct(user.session(), Vector3d.from(
                    packet.getX() + user.offset().getX(), packet.getY() + user.offset().getY(),
                    packet.getZ() + user.offset().getZ()));
            event.setPacket(new ServerboundMovePlayerPosRotPacket(packet.isOnGround(),
                    packet.isHorizontalCollision(), v.getX(), v.getY(), v.getZ(),
                    packet.getYaw(), packet.getPitch()));
        }
        if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet) {
            if (checkForReOffset(user, packet.getPosition().getX(),
                    packet.getPosition().getY(), packet.getPosition().getZ())) {
                event.cancel(); return;
            }
            event.setPacket(new ServerboundMoveVehiclePacket(
                    packet.getPosition().add(user.offset().toDouble()),
                    packet.getYRot(), packet.getXRot(), packet.isOnGround()));
        }
    }

    @Override public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        SessionPlayerEntity entity = user.session().getPlayerEntity();
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            Vector3f current = entity.getPosition().add(user.offset().toFloat());
            double x = packet.getPosition().getX()
                    + (packet.getRelatives().contains(PositionElement.X) ? current.getX() : 0);
            double z = packet.getPosition().getZ()
                    + (packet.getRelatives().contains(PositionElement.Z) ? current.getZ() : 0);
            int cappedValue = GeyserFloatingPoints.config().maxPosition();
            if (Math.abs(x - user.offset().getX()) > cappedValue
                    || Math.abs(z - user.offset().getZ()) > cappedValue) {
                user.offset(MathUtil.calculateOffset(Vector3d.from(x, 0, z)), false);
            }
            // Avoid mutating collections supplied by MCProtocolLib: some are immutable.
            EnumSet<PositionElement> relatives = EnumSet.noneOf(PositionElement.class);
            relatives.addAll(packet.getRelatives());
            relatives.remove(PositionElement.X);
            relatives.remove(PositionElement.Z);
            event.setPacket(new ClientboundPlayerPositionPacket(packet.getId(),
                    x - user.offset().getX(), packet.getPosition().getY(),
                    z - user.offset().getZ(), packet.getDeltaMovement().getX(),
                    packet.getDeltaMovement().getY(), packet.getDeltaMovement().getZ(),
                    packet.getYRot(), packet.getXRot(), relatives.toArray(new PositionElement[0])));
        }
    }

    private boolean checkForReOffset(GeyserFPUser user, double x, double y, double z) {
        Vector3i pos = Vector3i.from(x, y, z);
        if (pos.distance(user.prevPosition) > 0 && user.shouldShowPosition()) {
            SetTitlePacket title = new SetTitlePacket();
            title.setType(SetTitlePacket.Type.ACTIONBAR);
            title.setText("XYZ: " + pos.add(user.offset()));
            title.setXuid("");
            title.setPlatformOnlineId("");
            user.session().sendUpstreamPacket(title);
        }
        user.prevPosition = pos;
        int cappedValue = GeyserFloatingPoints.config().maxPosition();
        if (Math.abs(x) < cappedValue && Math.abs(z) < cappedValue) return false;
        user.offset(MathUtil.calculateOffset(Vector3d.from(x + user.offset().getX(), 0,
                z + user.offset().getZ())), true);
        return true;
    }
}
