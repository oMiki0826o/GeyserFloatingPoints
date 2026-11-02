package oxy.geyser.fp;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.session.GeyserSession;
import oxy.geyser.fp.network.PacketListenerRegistry;
import oxy.geyser.fp.packets.ActionPacketsRewriter;
import oxy.geyser.fp.packets.EntityPacketsRewriter;
import oxy.geyser.fp.packets.PositionPacketsRewriter;
import oxy.geyser.fp.packets.WorldPacketsRewriter;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.util.GeyserUtil;
import oxy.geyser.fp.util.config.Config;
import oxy.geyser.fp.util.config.ConfigLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Geyser Extension; based on oryxel1/GeyserFloatingPoints, GPL-3.0-or-later. */
public class GeyserFloatingPoints implements Extension {
    private static Config CONFIG;
    public static Config config() { return CONFIG; }

    private static final Map<GeyserSession, GeyserFPUser> SESSIONS = new ConcurrentHashMap<>();

    @Subscribe
    public void onPostInit(GeyserPostInitializeEvent event) {
        CONFIG = ConfigLoader.load(this, GeyserFloatingPoints.class, Config.class);
        if (CONFIG == null || CONFIG.maxPosition() < 32) {
            logger().error("Invalid config.yml; capped-value must be at least 32. Extension handlers not enabled.");
            return;
        }
        PacketListenerRegistry.instance().register(new WorldPacketsRewriter(),
                new PositionPacketsRewriter(), new EntityPacketsRewriter(), new ActionPacketsRewriter());
        if (!GeyserUtil.wrapAroundGeyserBootstrap()) {
            logger().warning("World manager wrapper could not be installed. Test world/chunk interactions carefully.");
        }
    }

    @Subscribe
    public void onSessionLogin(SessionLoginEvent event) {
        if (CONFIG == null) return;
        GeyserSession session = (GeyserSession) event.connection();
        GeyserFPUser user = new GeyserFPUser(session);
        SESSIONS.put(session, user);
        GeyserUtil.wrapAroundUpstreamHandler(user);
    }

    @Subscribe
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        SESSIONS.remove((GeyserSession) event.connection());
    }

    @Subscribe
    public void onCommands(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this).source(CommandSource.class)
                .name("position").playerOnly(true).bedrockOnly(true)
                .permission("geyserfloatingpoints.position", TriState.TRUE)
                .description("Toggle actionbar with your real Java coordinates")
                .executor((source, command, args) -> {
                    if (source.connection() instanceof GeyserSession session) {
                        GeyserFPUser user = SESSIONS.get(session);
                        if (user != null) {
                            user.setPositionShown(!user.shouldShowPosition());
                            source.sendMessage("Real coordinates display: " + user.shouldShowPosition());
                        }
                    }
                }).build());
    }
}
