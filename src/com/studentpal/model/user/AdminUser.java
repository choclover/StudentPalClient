package com.studentpal.model.user;

import com.studentpal.model.user.User;

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