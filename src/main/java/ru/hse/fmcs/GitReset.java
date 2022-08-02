package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "reset", description = "updates working directory according to revision and changes branch head")
public class GitReset implements Runnable {
    @CommandLine.Parameters
    String revision;

    @Override
    public void run() {
        try {
            Repository rep = Repository.findRepository();
            rep.reset(revision);
        }
        catch(GitException e) {
            System.out.println("Error while executing reset");
            System.out.println(e.getMessage());
            if (e.getCause() != null) {
                System.out.println(e.getCause().getMessage());
            }
        }
    }
}
