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
import org.guanxi.xal.idp.AttributorAttribute;
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

  /**
   * Retrieves attributes for a user from a database
   *
   * @param principal GuanxiPrincipal identifying the previously authenticated user
   * @param relyingParty The providerId of the relying party the attribute are for
   * @param attributes The document into whic to put the attributes
   * @throws GuanxiException if an error occurs
   */
  public void getAttributes(GuanxiPrincipal principal, String relyingParty, UserAttributesDocument.UserAttributes attributes) throws GuanxiException {
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
          if (arpEngine.release(relyingParty, attrName, attrValue)) {
            AttributorAttribute attribute = attributes.addNewAttribute();
            attribute.setName(attrName);
            attribute.setValue(attrValue);
            logger.debug("Released attribute " + attrName);
          }

          // Sort out any mappings. This will change the default name/value if necessary...
          if (mapper.map(principal, relyingParty, attrName, attrValue)) {
            for (int mapCount = 0; mapCount < mapper.getMappedNames().length; mapCount++) {
              logger.debug("Mapped attribute " + attrName + " to " + mapper.getMappedNames()[mapCount]);

              attrName = mapper.getMappedNames()[mapCount];
              attrValue = mapper.getMappedValues()[mapCount];

              // ...then run the original or mapped attribute through the ARP
              if (arpEngine.release(relyingParty, attrName, attrValue)) {
                AttributorAttribute attribute = attributes.addNewAttribute();
                attribute.setName(attrName);
                attribute.setValue(attrValue);

                logger.debug("Released attribute " + attrName);
              }
            } // for (int mapCount = 0; mapCount < mapper.getMappedNames().length; mapCount++) {
          } // if (mapper.map(principal.getProviderID(), attrName, attrValue)) {
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
