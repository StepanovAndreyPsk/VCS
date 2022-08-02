package ru.hse.fmcs;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Repository implements Serializable {
    private final String workingDir;

    private Repository(String path) throws GitException {
        workingDir = path;
        if (Files.exists(getHEADPath())) {
            setBranch();
        }
    }

    public static @NotNull Repository create(String path) throws GitException {
        Repository rep = new Repository(path);
        Path workingDirPath = Path.of(rep.workingDir);
        if (Files.exists(workingDirPath)) {
            if (!Files.isDirectory(workingDirPath)) {
                throw new GitException(rep.workingDir + " is not a directory");
            }
        } else {
            try {
                Files.createDirectories(workingDirPath);
            } catch (IOException e) {
                throw new GitException("Error while creating working directory");
            }
        }
        // Creating folder structure in .git
        try {
            rep.buildDirPath("branches");
            rep.buildDirPath("objects");
            rep.buildDirPath("refs/tags");
            rep.buildDirPath("refs/heads");

            createDefaultDescription(rep);
            createDefaultHead(rep);
            createDefaultConfig(rep);
            rep.setBranch();
        } catch (IOException e) {
            throw new GitException("Error while creating git files", e);
        }
        System.out.println("Project initialized");
        return rep;
    }

    // searching .git in the ancestor folders; returning instance of Repository or null if failed
    public static @Nullable Repository findRepository(String p) throws GitException {
        Path path = Path.of(p);
        Path root = path.getRoot();
        try {
            while (!Files.isSameFile(path, root)) {
                Path expectedGitFolderPath = path.resolve("git");
                if (Files.exists(expectedGitFolderPath)) {
                    Repository repository = new Repository(path.toString()); // using private constructor that doesn't validate the files in the path
                    performChecks(repository);                               // validating repository structure and config file
                    return repository;
                }
            }
        } catch (IOException e) {
            throw new GitException("Unexpected error while comparing paths", e);
        }
        return null;
    }

    private void setBranch() throws GitException {
        try {
            String head = Files.readString(getHEADPath());
//            branch = head.replace("ref: refs/heads/", "");
        } catch (IOException e) {
            throw new GitException("Error while reading HEAD");
        }
    }

    public static Repository findRepository() throws GitException {
        return findRepository(WorkingDirSetter.WORKING_DIR);
    }

    public Path getGitDir() {
        return getWorkingDirPath().resolve("git");
    }

    public Path getHEADPath() {
        return getGitDir().resolve("HEAD");
    }

    public Path getIndexPath() {
        return getGitDir().resolve("index");
    }

    public Path getWorkingDirPath() {
        return Path.of(workingDir);
    }

    private static void performChecks(@NotNull Repository repository) throws GitException {
        // Making sure that the directory structure and config are valid
        if (!Files.exists(repository.getGitDir())) {
            throw new GitException("Not a git repository");
        }
        try {
            Ini cf = new Ini(repository.getPath("config").toFile());
            int version = Integer.parseInt(cf.get("core", "repositoryformatversion"));
            if (version != 0) {
                throw new GitException("Unsupported repository format version");
            }
        } catch (InvalidFileFormatException e) {
            throw new GitException("Error while parsing config file", e);
        } catch (IOException e) {
            throw new GitException("Couldn't find config file", e);
        }
    }

    private static void createDefaultConfig(@NotNull Repository rep) throws IOException {
        File config = new File(rep.getPath("config").toString());
        if (config.createNewFile()) {
            Ini iniConfig = new Ini(config);
            iniConfig.put("core", "repositoryformatversion", "0");      // sets the format to initial (no extensions)
            iniConfig.put("core", "filemode", "false");                 // disables filemode tracking
            iniConfig.put("core", "bare", "false");                     // indicates worktree presence
            iniConfig.store();
        }
    }

    private static void createDefaultHead(@NotNull Repository rep) throws IOException {
        String headPath = rep.getPath("HEAD").toString();
        File head = new File(headPath);
        if (head.createNewFile()) {
            FileWriter writer = new FileWriter(head);
            writer.write("ref: refs/heads/master");
            writer.close();
        }
    }

    private static void createDefaultDescription(@NotNull Repository rep) throws IOException {
        String descriptionPath = rep.getPath("description").toString();
        File description = new File(descriptionPath);
        if (description.createNewFile()) {
            FileWriter writer = new FileWriter(description);
            writer.write("Edit this file to name the repository\n");
            writer.close();
        }
    }

    public Path getPath(String path) {
        if (Path.of(path).isAbsolute()) {
            return Path.of(path);
        }
        return getGitDir().resolve(path);
    }

    public @NotNull Path buildDirPath(String p) throws GitException {
        Path path = getPath(p);

        if (Files.exists(path)) {
            return path;
        }
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new GitException("Error while creating file structure", e);
        }
        return path;
    }

    public void buildFilePath(String p) throws GitException {
        Path path = getPath(p);
        path = path.getParent(); // remove file name from the path
        buildDirPath(path.toString());
    }

    // returns SHA of the head commit
    public String getHead() throws GitException {
        Path headFilePath = getGitDir().resolve("HEAD");
        try {
            String headID = Files.readString(headFilePath);
            if (headID.startsWith("ref: ")) {
                String refPathStr = headID.replace("ref: ", "");
                Path refPath = getGitDir().resolve(refPathStr);
                if (Files.exists(refPath)) {
                    return Files.readString(refPath);
                } else {
                    // no commits were made
                    return null;
                }
            } else {
                // Detached HEAD
                assert (headID.length() == 40); // HEAD should hold the commit sha
                return headID;
            }
        } catch (IOException e) {
            throw new GitException("Error while reading HEAD file");
        }
    }

    public String getRelativePath(String absolutePath) {
        return getWorkingDirPath().relativize(Path.of(absolutePath)).toString();
    }

    public boolean headIsDetached() throws GitException {
        Path headFilePath = getGitDir().resolve("HEAD");
        try {
            String headID = Files.readString(headFilePath);
            return !headID.startsWith("ref: ");
        } catch (IOException e) {
            throw new GitException("Error while reading HEAD file");
        }
    }

    private void updateBranchHead(String sha) throws GitException {
        File ref = new File(getGitDir().resolve("refs").resolve("heads").resolve(getCurrentBranch()).toString());
        try (FileWriter writer = new FileWriter(ref)) {
            writer.write(sha);
        } catch (IOException e) {
            throw new GitException("Error while updating branch head");
        }
    }

    public void commit(String message) throws GitException {
        if (headIsDetached()) {
            throw new GitException("Attempting to commit from detached HEAD. Aborting...");
        }
        GitIndex index = GitIndex.getIndex(this); // getting staged area
//        if (index.isEmpty()) {
//            throw new GitException("Nothing to commit");
//        }
        String parentCommitSha = getHead();
        String treeSha = GitObject.getObjectHash(index.tree);
        if (parentCommitSha != null) {
            GitCommitObject parentCommit = (GitCommitObject) GitObject.readObject(this, parentCommitSha);
            String parentCommitTreeSha = parentCommit.treeSha;
            if (parentCommitTreeSha.equals(treeSha)) {
                throw new GitException("Nothing to commit");
            }
        }
        GitObject.writeObject(index.tree);
        GitCommitObject commit = new GitCommitObject(message, treeSha, parentCommitSha);
        updateBranchHead(GitObject.writeObject(commit));
        System.out.println("Files committed");
    }

    public String getCurrentBranch() {
        return "master";
//        return branch;
    }

    private String getCommitInfo(String sha) throws GitException {
        GitObject obj = GitObject.readObject(this, sha);
        if (obj.format != GitObject.ObjectType.commit) {
            throw new GitException("Unexpected GitObject type while reading commit info");
        }
        GitCommitObject commit = (GitCommitObject) obj;
        StringBuilder sb = new StringBuilder();
        sb.append("commit ")
                .append(sha)
                .append('\n')
                .append("Date:\t");
        try {
            FileTime fileTime = Files.getLastModifiedTime(GitObject.getObjectPath(this, sha));
            LocalDateTime localDateTime = fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE LLL dd HH:mm:ss yyyy");
            sb.append(localDateTime.format(formatter))
                    .append("\n\n\t")
                    .append(commit.message)
                    .append("\n\n");
        } catch (IOException e) {
            throw new GitException("Error while getting object modification time");
        }
        return sb.toString();
    }

    private @Nullable String getBranchHead(String branch) throws GitException {
        Path branchRefPath = getGitDir().resolve("refs").resolve("heads").resolve(branch);
        if (Files.exists(branchRefPath)) {
            try {
                return Files.readString(branchRefPath);
            } catch (IOException e) {
                throw new GitException("Error while getting head of branch " + branch, e);
            }
        }
        return null;
    }

    private String convertRevisionToSha(String revision) throws GitException {
        if (revision.length() == 40 && revision.matches("[0-9a-fA-F]+")) {
            return revision;
        } else if (revision.startsWith("HEAD~")) {
            int n = Integer.parseInt(revision.replace("HEAD~", ""));
            return GitCommitObject.getNthAncestor(this, getHead(), n);
        } else {
            return getBranchHead(revision);
        }
    }

    public void printLog(String revision) throws GitException {
        StringBuilder sb = new StringBuilder();
        String curCommitSha = convertRevisionToSha(revision);
        while (curCommitSha != null) {
            sb.append(getCommitInfo(curCommitSha));
            GitCommitObject commit = (GitCommitObject) GitObject.readObject(this, curCommitSha);
            curCommitSha = commit.parentCommitSha;
        }
        System.out.println(sb);
    }

    public void printLog() throws GitException {
        printLog(getHead());
    }

    private boolean branchExists(String branch) {
        return Files.exists(getGitDir().resolve("refs").resolve("heads").resolve(branch));
    }

    private void changeHead(String revision) throws GitException {
        if (getHead().equals(convertRevisionToSha(revision))) {
            return;
        }
        String newHEADContent;
        if (revision.length() == 40 && revision.matches("[0-9a-fA-F]+")) {
            newHEADContent = revision;
        } else if (revision.startsWith("HEAD~")) {
            int n = Integer.parseInt(revision.replace("HEAD~", ""));
            newHEADContent = GitCommitObject.getNthAncestor(this, getHead(), n);
            if (newHEADContent == null) {
                throw new GitException("Couldn't find specified commit");
            }
        } else {
            if (!branchExists(revision)) {
                throw new GitException("specified branch doesn't exist");
            }
            newHEADContent = "ref: refs/heads/" + revision;
        }
        File HEAD = new File(getHEADPath().toString());
        try (FileWriter writer = new FileWriter(HEAD)) {
            writer.write(newHEADContent);
        } catch (IOException e) {
            throw new GitException("Error while writing to HEAD");
        }
    }

    private void updateSingleFileContent(String filePath, String blobSha) throws GitException {
        String absolutePathStr = this.getAbsPathInWorkDir(filePath);
        File file = new File(absolutePathStr);
        try (FileWriter writer = new FileWriter(file)) {
            GitBlobObject blob = (GitBlobObject) GitObject.readObject(this, blobSha);
            writer.write(new String(blob.binaryData));
        } catch (IOException e) {
            throw new GitException("Error while rewriting content of file " + filePath, e);
        }
    }

    private void updateFilesContent(GitIndex index) throws GitException {
        for (var indexItem : index.stagedItemsSet) {
            updateSingleFileContent(indexItem.filePath, indexItem.sha);
        }
    }

    private void updateWorkDirToRevision(String revision) throws GitException {
        String commitSha = convertRevisionToSha(revision);
        GitObject obj = GitObject.readObject(this, commitSha);
        if (obj.format != GitObject.ObjectType.commit) {
            throw new GitException("specified revision isn't commit object");
        }
        GitCommitObject commit = (GitCommitObject) obj;
        commit.index.writeIndex();  // updating index according to specified commit
        updateFilesContent(commit.index);
    }

    public void checkout(String revision) throws GitException {
        String commitSha = convertRevisionToSha(revision);
        changeHead(revision);
        updateWorkDirToRevision(commitSha);
        System.out.println("Checkout completed successful");
    }

    public void reset(String revision) throws GitException {
        updateWorkDirToRevision(revision);
        updateBranchHead(convertRevisionToSha(revision));
    }

    public void checkoutFile(String filePath) throws GitException {
        String commitSha = getHead();
        if (commitSha == null) {
            throw new GitException("previous commit doesn't exist");
        }
        GitCommitObject commit = (GitCommitObject) GitObject.readObject(this, commitSha);
        String blobSha = commit.index.pathToIndexItem.get(filePath).sha;
        updateSingleFileContent(filePath, blobSha);
        System.out.println("Checkout completed successful");
    }

    // file.txt -> absolute path: ~/.../workdir/file.txt
    public String getAbsPathInWorkDir(String pathStr) {
        return Path.of(workingDir).resolve(pathStr).toString();
    }
}