//: "The contents of this file are subject to the Mozilla Public License
//: Version 1.1 (the "License"); you may not use this file except in
//: compliance with the License. You may obtain a copy of the License at
//: http://www.mozilla.org/MPL/
//:
//: Software distributed under the License is distributed on an "AS IS"
//: basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//: License for the specific language governing rights and limitations
//: under the License.
//:
//: The Original Code is Guanxi (http://www.guanxi.uhi.ac.uk).
//:
//: The Initial Developer of the Original Code is Alistair Young alistair@codebrane.com
//: All Rights Reserved.
//:

package org.guanxi.idp.farm.filters;

import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.xal.saml_1_0.protocol.ResponseDocument;

public class UserAccountabilityFilter extends SimpleIdPFilter {
  /** The delimiter to use in the log file */
  private String delimiter = null;

  public void setDelimiter(String delimiter) { this.delimiter = delimiter; }
  public String getDelimiter() { return delimiter; }

  public void init() {
    super.init();
  }
  
  public void filter(GuanxiPrincipal principal, ResponseDocument ssoResponseDoc) {
    log.info(principal.getUniqueId() + delimiter +            // NameIdentifier
             principal.getName() + delimiter +          // Username
             principal.getRelyingPartyID() + delimiter +    // SP id
             principal.getPrivateProfileDataEntry("dn") + delimiter +      // User's DN
             ssoResponseDoc.getResponse().getAssertionArray(0).getIssueInstant());  // Time of issue
  }
}
