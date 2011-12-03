package com.studentpal.model.user;

public abstract class User {

  public boolean isEqualValue(String val1, String val2) {
    return (val1!=null && val2!=null && val1.equals(val2));
  }
}



