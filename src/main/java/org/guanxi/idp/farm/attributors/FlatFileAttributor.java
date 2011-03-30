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

import org.guanxi.common.GuanxiException;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.xal.idp.*;
import org.guanxi.idp.util.ARPEngine;
import org.guanxi.idp.util.AttributeMap;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * <h1>FlatFileAttributor</h1>
 * Attributor implementation that gets it's attribute information from a flat file.
 *
 * @author Alistair Young
 */
public class FlatFileAttributor extends SimpleAttributor {
  /** Our config */
  private FlatFileAuthenticatorConfigDocument.FlatFileAuthenticatorConfig ffConfig = null;

  public void init() {
    try {
      super.init();
      FlatFileAuthenticatorConfigDocument configDoc = FlatFileAuthenticatorConfigDocument.Factory.parse(new File(attributorConfig));
      ffConfig = configDoc.getFlatFileAuthenticatorConfig();
    }
    catch(IOException me) {
      logger.error("Can't load attributor config file", me);
    }
    catch(XmlException xe) {
      logger.error("Can't parse attributor config file", xe);
    }
  }

  /** @see SimpleAttributor#getAttributes(org.guanxi.common.GuanxiPrincipal, String, org.guanxi.idp.util.ARPEngine , org.guanxi.idp.util.AttributeMap , org.guanxi.xal.idp.UserAttributesDocument.UserAttributes) */
  public void getAttributes(GuanxiPrincipal principal, String relyingParty, ARPEngine arpEngine, AttributeMap mapper,
                            UserAttributesDocument.UserAttributes attributes) throws GuanxiException {
    // Before we do anything, see if we need to release a NameID
    processNameID(mapper, relyingParty, attributes);

    // GuanxiPrincipal is storing their username, put there by the authenticator
    String username = (String)principal.getPrivateProfileDataEntry("username");

    // Look for the user in the config file
    User[] users = ffConfig.getUserArray();
    for (int c=0; c < users.length; c++) {
      if (users[c].getUsername().equals(username)) {
        // Load up their attributes from the config file
        UserAttribute[] attrs = users[c].getUserAttributeArray();

        for (int cc=0; cc < attrs.length; cc++) {
          // This is the default name and value for the attribute
          String attrName = attrs[cc].getName();
          String attrValue = attrs[cc].getValue();

          // Can we release the original attributes without mapping?
          arp(arpEngine, relyingParty, attrName, attrValue, attributes);

          // Sort out any mappings. This will change the default name/value if necessary
          HashMap<String, String[]> packagedAttributes = packageAttributesForMapper(attrs);
          map(arpEngine, mapper, principal, relyingParty, attrName, attrValue, packagedAttributes, attributes);
        } // for (int cc=0; cc < attrs.length; cc++)
      } // if (users[c].getUsername().equals(username))
    } // for (int c=0; c < users.length; c++)
  }

  /**
   * Packages up all the attributes in a form the mapper can use to
   * cross reference them when doing the mapping.
   *
   * @param attrs All the attributes for the user
   * @return HashMap of all the attributes
   */
  private HashMap<String, String[]> packageAttributesForMapper(UserAttribute[] attrs) {
    HashMap<String, String[]> attributes = new HashMap<String, String[]>();
    for (int cc=0; cc < attrs.length; cc++) {
      attributes.put(attrs[cc].getName(), new String[]{attrs[cc].getValue()});
    }
    return attributes;
  }
}
