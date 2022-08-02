package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "rm", description = "removes files from index")
public class GitRm implements Runnable {
    @CommandLine.Parameters
    String[] paths;

    @Override
    public void run() {
        try {
            Repository repository = Repository.findRepository();
            GitIndex index = GitIndex.getIndex(repository);
            for (var path : paths) {
                index.removeFile(path);
            }
        }
        catch (GitException e) {
            System.out.println("Error while removing file");
            System.out.println(e.getMessage());
            if (e.getCause() != null) {
                System.out.println(e.getCause().getMessage());
            }
        }
    }
}
