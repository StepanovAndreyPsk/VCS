package ru.hse.fmcs;

import java.nio.charset.StandardCharsets;

public class GitTagObject extends GitObject {
    public GitTagObject(Repository rep, byte[] data) {
        repository = rep;
        format = ObjectType.tag;
        binaryData = data;
    }


}
