package com.studentpal.app.handler;

import static com.studentpal.engine.Event.*;

import android.os.Message;
import android.os.RemoteException;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.request.Request;
import com.studentpal.model.exception.STDException;
import com.studentpal.util.Utils;
import com.studentpal.util.logger.Logger;

public class MessageHandler extends android.os.Handler implements AppHandler {
  private static final String TAG = "@@ MessageHandler";
  
  /*
   * Field members
   */  
  private static MessageHandler instance = null;
  private ClientEngine  engine = null;
  private IoHandler     ioHandler = null;
  //private DaemonHandler daemonHandler = null;
  
  private MessageHandler() {
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
  }
  
  public void sendRequestToSvr(Request req) {
    Message msg = this.obtainMessage(SIGNAL_TYPE_REQACK, req);
    this.sendMessage(msg);
  }
  
  @Override
  public void handleMessage(android.os.Message message) {
    Object msg = message.obj;
    int sigType = message.what;
    Logger.i(TAG, "msg type:" /*+msg.getClass().getName()+ "id:"*/ +sigType);
    
    switch(sigType) {
    /*
     * REQ / ACK between server
     */
    case SIGNAL_TYPE_REQACK:
      if (msg instanceof Request) {
        Request req = (Request)msg;
        if (req.isIncomingReq()) {
          //Execute this request in the main thread, 
          //and then append the processed request (i.e. response) to message queue again
          req.execute();
          this.sendRequestToSvr(req);   
        
        } else if (req.isOutgoingReq() && req.isOutputContentReady()) {
          String replyStr = req.getOutputContent();
          if (Utils.isEmptyString(replyStr) ) {
            Logger.d(TAG, "Outgoing reply is NULL or empty for request "+req.getName());
          } else {
            this.ioHandler.sendMsgStr(replyStr);
          }
          
        } else {
          Logger.w(TAG, "Unhandled a request: "+req.getName());
        }
      }
      break;
        
    case SIGNAL_TYPE_OUTSTREAM_READY:
      // Start to login server
      try {
        engine.loginServer();
      } catch (STDException e1) {
        Logger.w(TAG, e1.getMessage());
      }
      break;
      
    case SIGNAL_SHOW_ACCESS_DENIED_NOTIFICATION:
      engine.showAccessDeniedNotification();
      break;
      
    case SIGNAL_ACCESS_RESCHEDULE_DAILY:
      engine.getAccessController().rescheduleAccessCategories();
      engine.getAccessController().runDailyRescheduleTask();
      break;
      
    default:
      super.handleMessage(message);
      break;
      
    } 
    
    
  }

  //////////////////////////////////////////////////////////////////////////////

}