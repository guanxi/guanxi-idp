/* CVS Header
   $
   $
*/

package org.guanxi.idp.job;

import org.guanxi.common.job.SimpleGuanxiJobConfig;

public class SAML2MetadataParserConfig extends SimpleGuanxiJobConfig {
  /** Where the get the SAML2 metadata */
  private String metadataURL = null;
  /** The value of User-Agent to set */
  private String who = null;

  public void setMetadataURL(String metadataURL) { this.metadataURL = metadataURL; }
  public String getMetadataURL() { return metadataURL; }

  public void setWho(String who) { this.who = who; }
  public String getWho() { return who; }
}
