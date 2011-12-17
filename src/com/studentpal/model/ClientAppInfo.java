package com.studentpal.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.util.logger.Logger;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class ClientAppInfo {
  private static final String TAG = "ClientAppInfo";

  private String app_name;
  private String app_classname;
  private String app_pkgname;
  private int    app_typeid = 0;
  //private String[] app_pkgList;

  private static PackageManager pm = ClientEngine.getInstance().getPackageManager();

  public ClientAppInfo(ApplicationInfo appInfo) {
    if (appInfo != null) {
      if (pm == null) {
        Logger.w(TAG, "Got NULL PackageManager from engine!");
        return;
      }

      app_name = pm.getApplicationLabel(appInfo).toString();
      app_classname = appInfo.className;
      app_pkgname  = appInfo.packageName;
    }
  }

  public ClientAppInfo(String name, String pkgName, String main_classname) {
    app_name = name;
    app_classname = main_classname;
    app_pkgname = pkgName;
  }

  public ClientAppInfo(JSONObject jsonAppInfoObj) throws JSONException {
    if (jsonAppInfoObj == null) {
      throw new JSONException("Input parameter is NULL!");
    }

    app_name = jsonAppInfoObj.getString(Event.TAGNAME_APP_NAME);
    app_pkgname = jsonAppInfoObj.getString(Event.TAGNAME_APP_PKGNAME);
    if (jsonAppInfoObj.has(Event.TAGNAME_APP_CLASSNAME)) {
      app_classname = jsonAppInfoObj.getString(Event.TAGNAME_APP_CLASSNAME);
    }
    if (jsonAppInfoObj.has(Event.TAGNAME_APP_TYPEID)) {
      app_typeid = jsonAppInfoObj.getInt(Event.TAGNAME_APP_TYPEID);
    }
  }

  public String getAppName() {
    return app_name;
  }

  public String getAppClassname() {
    return app_classname;
  }

  public String getAppPkgname() {
    return app_pkgname;
  }

  public int getAppTypeId() {
    return app_typeid;
  }

  public String getIndexingKey() {
    return getAppPkgname();
  }

  public String getIndexingValue() {
    return getAppClassname();
  }
}
