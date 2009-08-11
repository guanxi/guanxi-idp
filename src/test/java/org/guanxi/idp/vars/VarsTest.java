/* CVS Header
   $
   $
*/

package org.guanxi.idp.vars;

import org.junit.BeforeClass;
import org.guanxi.idp.IdPTest;
import org.guanxi.idp.Paths;

/**
 * Base class for vars tests
 */
public abstract class VarsTest extends IdPTest {
  protected static String[] varsTestConfigFiles = null;
  
  @BeforeClass
  public static void initVarsTest() {
    varsTestConfigFiles = new String[] {Paths.path("var-engine.xml")};
  }
}
