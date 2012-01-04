package com.studentpal.engine.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.app.db.DBaseManager;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.user.AdminUser;
import com.studentpal.model.user.ClientUser;
import com.studentpal.util.logger.Logger;


public class SyncAppTypeListRequest extends Request {
  /*
   * Member fields
   */
  //private int    appListVer;
  private boolean isAdminReq = true;
  private AdminUser managerDev;

  /*
   * Methods
   */
//  public SyncAppTypeListRequest(String targetPhoneNum, int appListVer) {
//    this.targetPhoneNo = targetPhoneNum;
//    this.appListVer = appListVer;
//
//    this.isAdminReq = true;
//  }

  public SyncAppTypeListRequest(AdminUser managerDev) {
    this.managerDev = managerDev;
  }

  @Override
  public String getName() {
    return Event.TASKNAME_SyncAppTypeList;
  }

  public void execute() {
    if (isAdminReq) {
      executeAdminRequest();
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  private void executeAdminRequest() {
    try {
      super.setRequestSeq(ClientEngine.getNextMsgId());

      JSONObject argsObj = new JSONObject();
      targetPhoneNo = managerDev.getPhoneNum();
      argsObj.put(Event.TAGNAME_PHONE_NUM, targetPhoneNo);

      int version = managerDev.getInstalledAppTypesVer();
      if (version == DBaseManager.INVALID_VERSION) {
        version = DBaseManager.getInstance().getAppTypesListVersion();
        managerDev.setInstalledAppTypesVer(version);
      }
      argsObj.put(Event.TAGNAME_VERSION, version);

      JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
      setOutputContent(reqObj.toString());

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

}