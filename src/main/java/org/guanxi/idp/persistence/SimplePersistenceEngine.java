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

package org.guanxi.idp.persistence;

import org.apache.log4j.Logger;
import org.guanxi.common.log.Log4JLoggerConfig;
import org.guanxi.common.log.Log4JLogger;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.GuanxiPrincipal;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

public abstract class SimplePersistenceEngine implements PersistenceEngine, ServletContextAware {
  /** The servlet context */
  protected ServletContext servletContext = null;
  /** Our logger */
  protected Logger log = null;
  /** The logger config */
  protected Log4JLoggerConfig loggerConfig = null;
  /** The Logging setup to use */
  protected Log4JLogger logger = null;

  public void init() {
    try {
      loggerConfig.setClazz(this.getClass());

      // Sort out the file paths for logging
      loggerConfig.setLogConfigFile(servletContext.getRealPath(loggerConfig.getLogConfigFile()));
      loggerConfig.setLogFile(servletContext.getRealPath(loggerConfig.getLogFile()));

      // Get our logger
      log = logger.initLogger(loggerConfig);
    }
    catch(GuanxiException ge) {
    }
  }
  
  // These must be overriden in derivatives
  public abstract boolean attributeExists(GuanxiPrincipal principal, String attributeName);
  public abstract String getAttributeValue(GuanxiPrincipal principal, String attributeName);
  public abstract boolean persistAttribute(GuanxiPrincipal principal, String attributeName, String attributeValue);
  public abstract boolean unpersistAttribute(GuanxiPrincipal principal, String attributeName);

  public void setLoggerConfig(Log4JLoggerConfig loggerConfig) { this.loggerConfig = loggerConfig; }
  public Log4JLoggerConfig getLoggerConfig() { return loggerConfig; }

  public void setLogger(Log4JLogger logger) { this.logger = logger; }
  public Log4JLogger getLogger() { return logger; }

  public void setServletContext(ServletContext servletContext) { this.servletContext = servletContext; }
}