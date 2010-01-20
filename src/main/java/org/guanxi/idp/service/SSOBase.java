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

package org.guanxi.idp.service;

import org.guanxi.xal.idp.Creds;
import org.guanxi.xal.idp.ServiceProvider;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.common.definitions.Shibboleth;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.ServletContextAware;
import org.springframework.context.MessageSource;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for SSO profile handlers
 * 
 * @author alistair
 */
public abstract class SSOBase extends AbstractController implements ServletContextAware {
  /** Our logger */
  protected Logger logger = null;
  /** The localised messages to use */
  protected MessageSource messages = null;
  /** Our config */
  protected IdpDocument.Idp idpConfig;
  /** The name of the default SP entry in the config file to use */
  protected String defaultSPEntry = null;
  /** The number of seconds assertions should be valid for */
  protected int assertionTimeLimit;
  /** The entityID to use when sending a SAML Response */
  protected String idpEntityID = null;
  /** The name qualifier to use when sending a SAML Response */
  protected String nameQualifier = null;
  /** The name qualifier format to use when sending a SAML Response */
  protected String nameQualifierFormat = null;
  /** The signing credentials to use */
  protected Creds credsConfig = null;

  public void init() {
    logger = Logger.getLogger(this.getClass().getName());
  }

  /**
   * Handles processing of an AuthnRequest message. If the request attribute:
   * wbsso-handler-error-message
   * is present then we must display the error text it contains and go no further.
   *
   * @param request Servlet request
   * @param response Servlet response
   * @return the view to display
   * @throws Exception if an error occurs
   */
  public abstract ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception;

  /**
   * Loads the identity and credential information based on the relying party.
   * These determine what entityID and signing credentials to use.
   *
   * @param request Servlet request
   */
  protected void loadPersona(HttpServletRequest request) {
    /* Now load the appropriate identity and creds from the config file.
     * We'll either use the default or the ones that the particular SP
     * needs to be sent.
     */
    String spID = null;
    ServiceProvider[] spList = idpConfig.getServiceProviderArray();
    for (int c=0; c < spList.length; c++) {
      if (spList[c].getName().equals(request.getParameter(Shibboleth.PROVIDER_ID))) {
        spID = request.getParameter(Shibboleth.PROVIDER_ID);
      }
    }
    if (spID == null) {
      // No specific requirement for this SP so use the default identity and creds
      spID = defaultSPEntry;
    }

    // Now we've sorted the SP id to use, load the identity and creds
    for (int c=0; c < spList.length; c++) {
      if (spList[c].getName().equals(spID)) {
        String identityToUse = spList[c].getIdentity();
        String credsToUse = spList[c].getCreds();

        // We've found the <service-provider> node so look for the corresponding <identity> node
        org.guanxi.xal.idp.Identity[] ids = idpConfig.getIdentityArray();
        for (int cc=0; cc < ids.length; cc++) {
          if (ids[cc].getName().equals(identityToUse)) {
            idpEntityID = ids[cc].getIssuer();
            nameQualifier = ids[cc].getNameQualifier();
            nameQualifierFormat = ids[cc].getFormat();
          }
        }

        // Look for the corresponding <creds> node
        org.guanxi.xal.idp.Creds[] creds = idpConfig.getCredsArray();
        for (int ccc=0; ccc < creds.length; ccc++) {
          if (creds[ccc].getName().equals(credsToUse)) {
            credsConfig = creds[ccc];
          }
        }
      }
    }
  }

  // Setters
  public void setDefaultSPEntry(String defaultSPEntry) { this.defaultSPEntry = defaultSPEntry; }
  public void setAssertionTimeLimit(int assertionTimeLimit) { this.assertionTimeLimit = assertionTimeLimit; }
}
