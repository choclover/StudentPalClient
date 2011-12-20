package com.studentpal.model.user;

import static com.studentpal.app.db.DBaseManager.INVALID_VERSION;
import com.studentpal.model.user.User;

public class ClientUser extends User {

  private String installedApps;

  private int installedAppsListVer   = INVALID_VERSION;
  private int installedAccessCateVer = INVALID_VERSION;

  /*
   * Methods
   */
  public ClientUser(String phoneNum, String phoneImsi) {
    this(phoneNum, phoneImsi, null);
  }

  public ClientUser(String phoneNum, String phoneImsi, String phoneImei) {
    this.phoneNum  = phoneNum;
    this.phoneImsi = phoneImsi;
    this.phoneImei = phoneImei;
  }

  public String getInstalledApps() {
    return installedApps;
  }

  public void setInstalledApps(String installedApps) {
    this.installedApps = installedApps;
  }

  public int getInstalledAppsListVer() {
    return installedAppsListVer;
  }

  public void setInstalledAppsListVer(int installedAppsListVer) {
    this.installedAppsListVer = installedAppsListVer;
  }

  public int getInstalledAccessCateVer() {
    return installedAccessCateVer;
  }

  public void setInstalledAccessCateVer(int installedAccessCateVer) {
    this.installedAccessCateVer = installedAccessCateVer;
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
