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

package org.guanxi.idp.farm.attributors;

import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.GuanxiException;

import java.sql.*;
import java.util.ArrayList;

/**
 * <h1>JDBCAttributor</h1>
 * Attributor implementation that gets it's attribute information from a database.
 *
 * @author Alistair Young
 */
public class JDBCAttributor extends SimpleAttributor {
  private String driverClass = null;
  private String connectionString = null;
  private String dbUsername = null;
  private String dbPassword = null;
  private String query = null;
  private Connection dbConnection = null;

  public void init() {
    super.init();
    try {
      Class.forName(driverClass).newInstance ();
      dbConnection = DriverManager.getConnection(connectionString, dbUsername, dbPassword);
    }
    catch(ClassNotFoundException cnfe) {
      logger.error("Can't find the JDBC class", cnfe);
    }
    catch(IllegalAccessException iae) {
      logger.error("JDBC class access problem", iae);
    }
    catch(InstantiationException ie) {
      logger.error("Can't load the JDBC class", ie);
    }
    catch(SQLException se) {
      logger.error("Can't access the database", se);
    }
  }

  /** @see SimpleAttributor#getAttributes(org.guanxi.common.GuanxiPrincipal, String, org.guanxi.xal.idp.UserAttributesDocument.UserAttributes) */
  public void getAttributes(GuanxiPrincipal principal, String relyingParty,
                            UserAttributesDocument.UserAttributes attributes) throws GuanxiException {
    try {
      String userQuery = query.replaceAll("__USERID__", principal.getName());

      Statement statement = dbConnection.createStatement();
      ResultSet results = statement.executeQuery(userQuery);
      ResultSetMetaData resultsMetadata = results.getMetaData();
      ArrayList<String> columnNames = new ArrayList<String>();
      for (int c = 0; c < resultsMetadata.getColumnCount(); c++) {
        columnNames.add(resultsMetadata.getColumnName(c+1));
      }

      while (results.next()) {
        for (String columnName : columnNames) {
          String attrName = columnName.toLowerCase();
          String attrValue = results.getString(columnName);

          // Can we release the original attributes without mapping?
          arp(relyingParty, attrName, attrValue, attributes);

          // Sort out any mappings. This will change the default name/value if necessary
          map(principal, relyingParty, attrName, attrValue, attributes);
        }
      }

      results.close();
      statement.close();
    }
    catch(SQLException se) {
      logger.error("Can't query the database", se);
      throw new GuanxiException(se);
    }
  }

  // Setters
  public void setDriverClass(String driverClass) { this.driverClass = driverClass; }
  public void setConnectionString(String connectionString) { this.connectionString = connectionString; }
  public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }
  public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
  public void setQuery(String query) { this.query = query; }
}
