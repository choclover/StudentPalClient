package com.studentpal.app.handler;

import static com.studentpal.app.listener.ProcessListener.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;

import com.studentpal.app.listener.ProcessListener;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.AccessCategory;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.model.rules.AccessRule;
import com.studentpal.model.rules.Recurrence;
import com.studentpal.model.rules.TimeRange;
import com.studentpal.ui.AccessDeniedNotification;
import com.studentpal.util.ActivityUtil;
import com.studentpal.util.Utils;
import com.studentpal.util.logger.Logger;


public class AccessController implements AppHandler {
  /*
   * Field constants
   */
  static final String TAG = "@@ AccessController";
  //private static final boolean forTest = true;

  private static final int MONITORTASK_INTERVAL = 3000;  //mill-seconds
  private static final int MAX_WATCHED_TASK_NUMBER = 2;
  
  /*
   * Field members
   */
  private static AccessController instance = null;
  private ClientEngine  engine = null;
  private ActivityManager activityManager = null;
  
  private Timer     _monitorTimer = null;
  private TimerTask _monitorTask  = null;
  
  private List<AccessCategory>    _accessCategoryList;
  private HashMap<String, String> _restrictedAppsMap;
  private Set<String>             _processInKillingSet;
  
  private Map<String, ProcessListenerInfo>   _processListenerMap;
  
  /*
   * Methods
   */
  private AccessController() {
  }
  
  public static AccessController getInstance() {
    if (instance == null) {
      instance = new AccessController();
    }
    return instance;
  }

  private void initialize() {
    this.engine = ClientEngine.getInstance();
    this.activityManager = engine.getActivityManager();

    if (_restrictedAppsMap != null) {
      _restrictedAppsMap.clear();
    } else {
      _restrictedAppsMap = new HashMap<String, String>();
    }
    
    if (_processInKillingSet != null) {
      _processInKillingSet.clear();
    } else {
      _processInKillingSet = new HashSet<String>();
    }
    
    if (_processListenerMap != null) {
      _processListenerMap.clear();
    } else {
      _processListenerMap = new HashMap<String, ProcessListenerInfo>();
    }
    
    if (_accessCategoryList != null) {
      _accessCategoryList.clear();
    } else {
      _accessCategoryList = new ArrayList<AccessCategory>();
    }
    
  }
  
  @Override
  public void launch() {
    initialize();
    
    //_loadRestrictedApps(_restrictedAppsMap);
    //runMonitoring(_restrictedAppsMap.size()>0 ? true : false);
    
    _loadAccessCategories(_accessCategoryList);
    rescheduleAccessCategories();
    
    runDailyRescheduleTask();
  }

  @Override
  public void terminate() {
    if (_restrictedAppsMap != null) {
      synchronized (_restrictedAppsMap) {
        _restrictedAppsMap.clear();
      }
    }
    runMonitoring(false);
    
    _terminateAccessCategories();
  }
  
  public void runMonitoring(boolean runMonitor) {
    Logger.i(TAG, "Ready to run monitoring is: "+runMonitor);
    
    if (runMonitor==true) {
      engine.getDaemonHandler().startDaemonTask();
    } else {
      engine.getDaemonHandler().stopDaemonTask();
    }
    
    if (runMonitor==true) {
      //First, kill restricted processes that is already running
      killRestrictedProcs();
      
      //we cannot reuse the old Timer if it is ever cancelled, so have to recreate one
      if (_monitorTask != null) {
        _monitorTask.cancel();
      }
      _monitorTask = getMonitorTask();
      
      if (_monitorTimer != null) {
        _monitorTimer.purge();
        _monitorTimer.cancel();
        _monitorTimer = null;
      }
      _monitorTimer = new Timer();
      _monitorTimer.schedule(_monitorTask, 0, MONITORTASK_INTERVAL);
      
    } else {
      if (_monitorTask != null) {
        _monitorTask.cancel();
        _monitorTask = null;
      }
      if (_monitorTimer != null) {
        _monitorTimer.purge();
        _monitorTimer.cancel();
        _monitorTimer = null;
      }
    }
    
    
  }
  
  public void appendRestrictedApp(ClientAppInfo appInfo) {
    ArrayList<ClientAppInfo> appList = new ArrayList<ClientAppInfo>(1);
    appendRestrictedAppList(appList);
  }
  
  public void appendRestrictedAppList(List<ClientAppInfo> appList) {
    boolean append = true;
    _setRestrictedAppList(this._restrictedAppsMap, appList, append);
  }
  
  public void setRestrictedAppList(List<ClientAppInfo> appList) {
    boolean append = false;
    _setRestrictedAppList(this._restrictedAppsMap, appList, append);
  }
  
  public void removeRestrictedAppList(List<ClientAppInfo> appList) {
    if (appList==null || appList.size()==0) return;
    
    synchronized(_restrictedAppsMap) {
      for (ClientAppInfo appInfo : appList) {
        _restrictedAppsMap.remove(appInfo.getIndexingKey());
      }
    }
    runMonitoring(_restrictedAppsMap.size()>0 ? true : false);
  }
  
  //remove and terminate original categories
  //add in new categories and launch them(reschedule all rules in each category)
  public void setAccessCategories(List<AccessCategory> categories) {
    _terminateAccessCategories();
    _addAccessCategories(categories);
    rescheduleAccessCategories();
  }
  
  public void killRestrictedApps(List runningApps) {
    synchronized (_restrictedAppsMap) {
      boolean restrictedAppFound = false;
        
      for (Object app : runningApps) {
        String pkgName = null;
        String clzName = null;
        if (app instanceof RunningAppProcessInfo) {
          pkgName = ((RunningAppProcessInfo)app).processName;
          
        } else if (app instanceof RunningTaskInfo) {
          pkgName = ((RunningTaskInfo)app).baseActivity.getPackageName();
          clzName = ((RunningTaskInfo)app).baseActivity.getClassName();
        }
        
        if (_restrictedAppsMap.containsKey(pkgName)
            //&& _restrictedAppsMap.get(pkgName).equals(clzName)
        ) {
          RunningAppProcessInfo appProc;

          Logger.i(TAG, "Ready to kill application: " + pkgName);
          if (app instanceof RunningAppProcessInfo) {
            appProc = (RunningAppProcessInfo)app;
            killProcess(appProc);
          } else /*if (app instanceof RunningTaskInfo)*/ {
            //appProc = ActivityUtil.findRunningAppProcess(activityManager, pkgName);
            //if (appProc == null) {
            //  Logger.w(TAG, "Cannot find process for task " + pkgName);
            //  continue;
            //}
            killProcessByName(pkgName);
          }
          
          restrictedAppFound = true;

          if (_processInKillingSet.contains(pkgName)) {
            Logger.d(TAG, pkgName+" is being killed, skipping...");
            //避免restrictedApp连续被kill时，多次显示Notification
            continue;
          } else {
            _processInKillingSet.add(pkgName);
          }
          
          this.engine.launchNewActivity(AccessDeniedNotification.class);
//          this.engine.showAccessDeniedNotification();
//          engine.getMsgHandler().sendEmptyMessage(
//              Event.SIGNAL_SHOW_ACCESS_DENIED_NOTIFICATION);
        }
      }
      
      //Not found any application that is restricted
      if (! restrictedAppFound) {
        //Logger.d(TAG, "clearing _processInKillingSet...");
        _processInKillingSet.clear();
      }
    }
  }
  
  public void rescheduleAccessCategories() {
    for (AccessCategory accessCate : _accessCategoryList) {
      List<AccessRule> rules = accessCate.getAccessRules();
      RuleScheduler scheduler = accessCate.getScheduler();
      scheduler.reScheduleRules(rules);
    }
  }
  
  public void runDailyRescheduleTask() {
    int start_h=24; int start_m=0;
    int start_s=2;  //delay a few seconds to avoid clock drifting(时钟漂移)
    
    final Calendar now = Calendar.getInstance();
    int nowHour = now.get(Calendar.HOUR_OF_DAY);
    int nowMin = now.get(Calendar.MINUTE);
    int nowSec = now.get(Calendar.SECOND);
    
    int delaySec = ((start_h-nowHour)*60 + (start_m-nowMin))*60 + (start_s-nowSec);
    if (delaySec > 0) {
      engine.getMsgHandler().sendEmptyMessageDelayed(
        Event.SIGNAL_ACCESS_RESCHEDULE_DAILY, delaySec*1000);
    }
    Logger.i(TAG, "Now is: " +nowHour+ ':' +nowMin+ ':' +nowSec+ 
        ", scheduling DailyRescheduleTask in " +delaySec+ " seconds.");
    
  }
  
  public void registerProcessListener(String procName, 
      ProcessListener procListener) {
    _modifyProcListenerMap(procName, procListener, true);
  }

  public void unregisterProcessListener(String procName, 
      ProcessListener procListener) {
    _modifyProcListenerMap(procName, procListener, false);
  }

  //////////////////////////////////////////////////////////////////////////////
  class ProcessListenerInfo {
    private int _state = PROCESS_IS_BACKGROUND;
    private Set<ProcessListener> _listeners;
    
    public ProcessListenerInfo() {
      if (_listeners == null) {
        _listeners = new HashSet<ProcessListener>();
      }
    }
    
    public Set<ProcessListener> getListener() {
      return _listeners;
    }
    
    public void addListener(ProcessListener listener) throws STDException {
      if (listener == null) {
        throw new STDException("Process Listener should NOT be NULL!");
      }
      _listeners.add(listener);
    }
    
    public void removeListener(ProcessListener listener) throws STDException {
      if (listener == null) {
        throw new STDException("Process Listener should NOT be NULL!");
      }
      
      if (_listeners == null || false == _listeners.contains(listener)) {
        throw new STDException("Process Listener is NEVER registered!");
      }
      _listeners.remove(_listeners);
    }
    
    public void setToForegroundState(boolean bFG) {
      _state = bFG ? PROCESS_IS_FOREGROUND : PROCESS_IS_BACKGROUND;
    }
    
    public boolean isForegroundState() {
      return (_state == PROCESS_IS_FOREGROUND);
    }
    
  }//class ProcessListenerInfo
  
  private void _loadAccessCategories(List intoList) {
    try {
      List<AccessCategory> catesList = ClientEngine.getInstance()
          .getDBaseManager().loadAccessCategoriesFromDB();
      
      for (AccessCategory cate : catesList) {
        if (cate == null) {
          Logger.w(TAG, "Category should NOT be NULL loaded from DB");
          continue;
        }
        Logger.v(TAG, "Loaded Access Category: "+cate.toString());
        intoList.add(cate);
      }
      
    } catch (STDException ex) {
      Logger.w(TAG, ex.toString());
    }
  }
  
  private void _terminateAccessCategories() {
    if (_accessCategoryList != null && _accessCategoryList.size()>0) {
      synchronized(_accessCategoryList) {
        for (AccessCategory accessCate : _accessCategoryList) {
          RuleScheduler scheduler = accessCate.getScheduler();
          if (scheduler != null) {
            scheduler.terminate();
          }
        }
        _accessCategoryList.clear();
      }
    }//if
  }
  
  private void _addAccessCategories(List<AccessCategory> cateList) {
    if (cateList!=null && cateList.size()>0) {
      synchronized(_accessCategoryList) {
        for (AccessCategory cate : cateList) {
          if (cate != null) {
            _accessCategoryList.add(cate);
          }
        }
      }
    }
  }
  
  private void _setRestrictedAppList(HashMap<String, String> restrictedAppsMap,
      List<ClientAppInfo> appList, boolean append) {
    if (restrictedAppsMap == null) {
      Logger.w(TAG, "Input restrictedAppsMap should NOT be NULL!");
      return;
    }
    
    if (appList!=null && appList.size()>0) {
      synchronized(restrictedAppsMap) {
        if (!append) {
          restrictedAppsMap.clear();
        }
        for (ClientAppInfo appInfo : appList) {
          if (appInfo == null) continue;
          //restrictedAppsMap.put(appInfo.getAppName(), appInfo.getAppClassname());
          //restrictedAppsMap.put(appInfo.getAppPkgname(), appInfo.getAppClassname());
          restrictedAppsMap.put(appInfo.getIndexingKey(), appInfo.getIndexingValue());
        }
      }//sync
      
      runMonitoring(_restrictedAppsMap.size()>0 ? true : false);
    }
  }
  
  private void _modifyProcListenerMap(String procName, 
      ProcessListener procListener, boolean bAdd) {
    if (Utils.isEmptyString(procName)) {
      Logger.w(TAG, "Process Name should NOT be NULL!");
      return;
    }
    
    try {
      if (_processListenerMap.containsKey(procName)) {
        ProcessListenerInfo plInfo = _processListenerMap.get(procName);
        if (bAdd) {
          plInfo.addListener(procListener);
        } else {
          plInfo.removeListener(procListener);
        }

      } else {
        if (bAdd) {
          ProcessListenerInfo plInfo = new ProcessListenerInfo();
          plInfo.addListener(procListener);
          _processListenerMap.put(procName, plInfo);
          
        } else {
          Logger.i(TAG, "Process Listener for "+procName+ " is NOT registered!");
        }
      }
      
    } catch (STDException e) {
      Logger.w(TAG, e.toString());
    }
    
  }
  
  private TimerTask getMonitorTask() {
    TimerTask task = new TimerTask() {
      int i = 0;
      @Override
      public void run() {
        if (i>10000) i=0;
        Logger.v(TAG, "Monitor Task starts to run @ "+ ++i);
        
        List runningApps = getRunningAppsList();
        killRestrictedApps(runningApps);
        handleProcessListener(runningApps);
      }
    };
    return task;
  }

  private boolean killProcessById(RunningAppProcessInfo p) {
    boolean result = true;
    android.os.Process.killProcess(p.pid);
    //android.os.Process.killProcess(android.os.Process.myPid());
    
    return result;
  }

  private boolean killProcess(RunningAppProcessInfo p) {
    String[] pkgs = p.pkgList;
    for (String pkg : pkgs) {
      killProcessByName(pkg);
    }
    return true;
  }
  
  private boolean killProcessByName(String pkgName) {
    if (Utils.isEmptyString(pkgName)) {
      Logger.w(TAG, "PkgName should NOT be empty!");
      return false;
    }
    
    int apiVer = android.os.Build.VERSION.SDK_INT;
    if (apiVer <= android.os.Build.VERSION_CODES.ECLAIR_MR1) {
      //for API 2.1 and earlier version
      activityManager.restartPackage(pkgName);
    } else if (apiVer >= android.os.Build.VERSION_CODES.FROYO) {
      //for API 2.2 and higher version
      activityManager.killBackgroundProcesses(pkgName);
    }
    return true;
  }

  private void killRestrictedProcs() {
    List<RunningAppProcessInfo> processes = activityManager
        .getRunningAppProcesses();
    
    synchronized (_restrictedAppsMap) {
      for (RunningAppProcessInfo process : processes) {
        String pname = process.processName;
        if (_restrictedAppsMap.containsKey(pname)
            //&& process.pkgList.equals(restrictedAppsMap.get(pname))
        ) {
          Logger.i(TAG, "Ready to kill process: " + pname);
          Logger.i(TAG, "Ready to kill process: " + process);
          
          killProcess(process);
        }
      }
    }
  }

  private List getRunningAppsList() {
    List runningApps = null;
    boolean useProcessInfo = false;
    
    if (useProcessInfo) {
      runningApps = activityManager.getRunningAppProcesses();
    } else {
      runningApps = activityManager.getRunningTasks(MAX_WATCHED_TASK_NUMBER);
      //List<RecentTaskInfo> recentTasks = activityManager.getRecentTasks(MAX_WATCHED_APP_NUMBER, 0);
    }
    
    return runningApps;
  }
  
  private void handleProcessListener(List runningApps) {
    if (runningApps == null || runningApps.size()==0) {
      Logger.i(TAG, "Running apps are empty!");
      return;
    }
    if (_processListenerMap == null || _processListenerMap.size()==0) {
      //Logger.i(TAG, "No Process Listener is existing!");
      return;
    }
   
    String topActName = ActivityUtil.getTopActivityName(runningApps.get(0));
    for (String actName : _processListenerMap.keySet()) {
      ProcessListenerInfo listenerInfo = _processListenerMap.get(actName);
      
      if (actName.equals(topActName)) {
        //如果刚开始监听当前前台activity，前一时刻并没有监听，即该activity刚被启动起来
        if (false == listenerInfo.isForegroundState()) {
          Set<ProcessListener> listenerSet = listenerInfo.getListener();
          for (ProcessListener listener : listenerSet) {
            listener.notifyProcessIsForeground(true);  //该activity被调度到前台
          }
          listenerInfo.setToForegroundState(true);
          
        } else {
          //Do nothing
        }

      } else {
        //如果该activity刚才还是前台的，刚刚被调度到后台
        if (true == listenerInfo.isForegroundState()) {
          Set<ProcessListener> listenerSet = listenerInfo.getListener();
          for (ProcessListener listener : listenerSet) {
            listener.notifyProcessIsForeground(false);  //该activity被调度到后台
          }
          listenerInfo.setToForegroundState(false);
        } else {
          //Do nothing
        }
        
      }
    }
    
    
  }
}


class TestCase {
  private final static String TAG = AccessController.TAG+".TestCase";
  
  private AccessCategory getDailyCate() {
    AccessCategory aCate = new AccessCategory();
    aCate.set_id(1);
    aCate.set_name("Cate 1");
    aCate.addManagedApp(new ClientAppInfo("Messaging", "com.android.mms",
        "com.android.mms.Messaging"));
    aCate.addManagedApp(new ClientAppInfo("Alarmclock",
        "com.android.alarmclock", "com.android.alarmclock.Alarmclock"));
    aCate.addManagedApp(new ClientAppInfo("DeskClock",
        "com.android.deskclock", "com.android.deskclock.DeskClock"));
    aCate.addManagedApp(new ClientAppInfo("Browser", "com.android.browser",
        "com.android.browser.Browser"));
    
    try {
      AccessRule aRule = new AccessRule();
      aRule.setAccessType(AccessRule.ACCESS_DENIED);
      
      Recurrence recur = Recurrence.getInstance(Recurrence.DAILY);
      aRule.setRecurrence(recur);

      TimeRange tr = null;

      tr = new TimeRange();
      tr.setTime(TimeRange.TIME_TYPE_START, 9, 45);
      tr.setTime(TimeRange.TIME_TYPE_END,   9, 47);
      aRule.addTimeRange(tr);

      tr = new TimeRange();
      tr.setTime(TimeRange.TIME_TYPE_START, 12, 04);
      tr.setTime(TimeRange.TIME_TYPE_END,   12, 05);
      aRule.addTimeRange(tr);

      tr = new TimeRange();
      tr.setTime(TimeRange.TIME_TYPE_START, 11, 28);
      tr.setTime(TimeRange.TIME_TYPE_END,   11, 30);
      aRule.addTimeRange(tr);

      aCate.addAccessRule(aRule);
      
      /////////////////////////////
      aRule = new AccessRule();
      aRule.setAccessType(AccessRule.ACCESS_PERMITTED);
      
      recur = Recurrence.getInstance(Recurrence.WEEKLY);
      int recureVal = 0;
      recureVal |= (1 << (Calendar.TUESDAY-1) );
      recureVal |= (1 << (Calendar.WEDNESDAY-1) );
      recur.setRecurValue(recureVal);
      aRule.setRecurrence(recur);
      
      tr = new TimeRange();
      tr.setTime(TimeRange.TIME_TYPE_START, 15, 53);
      tr.setTime(TimeRange.TIME_TYPE_END,   15, 54);
      aRule.addTimeRange(tr);
      
      aCate.addAccessRule(aRule);
      
    } catch (STDException e) {
      Logger.w(TAG, e.toString());
    }
    
    return aCate;
  }
  
  private AccessCategory getWeeklyCate() {
    AccessCategory aCate = new AccessCategory();
    return aCate;
  }
   
  private AccessCategory getMonthlyCate() {
    AccessCategory aCate = new AccessCategory();
    return aCate;
  }

}