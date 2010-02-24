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

package org.guanxi.idp.util;

import java.util.ArrayList;

/**
 * Encapsulates a profile neutral attribute that can contain
 * multiple names and values
 *
 * @author alistair
 */
public class GuanxiAttribute {
  /** The list of names of the attribute */
  private ArrayList<String> names = null;
  /** The list of friendly names of the attribute */
  private ArrayList<String> friendlyNames = null;
  /** The list of values of the attribute */
  private ArrayList<String> values = null;
  /** Does this attribute have friendly names? This is profile dependent */
  private boolean hasFriendlyNames = false;

  /**
   * Default constructor
   */
  public GuanxiAttribute() {
    names = new ArrayList<String>();
    friendlyNames = new ArrayList<String>();
    values = new ArrayList<String>();
  }

  /**
   * Adds another name for the attribute
   *
   * @param name the name to add
   */
  public void addName(String name) {
    names.add(name);
  }

  /**
   * Adds another friendly name for the attribute
   *
   * @param friendlyName the name to add
   */
  public void addFriendlyName(String friendlyName) {
    friendlyNames.add(friendlyName);
    hasFriendlyNames = true;
  }

  /**
   * Adds another value to the attribute
   *
   * @param value the value to add
   */
  public void addValue(String value) {
    values.add(value);
  }

  /**
   * Sets a value of an attribute based on the index
   *
   * @param index the index of the value to sync with the name to be used for that value
   * @param value the value
   */
  public void setValue(int index, String value) {
    values.set(index, value);
  }

  /**
   * Retrieves the attribute name based on index
   *
   * @param index the index
   * @return the attribute name
   */
  public String getNameAtIndex(int index) {
    return names.get(index);
  }

  /**
   * Retrieves the attribute friendly name based on index
   *
   * @param index the index
   * @return the attribute friendly name
   */
  public String getFriendlyNameAtIndex(int index) {
    return friendlyNames.get(index);
  }

  /**
   * Retrieves the attribute value based on index
   *
   * @param index the index
   * @return the attribute value
   */
  public String getValueAtIndex(int index) {
    return values.get(index);
  }

  /**
   * Determines whether the attribute has any friendly names
   *
   * @return true if it has friendly names, otherwise false
   */
  public boolean hasFriendlyNames() {
    return hasFriendlyNames;
  }

  public ArrayList<String> getNames() { return names; }
  public ArrayList<String> getValues() { return values; }
}
