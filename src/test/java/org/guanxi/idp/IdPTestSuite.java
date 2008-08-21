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

package org.guanxi.idp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.guanxi.idp.persistence.PersistenceTestSuite;
import org.guanxi.idp.attribute.AttributeTestSuite;
import org.guanxi.idp.metadata.MetadataTestSuite;

/**
 * This is the root of all tests. It will invoke the various test suites that handle
 * testing of the various IdP subsystems.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { PersistenceTestSuite.class,
                       AttributeTestSuite.class,
                       MetadataTestSuite.class } )
public class IdPTestSuite {
}
