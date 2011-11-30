package com.studentpal.engine.request;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.user.User;
import com.studentpal.model.user.AdminUser;
import com.studentpal.model.user.ClientUser;
import com.studentpal.util.logger.Logger;


public class LoginRequest extends Request {
  private User _user;
  private String _name = Event.TASKNAME_LOGIN;

  public LoginRequest(String name, User user) {
    this._name  = name;
    this._user  = user;
  }

  public String getName() {
    return _name;
  }

  public void execute() {
    try {
      JSONObject argsObj = new JSONObject();
      if (_user instanceof ClientUser) {
        argsObj.put(Event.TAGNAME_PHONE_NUM, ((ClientUser)_user).phoneNum);
        argsObj.put(Event.TAGNAME_PHONE_IMSI, ((ClientUser)_user).phoneImsi);

      } else if (_user instanceof AdminUser) {
        argsObj.put(Event.TAGNAME_LOGIN_NAME, ((AdminUser) _user).loginName);
        argsObj.put(Event.TAGNAME_LOGIN_PASSWD, ((AdminUser) _user).loginPwd);
      }

      JSONObject reqObj = new JSONObject();
      reqObj.put(Event.TAGNAME_MSG_TYPE, Event.MESSAGE_HEADER_REQ);
      reqObj.put(Event.TAGNAME_CMD_TYPE, getName());
      reqObj.put(Event.TAGNAME_MSG_ID, Event.MSG_ID_NOTUSED);
      reqObj.put(Event.TAGNAME_ARGUMENTS, argsObj);

      setOutputContent(reqObj.toString());

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

  public boolean isOutputContentReady() {
    return true;
  }

}