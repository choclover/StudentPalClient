package com.studentpal.model.user;

import static com.studentpal.app.db.DBaseManager.INVALID_VERSION;

import com.studentpal.model.user.User;
import com.studentpal.util.Utils;

public class AdminUser extends User {
  private String loginName;
  private String loginPwd;
  private String nickName;

  private int installedAppTypesVer   = INVALID_VERSION;

  public AdminUser(String loginName, String loginPwd) {
    this.loginName = loginName;
    this.loginPwd = loginPwd;
  }

  public String getLoginName() {
    return loginName;
  }

  public String getLoginPwd() {
    return loginPwd;
  }

  public String getEncryptedPwd() {
    return loginPwd==null ? null : Utils.toMd5(loginPwd.getBytes());
  }

  public int getInstalledAppTypesVer() {
    return installedAppTypesVer;
  }

  public void setInstalledAppTypesVer(int installedAppTypesVer) {
    this.installedAppTypesVer = installedAppTypesVer;
  }

  public String getNickName() {
    return nickName;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  /*
   *
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AdminUser)) {
      return false;
    }
    AdminUser user = (AdminUser) o;

    return isEqualValue(loginName, user.loginName);
  }
}