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

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.ServletContextAware;
import org.guanxi.common.definitions.Shibboleth;
import org.guanxi.xal.saml_2_0.protocol.ResponseDocument;
import org.guanxi.xal.saml_2_0.protocol.ResponseType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SAML2 Web Browser SSO Single Sign-On Service.
 * This is the authentication request protocol endpoint at the identity provider to which the
 * <AuthnRequest> message (or artifact representing it) is delivered by the user agent. 
 */
public class WebBrowserSSO extends AbstractController implements ServletContextAware {
  private String responseView = null;
  private String errorView = null;

  public void init() {}

  public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ModelAndView mAndV = new ModelAndView();

    ResponseDocument responseDoc = ResponseDocument.Factory.newInstance();
    ResponseType wbssoResponse = responseDoc.addNewResponse();

    // HTTP POST or HTTP Artifact
      
    // Send the Response to the SP
    mAndV.setViewName(responseView);
    mAndV.getModel().put(Shibboleth.SHIRE, request.getParameter(Shibboleth.SHIRE));
    return mAndV;
  }

  // Setters
  public void setResponseView(String responseView) { this.responseView = responseView; }
  public void setErrorView(String errorView) { this.errorView = errorView; }
}
