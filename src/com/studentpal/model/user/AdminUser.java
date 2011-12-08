package com.studentpal.model.user;

import com.studentpal.model.user.User;
import com.studentpal.util.Utils;

public class AdminUser extends User {
  private String loginName;
  private String loginPwd;

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