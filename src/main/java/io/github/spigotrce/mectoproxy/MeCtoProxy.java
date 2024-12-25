package io.github.spigotrce.mectoproxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.SkinParts;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "mectoproxy",
        name = "MeCtoProxy",
        version = "1.0-SNAPSHOT"
)
public class MeCtoProxy {
    @Inject
    public static Logger LOGGER;
    @Inject
    @DataDirectory
    public static Path DATA_DIRECTORY;
    @Inject
    public static ProxyServer PROXY_SERVER;

    public static String TARGET_SERVER_IP; // Numeric IP
    public static int TARGET_SERVER_PORT; // Port number
    public static String TARGET_SERVER_HOSTNAME; // Hostname

    public static ServerPing CACHED_SERVER_PING; // Cached target server ping

    public static ProtocolVersion LAST_PROTOCOL_VERSION; // Last protocol version of the client

    @Inject
    public MeCtoProxy(Logger logger, @DataDirectory Path dataDirectory, ProxyServer proxyServer) {
        LOGGER = logger;
        DATA_DIRECTORY = dataDirectory;
        PROXY_SERVER = proxyServer;

        // Placeholder
        CACHED_SERVER_PING = new ServerPing(
                new ServerPing.Version(47, "1.8.8"),
                null,
                Component.text("Welcome"),
                new Favicon("data:image/png;base64,") // TODO: Add a placing holding favicon
        );

        LAST_PROTOCOL_VERSION = ProtocolVersion.MINECRAFT_1_8; // Placeholder
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        LOGGER.info("Initializing MeCtoProxy...");

        // Unregistering all servers
        PROXY_SERVER.getAllServers().forEach(server -> PROXY_SERVER.unregisterServer(
                server.getServerInfo()
        ));

        // Registering the target server
        PROXY_SERVER.registerServer(new ServerInfo("lobby", new InetSocketAddress(TARGET_SERVER_IP, TARGET_SERVER_PORT)));

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

        LOGGER.info("MeCtoProxy initialized successfully!");
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        LOGGER.info("Received proxy ping from: {}", event.getConnection().getRemoteAddress().getAddress());
        LAST_PROTOCOL_VERSION = event.getConnection().getProtocolVersion(); // Updating the last protocol version oof the client
        event.setPing(CACHED_SERVER_PING);
    }

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

    @Subscribe
    public void onServerConnectEvent(ServerConnectedEvent event) {
        LOGGER.info(
                "Player {} connected to server {}",
                event.getPlayer().getUsername(),
                event.getServer().getServerInfo().getName()
        );
    }

    @Subscribe
    public void onPlayerCommandEvent(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) return;

        LOGGER.info(
                "Player {} executed command '{}'",
                player.getUsername(),
                event.getCommand()
        );
    }

    @Subscribe
    public void onPlayerChatEvent(PlayerChatEvent event) {
        LOGGER.info(
                "Player {} sent chat message: {}",
                event.getPlayer().getUsername(),
                event.getMessage()
        );
    }


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
}
