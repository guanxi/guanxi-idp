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

package org.guanxi.idp.farm.attributors;

import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.GuanxiException;
import org.guanxi.xal.idp.*;
import org.guanxi.idp.util.AttributeMap;
import org.guanxi.idp.util.ARPEngine;
import org.apache.xmlbeans.XmlException;

import com.novell.ldap.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;

/**
 * <p>LDAPAttributor</p>
 *
 * @author Alistair Young alistair@smo.uhi.ac.uk
 * @author Aggie Booth bmb6agb@ds.leeds.ac.uk
 */
public class LDAPAttributor extends SimpleAttributor {
  /** The version of LDAP to use */
  static final int LDAP_VERSION = LDAPConnection.LDAP_V3;
  /** Our configuration information */
  private LdapDocument.Ldap ldapConfig = null;

  public void init() {
    try {
      super.init();
      LdapDocument configDoc = LdapDocument.Factory.parse(new File(attributorConfig));
      ldapConfig = configDoc.getLdap();
    }
    catch(IOException me) {
      logger.error("Can't load attributor config file", me);
    }
    catch(XmlException xe) {
      logger.error("Can't parse attributor config file", xe);
    }
  }

  /** @see SimpleAttributor#getAttributes(org.guanxi.common.GuanxiPrincipal, String, ARPEngine, AttributeMap, org.guanxi.xal.idp.UserAttributesDocument.UserAttributes) */
  public void getAttributes(GuanxiPrincipal principal, String relyingParty, ARPEngine arpEngine, AttributeMap mapper,
                            UserAttributesDocument.UserAttributes attributes) throws GuanxiException {
    // Try to get the user's attributes from one of the available servers
    LDAPConnection lc = new LDAPConnection();

    String attrName, attrValue = null;

    // Try to get attributes for the user from all the available servers
    Server[] servers = ldapConfig.getServerArray();

    for (int c=0; c < servers.length; c++) {
      try {
        // Try the current LDAP server
        Server server = servers[c];

        // Connect to the server
        lc.connect(server.getAddress(), Integer.parseInt(server.getPort()));
        logger.info("Connected to " + server.getAddress() + " on port " + server.getPort());

        // Bind with enough rights to get attributes
        lc.bind(LDAP_VERSION, server.getPrivilegedDn(), server.getPrivilegedDnPassword().getBytes());
        logger.info("Authenticated admin user on " + server.getAddress());

        // Get all the available attributes for the user. The DN is stored in the principal by the authenticator

        // Leeds AGB: This will throw a NullPointerException if the principal has not been authenticated
        // (quite possible if multiple authenticators/attributors are being used sequentially)
        // so catch the NullPointerException below
        LDAPEntry attrGroup = lc.read((String)principal.getPrivateProfileDataEntry("dn"));

        lc.disconnect();

        // Does the user have attributes?
        if (attrGroup != null) {
          // Get all the attributes
          LDAPAttributeSet attrs = attrGroup.getAttributeSet();

          // Inject the DN into the attribute set in case we need to process it
          attrs.add(new LDAPAttribute("dn", attrGroup.getDN()));

          if (attrs != null) {
            LDAPAttribute attr = null;
            Iterator<?> entries = attrs.iterator();

            while (entries.hasNext()) {
              // Get the name of the attribute...
              attr = (LDAPAttribute)entries.next();

              // ...and all it's values
              String[] attrValues = attr.getStringValueArray();
              if (attrValues != null) {
                boolean noMoreValues = false;

                for (int valueCount=0; valueCount < attrValues.length; valueCount++) {
                  if (ldapConfig.getValues() != null) {
                    AttributeValueType[] values = ldapConfig.getValues().getAttributeArray();
                    for (int valueTypesCount=0; valueTypesCount < values.length; valueTypesCount++) {
                      if (values[valueTypesCount].getName().equalsIgnoreCase(attr.getName())) {
                        if (values[valueTypesCount].getPosition().equalsIgnoreCase("first")) {
                          valueCount = 0;
                          noMoreValues = true;
                        }
                        if (values[valueTypesCount].getPosition().equalsIgnoreCase("last")) {
                          valueCount = attrValues.length - 1;
                        }
                      }
                    }
                  }

                  attrName = attr.getName();
                  attrValue = attrValues[valueCount];

                  if (noMoreValues) {
                    valueCount = attrValues.length - 1;
                  }

                  logger.debug("Obtained attribute " + attrName);

                  // Can we release the original attributes without mapping?
                  arp(arpEngine, relyingParty, attrName, attrValue, attributes);

                  // Sort out any mappings. This will change the default name/value if necessary
                  HashMap<String, String[]> packagedAttributes = packageAttributesForMapper(attrs);
                  map(arpEngine, mapper, principal, relyingParty, attrName, attrValue, packagedAttributes, attributes);
                } // while (values.hasMoreElements())
              } // if (values != null) {
            } // while (entries.hasNext()) {
          }
          else { // if (attrs != null) {
            logger.debug("No attributes found for user");
          }
        }
        else { // if (attrGroup != null) {
          logger.debug("No attribute group found for user");
        }
      }
      catch(LDAPException le) {
        // If we get an exception, we'll just move on to the next server
        errorMessage = le.getMessage();
        logger.error(le);
      }
      catch(NullPointerException npe){
        // thrown because the principal hasn't been authenticated
        logger.debug("Caught null pointer exception in LDAPAttributor.");
      }
    } // while (((baseServerNode ...
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Packages up all the attributes in a form the mapper can use to
   * cross reference them when doing the mapping.
   *
   * @param attrs All the LDAP attributes for the user
   * @return HashMap of all the attributes
   */
  private HashMap<String, String[]> packageAttributesForMapper(LDAPAttributeSet attrs) {
    HashMap<String, String[]> attributes = new HashMap<String, String[]>();
    LDAPAttribute attr = null;
    Iterator<?> entries = attrs.iterator();
    while (entries.hasNext()) {
      attr = (LDAPAttribute)entries.next();
      String[] attrValues = attr.getStringValueArray();
      if (attrValues != null) {
        attributes.put(attr.getName(), attrValues);
      }
    }
    return attributes;
  }
}
