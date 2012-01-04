package com.studentpal.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.studentpal.model.user.ClientUser;

public class DataManager {
  /*
   * Field members
   */
  private static DataManager instance = null;

  private Set<ClientUser>     managedDevs      = null;
  private Set<AppTypeInfo>    appTypesList     = null;
  private Map<String, Set<ClientAppInfo>>  installedAppsMap = null;
  private Map<String, Set<AccessCategory>> accessCatesMap   = null;

  /*
   * Methods
   */
  private DataManager() {
    installedAppsMap = new HashMap<String, Set<ClientAppInfo>>();
    accessCatesMap   = new HashMap<String, Set<AccessCategory>>();
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

  public String[] getManagedPhoneNumAry() {
    String[] result = null;
    if (managedDevs!=null && managedDevs.size()>0) {
      result = new String[managedDevs.size()];
      int idx = 0;
      for (ClientUser managedDev : managedDevs) {
        result[idx++] = managedDev.getPhoneNum();
      }
    }
    return result;
  }

  public Set<AppTypeInfo> getAppTypesList() {
    return appTypesList;
  }

  public void setAppTypesList(Set<AppTypeInfo> appTypesList) {
    this.appTypesList = appTypesList;
  }

  public Set<ClientAppInfo> getAppsList(String phoneNum) {
    return installedAppsMap.get(phoneNum);
  }

  public void setAppsList(String phoneNum, Set<ClientAppInfo> appsList) {
    installedAppsMap.remove(phoneNum);
    if (appsList != null) {
      installedAppsMap.put(phoneNum, appsList);
    }
  }

  public Set<AccessCategory> getAccessCategories(String phoneNum) {
    return accessCatesMap.get(phoneNum);
  }

  public void setAccessCategories(String phoneNum, Set<AccessCategory> accessCates) {
    accessCatesMap.remove(phoneNum);
    if (accessCates != null) {
      accessCatesMap.put(phoneNum, accessCates);
    }
  }

}
