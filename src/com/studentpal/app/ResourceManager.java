package com.studentpal.app;

public class ResourceManager {
  // should be identical to value of "package" in Manifest
  public static final String APPLICATION_PKG_NAME = "com.studentpal";
  public static final String DAEMON_SVC_PKG_NAME = "com.studentpaldaemon";
  public static final String ACTIVITY_NAME_MANAGEAPPS = "com.android.settings.ManageApplications";
  public static final String ACTIVITY_NAME_APPSDETAILS = "com.android.settings.InstalledAppDetails";

  public static final String target_svr_ip1 = "10.60.4.79";
  public static final String target_svr_ip  = "192.168.11.252";
  public static final String target_svr_ip2 = "192.168.10.101";

  public static final String RES_STR_OK = "\u786e\u5b9a";  // 确定
  public static final String RES_STR_CANCEL = "\u53d6\u6d88";  // 取消
  public static final String RES_STR_WARNING = "\u8b66\u544a";  //警告
  public static final String RES_STR_ERROR = "\u9519\u8bef";  //错误
  public static final String RES_STR_FAIL = "\u5931\u8d25";  //失败
  public static final String RES_STR_TIMEOUT = "";  //超时


  public static final String RES_STR_LOGIN = "\u767b\u5f55";  //登录
  public static final String RES_STR_LOGIN_NAME = "\u767b\u5f55\u540d\u79f0";  //登录名称
  public static final String RES_STR_LOGIN_PWD  = "\u767b\u5f55\u5bc6\u7801";  //登录密码
  public static final String RES_STR_TIME = "\u65f6\u95f4"; // 时间
  public static final String RES_STR_START_TIME = "\u5f00\u59cb" + RES_STR_TIME; // 开始时间
  public static final String RES_STR_END_TIME = "\u7ed3\u675f" + RES_STR_TIME; // 结束时间
  public static final String RES_STR_SENDREQUEST = "\u53d1\u9001\u8bf7\u6c42"; // 发送请求

  public static final String RES_STR_NETWORK_ERROR = "\u7f51\u7edc\u9519\u8bef";  //网络错误
  public static final String RES_STR_QUITAPP = "\u9000\u51fa\u7a0b\u5e8f\uff1f";  //退出程序？

  public static final String RES_STR_OPERATION_DENIED =  // 很抱歉，您的操作被管理员所禁止！
      "\u5f88\u62b1\u6b49\uff0c\u60a8\u7684\u64cd\u4f5c\u88ab\u7ba1\u7406\u5458\u6240\u7981\u6b62\uff01";

  public static final String RES_STR_DEVICE_ADMIN_DEACTIVATED_WARNING  //若禁用此选项，您可能丧失对目标终端的管理。确定此操作吗？
    = "\u82e5\u7981\u7528\u6b64\u9009\u9879\uff0c\u60a8\u53ef\u80fd\u4e27\u5931"
    + "\u5bf9\u76ee\u6807\u7ec8\u7aef\u7684\u7ba1\u7406\u3002\u786e\u5b9a\u6b64"
    + "\u64cd\u4f5c\u5417\uff1f";

  public static final String RES_STR_NETWORK_SETUP_FAIL  //网络连接建立失败，请重试！
    = "\u7f51\u7edc\u8fde\u63a5\u5efa\u7acb\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5\uff01";

  public static final String RES_STR_LOGINNAME_PWD_CANNOT_EMPTY  //登录名或者密码不能为空！
    = "\u767b\u5f55\u540d\u6216\u8005\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a\uff01";

  public static final String RES_STR_LOGINING_AND_WAIT  //登录中，请稍候！
    = "\u767b\u5f55\u4e2d\uff0c\u8bf7\u7a0d\u5019\uff01";

  public static final String RES_STR_LOGINNAME_PWD_ERROR  //登录名称不存在或者登录密码错误！
    = RES_STR_LOGIN_NAME +"\u4e0d\u5b58\u5728\u6216\u8005"+ RES_STR_LOGIN_PWD + RES_STR_ERROR + "\uff01";


}
