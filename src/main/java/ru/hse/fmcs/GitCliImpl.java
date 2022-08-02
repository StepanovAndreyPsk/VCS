package ru.hse.fmcs;

import org.jetbrains.annotations.NotNull;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class GitCliImpl implements GitCli {

    public GitCliImpl(String workingDirectory) {
        WorkingDirSetter.WORKING_DIR = workingDirectory;
    }
    @Override
    public void runCommand(@NotNull String command, @NotNull List<@NotNull String> arguments) throws GitException {
        String[] args = new String[arguments.size() + 1];
        args[0] = command;
        for (int i = 0; i < arguments.size(); i++) {
            args[i + 1] = arguments.get(i);
        }
        GitCommand.main(args);
    }

    @Override
    public void setOutputStream(@NotNull PrintStream outputStream) {
        System.setOut(outputStream);
    }

    @Override
    public @NotNull String getRelativeRevisionFromHead(int n) throws GitException {
        return "HEAD~" + n;
    }
}
