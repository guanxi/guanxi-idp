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

package org.guanxi.idp.service.saml2;

import org.guanxi.idp.service.GenericAuthHandler;
import org.guanxi.common.Utils;
import org.guanxi.common.metadata.SPMetadata;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.entity.EntityFarm;
import org.guanxi.common.entity.EntityManager;
import org.guanxi.common.trust.TrustUtils;
import org.guanxi.xal.saml_2_0.protocol.AuthnRequestDocument;
import org.guanxi.xal.saml_2_0.metadata.EntityDescriptorType;
import org.guanxi.xal.saml_2_0.metadata.IndexedEndpointType;
import org.apache.xmlbeans.XmlException;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.security.cert.X509Certificate;

/**
 * Handles authenticating SAML2 Web Browser SSO connections
 * 
 * @author alistair
 */
public class WebBrowserSSOAuthHandler extends GenericAuthHandler {
  /** Our logger */
  private static final Logger logger = Logger.getLogger(WebBrowserSSOAuthHandler.class.getName());

  /**
   * Takes care of authenticating the user and verifying the requesting entity.
   * As this handler can't display any errors it sets a request attribute:
   * wbsso-handler-error-message = error message text to display
   * if entity verification fails. The main handler can then display an message.
   * The handler can assume everything was ok if that attribute is not present.
   * 
   * @param request Servlet request
   * @param response Servlet response
   * @param object the object!
   * @return true to continue with the request otherwise false
   * @throws Exception if an error occurs
   */
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
    String entityID = null;
    EntityManager manager = null;
    try {
      AuthnRequestDocument requestDoc = AuthnRequestDocument.Factory.parse(new StringReader(Utils.decodeBase64(request.getParameter("SAMLRequest"))));
      entityID = requestDoc.getAuthnRequest().getIssuer().getStringValue();
      // Pass the entityID to the service via the login page if required
      request.setAttribute("entityID", entityID);
      request.setAttribute("requestID", requestDoc.getAuthnRequest().getID());

      EntityFarm farm = (EntityFarm)servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_ENTITY_FARM);
      manager = farm.getEntityManagerForID(entityID);

      // Verify the signature if there is one
      if (requestDoc.getAuthnRequest().getSignature() != null) {
        if (TrustUtils.verifySignature(requestDoc)) {
          X509Certificate[] x509FromSig = new X509Certificate[] {TrustUtils.getX509CertFromSignature(requestDoc.getAuthnRequest().getSignature().getKeyInfo())};
          if (!manager.getTrustEngine().trustEntity(manager.getMetadata(entityID), x509FromSig)) {
            logger.info("failed to trust " + entityID);
            request.setAttribute("wbsso-handler-error-message",
                                 messageSource.getMessage("sp.failed.verification",
                                                          new Object[] {entityID},
                                                          request.getLocale()));
          }
        }
        else {
          logger.error("failed to verify signature from " + entityID);
          request.setAttribute("wbsso-handler-error-message",
                               messageSource.getMessage("sp.signature.verification.failed",
                                                        null,
                                                        request.getLocale()));
        }
      }
    }
    catch(XmlException xe) {
      logger.error("Error verifying entity " + entityID, xe);
      request.setAttribute("wbsso-handler-error-message",
                           messageSource.getMessage("sp.failed.verification",
                                                    new Object[] {entityID},
                                                    request.getLocale()));
    }

    // Entity verification was successful. Now get its attribute consumer URL
    SPMetadata metadata = (SPMetadata)manager.getMetadata(entityID);
    String acsURL = null;
    EntityDescriptorType saml2Metadata = (EntityDescriptorType)metadata.getPrivateData();
    IndexedEndpointType[] acss = saml2Metadata.getSPSSODescriptorArray(0).getAssertionConsumerServiceArray();
    for (IndexedEndpointType acs : acss) {
      if (acs.getBinding().equalsIgnoreCase("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST")) {
        acsURL = acs.getLocation();
      }
    }
    if (acsURL == null) {
      logger.error("SP does not support WBSSO" + entityID);
      request.setAttribute("wbsso-handler-error-message",
                           messageSource.getMessage("error.profile.not.supported",
                                                    new Object[] {entityID},
                                                    request.getLocale()));
    }
    else {
      request.setAttribute("acsURL", acsURL);
    }

    // Display the error without going through user authentication
    if (request.getAttribute("wbsso-handler-error-message") != null) {
      return true;
    }
    
    /* Redirects to the authentication page as required. This is to authenticate the user.
     * We'll end up back here after the user has logged in.
     */
    if (auth(entityID, request, response)) {
      return true;
    }
    else {
      return false;
    }
  }
}
