//: "The contents of this file are subject to the Mozilla Public License
//: Version 1.1 (the "License"); you may not use this file except in
//: compliance with the License. You may obtain a copy of the License at
//: http://www.mozilla.org/MPL/
//:
//: Software distributed under the License is distributed on an "AS IS"
//: basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//: License for the specific language governing rights and limitations
//: under the License.
//:
//: The Original Code is Guanxi (http://www.guanxi.uhi.ac.uk).
//:
//: The Initial Developer of the Original Code is Alistair Young alistair@codebrane.com
//: All Rights Reserved.
//:

package org.guanxi.idp.farm.authcookiehandlers;

import org.guanxi.idp.farm.Loggable;

import javax.servlet.http.Cookie;

/**
 * <font size=5><b></b></font>
 *
 * @author Alistair Young alistair@smo.uhi.ac.uk
 */
public interface AuthCookieHandler extends Loggable {
  public static final String COOKIE_NAME_DELIMITER = " ";
  public static final String NO_ERROR = "no_error";

  public void setCookieHandlerConfig(String cookieHandlerConfig);
  public String getCookieHandlerConfig();

  public void init();

  public boolean handlesCookie(String cookieName);

  public boolean authenticate(Cookie cookie);

  public String getCookieNames();

  public String getAuthenticatedID();

  public String getErrorMessage();
}
