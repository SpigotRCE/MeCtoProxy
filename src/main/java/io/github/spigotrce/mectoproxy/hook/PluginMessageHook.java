package io.github.spigotrce.mectoproxy.hook;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendPlaySessionHandler;
import com.velocitypowered.proxy.connection.backend.ConfigSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.github.spigotrce.mectoproxy.MeCtoProxy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class PluginMessageHook extends PluginMessagePacket implements PacketHook {
    protected static MethodHandle SERVER_CONNECTION_BACKEND_PLAY_FIELD;
    protected static MethodHandle SERVER_CONNECTION_CONFIG_FIELD;

    public PluginMessageHook() {
        try {
            PluginMessageHook.SERVER_CONNECTION_BACKEND_PLAY_FIELD = MethodHandles
                    .privateLookupIn(BackendPlaySessionHandler.class, MethodHandles.lookup())
                    .findGetter(BackendPlaySessionHandler.class, "serverConn", VelocityServerConnection.class);

            PluginMessageHook.SERVER_CONNECTION_CONFIG_FIELD = MethodHandles
                    .privateLookupIn(ConfigSessionHandler.class, MethodHandles.lookup())
                    .findGetter(ConfigSessionHandler.class, "serverConn", VelocityServerConnection.class);
        } catch (Exception e) {
            MeCtoProxy.LOGGER.error("Failed to initialize plugin message hook", e);
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        if (PluginMessageUtil.isMcBrand(this)) {
            try {
                if (handler instanceof BackendPlaySessionHandler) {
                    ConnectedPlayer player = ((VelocityServerConnection) SERVER_CONNECTION_BACKEND_PLAY_FIELD.invoke(handler)).getPlayer();
                    player.getConnection().write(this.rewriteMinecraftBrand(this, player.getProtocolVersion()));
                    return true;
                } else if (handler instanceof ConfigSessionHandler) {
                    ConnectedPlayer player = ((VelocityServerConnection) SERVER_CONNECTION_CONFIG_FIELD.invoke(handler)).getPlayer();
                    player.getConnection().write(this.rewriteMinecraftBrand(this, player.getProtocolVersion()));
                    return true;
                }
            } catch (Throwable e) {
                MeCtoProxy.LOGGER.error("Failed to handle plugin message", e);
            }
        }

        return super.handle(handler);
    }

    private PluginMessagePacket rewriteMinecraftBrand(PluginMessagePacket message, ProtocolVersion protocolVersion) {
        String currentBrand = PluginMessageUtil.readBrandMessage(message.content());
        ByteBuf rewrittenBuf = Unpooled.buffer();
        if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
            ProtocolUtils.writeString(rewrittenBuf, currentBrand);
        } else {
            rewrittenBuf.writeCharSequence(currentBrand, StandardCharsets.UTF_8);
        }

        return new PluginMessagePacket(message.getChannel(), rewrittenBuf);
    }

    @Override
    public Supplier<MinecraftPacket> getHook() {
        return PluginMessageHook::new;
    }

    @Override
    public Class<? extends MinecraftPacket> getType() {
        return PluginMessagePacket.class;
    }

    @Override
    public Class<? extends MinecraftPacket> getHookClass() {
        return this.getClass();
    }
}
