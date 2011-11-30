package com.studentpal.model.user;

public class AdminUser extends User {
  public String loginName;
  public String loginPwd;

  public AdminUser(String loginName, String loginPwd) {
    this.loginName = loginName;
    this.loginPwd = loginPwd;
  }
}
