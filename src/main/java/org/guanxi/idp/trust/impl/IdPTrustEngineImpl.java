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

package org.guanxi.idp.trust.impl;

import org.guanxi.common.trust.TrustEngine;
import org.guanxi.common.metadata.Metadata;
import org.guanxi.common.metadata.SPMetadata;

/**
 * Identity Provider trust engine
 *
 * @author alistair
 */
public class IdPTrustEngineImpl implements TrustEngine {
  public IdPTrustEngineImpl() {
  }

  /**
   * Implements the rules an IdP must use when trusting an SP as defined by:
   * internet2-mace-shibboleth-arch-protocols-200509 : 3.1.1.3 Processing Rules
   *
   * @param entityMetadata the metadata for the SP
   * @param entityData a String containing the shire parameter from the intitial GET request
   *
   * @see org.guanxi.common.trust.TrustEngine#trustEntity(org.guanxi.common.metadata.Metadata, Object) */
  public boolean trustEntity(Metadata entityMetadata, Object entityData) {
    String shireURL = (String)entityData;
    return ((SPMetadata)entityData).getAssertionConsumerServiceURL().equals(shireURL);
  }
}
