package com.studentpal.model.user;


public class ClientUser extends User {
  public String phoneNum;
  public String phoneImsi;
  public String phoneImei;

  public ClientUser(String phoneNum, String phoneImsi) {
    this(phoneNum, phoneImsi, null);
  }

  public ClientUser(String phoneNum, String phoneImsi, String phoneImei) {
    this.phoneNum = phoneNum;
    this.phoneImsi = phoneImsi;
    this.phoneImei = phoneImei;
  }
}