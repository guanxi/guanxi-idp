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

package org.guanxi.idp.farm.rule;

/**
 * Definitions for attribute mapping rules
 */
public interface AttributeRule {
  /**
   * Sets the rule name for this rule implementation. This corresponds to
   * mappedRule in an AttributeMap:
   * <AttributeMap>
   *   <map name="..." mappedRule="append_domain" />
   * </AttributeMap>
   * in this case, setRuleName() would be "append_domain"
   *
   * @param ruleName the name of the rule in the map file
   */
  public void setRuleName(String ruleName);
  
  /**
   * Gets the name of the rule the implementation provides. This corresponds to
   * mappedRule in an AttributeMap:
   * <AttributeMap>
   *   <map name="..." mappedRule="append_domain" />
   * </AttributeMap>
   * in this case, getRuleName() would return "append_domain"
   *
   * @return the value of mappedRule
   */
  public String getRuleName();

  /**
   * Applies a mapping rule to an attribute and/or its value
   *
   * @param attributeName The name of the attribute
   * @param attributeValue The value of the attribute
   * @return The new value of the attribute based on the implemented rule
   */
  public String applyRule(String attributeName, String attributeValue);
}
