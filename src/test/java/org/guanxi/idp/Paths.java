package org.guanxi.idp;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;

/**
 * Conveneience class for working out test file paths
 */
public class Paths {
  private static final String ROOT = "/contextroot/WEB-INF/guanxi_idp/config";
  private static HashMap<String, String> paths = null;

  static {
    paths = new HashMap<String, String>();
    try {
      // Spring needs file:// prepended...
      paths.put("servlet.context.home", "file:///" + new File(Paths.class.getResource("/contextroot").getPath()).getCanonicalPath());
      paths.put("attributors.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/farm/attributors.xml").getPath()).getCanonicalPath());
      paths.put("arp-engine.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/farm/arp.xml").getPath()).getCanonicalPath());
      paths.put("mapper.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/farm/mapper.xml").getPath()).getCanonicalPath());
      paths.put("rules.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/farm/rules.xml").getPath()).getCanonicalPath());
      paths.put("persistence.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/farm/persistence.xml").getPath()).getCanonicalPath());
      paths.put("var-engine.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/farm/var-engine.xml").getPath()).getCanonicalPath());
      paths.put("ukFederationMetadataParser.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/application/jobs/ukFederationMetadataParser.xml").getPath()).getCanonicalPath());
      paths.put("entity.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/application/entity.xml").getPath()).getCanonicalPath());
      paths.put("persistence.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/farm/persistence.xml").getPath()).getCanonicalPath());
      paths.put("aa-service.xml", "file:///" + new File(Paths.class.getResource(ROOT + "/spring/services/shibboleth/aa-service.xml").getPath()).getCanonicalPath());

      // ...which breaks XMLBeans!
      paths.put("vars.xml", new File(Paths.class.getResource(ROOT + "/shared/vars.xml").getPath()).getCanonicalPath());
      paths.put("arp.xml", new File(Paths.class.getResource(ROOT + "/shared/arp.xml").getPath()).getCanonicalPath());
      paths.put("flatfile.xml", new File(Paths.class.getResource(ROOT + "/shared/flatfile.xml").getPath()).getCanonicalPath());
    }
    catch(IOException ioe) {
    }
  }

  public static String path(String fileName) {
    return paths.get(fileName);
  }
}
