/* CVS Header
   $
   $
*/

package org.guanxi.idp.persistence.db;

import org.guanxi.common.GuanxiException;

import java.sql.Statement;
import java.sql.SQLException;

/**
 * Postgres specific JDBC persistence engine
 */
public class PostgresPersistenceEngine extends JDBCPersistenceEngine {
  protected void createTable() throws GuanxiException {
    String createString = "create table " + dbConfig.getTableName() + " (";
    createString += dbConfig.getFieldPrimaryKey() + " SERIAL PRIMARY KEY, ";
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
}
