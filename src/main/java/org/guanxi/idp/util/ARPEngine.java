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

import org.guanxi.common.GuanxiException;
import org.guanxi.xal.idp.*;
import org.guanxi.xal.idp.ArpDocument.Arp;
import org.apache.xmlbeans.XmlException;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * <font size=5><b></b></font>
 *
 * @author Alistair Young alistair@smo.uhi.ac.uk
 */
public class ARPEngine implements ServletContextAware {
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  private ServletContext servletContext = null;
  /** Our ARP files */
  private Vector<Arp> arpFiles = null;
  /** The map file to use */
  private String arpFile = null;
  /** The engine that handles variable interpolation */
  private VarEngine varEngine = null;

  public void init() {
    arpFiles = new Vector<Arp>();

    try {
      loadARPs(arpFile);
    }
    catch(GuanxiException ge) {
    }
  }

  public void setArpFile(String arpFile) { this.arpFile = arpFile; }
  public String getArpFile() { return arpFile; }
  public void setVarEngine(VarEngine varEngine) { this.varEngine = varEngine; }

  /**
   * Determines whether to release an attribute to a provider based on rules in
   * the ARP file
   *
   * @param provider The providerId of the provider who wants the attribute
   * @param attribute The fully qualified name of the attribute
   * @param attributeValue The value of the attribute
   * @return true if the attribute can be released otherwise false
   */
  public boolean release(String provider, String attribute, String attributeValue) {
    Vector<Provider> spProviders = new Vector<Provider>();

    // Find the provider's ruleset in the ARPs
    for (int arpFilesCount = 0; arpFilesCount < arpFiles.size(); arpFilesCount++) {
      ArpDocument.Arp arp = (ArpDocument.Arp)arpFiles.get(arpFilesCount);

      for (int count = 0; count < arp.getProviderArray().length; count++) {
        if ((varEngine.interpolate(arp.getProviderArray(count).getName()).equals(provider)) ||
            (varEngine.interpolate(arp.getProviderArray(count).getName()).equals("*")))
          // There must be at least one rule. If none are present, release nothing
          if (arp.getProviderArray(count).getAllowArray().length > 0) {
            spProviders.add(arp.getProviderArray(count));
          }
      }
    }

    // If the provider isn't listed, release nothing to it
    if (spProviders.size() == 0)
      return false;

    // Check the list of allowed attributes
    return releaseAttribute(spProviders, attribute, attributeValue);
  }

  /**
   * Checks a provider's element in the ARP file to see if an attribute is in any of the
   * bags referenced from a set of allow nodes.
   *
   * @param providers List of Provider objects that want the attribute to be released
   * @param attributeName The fully qualified name of the attribute
   * @param attributeValue The value of the attribute
   * @return true if the attribute can be released otherwise false
   */
  private boolean releaseAttribute(Vector<Provider> providers, String attributeName, String attributeValue) {
    // Cycle through all the <allow> nodes for a <provider> node
    for (int arpFilesCount = 0; arpFilesCount < arpFiles.size(); arpFilesCount++) {
      ArpDocument.Arp arp = (ArpDocument.Arp)arpFiles.get(arpFilesCount);

      for (Provider provider : providers) {
        for (int count = 0; count < provider.getAllowArray().length; count++) {
          String bagName = varEngine.interpolate(provider.getAllowArray(count));

          // Load up the bag from the list of <bag> nodes in the ARP file
          Bag bag = null;
          for (int bagCount = 0; bagCount < arp.getBagArray().length; bagCount++) {
            if (varEngine.interpolate(arp.getBagArray(bagCount).getName()).equals(bagName)) {
              bag = arp.getBagArray(bagCount);
            }
          }

          // If there's no Bag defined, then go on to the next one
          if (bag == null)
            continue;

          // Now look for the attribute in the Bag, or an attribute with a name of *
          Attribute attribute = null;
          for (int attrCount = 0; attrCount < bag.getAttributeArray().length; attrCount++) {
            if ((varEngine.interpolate(bag.getAttributeArray(attrCount).getName()).equals(attributeName)) ||
                (varEngine.interpolate(bag.getAttributeArray(attrCount).getName()).equals("*"))) {
              attribute = bag.getAttributeArray(attrCount);
            }
          }

          // If it's not listed, move on to the next Bag
          if (attribute == null)
            continue;

          /* Do we release the attribute with any value?
           * If not, move on to the next Bag.
           */
          if (attribute.getValue().equals("*"))
            return true;

          /* Not a wildcard so we need to restrict the attribute's value for release.
           * If the values don't match we'll just move on to the next Bag.
           */
          if (attribute.getValue().equals(attributeValue))
            return true;
        } // for (int count = 0; count < provider.getAllowArray().length; count++)
      } // for (Provider provider : providers) {
    } // for (int arpFilesCount = 0; arpFilesCount < arpFiles.size(); arpFilesCount++)

    return false;
  }

  /**
   * Loads up the chain of ARP files to use. The chain will always have at least one in it.
   *
   * @param arpXMLFile The full path and name of the root ARP file
   * @throws GuanxiException if an error occurs
   */
  private void loadARPs(String arpXMLFile) throws GuanxiException {
    try {
      // Sort out the path to the ARP file
      String arpFile = null;
      if ((arpXMLFile.startsWith("WEB-INF")) ||
          (arpXMLFile.startsWith("/WEB-INF"))) {
        arpFile = servletContext.getRealPath(arpXMLFile);
      }
      else
        arpFile = arpXMLFile;

      // Load up the root ARP file
      ArpDocument arpDoc = ArpDocument.Factory.parse(new File(arpFile));
      arpFiles.add(arpDoc.getArp());

      // Do we have any other ARP files to include?
      if (arpDoc.getArp().getIncludeArray() != null) {
        for (int c=0; c < arpDoc.getArp().getIncludeArray().length; c++) {
          // Load up any further included map files
          loadARPs(arpDoc.getArp().getIncludeArray(c).getArpFile());
        }
      }
    }
    catch(XmlException xe) {
      throw new GuanxiException(xe);
    }
    catch(IOException ioe) {
      throw new GuanxiException(ioe);
    }
  }

  public void setServletContext(ServletContext servletContext) { this.servletContext = servletContext; }
}
