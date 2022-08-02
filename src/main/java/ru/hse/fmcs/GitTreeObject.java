package ru.hse.fmcs;

import com.sun.source.tree.Tree;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

public class GitTreeObject extends GitObject implements Serializable {
    public final HashSet<TreeElement> elements;
    public final HashMap<String, TreeElement> nameToTreeElement;

    public static class TreeElement implements Serializable {
        // ? access bits (advanced)
        public ObjectType type;    // subtree or blob
        public String sha;         // identifier
        public String name;        // file name or directory name

        TreeElement(ObjectType tp, String shaHash, String itemName) {
            type = tp;
            sha = shaHash;
            name = itemName;
        }

        TreeElement(GitObject obj) throws GitException {
            sha = GitObject.getObjectHash(obj);
            type = obj.format;
            if (type != ObjectType.blob && type != ObjectType.tree) {
                throw new GitException("Unsupported GitObject type in tree object");
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TreeElement) {
                TreeElement other = (TreeElement) o;
                return (other.name + other.sha).equals(name + sha);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (name + sha).hashCode();
        }
    }

    public GitTreeObject(Repository rep, byte[] data) {
        repository = rep;
        format = ObjectType.tree;
        binaryData = data;
        elements = new HashSet<>();
        nameToTreeElement = new HashMap<>();
    }

    public GitTreeObject() throws GitException {
        repository = Repository.findRepository();
        format = ObjectType.tree;
        binaryData = null;
        elements = new HashSet<>();
        nameToTreeElement = new HashMap<>();
    }

    public void descendAndRewrite(String filepath, GitBlobObject blob) throws GitException {
        Path path = Path.of(filepath);
        if (path.getNameCount() == 1) {
            if (blob != null) {
                addOrReplaceElement(path.toString(), blob);
            } else {
                removeElement(path.toString());
            }
            return;
        }
        String subDirName = path.subpath(0, 1).toString();
        TreeElement subtreeElem = nameToTreeElement.get(subDirName);
        Path pathSuf = path.subpath(1, path.getNameCount());
        GitTreeObject subtree = (subtreeElem != null) ? (GitTreeObject) GitObject.readObject(repository, subtreeElem.sha) : new GitTreeObject();
        if (subtreeElem != null) {
            // sha of the subtree is going to change => deleting from elements old subtree
            elements.remove(subtreeElem);
        }
        subtree.descendAndRewrite(pathSuf.toString(), blob);
        String subtreeSha = GitObject.writeObject(subtree);
        elements.add(new TreeElement(ObjectType.tree, subtreeSha, subDirName));
    }

    public boolean checkIfMatch(String filepath, String blobSha) throws GitException {
        Path path = Path.of(filepath);
        if (path.getNameCount() == 1) {
            return elements.contains(new TreeElement(ObjectType.blob, blobSha, path.toString()));
        }
        String subDirName = path.subpath(0, 1).toString();
        TreeElement subtreeElem = nameToTreeElement.get(subDirName);
        Path pathSuf = path.subpath(1, path.getNameCount());
        GitTreeObject subtree = (subtreeElem != null) ? (GitTreeObject) GitObject.readObject(repository, subtreeElem.sha) : new GitTreeObject();
        return subtree.checkIfMatch(pathSuf.toString(), blobSha);
    }

    public void addOrReplaceElement(String name, GitObject obj) throws GitException {
        if (obj instanceof GitBlobObject) {
//            TreeElement elem = getElement(name);
            TreeElement elem = nameToTreeElement.get(name);
            if (elem != null) {
                elements.remove(elem);
            }
            var newElement = new TreeElement(obj.format, GitObject.getObjectHash(obj), name);
            elements.add(newElement);
            nameToTreeElement.put(name, newElement);
        } else if (obj instanceof GitTreeObject) {
            elements.add(new TreeElement(ObjectType.tree, GitObject.getObjectHash(obj), name));
        } else {
            throw new GitException("Attempt to add unsupported GitObject type to git tree");
        }
    }

    public void removeElement(String name) {
        TreeElement elem = nameToTreeElement.get(name);
        if (elem != null) {
            elements.remove(elem);
            nameToTreeElement.remove(name);
        }
    }
}
