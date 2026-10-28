package feca.command.utils;

import com.mojang.brigadier.Command;

public interface CommandResult {
    int SUCCESS = Command.SINGLE_SUCCESS;
    int FAILURE = SUCCESS - 1;
}
