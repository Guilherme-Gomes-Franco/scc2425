package tukano.impl.storage;

import com.azure.storage.blob.models.BlobItem;
import tukano.api.Result;

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
import utils.Props;

public class AzureBlobStorage implements BlobStorage {
    private final BlobContainerClient containerClient;

    public AzureBlobStorage() {
        containerClient = new BlobContainerClientBuilder()
                .connectionString(Props.get("BLOB_STORE_CONNECTION"))
                .containerName(Props.get("BLOB_CONTAINER_NAME"))
                .buildClient();
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (path == null )
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

        if(path.endsWith("/")){
            deleteAll(path);
            return ok();
        }

        BlobClient blob = containerClient.getBlobClient(path);

        if (blob.exists()) {
            System.out.println("DELETE>>>>" + path);
            blob.delete();
        }
        return ok();
    }

    public void deleteAll(String path) {
        try {
            for (BlobItem blobItem : containerClient.listBlobsByHierarchy(path)) {
                String blobName = blobItem.getName();  // Get the name of each blob
                BlobClient blobClient = containerClient.getBlobClient(blobName);
                if (blobClient.exists()) {
                    blobClient.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
