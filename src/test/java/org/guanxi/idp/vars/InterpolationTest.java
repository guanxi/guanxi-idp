/* CVS Header
   $
   $
*/

package org.guanxi.idp.vars;

import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.guanxi.idp.util.VarEngine;
import org.guanxi.idp.Paths;

/**
 * Unit test for the VarEngine
 */
public class InterpolationTest extends VarsTest {
  @Test
  public void test() {
    XmlWebApplicationContext ctx = new XmlWebApplicationContext();

    ctx.setConfigLocations(varsTestConfigFiles);
    ctx.setServletContext(servletContext);
    ctx.refresh();

    VarEngine varEngine = (VarEngine)ctx.getBean("idpVarEngine");
    assertNotNull(varEngine);

    varEngine.setVarFile(Paths.path("vars.xml"));
    varEngine.init();

    assertEquals(varEngine.interpolate("eduPersonTargetedID"), "eduPersonTargetedID");
    assertEquals(varEngine.interpolate("${not.present}"), "${not.present}");
    assertEquals(varEngine.interpolate("${eduPersonTargetedID}"), "urn:mace:dir:attribute-def:eduPersonTargetedID");
  }
}
