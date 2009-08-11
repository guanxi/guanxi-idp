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

package org.guanxi.idp.service.shibboleth;

import org.guanxi.common.GuanxiPrincipalFactory;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.Utils;

import javax.servlet.http.HttpServletRequest;

public class ShibbolethGuanxiPrincipalFactory implements GuanxiPrincipalFactory {
  private String cookieName = null;

  public GuanxiPrincipal createNewGuanxiPrincipal(HttpServletRequest request) {
    GuanxiPrincipal gxPrincipal = new GuanxiPrincipal();

    // Set the session ID... This will be the NameIdentifier
    gxPrincipal.setUniqueId(Utils.getUniqueID().replaceAll(":", "--"));

    return gxPrincipal;
  }

  public void setCookieName(String cookieName) { this.cookieName = cookieName; }
  public String getCookieName() { return cookieName; }
}
