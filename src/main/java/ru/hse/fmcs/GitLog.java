package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "log", description = "List commits that are reachable by following the parent links from the given commit(s)")
public class GitLog implements Runnable {
    @CommandLine.Parameters
    String[] from_revision;

    @Override
    public void run() {
        try {
            Repository repository = Repository.findRepository();
            if (from_revision == null) {
                repository.printLog();
            }
            else {
                repository.printLog(from_revision[0]);
            }
        }
        catch(GitException e) {
            System.out.println("Error while getting log");
            System.out.println(e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause : " + e.getCause().getMessage());
            }
        }
    }
}
