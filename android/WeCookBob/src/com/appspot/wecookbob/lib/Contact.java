package com.appspot.wecookbob.lib;

public class Contact {
	String phoneNumber;
	String userName;
	String userId;
	boolean hasLog;
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public boolean getHasLog() {
		return hasLog;
	}
	public void setHasLog(boolean hasLog) {
		this.hasLog = hasLog;
	}
}