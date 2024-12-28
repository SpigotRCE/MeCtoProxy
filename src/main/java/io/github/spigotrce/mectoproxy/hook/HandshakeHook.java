package io.github.spigotrce.mectoproxy.hook;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import java.lang.reflect.Field;
import java.util.function.Supplier;

import io.github.spigotrce.mectoproxy.MeCtoProxy;

public class HandshakeHook extends HandshakePacket implements PacketHook {
    private Field SERVER_ADDRESS_FIELD;
    private Field PORT_FIELD;

    public HandshakeHook() {
        try {
            SERVER_ADDRESS_FIELD = HandshakePacket.class.getDeclaredField("serverAddress");
            SERVER_ADDRESS_FIELD.setAccessible(true);

            PORT_FIELD = HandshakePacket.class.getDeclaredField("port");
            PORT_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            MeCtoProxy.LOGGER.error("Failed to initialize handshake hook", e);
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        try {
            SERVER_ADDRESS_FIELD.set(this, MeCtoProxy.TARGET_SERVER_HOSTNAME);
            PORT_FIELD.set(this, MeCtoProxy.TARGET_SERVER_PORT);
        } catch (IllegalAccessException e) {
            MeCtoProxy.LOGGER.error("Failed to initialize handshake hook", e);
        }

        return super.handle(handler);
    }

    @Override
    public Supplier<MinecraftPacket> getHook() {
        return HandshakeHook::new;
    }

    @Override
    public Class<? extends MinecraftPacket> getType() {
        return HandshakePacket.class;
    }

    @Override
    public Class<? extends MinecraftPacket> getHookClass() {
        return this.getClass();
    }
}
