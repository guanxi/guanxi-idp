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

package org.guanxi.idp.persistence;

import org.guanxi.idp.IdPTest;
import org.guanxi.idp.Paths;
import org.junit.BeforeClass;

/**
 * Base class for persistence tests
 */
public abstract class PersistenceTest extends IdPTest {
  protected static final String TEST_ATTRIBUTE_NAME = "eduPersonTargetedId";
  protected static final String TEST_ATTRIBUTE_VALUE = "666";

  protected static String[] persistenceConfigFiles = null;

  @BeforeClass
  public static void initPersistenceTest() {
    persistenceConfigFiles = new String[] {Paths.path("persistence.xml")};
  }
}
