package com.studentpal.engine.request;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.util.logger.Logger;


public class RefreshAppListRequest extends Request {
  private String targetPhoneNo;

  /*
   * Methods
   */
  public RefreshAppListRequest(String targetPhoneNum) {
    this.targetPhoneNo = targetPhoneNum;
  }

  /*
   * Don't delet this constructor, called by Phone client
   * MessageHandler.handleRequestMessage()
   */
  public RefreshAppListRequest() {
  }

  @Override
  public String getName() {
    return Event.TASKNAME_RefreshAppList;
  }

  public void execute() {
    if (isAdminReq) {
      executeAdminRequest();
    } else {
      executeClientRequest();
    }
  }

  public void setTargetPhoneNo(String targetPhoneNo) {
    this.targetPhoneNo = targetPhoneNo;
  }

  /////////////////////////////////////////////////////////////////////////////
  private void executeClientRequest() {
    try {
      JSONObject respObj = super.generateGenericReplyHeader(getName());

      try {
        List<ClientAppInfo> appList = ClientEngine.getInstance().getAppList();

        JSONArray appAry = new JSONArray();
        if (appList != null && appList.size() > 0) {
          for (ClientAppInfo appInfo : appList) {
            JSONObject app = new JSONObject();
            app.put(Event.TAGNAME_APP_NAME, appInfo.getAppName());
            app.put(Event.TAGNAME_APP_CLASSNAME, appInfo.getAppClassname());
            app.put(Event.TAGNAME_APP_PKGNAME, appInfo.getAppPkgname());
            app.put(Event.TAGNAME_ACCESS_CATEGORY, 1);  //FIXME

            appAry.put(app);
          }
        }

        JSONObject resultObj = new JSONObject();
        resultObj.put(Event.TAGNAME_PHONE_NUM, targetPhoneNo);
        resultObj.put(Event.TAGNAME_APPLICATIONS, appAry);

        respObj.put(Event.TAGNAME_RESULT, resultObj);

        respObj.put(Event.TAGNAME_ERR_CODE, Event.ERRCODE_NOERROR);

      } catch (Exception ex) {
        Logger.w(getName(), "In execute() got an error:" + ex.toString());
        respObj.put(Event.TAGNAME_ERR_CODE, Event.ERRCODE_SERVER_INTERNAL_ERR);

      } finally {
        if (respObj != null) {
          setOutputContent(respObj.toString());
        }
      }
    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

  private void executeAdminRequest() {
    try {
      super.setRequestSeq(ClientEngine.getNextMsgId());

      JSONObject argsObj = new JSONObject();
      argsObj.put(Event.TAGNAME_PHONE_NUM, targetPhoneNo);

      JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
      setOutputContent(reqObj.toString());

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

}