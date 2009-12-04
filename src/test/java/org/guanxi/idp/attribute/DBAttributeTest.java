/* CVS Header
   $
   $
*/

package org.guanxi.idp.attribute;

import org.junit.Test;
import static org.junit.Assert.*;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.AttributorAttribute;
import org.guanxi.idp.util.VarEngine;
import org.guanxi.idp.Paths;
import org.guanxi.idp.farm.attributors.Attributor;
import org.guanxi.common.GuanxiException;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.sql.*;
import java.io.File;

/**
 * Unit test for the JDBCAttributor
 */
public class DBAttributeTest extends AttributeTest {
  @Test
  public void test() {
    File derbyDir = null;
    Connection derbyConnection = null;
    Statement derbyStatement = null;

    try {
      derbyDir = new File(new File(".").getCanonicalPath() + System.getProperty("file.separator") + "dbattrtest");
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
      derbyConnection = DriverManager.getConnection("jdbc:derby:dbattrtest;create=true;user=user;password=passwd");
      derbyStatement = derbyConnection.createStatement();
      derbyStatement.executeUpdate(SQL_CREATE_TABLE);
      derbyStatement.executeUpdate(SQL_INSERT_ROW);
      derbyStatement.close();
      derbyConnection.close();
    }
    catch(Exception e) {
      fail(e.getMessage());
    }
    
    UserAttributesDocument attributesDoc = UserAttributesDocument.Factory.newInstance();
    UserAttributesDocument.UserAttributes attributes = attributesDoc.addNewUserAttributes();

    XmlWebApplicationContext ctx = new XmlWebApplicationContext();

    ctx.setConfigLocations(attributorConfigFiles);
    ctx.setServletContext(servletContext);
    ctx.refresh();

    VarEngine varEngine = (VarEngine)ctx.getBean("idpVarEngine");
    varEngine.setVarFile(Paths.path("vars.xml"));
    varEngine.init();

    Attributor dbAttributor = (Attributor)ctx.getBean("dbAttributor");
    dbAttributor.getArpEngine().setArpFile(Paths.path("arp.xml"));
    dbAttributor.getArpEngine().init();
    dbAttributor.init();

    boolean idEncrypted = false;
    boolean email = false;
    
    try {
      dbAttributor.getAttributes(principal, TEST_RELYING_PARTY, attributes);
      assertTrue(attributes.getAttributeArray().length > 0);
      AttributorAttribute[] releasedAttributes = attributes.getAttributeArray();
      for (AttributorAttribute releasedAttribute : releasedAttributes) {
        if (releasedAttribute.getName().equals("idEncrypted")) {
          assertEquals(releasedAttribute.getValue(), "C4CA4238A0B923820DCC509A6F75849B");
          idEncrypted = true;
        }
        if (releasedAttribute.getName().equals("protectedApp_Email")) {
          assertEquals(releasedAttribute.getValue(), TEST_USER_EMAIL);
          email = true;
        }
      }
      assertTrue(idEncrypted);
      assertTrue(email);
    }
    catch(GuanxiException ge) {
      fail(ge.getMessage());
    }
    finally {
      try {
        deleteDir(derbyDir);
      }
      catch(Exception e) {
        fail(e.getMessage());
      }
    }
  }

  // http://www.exampledepot.com/egs/java.io/DeleteDir.html
  private boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i=0; i<children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
      }
    }
  }

  // The directory is now empty so delete it
  return dir.delete();
  }
}
