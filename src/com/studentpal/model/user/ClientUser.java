package com.studentpal.model.user;

import com.studentpal.model.user.User;

public class ClientUser extends User {
  private String phoneNum;
  private String phoneImsi;
  private String phoneImei;

  public ClientUser(String phoneNum, String phoneImsi) {
    this(phoneNum, phoneImsi, null);
  }

  public ClientUser(String phoneNum, String phoneImsi, String phoneImei) {
    this.phoneNum  = phoneNum;
    this.phoneImsi = phoneImsi;
    this.phoneImei = phoneImei;
  }

  public String getPhoneNum() {
    return phoneNum;
  }

  public String getPhoneImsi() {
    return phoneImsi;
  }

  public String getPhoneImei() {
    return phoneImei;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClientUser)) {
      return false;
    }
    ClientUser user = (ClientUser) o;

    return isEqualValue(phoneNum, user.getPhoneNum()) ||
            isEqualValue(phoneImsi, user.getPhoneImsi());
  }
}
