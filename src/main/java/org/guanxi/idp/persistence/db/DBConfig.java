/* CVS Header
   $
   $
*/

package org.guanxi.idp.persistence.db;

/**
 * Persistence engine database configuration bean
 */
public class DBConfig {
  private String databaseName = null;
  private String tableName = null;
  private String fieldPrimaryKey = null;
  private String fieldUserid = null;
  private String fieldAttributeName = null;
  private String fieldAttributeValue = null;

  public String getDatabaseName() {
    return databaseName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getFieldPrimaryKey() {
    return fieldPrimaryKey;
  }

  public String getFieldUserid() {
    return fieldUserid;
  }

  public String getFieldAttributeName() {
    return fieldAttributeName;
  }

  public String getFieldAttributeValue() {
    return fieldAttributeValue;
  }

  public String getFieldRelyingParty() {
    return fieldRelyingParty;
  }

  private String fieldRelyingParty = null;

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public void setFieldPrimaryKey(String fieldPrimaryKey) {
    this.fieldPrimaryKey = fieldPrimaryKey;
  }

  public void setFieldUserid(String fieldUserid) {
    this.fieldUserid = fieldUserid;
  }

  public void setFieldAttributeName(String fieldAttributeName) {
    this.fieldAttributeName = fieldAttributeName;
  }

  public void setFieldAttributeValue(String fieldAttributeValue) {
    this.fieldAttributeValue = fieldAttributeValue;
  }

  public void setFieldRelyingParty(String fieldRelyingParty) {
    this.fieldRelyingParty = fieldRelyingParty;
  }
}
