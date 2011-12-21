package com.studentpal.model;

import static com.studentpal.engine.Event.ACCESS_TYPE_DENIED;
import static com.studentpal.engine.Event.ACCESS_TYPE_PERMITTED;
import static com.studentpal.engine.Event.RECUR_TYPE_DAILY;
import static com.studentpal.engine.Event.RECUR_TYPE_MONTHLY;
import static com.studentpal.engine.Event.RECUR_TYPE_WEEKLY;
import static com.studentpal.engine.Event.RECUR_TYPE_YEARLY;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_ID;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_NAME;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_RULE;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_RULES;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_TIMERANGE;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_TIMERANGES;
import static com.studentpal.engine.Event.TAGNAME_APPLICATION_TYPES;
import static com.studentpal.engine.Event.TAGNAME_RULE_AUTH_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_ENDTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_STARTTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_VALUE;
import static com.studentpal.engine.Event.TXT_ACCESS_TYPE_DENIED;
import static com.studentpal.engine.Event.TXT_ACCESS_TYPE_PERMITTED;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_DAILY;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_MONTHLY;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_WEEKLY;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_YEARLY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.studentpal.app.handler.RuleScheduler;
import com.studentpal.model.exception.STDException;
import com.studentpal.model.rules.AccessRule;
import com.studentpal.model.rules.Recurrence;
import com.studentpal.model.rules.TimeRange;
import com.studentpal.util.logger.Logger;

public class AccessCategory {
  private static final String TAG = "@@ AccessCategory";

  /*
   * Member fields
   */
  private String _name;
  private int _id;
  private RuleScheduler scheduler;

  /*
   * ClientAppInfo -- An AppInfo instance managed by this category Integer --
   * Count of Restricted Rules upon this ClientAppInfo
   */
  private HashMap<ClientAppInfo, Integer> _managedAppsMap;
  private List<AccessRule> _rulesList;

  public AccessCategory() {
    _managedAppsMap = new HashMap<ClientAppInfo, Integer>();
    _rulesList = new ArrayList<AccessRule>();
  }

  public AccessCategory(JSONObject jsonCateObj) throws JSONException {
    if (jsonCateObj == null) {
      throw new JSONException("Input parameter is NULL!");
    }

    try {
      populateFromJsObject(jsonCateObj);
    } catch (STDException e) {
      Logger.w(TAG, e.toString());
    }
  }

  public void addManagedApp(ClientAppInfo appInfo) {
    if (appInfo != null) {
      synchronized (_managedAppsMap) {
        _managedAppsMap.put(appInfo, 0); // init the restrictedRuleCnt to 0
      }
    }
  }

  public void removeManagedApp(ClientAppInfo appInfo) {
    if (appInfo != null) {
      synchronized (_managedAppsMap) {
        if (null == _managedAppsMap.remove(appInfo)) {
          Logger.w(TAG, "_managedAppsMap return NULL for AppInfo "
              + appInfo.getAppClassname());
        }
      }
    } else {
      Logger.w(TAG, "Input parameter ClientAppInfo is NULL!");
    }
  }

  public void addAccessRule(AccessRule rule) {
    if (rule != null) {
      rule.setAdhereCategory(this);
      _rulesList.add(rule);
    }
  }

  public List<AccessRule> getAccessRules() {
    return _rulesList;
  }

  public void clearRules() {
    if (_rulesList != null)
      _rulesList.clear();
  }

  public HashMap<ClientAppInfo, Integer> getManagedApps() {
    return _managedAppsMap;
  }

  public RuleScheduler getScheduler() {
    if (scheduler == null) {
      scheduler = new RuleScheduler();
    }
    return scheduler;
  }

  public void adjustRestrictedRuleCount(ClientAppInfo appInfo, int delta) {
    if (appInfo == null || delta == 0)
      return;

    if (_managedAppsMap.containsKey(appInfo) == false) {
      Logger.w(TAG, "_managedAppsMap NOT contains AppInfo: " + appInfo.getAppName());
    }

    synchronized (_managedAppsMap) {
      Integer oldCnt = _managedAppsMap.get(appInfo);
      Integer newCnt = oldCnt + delta;
      if (newCnt < 0) {
        newCnt = 0;
      }
      Logger.v(TAG, "Putting counter " + newCnt
          + " to _managedAppsMap for AppInfo: " + appInfo.getAppName());
      _managedAppsMap.put(appInfo, newCnt);
    }
  }

  public boolean isAccessPermitted(ClientAppInfo appInfo) {
    Integer restrictedRuleCount = _managedAppsMap.get(appInfo);
    boolean result = false;
    if (restrictedRuleCount != null) {
      result = (restrictedRuleCount == 0);
    } else {
      Logger.w(TAG, appInfo.getIndexingKey()
          + " is NOT managed by this category.");
    }
    return result;
  }

  public boolean isAccessDenied(ClientAppInfo appInfo) {
    return ! isAccessPermitted(appInfo);
  }

  public String get_name() {
    return _name;
  }

  public void set_name(String name) {
    _name = name;
  }

  public int get_id() {
    return _id;
  }

  public void set_id(int id) {
    _id = id;
  }

  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("\nCate ID: "+_id).append("\tCate Name: "+_name);
    for (AccessRule rule : _rulesList) {
      buff.append("\nRule auth type: " + rule.getAccessType())
          .append("\nRule recur type: " + rule.getRecurrence().getName())
          .append("\tRecur value: " + rule.getRecurrence().getRecurValue());

      for (TimeRange tr : rule.getTimeRangeList()) {
        buff.append("\nStart Time: " + tr.getStartTime().toString())
        .append("\tEnd Time: " +  tr.getEndTime().toString());
      }
    }

    for (ClientAppInfo appInfo : _managedAppsMap.keySet()) {
      buff.append("\nManaged App: " + appInfo.getAppName())
        .append(", "+ appInfo.getAppPkgname())
        .append(", "+ appInfo.getAppClassname());
    }

    return buff.toString();
  }

  public JSONObject toJsonObject() throws JSONException {
    JSONArray jsonRulesAry = new JSONArray();
    if (_rulesList != null) {
      for (int k=0; k<_rulesList.size(); k++) {
        AccessRule aRule = _rulesList.get(k);

        JSONObject jsonRuleObj = new JSONObject();
        jsonRuleObj.put(TAGNAME_RULE_AUTH_TYPE, aRule.getAccessType());
        jsonRuleObj.put(TAGNAME_RULE_REPEAT_TYPE, aRule.getRecurType());
        jsonRuleObj.put(TAGNAME_RULE_REPEAT_VALUE, aRule.getRecurrence().toString());

        JSONArray jsonTrsAry = new JSONArray();
        List<TimeRange> trsList = aRule.getTimeRangeList();
        for (TimeRange tr : trsList) {
          JSONObject jsonTrObj = new JSONObject();
          jsonTrObj.put(TAGNAME_RULE_REPEAT_STARTTIME, tr.getStartTime().toString());
          jsonTrObj.put(TAGNAME_RULE_REPEAT_ENDTIME, tr.getEndTime().toString());

          jsonTrsAry.put(jsonTrObj);
        }

        jsonRuleObj.put(TAGNAME_ACCESS_TIMERANGES, jsonTrsAry);
        jsonRulesAry.put(jsonRuleObj);
      }
    }

    JSONObject result = new JSONObject();

    result.put(TAGNAME_ACCESS_CATE_ID, _id);
    result.put(TAGNAME_ACCESS_CATE_NAME, _name);
    result.put(TAGNAME_APPLICATION_TYPES, _name);
    result.put(TAGNAME_ACCESS_RULES, jsonRulesAry);

    return result;
  }

  public void populateFromJsObject(JSONObject cateObj) throws JSONException, STDException {
    if (cateObj == null) {
      Logger.w(TAG,  "Input result obj should NOT be NULL");
      return;
    }

    set_id(cateObj.getInt(TAGNAME_ACCESS_CATE_ID));
    set_name(cateObj.getString(TAGNAME_ACCESS_CATE_NAME));

    if (cateObj.has(TAGNAME_ACCESS_RULES) == true) {
      JSONArray rulesAry = cateObj.getJSONArray(TAGNAME_ACCESS_RULES);
      for (int m=0; m<rulesAry.length(); m++) {
        JSONObject ruleObj = rulesAry.getJSONObject(m);

        AccessRule aRule = new AccessRule();
        aRule.setAccessType(ruleObj.getInt(TAGNAME_RULE_AUTH_TYPE));
        Recurrence recur = Recurrence.getInstance(ruleObj.getInt(TAGNAME_RULE_REPEAT_TYPE));
        if (recur.getRecurType() != Recurrence.DAILY) {
          recur.setRecurValue(ruleObj.getInt(TAGNAME_RULE_REPEAT_VALUE));
        }
        aRule.setRecurrence(recur);

        JSONArray timerangeAry = ruleObj.getJSONArray(TAGNAME_ACCESS_TIMERANGES);
        for (int k=0; k<timerangeAry.length(); k++) {
          JSONObject trObj = timerangeAry.getJSONObject(k);

          TimeRange tr = new TimeRange();
          String time = trObj.getString(TAGNAME_RULE_REPEAT_STARTTIME);
          int idx = time.indexOf(':');
          int hour = Integer.parseInt(time.substring(0, idx));
          int min  = Integer.parseInt(time.substring(idx+1));
          tr.setTime(TimeRange.TIME_TYPE_START, hour, min);

          time = trObj.getString(TAGNAME_RULE_REPEAT_ENDTIME);
          idx = time.indexOf(':');
          hour = Integer.parseInt(time.substring(0, idx));
          min  = Integer.parseInt(time.substring(idx+1));
          tr.setTime(TimeRange.TIME_TYPE_END, hour, min);

          aRule.addTimeRange(tr);
        }//for time_ranges

        addAccessRule(aRule);

      }//for rules
    }//if

  }//populate


}
