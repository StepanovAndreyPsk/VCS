package ru.hse.fmcs;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;

public abstract class GitObject implements Serializable {
    Repository repository;
    byte[] binaryData;
    ObjectType format;
    String sha;

    enum ObjectType {blob, commit, tree, tag}

    protected GitObject() {
    }

    public static @NotNull GitObject readObject(@NotNull Repository repository, @NotNull String sha) throws GitException {
        String objPathStr = getObjectPath(repository, sha).toString();
        try (FileInputStream fis = new FileInputStream(objPathStr);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (GitObject) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new GitException("Error while reading git object", e);
        }
    }

    public static @NotNull String writeObject(@NotNull GitObject obj) throws GitException {
        String sha = GitObject.getObjectHash(obj);
        String objPathStr = getObjectPath(obj.repository, sha).toString();
        if (!createFileForObject(obj.repository, sha)) {
            // git object has already been written
            return sha;
        }
        try (FileOutputStream fos = new FileOutputStream(objPathStr);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
            return sha;
        } catch (IOException e) {
            throw new GitException("Error while writing git object");
        }
    }

    public static String getObjectHash(GitObject obj) throws GitException {
        byte[] objRepresentation = getObjectRepresentation(obj);
        return computeSHA1(objRepresentation);
    }

    private static byte[] getObjectRepresentation(GitObject obj) throws GitException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (obj.format == ObjectType.blob) {
                GitBlobObject blob = (GitBlobObject) obj;
                baos.write(blob.binaryData);
            }
            else if (obj.format == ObjectType.commit) {
                GitCommitObject commit = (GitCommitObject) obj;
                if (commit.parentCommitSha != null) {
                    baos.write(commit.parentCommitSha.getBytes(StandardCharsets.UTF_8));
                }
                baos.write(commit.treeSha.getBytes(StandardCharsets.UTF_8));
                baos.write(commit.message.getBytes(StandardCharsets.UTF_8));
            }
            else if (obj.format == ObjectType.tree) {
                GitTreeObject tree = (GitTreeObject) obj;
                for (var elem : tree.elements) {
                    baos.write(elem.name.getBytes(StandardCharsets.UTF_8));
                    baos.write(elem.sha.getBytes(StandardCharsets.UTF_8));
                    baos.write(elem.type.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            else if (obj.format == ObjectType.tag) {
                assert(false);
//                GitTagObject tag = (GitTagObject) obj;
//                oos.writeObject(tag);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new GitException("Error while writing to git object", e);
        }
    }

    public static Path getObjectPath(Repository rep, String sha) {
        return rep.getGitDir().resolve("objects").resolve(sha.substring(0, 2)).resolve(sha.substring(2));
    }

    public static boolean createFileForObject(Repository repository, String sha) throws GitException {
        String objPathStr = getObjectPath(repository, sha).toString();
        repository.buildFilePath(objPathStr);
        try {
            return new File(objPathStr).createNewFile();
        }
        catch (IOException e) {
            throw new GitException("Error while creating file for git object");
        }
    }

    public static ObjectType getObjectType(String sha) throws GitException {
        GitObject obj = readObject(Repository.findRepository(), sha);
        if (obj instanceof GitBlobObject) {
            return ObjectType.blob;
        } else if (obj instanceof GitTreeObject) {
            return ObjectType.tree;
        } else if (obj instanceof GitCommitObject) {
            return ObjectType.commit;
        } else if (obj instanceof GitTagObject) {
            return ObjectType.tag;
        }
        throw new GitException("Unknown Git Object type");
    }

    public static @NotNull String computeSHA1(byte[] data) throws GitException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(data);
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new GitException("Invalid hashing algorithm", e);
        }
    }
}
