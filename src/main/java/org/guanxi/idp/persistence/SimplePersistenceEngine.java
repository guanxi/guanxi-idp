/* CVS Header
   $
   $
*/

package org.guanxi.idp.persistence;

import org.apache.log4j.Logger;
import org.guanxi.common.log.Log4JLoggerConfig;
import org.guanxi.common.log.Log4JLogger;
import org.guanxi.common.GuanxiException;
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
  public abstract boolean attributeExists(String attributeName);
  public abstract String getAttributeValue(String attributeName);
  public abstract boolean persistAttribute(String attributeName, String attributeValue);
  public abstract boolean unpersistAttribute(String attributeName);

  public void setLoggerConfig(Log4JLoggerConfig loggerConfig) { this.loggerConfig = loggerConfig; }
  public Log4JLoggerConfig getLoggerConfig() { return loggerConfig; }

  public void setLogger(Log4JLogger logger) { this.logger = logger; }
  public Log4JLogger getLogger() { return logger; }

  public void setServletContext(ServletContext servletContext) { this.servletContext = servletContext; }
}
