package com.studentpal.app.handler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.studentpal.app.ResourceManager;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.codec.Codec;
import com.studentpal.model.exception.STDException;
import com.studentpal.util.Utils;
import com.studentpal.util.logger.Logger;

public class IoHandler implements AppHandler {
  private static final String TAG = "IoHandler";

  /*
   * Constants
   */
  private static final int SLEEP_TIME = 250;  //mill-seconds
  private static final String CHARSET_NAME = "UTF-8";
  private static final int LOG_LENGTH_LIMIT = 512;

  /*
   * Field members
   */
  private static IoHandler instance = null;
  private static String serverIP = "";

  private ClientEngine  engine      = null;
  private MessageHandler msgHandler = null;

  //private boolean isLogin = false;  //TODO how to use this flag in client?

  private Socket socketConn = null;
  //private SocketChannel socketConn = null;

  private BufferedInputStream bis  = null;
  private BufferedOutputStream bos = null;
  private Thread initNetworkThread = null;

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

    if (true) { // init network asynchronously
      Runnable r = new Runnable() {
        @Override
        public void run() {
          init_network();
        }
      };
      initNetworkThread = new Thread(r);
      initNetworkThread.start();

    } else {// init network synchronously
      // init_network();
    }
  }

  @Override
  public void terminate() {
    Logger.v(TAG, "enter terminate()!");

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

    if (initNetworkThread != null)
      initNetworkThread.interrupt();
    initNetworkThread = null;
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
    if (socketConn == null || false==socketConn.isConnected()) {
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
        aSock = new Socket();
        InetSocketAddress isa = new InetSocketAddress(
            InetAddress.getByName(svrAddr), svrPort);
        aSock.connect(isa, getSocketTimeout());
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

  private SocketChannel constructSocketChannel() {
    SocketChannel channel = null;

    try {
      channel = SocketChannel.open();

      String svrAddr = getRemoteSvrDomainName();
      svrAddr = getRemoteSvrIP();
      int svrPort = getRemoteSvrPort();

      InetSocketAddress isa = new InetSocketAddress(svrAddr, svrPort);
      channel.connect(isa);
      channel.configureBlocking(false);

    } catch (UnknownHostException e) {
      Logger.w(TAG, e.toString());
    } catch (IOException e) {
      Logger.w(TAG, e.toString());
    }

    return channel;
  }

  private String getRemoteSvrIP() {
    /*
     * Do NOT use localhost/127.0.0.1 which is the phone itself
     */
    // TODO read from config
    String addr = "";

    if ( engine.isAdmin()) {
      addr = ResourceManager.target_svr_ip;
    } else {
      addr = ResourceManager.target_svr_ip;
      //addr = engine.getContext().getString(com.studentpal.R.string.target_svr_ip);

      //Read from Launcher Screen
      if (Utils.isValidIpv4Address(serverIP)) {
        addr = serverIP;
      }
    }

    return addr;
  }

  private String getRemoteSvrDomainName() {
    // TODO read from config
    String addr = "coeustec.gicp.net";
    return addr;
  }

  private int getRemoteSvrPort() {
    return 9177;  //9123;
  }

  private String getEncoding() {
    return CHARSET_NAME;
  }

  public int getSocketTimeout() {
    //TODO read from config
    return 3*1000;
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
          int markLimit = bis.available();
          if (markLimit > 0) {
            byte[] msgLenBytes = new byte[Codec.MSG_LENGTH_HEADER_SIZE];
            bis.read(msgLenBytes);   // 读取前4字节

            int msgLen = Utils.byteArrayToInt(msgLenBytes);
            int markPos = 0;
            buffer = new byte[msgLen];
            while (msgLen > 0) {
              markPos = bis.read(buffer, markPos, msgLen);
              msgLen -= markPos;
            }

            // All data is available now
            String msgStr = new String(buffer, getEncoding());
            if (Utils.isEmptyString(msgStr)) {
              continue;
            }
            if (msgStr.length() > LOG_LENGTH_LIMIT) {
              //Logger.i(TAG, "AndrClient Got a too long message to print\n");
              msgStr = msgStr.substring(0, LOG_LENGTH_LIMIT) + "  ......";
            }
            Logger.i(TAG, "AndrClient Got a message:\n" + msgStr);

            msgHandler.receiveMessageFromServer(msgStr);

            // 如果读取内容后还粘了包，就让发送方再传送 一次数据，进行下一次解析
            if (bis.available() > 0) {
              continue;
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



