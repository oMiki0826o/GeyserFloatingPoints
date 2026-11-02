package oxy.geyser.fp.network.event;

import org.geysermc.mcprotocollib.network.packet.Packet;

public class JavaPacketEvent {
    private Packet packet;
    private boolean cancelled;
    public JavaPacketEvent(Packet packet) { this.packet = packet; }
    public Packet getPacket() { return packet; }
    public void setPacket(Packet packet) { this.packet = packet; }
    public boolean isCancelled() { return cancelled; }
    public void cancel() { cancelled = true; }
}
