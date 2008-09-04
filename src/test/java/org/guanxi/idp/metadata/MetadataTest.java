/* CVS Header
   $
   $
*/

package org.guanxi.idp.metadata;

import org.guanxi.idp.IdPTest;
import org.junit.BeforeClass;

public abstract class MetadataTest extends IdPTest {
  protected static String[] metadataConfigFiles = null;
  
  @BeforeClass
  public static void initMetadataTest() {
    metadataConfigFiles = new String[] {idpHome + "/WEB-INF/guanxi_idp/config/spring/application/jobs/ukFederationMetadataParser.xml",
                                        idpHome + "/WEB-INF/guanxi_idp/config/spring/application/entity.xml"};
  }
}
