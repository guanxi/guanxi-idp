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

package org.guanxi.idp.form;

public class RegisterSP {
  private String providerId = null;
  private String identity = null;
  private String creds = null;

  public String getProviderId() { return providerId; }
  public String getIdentity() { return identity; }
  public String getCreds() { return creds; }
  public void setProviderId(String providerId) { this.providerId = providerId; }
  public void setIdentity(String identity) { this.identity = identity; }
  public void setCreds(String creds) { this.creds = creds; }
}
