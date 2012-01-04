package com.studentpal.engine.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.app.db.DBaseManager;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.user.ClientUser;
import com.studentpal.util.logger.Logger;


public class SyncAppListRequest extends Request {
  /*
   * Member fields
   */
  //private int     appListVer;
  private boolean isAdminReq = true;
  private ClientUser managedDev;

  /*
   * Methods
   */
//  public SyncAppListRequest(String targetPhoneNum, int appListVer) {
//    this.targetPhoneNo = targetPhoneNum;
//    this.appListVer = appListVer;
//  }

  public SyncAppListRequest(ClientUser managedDev) {
    this.managedDev = managedDev;
  }

  @Override
  public String getName() {
    return Event.TASKNAME_SyncAppList;
  }

  public void execute() {
    if (isAdminReq) {
      executeAdminRequest();
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  private void executeAdminRequest() {
    try {
      if (managedDev == null) return;

      super.setRequestSeq(ClientEngine.getNextMsgId());

      JSONObject argsObj = new JSONObject();
      targetPhoneNo = managedDev.getPhoneNum();
      argsObj.put(Event.TAGNAME_PHONE_NUM, targetPhoneNo);

      int version = managedDev.getInstalledAppsListVer();
      if (version == DBaseManager.INVALID_VERSION) {
        version = DBaseManager.getInstance().getAppsListVersion(
            targetPhoneNo);
        managedDev.setInstalledAppsListVer(version);
      }
      argsObj.put(Event.TAGNAME_VERSION, version);

      JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
      setOutputContent(reqObj.toString());

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

}