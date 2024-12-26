package io.github.spigotrce.mectoproxy.command;

import com.velocitypowered.api.command.SimpleCommand.Invocation;

public abstract class AbstractCommand {
    public abstract void execute(Invocation invocation);
}
