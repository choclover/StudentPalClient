package com.studentpal.app.handler;

import static com.studentpal.engine.Event.SIGNAL_ACCESS_RESCHEDULE_DAILY;
import static com.studentpal.engine.Event.SIGNAL_SHOW_ACCESS_DENIED_NOTIFICATION;
import static com.studentpal.engine.Event.SIGNAL_TYPE_MSG_FROM_SVR;
import static com.studentpal.engine.Event.SIGNAL_TYPE_MSG_TO_SVR;
import static com.studentpal.engine.Event.SIGNAL_TYPE_NETWORK_FAIL;
import static com.studentpal.engine.Event.SIGNAL_TYPE_OUTSTREAM_READY;
import static com.studentpal.engine.Event.SIGNAL_TYPE_RESP_LOGIN;
import static com.studentpal.engine.Event.SIGNAL_TYPE_UNKNOWN;
import static com.studentpal.engine.Event.TAGNAME_ERR_CODE;
import static com.studentpal.engine.Event.TASKNAME_LOGIN;
import static com.studentpal.engine.Event.TASKNAME_LOGIN_ADMIN;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Message;

import com.studentpal.R;
import com.studentpal.app.listener.EventListener;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.engine.request.Request;
import com.studentpal.util.ActivityUtil;
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

  public void reomveEventListener(int evtType, EventListener listener) {
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
      // Move loginServer to Main UI Screen
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

  public void receiveMessageFromServer(Request req) {
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
          String args = msgObjRoot.getString(Event.TAGNAME_ARGUMENTS);
          request.setInputArguments(args);
        }

        //Received a REQUEST message from server,
        //then send the incoming request to MessageHandler to handle
        receiveMessageFromServer(request);
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
    Event respEvt = null;

    if (Request.isEqualRequestType(respType, TASKNAME_LOGIN_ADMIN)) {
      evtType = SIGNAL_TYPE_RESP_LOGIN;
      respEvt = new Event();
      respEvt.setData(evtType, errCode, null);
    } else if (Request.isEqualRequestType(respType, TASKNAME_LOGIN)) {
      //TODO
    }

    if (evtType != SIGNAL_TYPE_UNKNOWN) {
      Message msg = this.obtainMessage(evtType, respEvt);
      this.sendMessage(msg);
    }
  }

}