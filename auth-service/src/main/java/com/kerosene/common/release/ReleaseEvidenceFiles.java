package com.kerosene.common.release;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;

public final class ReleaseEvidenceFiles {
    private ReleaseEvidenceFiles() {}
    public static Path directory(String value) throws IOException {
        Path path = path(value);
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("not a directory");
        var mode = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
        if (mode.contains(PosixFilePermission.GROUP_WRITE) || mode.contains(PosixFilePermission.OTHERS_WRITE))
            throw new IOException("writable shared target directory");
        return path;
    }
    private static Path path(String value) throws IOException {
        Path path = Path.of(value);
        if (!path.isAbsolute() || !path.normalize().equals(path)) throw new IOException("absolute normalized path required");
        for (Path cursor = path; cursor != null; cursor = cursor.getParent())
            if (Files.isSymbolicLink(cursor)) throw new IOException("symlink not allowed");
        return path;
    }
    public static byte[] read(Path supplied, int limit, boolean secret) throws IOException {
        if (limit < 1 || limit > 1048576) throw new IllegalArgumentException("bounded runtime file limit required");
        Path path = path(supplied.toString());
        var attrs = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isRegularFile() || attrs.size() == 0 || attrs.size() > limit) throw new IOException("invalid file size/type");
        var mode = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
        if (mode.contains(PosixFilePermission.GROUP_WRITE) || mode.contains(PosixFilePermission.OTHERS_WRITE)
                || (secret && mode.stream().anyMatch(p -> !p.name().startsWith("OWNER_"))))
            throw new IOException("unsafe file permissions");
        try (var channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            var bytes = ByteBuffer.allocate(limit + 1);
            while (bytes.hasRemaining() && channel.read(bytes) != -1) { }
            if (bytes.position() == 0 || bytes.position() > limit) throw new IOException("bounded read exceeded");
            byte[] result = java.util.Arrays.copyOf(bytes.array(), bytes.position());
            java.util.Arrays.fill(bytes.array(), (byte) 0);
            return result;
        }
    }
}
