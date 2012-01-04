package com.studentpal.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.studentpal.model.user.ClientUser;

public class DataManager {
  /*
   * Field members
   */
  private static DataManager instance = null;

  private Set<ClientUser>            managedDevs            = null;
  private Map<Integer, AppTypeInfo>  appTypesMap            = null;
  //所有的data model只存储当前UI所需的数据，更多数据保存在DB中需要时再读取出，以此减少内存的消耗
  private Map<String, Set<ClientAppInfo>>  installedAppsMap = null;
  private Map<String, Set<AccessCategory>> accessCatesMap   = null;

  /*
   * Methods
   */
  private DataManager() {
    appTypesMap      = new HashMap<Integer, AppTypeInfo>();
    //TODO  change to list
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
    Set<AppTypeInfo> result = null;
    Collection<AppTypeInfo> c = appTypesMap.values();
    if (c!=null && c.size()>0) {
      result = new HashSet<AppTypeInfo>();
      for (AppTypeInfo appType : c) {
        result.add(appType);
      }
    }
    return result;
  }

  public String getAppTypeName(int appTypeId) {
    AppTypeInfo appType = appTypesMap.get(appTypeId);
    if (appType != null) {
      return appType.getName();
    } else {
      return "";
    }
  }

  public void setAppTypesList(Set<AppTypeInfo> appTypesList) {
    if (appTypesMap != null) {
      appTypesMap.clear();
    } else {
      appTypesMap = new HashMap<Integer, AppTypeInfo>();
    }

    if (appTypesList!=null && appTypesList.size()>0) {
      for (AppTypeInfo appType : appTypesList) {
        if (appType != null) {
          appTypesMap.put(appType.getId(), appType);
        }
      }
    }
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
