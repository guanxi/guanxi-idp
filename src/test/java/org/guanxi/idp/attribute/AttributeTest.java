/* CVS Header
   $
   $
*/

package org.guanxi.idp.attribute;

import org.guanxi.idp.IdPTest;
import org.guanxi.idp.Paths;
import org.junit.BeforeClass;

/**
 * Base class for attribute tests
 */
public abstract class AttributeTest extends IdPTest {
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
