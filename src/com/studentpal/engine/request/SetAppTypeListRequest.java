package com.studentpal.engine.request;

import static com.studentpal.engine.Event.ERRCODE_MSG_FORMAT_ERR;
import static com.studentpal.engine.Event.ERRCODE_NOERROR;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATEGORIES;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_ID;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_NAME;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_RULES;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_TIMERANGES;
import static com.studentpal.engine.Event.TAGNAME_APPLICATIONS;
import static com.studentpal.engine.Event.TAGNAME_APP_CLASSNAME;
import static com.studentpal.engine.Event.TAGNAME_APP_NAME;
import static com.studentpal.engine.Event.TAGNAME_APP_PKGNAME;
import static com.studentpal.engine.Event.TAGNAME_ERR_CODE;
import static com.studentpal.engine.Event.TAGNAME_ERR_DESC;
import static com.studentpal.engine.Event.TAGNAME_RESULT;
import static com.studentpal.engine.Event.TAGNAME_RULE_AUTH_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_ENDTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_STARTTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_VALUE;
import static com.studentpal.engine.Event.TASKNAME_SetAppTypeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.AccessCategory;
import com.studentpal.model.AppTypeInfo;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.model.rules.AccessRule;
import com.studentpal.model.rules.Recurrence;
import com.studentpal.model.rules.TimeRange;
import com.studentpal.util.logger.Logger;


public class SetAppTypeListRequest extends Request {
  private static final String TAG = "@@ SetAppTypeListRequest";

  @Override
  public String getName() {
    return TASKNAME_SetAppTypeList;
  }

  @Override
  public void execute() {
    if (isAdminReq) {
      executeAdminRequest();
    } else {
      Logger.w("Should NOT execute from client side!");
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  private void executeAdminRequest() {
    try {
      if (inputArguments==null || ! (inputArguments instanceof Set<?>)) {
        Logger.e(TAG, "Input argument format error");

      } else {
        JSONArray jsonCatesAry = new JSONArray();
        for (AppTypeInfo appType : (Set<AppTypeInfo>)inputArguments) {
          jsonCatesAry.put(appType.toJsonObject());
        }

        super.setRequestSeq(ClientEngine.getNextMsgId());

        JSONObject argsObj = new JSONObject();
        argsObj.put(Event.TAGNAME_PHONE_NUM, this.targetPhoneNo);


        JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
        setOutputContent(reqObj.toString());
      }

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }


}