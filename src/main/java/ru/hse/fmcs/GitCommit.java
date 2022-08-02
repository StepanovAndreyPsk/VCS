package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "commit", description = "making commit")
public class GitCommit implements Runnable {
    @CommandLine.Parameters
    String message;
    public void run() {
        try {
            Repository rep = Repository.findRepository();
            rep.commit(message);
        }
        catch(GitException e) {
            System.out.println("Error while making commit");
            System.out.println(e.getMessage());
        }
    }
}
