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
