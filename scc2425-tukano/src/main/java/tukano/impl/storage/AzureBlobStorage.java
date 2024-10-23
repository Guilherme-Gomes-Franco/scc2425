package tukano.impl.storage;

import tukano.api.Result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.CONFLICT;
import static tukano.api.Result.error;
import static tukano.api.Result.ok;
import utils.Hash;

public class AzureBlobStorage implements tukano.impl.storage.BlobStorage {
    private static final String STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName=scc2323;AccountKey=8gfHhcOfIAtd+mZkMcYCNwW1rLYHvKvimgfplN/0PWl4ceca+qjkBUWpwJoX4bln8eRk/Nq2431j+ASt9TXmRw==;EndpointSuffix=core.windows.net";
    private static final String BLOBS_CONTAINER_NAME = "blobs";
    private final BlobContainerClient containerClient;

    public AzureBlobStorage() {
        containerClient = new BlobContainerClientBuilder()
                .connectionString(STORAGE_CONNECTION_STRING)
                .containerName(BLOBS_CONTAINER_NAME)
                .buildClient();
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (path == null)
            return error(BAD_REQUEST);

        BlobClient blob = containerClient.getBlobClient(path);

        // check if file exists in the container
        if (blob.exists()) {
            if (Arrays.equals(Hash.sha256(bytes), Hash.sha256(blob.downloadContent().toBytes())))
                return ok();
            else
                return error(CONFLICT);

        }
        blob.upload(BinaryData.fromBytes(bytes));
        return ok();
    }

    @Override
    public Result<Void> delete(String path) {
        if (path == null)
            return error(BAD_REQUEST);

        BlobClient blob = containerClient.getBlobClient(path);

        if (blob.exists()) {
            System.out.println("DELETE>>>>" + path);
            blob.delete();
        }
        return ok();
    }

    @Override
    public Result<byte[]> read(String path) {
        if (path == null)
            return error(BAD_REQUEST);

        BlobClient blob = containerClient.getBlobClient(path);

        if (!blob.exists())
            return error(Result.ErrorCode.NOT_FOUND);

        System.out.println("READ>>>>" + path);
        byte[] arr = blob.downloadContent().toBytes();
        return ok(arr);
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        if (path == null)
            return error(BAD_REQUEST);

        BlobClient blob = containerClient.getBlobClient(path);

        if (!blob.exists())
            return error(Result.ErrorCode.NOT_FOUND);

        sink.accept(blob.downloadContent().toBytes());
        return ok();
    }
}
