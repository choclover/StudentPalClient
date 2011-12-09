package com.studentpal.engine.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.user.AdminUser;
import com.studentpal.model.user.ClientUser;
import com.studentpal.model.user.User;
import com.studentpal.util.logger.Logger;

public class LoginRequest extends Request {
  private Object _user;
  private String _name = Event.TASKNAME_LOGIN;

  public LoginRequest(String reqName, User user) {
    this._name  = reqName;
    this._user  = user;
  }

  public String getName() {
    return _name;
  }

  public void execute() {
    try {
      super.setRequestSeq(ClientEngine.getNextMsgId());

      JSONObject argsObj = new JSONObject();
      if (_user instanceof ClientUser) {
        argsObj.put(Event.TAGNAME_PHONE_NUM, ((ClientUser)_user).getPhoneNum());
        argsObj.put(Event.TAGNAME_PHONE_IMSI, ((ClientUser)_user).getPhoneImsi());
      } else if (_user instanceof AdminUser) {
        argsObj.put(Event.TAGNAME_LOGIN_NAME, ((AdminUser) _user).getLoginName());

        String passwd = ((AdminUser) _user).getLoginPwd();
        //passwd = ((AdminUser) _user).getEncryptedPwd();  //FIXME
        argsObj.put(Event.TAGNAME_LOGIN_PASSWD, passwd);
      }

      JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
      setOutputContent(reqObj.toString());

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

  public boolean isOutputContentReady() {
    return true;
  }

}