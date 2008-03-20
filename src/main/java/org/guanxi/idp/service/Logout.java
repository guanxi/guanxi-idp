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

import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.GuanxiPrincipalFactory;
import org.guanxi.common.log.Log4JLoggerConfig;
import org.guanxi.common.log.Log4JLogger;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.context.ServletContextAware;
import org.springframework.context.MessageSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.*;
import java.io.IOException;

/**
 * <p>Logout</p>
 *
 * This filter provides an HTTP binding for logout functionality.
 * It calls the low level SSO API to do the actual logout.
 * To use it, set the <url-pattern> of the <filter-mapping> to be
 * the logout URL of the application in which the IdP is embedded.
 * e.g.
 * <url-pattern>/logout</url-pattern>
 * The default value will let users logout out of the standalone
 * IdP.
 *
 * @author Alistair Young alistair@smo.uhi.ac.uk
 */
public class Logout extends HandlerInterceptorAdapter implements ServletContextAware {
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  private ServletContext servletContext = null;
  /** Our logger */
  private Logger log = null;
  /** The logger config */
  private Log4JLoggerConfig loggerConfig = null;
  /** The Logging setup to use */
  private Log4JLogger logger = null;
  /** The error page to use */
  private String logoutPage = null;
  /** Whether the IdP is embedded in another application */
  private boolean passive;
  /** The factory to use to locate GuanxiPrincipal objects */
  private GuanxiPrincipalFactory gxPrincipalFactory = null;
  /** The localised messages */
  private MessageSource messageSource = null;

  /**
   * Initialise the interceptor
   */
  public void init() {
    try {
      loggerConfig.setClazz(Logout.class);

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
   *  Lets the filter clean up prior to container shutdown
   */
  public void destroy() {
  }

  /**
   * This handler traps logout requests to the IdP or the application in which the
   * IdP is embedded. If the IdP is standalone (passive = no), then the filter
   * will log the user out of the IdP and display the logout page as specified in
   * web.xml. If the IdP is embedded in an application (passive = yes) then the
   * filter will log the user out of the IdP and stand back and allow the
   * application to continue with it's logout functionality. The filter will not
   * display the logout page in this case.
   *
   * @param request Standard HttpServletRequest
   * @param response Standard HttpServletResponse
   * @param object handler
   * @return true
   * @throws Exception if an error occurs
   */
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
    // Logout of the IdP
    processLogout((HttpServletRequest)request, (HttpServletResponse)response);

    /* Continue with the request cycle. This depends on the passive setting in web.xml.
     * If we're not passive then we've already redirected to the logout page and
     * continuing with the filter chain will produce an IllegalStateException.
     */
    if (passive) {
      return true;
    }
    else {
      // We'll have displayed the logout page if not passive
      return false;
    }
  }

  /**
   * Does the logging out. The method looks for the user's IdP cookie in the request
   * and if it finds it, it extracts the corresponding GuanxiPrincipal and sends it
   * to SSO.logout() for processing.
   *
   * @param request Standard HttpServletRequest
   * @param response Standard HttpServletRequest
   * @throws ServletException if an error occurrs
   * @throws IOException if an error occurrs
   */
  public void processLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String cookieName = getCookieName();
    boolean loggedOut = false;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (int c=0; c < cookies.length; c++) {
        if (cookies[c].getName().equals(cookieName)) {
          // Retrieve the principal from the servlet context...
          GuanxiPrincipal principal = (GuanxiPrincipal)servletContext.getAttribute(cookies[c].getValue());

          // ...and get rid of it
          if (principal != null) {
            servletContext.setAttribute(principal.getUniqueId(), null);
          }

          loggedOut = true;
        }
      }
    }

    /* Only display the logout page if we're not in passive mode.
     * What this means is if we're in passive mode (passive = yes)
     * then we're most likely embedded in an application, which has
     * it's own logout page.
     */
    if (!passive) {
      if (loggedOut)
        request.setAttribute("LOGOUT_MESSAGE", messageSource.getMessage("idp.logout.successful", null, request.getLocale()));
      else
        request.setAttribute("LOGOUT_MESSAGE", messageSource.getMessage("idp.logout.unsuccessful", null, request.getLocale()));

      request.getRequestDispatcher(logoutPage).forward(request, response);
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

  public void setLogoutPage(String logoutPage) { this.logoutPage = logoutPage; }

  public void setPassive(boolean passive) { this.passive = passive; }

  public void setGxPrincipalFactory(GuanxiPrincipalFactory gxPrincipalFactory) { this.gxPrincipalFactory = gxPrincipalFactory; }

  public void setMessageSource(MessageSource messageSource) { this.messageSource = messageSource; }
}
