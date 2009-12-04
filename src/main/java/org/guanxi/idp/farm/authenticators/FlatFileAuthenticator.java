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
import org.guanxi.xal.idp.FlatFileAuthenticatorConfigDocument;
import org.guanxi.xal.idp.User;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;

/**
 * <h1>FlatFileAuthenticator</h1>
 * Authenticator implementation that gets it's user information from a flat file.
 *
 * @author Alistair Young
 */
public class FlatFileAuthenticator extends SimpleAuthenticator {
  /** Our config */
  private FlatFileAuthenticatorConfigDocument.FlatFileAuthenticatorConfig ffConfig = null;
  /** Current status */
  private String errorMessage = null;

  public void init() {
    try {
      super.init();
      
      FlatFileAuthenticatorConfigDocument configDoc = FlatFileAuthenticatorConfigDocument.Factory.parse(new File(servletContext.getRealPath(authenticatorConfig)));
      ffConfig = configDoc.getFlatFileAuthenticatorConfig();
    }
    catch(IOException me) {
      logger.error("Can't load attributor config file", me);
    }
    catch(XmlException xe) {
      logger.error("Can't parse attributor config file", xe);
    }
  }

  /**
   * Authenticates a user based on their username and password in the config file
   *
   * @param principal GuanxiPrincipal which must be filled in upon successful authentication
   * @param username username, which is case sensitive
   * @param password password, which is case sensitive
   * @return true if authentication was successful, otherwise false
   */
  public boolean authenticate(GuanxiPrincipal principal, String username, String password) {
    // Look for the user in the config file
    User[] users = ffConfig.getUserArray();
    for (int c=0; c < users.length; c++) {
      if (users[c].getUsername().equals(username)) {
        if (users[c].getPassword().equals(password)) {
          // Use this later to get their attributes
          principal.addPrivateProfileDataEntry("username", users[c].getUsername());
          return true;
        }
      }
    }

    return false;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
