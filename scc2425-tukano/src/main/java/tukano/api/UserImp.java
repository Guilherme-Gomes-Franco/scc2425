package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UserImp {

	@Id
	private String userId;
	private String pwd;
	private String email;
	private String displayName;
	private String id;

	public UserImp() {
	}

	public UserImp(String userId, String pwd, String email, String displayName) {
		this.pwd = pwd;
		this.email = email;
		this.userId = userId;
		this.displayName = displayName;
		this.id = userId;
	}

	private String _rid;

	public String get_rid() {
		return _rid;
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

	public String get_ts() {
		return _ts;
	}

	public void set_ts(String _ts) {
		this._ts = _ts;
	}

	private String _ts;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String userId() {
		return userId;
	}

	public String pwd() {
		return pwd;
	}

	public String email() {
		return email;
	}

	public String displayName() {
		return displayName;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", pwd=" + pwd + ", email=" + email + ", displayName=" + displayName + "]";
	}

	public UserImp copyWithoutPassword() {
		return new UserImp(userId, "", email, displayName);
	}

	public UserImp updateFrom(UserImp other) {
		return new UserImp(userId,
				other.pwd != null ? other.pwd : pwd,
				other.email != null ? other.email : email,
				other.displayName != null ? other.displayName : displayName);
	}
}
