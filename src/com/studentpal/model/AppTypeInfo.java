package com.studentpal.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.ApplicationInfo;

import com.studentpal.engine.Event;
import com.studentpal.util.logger.Logger;

public class AppTypeInfo {
  //private static final String TAG = "AppTypeInfo";

  private String type_name;
  private String type_id;

  public AppTypeInfo(String _id, String typeName) {
    type_name = typeName;
    type_id   = _id;
  }

  public AppTypeInfo(JSONObject jsonAppInfoObj) throws JSONException {
    if (jsonAppInfoObj == null) {
      throw new JSONException("Input parameter is NULL!");
    }
    type_id = jsonAppInfoObj.getString(Event.TAGNAME_APP_TYPEID);
    type_name = jsonAppInfoObj.getString(Event.TAGNAME_APP_TYPENAME);
  }

  public String getType_name() {
    return type_name;
  }

  public void setType_name(String type_name) {
    this.type_name = type_name;
  }

  public String getType_id() {
    return type_id;
  }

  public void setType_id(String type_id) {
    this.type_id = type_id;
  }

}
