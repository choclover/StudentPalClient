package com.studentpal.engine.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.studentpal.app.handler.MessageHandler;
import com.studentpal.engine.ClientEngine;
import com.studentpal.engine.Event;
import com.studentpal.model.exception.STDException;


public abstract class Request /*extends Message*/ {
  /*
   * Constants
   */

  /*
   * Field Members
   */
  protected boolean bIncoming = true;
  protected boolean bOutputContentReady = false;

  protected Object inputArguments = null;

  protected String outputContentStr = null;

  protected int req_seq = Event.MSG_ID_INVALID;
  protected boolean isAdminReq = false;
  protected String targetPhoneNo;

  /*
   * Methods
   */
  public static boolean isEqualRequestType(String reqType1, String reqType2) {
    return reqType1!=null && reqType2!=null && reqType1.equals(reqType2);
  }

  public void setTargetPhoneNo(String targetPhoneNo) {
    this.targetPhoneNo = targetPhoneNo;
  }

  /////////////////////////////////////////////////////////////////////////////
  // public void execute(MessageHandler msgHandler) {
  // }

  public abstract void execute() throws STDException;

  public String getName() {
    return Event.TASKNAME_Generic;
  }

  public boolean isIncomingReq() {
    return bIncoming;
  }

  public boolean isOutgoingReq() {
    return ! bIncoming;
  }

  public boolean isOutputContentReady() {
    return bOutputContentReady;
  }

  public String getOutputContent() {
    return outputContentStr;
  }

  public void setOutputContent(String content) {
    this.bIncoming = false;
    outputContentStr = content;
    if (content!=null && content.trim().length() > 0) {
      this.bOutputContentReady = true;
    } else {
      this.bOutputContentReady = false;
    }
  }

  public Object getInputArguments() {
    return inputArguments;
  }

  public void setInputArguments(Object args) {
    inputArguments = args;
  }

  public JSONObject generateGenericRequestHeader(String cmd_type, JSONObject argsObj)
      throws JSONException {
    JSONObject reqObj = new JSONObject();
    reqObj.put(Event.TAGNAME_MSG_TYPE, Event.MESSAGE_HEADER_REQ);
    reqObj.put(Event.TAGNAME_CMD_TYPE, cmd_type);
    reqObj.put(Event.TAGNAME_MSG_ID, req_seq);
    if (argsObj.length() > 0) {
      reqObj.put(Event.TAGNAME_ARGUMENTS, argsObj);
    }

    return reqObj;
  }

  public JSONObject generateGenericReplyHeader(String cmd_type)
      throws JSONException {
    JSONObject header = new JSONObject();
    header.put(Event.TAGNAME_MSG_TYPE, Event.MESSAGE_HEADER_ACK);
    header.put(Event.TAGNAME_MSG_ID, req_seq);
    header.put(Event.TAGNAME_CMD_TYPE, cmd_type);

    return header;
  }

  public void setRequestSeq(int seq) {
    this.req_seq = seq;
  }

  public void setIsAdminReq(boolean isAdminReq) {
    this.isAdminReq = isAdminReq;
  }

}