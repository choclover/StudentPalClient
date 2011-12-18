package com.studentpal.engine;

import java.util.HashMap;

public class Event {
  private static final String TAG = "Engine.Event";
  /*
   * Constants
   */
  public static final String APP_PKGNAME_DELIMETER = "|";

  public static final String MESSAGE_HEADER_ACK      = "A";
  public static final String MESSAGE_HEADER_REQ      = "R";
  public static final String MESSAGE_HEADER_NOTIF    = "N";

  public static final String TAGNAME_MSG_TYPE        = "msg_type";
  public static final String TAGNAME_MSG_ID          = "msg_id";
  public static final String TAGNAME_CMD_TYPE        = "cmd_type";
  public static final String TAGNAME_ERR_CODE        = "err_code";
  public static final String TAGNAME_ERR_DESC        = "err_desc";
  public static final String TAGNAME_RESULT          = "result";
  public static final String TAGNAME_ARGUMENTS       = "args";
  //public static final String TAGNAME_BUNDLE_PARAM    = "bundle_param";

  public static final String TAGNAME_VERSION         = "version";
  public static final String TAGNAME_APPSLIST_VER    = "applist_ver";
  public static final String TAGNAME_PHONE_NUM       = "phone_no";
  public static final String TAGNAME_PHONE_IMSI      = "phone_imsi";
  public static final String TAGNAME_PHONE_IMEI      = "phone_imei";
  public static final String TAGNAME_LOGIN_NAME      = "login_name";
  public static final String TAGNAME_LOGIN_PASSWD    = "login_passwd";
  public static final String TAGNAME_NICKNAME        = "nickname";

  public static final String TAGNAME_DEVICES         = "devices";
  //public static final String TAGNAME_DEVICE          = "device";
  public static final String TAGNAME_APPLICATIONS      = "applications";
  public static final String TAGNAME_APPLICATION_TYPES = "application_types";
  public static final String TAGNAME_APP               = "application";
  public static final String TAGNAME_APP_NAME          = "app_name";
  public static final String TAGNAME_APP_CLASSNAME     = "app_classname";
  public static final String TAGNAME_APP_PKGNAME       = "app_pkgname";
  public static final String TAGNAME_APP_COMNAME       = "app_companyname";
  public static final String TAGNAME_APP_TYPEID        = "app_typeid";
  public static final String TAGNAME_APP_TYPENAME      = "app_typename";
  public static final String TAGNAME_APP_TYPEDESC      = "app_typendesc";

  public static final String TAGNAME_ACCESS_CATEGORIES = "access_categories";
  public static final String TAGNAME_ACCESS_CATEGORY   = "access_cate";
  public static final String TAGNAME_ACCESS_CATE_ID    = "cate_id";
  public static final String TAGNAME_ACCESS_CATE_NAME  = "cate_name";

  public static final String TAGNAME_ACCESS_RULES      = "access_rules";
  public static final String TAGNAME_ACCESS_RULE       = "access_rule";
  public static final String TAGNAME_RULE_AUTH_TYPE    = "auth_type";
  public static final String TAGNAME_RULE_REPEAT_TYPE  = "repeat_type";
  public static final String TAGNAME_RULE_REPEAT_VALUE = "repeat_value";
  public static final String TAGNAME_ACCESS_TIMERANGES = "time_ranges";
  public static final String TAGNAME_ACCESS_TIMERANGE  = "time_range";
  public static final String TAGNAME_RULE_REPEAT_STARTTIME  = "start_time";
  public static final String TAGNAME_RULE_REPEAT_ENDTIME    = "end_time";

  /*
   * Extras constants
   */
  public static final String EXTRANAME_COMMAND_TYPE = "command_type";
  public static final String EXTRANAME_FILTERED_PKG = "filtered_pkgname";
//  public static final int    EXTRACMD_REG_FILTERED_PKG   = 1001;
//  public static final int    EXTRACMD_UNREG_FILTERED_PKG = 1002;

  /*
   * TASK constants
   */
  public static final String TASKNAME_Generic               = "Generic";
  public static final String TASKNAME_GetAppList            = "GetAppList";
  public static final String TASKNAME_GetAppTypeList        = "GetAppTypeList";
  public static final String TASKNAME_RefreshAppList        = "RefreshAppList";
  public static final String TASKNAME_SetAppAccessCategory  = "SetAppAccessCategory";
  //public static final String TASKNAME_SetAccessCategories  = "SetAccessCategories";
  /* Tasks from Phone */
  public static final String TASKNAME_LOGIN         = "Login";
  public static final String TASKNAME_LOGIN_ADMIN   = "LoginAdmin";
  public static final String TASKNAME_LOGOUT        = "Logout";

  /*
   * Configuration constants
   */
  public static final String CFG_SHOW_LAUNCHER_UI = "show_launcher_ui";

  /*
   * Intent action / Activity name constants
   */
  //launch the daemon service
  public static final String ACTION_DAEMON_SVC          = "spaldaemon.intent.action.daemonsvc";
  //launch the daemon activity screen, must be the same as value in the Daemon application's Manifest
  public static final String ACTION_DAEMON_LAUNCHER_SCR = "spaldaemon.intent.action.launcherscr";

  public static final String ACTION_MAINSVC_INFO_UPDATED    = "studentpal.action.mainappsvc.updated";
  public static final String ACTION_DAEMONSVC_INFO_UPDATED  = "studentpal.action.daemonsvc.updated";

  public static final String ACTION_PKGINSTALLER_REG_FILTER   = "studentpal.intent.action.reg_filter";
  //Not used yet
  //public static final String ACTION_PKGINSTALLER_UNREG_FILTER = "studentpal.intent.action.unreg_filter";

  /*
   * Error code constants
   */
  public static final int ERRCODE_NOERROR                   = 0;
  public static final int ERRCODE_TIMEOUT                   = 100;
  public static final int ERRCODE_USERNAME_NOT_EXIST        = 101;
  public static final int ERRCODE_PASSWORD_MISMATCH         = 102;
  public static final int ERRCODE_TARGET_NOT_EXIST          = 103;
  public static final int ERRCODE_DATA_NEWER_THAN_DB        = 104;

  public static final int ERRCODE_CLIENT_CONN_LOST          = 200;
  public static final int ERRCODE_SERVER_CONN_LOST          = 300;
  public static final int ERRCODE_MSG_FORMAT_ERR            = 400;
  public static final int ERRCODE_RESP_MSG_FORMAT_ERR       = 401;

  public static final int ERRCODE_SERVER_INTERNAL_ERR       = 500;

  /*
   * Value constants
   */
  public static final int MSG_ID_INVALID = -1;
  public static final int MSG_ID_NOTUSED = 0;

  public static final int    RECUR_TYPE_DAILY        = 0x01;
  public static final int    RECUR_TYPE_WEEKLY       = 0x02;
  public static final int    RECUR_TYPE_MONTHLY      = 0x03;
  public static final int    RECUR_TYPE_YEARLY       = 0x04;
  public static final String TXT_RECUR_TYPE_DAILY    = "daily";
  public static final String TXT_RECUR_TYPE_WEEKLY   = "weekly";
  public static final String TXT_RECUR_TYPE_MONTHLY  = "monthly";
  public static final String TXT_RECUR_TYPE_YEARLY   = "yearly";

  public static final int ACCESS_TYPE_DENIED    = 0x01;
  public static final int ACCESS_TYPE_PERMITTED = 0x02;
  public static final String TXT_ACCESS_TYPE_DENIED    = "access_denied";
  public static final String TXT_ACCESS_TYPE_PERMITTED = "access_permitted";

  //signaling that a request or response is coming in
  public static final int SIGNAL_TYPE_UNKNOWN                    = -1;
  public static final int SIGNAL_TYPE_MSG_FROM_SVR               = 1001;
  public static final int SIGNAL_TYPE_MSG_TO_SVR                 = 1002;
  public static final int SIGNAL_TYPE_START_WATCHING_APP         = 1003;
  public static final int SIGNAL_TYPE_STOP_WATCHING_APP          = 1004;

  public static final int SIGNAL_SHOW_ACCESS_DENIED_NOTIFICATION = 1011;
  public static final int SIGNAL_ACCESS_RESCHEDULE_DAILY         = 1012;

  //------------------------------------------------------------------
  public static final int SIGNAL_TYPE_NETWORK_FAIL               = 1050;
  public static final int SIGNAL_TYPE_OUTSTREAM_READY            = 1051;

  public static final int SIGNAL_TYPE_DEVICE_ADMIN_ENABLED       = 1062;
  public static final int SIGNAL_TYPE_DEVICE_ADMIN_DISABLED      = 1063;
  public static final int SIGNAL_TYPE_REG_FILTERED_PKG           = 1064;
  public static final int SIGNAL_TYPE_UNREG_FILTERED_PKG         = 1065;

  public static final int SIGNAL_TYPE_START_DAEMONTASK           = 2001;
  public static final int SIGNAL_TYPE_STOP_DAEMONTASK            = 2002;
  public static final int SIGNAL_TYPE_EXIT_DAEMONTASK            = 2003;
  //Daemon WatchDog message related
  public static final int SIGNAL_TYPE_DAEMON_WD_REQ              = 2004;
  public static final int SIGNAL_TYPE_DAEMON_WD_RESP             = 2005;
  public static final int SIGNAL_TYPE_DAEMON_WD_TIMEOUT          = 2006;

  //-------------------------------------------------------------------
  public static final int SIGNAL_TYPE_RESP_LOGIN                 = 3000;
  public static final int SIGNAL_TYPE_RESP_GetAppList            = 3001;
  public static final int SIGNAL_TYPE_RESP_RefreshAppList        = 3002;

  private static final HashMap<Integer, String> ERRCODE_DESC_MAPPER
    = new HashMap<Integer, String>();
  // static {
  // ERRCODE_DESC_MAPPER.put(ERRCODE_NOERROR, "SUCCESS");
  // }

  //////////////////////////////////////////////////////////////////////////////
  /*
   * Member fields
   */
  private int type = 0;
  private int errcoe = 0;
  private Object data = new Object();

  public void setData(int type, int code, Object data) {
    this.type = type;
    this.errcoe = code;
    this.data = data;
  }

  public int getType() {
    return this.type;
  }

  public int getCode() {
    return this.errcoe;
  }

  public Object getData() {
    return this.data;
  }


}
