package com.studentpal.app.handler;

import static com.studentpal.engine.Event.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.app.ResourceManager;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.engine.request.Request;
import com.studentpal.model.codec.Codec;
import com.studentpal.model.exception.STDException;
import com.studentpal.util.Utils;
import com.studentpal.util.logger.Logger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class IoHandler implements AppHandler {

  private static final String TAG = "IoHandler";
  private static final int SLEEP_TIME = 100;  //mill-seconds

  /*
   * Field members
   */
  private static IoHandler instance = null;
  private static String serverIP = "";

  private ClientEngine  engine = null;
  private MessageHandler msgHandler = null;

  //private boolean isLogin = false;  //TODO how to use this flag in client?

  private Socket socketConn = null;
  private BufferedInputStream bis = null;
  private BufferedOutputStream bos = null;

  private InputConnectionThread inputConnThread  = null;
  private OutputConnectionThread outputConnThread  = null;

  /*
   * Methods
   */
  private IoHandler() {
    initialize();
  }

  public static IoHandler getInstance() {
    if (instance == null) {
      instance = new IoHandler();
    }
    return instance;
  }

  @Override
  public void launch() {
    this.engine = ClientEngine.getInstance();
    this.msgHandler = this.engine.getMsgHandler();

    if (false) {    //init network synchronously
      //init_network();
    } else {    //asynchronously
      Runnable r = new Runnable() {
        @Override
        public void run() {
          init_network();
        }
      };
      new Thread(r).start();
    }
  }

  @Override
  public void terminate() {
    if (inputConnThread != null) {
      inputConnThread.terminate();
      inputConnThread = null;
    }
    if (outputConnThread != null) {
      outputConnThread.terminate();
      outputConnThread = null;
    }

    if (bis != null)
      try {
        bis.close();
      } catch (IOException e) {
        Logger.w(TAG, e.toString());
      }

    if (bos != null)
      try {
        bos.close();
      } catch (IOException e) {
        Logger.w(TAG, e.toString());
      }

    if (socketConn != null)
      try {
        socketConn.close();
      } catch (IOException e) {
        Logger.w(TAG, e.toString());
      }
  }

  public String getRemoteSvrIP() {
    /*
     * Do NOT use localhost/127.0.0.1 which is the phone itself
     */
    // TODO read from config
    String addr = "";

    if (! engine.isAdmin()) {
      addr = engine.getContext().getString(com.studentpal.R.string.target_svr_ip);
    } else {
      addr = ResourceManager.target_svr_ip;
    }

    //Read from Launcher Screen
    if (Utils.isValidIpv4Address(serverIP)) {
      addr = serverIP;
    }

    return addr;
  }

  public String getRemoteSvrDomainName() {
    // TODO read from config
    String addr = "coeustec.gicp.net";
    return addr;
  }

  public int getRemoteSvrPort() {
    return 9177;  //9123;
  }

  public String getEncoding() {
    return "UTF-8";
  }

  public void sendMsgStr(String msg) {
    try {
      if (outputConnThread != null) {
        //通过Output Connection线程去发送message
        outputConnThread.sendMsgStr(msg);
      } else {
        Logger.w(TAG, "Output connection is NULL");
      }
    } catch (STDException e) {
      Logger.w(TAG, "SendMsgStr() got error of "+e.getMessage());
    }
  }

  public void sendSignalToMainUI(int signalType) {
    Message signal = msgHandler.obtainMessage(signalType);
    msgHandler.sendMessage(signal);
  }

  //For test Launcher screen purpose
  public static void setServerIP(String ip) {
    serverIP = ip;
  }

  //////////////////////////////////////////////////////////////////////////////
  private void initialize() {
  }

  private void init_network() {
    //reset the network
    if (socketConn != null) {
      try { socketConn.close(); }
      catch (IOException e) { e.printStackTrace(); }
      socketConn = null;
    }

    socketConn = constructSocketConnection();
    if (socketConn == null) {
      Logger.w(TAG, "Creating Socket returns NULL!");
      sendSignalToMainUI(Event.SIGNAL_TYPE_NETWORK_FAIL);

    } else {
      try {
        bis = new BufferedInputStream(socketConn.getInputStream());
        bos = new BufferedOutputStream(socketConn.getOutputStream());

        inputConnThread = new InputConnectionThread();
        inputConnThread.start();
        outputConnThread = new OutputConnectionThread();
        outputConnThread.start();

      } catch (IOException e) {
        Logger.w(TAG, e.toString());
      }
    }
  }

  @SuppressWarnings("unused")
  private Socket constructSocketConnection() {
    Socket aSock = null;
    final int RETRY_TIMES = 1;

    for (int i=0; i<RETRY_TIMES; i++) {
      try {
        String svrAddr = getRemoteSvrDomainName();
        svrAddr = getRemoteSvrIP();
        int svrPort = getRemoteSvrPort();

        Logger.d(TAG, "Connecting to " + svrAddr +":"+ svrPort);
        aSock = new Socket(InetAddress.getByName(svrAddr), svrPort);
        aSock.setKeepAlive(true);
        Logger.d(TAG, "Connected to "+aSock.getInetAddress().toString());

      } catch (UnknownHostException e) {
        Logger.w(TAG, e.toString());
      } catch (IOException e) {
        Logger.w(TAG, e.toString());
      }

      if (aSock != null) break;

      try {
        if (RETRY_TIMES > 1) {
          Thread.sleep((i*10+5) * 1000);  //休息时间逐渐延长
        }
      } catch (InterruptedException e) {
        Logger.w(TAG, e.toString());
      }
    }

    return aSock;
  }

  /*
   * Received a "R" type message from server,
   * then send the incoming request to MessageHandler to handle
   */
  public void handleRequestMessage(JSONObject msgObjRoot) throws JSONException {
    try {
      String reqPkgName = Request.class.getName();
      if (reqPkgName.indexOf('.') != -1) {
        reqPkgName = reqPkgName.substring(0, reqPkgName.lastIndexOf('.')+1);
      } else {
        reqPkgName = "";
      }

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
        msgHandler.receiveMessageFromServer(request);
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
  public void handleResponseMessage(JSONObject msgObjRoot) throws JSONException {
    String respType = msgObjRoot.getString(Event.TAGNAME_CMD_TYPE);
    int errCode = msgObjRoot.getInt(TAGNAME_ERR_CODE);
    int evtType = SIGNAL_TYPE_UNKNOWN;
    Event respEvt = null;

    if (Request.isEqualRequestType(respType, TASKNAME_LOGIN_ADMIN)) {
      evtType = SIGNAL_TYPE_RESP_LOGIN;
      respEvt = new Event();
      respEvt.setData(evtType, errCode, null);
    }

    if (evtType != SIGNAL_TYPE_UNKNOWN) {
      Message msg = msgHandler.obtainMessage(evtType, respEvt);
      msgHandler.sendMessage(msg);
    }
  }

  //**************************************************************************//
  /**
   * Output Connection thread
   * @author Simon He
   */
  class OutputConnectionThread extends Thread/*HandlerThread*/ {
    private static final String TAG = "@@ OutputConnectionThread";
    private Handler outputMsgHandler = null;
//    private boolean isReady = false;

    public OutputConnectionThread() {
      super(TAG);
    }

    public void terminate() {
      this.interrupt();

      if (outputMsgHandler != null) {
        outputMsgHandler.removeMessages(0);
        outputMsgHandler = null;
      }
    }

    //public boolean isReady() {
    //  return isReady;
    //}

    public void run() {
      /*
       * Output message handler
       */
      Looper.prepare();

      this.outputMsgHandler = new Handler(/*this.getLooper()*/) {
        @Override
        public void handleMessage(android.os.Message message) {
          String msgStr = (String) message.obj;

          try {
            sendMsgStr_internal(msgStr);
          } catch (STDException e) {
            Logger.w(TAG, e.toString());
          }
        }
      };

      //we can start to login server now since OUTPUT stream is ready
      sendSignalToMainUI(Event.SIGNAL_TYPE_OUTSTREAM_READY);

      Looper.loop();
    }

    public void sendMsgStr(String msgStr) throws STDException {
      if (this.outputMsgHandler == null) {
        throw new STDException("Output Msg handler should NOT be null!");
      }
      //通过线程内部的Handler去处理message，从而不再占用主UI线程去处理message
      Message msg = this.outputMsgHandler.obtainMessage(0, msgStr);
      this.outputMsgHandler.sendMessage(msg);
    }

    ////////////////////////////////////////////////////////////////////////////
    private void sendMsgStr_internal(String msg) throws STDException {
      if (bos == null) {
        throw new STDException("Output stream should NOT be null!");
      }
      Logger.i("Ready to send msg:\n"+msg);

      try {
        byte[] msgBytes;
        msgBytes = msg.getBytes(getEncoding());
        msgBytes = Codec.encode(msgBytes);

        bos.write(msgBytes);
        bos.flush();

      } catch (UnsupportedEncodingException e) {
        Logger.w(TAG, e.toString());
      } catch (IOException e) {
        Logger.w(TAG, e.toString());
      }
    }

  }//class OutputConnectionThread

  //**************************************************************************//
  /**
   * Input Connection thread
   * @author Simon He
   */
  class InputConnectionThread extends Thread {
    private static final String TAG = "@@ InputConnectionThread";

    private boolean isStop = false;

    public void terminate() {
      isStop = true;
      this.interrupt();
    }

    public void run() {
      try {
        /*
         * Input message receiver
         */
        byte[] buffer = null;

        while (!isStop) {
          if (bis.available() > 0) {
            buffer = new byte[bis.available()];
            bis.read(buffer);

            String msgStr = new String(buffer, getEncoding());
            Logger.i(TAG, "AndrClient Got a message:\n" + msgStr);
            if (Utils.isEmptyString(msgStr)) {
              continue;
            }

            try {
              JSONObject msgObjRoot = new JSONObject(msgStr);
              String msgType = msgObjRoot.getString(Event.TAGNAME_MSG_TYPE);

              if (msgType.equals(Event.MESSAGE_HEADER_REQ)) {
                //This is a incoming request message
                handleRequestMessage(msgObjRoot);

              } else if (msgType.equals(Event.MESSAGE_HEADER_ACK)) {
                //This is a response message
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

          Thread.sleep(SLEEP_TIME);

        }// while !stopped

      } catch (InterruptedException e) {
        Logger.w(TAG, e.toString());
      } catch (UnsupportedEncodingException e) {
        Logger.w(TAG, e.toString());
      } catch (IOException e) {
        Logger.w(TAG, e.toString());
      }
    }

  }// class RemoteConnectionThread

}//class IoHandler



