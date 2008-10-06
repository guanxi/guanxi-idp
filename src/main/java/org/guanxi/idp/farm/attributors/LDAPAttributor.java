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
import org.guanxi.idp.util.ARPEngine;
import org.guanxi.idp.util.AttributeMap;
import org.guanxi.xal.idp.*;
import org.apache.xmlbeans.XmlException;

import com.novell.ldap.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * <p>LDAPAttributor</p>
 *
 * @author Alistair Young alistair@smo.uhi.ac.uk
 * @author Aggie Booth bmb6agb@ds.leeds.ac.uk
 */
public class LDAPAttributor extends SimpleAttributor {
  /** The version of LDAP to use */
  static final int LDAP_VERSION = LDAPConnection.LDAP_V3;
  /** Our ARP engine */
  private ARPEngine arpEngine = null;
  /** Our attribute mapper */
  private AttributeMap mapper = null;
  /** Our configuration information */
  private LdapDocument.Ldap ldapConfig = null;

  public void setMapper(AttributeMap mapper) { this.mapper = mapper; }
  public AttributeMap getMapper() { return mapper; }

  public void setArpEngine(ARPEngine arpEngine) { this.arpEngine = arpEngine; }
  public ARPEngine getArpEngine() { return arpEngine; }

  public void init() {
    try {
      super.init();

      LdapDocument configDoc = LdapDocument.Factory.parse(new File(servletContext.getRealPath(attributorConfig)));
      ldapConfig = configDoc.getLdap();
    }
    catch(IOException me) {
      logger.error("Can't load attributor config file", me);
    }
    catch(XmlException xe) {
      logger.error("Can't parse attributor config file", xe);
    }
  }

  /**
   * Retrieves attributes for a user from all available LDAP servers.
   *
   * @param principal GuanxiPrincipal identifying the previously authenticated user
   * @param relyingParty The providerId of the relying party the attribute are for
   * @param attributes The document into whic to put the attributes
   * @throws GuanxiException if an error occurs
   */
  public void getAttributes(GuanxiPrincipal principal, String relyingParty, UserAttributesDocument.UserAttributes attributes) throws GuanxiException {
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

                  logger.debug("Obtained attribute " + attr.getName());

                  // Can we release the original attributes without mapping?
                  if (arpEngine.release(relyingParty, attrName, attrValue)) {
                    logger.debug("Released attribute " + attrName);
                    AttributorAttribute attribute = attributes.addNewAttribute();
                    attribute.setName(attrName);
                    attribute.setValue(attrValue);
                  }
                  else {
                    logger.debug("Attribute release blocked by ARP : " + attrName + " to " + relyingParty);
                  }

                  // Sort out any mappings. This will change the default name/value if necessary...
                  if (mapper.map(principal, relyingParty, attr.getName(), attrValue)) {
                    for (int mapCount = 0; mapCount < mapper.getMappedNames().length; mapCount++) {
                      // Release the mapped attribute if appropriate
                      if (arpEngine.release(relyingParty, mapper.getMappedNames()[mapCount],
                                            mapper.getMappedValues()[mapCount])) {
                        String mappedValue = mapper.getMappedValues()[mapCount];
                        if (mappedValue.endsWith("@")) mappedValue += ldapConfig.getDomain();

                        AttributorAttribute attribute = attributes.addNewAttribute();
                        attribute.setName(mapper.getMappedNames()[mapCount]);
                        attribute.setValue(mappedValue);

                        logger.debug("Released attribute " + mapper.getMappedNames()[mapCount] +
                                  " -> " + mappedValue + " to " + relyingParty);
                      }
                      else {
                        logger.debug("Attribute release blocked by ARP : " + mapper.getMappedNames()[mapCount] +
                                  " to " + relyingParty);
                      }
                    } // for (int mapCount = 0; mapCount < mapper.getMappedNames().length; mapCount++) {
                  }
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
}
