package com.studentpal.model;

import java.util.Set;

import com.studentpal.model.user.ClientUser;

public class DataManager {
  /*
   * Field members
   */
  private static DataManager instance = null;

  private Set<ClientUser>     managedDevs  = null;
  private Set<AppTypeInfo>    appTypesList = null;
  private Set<ClientAppInfo>  appsList     = null;
  private Set<AccessCategory> accessCates  = null;

  /*
   * Methods
   */
  private DataManager() {
  }

  public static DataManager getInstance() {
    if (instance == null) {
      instance  = new DataManager();
    }
    return instance;
  }

  public Set<ClientUser> getManagedDevs() {
    return managedDevs;
  }

  public void setManagedDevs(Set<ClientUser> managedDevs) {
    this.managedDevs = managedDevs;
  }

  public Set<AppTypeInfo> getAppTypesList() {
    return appTypesList;
  }

  public void setAppTypesList(Set<AppTypeInfo> appTypesList) {
    this.appTypesList = appTypesList;
  }

  public Set<ClientAppInfo> getAppsList() {
    return appsList;
  }

  public void setAppsList(Set<ClientAppInfo> appsList) {
    this.appsList = appsList;
  }

  public Set<AccessCategory> getAccessCategories() {
    return accessCates;
  }

  public void setAccessCategories(Set<AccessCategory> accessCates) {
    this.accessCates = accessCates;
  }

}
