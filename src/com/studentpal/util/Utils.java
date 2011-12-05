package com.studentpal.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

  public static boolean isValidPhoneNumber(final String phoneNum) {
    return (phoneNum != null && phoneNum.trim().length() == 11);
  }

  //International Mobile Equipment Identification Number
  public static boolean isValidPhoneIMEI(final String phoneIMEI) {
    return (phoneIMEI != null && phoneIMEI.trim().length() == 15);
  }

  //International Mobile Subscriber Identification Number
  public static boolean isValidPhoneIMSI(final String phoneIMSI) {
    return (phoneIMSI != null && phoneIMSI.trim().length() == 15);
  }

  private static final String IP_PATTERN =
      "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
      "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
  public static boolean isValidIpv4Address(final String ipaddr) {
    if (ipaddr == null) return false;

    Pattern pattern = Pattern.compile(IP_PATTERN);
    Matcher matcher = pattern.matcher(ipaddr);
    return matcher.matches();
  }

  public static boolean isEmptyString(String str) {
    return (str==null || str.trim().length()==0);
  }

  public static final int byteArrayToInt(byte[] b) {
    return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8)
        + (b[3] & 0xFF);
  }

  public static final byte[] intToByteArray(int value) {
    return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
        (byte) (value >>> 8), (byte) value };
  }

  public static void sleep(int time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static String getPackageName(Class claz) {
    String pkgName = claz.getName();
    if (pkgName.indexOf('.') != -1) {
      pkgName = pkgName.substring(0, pkgName.lastIndexOf('.')+1);
    } else {
      pkgName = "";
    }

    return pkgName;
  }

}
