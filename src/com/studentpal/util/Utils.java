package com.studentpal.util;

import static com.studentpal.engine.Event.ACCESS_TYPE_DENIED;
import static com.studentpal.engine.Event.ACCESS_TYPE_PERMITTED;
import static com.studentpal.engine.Event.RECUR_TYPE_DAILY;
import static com.studentpal.engine.Event.RECUR_TYPE_MONTHLY;
import static com.studentpal.engine.Event.RECUR_TYPE_WEEKLY;
import static com.studentpal.engine.Event.RECUR_TYPE_YEARLY;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATEGORY;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_ID;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_CATE_NAME;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_RULE;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_RULES;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_TIMERANGE;
import static com.studentpal.engine.Event.TAGNAME_ACCESS_TIMERANGES;
import static com.studentpal.engine.Event.TAGNAME_APP;
import static com.studentpal.engine.Event.TAGNAME_APPLICATION_TYPE;
import static com.studentpal.engine.Event.TAGNAME_APPLICATION_TYPES;
import static com.studentpal.engine.Event.TAGNAME_APP_CLASSNAME;
import static com.studentpal.engine.Event.TAGNAME_APP_NAME;
import static com.studentpal.engine.Event.TAGNAME_APP_PKGNAME;
import static com.studentpal.engine.Event.TAGNAME_APP_TYPEID;
import static com.studentpal.engine.Event.TAGNAME_APP_TYPENAME;
import static com.studentpal.engine.Event.TAGNAME_RULE_AUTH_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_ENDTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_STARTTIME;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_TYPE;
import static com.studentpal.engine.Event.TAGNAME_RULE_REPEAT_VALUE;
import static com.studentpal.engine.Event.TXT_ACCESS_TYPE_DENIED;
import static com.studentpal.engine.Event.TXT_ACCESS_TYPE_PERMITTED;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_DAILY;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_MONTHLY;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_WEEKLY;
import static com.studentpal.engine.Event.TXT_RECUR_TYPE_YEARLY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.studentpal.model.AccessCategory;
import com.studentpal.model.AppTypeInfo;
import com.studentpal.model.ClientAppInfo;
import com.studentpal.util.logger.Logger;


public class Utils {
  private static final String TAG = "@@ Client.Utils";

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

  public static String getPackageName(Class<?> claz) {
    String pkgName = claz.getName();
    if (pkgName.indexOf('.') != -1) {
      pkgName = pkgName.substring(0, pkgName.lastIndexOf('.')+1);
    } else {
      pkgName = "";
    }

    return pkgName;
  }

  public static String truncateLongString(String oriStr, int limit) {
    String result = oriStr;
    if (oriStr.length() > limit) {
      result = oriStr.substring(0, limit) + "  ......";
    }
    return result;
  }

  public static String toMd5(byte[] bytes) {
    try {
      MessageDigest algorithm = MessageDigest.getInstance("MD5");
      algorithm.reset();
      algorithm.update(bytes);
      return toHexString(algorithm.digest(), "");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static void loadAppTypesDefFromFile(InputStream is,
      Set<AppTypeInfo> appTypesAry) {
    if (is == null) return;

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder dbd = dbf.newDocumentBuilder();
      Document doc = dbd.parse(is);
      Element rootElem = doc.getDocumentElement();

      NodeList appsTypeList = rootElem.getElementsByTagName(TAGNAME_APPLICATION_TYPE);
      if (appsTypeList!=null && appsTypeList.getLength()>0) {
        for (int i=0; i<appsTypeList.getLength(); i++) {
          Element appTypeElem = (Element)appsTypeList.item(i);

          AppTypeInfo appTypeObj = new AppTypeInfo();
          NamedNodeMap attrs = appTypeElem.getAttributes();
          if (attrs != null) {
            Node typeIdNode = attrs.getNamedItem(TAGNAME_APP_TYPEID);
            if (typeIdNode!=null
                && false==isEmptyString(typeIdNode.getNodeValue())) {
              appTypeObj.setId(Integer.valueOf(typeIdNode.getNodeValue()));
            }

            Node typeNameNode = attrs.getNamedItem(TAGNAME_APP_TYPENAME);
            if (typeNameNode != null) {
              appTypeObj.setName(typeNameNode.getNodeValue());
            } else {
              continue;
            }

            if (appTypesAry != null) appTypesAry.add(appTypeObj);
          }
        }
      }//appsTypeList

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void loadCateDefFromFile(InputStream is,
      Set<AccessCategory> catesSet, Set<ClientAppInfo> appsSet) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder dbd = dbf.newDocumentBuilder();
      Document doc = dbd.parse(is);
      Element rootElem = doc.getDocumentElement();

      JSONArray jsonCatesAry = new JSONArray();
      JSONArray jsonAppsAry = new JSONArray();

      NodeList catesNodeList = rootElem.getElementsByTagName(TAGNAME_ACCESS_CATEGORY);
      if (catesNodeList!=null && catesNodeList.getLength()>0) {
        for (int i=0; i<catesNodeList.getLength(); i++) {
          Element cateElem = (Element)catesNodeList.item(i);

          JSONArray rulesAry = new JSONArray();
          NodeList rulesList = cateElem.getElementsByTagName(TAGNAME_ACCESS_RULE);
          for (int k=0; k<rulesList.getLength(); k++) {
            Element ruleElem = (Element)rulesList.item(k);

            JSONObject aRuleObj = new JSONObject();
            NamedNodeMap attrs = ruleElem.getAttributes();
            if (attrs != null) {
              String attrVal = attrs.getNamedItem(TAGNAME_RULE_AUTH_TYPE).getNodeValue();
              if (attrVal.equalsIgnoreCase(TXT_ACCESS_TYPE_DENIED)) {
                aRuleObj.put(TAGNAME_RULE_AUTH_TYPE, ACCESS_TYPE_DENIED);
              } else if (attrVal.equalsIgnoreCase(TXT_ACCESS_TYPE_PERMITTED)) {
                aRuleObj.put(TAGNAME_RULE_AUTH_TYPE, ACCESS_TYPE_PERMITTED);
              }

              attrVal = attrs.getNamedItem(TAGNAME_RULE_REPEAT_TYPE).getNodeValue();
              if (attrVal.equalsIgnoreCase(TXT_RECUR_TYPE_DAILY)) {
                aRuleObj.put(TAGNAME_RULE_REPEAT_TYPE, RECUR_TYPE_DAILY);

                String repeatStr = attrs.getNamedItem(TAGNAME_RULE_REPEAT_VALUE).getNodeValue();
                aRuleObj.put(TAGNAME_RULE_REPEAT_VALUE, 0);

              } else if (attrVal.equalsIgnoreCase(TXT_RECUR_TYPE_WEEKLY)) {
                aRuleObj.put(TAGNAME_RULE_REPEAT_TYPE, RECUR_TYPE_WEEKLY);

                int repeatVal = 0;
                String repeatStr = attrs.getNamedItem(TAGNAME_RULE_REPEAT_VALUE).getNodeValue();
                StringTokenizer repeatTokens = new StringTokenizer(repeatStr, ",");
                while (repeatTokens.hasMoreTokens()) {
                  repeatStr = repeatTokens.nextToken();
                  int repeatDay = 0;
                  if (repeatStr.equalsIgnoreCase("SUNDAY")) {
                    repeatDay = Calendar.SUNDAY;
                  } else if (repeatStr.equalsIgnoreCase("MONDAY")) {
                    repeatDay = Calendar.MONDAY;
                  } else if (repeatStr.equalsIgnoreCase("TUESDAY")) {
                    repeatDay = Calendar.TUESDAY;
                  } else if (repeatStr.equalsIgnoreCase("WEDNESDAY")) {
                    repeatDay = Calendar.WEDNESDAY;
                  } else if (repeatStr.equalsIgnoreCase("THURSDAY")) {
                    repeatDay = Calendar.THURSDAY;
                  } else if (repeatStr.equalsIgnoreCase("FRIDAY")) {
                    repeatDay = Calendar.FRIDAY;
                  } else if (repeatStr.equalsIgnoreCase("SATURDAY")) {
                    repeatDay = Calendar.SATURDAY;
                  }

                  if (repeatDay > 0) repeatVal |= (1 << (repeatDay-1) );
                }
                aRuleObj.put(TAGNAME_RULE_REPEAT_VALUE, repeatVal);

              } if (attrVal.equalsIgnoreCase(TXT_RECUR_TYPE_MONTHLY)) {
                aRuleObj.put(TAGNAME_RULE_REPEAT_TYPE, RECUR_TYPE_MONTHLY);
              } if (attrVal.equalsIgnoreCase(TXT_RECUR_TYPE_YEARLY)) {
                aRuleObj.put(TAGNAME_RULE_REPEAT_TYPE, RECUR_TYPE_YEARLY);
              }

              JSONArray trsAry = new JSONArray();
              NodeList trList = ruleElem.getElementsByTagName(TAGNAME_ACCESS_TIMERANGE);
              for (int m=0; m<trList.getLength(); m++) {
                Element trElem = (Element)trList.item(m);
                attrs = trElem.getAttributes();
                if (attrs != null) {
                  JSONObject aTrObj = new JSONObject();
                  String trVal = attrs.getNamedItem(TAGNAME_RULE_REPEAT_STARTTIME).getNodeValue();
                  aTrObj.put(TAGNAME_RULE_REPEAT_STARTTIME, trVal);
                  trVal = attrs.getNamedItem(TAGNAME_RULE_REPEAT_ENDTIME).getNodeValue();
                  aTrObj.put(TAGNAME_RULE_REPEAT_ENDTIME, trVal);

                  trsAry.put(aTrObj);
                }
              }
              aRuleObj.put(TAGNAME_ACCESS_TIMERANGES, trsAry);

            }//ruleElem' attrs

            rulesAry.put(aRuleObj);

          }//rulesList

          JSONObject jsonCateObj = new JSONObject();
          NamedNodeMap cateAttrs = cateElem.getAttributes();
          if (cateAttrs != null) {
            jsonCateObj.put(TAGNAME_ACCESS_CATE_ID,
                Integer.valueOf(cateAttrs.getNamedItem(
                    TAGNAME_ACCESS_CATE_ID).getNodeValue()));
            jsonCateObj.put(TAGNAME_ACCESS_CATE_NAME,
                cateAttrs.getNamedItem(TAGNAME_ACCESS_CATE_NAME).getNodeValue());

            //load cate ids string
            jsonCateObj.put(TAGNAME_APPLICATION_TYPES,
                cateAttrs.getNamedItem(TAGNAME_APPLICATION_TYPES).getNodeValue());
          }
          jsonCateObj.put(TAGNAME_ACCESS_RULES, rulesAry);

          jsonCatesAry.put(jsonCateObj);
          //translate from JSONObject
          catesSet.add(new AccessCategory(jsonCateObj));
        }
      }//catesList

      NodeList appsList = rootElem.getElementsByTagName(TAGNAME_APP);
      if (appsList!=null && appsList.getLength()>0) {
        for (int i=0; i<appsList.getLength(); i++) {
          Element appElem = (Element)appsList.item(i);

          JSONObject anAppObj = new JSONObject();
          NamedNodeMap attrs = appElem.getAttributes();
          if (attrs != null) {
            anAppObj.put(TAGNAME_APP_NAME, attrs.getNamedItem(TAGNAME_APP_NAME).getNodeValue());
            anAppObj.put(TAGNAME_APP_PKGNAME, attrs.getNamedItem(TAGNAME_APP_PKGNAME).getNodeValue());
            anAppObj.put(TAGNAME_APP_CLASSNAME, attrs.getNamedItem(TAGNAME_APP_CLASSNAME).getNodeValue());
            anAppObj.put(TAGNAME_ACCESS_CATE_ID, attrs.getNamedItem(TAGNAME_ACCESS_CATE_ID).getNodeValue());

            if (jsonAppsAry != null) jsonAppsAry.put(anAppObj);
            //translate from JSONObject
            if (appsSet != null) appsSet.add(new ClientAppInfo(anAppObj));
          }
        }
      }//appsList

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }
  ////////////////////////////////////////////////////////////////////////////
  private static String toHexString(byte[] bytes, String separator) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : bytes) {
      hexString.append(String.format("%02x", 0xFF & b))
               .append(separator);
    }
    return hexString.toString();
  }

}
