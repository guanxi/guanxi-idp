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

import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Test to exercise attribute persistence
 */
public class AttributePersistenceTest extends PersistenceTest {
  @Test
  public void test() {
    XmlWebApplicationContext ctx = new XmlWebApplicationContext();

    ctx.setConfigLocations(persistenceFiles);
    ctx.setServletContext(servletContext);
    ctx.refresh();

    PersistenceEngine engine = (PersistenceEngine)ctx.getBean("idpPersistenceEngine");

    assertFalse(engine.attributeExists(principal, TEST_RELYING_PARTY, TEST_ATTRIBUTE_NAME));

    assertTrue(engine.persistAttribute(principal, TEST_RELYING_PARTY, TEST_ATTRIBUTE_NAME, TEST_ATTRIBUTE_VALUE));

    assertTrue(engine.attributeExists(principal, TEST_RELYING_PARTY, TEST_ATTRIBUTE_NAME));

    assertEquals(engine.getAttributeValue(principal, TEST_RELYING_PARTY, TEST_ATTRIBUTE_NAME), TEST_ATTRIBUTE_VALUE);

    assertTrue(engine.unpersistAttribute(principal, TEST_RELYING_PARTY, TEST_ATTRIBUTE_NAME));

    assertFalse(engine.attributeExists(principal, TEST_RELYING_PARTY, TEST_ATTRIBUTE_NAME));
  }
}
