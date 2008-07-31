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

package org.guanxi.idp.farm.authenticators;

import org.guanxi.common.GuanxiPrincipal;

/**
 * <font size=5><b></b></font>
 *
 * @author Alistair Young alistair@smo.uhi.ac.uk
 */
public interface Authenticator {
  /** Default status for use by implementations */
  public static final String NO_ERROR = "no_error";

  public void init();

  public void setAuthenticatorConfig(String authenticatorConfig);
  public String getAuthenticatorConfig();

  /**
   * Authenticates a Principal
   *
   * @param principal An object to store user specific details after authentication
   * @param username The Principal's username
   * @param password The Principal's password
   * @return An implementing class should return TRUE if authentication succeeded, otherwise FALSE.
   * If authentication fails, the class should set it's errorMessage.
   */
  public boolean authenticate(GuanxiPrincipal principal, String username, String password);

  /**
   * Provides a way for an interested party to find out why authentication failed
   *
   * @return Authentication error message
   */
  public String getErrorMessage();
}
