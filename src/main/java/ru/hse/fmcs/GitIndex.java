package ru.hse.fmcs;

import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// This class represents current staging area
public class GitIndex implements Serializable {
    Repository repository;
    File indexFile;
    TreeSet<IndexItem> stagedItemsSet;
    Map<String, IndexItem> pathToIndexItem;
    GitTreeObject tree;

    public static class IndexItem implements Comparable<IndexItem>, Serializable {
        public String filePath;
        public String sha;
        public String lastModified;

        public IndexItem(String path, String hash, FileTime time) {
            filePath = path;
            sha = hash;
            lastModified = FileTimeFormatter.fileTimeToString(time);
        }

        @Override
        public int compareTo(@NotNull GitIndex.IndexItem o) {
            return filePath.compareTo(o.filePath);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof IndexItem) {
                IndexItem other = (IndexItem) o;
                return filePath.equals(other.filePath);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return filePath.hashCode();
        }
    }

    private GitIndex(@NotNull Repository rep) throws GitException {
        // should be called only if index doesn't exist
        repository = rep;
        indexFile = new File(repository.getGitDir().resolve("index").toString());
        assert (!indexFile.exists());
        tree = new GitTreeObject();
        stagedItemsSet = new TreeSet<>();
        pathToIndexItem = new HashMap<>();
    }

    public static GitIndex getIndex(@NotNull Repository repository) throws GitException {
        if (Files.exists(repository.getIndexPath())) {
            return readIndex(repository);
        }
        return new GitIndex(repository);
    }

    private static GitIndex readIndex(Repository repository) throws GitException {
        try (FileInputStream fis = new FileInputStream(repository.getGitDir().resolve("index").toString());
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (GitIndex) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new GitException("Error while index deserialization");
        }
    }

    public void writeIndex() throws GitException {
        try (FileOutputStream fos = new FileOutputStream(repository.getGitDir().resolve("index").toString());
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this);
            oos.flush();
        } catch (IOException e) {
            throw new GitException("Error while serializing index", e);
        }
    }

    public void addFile(String path) throws GitException {
        try {
            if (!indexFile.exists()) {
                indexFile.createNewFile();
            }
            // path should be relative to the repository root folder!
            File file = new File(repository.getAbsPathInWorkDir(path));
            if (!file.exists()) {
                throw new GitException("File doesn't exist");
            }

            GitBlobObject blob = GitBlobObject.createBlob(file.getAbsolutePath());
            IndexItem item = new IndexItem(repository.getRelativePath(file.getAbsolutePath()), GitObject.getObjectHash(blob), Files.getLastModifiedTime(file.toPath()));
            if (stagedItemsSet.contains(item)) {
                assert (Objects.equals(stagedItemsSet.ceiling(item), stagedItemsSet.floor(item)));
                IndexItem prev = stagedItemsSet.ceiling(item);
                assert prev != null;
                if (!prev.lastModified.equals(item.lastModified)) {
                    stagedItemsSet.remove(prev);
                    stagedItemsSet.add(item);
                    pathToIndexItem.put(repository.getRelativePath(file.getAbsolutePath()), item);
                    tree.descendAndRewrite(path, blob);
                    writeIndex();
                    System.out.println("Add completed successfully");
                    return;
                }
                throw new GitException("File is already in staged area");
            } else {
                stagedItemsSet.add(item);
                pathToIndexItem.put(repository.getRelativePath(file.getAbsolutePath()), item);
            }
            tree.descendAndRewrite(path, blob);
            writeIndex();
        } catch (IOException e) {
            throw new GitException("Couldn't read from the file", e);
        }
        System.out.println("Add completed successfully");
    }

    public void removeFile(String path) throws GitException {
        File file = new File(repository.getAbsPathInWorkDir(path));
        String relativePath = repository.getRelativePath(file.getAbsolutePath());
        if (!file.exists()) {
            throw new GitException("File doesn't exist");
        }
        if (pathToIndexItem.get(relativePath) == null) {
            throw new GitException("File isn't being tracked");
        } else {
            stagedItemsSet.remove(pathToIndexItem.get(relativePath));
            pathToIndexItem.remove(relativePath);
            tree.descendAndRewrite(relativePath, null);
        }
        writeIndex();
        System.out.println("Rm completed successful");
    }

    public void printStatus() throws GitException {
        Set<String> untrackedFiles = new HashSet<>();
        Set<String> changesToBeCommitted = new HashSet<>();
        Set<String> changesNotStagedForCommit = new HashSet<>();
        Set<String> removeFiles = new HashSet<>();
        updateFilesInfo(untrackedFiles, changesToBeCommitted, changesNotStagedForCommit, removeFiles);
        StringBuilder sb = new StringBuilder();
        sb.append("On branch ")
                .append('\'')
                .append(repository.getCurrentBranch())
                .append('\'')
                .append('\n');
        if (untrackedFiles.isEmpty() && changesToBeCommitted.isEmpty() && changesNotStagedForCommit.isEmpty() && removeFiles.isEmpty()) {
            sb.append("Everything is up to date");
        }
        if (!changesToBeCommitted.isEmpty()) {
            sb.append("Changes to be committed:\n");
            for (var item : changesToBeCommitted) {
                sb.append("@|green \t\tmodified:\t")
                        .append(item)
                        .append("\n|@");
            }
        }
        if (!changesNotStagedForCommit.isEmpty()) {
            sb.append("Changes not staged for commit:\n");
            for (var file : changesNotStagedForCommit) {
                sb.append("@|red \t\tmodified:\t")
                        .append(file)
                        .append("\n|@");
            }
        }
        if (!removeFiles.isEmpty()) {
            sb.append("Removed files:\n");
            for (var file : removeFiles) {
                sb.append("@|red \t\tremoved:\t")
                        .append(file)
                        .append("\n|@");
            }
        }
        if (!untrackedFiles.isEmpty()) {
            sb.append("Untracked files:\n");
            for (var file : untrackedFiles) {
                sb.append("@|red \t\tmodified:\t")
                        .append(file)
                        .append("\n|@");
            }
        }
        String response = CommandLine.Help.Ansi.AUTO.string(sb.toString());
        System.out.println(response);
    }

    private String getFileHash(Path absolutePath) throws GitException {
        try {
            return GitObject.computeSHA1(Files.readAllBytes(absolutePath));
        }
        catch(IOException e) {
            throw new GitException("Error while reading bytes from file " + absolutePath);
        }
    }

    public void updateFilesInfo(Set<String> untrackedFiles, Set<String> changedToBeCommitted, Set<String> changesNotStagedForCommit, Set<String> removedFiles) throws GitException {
        Path workingDirPath = repository.getWorkingDirPath();
        GitCommitObject headCommit = null;
        if (repository.getHead() != null) {
            headCommit = (GitCommitObject) GitObject.readObject(repository, repository.getHead());
        }
        try (Stream<Path> paths = Files.walk(workingDirPath)) {
            List<Path> pathList = paths.filter(path -> !path.startsWith(repository.getGitDir()) && !path.equals(workingDirPath) && Files.isRegularFile(path)).map(workingDirPath::relativize).collect(Collectors.toList());
            for (var relativePath : pathList) {
                String relativePathStr = relativePath.toString();
                Path absolutePath = Path.of(repository.getAbsPathInWorkDir(relativePathStr));
                IndexItem item = pathToIndexItem.get(relativePathStr);
                if (item == null) {
                    if (headCommit != null && headCommit.checkIfCommitted(relativePathStr, null)) {
                        removedFiles.add(relativePathStr);
                    } else {
                        untrackedFiles.add(relativePathStr);
                    }
                } else {
                    String realFileSha = getFileHash(absolutePath);
                    if ((headCommit == null || !headCommit.checkIfCommitted(item.filePath, item.sha))) {
                        if (item.sha.equals(realFileSha)) {
                            changedToBeCommitted.add(relativePathStr);
                        } else {
                            changesNotStagedForCommit.add(relativePathStr);
                        }
                    } else {
                        // file was committed earlier
                        if (!item.sha.equals(realFileSha)) {
                            changesNotStagedForCommit.add(relativePathStr);
                        } else {
                            IndexItem prevIndexItem = headCommit.index.pathToIndexItem.get(relativePathStr);
                            if (!prevIndexItem.sha.equals(realFileSha)) {
                                changedToBeCommitted.add(relativePathStr);
                            }
                        }
                    }
                }
            }
            for (var indexItem : stagedItemsSet) {
                Path absolutePath = Path.of(repository.getAbsPathInWorkDir(indexItem.filePath));
                String relativePathStr = indexItem.filePath;
                if (!Files.exists(absolutePath)) {
                    removedFiles.add(relativePathStr);
                }
            }
        } catch (IOException e) {
            throw new GitException("Error while walking the working directory");
        }
    }
}
