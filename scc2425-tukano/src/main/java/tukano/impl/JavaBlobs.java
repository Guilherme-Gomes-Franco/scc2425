package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Cookie;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.impl.rest.TukanoRestApplication;
import tukano.impl.storage.BlobStorage;
import tukano.impl.storage.DockerFilesystemStorage;
import utils.DB;
import utils.Hash;
import utils.Hex;
import utils.Props;

public class JavaBlobs implements Blobs {

	private static Blobs instance;
	private static Logger Log = Logger.getLogger(JavaBlobs.class.getName());

	public String baseURI;
	private BlobStorage storage;

	synchronized public static Blobs getInstance() {
		if (instance == null)
			instance = new JavaBlobs();
		return instance;
	}

	private JavaBlobs() {
		String storageRootPath = System.getenv("STORAGE_ROOT"); // Docker volume path from environment variable
		//storage = new DockerFilesystemStorage(storageRootPath);
		storage = new DockerFilesystemStorage(storageRootPath);
		baseURI = String.format("%s/%s/", TukanoRestApplication.serverURI, Blobs.NAME);
	}

	@Override
	public Result<Void> upload(String blobId, byte[] bytes, String token) {
		Log.info(() -> format("upload : blobId = %s, sha256 = %s, token = %s\n", blobId, Hex.of(Hash.sha256(bytes)),
				token));

		if (!validBlobId(blobId, token))
			return error(FORBIDDEN);

		return storage.write(toPath(blobId), bytes);
	}

	@Override
	public Result<byte[]> download(String blobId, String token) {
		Log.info(() -> format("download : blobId = %s, token=%s\n", blobId, token));

		if (!validBlobId(blobId, token))
			return error(FORBIDDEN);

		var res = storage.read(toPath(blobId));
		if (res.isOK())
			updateViews(blobId);

		return res;
	}

	@Override
	public Result<Void> delete(String blobId, String token) {
		Log.info(() -> format("delete : blobId = %s, token=%s\n", blobId, token));

		if (!validBlobId(blobId, token))
			return error(FORBIDDEN);

		return storage.delete(toPath(blobId));
	}

	@Override
	public Result<Void> deleteAllBlobs(String userId, String token) {
		Log.info(() -> format("deleteAllBlobs : userId = %s, token=%s\n", userId, token));

		if (!Token.isValid(token, userId))
			return error(FORBIDDEN);

		return storage.delete(toPath("short_"+userId));
	}

	private boolean validBlobId(String blobId, String token) {
		if (blobId == null || blobId.isEmpty() || token == null || token.isEmpty())
			return false;
		return Token.isValid(token, toURL(blobId));
	}

	private String toPath(String blobId) {
		return blobId.replace("+", "/");
	}

	private String toURL(String blobId) {
		return baseURI + blobId;
	}

	public static void updateViews(String blobId) {
		CompletableFuture.runAsync(() -> {
			try {
				HttpURLConnection conn = getHttpURLConnection(blobId);

				try (OutputStream os = conn.getOutputStream()) {
					byte[] input = "".getBytes(StandardCharsets.UTF_8);
					os.write(input, 0, input.length);
				}

				int responseCode = conn.getResponseCode();
				System.out.println("Response Code: " + responseCode);

				conn.disconnect();

			} catch (Exception e) {
				e.printStackTrace();
			}
		})
				.thenAccept(result -> Log.info(() -> format("Successfully updatedViews : blobId = %s\n", blobId)))
				.exceptionally(ex -> {
					Log.info(() -> format("Failed to execute updateViews : error = %s\n", ex.getMessage()));
					return null;
				});
	}

	private static HttpURLConnection getHttpURLConnection(String blobId) throws IOException {

		String urlString = String.format(
				"http://blob-http-trigger/blob-http-trigger-1/rest/update_views?id=%s&token=%s",
				blobId, Props.get("SECRET_TOKEN"));

		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		return conn;
	}
}
