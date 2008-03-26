/* CVS Header
   $
   $
*/

package org.guanxi.idp.service.shibboleth;

import org.guanxi.idp.service.SAML2EntityVerifier;
import org.guanxi.xal.saml_2_0.metadata.EntityDescriptorType;
import org.guanxi.xal.saml_2_0.metadata.IndexedEndpointType;
import org.guanxi.common.definitions.SAML;

import javax.servlet.http.HttpServletRequest;

public class ShibbolethSAML2EntityVerifier implements SAML2EntityVerifier {
  public boolean verify(EntityDescriptorType entityDescriptor, HttpServletRequest request) {
    IndexedEndpointType[] endpoints = entityDescriptor.getSPSSODescriptorArray(0).getAssertionConsumerServiceArray();
    for (IndexedEndpointType endpoint : endpoints) {
      if (endpoint.getBinding().equals(SAML.BROWSER_POST_BINDING)) {
        if (request.getParameter("shire").equals(endpoint.getLocation())) {
          return true;
        }
      }
    }

    return false;
  }
}
