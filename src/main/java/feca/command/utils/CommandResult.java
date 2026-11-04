package feca.command.utils;

import com.mojang.brigadier.Command;

/// 命令返回值约定
public interface CommandResult {
    /// 成功
    int SUCCESS = Command.SINGLE_SUCCESS;
    /// 失败
    int FAILURE = SUCCESS - 1;
}
