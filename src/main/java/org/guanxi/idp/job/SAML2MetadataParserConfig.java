/* CVS Header
   $
   $
*/

package org.guanxi.idp.job;

import org.guanxi.common.job.SimpleGuanxiJobConfig;

public class SAML2MetadataParserConfig extends SimpleGuanxiJobConfig {
  /** Where the get the SAML2 metadata */
  private String metadataURL = null;

  public String getMetadataURL() { return metadataURL; }
  public void setMetadataURL(String metadataURL) { this.metadataURL = metadataURL; }
}
