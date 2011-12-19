package com.studentpal.app.handler;

import static com.studentpal.engine.Event.SIGNAL_ACCESS_RESCHEDULE_DAILY;
import static com.studentpal.engine.Event.SIGNAL_SHOW_ACCESS_DENIED_NOTIFICATION;
import static com.studentpal.engine.Event.SIGNAL_TYPE_MSG_FROM_SVR;
import static com.studentpal.engine.Event.SIGNAL_TYPE_MSG_TO_SVR;
import static com.studentpal.engine.Event.SIGNAL_TYPE_NETWORK_FAIL;
import static com.studentpal.engine.Event.SIGNAL_TYPE_OUTSTREAM_READY;
import static com.studentpal.engine.Event.SIGNAL_TYPE_RESP_GetAppList;
import static com.studentpal.engine.Event.SIGNAL_TYPE_RESP_LOGIN;
import static com.studentpal.engine.Event.SIGNAL_TYPE_RESP_RefreshAppList;
import static com.studentpal.engine.Event.SIGNAL_TYPE_UNKNOWN;
import static com.studentpal.engine.Event.TAGNAME_PHONE_NUM;
import static com.studentpal.engine.Event.TAGNAME_PHONE_IMSI;
import static com.studentpal.engine.Event.TAGNAME_ERR_CODE;
import static com.studentpal.engine.Event.TAGNAME_RESULT;
import static com.studentpal.engine.Event.TASKNAME_SyncAppList;
import static com.studentpal.engine.Event.TASKNAME_LOGIN;
import static com.studentpal.engine.Event.TASKNAME_LOGIN_ADMIN;
import static com.studentpal.engine.Event.TASKNAME_RefreshAppList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Message;
import android.util.Log;

import com.studentpal.app.db.DBaseManager;
import com.studentpal.app.listener.EventListener;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.engine.request.Request;
import com.studentpal.model.AccessCategory;
import com.studentpal.model.AppTypeInfo;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.model.user.ClientUser;
import com.studentpal.util.Utils;
import com.studentpal.util.logger.Logger;

/**
 * @author Simon He
 * This class is used for handling message which is sent to and received from
 * server, and then dispatch message to corresponding components to process.
 */
public class MessageHandler extends android.os.Handler implements AppHandler {
  private static final String TAG = "@@ MessageHandler";

  /*
   * Field members
   */
  private static MessageHandler instance = null;
  private ClientEngine  engine = null;
  private IoHandler     ioHandler = null;
  //private DaemonHandler daemonHandler = null;

  private Map<Integer, Set<EventListener>> eventsListenerMap = null;

  /*
   * Methods
   */
  private MessageHandler() {
    initialize();
  }

  public static MessageHandler getInstance() {
    if (instance == null) {
      instance  = new MessageHandler();
    }
    return instance;
  }

  @Override
  public void launch() {
    this.engine = ClientEngine.getInstance();
    this.ioHandler = engine.getIoHandler();
    //this.daemonHandler = engine.getDaemonHandler();
  }

  @Override
  public void terminate() {
    removeMessages(0);

    //clean up the events listeners
    if (eventsListenerMap!=null && eventsListenerMap.size()>0) {
      Collection<Set<EventListener>> c = eventsListenerMap.values();
      Iterator<Set<EventListener>> iter = c.iterator();
      while (iter.hasNext()) {
        Set<EventListener> aSet = iter.next();
        if (aSet != null) {
          aSet.clear();
        }
        aSet = null;
      }

      eventsListenerMap.clear();
      eventsListenerMap = null;
    }
  }

  public void addEventListener(int evtType, EventListener listener) {
    if (evtType<=0 || listener==null) {
      Logger.w(TAG, "Invalid param with evtType of '" +evtType+
          "' or listener of " + listener);
      return;
    }

    Set<EventListener> evtListenerSet = eventsListenerMap.get(evtType);
    if (evtListenerSet == null) {
      evtListenerSet = new HashSet<EventListener>();
    }
    evtListenerSet.add(listener);
    eventsListenerMap.put(evtType, evtListenerSet);
  }

  public void removeEventListener(int evtType, EventListener listener) {
    if (evtType<=0 || listener==null) {
      Logger.w(TAG, "Invalid param with evtType of '" +evtType+
          "' or listener of " + listener);
      return;
    }

    Set<EventListener> evtListenerSet = eventsListenerMap.get(evtType);
    if (evtListenerSet != null) {
      evtListenerSet.remove(listener);
      if (evtListenerSet.size() == 0) {
        evtListenerSet = null;
      }
    }
  }

  public void reomveAllEventListeners(int evtType) {
    Set<EventListener> evtListenerSet = eventsListenerMap.get(evtType);
    if (evtListenerSet != null) {
      evtListenerSet.clear();
      evtListenerSet = null;
    }
  }

  public void sendMessageToServer(Request req) {
    Message msg = this.obtainMessage(SIGNAL_TYPE_MSG_TO_SVR, req);
    this.sendMessage(msg);
  }

  public void receiveMessageFromServer(String msgStr) {
    try {
      JSONObject msgObjRoot = new JSONObject(msgStr);
      String msgType = msgObjRoot.getString(Event.TAGNAME_MSG_TYPE);

      if (msgType.equals(Event.MESSAGE_HEADER_REQ)) {
        // This is an incoming request message
        handleRequestMessage(msgObjRoot);

      } else if (msgType.equals(Event.MESSAGE_HEADER_ACK)) {
        // This is a (incoming) response message
        handleResponseMessage(msgObjRoot);

      } else {
        Logger.i(TAG, "Unsupported Incoming MESSAGE type(" + msgType
            + ") in this version.");
      }

    } catch (JSONException ex) {
      Logger.w(TAG, "JSON paring error for request:\n\t" + msgStr);
      Logger.w(TAG, ex.toString());
    }
  }

  @Override
  public void handleMessage(android.os.Message message) {
    Object msgBody = message.obj;
    int sigType = message.what;
    Logger.i(TAG, "msg type:" /*+msg.getClass().getName()+ "id:"*/ +sigType);

    switch(sigType) {
    /*
     * REQ / ACK between server
     */
    case SIGNAL_TYPE_MSG_TO_SVR:
    case SIGNAL_TYPE_MSG_FROM_SVR:
      if (msgBody instanceof Request) {
        Request req = (Request)msgBody;
        if (req.isIncomingReq()) {
          //Execute this request in the main thread,
          //and then append the processed request (i.e. response) to message queue again
          req.execute();
          this.sendMessageToServer(req);

        } else if (req.isOutgoingReq() && req.isOutputContentReady()) {
          String replyStr = req.getOutputContent();
          if (Utils.isEmptyString(replyStr) ) {
            Logger.d(TAG, "Outgoing reply is NULL or empty for request "+req.getName());
          } else {
            //Send message to remote server via IoHandler
            this.ioHandler.sendMsgStr(replyStr);
          }

        } else {
          Logger.w(TAG, "Unhandled a request: "+req.getName());
        }
      }
      break;

    case SIGNAL_SHOW_ACCESS_DENIED_NOTIFICATION:
      engine.showAccessDeniedNotification();
      break;

    case SIGNAL_ACCESS_RESCHEDULE_DAILY:
      engine.getAccessController().rescheduleAccessCategories();
      engine.getAccessController().runDailyRescheduleTask();
      break;

    case SIGNAL_TYPE_OUTSTREAM_READY:
      // IO output stream is ready, so start to login to remote server
      // Move loginServer to Main Admin UI Screen
      if (engine.isAdmin() == false) {
        try {
          engine.loginServerFromClient();
        } catch (STDException e) {
          Log.w(TAG, e.toString());
        }
      }
      break;

    case SIGNAL_TYPE_NETWORK_FAIL:
      // show network error dialog
    default:
      super.handleMessage(message);
      break;
    }

    //Notify every EventHandler that their interested event has arrived
    Set<EventListener> evtListenerSet = eventsListenerMap.get(sigType);
    if (evtListenerSet != null) {
      Iterator<EventListener> iter = evtListenerSet.iterator();
      while (iter.hasNext()) {
        EventListener listener = iter.next();
        if (listener != null) {
          listener.notifyEventArrived(sigType, msgBody);
        }
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private void initialize() {
    if (eventsListenerMap == null) {
      eventsListenerMap = new HashMap<Integer, Set<EventListener>>();
    }
  }

  private void receiveRequestFromServer(Request req) {
    Message msg = this.obtainMessage(SIGNAL_TYPE_MSG_FROM_SVR, req);
    this.sendMessage(msg);
  }

  /*
   * Received a "R" type message from server,
   * then send the incoming request to MessageHandler to handle
   */
  private void handleRequestMessage(JSONObject msgObjRoot) throws JSONException {
    try {
      String reqPkgName = Utils.getPackageName(Request.class);
      String reqType = msgObjRoot.getString(Event.TAGNAME_CMD_TYPE);
      String reqClazName = reqPkgName + reqType + "Request";
      Logger.i(TAG, "Ready to create new instance of:"+reqClazName);

      Request request;
      request = (Request) Class.forName(reqClazName).newInstance();

      if (request != null) {
        int msgId = msgObjRoot.getInt(Event.TAGNAME_MSG_ID);
        request.setRequestSeq(msgId);

        if (msgObjRoot.has(Event.TAGNAME_ARGUMENTS)) {
          JSONObject args = msgObjRoot.getJSONObject(Event.TAGNAME_ARGUMENTS);
          request.setInputArguments(args);
        }

        //Received a REQUEST message from server,
        //then send the incoming request to MessageHandler to handle
        receiveRequestFromServer(request);
      }

    } catch (InstantiationException ex) {
      Logger.w(TAG, ex.toString());
    } catch (IllegalAccessException e) {
      Logger.w(TAG, e.toString());
    } catch (ClassNotFoundException e) {
      Logger.w(TAG, e.toString());
    }
  }

  /*
   * Received a "A" type message from server,
   * then send the incoming response to MessageHandler to handle
   */
  private void handleResponseMessage(JSONObject msgObjRoot) throws JSONException {
    String respType = msgObjRoot.getString(Event.TAGNAME_CMD_TYPE);
    int errCode = msgObjRoot.getInt(TAGNAME_ERR_CODE);
    int evtType = SIGNAL_TYPE_UNKNOWN;

    JSONObject resultObj = null;
    if (msgObjRoot.has(TAGNAME_RESULT)) {
      resultObj = msgObjRoot.getJSONObject(TAGNAME_RESULT);
    }

    Event respEvt = null;

    if (Request.isEqualRequestType(respType, TASKNAME_LOGIN_ADMIN)) {
      Set<ClientUser> clientUserSet = null;
      if (resultObj != null) {
        clientUserSet = saveManagedDevsInfoToDB(resultObj);
      }

      if (resultObj != null) {
        if (resultObj.has(TAGNAME_PHONE_NUM)) {
          ClientEngine.getInstance().setPhoneNum(resultObj.getString(TAGNAME_PHONE_NUM));
        }
        if (resultObj.has(TAGNAME_PHONE_IMSI)) {
          ClientEngine.getInstance().setPhoneIMSI(resultObj.getString(TAGNAME_PHONE_IMSI));
        }
      }

      evtType = SIGNAL_TYPE_RESP_LOGIN;
      respEvt = new Event();
      respEvt.setData(evtType, errCode, clientUserSet);

    } else if (Request.isEqualRequestType(respType, TASKNAME_LOGIN)) {
      if (resultObj != null) {
        if (resultObj.has(TAGNAME_PHONE_NUM)) {
          ClientEngine.getInstance().setPhoneNum(resultObj.getString(TAGNAME_PHONE_NUM));
        }
        if (resultObj.has(TAGNAME_PHONE_IMSI)) {
          ClientEngine.getInstance().setPhoneIMSI(resultObj.getString(TAGNAME_PHONE_IMSI));
        }
      }

    } else if (Request.isEqualRequestType(respType, TASKNAME_SyncAppList)) {
      Set<ClientAppInfo> appsInfoSet = null;
      if (resultObj != null) {
        appsInfoSet = saveManagedAppsInfoToDB(resultObj);
      }

      evtType = SIGNAL_TYPE_RESP_GetAppList;
      respEvt = new Event();
      respEvt.setData(evtType, errCode, appsInfoSet);

    } else if (Request.isEqualRequestType(respType, TASKNAME_RefreshAppList)) {
      Set<ClientAppInfo> appsInfoSet = null;
      if (resultObj != null) {
        appsInfoSet = saveManagedAppsInfoToDB(resultObj);
      }

      evtType = SIGNAL_TYPE_RESP_RefreshAppList;
      respEvt = new Event();
      respEvt.setData(evtType, errCode, appsInfoSet);
    }

    //Dispatch ACK event to corresponding screen to handle
    if (evtType != SIGNAL_TYPE_UNKNOWN) {
      Message msg = this.obtainMessage(evtType, respEvt);
      this.sendMessage(msg);
    }
  }

  private Set<ClientUser> saveManagedDevsInfoToDB(JSONObject jsonResObj) {
    if (jsonResObj == null) {
      Logger.w(TAG,  "Input result obj should NOT be NULL");
      return null;
    }

    Set<ClientUser> managedDevs = null;
    try {
      JSONArray  devAry = jsonResObj.getJSONArray(Event.TAGNAME_DEVICES);
      if (devAry!=null && devAry.length()>0) {

        managedDevs = new HashSet<ClientUser>(devAry.length());

        for (int i=0; i<devAry.length(); i++) {
          JSONObject devObj = devAry.getJSONObject(i);
          String phoneNo = devObj.getString(Event.TAGNAME_PHONE_NUM);
          String phoneImsi = devObj.getString(Event.TAGNAME_PHONE_IMSI);
          String phoneImei = null;
          if (devObj.has(Event.TAGNAME_PHONE_IMEI)) {
            phoneImei = devObj.getString(Event.TAGNAME_PHONE_IMEI);
          }

          ClientUser managedDev = new ClientUser(phoneNo, phoneImsi, phoneImei);
          managedDevs.add(managedDev);
        }

        DBaseManager.getInstance().saveManagedDevInfoToDB(managedDevs);
      }

    } catch (JSONException e) {
      Logger.d(TAG, e.toString());
    }

    return managedDevs;
  }

  private Set<ClientAppInfo> saveManagedAppsInfoToDB(JSONObject jsonResObj) {
    if (jsonResObj == null) {
      Logger.w(TAG,  "Input result obj should NOT be NULL");
      return null;
    }

    Set<ClientAppInfo> appsInfoList = null;
    try {
      String installedApps = "";

      JSONArray jsonAppsAry = jsonResObj.getJSONArray(Event.TAGNAME_APPLICATIONS);
      if (jsonAppsAry!=null && jsonAppsAry.length()>0) {

        appsInfoList = new HashSet<ClientAppInfo>();

        for (int i=0; i<jsonAppsAry.length(); i++) {
          ClientAppInfo appInfo = new ClientAppInfo(jsonAppsAry.getJSONObject(i));
          appsInfoList.add(appInfo);

          installedApps += appInfo.getAppPkgname() + Event.APP_PKGNAME_DELIMETER;
        }

        DBaseManager.getInstance().saveManagedAppsToDB(appsInfoList);
      }

      Set<AppTypeInfo> appTypesList = null;
      JSONArray jsonAppTypesAry = jsonResObj.getJSONArray(
          Event.TAGNAME_APPLICATION_TYPES);
      if (jsonAppTypesAry!=null && jsonAppTypesAry.length()>0) {
        appTypesList = new HashSet<AppTypeInfo>();
        for (int i=0; i<jsonAppTypesAry.length(); i++) {
          AppTypeInfo appTypeInfo = new AppTypeInfo(jsonAppTypesAry.getJSONObject(i));
          appTypesList.add(appTypeInfo);
        }
        DBaseManager.getInstance().saveManagedAppTypesToDB(appTypesList);
      }

      if (jsonResObj.has(Event.TAGNAME_PHONE_NUM)) {
        String phoneNum = jsonResObj.getString(Event.TAGNAME_PHONE_NUM);
        int appListVer = jsonResObj.getInt(Event.TAGNAME_VERSION);
        ClientUser managedDev = new ClientUser(phoneNum, null, null);
        managedDev.setInstalledAppsListVer(appListVer);
        managedDev.setInstalledApps(installedApps);

        DBaseManager.getInstance().saveManagedDevInfoToDB(managedDev);
      }

    } catch (JSONException e) {
      Logger.d(TAG, e.toString());
    }

    return appsInfoList;
  }

  private Set<AccessCategory> saveAccessCatesInfoToDB(JSONObject jsonResObj) {
    //TODO
    Set<AccessCategory> result = null;

    return result;
  }
}