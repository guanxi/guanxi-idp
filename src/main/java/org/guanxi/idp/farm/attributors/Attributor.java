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

package org.guanxi.idp.farm.attributors;

import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.GuanxiException;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.idp.util.ARPEngine;
import org.guanxi.idp.util.AttributeMap;

/**
 * <font size=5><b></b></font>
 *
 * @author Alistair Young alistair@smo.uhi.ac.uk
 */
public interface Attributor {
  public static final String NO_ERROR = "no_error";

  public void init();

  public void setAttributorConfig(String attributorConfig);
  public String getAttributorConfig();

  public void setMapper(AttributeMap mapper);
  public AttributeMap getMapper();

  public void setArpEngine(ARPEngine arpEngine);
  public ARPEngine getArpEngine();

  public void getAttributes(GuanxiPrincipal principal, String relyingParty, UserAttributesDocument.UserAttributes attributes) throws GuanxiException;

  public String getErrorMessage();
}
