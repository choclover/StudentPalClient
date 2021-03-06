package com.studentpal.model.rules;

import com.studentpal.R;
import com.studentpal.app.ResourceManager;
import com.studentpal.model.exception.STDException;
import com.studentpal.util.ActivityUtil;
import com.studentpal.util.logger.Logger;

public class TimeRange {
  private static final String TAG = "@@ TimeRange";

  public static final int TIME_TYPE_START = 0x1;
  public static final int TIME_TYPE_END = 0x2;

  private ScheduledTime startTime, endTime;

  public TimeRange() {
  }

  public TimeRange(int startHour, int startMin, int endHour, int endMin)
    throws STDException {
    setStartTime(startHour, startMin);
    setEndTime(endHour, endMin);
  }

  //@Deprecated
  private void setStartTime(int hour, int minute) throws STDException {
    if (startTime == null) {
      startTime = new ScheduledTime(ResourceManager.RES_STR_START_TIME, true);
    }
    _setTime(startTime, hour, minute);
  }

  //@Deprecated
  private void setEndTime(int hour, int minute) throws STDException {
    if (endTime == null) {
      endTime = new ScheduledTime(ResourceManager.RES_STR_END_TIME, false);
    }
    _setTime(endTime, hour, minute);
  }

  public void setTime(int timeType, int hour, int minute) throws STDException {
    switch (timeType) {
    case TIME_TYPE_START:
      setStartTime(hour, minute);
      break;

    case TIME_TYPE_END:
      setEndTime(hour, minute);
      break;

    default:
      Logger.d(TAG, "Invalid Time type of "+timeType);
      break;
    }
  }

  public void setTime(int timeType, String timeStr) throws STDException {
    try {
      int idx = timeStr.indexOf(':');
      if (idx != -1) {
        int hour = Integer.parseInt(timeStr.substring(0, idx));
        int min  = Integer.parseInt(timeStr.substring(idx+1));
        setTime(timeType, hour, min);

      } else {
        throw new STDException("Invalid time range format!");
      }

    } catch (Exception ex) {
      Logger.w(TAG, ex.toString());
      throw new STDException(ex.toString());
    }
  }

  public ScheduledTime getStartTime() {
    return startTime;
  }

  public ScheduledTime getEndTime() {
    return endTime;
  }

  public boolean isValid() {
    boolean result = true;
    if (startTime == null || endTime == null
        || startTime.isAfter(endTime._hour, endTime._minute)) {
      result = false;
    }
    return result;
  }

  // //////////////////////////////////////////////////////////////////////////
  private void _setTime(ScheduledTime time, int hour, int minute)
      throws STDException {
    if ((hour > 23 && hour < 0) || (minute > 59 && minute < 0)) {
      String msg = "Invalid input time for " + time.getName() + "on HOUR: "
          + hour + "\tMINUTE: " + minute;
      Logger.w(TAG, msg);
      throw new STDException(msg);
    }
    time._hour = hour;
    time._minute = minute;
  }

  /*
   * Inner class
   */
  public class ScheduledTime {
    public int _hour;
    public int _minute;

    String _name = "";
    boolean isStarttime;

    public ScheduledTime(String name, boolean isStarttime) {
      this._name = name;
      this.isStarttime = isStarttime;
    }

    public String getName() {
      return _name;
    }

    public int toIntValue() {
      return (_hour * 100 + _minute);
    }

    public boolean isBeforeEqualTo(int hour, int minute) {
      boolean result = false;
      if (_hour < hour || (_hour == hour && _minute <= minute)) {
        result = true;
      }
      return result;
    }

    public boolean isAfter(int hour, int minute) {
      boolean result = false;
      if (_hour > hour || (_hour == hour && _minute > minute)) {
        result = true;
      }
      return result;
    }

    public boolean isStartTime() {
      return isStarttime;
    }

    /*
     * 计算距离指定时间点的seconds数目。
     * = 0: 本scheduled time正好等于指定的特定时间点
     * > 0: 本scheduled time在指定的指定时间点之前(尚未到达指定时间点)
     * < 0: 本scheduled time在指定的指定时间点之后(已经超过指定时间点)
     */
    public int calcSecondsToSpecificTime(int hour, int minute, int second) {
      int seconds = ((hour - _hour) * 60 + (minute - _minute)) * 60 + second;
      return seconds;
    }

    public int calcSecondsToSpecificTime(ScheduledTime time) {
      return calcSecondsToSpecificTime(time._hour, time._minute, 0);
    }

    public String toString() {
      return "" +_hour+ ":" +_minute;
    }

  }
}
