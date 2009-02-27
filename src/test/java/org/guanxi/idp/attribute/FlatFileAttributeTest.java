/* CVS Header
   $
   $
*/

package org.guanxi.idp.attribute;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.AttributorAttribute;
import org.guanxi.idp.farm.attributors.Attributor;
import org.guanxi.idp.util.VarEngine;
import org.guanxi.idp.Paths;
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

    VarEngine varEngine = (VarEngine)ctx.getBean("idpVarEngine");
    varEngine.setVarFile(Paths.path("vars.xml"));
    varEngine.init();

    Attributor ffAttributor = (Attributor)ctx.getBean("flatFileAttributor");
    ffAttributor.setAttributorConfig(Paths.path("flatfile.xml"));
    ffAttributor.getArpEngine().setArpFile(Paths.path("arp.xml"));
    ffAttributor.getArpEngine().init();
    ffAttributor.init();
    
    try {
      ffAttributor.getAttributes(principal, TEST_RELYING_PARTY, attributes);
      assertTrue(attributes.getAttributeArray().length > 0);

      boolean idEncrypted = false;
      boolean protectedApp_FirstName = false;
      boolean protectedApp_Surname = false;
      boolean protectedApp_Email = false;
      boolean mail = false;
      boolean eduPersonScopedAffiliation = false;
      boolean memberOf = false;

      AttributorAttribute[] attrs = attributes.getAttributeArray();
      for (AttributorAttribute attr : attrs) {
        if (attr.getName().equals("idEncrypted")) {
          assertEquals(attr.getValue(), "8C2D18A23E6CC5109117BD7F2A0D40A4");
          idEncrypted = true;
        }
        if (attr.getName().equals("protectedApp_FirstName")) {
          assertEquals(attr.getValue(), "Harry");
          protectedApp_FirstName = true;
        }
        if (attr.getName().equals("protectedApp_Surname")) {
          assertEquals(attr.getValue(), "McDesperate");
          protectedApp_Surname = true;
        }
        if (attr.getName().equals("protectedApp_Email")) {
          assertEquals(attr.getValue(), "HarryMcD@jumpingupanddown.net");
          protectedApp_Email = true;
        }
        if (attr.getName().equals("mail")) {
          assertEquals(attr.getValue(), "alternative email");
          mail = true;
        }
        if (attr.getName().equals("urn:mace:dir:attribute-def:eduPersonScopedAffiliation")) {
          assertEquals(attr.getValue(), "staff@uni.ac.uk");
          eduPersonScopedAffiliation = true;
        }
        if (attr.getName().equals("memberOf")) {
          assertEquals(attr.getValue(), "heidbangers");
          memberOf = true;
        }
        if (attr.getName().equals("function")) {
          fail("Found the attribute known as 'function' - it shouldn't be there!");
        }
      }

      // Did we find all the correct attributes?
      assertTrue(idEncrypted);
      assertTrue(protectedApp_FirstName);
      assertTrue(protectedApp_Surname);
      assertTrue(protectedApp_Email);
      assertTrue(mail);
      assertTrue(eduPersonScopedAffiliation);
      assertTrue(memberOf);
    }
    catch(GuanxiException ge) {
      fail(ge.getMessage());
    }
  }
}
