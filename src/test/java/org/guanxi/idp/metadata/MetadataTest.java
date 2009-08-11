/* CVS Header
   $
   $
*/

package org.guanxi.idp.metadata;

import org.guanxi.idp.IdPTest;
import org.guanxi.idp.Paths;
import org.junit.BeforeClass;

public abstract class MetadataTest extends IdPTest {
  protected static String[] metadataConfigFiles = null;
  
  @BeforeClass
  public static void initMetadataTest() {
    metadataConfigFiles = new String[] {Paths.path("ukFederationMetadataParser.xml"),
                                        Paths.path("entity.xml")};
  }
}
