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

package org.guanxi.idp.persistence.db;

import org.guanxi.idp.persistence.SimplePersistenceEngine;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC implementation of a PersistenceEngine
 */
public class JDBCPersistenceEngine extends SimplePersistenceEngine {
  // Injected
  private String framework = null;
  private String driver = null;
  private String databaseName = null;
  private String protocol = null;
  private String username = null;
  private String password = null;

  private Connection dbConnection = null;
  private Properties props = null;

  public void init() {
    super.init();

    loadDriver();

    props = new Properties();
    // Providing a user name and password is optional in the embedded and derbyclient frameworks
    props.put("user", username);
    props.put("password", password);

    try {
      dbConnection = DriverManager.getConnection(protocol + databaseName + ";create=true", props);
    }
    catch(SQLException sqle) {
      log.error("Error connecting to database", sqle);
    }
  }

  public void destroy() {
    shutdownDatabase();
  }
  
  public boolean attributeExists(String attributeName) {
    return false;
  }

  public String getAttributeValue(String attributeName) {
    return null;
  }

  public boolean persistAttribute(String attributeName, String attributeValue) {
    return false;
  }

  public boolean unpersistAttribute(String attributeName) {
    return false;
  }

  /*
   *  The JDBC driver is loaded by loading its class.
   *  If you are using JDBC 4.0 (Java SE 6) or newer, JDBC drivers may
   *  be automatically loaded, making this code optional.
   *
   *  In an embedded environment, this will also start up the Derby
   *  engine (though not any databases), since it is not already
   *  running. In a client environment, the Derby engine is being run
   *  by the network server framework.
   *
   *  In an embedded environment, any static Derby system properties
   *  must be set before loading the driver to take effect.
   */
  private void loadDriver() {
    try {
      Class.forName(driver).newInstance();
    }
    catch (ClassNotFoundException cnfe) {
    }
    catch (InstantiationException ie) {
    }
    catch (IllegalAccessException iae) {
    }
  }

  /*
   * In embedded mode, an application should shut down the database.
   * If the application fails to shut down the database,
   * Derby will not perform a checkpoint when the JVM shuts down.
   * This means that it will take longer to boot (connect to) the
   * database the next time, because Derby needs to perform a recovery
   * operation.
   *
   * It is also possible to shut down the Derby system/engine, which
   * automatically shuts down all booted databases.
   *
   * Explicitly shutting down the database or the Derby engine with
   * the connection URL is preferred. This style of shutdown will
   * always throw an SQLException.
   *
   * Not shutting down when in a client environment, see method
   * Javadoc.
   */
  private void shutdownDatabase() {
    if (framework.equals("embedded")) {
      try {
        DriverManager.getConnection("jdbc:derby:;shutdown=true");
      }
      catch (SQLException sqle) {
        if (((sqle.getErrorCode() == 50000) && ("XJ015".equals(sqle.getSQLState())))) {
          /* We got the expected exception.
           * Note that for single database shutdown, the expected SQL state is "08006"
           * and the error code is 45000.
           */
          log.info("Derby shut down normally");
        }
        else {
          /* if the error code or SQLState is different, we have an unexpected exception
           * (shutdown failed)
           */
          log.error("Derby did not shut down normally", sqle);
        }
      }
    }
  }

  public void setDriver(String driver) { this.driver = driver; }
  public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
  public void setProtocol(String protocol) { this.protocol = protocol; }
  public void setUsername(String username) { this.username = username; }
  public void setPassword(String password) { this.password = password; }
  public void setFramework(String framework) { this.framework = framework; }
}
