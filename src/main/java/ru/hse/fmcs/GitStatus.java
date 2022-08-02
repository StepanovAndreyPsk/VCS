package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "status", description = "Displays paths that have differences between the index file and the current HEAD commit, paths\n" +
        "       that have differences between the working tree and the index file, and paths in the working\n" +
        "       tree that are not tracked by Git")
public class GitStatus implements Runnable {
    @Override
    public void run() {
        try {
            Repository repository = Repository.findRepository();
            GitIndex index = GitIndex.getIndex(repository);
            index.printStatus();
        }
        catch(GitException e) {
            System.out.println("Error while getting status");
            System.out.println(e.getMessage());
            if (e.getCause() != null) {
                System.out.println(e.getCause().getMessage());
            }
        }
    }
}
