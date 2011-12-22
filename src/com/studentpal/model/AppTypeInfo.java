package com.studentpal.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.engine.Event;

public class AppTypeInfo {
  //private static final String TAG = "AppTypeInfo";

  private String   _name;
  private int      _id;
  //if or not this apptype is system preset or user-created
  private boolean  isSysPreset;

  public AppTypeInfo(int _id, String name) {
    this._name = name;
    this._id   = _id;
  }

  public AppTypeInfo(JSONObject jsonAppInfoObj) throws JSONException {
    if (jsonAppInfoObj == null) {
      throw new JSONException("Input parameter is NULL!");
    }
    _id = jsonAppInfoObj.getInt(Event.TAGNAME_APP_TYPEID);
    _name = jsonAppInfoObj.getString(Event.TAGNAME_APP_TYPENAME);
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    this._name = name;
  }

  public int getId() {
    return _id;
  }

  public void setId(int id) {
    this._id = id;
  }

  public boolean isSysPreset() {
    return isSysPreset;
  }

  public void setSysPreset(boolean isSysPreset) {
    this.isSysPreset = isSysPreset;
  }

  public JSONObject toJsonObject() throws JSONException {
    JSONObject result = new JSONObject();
    result.put(Event.TAGNAME_APP_TYPEID, _id);
    result.put(Event.TAGNAME_APP_TYPENAME, _name);
    result.put(Event.TAGNAME_SYS_PRESET, isSysPreset ? 1 : 0);

    return result;
  }

}
