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

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.context.ServletContextAware;
import org.springframework.context.MessageSource;
import org.apache.log4j.Logger;
import org.guanxi.idp.farm.authenticators.Authenticator;
import org.guanxi.common.GuanxiPrincipalFactory;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.xal.idp.AuthPage;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.util.List;

public abstract class GenericAuthHandler extends HandlerInterceptorAdapter implements ServletContextAware {
  /** The name of the request attribute that the form action will be stored under */
  public static final String FORM_METHOD_ATTRIBUTE = "FORM_METHOD_ATTRIBUTE";
  /** The name of the request attribute that the form action will be stored under */
  public static final String FORM_ACTION_ATTRIBUTE = "FORM_ACTION_ATTRIBUTE";
  /** When passing required parameters to the authenticator page, the request attributes
   *  will be prefixed by this.
   */
  public static final String REQUIRED_PARAM_PREFIX = "REQUIRED_PARAM_";
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  protected ServletContext servletContext = null;
  /** The IdP's config */
  protected IdpDocument.Idp idpConfig = null;
  /** Our logger */
  protected static final Logger logger = Logger.getLogger(GenericAuthHandler.class.getName());
  /** The error page to use */
  private String errorPage = null;
  /** The authenticator to use */
  private Authenticator authenticator = null;
  /** The localised messages */
  protected MessageSource messageSource = null;
  /** The factory to use to create new GuanxiPrincipal objects */
  private GuanxiPrincipalFactory gxPrincipalFactory = null;
  /** The URL for the authentication form\s action */
  private String authFormAction = null;
  /** The list of required request parameters for the particular instance */
  private List<String> requiredRequestParams = null;

  /**
   * Initialise the interceptor
   */
  public void init() {
    idpConfig = (IdpDocument.Idp)servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG);
  }

  public abstract boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception;

  protected boolean auth(String spEntityID, HttpServletRequest request, HttpServletResponse response) {
    // Look for our cookie. This is after any application cookie handler has authenticated the user
    String cookieName = getCookieName();
    Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (int c=0; c < cookies.length; c++) {
          if (cookies[c].getName().equals(cookieName)) {
            // Retrieve the principal from the servlet context
            if (servletContext.getAttribute(cookies[c].getValue()) == null) {
              // Out of date cookie value, so remove the cookie
              cookies[c].setMaxAge(0);
              response.addCookie(cookies[c]);
            }
            else {
              // Found the principal from a previously established authentication
              request.setAttribute(Guanxi.REQUEST_ATTR_IDP_PRINCIPAL,
                                   (GuanxiPrincipal)servletContext.getAttribute(cookies[c].getValue()));
              return true;
            }
          }
      }
    }

    // Are we getting an authentication request from the login page?
    if (request.getParameter("guanxi:mode") != null) {
      if (request.getParameter("guanxi:mode").equalsIgnoreCase("authenticate")) {
        // Get a new GuanxiPrincipal...
        GuanxiPrincipal principal = gxPrincipalFactory.createNewGuanxiPrincipal(request);
        if (authenticator.authenticate(principal, request.getParameter("userid"), request.getParameter("password"))) {
          // ...associate it with a login name...
          if (principal.getName() == null) {
            //The login name from the authenticator page
            principal.setName(request.getParameter("userid"));
          }
          // ...store it in the request for the SSO to use...
          request.setAttribute(Guanxi.REQUEST_ATTR_IDP_PRINCIPAL, principal);
          // ...and store it in application scope for the rest of the profile to use
          servletContext.setAttribute(principal.getUniqueId(), principal);

          // Get a new cookie ready to reference the principal in the servlet context
          Cookie cookie = new Cookie(getCookieName(), principal.getUniqueId());
          cookie.setDomain((String)servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_DOMAIN));
          cookie.setPath(idpConfig.getCookie().getPath());
          if (((Integer)(servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_AGE))).intValue() != -1)
            cookie.setMaxAge(((Integer)(servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_AGE))).intValue());
          response.addCookie(cookie);

          return true;
        } // if (authenticator.authenticate...
        else {
          logger.error("Authentication error : " + authenticator.getErrorMessage());
          request.setAttribute("message", messageSource.getMessage("authentication.error",
                                                                   null,
                                                                   request.getLocale()));
          try {
            request.getRequestDispatcher(errorPage).forward(request, response);
          }
          catch(Exception e) {
            logger.error("Could not display authentication error page", e);
          }
          return false;
        }
      }
    } // if (request.getParameter("guanxi:mode") != null) {

    // No embedded cookie authentication or local auth, so show the login page
    String authPage = null;
    AuthPage[] authPages = idpConfig.getAuthenticatorPages().getAuthPageArray();
    for (int c=0; c < authPages.length; c++) {
      // We'll use the default auth page if none is specified for this service provider
      if (authPages[c].getProviderId().equals(Guanxi.DEFAULT_AUTH_PAGE_MARKER)) {
        authPage = authPages[c].getUrl();
      }

      // Customised auth page for this service provider
      if (authPages[c].getProviderId().equals(spEntityID)) {
        authPage = authPages[c].getUrl();
      }
    }

    addRequiredParamsAsPrefixedAttributes(request);
    try {
      request.getRequestDispatcher(authPage).forward(request, response);
    }
    catch(Exception e) {
      logger.error("Could not display authentication page", e);
    }

    return false;
  }

  /**
   * Copies required request parameters to prefixed request attributes to pass to the
   * authenticator page.
   * @param request Standard HttpServletRequest
   */
  protected void addRequiredParamsAsPrefixedAttributes(HttpServletRequest request) {
    if (request.getAttribute("binding") != null) {
      if (request.getAttribute("binding").equals("HTTP-POST")) {
        request.setAttribute(FORM_METHOD_ATTRIBUTE, "post");
      }
      else if (request.getAttribute("binding").equals("HTTP-Redirect")) {
        request.setAttribute(FORM_METHOD_ATTRIBUTE, "get");
      }
    }
    else {
      request.setAttribute(FORM_METHOD_ATTRIBUTE, "post");
    }

    request.setAttribute(FORM_ACTION_ATTRIBUTE, authFormAction);
    if (requiredRequestParams != null) {
      for (String param : requiredRequestParams) {
        if (request.getParameter(param) != null) {
          request.setAttribute(REQUIRED_PARAM_PREFIX + param, request.getParameter(param));
        }
        if (request.getAttribute(param) != null) {
          request.setAttribute(REQUIRED_PARAM_PREFIX + param, request.getAttribute(param));
        }
      }
    }
  }

  /**
   * Works out the profile specific cookie name
   *
   * @return profile specific cookie name
   */
  private String getCookieName() {
    return (String)servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_NAME) + "_" + gxPrincipalFactory.getCookieName();
  }

  // Setters
  public void setServletContext(ServletContext servletContext) { this.servletContext = servletContext; }
  public void setErrorPage(String errorPage) { this.errorPage = errorPage; }
  public void setAuthenticator(Authenticator authenticator) { this.authenticator = authenticator; }
  public void setMessageSource(MessageSource messageSource) { this.messageSource = messageSource; }
  public void setGxPrincipalFactory(GuanxiPrincipalFactory gxPrincipalFactory) { this.gxPrincipalFactory = gxPrincipalFactory; }
  public void setAuthFormAction(String authFormAction) { this.authFormAction = authFormAction; }
  public void setRequiredRequestParams(List<String> requiredRequestParams) { this.requiredRequestParams = requiredRequestParams; }
}
