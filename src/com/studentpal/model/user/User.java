package com.studentpal.model.user;

public abstract class User {
  protected String phoneNum;
  protected String phoneImsi;
  protected String phoneImei;

  public String getPhoneNum() {
    return phoneNum;
  }

  public String getPhoneImsi() {
    return phoneImsi;
  }

  public String getPhoneImei() {
    return phoneImei;
  }

  public void setPhoneNum(String phoneNum) {
    this.phoneNum = phoneNum;
  }

  public void setPhoneImsi(String phoneImsi) {
    this.phoneImsi = phoneImsi;
  }

  public void setPhoneImei(String phoneImei) {
    this.phoneImei = phoneImei;
  }

  public boolean isEqualValue(String val1, String val2) {
    return (val1!=null && val2!=null && val1.equals(val2));
  }
}



