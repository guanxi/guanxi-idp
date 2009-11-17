package org.guanxi.idp.attribute;

import org.guanxi.idp.IdPTest;
import org.guanxi.idp.Paths;
import org.junit.BeforeClass;

/**
 * Base class for attribute tests
 */
public abstract class AttributeTest extends IdPTest {
  protected static final String TEST_DB_ATTRIBUTE_NAME = "testattr";
  protected static final String TEST_DB_ATTRIBUTE_VALUE = "testattrvalue";
  protected static final String SQL_CREATE_TABLE = "CREATE TABLE attrs " +
                                                   "(id integer not null primary key generated always as identity (start with 1, increment by 1), "+
                                                   "userid VARCHAR(10), email VARCHAR(255))";
  protected static final String SQL_INSERT_ROW = "INSERT INTO attrs (userid, email) VALUES " +
                                                 "('" + TEST_USER_NAME + "', " +
                                                 "'" + TEST_USER_EMAIL + "')";
  protected static final String SQL_DELETE_DB = "DELETE DATABASE dbattrtest";

  protected static String[] attributorConfigFiles = null;

  @BeforeClass
  public static void initAttributeTest() {
    attributorConfigFiles = new String[] {Paths.path("attributors.xml"),
                                          Paths.path("mapper.xml"),
                                          Paths.path("rules.xml"),
                                          Paths.path("persistence.xml"),
                                          Paths.path("var-engine.xml")};
  }
}
