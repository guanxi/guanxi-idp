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

import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.GuanxiPrincipalFactory;
import org.guanxi.common.log.Log4JLoggerConfig;
import org.guanxi.common.log.Log4JLogger;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.xal.idp.AuthPage;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.xal.idp.ServiceProvider;
import org.guanxi.idp.farm.authenticators.Authenticator;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.context.ServletContextAware;
import org.springframework.context.MessageSource;
import org.apache.log4j.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.util.List;

public class AuthHandler extends HandlerInterceptorAdapter implements ServletContextAware {
  /** The name of the request attribute that the form action will be stored under */
  public static final String FORM_ACTION_ATTRIBUTE = "FORM_ACTION_ATTRIBUTE";
  /** When passing required parameters to the authenticator page, the request attributes
   *  will be prefixed by this.
   */
  public static final String REQUIRED_PARAM_PREFIX = "REQUIRED_PARAM_";
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  private ServletContext servletContext = null;
  /** Our logger */
  private Logger log = null;
  /** The logger config */
  private Log4JLoggerConfig loggerConfig = null;
  /** The Logging setup to use */
  private Log4JLogger logger = null;
  /** The error page to use */
  private String errorPage = null;
  /** The authenticator to use */
  private Authenticator authenticator = null;
  /** The localised messages */
  private MessageSource messageSource = null;
  /** The factory to use to create new GuanxiPrincipal objects */
  private GuanxiPrincipalFactory gxPrincipalFactory = null;
  /** The URL for the authentication form\s action */
  private String authFormAction = null;
  /** The request parameter that holds the ID of the service provider */
  private String spIDRequestParam = null;
  /** The list of required request parameters for the particular instance */
  private List<String> requiredRequestParams = null;

  /**
   * Initialise the interceptor
   */
  public void init() {
    try {
      loggerConfig.setClazz(AuthHandler.class);

      // Sort out the file paths for logging
      loggerConfig.setLogConfigFile(servletContext.getRealPath(loggerConfig.getLogConfigFile()));
      loggerConfig.setLogFile(servletContext.getRealPath(loggerConfig.getLogFile()));

      // Get our logger
      log = logger.initLogger(loggerConfig);
    }
    catch(GuanxiException me) {
    }
  }

  /**
   * Looks for an existing GuanxiPrincipal referenced by a request cookie. When a cookie is created after
   * a successful authentication at the IdP, either via the login page or an application cookie handler,
   * the corresponding GuanxiPrincipal is stored in the servlet context against the cookie value.
   * The new GuanxiPrincipal that is created after successful authentication is stored in the servlet
   * context under GuanxiPrincipal.id
   *
   * @param request Standard HttpServletRequest
   * @param response Standard HttpServletResponse
   * @param object handler
   * @return true 
   * @throws Exception if an error occurs
   */
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
    String missingParams = checkRequestParameters(request);
    if (missingParams != null) {
      log.info("Missing param(s)");
      request.setAttribute("message", messageSource.getMessage("missing.param",
                                                               new Object[] {missingParams},
                                                               request.getLocale()));
      request.getRequestDispatcher(errorPage).forward(request, response);
      return false;
    }

    IdpDocument.Idp idpConfig = (IdpDocument.Idp)servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG);

    // Look for the service provider in the metadata
    boolean spSupported = false;
    ServiceProvider[] spList = idpConfig.getServiceProviderArray();
    for (int c=0; c < spList.length; c++) {
      if (spList[c].getName().equals(request.getParameter(spIDRequestParam))) {
        spSupported = true;
      }
    }

    // Did we find the service provider?
    if (!spSupported) {
      log.error("Service Provider providerId " + request.getParameter(spIDRequestParam) + " not supported");
      request.setAttribute("message", messageSource.getMessage("sp.not.supported",
                                                               new Object[]{request.getParameter(spIDRequestParam)},
                                                               request.getLocale()));
      request.getRequestDispatcher(errorPage).forward(request, response);
      return false;
    }

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
          servletContext.setAttribute(principal.getID(), principal);

          // Get a new cookie ready to reference the principal in the servlet context
          Cookie cookie = new Cookie(getCookieName(), principal.getID());
          cookie.setDomain((String)servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_DOMAIN));
          cookie.setPath(idpConfig.getCookie().getPath());
          if (((Integer)(servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_AGE))).intValue() != -1)
            cookie.setMaxAge(((Integer)(servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_AGE))).intValue());
          response.addCookie(cookie);

          return true;
        } // if (authenticator.authenticate...
        else {
          log.error("Authentication error : " + authenticator.getErrorMessage());
          request.setAttribute("message", messageSource.getMessage("authentication.error",
                                                                   null,
                                                                   request.getLocale()));
          request.getRequestDispatcher(errorPage).forward(request, response);
          return false;
        }
      }
    }

    // No embedded cookie authentication or local auth, so show the login page
    String authPage = null;
    AuthPage[] authPages = idpConfig.getAuthenticatorPages().getAuthPageArray();
    for (int c=0; c < authPages.length; c++) {
      // We'll use the default auth page if none is specified for this service provider
      if (authPages[c].getProviderId().equals(Guanxi.DEFAULT_AUTH_PAGE_MARKER)) {
        authPage = authPages[c].getUrl();
      }

      // Customised auth page for this service provider
      if (authPages[c].getProviderId().equals(request.getParameter(spIDRequestParam))) {
        authPage = authPages[c].getUrl();
      }
    }

    addRequiredParamsAsPrefixedAttributes(request);
    request.getRequestDispatcher(authPage).forward(request, response);

    return false;
  }

  /**
   * Checks the request for miussing parameters
   *
   * @param request Standard HttpServletRequest
   * @return null if no parameters are missing, otherwise a comma separated list of the missing parameters
   */
  private String checkRequestParameters(HttpServletRequest request) {
    String missingParams = "";

    if (requiredRequestParams == null) return null;

    for (String param : requiredRequestParams) {
      if (request.getParameter(param) == null) {
        missingParams += " " + param;
      }
    }

    if (missingParams.equals(""))
      return null;
    else
      return missingParams;
  }

  /**
   * Copies required request parameters to prefixed request attributes to pass to the
   * authenticator page.
   * @param request Standard HttpServletRequest
   */
  private void addRequiredParamsAsPrefixedAttributes(HttpServletRequest request) {
    request.setAttribute(FORM_ACTION_ATTRIBUTE, authFormAction);
    for (String param : requiredRequestParams) {
      request.setAttribute(REQUIRED_PARAM_PREFIX + param, request.getParameter(param));
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

  // Called by Spring as we are ServletContextAware
  public void setServletContext(ServletContext servletContext) { this.servletContext = servletContext; }

  public void setLog(Logger log) { this.log = log; }
  public Logger getLog() { return log; }

  public void setLoggerConfig(Log4JLoggerConfig loggerConfig) { this.loggerConfig = loggerConfig; }
  public Log4JLoggerConfig getLoggerConfig() { return loggerConfig; }

  public void setLogger(Log4JLogger logger) { this.logger = logger; }
  public Log4JLogger getLogger() { return logger; }

  public void setErrorPage(String errorPage) { this.errorPage = errorPage; }

  public void setAuthenticator(Authenticator authenticator) { this.authenticator = authenticator; }

  public void setMessageSource(MessageSource messageSource) { this.messageSource = messageSource; }

  public void setGxPrincipalFactory(GuanxiPrincipalFactory gxPrincipalFactory) { this.gxPrincipalFactory = gxPrincipalFactory; }

  public void setAuthFormAction(String authFormAction) { this.authFormAction = authFormAction; }

  public void setSpIDRequestParam(String spIDRequestParam) { this.spIDRequestParam = spIDRequestParam; }

  public void setRequiredRequestParams(List<String> requiredRequestParams) { this.requiredRequestParams = requiredRequestParams; }
}
