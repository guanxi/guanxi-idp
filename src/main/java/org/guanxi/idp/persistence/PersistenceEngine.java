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

/**
 * Represents an engine that can persist attribute names and values
 */
public interface PersistenceEngine {
  /**
   * Checks whether the specified attribute has been persisted
   *
   * @param attributeName the name of the attribute
   * @return true if the attribute has been persisted otherwise false
   */
  public boolean attributeExists(String attributeName);

  /**
   * Gets the value of a persisted attribute
   *
   * @param attributeName the name of the attribute
   * @return the value of the attribute
   */
  public String getAttributeValue(String attributeName);

  /**
   * Persists an attribute name and value
   *
   * @param attributeName the attribute name
   * @param attributeValue the attribute value
   * @return true if successful otherwise false
   */
  public boolean persistAttribute(String attributeName, String attributeValue);

  /**
   * Removes a persisted attribute from the persistence store
   *
   * @param attributeName the name of the attribute to remove. Its value will be removed too
   * @return true if successful otherwise false
   */
  public boolean unpersistAttribute(String attributeName);
}
