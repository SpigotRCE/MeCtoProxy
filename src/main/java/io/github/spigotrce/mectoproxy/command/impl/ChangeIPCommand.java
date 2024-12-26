package io.github.spigotrce.mectoproxy.command.impl;

import com.velocitypowered.api.command.SimpleCommand.Invocation;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import io.github.spigotrce.mectoproxy.command.AbstractCommand;
import io.github.spigotrce.mectoproxy.MeCtoProxy;
import net.kyori.adventure.text.Component;

public class ChangeIPCommand extends AbstractCommand {
    @Override
    public void execute(String[] arguments) {
        String[] args = arguments[0].split(":");
        if (args.length!= 2) {
            MeCtoProxy.PROXY_SERVER.getConsoleCommandSource().sendMessage(Component.text("Usage: /mectoproxy changeip <new_ip:new_port>"));
            return;
        }

        MeCtoProxy.TARGET_SERVER_IP = args[0];
        MeCtoProxy.TARGET_SERVER_PORT = Integer.parseInt(args[1]);
        MeCtoProxy.TARGET_SERVER_HOSTNAME = MeCtoProxy.TARGET_SERVER_IP + ":" + MeCtoProxy.TARGET_SERVER_PORT;

        MeCtoProxy.PROXY_SERVER.getConsoleCommandSource().sendMessage(Component.text("Target server IP and port changed to " + MeCtoProxy.TARGET_SERVER_HOSTNAME));
    }
}
