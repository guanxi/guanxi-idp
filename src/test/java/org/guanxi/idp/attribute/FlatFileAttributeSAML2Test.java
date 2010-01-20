package org.guanxi.idp.attribute;

import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.AttributorAttribute;
import org.guanxi.idp.util.VarEngine;
import org.guanxi.idp.util.AttributeMap;
import org.guanxi.idp.util.ARPEngine;
import org.guanxi.idp.Paths;
import org.guanxi.idp.farm.attributors.Attributor;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.definitions.EduPersonOID;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Test for SAML2 Web Browser SSO Profile attribute release process
 *
 * @author alistair
 */
public class FlatFileAttributeSAML2Test extends AttributeTest {
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

    AttributeMap mapper = (AttributeMap)ctx.getBean("saml2AttributeMapper");
    ARPEngine arpEngine = (ARPEngine)ctx.getBean("idpARPEngine");

    try {
      ffAttributor.getAttributes(principal, TEST_RELYING_PARTY, arpEngine, mapper, attributes);
      assertTrue(attributes.getAttributeArray().length > 0);

      boolean idEncrypted = false;
      boolean protectedApp_FirstName = false;
      boolean protectedApp_Surname = false;
      boolean protectedApp_Email = false;
      boolean mail = false;
      boolean eduPersonScopedAffiliation = false;

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
        if (attr.getName().equals(EduPersonOID.OID_MAIL)) {
          assertEquals(attr.getValue(), "alternative email");
          mail = true;
        }
        if (attr.getName().equals(EduPersonOID.OID_EDUPERSON_SCOPED_AFFILIATION)) {
          assertEquals(attr.getValue(), "staff@uni.ac.uk");
          eduPersonScopedAffiliation = true;
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
    }
    catch(GuanxiException ge) {
      fail(ge.getMessage());
    }
  }
}
