package ru.hse.fmcs;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitBlobObject extends GitObject implements Serializable {
    public GitBlobObject(Repository rep, byte[] data) {
        repository = rep;
        format = ObjectType.blob;
        binaryData = data;
//        GitObject.writeObject(this);
    }

    public static GitBlobObject createBlob(String filepath) throws GitException {
        try {
            byte[] fileContent = Files.readAllBytes(Path.of(filepath));
            GitBlobObject blob = new GitBlobObject(Repository.findRepository(), fileContent);
            GitObject.writeObject(blob);
            return blob;
        }
        catch(IOException e) {
            throw new GitException("Error while creating blob based on file " + filepath, e);
        }
    }
}
