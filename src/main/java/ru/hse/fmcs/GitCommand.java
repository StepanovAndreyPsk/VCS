package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(
        subcommands = {
                GitInit.class,
                GitAdd.class,
                GitCommit.class,
                GitStatus.class,
                GitLog.class,
                GitRm.class,
                GitCheckout.class,
                GitReset.class
        }
)
public class GitCommand implements Runnable {
    public static void main(String[] args) {
        if (WorkingDirSetter.WORKING_DIR == null) {
            WorkingDirSetter.WORKING_DIR = System.getProperty("user.dir");
        }
        new CommandLine(new GitCommand()).execute(args);
    }

    @Override
    public void run() {
        System.out.println("Git usage : ");
    }
}
