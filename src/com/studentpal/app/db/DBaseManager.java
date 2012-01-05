package com.studentpal.app.db;

import static com.studentpal.engine.Event.APP_PKGNAME_DELIMETER;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_ID;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_NAME;
import static com.studentpal.engine.Event.TAGNAME_APP_CLASSNAME;
import static com.studentpal.engine.Event.TAGNAME_APP_NAME;
import static com.studentpal.engine.Event.TAGNAME_APP_PKGNAME;
import static com.studentpal.engine.Event.TAGNAME_APP_TYPEDESC;
import static com.studentpal.engine.Event.TAGNAME_APP_TYPEID;
import static com.studentpal.engine.Event.TAGNAME_APP_TYPENAME;
import static com.studentpal.engine.Event.TAGNAME_NICKNAME;
import static com.studentpal.engine.Event.TAGNAME_OWNERID;
import static com.studentpal.engine.Event.TAGNAME_PHONE_IMSI;
import static com.studentpal.engine.Event.TAGNAME_PHONE_NUM;
import static com.studentpal.engine.Event.TAGNAME_RULE_AUTH_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_ENDTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_STARTTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_VALUE;
import static com.studentpal.engine.Event.TAGNAME_SYS_PRESET;

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
import com.studentpal.model.AccessCategory;
import com.studentpal.model.AppTypeInfo;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.model.exception.STDException;
import com.studentpal.model.rules.AccessRule;
import com.studentpal.model.rules.Recurrence;
import com.studentpal.model.rules.TimeRange;
import com.studentpal.model.user.AdminUser;
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
  private static final String TABLE_NAME_MANAGED_APPTYPES   = "managed_apptypes";
  private static final String TABLE_NAME_MANAGED_DEVICE     = "managed_devices";
  private static final String TABLE_NAME_ADMIN_DEVICE       = "admin_device";

  private static final String COL_NAME_ID                   = "_id";
  private static final String COL_NAME_APPSLIST             = "installedApps";
  private static final String COL_NAME_APPSLIST_VERSION     = "installedAppsListVer";
  private static final String COL_NAME_APPTYPES_VERSION     = "installedAppTypesVer";
  private static final String COL_NAME_CATESLIST_VERSION    = "installedCatesListVer";
  private static final String COL_NAME_MANAGED_APPTYPES     = "managedAppTypes";

  private static final String COL_NAME_IS_ACTIVE            = "isActive";
  //private static final String COL_NAME_APPSLIST_NAME        = "apps_list_version";

  private static final String TIME_LIST_DELIMETER = ";";

  public static final int    INVALID_VERSION = -1;

  /*
   * Member fields
   */
  private static DBaseManager dbManager;

  private ClientEngine  engine = null;
  private SQLiteDatabase mDb;
  private File mDbFile;

  private boolean isAdmin = false;

  /*
   * 获取实例，应当在欢迎界面之后建立实例
   */
  public static DBaseManager getInstance() {
    if (dbManager == null) {
      dbManager = new DBaseManager();
    }
    return dbManager;
  }

  private DBaseManager() {
    initialize();
  }

  /////////////////////////////////////////////////////////////////////////////
  public void saveAccessCategoriesToDB(Set<AccessCategory> catesSet) {
    saveAccessCategoriesToDB(null, catesSet);
  }
  public void saveAccessCategoriesToDB(String targetPhoneNo,
      Set<AccessCategory> catesSet) {
    if (catesSet==null || catesSet.size()==0) return;
    Logger.i(TAG, "enter saveAccessCategoriesToDB()!");

    try {
      mDb = openDB();

      /*
       * 首先清空所有已存的category相关记录
       */
      //clear old records first
      //String sqlStr = "DELETE FROM " + ACCESS_CATEGORY_TABLE_NAME;
      //mDb.execSQL(sqlStr);

      long res = -1;
      if (false == isAdmin) {
        //对于管理端，不能将所有的受控程序记录首先删除
        res = mDb.delete(TABLE_NAME_ACCESS_CATEGORIES, "1", null);
        res = mDb.delete(TABLE_NAME_ACCESS_RULES, "1", null);
        res = mDb.delete(TABLE_NAME_MANAGED_APPS, "1", null);
      } else {
        res = mDb.delete(TABLE_NAME_ACCESS_CATEGORIES,
            TAGNAME_OWNERID+"=?", new String[] {targetPhoneNo});
      }

      ContentValues cv;
      for (AccessCategory aCate : catesSet) {
        if (isAdmin) {
          //对于管理端，将目标受控端的记录删除
          res = mDb.delete(TABLE_NAME_ACCESS_RULES,
              TAGNAME_ACCESS_CATE_ID+"=?",
              new String[] {String.valueOf(aCate.get_id())}
          );
        }

        cv = new ContentValues();
        cv.put(TAGNAME_ACCESS_CATE_ID,    aCate.get_id());
        cv.put(TAGNAME_ACCESS_CATE_NAME,  aCate.get_name());
        if (isAdmin) {
          if (Utils.isEmptyString(targetPhoneNo)) {
            cv.put(TAGNAME_OWNERID,  targetPhoneNo);
          }
          cv.put(COL_NAME_MANAGED_APPTYPES, aCate.getManagedAppTypesIdStr());
        }

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
                TAGNAME_APP_PKGNAME +"=?",
                new String[] {appInfo.getAppPkgname()},
                null, null, null, null);
            if (curApps.moveToFirst()) {
              res = mDb.update(TABLE_NAME_MANAGED_APPS, cv,
                               TAGNAME_APP_PKGNAME + "=?",
                               new String[] {appInfo.getAppPkgname()}
                    );
            } else {
              res = mDb.insert(TABLE_NAME_MANAGED_APPS, null, cv);
            }

            curApps.close();

          }//for
        }
      }

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
      //rebuildTables(mDb);
    } finally {
      mDb.close();
    }
  }

  public List<AccessCategory> loadAccessCategoriesFromClientDB() throws STDException {
    List<AccessCategory> result = new ArrayList<AccessCategory>();
    Logger.i(TAG, "enter loadAccessCategoriesFromClientDB()!");

    try {
      mDb = openDB();

      Cursor curCate = null;
      curCate = mDb.query(TABLE_NAME_ACCESS_CATEGORIES, null, null,
          null, null, null, null);

      while (curCate.moveToNext()) {
        AccessCategory aCate = new AccessCategory();

        int startIdx = 0;
        int cate_id = curCate.getInt(startIdx++);  //0
        aCate.set_id(cate_id);
        aCate.set_name(curCate.getString(startIdx++));  //1

        loadAccessRulesFromDB(aCate, mDb);

        Cursor curApp = mDb.query(TABLE_NAME_MANAGED_APPS, null,
            TAGNAME_ACCESS_CATE_ID + "=?",
            new String[] { String.valueOf(aCate.get_id()) },
            null, null, null, null);
        while (curApp.moveToNext()) {
          String appName = curApp.getString(1);
          String pkgName = curApp.getString(2);
          String className = curApp.getString(3);
          ClientAppInfo appInfo = new ClientAppInfo(appName, pkgName, className);
          aCate.addManagedApp(appInfo);
        }//while curApp
        curApp.close();

        result.add(aCate);

      }//while curCate

      curCate.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
      //rebuildTables(mDb);
    } finally {
      mDb.close();
    }

    return result;
  }

  public Set<AccessCategory> loadAccessCategoriesFromAdminDB(String targetPhoneNo)
      throws STDException {
    Logger.i(TAG, "enter loadAccessCategoriesFromAdminDB()!");
    Set<AccessCategory> result = new HashSet<AccessCategory>();

    try {
      mDb = openDB();

      Cursor curCate = null;
      if (targetPhoneNo != null) {
        curCate = mDb.query(TABLE_NAME_ACCESS_CATEGORIES, null,
            TAGNAME_OWNERID+"=?", new String[] {targetPhoneNo},
            null, null, null, null);
      } else {
        curCate = mDb.query(TABLE_NAME_ACCESS_CATEGORIES, null,
            null, null, null, null, null);
      }

      while (curCate.moveToNext()) {
        AccessCategory aCate = new AccessCategory();

        int startIdx = 0;
        int cate_id = curCate.getInt(startIdx++);  //0
        aCate.set_id(cate_id);
        aCate.set_name(curCate.getString(startIdx++));  //1

        Set<Integer> appTypesIdSet = null;
        String appTypesIdStr = curCate.getString(startIdx++);
        if (appTypesIdStr != null) {
          String[] appTypesIdAry = appTypesIdStr.split("\\"+APP_PKGNAME_DELIMETER);
          if (appTypesIdAry!=null && appTypesIdAry.length>0) {
            appTypesIdSet = new HashSet<Integer>();
            for (int i=0; i<appTypesIdAry.length; i++) {
              try {
                appTypesIdSet.add(Integer.valueOf(appTypesIdAry[i]));
              } catch (NumberFormatException ex) {
                Logger.w(TAG, ex.getMessage());
              }
            }
          }
        }
        aCate.setManagedAppTypesIdSet(appTypesIdSet);

        loadAccessRulesFromDB(aCate, mDb);

        result.add(aCate);

      }//while curCate

      curCate.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
      //rebuildTables(mDb);
    } finally {
      mDb.close();
    }

    return result;
  }

  public void loadAccessRulesFromDB(AccessCategory aCate, SQLiteDatabase mDb)
      throws STDException {
    if (aCate==null || mDb==null) {
      Logger.w(TAG, "Input params should NOT be NULL!");
    }
    Logger.i(TAG, "enter loadAccessRuleFromDB()!");

    Cursor curRule = mDb.query(TABLE_NAME_ACCESS_RULES, null,
        TAGNAME_ACCESS_CATE_ID + "=?",
        new String[] {String.valueOf(aCate.get_id())},
        null, null, null);
    while (curRule.moveToNext()) {
      AccessRule aRule = new AccessRule();

      int startIdx = 1;
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

  }

  public void saveManagedAppsToDB(String targetPhoneNo,
      Set<ClientAppInfo> appsListSet) {
    if (appsListSet==null || appsListSet.size()==0) return;
    Logger.i(TAG, "enter saveManagedAppsToDB()!");

    try {
      mDb = openDB();
      long res = -1;

      ContentValues cv = new ContentValues();
      String appVal;
      for (ClientAppInfo appInfo : appsListSet) {
        cv.clear();
        appVal = appInfo.getAppName();
        cv.put(TAGNAME_APP_NAME,      appVal);
        appVal = appInfo.getAppPkgname();
        cv.put(TAGNAME_APP_PKGNAME,   appVal);
        appVal = appInfo.getAppClassname();
        if (! Utils.isEmptyString(appVal)) {
          cv.put(TAGNAME_APP_CLASSNAME, appVal);
        }
        cv.put(TAGNAME_APP_TYPEID, appInfo.getAppTypeId());
        cv.put(TAGNAME_OWNERID, targetPhoneNo);

        Cursor curApps = mDb.query(TABLE_NAME_MANAGED_APPS,
            null,
            TAGNAME_APP_PKGNAME +"=?", new String[] {appInfo.getAppPkgname()},
            null, null, null);
        if (curApps.moveToFirst()) {
          //record already exists
          res = mDb.update(TABLE_NAME_MANAGED_APPS, cv,
              TAGNAME_APP_PKGNAME +"=?", new String[] {appInfo.getAppPkgname()} );
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

  public Set<ClientAppInfo> loadManagedAppsFromDB(String targetPhoneNo) {
    Set<ClientAppInfo> result = new HashSet<ClientAppInfo>();
    Logger.i(TAG, "enter loadManagedAppsFromDB()!");

    try {
      mDb = openDB();

      Cursor curApps = mDb.query(TABLE_NAME_MANAGED_APPS,
          new String[] {TAGNAME_APP_NAME, TAGNAME_APP_PKGNAME,
            TAGNAME_APP_CLASSNAME, TAGNAME_APP_TYPEID},
          TAGNAME_OWNERID+"=?", new String[] {targetPhoneNo},
          null, null, null, null);
      while (curApps.moveToNext()) {
        String appName    = curApps.getString(0);
        String appPkgName = curApps.getString(1);
        String appClsName = curApps.getString(2);
        ClientAppInfo anAppInfo = new ClientAppInfo(appName, appPkgName, appClsName);
        anAppInfo.setAppTypeId(curApps.getInt(3));

        result.add(anAppInfo);
      }//while

      curApps.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }

    return result;
  }

  public void saveManagedAppTypesToDB(Set<AppTypeInfo> appTypesSet) {
    if (appTypesSet==null || appTypesSet.size()==0) return;
    Logger.i(TAG, "enter saveManagedAppTypesToDB()!");

    try {
      mDb = openDB();
      long res = -1;

      ContentValues cv = new ContentValues();
      String appVal;
      for (AppTypeInfo appTypeInfo : appTypesSet) {
        cv.clear();
        cv.put(COL_NAME_ID,  appTypeInfo.getId());
        appVal = appTypeInfo.getName();
        cv.put(TAGNAME_APP_TYPENAME,  appVal);

        appVal = appTypeInfo.getDesc();
        if (! Utils.isEmptyString(appVal)) {
          cv.put(TAGNAME_APP_TYPEDESC,  appVal);
        } else {
          //FIXME remove default app type desc
          cv.put(TAGNAME_APP_TYPEDESC,  "DEFAULT DESC");
        }

        if (appTypeInfo.isSysPreset()) {
          cv.put(TAGNAME_SYS_PRESET, 1);
        } else {
          cv.put(TAGNAME_SYS_PRESET, 0);
        }

        Cursor curApps = mDb.query(TABLE_NAME_MANAGED_APPTYPES,
            new String[] {COL_NAME_ID},
            TAGNAME_APP_TYPENAME +"=?", new String[] {appTypeInfo.getName()},
            null, null, null);
        if (curApps.moveToFirst()) {
          //record already exists
          res = mDb.update(TABLE_NAME_MANAGED_APPTYPES, cv,
                           TAGNAME_APP_TYPENAME +"=?",
                           new String[] {appTypeInfo.getName()} );
        } else {
          res = mDb.insert(TABLE_NAME_MANAGED_APPTYPES, null, cv);
        }
        curApps.close();
      }

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }
  }

  public Set<AppTypeInfo> loadManagedAppTypesFromDB() {
    Set<AppTypeInfo> result = new HashSet<AppTypeInfo>();
    Logger.i(TAG, "enter loadManagedAppTypesFromDB()!");

    try {
      mDb = openDB();

      Cursor curType = mDb.query(TABLE_NAME_MANAGED_APPTYPES,
          null,
          null, null,
          null, null, null, null);
      while (curType.moveToNext()) {
        AppTypeInfo appType = new AppTypeInfo();
        appType.setId(curType.getInt(0));
        appType.setName(curType.getString(1));
        appType.setDesc(curType.getString(2));
        appType.setSysPreset(curType.getInt(3) == 1);
        result.add(appType);
      }//while
      curType.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }

    return result;
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
      for (ClientUser managedDev : managedDevs) {
        cv = new ContentValues();

        String targetPhoneNo = managedDev.getPhoneNum();
        if (Utils.isEmptyString(targetPhoneNo)) {
          Logger.v("Skipped a ClientUser with NO phone number!");
          continue;
        }

        String targetPhoneImsi = managedDev.getPhoneImsi();
        if (! Utils.isEmptyString(targetPhoneImsi)) {
          cv.put(TAGNAME_PHONE_IMSI, targetPhoneImsi);
        }

        int appsListVer = managedDev.getInstalledAppsListVer();
        if (appsListVer > 0) {
          cv.put(COL_NAME_APPSLIST_VERSION, appsListVer);
        }

        int accCatesVer = managedDev.getInstalledAccessCateVer();
        if (accCatesVer > 0) {
          cv.put(COL_NAME_CATESLIST_VERSION, accCatesVer);
        }

        String appsListStr = managedDev.getInstalledApps();
        if (! Utils.isEmptyString(appsListStr)) {
          cv.put(COL_NAME_APPSLIST, appsListStr);
        }

        //Whether this managed device is ACTIVE or NOT
        cv.put(COL_NAME_IS_ACTIVE, 1);

        Cursor curDev = mDb.query(TABLE_NAME_MANAGED_DEVICE,
            new String[] {COL_NAME_APPSLIST_VERSION},
            TAGNAME_PHONE_NUM +"=?", new String[] {targetPhoneNo},
            //TAGNAME_PHONE_IMSI +"='"+ managedDev.getPhoneImsi() +"'",
            null, null, null);
        if (curDev.moveToFirst()) {
          //record already exists
          res = mDb.update(TABLE_NAME_MANAGED_DEVICE, cv,
              TAGNAME_PHONE_NUM +"=?",
              //TAGNAME_PHONE_IMSI +"='"+ managedDev.getPhoneImsi() +"'",
              new String[] {targetPhoneNo} );
        } else {
          cv.put(TAGNAME_PHONE_NUM, targetPhoneNo);
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

  public void saveAdminUserInfoToDB(AdminUser adminUser) {
    if (adminUser == null) return;
    Logger.i(TAG, "enter saveAdminUserInfoToDB()!");

    try {
      mDb = openDB();
      long res = -1;

      ContentValues cv = new ContentValues();
      String appVal;
      appVal = adminUser.getPhoneNum();
      cv.put(TAGNAME_PHONE_NUM, appVal);

      appVal = adminUser.getPhoneImsi();
      if (! Utils.isEmptyString(appVal)) {
        cv.put(TAGNAME_PHONE_IMSI, appVal);
      }

      appVal = adminUser.getNickName();
      if (! Utils.isEmptyString(appVal)) {
        cv.put(TAGNAME_NICKNAME, appVal);
      } else {
        cv.put(TAGNAME_NICKNAME, adminUser.getPhoneNum());
      }

      int appTypesVer = adminUser.getInstalledAppTypesVer();
      if (appTypesVer > 0) {
        cv.put(COL_NAME_APPTYPES_VERSION, appTypesVer);
      }

      Cursor curApps = mDb.query(TABLE_NAME_ADMIN_DEVICE,
          null,
          TAGNAME_PHONE_NUM +"=?", new String[] {adminUser.getPhoneNum()},
          null, null, null);
      if (curApps.moveToFirst()) {
        //record already exists
        res = mDb.update(TABLE_NAME_ADMIN_DEVICE, cv,
            TAGNAME_PHONE_NUM +"=?",
            new String[] {adminUser.getPhoneNum()});
      } else {
        res = mDb.insert(TABLE_NAME_ADMIN_DEVICE, null, cv);
      }
      curApps.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
      //rebuildTables(mDb);
    } finally {
      mDb.close();
    }
  }

  public void saveCategoryVerToDB(int version, String targetPhoneNo) {
    ClientUser managedDev = new ClientUser(targetPhoneNo, null);
    managedDev.setInstalledAccessCateVer(version);
    saveManagedDevInfoToDB(managedDev);
  }

  public Set<ClientUser> getAllManagedDevs() {
    Logger.i(TAG, "enter getAllManagedDevs()!");
    Set<ClientUser> result = null;

    try {
      mDb = openDB();
      Cursor curDevice = mDb.query(TABLE_NAME_MANAGED_DEVICE,
          null,
          COL_NAME_IS_ACTIVE + "=?",
          new String[] {"1"}, null, null, null);

      int devCnt = curDevice.getCount();
      if (devCnt > 0)  result = new HashSet<ClientUser>(devCnt);
      while (curDevice.moveToNext()) {
        String phoneNum  = curDevice.getString(1);
        String phoneImsi = curDevice.getString(2);
        ClientUser managedDev = new ClientUser(phoneNum, phoneImsi);
        managedDev.setInstalledAppsListVer(curDevice.getInt(3));
        managedDev.setInstalledAccessCateVer(curDevice.getInt(5));

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

  public ClientUser getManagedDev(String targetPhoneNo) {
    if (Utils.isEmptyString(targetPhoneNo)) {
      return null;
    }

    ClientUser result = null;

    try {
      mDb = openDB();

      Cursor curDev = mDb.query(TABLE_NAME_MANAGED_DEVICE,
          new String[] { COL_NAME_APPSLIST_VERSION }, TAGNAME_PHONE_NUM
              + "=? AND " + COL_NAME_IS_ACTIVE + "=1",
          new String[] { targetPhoneNo },
          // TAGNAME_PHONE_IMSI +"='"+ managedDev.getPhoneImsi() +"'",
          null, null, null);

      if (curDev.moveToFirst()) {
        // int startIdx = 0;
        String phoneNum = curDev.getString(0); // col0
        String phoneImsi = curDev.getString(1); // col1
        int appsListVer = curDev.getInt(2); // col2
        int catesListVer = curDev.getInt(4); // col4

        result = new ClientUser(phoneNum, phoneImsi);
        result.setInstalledAppsListVer(appsListVer);
        result.setInstalledAccessCateVer(catesListVer);
      }
      curDev.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }

    return result;
  }

  public int getAppsListVersion(String phone_no) {
    Logger.i(TAG, "enter getAppsListVersion()!");

    int appListVer = 0;
    try {
      mDb = openDB();
      Cursor curDevice = mDb.query(TABLE_NAME_MANAGED_DEVICE,
          new String[] {COL_NAME_APPSLIST_VERSION},
          TAGNAME_PHONE_NUM +"=?", new String[] {phone_no},
          null, null, null);

      if (curDevice.moveToFirst()) {
        //get the value of COL_NAME_APPSLIST_VERSION
        appListVer = curDevice.getInt(0);

//        //检查每个受控的Application记录都存在于表中，否则重新获取记录列表，即返回版本号为0
//        String appListStr = "";
//        appListStr = curDevice.getString(startIdx++);
//        if (! Utils.isEmptyString(appListStr)) {
//          String[] pkgNameAry = appListStr.split("\\"+Event.APP_PKGNAME_DELIMETER);
//          if (pkgNameAry!=null && pkgNameAry.length>0) {
//            for (String pkgName : pkgNameAry) {
//              Cursor curApp = mDb.query(TABLE_NAME_MANAGED_APPS,
//                  null, TAGNAME_APP_PKGNAME +"=?", new String[] {pkgName},
//                  null, null, null);
//              //当某个Application记录不存在于表中
//              if (! curApp.moveToFirst()) {
//                appListVer = 0;
//                curApp.close();
//                break;
//              }
//              curApp.close();
//            }
//          }
//        }

      }
      curDevice.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }

    return appListVer;
  }

  public int getAppTypesListVersion() {
    Logger.i(TAG, "enter getAppTypesListVersion()!");
    int result = INVALID_VERSION;

    try {
      mDb = openDB();
      Cursor curDevice = mDb.query(TABLE_NAME_ADMIN_DEVICE,
          new String[] {COL_NAME_APPTYPES_VERSION}, null,
          null, null, null, null);
      if (curDevice.moveToFirst()) {
        //get the value of column of COL_NAME_APPTYPES_VERSION
        result = curDevice.getInt(0);
      }
      curDevice.close();

    } catch (SQLiteException ex) {
      Logger.w(TAG, ex.toString());
    } finally {
      mDb.close();
    }

    return result;
  }

  public int getAccessCatesListVersion(String phone_no) {
    Logger.i(TAG, "enter getAccessCatesListVersion()!");
    int result = INVALID_VERSION;

    try {
      mDb = openDB();
      Cursor curDevice = mDb.query(TABLE_NAME_MANAGED_DEVICE,
          new String[] {COL_NAME_CATESLIST_VERSION},
          TAGNAME_PHONE_NUM +"=?", new String[] {phone_no},
          null, null, null);

      if (curDevice.moveToFirst()) {
        //get value of the column of COL_NAME_CATESLIST_VERSION
        result = curDevice.getInt(0);
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
      if (mDb!=null && mDb.isOpen()) {
        mDb.close();
      }

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

    final String create_rules_table_sql = new StringBuffer().append(
      "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_ACCESS_RULES).append(
      "(  _id INTEGER PRIMARY KEY AUTOINCREMENT ").append(
      ", " +TAGNAME_RULE_AUTH_TYPE+        " INTEGER").append(
      ", " +TAGNAME_RULE_REPEAT_TYPE+      " INTEGER").append(
      ", " +TAGNAME_RULE_REPEAT_VALUE+     " INTEGER").append(
      ", " +TAGNAME_RULE_REPEAT_STARTTIME+ " TEXT").append(
      ", " +TAGNAME_RULE_REPEAT_ENDTIME+   " TEXT").append(
      ", " +TAGNAME_ACCESS_CATE_ID+        " INTEGER").append(
      ", FOREIGN KEY(" +TAGNAME_ACCESS_CATE_ID+ ") REFERENCES "
          +TABLE_NAME_ACCESS_CATEGORIES+ "("
          +TAGNAME_ACCESS_CATE_ID+ ") ON DELETE CASCADE ").append(
      ");").toString();
    dbase.execSQL(create_rules_table_sql);

    /*
     * Differs between Admin & Client
     */
    if (true == isAdmin) {  //for admin
      final String create_admin_device_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_ADMIN_DEVICE).append(
          "( " +TAGNAME_PHONE_NUM+       " TEXT").append(
          ", " +TAGNAME_PHONE_IMSI+      " TEXT").append(
          ", " +TAGNAME_NICKNAME+        " TEXT").append(  //default to be the phone number
          ", " +COL_NAME_APPTYPES_VERSION+ " INTEGER DEFAULT 0").append(
          ");").toString();
      dbase.execSQL(create_admin_device_table_sql);

      final String create_managed_device_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_MANAGED_DEVICE).append(
          "( _id INTEGER PRIMARY KEY AUTOINCREMENT ").append(
          ", " +TAGNAME_PHONE_NUM+       " TEXT").append(
          ", " +TAGNAME_PHONE_IMSI+      " TEXT").append(
          ", " +COL_NAME_APPSLIST_VERSION+  " INTEGER DEFAULT 0").append(
          ", " +COL_NAME_APPSLIST+          " TEXT DEFAULT NULL").append(
          ", " +COL_NAME_CATESLIST_VERSION+ " INTEGER DEFAULT 0").append(
          ", " +COL_NAME_IS_ACTIVE+         " INTEGER DEFAULT 0").append(
          ");").toString();
      dbase.execSQL(create_managed_device_table_sql);

      final String create_app_types_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_MANAGED_APPTYPES).append(
          "( _id INTEGER PRIMARY KEY ").append(
          ", " +TAGNAME_APP_TYPENAME+       " TEXT").append(
          ", " +TAGNAME_APP_TYPEDESC+       " TEXT").append(
          ", " +TAGNAME_SYS_PRESET+         " INTEGER DEFAULT 0").append(
          /*
           * 无需使用OWNERID字段，owner即是admin phone自身
           */
          //", " +TAGNAME_OWNERID+            " INTEGER").append(
          //", FOREIGN KEY(" +TAGNAME_OWNERID+ ") REFERENCES " +TABLE_NAME_MANAGED_DEVICE+ "(" +TAGNAME_PHONE_NUM+ ")").append(
          ");").toString();
      dbase.execSQL(create_app_types_table_sql);

      /**
       * 与受控客户端同名的表名
       */
      //本地存储受控手机上安装的程序列表
      final String create_applications_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_MANAGED_APPS).append(
          "( _id INTEGER PRIMARY KEY AUTOINCREMENT ").append(
          ", " +TAGNAME_APP_NAME+       " TEXT").append(
          ", " +TAGNAME_APP_PKGNAME+    " TEXT").append(
          ", " +TAGNAME_APP_CLASSNAME+  " TEXT").append(
          /* 相比client多出如下字段  */
          ", " +TAGNAME_APP_TYPEID+     " INTEGER").append(
          ", " +TAGNAME_OWNERID+        " INTEGER").append(
          //", FOREIGN KEY(" +TAGNAME_APP_TYPEID+ ") REFERENCES " +TABLE_NAME_MANAGED_APPTYPES+ "( _id )").append(
          ", FOREIGN KEY(" +TAGNAME_OWNERID+ ") REFERENCES " +TABLE_NAME_MANAGED_DEVICE+ "(" +TAGNAME_PHONE_NUM+ ")").append(
          ");").toString();
      dbase.execSQL(create_applications_table_sql);

      final String create_catory_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_ACCESS_CATEGORIES).append(
          "( " +TAGNAME_ACCESS_CATE_ID+    " INTEGER PRIMARY KEY ").append(
          ", " +TAGNAME_ACCESS_CATE_NAME+  " TEXT").append(
          /* 相比client多出如下字段 */
          ", " +COL_NAME_MANAGED_APPTYPES+ " TEXT").append(
          ", " +TAGNAME_OWNERID+           " INTEGER").append(
          ", FOREIGN KEY(" +TAGNAME_OWNERID+ ") REFERENCES " +TABLE_NAME_MANAGED_DEVICE+ "(" +TAGNAME_PHONE_NUM+ ")").append(
          ");").toString();
      dbase.execSQL(create_catory_table_sql);

    } else {  //for client
      final String create_applications_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_MANAGED_APPS).append(
          "( _id INTEGER PRIMARY KEY AUTOINCREMENT ").append(
          ", " +TAGNAME_APP_NAME+       " TEXT").append(
          ", " +TAGNAME_APP_PKGNAME+    " TEXT").append(
          ", " +TAGNAME_APP_CLASSNAME+  " TEXT").append(
          /* 相比admin多出如下字段  */
          ", " +TAGNAME_ACCESS_CATE_ID+ " INTEGER").append(
          ", FOREIGN KEY(" +TAGNAME_ACCESS_CATE_ID+ ") REFERENCES "
              +TABLE_NAME_ACCESS_CATEGORIES+ "("
              +TAGNAME_ACCESS_CATE_ID+ ")").append(
          ");").toString();
      dbase.execSQL(create_applications_table_sql);

      final String create_catory_table_sql = new StringBuffer().append(
          "CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME_ACCESS_CATEGORIES).append(
          "( " +TAGNAME_ACCESS_CATE_ID+   " INTEGER PRIMARY KEY ").append(
          ", " +TAGNAME_ACCESS_CATE_NAME+ " TEXT").append(
          ");").toString();
        dbase.execSQL(create_catory_table_sql);
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
      table_name_ary.add(TABLE_NAME_ADMIN_DEVICE);
      table_name_ary.add(TABLE_NAME_MANAGED_DEVICE);
      table_name_ary.add(TABLE_NAME_MANAGED_APPTYPES);
    }

    for (int i=0; i<table_name_ary.size(); i++) {
      String drop_table_sql = "DROP TABLE " +table_name_ary.get(i)+ " ;";
      dbase.execSQL(drop_table_sql);
    }

    createTables(dbase);
  }
}

