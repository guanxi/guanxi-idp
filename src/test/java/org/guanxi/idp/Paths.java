package org.guanxi.idp;

import java.io.IOException;
import java.util.HashMap;
import java.io.File;

import static org.junit.Assert.fail;

/**
 * Conveneience class for working out test file paths
 */
public class Paths {
  private static String ROOT = null;
  private static HashMap<String, String> paths = null;

  static {
    try {
      ROOT = new File(".").getCanonicalPath() + "/src/main/webapp";
    }
    catch(IOException ioe) {
      fail(ioe.getMessage());
    }
    paths = new HashMap<String, String>();
    // Spring needs file:// prepended...
    paths.put("servlet.context.home", "file:///" + ROOT);
    paths.put("attributors.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/farm/attributors.xml");
    paths.put("arp-engine.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/farm/arp.xml");
    paths.put("mapper.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/farm/mapper.xml");
    paths.put("rules.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/farm/rules.xml");
    paths.put("persistence.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/farm/persistence.xml");
    paths.put("var-engine.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/farm/var-engine.xml");
    paths.put("ukFederationMetadataParser.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/application/jobs/ukFederationMetadataParser.xml");
    paths.put("entity.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/application/entity.xml");
    paths.put("persistence.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/farm/persistence.xml");
    paths.put("aa-service.xml", "file:///" + ROOT + "/WEB-INF/guanxi_idp/config/spring/services/shibboleth/aa-service.xml");

    // ...which breaks XMLBeans!
    paths.put("vars.xml", ROOT + "/WEB-INF/guanxi_idp/config/shared/vars.xml");
    paths.put("arp.xml", ROOT + "/WEB-INF/guanxi_idp/config/shared/arp.xml");
    paths.put("flatfile.xml", ROOT + "/WEB-INF/guanxi_idp/config/shared/flatfile.xml");
    paths.put("saml2map.xml", ROOT + "/WEB-INF/guanxi_idp/config/shared/saml2map.xml");
  }

  public static String path(String fileName) {
    return paths.get(fileName);
  }
}
