package oxy.geyser.fp.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderCenterPacket;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import java.util.ArrayList;
import java.util.List;

public class WorldPacketsRewriter implements JavaPacketListener {
    @Override public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ClientboundLoginPacket
                || event.getPacket() instanceof ClientboundRespawnPacket) {
            user.chunkCache().clear();
        }
        if (event.getPacket() instanceof ClientboundSetBorderCenterPacket p)
            event.setPacket(new ClientboundSetBorderCenterPacket(p.getNewCenterX() - user.offset().getX(),
                    p.getNewCenterZ() - user.offset().getZ()));
        if (event.getPacket() instanceof ClientboundInitializeBorderPacket p)
            event.setPacket(p.withNewCenterX(p.getNewCenterX() - user.offset().getX())
                    .withNewCenterZ(p.getNewCenterZ() - user.offset().getZ()));
        if (event.getPacket() instanceof ClientboundSetDefaultSpawnPositionPacket p) {
            GlobalPos spawn = p.getGlobalPos();
            event.setPacket(p.withGlobalPos(new GlobalPos(spawn.getDimension(), spawn.getPosition().sub(user.offset()))));
        }
        if (event.getPacket() instanceof ClientboundSoundPacket p)
            event.setPacket(p.withX(p.getX() - user.offset().getX())
                    .withZ(p.getZ() - user.offset().getZ()));
        if (event.getPacket() instanceof ClientboundLevelEventPacket p)
            event.setPacket(p.withPosition(p.getPosition().sub(user.offset())));
        if (event.getPacket() instanceof ClientboundLevelParticlesPacket p)
            event.setPacket(p.withX(p.getX() - user.offset().getX())
                    .withZ(p.getZ() - user.offset().getZ()));
        if (event.getPacket() instanceof ClientboundExplodePacket p)
            event.setPacket(p.withCenter(p.getCenter().sub(user.offset().toDouble())));
        if (event.getPacket() instanceof ClientboundOpenSignEditorPacket p)
            event.setPacket(p.withPosition(p.getPosition().sub(user.offset())));
        if (event.getPacket() instanceof ClientboundPlayerLookAtPacket p)
            event.setPacket(p.withX(p.getX() - user.offset().getX())
                    .withZ(p.getZ() - user.offset().getZ()));
        if (event.getPacket() instanceof ClientboundBlockEntityDataPacket p)
            event.setPacket(p.withPosition(p.getPosition().sub(user.offset())));
        if (event.getPacket() instanceof ClientboundBlockEventPacket p)
            event.setPacket(p.withPosition(p.getPosition().sub(user.offset())));
        if (event.getPacket() instanceof ClientboundSetChunkCacheCenterPacket p) {
            int x = (p.getChunkX() << 4) - user.offset().getX();
            int z = (p.getChunkZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundSetChunkCacheCenterPacket(x >> 4, z >> 4));
        }
        if (event.getPacket() instanceof ClientboundForgetLevelChunkPacket p) {
            user.chunkCache().remove(p.getX(), p.getZ());
            int x = (p.getX() << 4) - user.offset().getX();
            int z = (p.getZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundForgetLevelChunkPacket(x >> 4, z >> 4));
        }
        if (event.getPacket() instanceof ClientboundLevelChunkWithLightPacket p) {
            int sections = user.chunkCache().getChunkHeightY();
            ChunkSection[] palette = new ChunkSection[sections];
            ByteBuf buf = Unpooled.wrappedBuffer(p.getChunkData());
            try {
                for (int sy = 0; sy < sections; sy++) {
                    palette[sy] = MinecraftTypes.readChunkSection(buf,
                            BlockRegistries.BLOCK_STATES.get().size(),
                            user.session().javaRegistries().registry(JavaRegistries.BIOME).size());
                }
            } finally { buf.release(); }
            user.chunkCache().addToCache(p, palette);
            int x = (p.getX() << 4) - user.offset().getX();
            int z = (p.getZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundLevelChunkWithLightPacket(x >> 4, z >> 4,
                    p.getChunkData(), p.getHeightMaps(), p.getBlockEntities(), p.getLightData()));
        }
        if (event.getPacket() instanceof ClientboundBlockUpdatePacket p) {
            user.chunkCache().updateBlock(p.getEntry().getPosition(), p.getEntry().getBlock());
            event.setPacket(new ClientboundBlockUpdatePacket(new BlockChangeEntry(
                    p.getEntry().getPosition().sub(user.offset()), p.getEntry().getBlock())));
        }
        if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket p) {
            user.chunkCache().updateBlockSections(p);
            List<BlockChangeEntry> entries = new ArrayList<>();
            for (BlockChangeEntry entry : p.getEntries()) {
                user.chunkCache().updateBlock(entry.getPosition(), entry.getBlock());
                entries.add(new BlockChangeEntry(entry.getPosition().sub(user.offset()), entry.getBlock()));
            }
            int x = (p.getChunkX() << 4) - user.offset().getX();
            int z = (p.getChunkZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundSectionBlocksUpdatePacket(x >> 4, p.getChunkY(), z >> 4,
                    entries.toArray(new BlockChangeEntry[0])));
        }
    }
}
