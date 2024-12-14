package tukano.impl.storage;

import static tukano.api.Result.error;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.CONFLICT;
import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.impl.JavaShorts;
import utils.Hash;
import utils.IO;
import utils.Props;

public class DockerFilesystemStorage implements BlobStorage {
    private final String rootDir;
    private static final int CHUNK_SIZE = 4096;

    private static Logger Log = Logger.getLogger(DockerFilesystemStorage.class.getName());


    public DockerFilesystemStorage(String rootDir) {
        this.rootDir = rootDir != null ? rootDir : Props.get("BLOB_PATH");
    }

    public DockerFilesystemStorage() {
        this.rootDir = Props.get("BLOB_PATH");
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (path == null)
            return error(BAD_REQUEST);

        var file = toFile(path);

        if (file.exists()) {
            if (Arrays.equals(Hash.sha256(bytes), Hash.sha256(IO.read(file))))
                return ok();
            else
                return error(CONFLICT);
        }
        IO.write(file, bytes);
        return ok();
    }

    @Override
    public Result<byte[]> read(String path) {
        if (path == null)
            return error(BAD_REQUEST);

        var file = toFile(path);
        if (!file.exists())
            return error(NOT_FOUND);

        var bytes = IO.read(file);
        return bytes != null ? ok(bytes) : error(INTERNAL_ERROR);
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        if (path == null)
            return error(BAD_REQUEST);

        var file = toFile(path);
        if (!file.exists())
            return error(NOT_FOUND);

        IO.read(file, CHUNK_SIZE, sink);
        return ok();
    }

    @Override
    public Result<Void> delete(String path) {
        if (path == null || path.isEmpty()) {
            return error(BAD_REQUEST);
        }

        try {
            File parentDir = toFile("");
            File[] files = parentDir.listFiles((dir, name) -> name.startsWith("short_" + path));

            if (files != null) {
                Log.info(() -> "Deleting " + files.length + " files");
                for (File file : files) {
                    Log.info(() -> "Deleting file: " + file.getName());
                    if (!file.delete()) {
                        return error(INTERNAL_ERROR);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return error(INTERNAL_ERROR);
        }

        return ok();
    }

    private File toFile(String path) {
        String sanitizedPath = path.replace(":", "_"); // Replace invalid characters
        return new File(rootDir, sanitizedPath);
    }
}
