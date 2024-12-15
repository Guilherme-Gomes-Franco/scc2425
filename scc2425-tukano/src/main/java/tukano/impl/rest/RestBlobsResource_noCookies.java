/*package tukano.impl.rest;

import jakarta.inject.Singleton;
import tukano.api.Blobs;
import tukano.api.rest.RestBlobs_noCookies;
import tukano.impl.JavaBlobs;

@Singleton
public class RestBlobsResource_noCookies extends RestResource implements RestBlobs_noCookies {

	private static final String ADMIN = "admin";

	final Blobs impl;
	
	public RestBlobsResource_noCookies() {
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
*/