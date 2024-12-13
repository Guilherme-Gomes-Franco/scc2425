package tukano.impl.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Cookie;
import tukano.api.Blobs;
import tukano.api.rest.RestBlobs;
import tukano.impl.JavaBlobs;

@Singleton
public class RestBlobsResource extends RestResource implements RestBlobs {

	private static final String ADMIN = "admin";

	final Blobs impl;
	
	public RestBlobsResource() {
		this.impl = JavaBlobs.getInstance();
	}
	
	@Override
	public void upload(String blobId, byte[] bytes, String token) {
		//Authentication.validateSession(cookie);

		super.resultOrThrow( impl.upload(blobId, bytes, token));
	}

	@Override
	public byte[] download(String blobId, String token) {
		//Authentication.validateSession(cookie);

		return super.resultOrThrow( impl.download(blobId, token ));
	}

	@Override
	public void delete(String blobId, String token) {
		//Authentication.validateSession(cookie, ADMIN);
		super.resultOrThrow( impl.delete( blobId, token ));
	}
	
	@Override
	public void deleteAllBlobs(String userId, String password) {
		//Authentication.validateSession(cookie, ADMIN);

		super.resultOrThrow( impl.deleteAllBlobs( userId, password ));
	}
}
