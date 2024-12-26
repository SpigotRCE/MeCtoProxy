package io.github.spigotrce.mectoproxy.command;

import com.velocitypowered.api.command.SimpleCommand.Invocation;

public abstract class AbstractCommand {
    public final String alias;

    public AbstractCommand(String alias) {
        this.alias = alias;
    }

    public abstract void execute(Invocation invocation);
}
