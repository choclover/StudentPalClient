package com.studentpal.engine.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.util.logger.Logger;


public class SyncAccessCategoryRequest extends Request {
  /*
   * Member fields
   */
  private int    cateListVer;

  /*
   * Methods
   */
  public SyncAccessCategoryRequest(String targetPhoneNum, int cateListVer) {
    this.targetPhoneNo = targetPhoneNum;
    this.cateListVer = cateListVer;

    this.isAdminReq = true;
  }

  @Override
  public String getName() {
    return Event.TASKNAME_SyncAccessCategory;
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
      argsObj.put(Event.TAGNAME_PHONE_NUM, targetPhoneNo);
      argsObj.put(Event.TAGNAME_VERSION, cateListVer);

      JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
      setOutputContent(reqObj.toString());

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

}