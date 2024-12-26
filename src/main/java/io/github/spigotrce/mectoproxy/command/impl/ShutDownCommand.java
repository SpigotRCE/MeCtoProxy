package io.github.spigotrce.mectoproxy.command.impl;

import io.github.spigotrce.mectoproxy.MeCtoProxy;
import io.github.spigotrce.mectoproxy.command.AbstractCommand;
import net.kyori.adventure.text.Component;

public class ShutDownCommand extends AbstractCommand {
    @Override
    public void execute(String[] arguments) {
        MeCtoProxy.PROXY_SERVER.shutdown();
    }
}
