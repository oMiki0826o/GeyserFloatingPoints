package oxy.geyser.fp.session.cache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistry;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import oxy.geyser.fp.session.GeyserFPUser;

/** Chunk cache and retransmission, derived from upstream 2.0. */
public class ChunkCache {
    private final GeyserFPUser user;
    private final Long2ObjectMap<ChunkSection[]> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ClientboundLevelChunkWithLightPacket> chunkPackets = new Long2ObjectOpenHashMap<>();
    private boolean resendingWithOffset;
    public ChunkCache(GeyserFPUser user) { this.user = user; }

    public void sendChunksWithOffset() {
        if (resendingWithOffset || chunks.isEmpty()) return;
        resendingWithOffset = true;
        try {
            for (Long2ObjectMap.Entry<ChunkSection[]> entry : chunks.long2ObjectEntrySet()) {
            ClientboundLevelChunkWithLightPacket oldPacket = chunkPackets.get(entry.getLongKey());
            if (oldPacket == null) continue;
            ByteBuf oldByteBuf = Unpooled.wrappedBuffer(oldPacket.getChunkData());
            ByteBuf output = null;
            try {
                for (int sy = 0; sy < getChunkHeightY(); sy++) {
                    MinecraftTypes.readChunkSection(oldByteBuf, BlockRegistries.BLOCK_STATES.get().size(),
                            user.session().javaRegistries().registry(JavaRegistries.BIOME).size());
                }
                output = ByteBufAllocator.DEFAULT.ioBuffer();
                for (ChunkSection section : entry.getValue()) MinecraftTypes.writeChunkSection(output, section);
                output.writeBytes(oldByteBuf);
                byte[] payload = new byte[output.readableBytes()];
                output.readBytes(payload);
                int x = (oldPacket.getX() << 4) - user.offset().getX();
                int z = (oldPacket.getZ() << 4) - user.offset().getZ();
                Registries.JAVA_PACKET_TRANSLATORS.translate(ClientboundLevelChunkWithLightPacket.class,
                        new ClientboundLevelChunkWithLightPacket(x >> 4, z >> 4, payload,
                                oldPacket.getHeightMaps(), oldPacket.getBlockEntities(), oldPacket.getLightData()),
                        user.session(), true);
            } finally {
                if (output != null) output.release();
                oldByteBuf.release();
            }
            }
        } finally {
            resendingWithOffset = false;
        }
    }

    public void addToCache(ClientboundLevelChunkWithLightPacket packet, ChunkSection[] palette) {
        long key = MathUtils.chunkPositionToLong(packet.getX(), packet.getZ());
        chunks.put(key, palette);
        chunkPackets.put(key, packet);
    }
    private ChunkSection[] getChunk(int chunkX, int chunkZ) {
        return chunks.get(MathUtils.chunkPositionToLong(chunkX, chunkZ));
    }
    public void updateBlockSections(ClientboundSectionBlocksUpdatePacket packet) {
        ChunkSection[] column = getChunk(packet.getChunkX(), packet.getChunkZ());
        if (column == null) return;
        int y = packet.getChunkY() << 4;
        int index = (y - getMinY()) >> 4;
        if (index < 0 || index >= column.length) return;
        ChunkSection palette = column[index];
        if (palette == null) {
            JavaRegistry<Integer> registry = user.session().javaRegistries().registry(JavaRegistries.BIOME);
            column[index] = palette = new ChunkSection(Block.JAVA_AIR_ID,
                    BlockRegistries.BLOCK_STATES.get().size(), registry.stream().toList().getFirst(), registry.size());
        }
        for (BlockChangeEntry entry : packet.getEntries()) {
            palette.setBlock(entry.getPosition().getX() & 15,
                    entry.getPosition().getY() & 15, entry.getPosition().getZ() & 15, entry.getBlock());
        }
    }
    public void updateBlock(Vector3i position, int block) {
        updateBlock(position.getX(), position.getY(), position.getZ(), block);
    }
    public void updateBlock(int x, int y, int z, int block) {
        ChunkSection[] column = getChunk(x >> 4, z >> 4);
        if (column == null) return;
        int index = (y - getMinY()) >> 4;
        if (index < 0 || index >= column.length) return;
        ChunkSection section = column[index];
        if (section == null) {
            if (block == Block.JAVA_AIR_ID) return;
            JavaRegistry<Integer> registry = user.session().javaRegistries().registry(JavaRegistries.BIOME);
            column[index] = section = new ChunkSection(Block.JAVA_AIR_ID,
                    BlockRegistries.BLOCK_STATES.get().size(), registry.stream().toList().getFirst(), registry.size());
        }
        section.setBlock(x & 15, y & 15, z & 15, block);
    }
    public void remove(int chunkX, int chunkZ) {
        long key = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(key);
        chunkPackets.remove(key);
    }
    public void clear() { chunks.clear(); chunkPackets.clear(); }
    public int getMinY() { return user.session().getChunkCache().getChunkMinY() << 4; }
    public int getChunkHeightY() { return user.session().getChunkCache().getChunkHeightY(); }
}
