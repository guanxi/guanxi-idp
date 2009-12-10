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
import org.springframework.context.MessageSource;
import org.guanxi.xal.saml_2_0.protocol.ResponseDocument;
import org.guanxi.xal.saml_2_0.protocol.ResponseType;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SAML2 Web Browser SSO Single Sign-On Service.
 * This is the authentication request protocol endpoint at the identity provider to which the
 * <AuthnRequest> message (or artifact representing it) is delivered by the user agent.
 *
 * @author alistair
 */
public class WebBrowserSSO extends AbstractController implements ServletContextAware {
  /** Our logger */
  private static final Logger logger = Logger.getLogger(WebBrowserSSO.class.getName());
  /** The localised messages to use */
  private MessageSource messages = null;
  /** The JSP to use to POST the response to the SP */
  private String httpPOSTView = null;
  /** The JSP to display if an error occurs */
  private String errorView = null;
  /** The request attribute that holds the error message for the error view */
  private String errorViewDisplayVar = null;

  public void init() {}

  /**
   * Handles processing of an AuthnRequest message. If the request attribute:
   * wbsso-handler-error-message
   * is present then we must display the error text it contains and go no further.
   * 
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ModelAndView mAndV = new ModelAndView();

    // Display an error message if it exists and go no further
    if (request.getAttribute("wbsso-handler-error-message") != null) {
      logger.error("Displaying auth handler error");
      mAndV.setViewName(errorView);
      mAndV.getModel().put(errorViewDisplayVar, request.getAttribute("wbsso-handler-error-message"));
      return mAndV;
    }

    ResponseDocument responseDoc = ResponseDocument.Factory.newInstance();
    ResponseType wbssoResponse = responseDoc.addNewResponse();

    // HTTP POST or HTTP Artifact

    String b64SAMLResponse = null;
      
    // Send the Response to the SP
    request.setAttribute("SAMLResponse", b64SAMLResponse);
    request.setAttribute("RelayState", request.getParameter("RelayState"));
    mAndV.setViewName(httpPOSTView);
    mAndV.getModel().put("wbsso_acs_endpoint", request.getAttribute("acsURL"));
    return mAndV;
  }

  // Setters
  public void setMessages(MessageSource messages) { this.messages = messages; }
  public void setHttpPOSTView(String httpPOSTView) { this.httpPOSTView = httpPOSTView; }
  public void setErrorView(String errorView) { this.errorView = errorView; }
  public void setErrorViewDisplayVar(String errorViewDisplayVar) { this.errorViewDisplayVar = errorViewDisplayVar; }
}
