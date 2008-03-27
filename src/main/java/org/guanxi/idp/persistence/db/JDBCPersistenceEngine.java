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
 * JDBC implementation of a PersistenceEngine.
 */
public class JDBCPersistenceEngine extends SimplePersistenceEngine {
  protected static final String WILDCARD = "__WILDCARD__";
  
  // Injected
  protected String driver = null;
  protected String databaseName = null;
  protected String usernameProperty = null;
  protected String usernameValue = null;
  protected String passwordProperty = null;
  protected String passwordValue = null;
  protected String tableName = null;
  protected String fieldPrimaryKey = null;
  protected String fieldUserid = null;
  protected String fieldAttributeName = null;
  protected String fieldAttributeValue = null;
  protected String fieldRelyingParty = null;
  protected String connectionString = null;
  protected Connection dbConnection = null;

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

  protected void connect() throws GuanxiException {
    try {
      Class.forName(driver).newInstance();

      Properties props = new Properties();
      props.put(usernameProperty, usernameValue);
      props.put(passwordProperty, passwordValue);

      dbConnection = DriverManager.getConnection(connectionString, props);
    }
    catch (Exception e) {
      throw new GuanxiException(e);
    }
  }

  protected void disconnect() throws GuanxiException {
    try {
      if (dbConnection != null) {
        if (!dbConnection.isClosed()) dbConnection.close();
      }
    }
    catch (SQLException sqle) {
      throw new GuanxiException(sqle);
    }
  }

  protected ResultSet query(String query) throws GuanxiException {
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

  protected void insert(String userid, String attributeName, String attributeValue, String relyingParty) throws GuanxiException {
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

  protected void update(String userid, String attributeName, String attributeValue, String relyingParty) throws GuanxiException {
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

  protected void delete(String userid, String attributeName, String relyingParty) throws GuanxiException {
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

  protected String[] getField(String tableName, String[] columnNames, String[] columnValues, String fieldName) throws GuanxiException {
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

  protected boolean tableExists() throws GuanxiException {
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

  protected void createTable() throws GuanxiException {
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
  public void setTableName(String tableName) { this.tableName = tableName; }
  public void setFieldPrimaryKey(String fieldPrimaryKey) { this.fieldPrimaryKey = fieldPrimaryKey; }
  public void setFieldUserid(String fieldUserid) { this.fieldUserid = fieldUserid; }
  public void setFieldAttributeName(String fieldAttributeName) { this.fieldAttributeName = fieldAttributeName; }
  public void setFieldAttributeValue(String fieldAttributeValue) { this.fieldAttributeValue = fieldAttributeValue; }
  public void setFieldRelyingParty(String fieldRelyingParty) { this.fieldRelyingParty = fieldRelyingParty; }
  public void setConnectionString(String connectionString) { this.connectionString = connectionString; }
  public void setUsernameProperty(String usernameProperty) { this.usernameProperty = usernameProperty; }
  public void setUsernameValue(String usernameValue) { this.usernameValue = usernameValue; }
  public void setPasswordProperty(String passwordProperty) { this.passwordProperty = passwordProperty; }
  public void setPasswordValue(String passwordValue) { this.passwordValue = passwordValue; }
}
