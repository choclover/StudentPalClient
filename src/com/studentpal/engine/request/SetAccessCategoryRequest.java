package com.studentpal.engine.request;

import static com.studentpal.engine.Event.ERRCODE_MSG_FORMAT_ERR;
import static com.studentpal.engine.Event.ERRCODE_NOERROR;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATEGORIES;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_ID;
import static com.studentpal.engine.Event.TAGNAME_APPLICATIONS;
import static com.studentpal.engine.Event.TAGNAME_APP_CLASSNAME;
import static com.studentpal.engine.Event.TAGNAME_APP_NAME;
import static com.studentpal.engine.Event.TAGNAME_APP_PKGNAME;
import static com.studentpal.engine.Event.TAGNAME_ERR_CODE;
import static com.studentpal.engine.Event.TAGNAME_ERR_DESC;
import static com.studentpal.engine.Event.TAGNAME_PHONE_NUM;
import static com.studentpal.engine.Event.TAGNAME_RESULT;
import static com.studentpal.engine.Event.TASKNAME_SetAccessCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.app.db.DBaseManager;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.AccessCategory;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.util.logger.Logger;


public class SetAccessCategoryRequest extends Request {
  private static final String TAG = "@@ SetAccessCategoryRequest";

  /*
   * Methods
   */
  @Override
  public String getName() {
    return TASKNAME_SetAccessCategory;
  }

  @Override
  public void execute() throws STDException {
    if (isAdminReq) {
      executeAdminRequest();
    } else {
      executeClientRequest();
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  private void executeClientRequest() {
    try {
      JSONObject respObj = super.generateGenericReplyHeader(getName());
      JSONObject resultObj = new JSONObject();

      try {
        if (inputArguments==null || ! (inputArguments instanceof JSONObject)) {
          respObj.put(TAGNAME_ERR_CODE, ERRCODE_MSG_FORMAT_ERR);

        } else {
          Map<Integer, AccessCategory> catesMap =
              new HashMap<Integer, AccessCategory>();

          JSONObject argsParam = (JSONObject)inputArguments;  //new JSONObject(inputJsonArgs);

          //从输入参数中取出CATEGORY信息
          JSONArray catesAry = argsParam.getJSONArray(TAGNAME_ACCESS_CATEGORIES);
          retrieveAccessCategories(catesAry, catesMap);

          JSONArray appsAry = argsParam.getJSONArray(TAGNAME_APPLICATIONS);
          retrieveAppAccessCategory(appsAry, catesMap);

          List<AccessCategory> catesList = null;  //catesMap.values();
          catesList = new ArrayList<AccessCategory>();
          for (Integer key : catesMap.keySet()) {
            catesList.add(catesMap.get(key));
          }

          //save to DB
          ClientEngine.getInstance().getDBaseManager().saveAccessCategoriesToDB(
              catesList);

          //update the access controller
          ClientEngine.getInstance().getAccessController().setAccessCategories(
              catesList);

          respObj.put(TAGNAME_ERR_CODE, ERRCODE_NOERROR);
        }

      } catch (STDException ex) {
        Logger.w(getName(), "In execute() got an error:" + ex.toString());
        respObj.put(TAGNAME_ERR_CODE, ERRCODE_MSG_FORMAT_ERR);
        resultObj.put(TAGNAME_ERR_DESC, ex.getMessage());

      } finally {
        if (resultObj.length() > 0) {
          respObj.put(TAGNAME_RESULT, resultObj);
        }
        if (respObj != null) {
          setOutputContent(respObj.toString());
        }
      }

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

  private void executeAdminRequest() throws STDException {
    try {
      if (inputArguments==null || ! (inputArguments instanceof Set<?>)) {
        throw new STDException("Input argument format error for "+getName());

      } else {
        int version = DBaseManager.getInstance().getAccessCatesListVersion(
            this.targetPhoneNo);

        JSONArray jsonCatesAry = new JSONArray();
        for (AccessCategory anCate : (Set<AccessCategory>)inputArguments) {
          jsonCatesAry.put(anCate.toJsonObject());
        }

        super.setRequestSeq(ClientEngine.getNextMsgId());

        JSONObject argsObj = new JSONObject();
        argsObj.put(TAGNAME_PHONE_NUM, targetPhoneNo);
        argsObj.put(Event.TAGNAME_VERSION, version);
        argsObj.put(TAGNAME_ACCESS_CATEGORIES, jsonCatesAry);

        JSONObject reqObj = super.generateGenericRequestHeader(getName(), argsObj);
        setOutputContent(reqObj.toString());
      }

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private void retrieveAppAccessCategory(
      JSONArray appsAry, Map<Integer, AccessCategory> sourceMap) throws STDException {
    if (appsAry == null) {
      throw new STDException(TAGNAME_APPLICATIONS+" is NULL in input arguments");
    }

    try {
      for (int i = 0; i < appsAry.length(); i++) {
        JSONObject appObj = appsAry.getJSONObject(i);

        String appName, pkgName, className = null;
        appName = appObj.getString(TAGNAME_APP_NAME);
        pkgName = appObj.getString(TAGNAME_APP_PKGNAME);
        if (appObj.has(TAGNAME_APP_CLASSNAME)) {
          className = appObj.getString(TAGNAME_APP_CLASSNAME);
        }
        ClientAppInfo appInfo = new ClientAppInfo(appName, pkgName, className);

        int cateId = appObj.getInt(TAGNAME_ACCESS_CATE_ID);
        AccessCategory aCate = sourceMap.get(cateId);
        if (aCate != null) {
          aCate.addManagedApp(appInfo);
        }
      }// for apps

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
      throw new STDException(ex.toString());
    }
  }

  private void retrieveAccessCategories(
      JSONArray catesAry, Map<Integer, AccessCategory> intoMap) throws STDException {
    if (catesAry == null) {
      throw new STDException(TAGNAME_ACCESS_CATEGORIES+" is NULL in input arguments");
    }

    try {
      for (int i=0; i<catesAry.length(); i++) {
        JSONObject cateObj = catesAry.getJSONObject(i);

        AccessCategory aCate = new AccessCategory();
        aCate.populateFromJsObject(cateObj);
        intoMap.put(aCate.get_id(), aCate);
      }//for cates

    } catch (JSONException ex) {
      Logger.w(getName(), "In execute() got an error:" + ex.toString());
      throw new STDException(ex.toString());
    }
  }

}