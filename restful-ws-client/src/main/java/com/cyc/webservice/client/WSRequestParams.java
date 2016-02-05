/* $Id: WSRequestParams.java 130517 2010-04-02 01:42:40Z tbrussea $
 *
 * Copyright (c) 2010 Cycorp, Inc.  All rights reserved.
 * This software is the proprietary information of Cycorp, Inc.
 * Use is subject to license terms.
 */
package com.cyc.webservice.client;

//// Internal Imports
//// External Imports
/** 
 * <P>WSRequestParams is designed to...
 *
 * <P>Copyright (c) 2010 Cycorp, Inc.  All rights reserved.
 * <BR>This software is the proprietary information of Cycorp, Inc.
 * <P>Use is subject to license terms.
 *
 * Created on : Apr 1, 2010, 8:03:55 PM
 * Author     : tbrussea
 * @version $Id: WSRequestParams.java 130517 2010-04-02 01:42:40Z tbrussea $
 */
public class WSRequestParams {

  //// Constructors

  /** Creates a new instance of WSRequestParams. */
  public WSRequestParams(Object extraData, String requestType, Object... params) {
    if ((requestType == null) || (params == null)) {
      throw new IllegalArgumentException();
    }
    this.requestType = requestType;
    this.params = params;
    this.extraData = extraData;
  }

  //// Public Area

  public String getRequestType() {
    return requestType;
  }

  public Object[] getParams() {
    return params;
  }

  public Object getExtraData() {
    return extraData;
  }

  @Override
  public String toString() {
    return "";
  }

  @Override
  public int hashCode() {
    int code = (extraData == null) ? 0 : 0xFFFFFFFF;
    code |= requestType.hashCode();
    for (Object param : params) {
      code |= (param == null) ? 0 : param.hashCode();
    }
    return code;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) { // fast fail
      return false;
    }
    if (o == this) { // fast success
      return true;
    }
    if (!(o instanceof WSRequestParams)) {
      return false;
    }
    WSRequestParams req = (WSRequestParams) o;
    if (extraData != req.extraData) {
      if ((extraData == null) || (!extraData.equals(req.extraData))) {
        return false;
      }
    }
    if (!requestType.equals(req.requestType)) {
      return false;
    }
    int i = 0;
    for (Object param : params) {
      Object otherParam = req.params[i++];
      if (param != otherParam) {
        if ((param == null) || (!param.equals(otherParam))) {
          return false;
        }
      }
    }
    return true;
  }

  //// Protected Area

  //// Private Area

  //// Internal Rep

  private String requestType;
  private Object[] params;
  private Object extraData;

  //// Main

}
