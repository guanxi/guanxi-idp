/* CVS Header
   $
   $
*/

package org.guanxi.idp.attribute;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.AttributorAttribute;
import org.guanxi.idp.farm.attributors.Attributor;
import org.guanxi.common.GuanxiException;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Unit test for the FlatFileAttributor
 */
public class FlatFileAttributeTest extends AttributeTest {
  @Test
  public void test() {
    UserAttributesDocument attributesDoc = UserAttributesDocument.Factory.newInstance();
    UserAttributesDocument.UserAttributes attributes = attributesDoc.addNewUserAttributes();
    
    XmlWebApplicationContext ctx = new XmlWebApplicationContext();

    ctx.setConfigLocations(attributorConfigFiles);
    ctx.setServletContext(servletContext);
    ctx.refresh();

    Attributor ffAttributor = (Attributor)ctx.getBean("flatFileAttributor");
    try {
      ffAttributor.getAttributes(principal, attributes);
      assertTrue(attributes.getAttributeArray().length > 0);

      AttributorAttribute[] attrs = attributes.getAttributeArray();
      for (AttributorAttribute attr : attrs) {
        if (attr.getName().equals("idEncrypted")) {
          assertEquals(attr.getValue(), "8C2D18A23E6CC5109117BD7F2A0D40A4");
        }
        if (attr.getName().equals("protectedApp_FirstName")) {
          assertEquals(attr.getValue(), "Harry");
        }
        if (attr.getName().equals("protectedApp_Surname")) {
          assertEquals(attr.getValue(), "McDesperate");
        }
        if (attr.getName().equals("protectedApp_Email")) {
          assertEquals(attr.getValue(), "HarryMcD@jumpingupanddown.net");
        }
        if (attr.getName().equals("mail")) {
          assertEquals(attr.getValue(), "alternative email");
        }
        if (attr.getName().equals("urn:mace:dir:attribute-def:eduPersonScopedAffiliation")) {
          assertEquals(attr.getValue(), "staff@uni.ac.uk");
        }
      }
    }
    catch(GuanxiException ge) {
      fail(ge.getMessage());
    }
  }
}
