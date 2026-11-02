package oxy.geyser.fp.network;

import oxy.geyser.fp.network.listener.JavaPacketListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PacketListenerRegistry {
    private static final PacketListenerRegistry INSTANCE = new PacketListenerRegistry();
    public static PacketListenerRegistry instance() { return INSTANCE; }
    private final List<JavaPacketListener> javaPacketListeners = new CopyOnWriteArrayList<>();
    public List<JavaPacketListener> javaListeners() { return List.copyOf(javaPacketListeners); }
    public void register(JavaPacketListener... listeners) { javaPacketListeners.addAll(List.of(listeners)); }
}
