package com.studentpal.app.db;

import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_ID;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_NAME;
import static com.studentpal.engine.Event.TAGNAME_APP_CLASSNAME;
import static com.studentpal.engine.Event.TAGNAME_APP_NAME;
import static com.studentpal.engine.Event.TAGNAME_APP_PKGNAME;
import static com.studentpal.engine.Event.TAGNAME_PHONE_IMSI;
import static com.studentpal.engine.Event.TAGNAME_PHONE_NUM;
import static com.studentpal.engine.Event.TAGNAME_RULE_AUTH_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_ENDTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_STARTTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_VALUE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.AccessCategory;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.model.rules.AccessRule;
import com.studentpal.model.rules.Recurrence;
import com.studentpal.model.rules.TimeRange;
import com.studentpal.model.user.ClientUser;
import com.studentpal.util.Utils;
import com.studentpal.util.logger.Logger;

public class DBaseManager /*implements AppHandler*/ {
  /*
   * Constants
   */
  private static final String TAG = "@@ DBaseManager";

//  private static final String DATABASE_ROOT = "/mnt/sdcard/studentpal/db/";
  private static final String DATABASE_NAME         = "studentpal";
  private static final String DATABASE_FNAME        = DATABASE_NAME + ".db";
  private static final String DATABASE_FNAME_ADMIN  = DATABASE_NAME + "_admin.db";

  private static final String TABLE_NAME_ACCESS_CATEGORIES  = "access_categories";
  private static final String TABLE_NAME_ACCESS_RULES       = "access_rules";
  private static final String TABLE_NAME_MANAGED_APPS       = "managed_applications";
  private static final String TABLE_NAME_MANAGED_DEVICE     = "managed_device";

  private static final String COL_NAME_APPSLIST             = "installedApps";
  private static final String COL_NAME_APPSLIST_VERSION     = "installedAppsListVer";
  private static final String COL_NAME_IS_ACTIVE            = "isActive";
  //private static final String COL_NAME_APPSLIST_NAME        = "apps_list_version";

  private static final String TIME_LIST_DELIMETER = ";";

  public static final int    INVALID_APPLIST_VERSION = -1;

  /*
   * Member fields
   */
  private static DBaseManager dataManager;

  private ClientEngine  engine = null;
  private SQLiteDatabase mDb;
  private File mDbFile;

  private boolean isAdmin = false;

  /*
   * 获取实例，应当在欢迎界面之后建立实例
   */
  public static DBaseManager getInstance() {
    if (dataManager == null) {
      dataManager = new DBaseManager();
    }
    return dataManager;
  }

  private DBaseManager() {
    initialize();
  }

  /////////////////////////////////////////////////////////////////////////////
  public void saveAccessCategoriesToDB(List<AccessCategory> catesList) {
    if (catesList==null || catesList.size()==0) return;
    Logger.i(TAG, "enter saveAccessCategoriesToDB()!");

    try {
      mDb = openDB();

      //clear old records first
      //String sqlStr = "DELETE FROM " + ACCESS_CATEGORY_TABLE_NAME;
      //mDb.execSQL(sqlStr);
      long res = -1;
      res = mDb.delete(TABLE_NAME_ACCESS_CATEGORIES, "1", null);
      res = mDb.delete(TABLE_NAME_ACCESS_RULES, "1", null);
      if (! isAdmin) {
        //对于管理端，不能将所有的受控程序记录首先删除
        res = mDb.delete(TABLE_NAME_MANAGED_APPS, "1", null);
      }

      ContentValues cv;
      for (AccessCategory aCate : catesList) {
        cv = new ContentValues();
        cv.put(TAGNAME_ACCESS_CATE_ID,    aCate.get_id());
        cv.put(TAGNAME_ACCESS_CATE_NAME,  aCate.get_name());
        res = mDb.insert(TABLE_NAME_ACCESS_CATEGORIES, null, cv);

        //Insert access rules records
        List<AccessRule> cateRules = aCate.getAccessRules();
        if (cateRules!=null && cateRules.size()>0) {
          for (AccessRule rule : cateRules) {
            cv = new ContentValues();
            cv.put(TAGNAME_RULE_AUTH_TYPE, rule.getAccessType());
            cv.put(TAGNAME_RULE_REPEAT_TYPE, rule.getRecurType());
            cv.put(TAGNAME_RULE_REPEAT_VALUE, rule.getRecurrence().toString());

            String startTimeStr = "";
            String endTimeStr = "";
            for (TimeRange tr : rule.getTimeRangeList()) {
              startTimeStr += tr.getStartTime().toString() + TIME_LIST_DELIMETER;
              endTimeStr += tr.getEndTime().toString() + TIME_LIST_DELIMETER;
            }
            cv.put(TAGNAME_RULE_REPEAT_STARTTIME, startTimeStr);
            cv.put(TAGNAME_RULE_REPEAT_ENDTIME, endTimeStr);
            cv.put(TAGNAME_ACCESS_CATE_ID, aCate.get_id());

            res = mDb.insert(TABLE_NAME_ACCESS_RULES, null, cv);
          }
        }

        //Insert access managed apps records
        Set<ClientAppInfo> managedApps = aCate.getManagedApps().keySet();
        if (managedApps!=null && managedApps.size()>0) {
          for (ClientAppInfo appInfo : managedApps) {
            cv = new ContentValues();
            cv.put(TAGNAME_APP_NAME,      appInfo.getAppName());
            cv.put(TAGNAME_APP_PKGNAME,   appInfo.getAppPkgname());
            String val = appInfo.getAppClassname();
            if (! Utils.isEmptyString(val)) {
              cv.put(TAGNAME_APP_CLASSNAME, val);
            }
            cv.put(TAGNAME_ACCESS_CATE_ID, aCate.get_id());

            Cursor curApps = mDb.query(TABLE_NAME_MANAGED_APPS,
                null,
                TAGNAME_APP_PKGNAME +"='"+ appInfo.getAppPkgname() +"'",
                null, null, null, null);
            if (curApps.moveToFirst()) {
              res = mDb.update(TABLE_NAME_MANAGED_APPS, cv,
                  "TAGNAME_APP_PKGNAME = "+appInfo.getAppPkgname(), null);
            } else {
              res = mDb.insert(TABLE_NAME_MANAGED_APPS, null, cv);
            }
          }
        }
      }

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
      //rebuildTables(mDb);
    } finally {
      mDb.close();
    }
  }

  public List<AccessCategory> loadAccessCategoriesFromDB() throws STDException {
    List<AccessCategory> catesList = new ArrayList<AccessCategory>();
    Logger.i(TAG, "enter loadAccessCategoriesFromDB()!");

    try {
      mDb = openDB();

      Cursor curCate = mDb.query(TABLE_NAME_ACCESS_CATEGORIES, null, null,
          null, null, null, null);
      while (curCate.moveToNext()) {
        AccessCategory aCate = new AccessCategory();

        int startIdx = 0;
        int cate_id = curCate.getInt(startIdx++);  //0
        aCate.set_id(cate_id);
        aCate.set_name(curCate.getString(startIdx++));  //1

        Cursor curRule = mDb.query(TABLE_NAME_ACCESS_RULES, null,
            TAGNAME_ACCESS_CATE_ID + "=" + aCate.get_id(), null, null, null, null);
        while (curRule.moveToNext()) {
          AccessRule aRule = new AccessRule();

          startIdx = 1;
          aRule.setAccessType(curRule.getInt(startIdx++));  //1
          Recurrence recur = Recurrence.getInstance(curRule.getInt(startIdx++));  //2
          if (recur.getRecurType() == Recurrence.DAILY) {
            startIdx++;
            recur.setRecurValue(0);
          } else {
            recur.setRecurValue(curRule.getInt(startIdx++));  //3
          }
          aRule.setRecurrence(recur);

          String timeStr = curRule.getString(startIdx++);
          StringTokenizer startTokens = new StringTokenizer(timeStr,  //4
              TIME_LIST_DELIMETER);
          timeStr = curRule.getString(startIdx++);
          StringTokenizer endTokens = new StringTokenizer(timeStr,  //5
              TIME_LIST_DELIMETER);
          while (startTokens.hasMoreTokens()) {
            TimeRange tr = new TimeRange();

            timeStr = startTokens.nextToken();
            tr.setTime(TimeRange.TIME_TYPE_START, timeStr);

            timeStr = endTokens.nextToken();
            tr.setTime(TimeRange.TIME_TYPE_END, timeStr);

            aRule.addTimeRange(tr);
          }//for time_ranges

          aCate.addAccessRule(aRule);

        }//while curRule
        curRule.close();

        Cursor curApp = mDb.query(TABLE_NAME_MANAGED_APPS, null,
            TAGNAME_ACCESS_CATE_ID + "=" + aCate.get_id(), null, null, null, null);
        while (curApp.moveToNext()) {
          startIdx = 1;
          String appName = curApp.getString(startIdx++);    //1
          String pkgName = curApp.getString(startIdx++);    //2
          String className = curApp.getString(startIdx++);  //3
          ClientAppInfo appInfo = new ClientAppInfo(appName, pkgName, className);
          aCate.addManagedApp(appInfo);

        }//while curApp
        curApp.close();

        catesList.add(aCate);

      }//while curCate
      curCate.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
      //rebuildTables(mDb);
    } finally {
      mDb.close();
    }

    return catesList;
  }

  public void saveManagedAppsToDB(Set<ClientAppInfo> appsListSet) {
    if (appsListSet==null || appsListSet.size()==0) return;
    Logger.i(TAG, "enter saveManagedAppsToDB()!");

    try {
      mDb = openDB();
      long res = -1;

      ContentValues cv;
      String appVal;
      for (ClientAppInfo appInfo : appsListSet) {
        cv = new ContentValues();
        appVal = appInfo.getAppName();
        cv.put(TAGNAME_APP_NAME,      appVal);
        appVal = appInfo.getAppPkgname();
        cv.put(TAGNAME_APP_PKGNAME,   appVal);
        appVal = appInfo.getAppClassname();
        if (! Utils.isEmptyString(appVal)) {
          cv.put(TAGNAME_APP_CLASSNAME, appVal);
        }

        Cursor curApps = mDb.query(TABLE_NAME_MANAGED_APPS,
            null,
            TAGNAME_APP_PKGNAME +"='"+ appInfo.getAppPkgname() +"'",
            null, null, null, null);
        if (curApps.moveToFirst()) {
          //record already exists
          res = mDb.update(TABLE_NAME_MANAGED_APPS, cv,
              TAGNAME_APP_PKGNAME +"='"+ appInfo.getAppPkgname() +"'", null);
        } else {
          res = mDb.insert(TABLE_NAME_MANAGED_APPS, null, cv);
        }
        curApps.close();
      }

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
      //rebuildTables(mDb);
    } finally {
      mDb.close();
    }
  }

  public void saveManagedDevInfoToDB(ClientUser managedDev) {
    Set<ClientUser> managedDevs = new HashSet<ClientUser>();
    managedDevs.add(managedDev);
    saveManagedDevInfoToDB(managedDevs);
  }

  public void saveManagedDevInfoToDB(Set<ClientUser> managedDevs) {
    Logger.i(TAG, "enter saveManagedDevInfoToDB()!");
    if (managedDevs==null || managedDevs.size()==0) {
      return;
    }

    try {
      mDb = openDB();
      long res = -1;

      ContentValues cv;
      String appVal;
      for (ClientUser managedDev : managedDevs) {
        cv = new ContentValues();
        appVal = managedDev.getPhoneNum();
        cv.put(TAGNAME_PHONE_NUM, appVal);
        appVal = managedDev.getPhoneImsi();
        cv.put(TAGNAME_PHONE_IMSI, appVal);
        appVal = managedDev.getPhoneImei();

        int appListVer = managedDev.getInstalledAppsListVer();
        if (appListVer > 0) {
          cv.put(COL_NAME_APPSLIST_VERSION, appListVer);
        }

        appVal = managedDev.getInstalledApps();
        if (appVal != null) {
          cv.put(COL_NAME_APPSLIST, appVal);
        }
        cv.put(COL_NAME_IS_ACTIVE, 1);

        Cursor curDev = mDb.query(TABLE_NAME_MANAGED_DEVICE,
            null,
            TAGNAME_PHONE_NUM +"='"+ managedDev.getPhoneNum() +"'",
            null, null, null, null);
        if (curDev.moveToFirst()) {
          //record already exists
          res = mDb.update(TABLE_NAME_MANAGED_DEVICE, cv,
              TAGNAME_PHONE_NUM +"='"+ managedDev.getPhoneNum() +"'", null);
        } else {
          res = mDb.insert(TABLE_NAME_MANAGED_DEVICE, null, cv);
        }
        curDev.close();

      }//for
    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }
  }

  public int getAppsListVersion(String phone_no) {
    Logger.i(TAG, "enter getAppsListVersion()!");

    int appListVer = 0;
    try {
      mDb = openDB();
      Cursor curDevice = mDb.query(TABLE_NAME_MANAGED_DEVICE,
          null/*new String[] {COL_NAME_APPSLIST_VERSION}*/,
          TAGNAME_PHONE_NUM +"='"+ phone_no +"'",
          null, null, null, null);

      String appListStr = "";
      if (curDevice.moveToFirst()) {
        //col 3 is COL_NAME_APPSLIST_VERSION
        int startIdx = 3;
        appListVer = curDevice.getInt(startIdx++);

        //检查每个受控的Application记录都存在于表中，否则重新获取记录列表，即返回版本号为0
        appListStr = curDevice.getString(startIdx++);
        if (! Utils.isEmptyString(appListStr)) {
          String[] pkgNameAry = appListStr.split("\\"+Event.APP_PKGNAME_DELIMETER);
          if (pkgNameAry!=null && pkgNameAry.length>0) {
            for (String pkgName : pkgNameAry) {
              Cursor curApp = mDb.query(TABLE_NAME_MANAGED_APPS,
                  null, TAGNAME_APP_PKGNAME +"='"+ pkgName +"'", null, null, null, null);
              //当某个Application记录不存在于表中
              if (! curApp.moveToFirst()) {
                appListVer = 0;
                curApp.close();
                break;
              }
              curApp.close();
            }
          }
        }
      }
      curDevice.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }

    return appListVer;
  }

  public Set<ClientUser> getManagedDevsSet() {
    Logger.i(TAG, "enter getAppsListVersion()!");
    Set<ClientUser> result = null;

    try {
      mDb = openDB();
      Cursor curDevice = mDb.query(TABLE_NAME_MANAGED_DEVICE,
          null,
          COL_NAME_IS_ACTIVE +"=1",
          null, null, null, null);

      int devCnt = curDevice.getCount();
      if (devCnt > 0)  result = new HashSet<ClientUser>(devCnt);
      while (curDevice.moveToNext()) {
        String phoneNum  = curDevice.getString(1);
        String phoneImsi = curDevice.getString(2);
        ClientUser managedDev = new ClientUser(phoneNum, phoneImsi);
        managedDev.setInstalledAppsListVer(curDevice.getInt(3));
        managedDev.setInstalledApps(curDevice.getString(4));

        result.add(managedDev);
      }
      curDevice.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }

    return result;
  }

  //////////////////////////////////////////////////////////////////////////////
  private String getDatabaseRoot() {
    String result = "";

//    int apiVer = android.os.Build.VERSION.SDK_INT;
//    if (apiVer <= android.os.Build.VERSION_CODES.ECLAIR_MR1) {
//      // for API 2.1 and earlier version
//      result = "/sdcard/studentpal/db/";
//    } else if (apiVer >= android.os.Build.VERSION_CODES.FROYO) {
//      // for API 2.2 and higher version
//      result = "/mnt/sdcard/studentpal/db/";
//    }
//    result = Environment.getDataDirectory().toString();

    result = getDbFilePath().getParent();

    return result;
  }

  private void initialize() {
    engine = ClientEngine.getInstance();
    isAdmin = engine.isAdmin();

    mDbFile = getDbFilePath();
    File dbFolder = mDbFile.getParentFile();
    if (!dbFolder.exists()) {
      dbFolder.mkdirs();
    }

    mDb = openDB();
    if (mDb != null) {
      createTables(mDb);
      mDb.close();
    }
  }

  private File getDbFilePath() {
    File result = null;
    ClientEngine engine = ClientEngine.getInstance();
    if (isAdmin == true) {
      result = engine.getContext().getApplicationContext()
          .getDatabasePath(DATABASE_FNAME_ADMIN);
    } else {
      result = engine.getContext().getApplicationContext()
          .getDatabasePath(DATABASE_FNAME);
    }

    return result;
  }

  /*
   * 打开数据库
   */
  private synchronized SQLiteDatabase openDB() {
    SQLiteDatabase db = null;

    try {
      db = SQLiteDatabase.openDatabase(this.mDbFile.getAbsolutePath(),
          null, SQLiteDatabase.OPEN_READWRITE + SQLiteDatabase.CREATE_IF_NECESSARY);
    } catch (SQLiteException e) {
      Logger.w(TAG, e.toString());
    }
    return db;
  }

  /*
   * 创建数据表
   */
  private void createTables(SQLiteDatabase dbase) {
    if (dbase == null || dbase.isOpen()==false) {
      Logger.w(TAG, "DBASE is NULL or NOT open!");
      return;
    }

    final String create_catory_table_sql = new StringBuffer().append(
      "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_ACCESS_CATEGORIES).append(
      "( " +TAGNAME_ACCESS_CATE_ID+   " INTEGER PRIMARY KEY ").append(
      ", " +TAGNAME_ACCESS_CATE_NAME+ " TEXT").append(
      ");").toString();
    dbase.execSQL(create_catory_table_sql);

    final String create_rules_table_sql = new StringBuffer().append(
      "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_ACCESS_RULES).append(
      "(  _id INTEGER PRIMARY KEY AUTOINCREMENT ").append(
      ", " +TAGNAME_RULE_AUTH_TYPE+        " INTEGER").append(
      ", " +TAGNAME_RULE_REPEAT_TYPE+      " INTEGER").append(
      ", " +TAGNAME_RULE_REPEAT_VALUE+     " INTEGER").append(
      ", " +TAGNAME_RULE_REPEAT_STARTTIME+ " TEXT").append(
      ", " +TAGNAME_RULE_REPEAT_ENDTIME+   " TEXT").append(
      ", " +TAGNAME_ACCESS_CATE_ID+        " INTEGER").append(
      ", FOREIGN KEY(" +TAGNAME_ACCESS_CATE_ID+ ") REFERENCES " +TABLE_NAME_ACCESS_CATEGORIES+ "(" +TAGNAME_ACCESS_CATE_ID+ ")").append(
      ");").toString();
    dbase.execSQL(create_rules_table_sql);

    final String create_applications_table_sql = new StringBuffer().append(
        "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_MANAGED_APPS).append(
        "( _id INTEGER PRIMARY KEY AUTOINCREMENT ").append(
        ", " +TAGNAME_APP_NAME+       " TEXT").append(
        ", " +TAGNAME_APP_PKGNAME+    " TEXT").append(
        ", " +TAGNAME_APP_CLASSNAME+  " TEXT").append(
        ", " +TAGNAME_ACCESS_CATE_ID+ " INTEGER").append(
        ", FOREIGN KEY(" +TAGNAME_ACCESS_CATE_ID+ ") REFERENCES " +TABLE_NAME_ACCESS_CATEGORIES+ "(" +TAGNAME_ACCESS_CATE_ID+ ")").append(
        ");").toString();
    dbase.execSQL(create_applications_table_sql);

    if (isAdmin) {
      final String create_managed_device_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_MANAGED_DEVICE).append(
          "( _id INTEGER PRIMARY KEY AUTOINCREMENT ").append(
          ", " +TAGNAME_PHONE_NUM+       " TEXT").append(
          ", " +TAGNAME_PHONE_IMSI+      " TEXT").append(
          ", " +COL_NAME_APPSLIST_VERSION+ " INTEGER DEFAULT 0").append(
          ", " +COL_NAME_APPSLIST+         " TEXT DEFAULT NULL").append(
          ", " +COL_NAME_IS_ACTIVE+        " INTEGER DEFAULT 0").append(
          ");").toString();
      dbase.execSQL(create_managed_device_table_sql);
    }

  }

  private void rebuildTables(SQLiteDatabase dbase) {
    if (dbase == null || dbase.isOpen()==false) {
      Logger.w(TAG, "DBASE is NULL or NOT open!");
      return;
    }

    List <String> table_name_ary = new ArrayList<String>();
    table_name_ary.add(TABLE_NAME_ACCESS_CATEGORIES);
    table_name_ary.add(TABLE_NAME_ACCESS_RULES);
    table_name_ary.add(TABLE_NAME_MANAGED_APPS);
    if (isAdmin) {
      table_name_ary.add(TABLE_NAME_MANAGED_DEVICE);
    }

    for (int i=0; i<table_name_ary.size(); i++) {
      String drop_table_sql = "DROP TABLE " +table_name_ary.get(i)+ " ;";
      dbase.execSQL(drop_table_sql);
    }

    createTables(dbase);
  }
}

