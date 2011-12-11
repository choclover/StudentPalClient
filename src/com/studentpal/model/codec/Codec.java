package com.studentpal.model.codec;

import com.studentpal.util.Utils;

public class Codec {
  public static final int MSG_LENGTH_HEADER_SIZE = 4;
  
  public static byte[] encode(byte[] sour) {
    byte[] dest = new byte[sour.length + MSG_LENGTH_HEADER_SIZE];
    byte[] lenBytes = Utils.intToByteArray(sour.length);

    // 将前4位设置成数据体的字节长度
    System.arraycopy(lenBytes, 0, dest, 0, MSG_LENGTH_HEADER_SIZE);   
    // message content
    System.arraycopy(sour, 0, dest, MSG_LENGTH_HEADER_SIZE, sour.length);    
    
    return dest;
  }
  
  public static byte[] decode(byte[] src) {
    
    return null;
  }
}
