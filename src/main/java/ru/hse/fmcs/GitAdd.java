package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "add", description = "adds specified files to staging area")
public class GitAdd implements Runnable {
    @CommandLine.Parameters
    String[] paths;

    @Override
    public void run() {
        try {
            Repository repository = Repository.findRepository();
            GitIndex index = GitIndex.getIndex(repository);
            for (var path : paths) {
                index.addFile(path);
            }
        }
        catch(GitException e) {
            System.out.println("Error while adding the files to staging area");
            System.out.println(e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause : " + e.getCause().getMessage());
            }
        }
    }
}
