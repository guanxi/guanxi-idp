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

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.xal.saml_1_0.protocol.ResponseDocument;
import org.springframework.web.context.ServletContextAware;

public abstract class SimpleIdPFilter implements IdPFilter, ServletContextAware {
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  ServletContext servletContext = null;
  /** Our logger */
  protected Logger logger = null;

  public void init() {
    logger = Logger.getLogger(this.getClass().getName());
  }

  public abstract void filter(GuanxiPrincipal principal, String relyingParty,
                              ResponseDocument ssoResponseDoc);

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }
}
