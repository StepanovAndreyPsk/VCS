package ru.hse.fmcs;

import picocli.CommandLine;

@CommandLine.Command(name = "checkout", description = "restore working tree files")
public class GitCheckout implements Runnable {
    @CommandLine.Option(names = "-f")
    boolean checkoutFiles;

    @CommandLine.Parameters
    String[] parameters;

    @Override
    public void run() {
        try {
            Repository rep = Repository.findRepository();
            if (checkoutFiles) {
                for (String parameter : parameters) {
                    rep.checkoutFile(parameter);
                }
            }
            else {
                rep.checkout(parameters[0]);
            }
        }
        catch (GitException e) {
            System.out.println("Error while executing checkout");
            System.out.println(e.getMessage());
            if (e.getCause() != null) {
                System.out.println(e.getCause().getMessage());
            }
        }
    }
}
