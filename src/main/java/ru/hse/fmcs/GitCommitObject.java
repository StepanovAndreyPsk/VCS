package ru.hse.fmcs;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class GitCommitObject extends GitObject implements Serializable {
    String parentCommitSha;
    String treeSha;
    String message;
    GitIndex index;

    public GitCommitObject(String msg, String tree, @Nullable String parent) throws GitException {
        repository = Repository.findRepository();
        format = ObjectType.commit;
        message = msg;
        treeSha = tree;
        parentCommitSha = parent;
        index = GitIndex.getIndex(repository);
    }

    // return Nth ancestor commit sha
    public static String getNthAncestor(Repository rep, String curSha, int n) throws GitException {
        while (curSha != null && n != 0) {
            GitCommitObject curCommit = (GitCommitObject) GitObject.readObject(rep, curSha);
            curSha = curCommit.parentCommitSha;
            n--;
        }
        return curSha;
    }

    public boolean checkIfCommitted(String filePath, String sha) throws GitException {
        GitTreeObject tree = (GitTreeObject) GitObject.readObject(repository, treeSha);
        return tree.checkIfMatch(filePath, sha);
    }

}
