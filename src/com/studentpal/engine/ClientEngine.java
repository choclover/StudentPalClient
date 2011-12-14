package com.studentpal.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import com.studentpal.app.MainAppService;
import com.studentpal.app.ResourceManager;
import com.studentpal.app.db.DBaseManager;
import com.studentpal.app.handler.AccessController;
import com.studentpal.app.handler.AppHandler;
import com.studentpal.app.handler.DaemonHandler;
import com.studentpal.app.handler.IoHandler;
import com.studentpal.app.handler.MessageHandler;
import com.studentpal.app.receiver.SystemStateReceiver;
import com.studentpal.engine.request.LoginRequest;
import com.studentpal.engine.request.Request;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.model.user.AdminUser;
import com.studentpal.model.user.ClientUser;
import com.studentpal.ui.AccessDeniedNotification;
import com.studentpal.util.ActivityUtil;
import com.studentpal.util.Utils;
import com.studentpal.util.logger.Logger;

public class ClientEngine implements AppHandler {

  private static final String TAG = "@@ ClientEngine";

  /*
   * Field Members
   */
  private static ClientEngine instance = null;

  //Flag for indicating if it is the device admin controller
  private boolean             _isAdmin           = false;

  private Context             _launcher;
  private PackageManager      _packageManager   = null;
  private SystemStateReceiver _sysStateReceiver = null;
  private ActivityManager     _activityManager  = null;
  private TelephonyManager    _teleManager      = null;

  //Handlers
  private Set<AppHandler>     appHandlerSet     = null;
  private MessageHandler      msgHandler        = null;
  private IoHandler           ioHandler         = null;
  private AccessController    accController     = null;
  private DaemonHandler       daemonHandler     = null;
  private DBaseManager        dbaseManager      = null;

  //Global MSG ID which will increase by 1 for each different request instance
  private static int gMsgId = 0;

  /*
   * Methods
   */
  private ClientEngine() {
  }

  public static ClientEngine getInstance() {
    if (instance == null) {
      instance = new ClientEngine();
    }
    return instance;
  }

  public void initialize(Context context, boolean isAdmin) throws STDException {
    if (context == null) {
      throw new STDException("Context launcher should NOT be NULL");
    } else {
      this._launcher = context;
    }

    this._isAdmin = isAdmin;
    ClientEngine.gMsgId = 0;

    //Create Activity Manager
    this._activityManager = (ActivityManager)this._launcher.getSystemService(Context.ACTIVITY_SERVICE);
    //Create Telephony Manager
    this._teleManager = (TelephonyManager)this._launcher.getSystemService(Context.TELEPHONY_SERVICE);

    //Register System State Broadcast receiver
    if (! _isAdmin) {
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
      intentFilter.addAction(Intent.ACTION_SCREEN_ON);
      intentFilter.addAction(Intent.ACTION_MAIN);
      this._sysStateReceiver = new SystemStateReceiver();
      this._launcher.registerReceiver(_sysStateReceiver, intentFilter);
    }

    /**
     * App Handlers
     */
    //Create MessageHandler instance
    this.msgHandler = MessageHandler.getInstance();

    //Create IoHandler instance
    this.ioHandler =  IoHandler.getInstance();

    if (! _isAdmin) {
      //Create AccessController instance
      this.accController = AccessController.getInstance();

      //Create DaemonHandler instance
      this.daemonHandler = DaemonHandler.getInstance();
    }

    //Create DBaseManager instance
    this.dbaseManager = DBaseManager.getInstance();

    if (appHandlerSet == null) {
      appHandlerSet = new HashSet<AppHandler>();

      appHandlerSet.add(msgHandler);
      appHandlerSet.add(ioHandler);

      if (! _isAdmin) {
        appHandlerSet.add(accController);
        appHandlerSet.add(daemonHandler);
      }

//      appHandlerSet.add(dbaseManager);
    }
  }

  @Override
  public void launch() {
    for (AppHandler handler : appHandlerSet) {
      if (handler != null) {
        handler.launch();
      }
    }
  }

  @Override
  public void terminate() {
    if (appHandlerSet != null) {
      for (AppHandler handler : appHandlerSet) {
        if (handler != null) {
          handler.terminate();
        }
      }
      appHandlerSet.clear();
      appHandlerSet = null;
    }

    if (_launcher != null) {
      if (_sysStateReceiver != null) {
        _launcher.unregisterReceiver(_sysStateReceiver);
      }
      _launcher = null;
    }
  }

  public Context getContext() {
    return _launcher;
  }

  public ActivityManager getActivityManager() {
    return _activityManager;
  }

  public MessageHandler getMsgHandler() {
    return msgHandler;
  }

  public IoHandler getIoHandler() {
    return ioHandler;
  }

  public AccessController getAccessController() {
    return accController;
  }

  public DaemonHandler getDaemonHandler() {
    return daemonHandler;
  }

  public DBaseManager getDBaseManager() {
    return dbaseManager;
  }

  public PackageManager getPackageManager() {
    if (_packageManager == null) {
      _packageManager = _launcher.getPackageManager();
    }
    return _packageManager;
  }

  public boolean isAdmin() {
    return _isAdmin;
  }

  // Utility Methods ////////////////////////////////////////////////////////////
  public static int getNextMsgId() {
    return ++gMsgId;
  }
  public static void resetMsgId() {
    gMsgId = 0;
  }

  public String getPhoneNum() {
    String result = "";
    if (this._teleManager != null) {
      result = this._teleManager.getLine1Number();
//      if (Utils.isEmptyString(result)) {
//        result = this._teleManager.getDeviceId();
//      }
    }
    return result;
  }

  public String getPhoneIMEI() {
    String result = "";
    if (this._teleManager != null) {
      result = this._teleManager.getDeviceId();
    }
    return result;
  }

  public String getPhoneIMSI() {
    String result = "";
    if (this._teleManager != null) {
      result = this._teleManager.getSubscriberId();
    }
    return result;
  }

  public int getApiVersion() {
    return android.os.Build.VERSION.SDK_INT;
  }

  // Message handling methods //////////////////////////////////////////////////

  public void showAccessDeniedNotification() {
    ActivityUtil.launchNewActivity(this._launcher, AccessDeniedNotification.class);
  }

  /*
   * Get a list of installed applications
   */
  public List<ClientAppInfo> getAppList() {
    List<ApplicationInfo> applications = getPackageManager().getInstalledApplications(0);
    //List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);

    List<ClientAppInfo> result = new ArrayList<ClientAppInfo>(applications.size());

    Iterator<ApplicationInfo> iter = applications.iterator();
    while (iter.hasNext()) {
      ClientAppInfo clientApp = new ClientAppInfo(iter.next());
      result.add(clientApp);
      Logger.d(TAG, "Adding AppInfo with name: "+clientApp.getAppName()
                +", \nPackageName: "+clientApp.getAppPkgname()
                +", \nClassName: "+clientApp.getAppClassname()
                );
    }

    return result;
  }

  public void loginServerFromClient() throws STDException {
    Logger.i(TAG, "enter loginServerFromClient");

    String phoneNum = getPhoneNum();
    String imsiNum = getPhoneIMSI();
    if (Utils.isValidPhoneNumber(phoneNum) == false &&
        Utils.isValidPhoneIMSI(imsiNum) == false) {
      throw new STDException("Unable to login, got invalid phone number of " + phoneNum
          + ", invalid IMSI number of " + imsiNum);
    }

    //This is a Client engine
    ClientUser user = new ClientUser(phoneNum, imsiNum);
    Request request = new LoginRequest(Event.TASKNAME_LOGIN, user);
    msgHandler.sendMessageToServer(request);
  }

  public void loginServerFromAdmin(String loginName, String loginPwd) throws STDException {
    Logger.i(TAG, "enter loginServerFromAdmin");

    //This is a Admin engine
    AdminUser user = new AdminUser(loginName, loginPwd);
    Request request = new LoginRequest(Event.TASKNAME_LOGIN_ADMIN, user);
    msgHandler.sendMessageToServer(request);
  }

  public void logoutServer() throws STDException {
    Logger.i(TAG, "enter loginServerFromClient");

    Request request = new LoginRequest(Event.TASKNAME_LOGOUT, null);
    msgHandler.sendMessageToServer(request);
  }

  /*
   * Update information displayed on launcher screen
   */
  public void updateLauncherScreenInfo(String action, String info) {
    if (! MainAppService.forTest) return;

    Intent intent = new Intent();
    intent.putExtra("info", info);
    intent.setAction(action);
    this._launcher.sendBroadcast(intent);
  }

  public void showNetworkErrorDialog() {
    Logger.v(TAG, "Ready to show network error dialog!");
    if (_isAdmin) {
      String title = ResourceManager.RES_STR_NETWORK_ERROR;
      String msgStr = ResourceManager.RES_STR_NETWORK_SETUP_FAIL;
      ActivityUtil.showConfirmDialog(_launcher, title, msgStr);
    }
  }


}



