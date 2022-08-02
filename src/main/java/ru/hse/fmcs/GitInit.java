package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "init", description = "initializes an empty git repository")
public class GitInit implements Runnable {
    @Override
    public void run() {
        try {
            Repository.create(WorkingDirSetter.WORKING_DIR);
        }
        catch (GitException e) {
            System.out.println("Error while initialization: ");
            System.out.println(e.getMessage());
        }
    }
}
