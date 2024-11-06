package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import tukano.impl.Token;

/**
 * Represents a Short video uploaded by an user.
 * 
 * A short has an unique shortId and is owned by a given user;
 * Comprises of a short video, stored as a binary blob at some bloburl;.
 * A post also has a number of likes, which can increase or decrease over time.
 * It is the only piece of information that is mutable.
 * A short is timestamped when it is created.
 *
 */
@Entity
public class Short {

	@Id
	private String shortId;
	private String ownerId;
	private String blobUrl;
	private long timestamp;
	private int totalLikes;
	private String _rid;
	private String _ts;
	private String id;

	public Short() {
	}

	public Short(String shortId, String ownerId, String blobUrl, long timestamp, int totalLikes) {
		super();
		this.id = shortId;
		this.shortId = shortId;
		this.ownerId = ownerId;
		this.blobUrl = blobUrl;
		this.timestamp = timestamp;
		this.totalLikes = totalLikes;
	}

	public Short(String shortId, String ownerId, String blobUrl) {
		this(shortId, ownerId, blobUrl, System.currentTimeMillis(), 0);
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public void set_rid(String _rid) {
		this._rid = _rid;
	}

	public void set_ts(String _ts) {
		this._ts = _ts;
	}


	public String getShortId() {
		return shortId;
	}

	public String get_rid() {
		return _rid;
	}

	public String get_ts() {
		return _ts;
	}

	public void setShortId(String shortId) {
		this.shortId = shortId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getBlobUrl() {
		return blobUrl;
	}

	public void setBlobUrl(String blobUrl) {
		this.blobUrl = blobUrl;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getTotalLikes() {
		return totalLikes;
	}

	public void setTotalLikes(int totalLikes) {
		this.totalLikes = totalLikes;
	}

	@Override
	public String toString() {
		return "Short [shortId=" + shortId + ", ownerId=" + ownerId + ", blobUrl=" + blobUrl + ", timestamp="
				+ timestamp + ", totalLikes=" + totalLikes + "]";
	}

	public Short copyWithLikes_And_Token(long totLikes) {
		// Remove any existing token from blobUrl
		String cleanBlobUrl = blobUrl.split("\\?token=")[0];

		// If the original URL had a token, replace it; otherwise, append a new token
		String urlWithToken;
		if (blobUrl.contains("?token=")) {
			urlWithToken = String.format("%s?token=%s", cleanBlobUrl, Token.get(cleanBlobUrl));
		} else {
			urlWithToken = String.format("%s?token=%s", blobUrl, Token.get(cleanBlobUrl));
		}

		return new Short(shortId, ownerId, urlWithToken, timestamp, (int) totLikes);
	}
}