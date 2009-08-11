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

package org.guanxi.idp.farm.authcookiehandlers;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApplicationCookieAuthenticator extends HandlerInterceptorAdapter implements ServletContextAware {
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  @SuppressWarnings("unused")
  private ServletContext servletContext = null;

  /**
   * Initialise the interceptor
   */
  public void init() {
  }

  /**
   * Uses the chain of cookie handlers to try to authenticator a user without recourse to the login page
   *
   * @param request Standard HttpServletRequest
   * @param response Standard HttpServletResponse
   * @param object handler
   * @return true
   * @throws Exception if an error occurs
   */
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
    /*
    GuanxiPrincipal principal = new GuanxiPrincipal();
    principal.setServletContext(servletContext);
    request.setAttribute(Guanxi.REQUEST_ATTR_IDP_PRINCIPAL, principal);
    request.setAttribute(Guanxi.REQUEST_ATTR_IDP_COOKIE_AUTHENTICATED, Guanxi.REQUEST_ATTR_IDP_COOKIE_AUTHENTICATED);
    */

    return true;
  }

  // Called by Spring as we are ServletContextAware
  public void setServletContext(ServletContext servletContext) { this.servletContext = servletContext; }
}
