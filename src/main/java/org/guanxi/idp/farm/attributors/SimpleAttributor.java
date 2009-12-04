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

import org.apache.log4j.Logger;
import org.guanxi.idp.util.ARPEngine;
import org.guanxi.idp.util.AttributeMap;
import org.guanxi.xal.idp.AttributorAttribute;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.GuanxiException;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

public abstract class SimpleAttributor implements Attributor, ServletContextAware {
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  ServletContext servletContext = null;
  /** Our logger */
  protected Logger logger = null;
  /** Our ARP engine */
  protected ARPEngine arpEngine = null;
  /** Our attribute mapper */
  protected AttributeMap mapper = null;
  /** The path/name of our own config file */
  protected String attributorConfig = null;
  /** Current status */
  protected String errorMessage = null;

  /**
   * Passes an attribute name and value through the ARP engine. If the name/value can be
   * released, they will be added to the attributes document.
   *
   * @param relyingParty the entityID of the entity looking for attributes
   * @param attributeName the name of the attribute
   * @param attributeValue the value of the attribute
   * @param attributes the attributes document that will hold the released attribute
   */
  protected void arp(String relyingParty, String attributeName, String attributeValue,
                     UserAttributesDocument.UserAttributes attributes) {
    // Can we release the original attributes without mapping?
    if (arpEngine.release(relyingParty, attributeName, attributeValue)) {
      AttributorAttribute attribute = attributes.addNewAttribute();
      attribute.setName(attributeName);
      attribute.setValue(attributeValue);
      logger.debug("Released attribute " + attributeName + " to " + relyingParty);
    }
    else {
      logger.debug("Attribute release blocked by ARP : " + attributeName + " to " + relyingParty);
    }
  }

  /**
   * Passes an attribute name and value through the Mapper and ARP engines. If the name/value can be
   * released after being mapped, they will be added to the attributes document.
   *
   * @param principal the GuanxiPrincipal for the user who's attributes are being requested
   * @param relyingParty the entityID of the entity looking for attributes
   * @param attributeName the name of the attribute
   * @param attributeValue the value of the attribute
   * @param attributes the attributes document that will hold the released attribute
   */
  protected void map(GuanxiPrincipal principal, String relyingParty, String attributeName, String attributeValue,
                     UserAttributesDocument.UserAttributes attributes) {
    if (mapper.map(principal, relyingParty, attributeName, attributeValue)) {
      for (int mapCount = 0; mapCount < mapper.getMappedNames().length; mapCount++) {
        // Release the mapped attribute if appropriate
        if (arpEngine.release(relyingParty, mapper.getMappedNames()[mapCount],
                              mapper.getMappedValues()[mapCount])) {
          String mappedValue = mapper.getMappedValues()[mapCount];

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
      }
    }
  }

  /**
   * Retrieves attributes for a user from a database
   *
   * @param principal GuanxiPrincipal identifying the previously authenticated user
   * @param relyingParty The providerId of the relying party the attribute are for
   * @param attributes The document into which to put the attributes
   * @throws GuanxiException if an error occurs
   */
  public abstract void getAttributes(GuanxiPrincipal principal, String relyingParty,
                                     UserAttributesDocument.UserAttributes attributes) throws GuanxiException;

  /**
   * Initialises the subclass
   */
  public void init() {
    logger = Logger.getLogger(this.getClass().getName());
    
    // Sort out the path to the config file if there is one
    if (attributorConfig != null) {
      if ((attributorConfig.startsWith("WEB-INF")) ||
          (attributorConfig.startsWith("/WEB-INF"))) {
        attributorConfig = servletContext.getRealPath(attributorConfig);
      }
    }
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  public void setAttributorConfig(String attributorConfig) {
    this.attributorConfig = attributorConfig;
  }

  public String getAttributorConfig() {
    return attributorConfig;
  }

  public void setMapper(AttributeMap mapper) {
    this.mapper = mapper;
  }

  public AttributeMap getMapper() {
    return mapper;
  }

  public void setArpEngine(ARPEngine arpEngine) {
    this.arpEngine = arpEngine;
  }

  public ARPEngine getArpEngine() {
    return arpEngine;
  }
}
