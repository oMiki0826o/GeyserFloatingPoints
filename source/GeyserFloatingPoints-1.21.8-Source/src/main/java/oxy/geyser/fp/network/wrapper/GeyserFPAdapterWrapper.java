package oxy.geyser.fp.network.wrapper;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.network.packet.Packet;
import oxy.geyser.fp.network.PacketListenerRegistry;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import java.util.List;

/** Processes the Java protocol packets before Geyser's own listeners. */
public class GeyserFPAdapterWrapper extends SessionAdapter {
    private final GeyserFPUser user;
    private final List<SessionListener> listeners;
    public GeyserFPAdapterWrapper(GeyserFPUser user, List<SessionListener> listeners) {
        this.user = user;
        this.listeners = List.copyOf(listeners);
    }
    @Override public void packetReceived(Session session, Packet packet) {
        JavaPacketEvent event = new JavaPacketEvent(packet);
        for (JavaPacketListener listener : PacketListenerRegistry.instance().javaListeners()) {
            listener.onReceived(user, event);
            if (event.isCancelled()) return;
        }
        for (SessionListener listener : listeners) listener.packetReceived(session, event.getPacket());
    }
    @Override public void packetSending(PacketSendingEvent sendingEvent) {
        JavaPacketEvent event = new JavaPacketEvent(sendingEvent.getPacket());
        for (JavaPacketListener listener : PacketListenerRegistry.instance().javaListeners()) {
            listener.onSend(user, event);
            if (event.isCancelled()) {
                // Also cancel the real MCProtocolLib event. Returning alone still
                // allows the Java protocol session to transmit the old position.
                sendingEvent.setCancelled(true);
                return;
            }
        }
        sendingEvent.setPacket(event.getPacket());
        for (SessionListener listener : listeners) listener.packetSending(sendingEvent);
    }
    @Override public void packetSent(Session session, Packet packet) {
        for (SessionListener listener : listeners) listener.packetSent(session, packet);
    }
    @Override public void connected(ConnectedEvent event) {
        for (SessionListener listener : listeners) listener.connected(event);
    }
    @Override public void disconnected(DisconnectedEvent event) {
        for (SessionListener listener : listeners) listener.disconnected(event);
    }
    @Override public void packetError(PacketErrorEvent event) {
        for (SessionListener listener : listeners) listener.packetError(event);
    }
}
