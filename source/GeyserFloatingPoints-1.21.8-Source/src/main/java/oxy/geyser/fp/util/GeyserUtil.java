package oxy.geyser.fp.util;

import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.UpstreamSession;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import oxy.geyser.fp.network.wrapper.GeyserFPAdapterWrapper;
import oxy.geyser.fp.network.wrapper.UpstreamHandlerWrapper;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.wrappers.GeyserBootstrapWrapper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class GeyserUtil {
    private GeyserUtil() { }

    public static boolean wrapAroundGeyserBootstrap() {
        try {
            Field field = GeyserImpl.class.getDeclaredField("bootstrap");
            field.setAccessible(true);
            field.set(GeyserImpl.getInstance(),
                    new GeyserBootstrapWrapper(GeyserImpl.getInstance().getBootstrap()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
        return true;
    }

    public static void wrapAroundSessionAdapter(GeyserFPUser user) {
        try {
            ClientSession session = user.session().getDownstream().getSession();
            List<SessionListener> listeners = new ArrayList<>(session.getListeners());
            session.getListeners().forEach(session::removeListener);
            session.addListener(new GeyserFPAdapterWrapper(user, listeners));
        } catch (RuntimeException e) {
            user.session().disconnect("GeyserFloatingPoints: unable to intercept Java packets; check Geyser API compatibility.");
        }
    }

    public static void wrapAroundUpstreamHandler(GeyserFPUser user) {
        try {
            BedrockServerSession session = user.session().getUpstream().getSession();
            Field upstreamField = GeyserSession.class.getDeclaredField("upstream");
            upstreamField.setAccessible(true);
            UpstreamSession original = (UpstreamSession) upstreamField.get(user.session());
            upstreamField.set(user.session(), new UpstreamHandlerWrapper(user, session, original));
        } catch (ReflectiveOperationException | RuntimeException e) {
            user.session().disconnect("GeyserFloatingPoints: unable to intercept Bedrock packets; check Geyser API compatibility.");
        }
    }
}
