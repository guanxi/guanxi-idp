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
import org.guanxi.idp.service.SAML2EntityVerifier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebBrowserSSOAuthHandler extends GenericAuthHandler {
  /** The verifier instance to use to verify the incoming entity */
  private SAML2EntityVerifier entityVerifier = null;
  
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
    //@todo extract the AuthnRequest from the SAMLRequest param and verify SP

    if(auth("test-entityid", request, response)) {
      //@todo add the AuthnRequest to the request to let the SSO endpoint handle it
      
      return true;
    }

    return false;
  }

  // Setters
  public void setEntityVerifier(SAML2EntityVerifier entityVerifier) { this.entityVerifier = entityVerifier; }
}
