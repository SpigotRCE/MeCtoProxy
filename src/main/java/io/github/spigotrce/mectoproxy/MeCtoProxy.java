package io.github.spigotrce.mectoproxy;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.SkinParts;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.github.spigotrce.mectoproxy.command.AbstractCommand;
import io.github.spigotrce.mectoproxy.command.impl.ChangeIPCommand;
import io.github.spigotrce.mectoproxy.command.impl.ShutDownCommand;
import io.github.spigotrce.mectoproxy.hook.HandshakeHook;
import io.github.spigotrce.mectoproxy.hook.PacketHook;
import io.github.spigotrce.mectoproxy.hook.PluginMessageHook;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Plugin(
        id = "mectoproxy",
        name = "MeCtoProxy",
        version = "1.0-SNAPSHOT"
)
public class MeCtoProxy {
    public static MeCtoProxy INSTANCE;

    @Inject
    public static Logger LOGGER;
    @Inject
    @DataDirectory
    public static Path DATA_DIRECTORY;
    @Inject
    public static ProxyServer PROXY_SERVER;

    public static List<PacketHook> PACKET_HOOKS;

    public static String TARGET_SERVER_HOSTNAME; // Numeric IP
    public static int TARGET_SERVER_PORT; // Port number

    public static ServerPing CACHED_SERVER_PING; // Cached target server ping

    public static ProtocolVersion LAST_PROTOCOL_VERSION; // Last protocol version of the client

    @Inject
    public MeCtoProxy(Logger logger, @DataDirectory Path dataDirectory, ProxyServer proxyServer) {
        INSTANCE = this;

        LOGGER = logger;
        DATA_DIRECTORY = dataDirectory;
        PROXY_SERVER = proxyServer;

        PACKET_HOOKS = new ArrayList<>();

        TARGET_SERVER_HOSTNAME = "Mikthedev.aternos.me";
        TARGET_SERVER_PORT = 11839;

        // Placeholder
        CACHED_SERVER_PING = new ServerPing(
                new ServerPing.Version(47, "1.8.8"),
                null,
                Component.text("Welcome"),
                new Favicon("data:image/png;base64,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")
        );

        LAST_PROTOCOL_VERSION = ProtocolVersion.MINECRAFT_1_8; // Placeholder
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        LOGGER.info("Initializing MeCtoProxy...");

        registerPacketHooks();

        registerTargetServer();

        // Starting an asynchronous task to cache the server ping information
        PROXY_SERVER.getScheduler().buildTask(this, () -> {
            try {
                CACHED_SERVER_PING = PROXY_SERVER.getAllServers().stream().findFirst().get().ping(
                        PingOptions.builder().version(LAST_PROTOCOL_VERSION).build()
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Failed to ping target server", e);
                LOGGER.info("Using cached server ping indication until next successful ping");
            }
        }).repeat(10, TimeUnit.SECONDS).schedule();

        PROXY_SERVER.getCommandManager().getAliases().forEach(command -> PROXY_SERVER.getCommandManager().unregister(command));
        PROXY_SERVER.getCommandManager().register("mectoproxy", new MeCtoCommand());

        LOGGER.info("MeCtoProxy initialized successfully!");
    }

    public void registerPacketHooks() {
        try {
            MethodHandle packetIdToSupplierField = MethodHandles
                    .privateLookupIn(StateRegistry.PacketRegistry.ProtocolRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetIdToSupplier", IntObjectMap.class);

            MethodHandle packetClassToIdField = MethodHandles
                    .privateLookupIn(StateRegistry.PacketRegistry.ProtocolRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetClassToId", Object2IntMap.class);

            PACKET_HOOKS.add(new PluginMessageHook());
            PACKET_HOOKS.add(new HandshakeHook());

            BiConsumer<? super ProtocolVersion, ? super StateRegistry.PacketRegistry.ProtocolRegistry> consumer = (version, registry) -> {
                try {
                    IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier
                            = (IntObjectMap<Supplier<? extends MinecraftPacket>>) packetIdToSupplierField.invoke(registry);

                    Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId
                            = (Object2IntMap<Class<? extends MinecraftPacket>>) packetClassToIdField.invoke(registry);

                    PACKET_HOOKS.forEach(hook -> {
                        int packetId = packetClassToId.getInt(hook.getType());
                        packetClassToId.put(hook.getHookClass(), packetId);
                        packetIdToSupplier.remove(packetId);
                        packetIdToSupplier.put(packetId, hook.getHook());
                    });
                } catch (Throwable e) {
                    LOGGER.error("Failed to initialize packet hooks", e);
                }
            };

            MethodHandle clientboundGetter = MethodHandles.privateLookupIn(StateRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.class, "clientbound", StateRegistry.PacketRegistry.class);

            MethodHandle serverboundGetter = MethodHandles.privateLookupIn(StateRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.class, "serverbound", StateRegistry.PacketRegistry.class);

            StateRegistry.PacketRegistry playClientbound = (StateRegistry.PacketRegistry) clientboundGetter.invokeExact(StateRegistry.PLAY);
            StateRegistry.PacketRegistry configClientbound = (StateRegistry.PacketRegistry) clientboundGetter.invokeExact(StateRegistry.CONFIG);
            StateRegistry.PacketRegistry handshakeServerbound = (StateRegistry.PacketRegistry) serverboundGetter.invokeExact(StateRegistry.HANDSHAKE);

            MethodHandle versionsField = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.class, MethodHandles.lookup())
                    .findGetter(StateRegistry.PacketRegistry.class, "versions", Map.class);

            ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.invokeExact(playClientbound)).forEach(consumer);
            ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.invokeExact(configClientbound)).forEach(consumer);
            ((Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) versionsField.invokeExact(handshakeServerbound)).forEach(consumer);
        } catch (Throwable e) {
            LOGGER.error("Failed to initialize packet hooks", e);
        }
    }

    public void registerTargetServer() {
        // Unregistering all servers
        PROXY_SERVER.getAllServers().forEach(server -> PROXY_SERVER.unregisterServer(
                server.getServerInfo()
        ));

        // Registering the target server
        PROXY_SERVER.registerServer(new ServerInfo("lobby", new InetSocketAddress(TARGET_SERVER_HOSTNAME, TARGET_SERVER_PORT)));
    }

    // MoTD event
    @Subscribe
    public void onProxyPingEvent(ProxyPingEvent event) {
        LOGGER.info("Received proxy ping from: {}", event.getConnection().getRemoteAddress().getAddress());
        LAST_PROTOCOL_VERSION = event.getConnection().getProtocolVersion(); // Updating the last protocol version oof the client
        event.setPing(CACHED_SERVER_PING);
    }

    // Player pre-login listener
    @Subscribe
    public void onPlayerPreLoginEvent(PreLoginEvent event) {
        // Force offline mode authentication
        event.setResult(
                PreLoginEvent.PreLoginComponentResult.forceOfflineMode()
        );
        LOGGER.info(
                "Incoming connection from {} player name: {}, forcing offline mode authentication",
                event.getUsername(),
                event.getConnection().getRemoteAddress().getAddress()
        );
    }

    // Player post-login event
    @Subscribe
    public void onPlayerPostLoginEvent(PostLoginEvent event) {
        LOGGER.info(
                "Player {} logged in successfully! ClientBrand: {} GameProfile: {} PlayerSetting: {}",
                event.getPlayer().getUsername(),
                event.getPlayer().getClientBrand(),
                event.getPlayer().getGameProfileProperties(),
                toString(event.getPlayer().getPlayerSettings())
        );
    }

    // Player connect to backend server event
    @Subscribe
    public void onServerConnectEvent(ServerConnectedEvent event) {
        LOGGER.info(
                "Player {} connected to server {}",
                event.getPlayer().getUsername(),
                event.getServer().getServerInfo().getName()
        );
    }

    // Player tab complete event
    @Subscribe
    public void onPlayerAvailableCommandsEvent(PlayerAvailableCommandsEvent event) {
        event.getRootNode().getChildren().removeIf(child ->
                PROXY_SERVER.getCommandManager().getAliases().contains(
                        child.getName().toLowerCase()
                )
        );
    }

    // Player execute command event
    @Subscribe
    public void onPlayerCommandEvent(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;

        event.setResult(CommandExecuteEvent.CommandResult.forwardToServer());

        LOGGER.info(
                "Player {} executed command '{}'",
                player.getUsername(),
                event.getCommand()
        );
    }

    // Player send chat event
    @Subscribe
    public void onPlayerChatEvent(PlayerChatEvent event) {
        LOGGER.info(
                "Player {} sent chat message: {}",
                event.getPlayer().getUsername(),
                event.getMessage()
        );
    }

    // Custom toString method for PlayerSettings
    private String toString(PlayerSettings playerSettings) {
        if (playerSettings == null) return "null";
        return "PlayerSettings{" +
                ", locale=" + playerSettings.getLocale() +
                ", viewdistance=" + playerSettings.getViewDistance() +
                ", chatmode=" + playerSettings.getChatMode() +
                ", skinparts=" + toString(playerSettings.getSkinParts()) +
                ", mainhand=" + playerSettings.getMainHand() +
                ", clientlistingallowed=" + playerSettings.isClientListingAllowed() +
                ", textfilteringenabled=" + playerSettings.isTextFilteringEnabled() +
                ", particlestatus=" + playerSettings.getParticleStatus() +
                "}";
    }

    //  Custom toString method for SkinParts
    private String toString(SkinParts skinParts) {
        if (skinParts == null) return "null";
        return "SkinParts{" +
                "cape=" + skinParts.hasCape() +
                ", jacket=" + skinParts.hasJacket() +
                ", leftsleve=" + skinParts.hasLeftSleeve() +
                ", rightsleve=" + skinParts.hasRightSleeve() +
                ", leftpants=" + skinParts.hasLeftPants() +
                ", rightpants=" + skinParts.hasRightPants() +
                ", hat=" + skinParts.hasHat() +
                "}";
    }

    // Custom command implementation using SimpleCommand
    private static class MeCtoCommand implements SimpleCommand {
        private final HashMap<String, AbstractCommand> COMMANDS;
        public MeCtoCommand() {
            COMMANDS = new HashMap<>();

            COMMANDS.put("changeip", new ChangeIPCommand());
            COMMANDS.put("shutdown", new ShutDownCommand());
        }

        @Override
        public void execute(Invocation invocation) {
            if (!(invocation.source() instanceof ConsoleCommandSource)) return;

            String[] args = invocation.arguments();
            if (args.length == 0) {
                invocation.source().sendMessage(Component.text("Available commands: " + String.join(", ", COMMANDS.keySet())));
                return;
            }
            String commandName = args[0];
            AbstractCommand command = COMMANDS.get(commandName);
            if (command == null) {
                invocation.source().sendMessage(Component.text("Unknown command: " + commandName));
                return;
            }
            command.execute(Arrays.copyOfRange(args, 1, args.length));
        }
    }
}
