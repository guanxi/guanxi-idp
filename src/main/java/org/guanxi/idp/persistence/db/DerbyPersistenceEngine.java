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

import org.guanxi.common.GuanxiException;

import java.sql.*;

/**
 * Apache Derby specific JDBC persistence engine
 */
public class DerbyPersistenceEngine extends JDBCPersistenceEngine {
  // Injected
  private String framework = null;
  private String databaseDirectory = null;

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
  protected void connect() throws GuanxiException {
    System.setProperty("derby.system.home", databaseDirectory);
    connectionString += ";create=true";
    super.connect();
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
  protected void disconnect() throws GuanxiException {
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

    try {
      if (dbConnection != null) {
        if (!dbConnection.isClosed()) dbConnection.close();
      }
    }
    catch (SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  protected boolean tableExists() throws GuanxiException {
    try {
      DatabaseMetaData meta = dbConnection.getMetaData();
      ResultSet tables = meta.getTables(null, null, null, null);

      while(tables.next()) {
        // Derby creates tables with uppercase names
        if (tables.getString("TABLE_NAME").equals(dbConfig.getTableName().toUpperCase())) {
          return true;
        }
      }

      return false;
    }
    catch(SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  protected void createTable() throws GuanxiException {
    String createString = "create table " + dbConfig.getTableName() + " (";
    createString += dbConfig.getFieldPrimaryKey() + " integer not null primary key generated always as identity (start with 1, increment by 1), ";
    createString += dbConfig.getFieldUserid() + " varchar(255) not null, ";
    createString += dbConfig.getFieldAttributeName() + " varchar(255) not null, ";
    createString += dbConfig.getFieldAttributeValue() + " varchar(255) not null, ";
    createString += dbConfig.getFieldRelyingParty() + " varchar(255) not null";
    createString += ")";

    try {
      Statement statement = dbConnection.createStatement();
      statement.executeUpdate(createString);
    }
    catch(SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  public void setFramework(String framework) { this.framework = framework; }
  public void setDatabaseDirectory(String databaseDirectory) { this.databaseDirectory = databaseDirectory; }
}
