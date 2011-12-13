package com.studentpal.model.user;

import static com.studentpal.app.db.DBaseManager.INVALID_APPLIST_VERSION;
import com.studentpal.model.user.User;

public class ClientUser extends User {
  private String phoneNum;
  private String phoneImsi;
  private String phoneImei;
  private String installedApps;

  private int installedAppsListVer = INVALID_APPLIST_VERSION;

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

  public String getPhoneNum() {
    return phoneNum;
  }

  public String getPhoneImsi() {
    return phoneImsi;
  }

  public String getPhoneImei() {
    return phoneImei;
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
