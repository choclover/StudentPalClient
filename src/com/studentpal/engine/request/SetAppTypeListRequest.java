package com.studentpal.engine.request;

import static com.studentpal.engine.Event.TASKNAME_SetAppTypeList;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.app.db.DBaseManager;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.AppTypeInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.util.logger.Logger;


public class SetAppTypeListRequest extends Request {
  private static final String TAG = "@@ SetAppTypeListRequest";

  @Override
  public String getName() {
    return TASKNAME_SetAppTypeList;
  }

  @Override
  public void execute() throws STDException {
    if (isAdminReq) {
      executeAdminRequest();
    } else {
      Logger.w("Should NOT execute from client side!");
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  private void executeAdminRequest() throws STDException {
    try {
      if (inputArguments==null || ! (inputArguments instanceof Set<?>)) {
        throw new STDException("Input argument format error for "+getName());

      } else {
        int version = DBaseManager.getInstance().getAppTypesListVersion();

        JSONArray jsonTypesAry = new JSONArray();
        for (AppTypeInfo appType : (Set<AppTypeInfo>)inputArguments) {
          if (! appType.isSysPreset()) {
            jsonTypesAry.put(appType.toJsonObject());
          }
        }

        super.setRequestSeq(ClientEngine.getNextMsgId());

        JSONObject argsObj = new JSONObject();
        argsObj.put(Event.TAGNAME_PHONE_NUM, targetPhoneNo);
        argsObj.put(Event.TAGNAME_VERSION, version);
        argsObj.put(Event.TAGNAME_APPLICATION_TYPES, jsonTypesAry);

        JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
        setOutputContent(reqObj.toString());
      }

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }


}