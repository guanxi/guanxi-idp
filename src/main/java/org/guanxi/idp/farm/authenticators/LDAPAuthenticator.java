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
import org.guanxi.xal.idp.LdapDocument;
import org.guanxi.xal.idp.Server;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;

import com.novell.ldap.*;

/**
 * <p>LDAPAuthenticator</p>
 *
 * @author Alistair Young
 */
public class LDAPAuthenticator extends SimpleAuthenticator {
  /** The version of LDAP to use */
  static final int LDAP_VERSION = LDAPConnection.LDAP_V3;
  /** Our configuration information */
  private LdapDocument.Ldap ldapConfig = null;

  public void init() {
    try {
      super.init();

      LdapDocument configDoc = LdapDocument.Factory.parse(new File(servletContext.getRealPath(authenticatorConfig)));
      ldapConfig = configDoc.getLdap();
    }
    catch(IOException me) {
      log.error("Can't load attributor config file", me);
    }
    catch(XmlException xe) {
      log.error("Can't parse attributor config file", xe);
    }
  }

  /**
   * Authenticates a user against a set of LDAP servers, based on the specified
   * username and password.<br />
   * Currently only clear text passwords are supported.
   *
   * @param principal GuanxiPrincipal to be filled in upon successful authentication
   * @param username username to authenticate
   * @param password password for authentication
   * @return true if authentication was successful, otherwise false
   */
  public boolean authenticate(GuanxiPrincipal principal, String username, String password) {
    LDAPConnection lc = new LDAPConnection();

    // Try to authenticate the user against all the available servers
    Server[] servers = ldapConfig.getServerArray();
    for (int c=0; c < servers.length; c++) {
      try {
        // Try the current LDAP server
        Server server = servers[c];
        
        // Connect to the server
        lc.connect(server.getAddress(), Integer.parseInt(server.getPort()));
        log.info("Connected to " + server.getAddress() + " on port " + server.getPort());

        lc.bind(LDAP_VERSION, server.getPrivilegedDn(), server.getPrivilegedDnPassword().getBytes());
        log.info("Authenticated admin user on " + server.getAddress());

        // Build the CN of the user to look for
        String searchFilter = "(" + server.getSearchAttribute() +"=" + username + ")";

        // Sort out the scope of the search
        int ldapSearchScope = LDAPConnection.SCOPE_BASE;
        if (server.getSearchScope().equalsIgnoreCase("recursive"))
          ldapSearchScope = LDAPConnection.SCOPE_SUB;

        // Search for the user - first set the time limit...
        LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();
        ldapSearchConstraints.setTimeLimit(Integer.parseInt(server.getTimeout()) * 1000);
        
        // ...then do the search
        LDAPSearchResults searchResults = lc.search(server.getSearchBaseDn(),
                                                    ldapSearchScope,
                                                    searchFilter,
                                                    null, false, ldapSearchConstraints);

        /* Get the DN of the user - should only ever be one user with the specified user name. If somehow there are
         * two users with the same CN but different DN then we'll just take the last one we find. If it's the right
         * one fine, otherwise the user will complain to the helpdesk and whoever created two usernames the same can
         * sort it out!
         */
        String userDN = null;
        while (searchResults.hasMore()) {
          try {
            LDAPEntry nextEntry = searchResults.next();
            
            // Get the users's DN to use in the authentication call
            userDN = nextEntry.getDN();
            log.info("Found user : " + userDN);

            // Only take the first DN found
            break;
          }
          catch(LDAPException le) {
            errorMessage = le.getMessage();
            log.error(le);
            lc.disconnect();
          }
        }

        // Did we find them?
        if (userDN != null) {
          // If their password is wrong then the exception will catch it
          lc.bind(LDAP_VERSION, userDN, password.getBytes());

          // disconnect from the server
          lc.disconnect();

          // Store the user's DN to get their attributes later
          principal.setUserData(userDN);

          log.info("Authenticated user");

          // User authenticated
          return true;
        }
      }
      catch(LDAPException le) {
        // If we get an exception, we'll just move on to the next server
        errorMessage = le.getMessage();
        log.error(le);
      }
    } // while (((baseServerNode ...

    // User could not be authenticated
    return false;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
