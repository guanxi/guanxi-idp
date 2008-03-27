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
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.GuanxiException;

import java.sql.*;
import java.util.Properties;
import java.util.Vector;

/**
 * JDBC implementation of a PersistenceEngine
 *
 * DB schema:
 * id userid attribute_name attribute_value relying_party
 */
public class JDBCPersistenceEngine extends SimplePersistenceEngine {
  private static final String WILDCARD = "__WILDCARD__";
  
  // Injected
  private String framework = null;
  private String driver = null;
  private String databaseName = null;
  private String protocol = null;
  private String username = null;
  private String password = null;
  private String databaseDirectory = null;
  private String tableName = null;
  private String fieldPrimaryKey = null;
  private String fieldUserid = null;
  private String fieldAttributeName = null;
  private String fieldAttributeValue = null;
  private String fieldRelyingParty = null;

  private Connection dbConnection = null;

  public void init() {
    super.init();

    try {
      connect();
      if (!tableExists()) {
        createTable();
      }
    }
    catch(GuanxiException ge) {
      log.error("Error connecting to the database", ge);
    }
  }

  public void destroy() {
    try {
      disconnect();
    }
    catch(GuanxiException ge) {
      log.error("Error disconnecting from the database", ge);
    }
  }
  
  public boolean attributeExists(GuanxiPrincipal principal, String attributeName) {
    String[] columnNames = new String[] {fieldUserid, fieldAttributeName, fieldRelyingParty};
    String[] columnValues = new String[] {principal.getName(), attributeName, principal.getRelyingPartyID()};

    try {
      String[] attributes = getField(tableName, columnNames, columnValues, fieldAttributeValue);
      if (attributes.length > 0)
        return true;
      else
        return false;
    }
    catch(GuanxiException ge) {
      log.error("attributeExists error", ge);
      return false;
    }
  }

  public String getAttributeValue(GuanxiPrincipal principal, String attributeName) {
    String[] columnNames = new String[] {fieldUserid, fieldAttributeName, fieldRelyingParty};
    String[] columnValues = new String[] {principal.getName(), attributeName, principal.getRelyingPartyID()};

    try {
      String[] attributes = getField(tableName, columnNames, columnValues, fieldAttributeValue);
      if (attributes.length > 0) {
        return attributes[0];
      }
      else
        return null;
    }
    catch(GuanxiException ge) {
      log.error("getAttributeValue error", ge);
      return null;
    }
  }

  public boolean persistAttribute(GuanxiPrincipal principal, String attributeName, String attributeValue) {
    try {
      if (attributeExists(principal, attributeName)) {
        update(principal.getName(), attributeName, attributeValue, principal.getRelyingPartyID());
      }
      else {
        insert(principal.getName(), attributeName, attributeValue, principal.getRelyingPartyID());
      }
      return true;
    }
    catch(GuanxiException ge) {
      log.error("persistAttribute error", ge);
      return false;
    }
  }

  public boolean unpersistAttribute(GuanxiPrincipal principal, String attributeName) {
    try {
      delete(principal.getName(), attributeName, principal.getRelyingPartyID());
      return true;
    }
    catch(GuanxiException ge) {
      log.error("unpersistAttribute error", ge);
      return false;
    }
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
  private void connect() throws GuanxiException {
    try {
      Class.forName(driver).newInstance();

      Properties props = null;props = new Properties();
      // Providing a user name and password is optional in the embedded and derbyclient frameworks
      props.put("user", username);
      props.put("password", password);

      dbConnection = DriverManager.getConnection(protocol + databaseName + ";create=true", props);
    }
    catch (Exception e) {
      throw new GuanxiException(e);
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
  private void disconnect() throws GuanxiException {
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

  private ResultSet query(String query) throws GuanxiException {
    if (dbConnection == null) {
      throw new GuanxiException("query cannot be performed : Not connected to the database");
    }

    try {
      String sql = dbConnection.nativeSQL(query);
      Statement statement = dbConnection.createStatement();
      return statement.executeQuery(sql);
    }
    catch (SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  private void insert(String userid, String attributeName, String attributeValue, String relyingParty) throws GuanxiException {
    String insertString = "insert into " + tableName + " (" + fieldUserid + ", " + fieldAttributeName + ", " + fieldAttributeValue + ", " + fieldRelyingParty + ") ";
    insertString += " values(";
    insertString += "'" + userid + "',";
    insertString += "'" + attributeName + "',";
    insertString += "'" + attributeValue + "',";
    insertString += "'" + relyingParty + "'";
    insertString += ")";

    try {
      Statement statement = dbConnection.createStatement();
      statement.executeUpdate(insertString);
    }
    catch(SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  private void update(String userid, String attributeName, String attributeValue, String relyingParty) throws GuanxiException {
    String updateString = "update " + tableName + "set ";
    updateString += fieldAttributeValue + " = '" + attributeValue + "' ";
    updateString += "where ";
    updateString += fieldUserid + " = '" + userid + "' and";
    updateString += fieldAttributeName + " = '" + attributeName + "' and";
    updateString += fieldRelyingParty + " = '" + relyingParty + "'";

    try {
      Statement statement = dbConnection.createStatement();
      statement.executeUpdate(updateString);
    }
    catch(SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  private void delete(String userid, String attributeName, String relyingParty) throws GuanxiException {
    String deleteString = "delete from " + tableName + " where ";
    deleteString += fieldUserid + " = '" + userid + "' and ";
    deleteString += fieldAttributeName + " = '" + attributeName + "' and ";
    deleteString += fieldRelyingParty + " = '" + relyingParty + "'";

    try {
      Statement statement = dbConnection.createStatement();
      statement.executeUpdate(deleteString);
    }
    catch(SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  public String[] getField(String tableName, String[] columnNames, String[] columnValues, String fieldName) throws GuanxiException {
    String[] fields = null;

    String queryString = "select * from " + tableName + " where ";
    for (int c=0; c < columnNames.length; c++) {
      String op = null;
      if (columnValues[c].equals(WILDCARD)) op = " like ";
      else op = "=";

      if (c == 0)
        queryString += columnNames[c] + "" + op + "'" + columnValues[c] + "'";
      else
        queryString += " and " + columnNames[c] + "" + op + "'" + (columnValues[c].equals(WILDCARD) ? "%" : columnValues[c]) + "'";
    }

    ResultSet results = query(queryString);
    if (results != null) {
      Vector<String> buffer = new Vector<String>();
      try {
        while (results.next()) {
          buffer.add(results.getString(fieldName));
        }

        results.close();

        fields = new String[buffer.size()];
        buffer.copyInto(fields);
      }
      catch(SQLException sqle) {
        throw new GuanxiException(sqle);
      }
    }

    return fields;
  }

  private boolean tableExists() throws GuanxiException {
    try {
      DatabaseMetaData meta = dbConnection.getMetaData();
      ResultSet tables = meta.getTables(null, null, null, null);
      
      while(tables.next()) {
        // Derby creates tables with uppercase names
        if (tables.getString("TABLE_NAME").equals(tableName.toUpperCase())) {
          return true;
        }
      }

      return false;
    }
    catch(SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  private void createTable() throws GuanxiException {
    String createString = "create table " + tableName + " (";
    createString += fieldPrimaryKey + " integer not null primary key generated always as identity (start with 1, increment by 1), ";
    createString += fieldUserid + " varchar(255) not null, ";
    createString += fieldAttributeName + " varchar(255) not null, ";
    createString += fieldAttributeValue + " varchar(255) not null, ";
    createString += fieldRelyingParty + " varchar(255) not null";
    createString += ")";

    try {
      Statement statement = dbConnection.createStatement();
      statement.executeUpdate(createString);
    }
    catch(SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  public void setDriver(String driver) { this.driver = driver; }
  public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
  public void setProtocol(String protocol) { this.protocol = protocol; }
  public void setUsername(String username) { this.username = username; }
  public void setPassword(String password) { this.password = password; }
  public void setFramework(String framework) { this.framework = framework; }
  public void setDatabaseDirectory(String databaseDirectory) { this.databaseDirectory = databaseDirectory; }
  public void setTableName(String tableName) { this.tableName = tableName; }
  public void setFieldPrimaryKey(String fieldPrimaryKey) { this.fieldPrimaryKey = fieldPrimaryKey; }
  public void setFieldUserid(String fieldUserid) { this.fieldUserid = fieldUserid; }
  public void setFieldAttributeName(String fieldAttributeName) { this.fieldAttributeName = fieldAttributeName; }
  public void setFieldAttributeValue(String fieldAttributeValue) { this.fieldAttributeValue = fieldAttributeValue; }
  public void setFieldRelyingParty(String fieldRelyingParty) { this.fieldRelyingParty = fieldRelyingParty; }
}
