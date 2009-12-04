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

import org.guanxi.xal.idp.MapVar;
import org.guanxi.xal.idp.VarsDocument;
import org.guanxi.common.GuanxiException;
import org.apache.xmlbeans.XmlException;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Variable interpolation engine for attribute mapping and releasing
 *
 * @author alistair
 */
public class VarEngine implements ServletContextAware {
  /** The token that denotes a map variable in a map value */
  public static final String MAP_VARIABLE_TOKEN_START = "${";
  public static final String MAP_VARIABLE_TOKEN_END = "}";
  
  /** The ServletContext, passed to us by Spring as we are ServletContextAware */
  private ServletContext servletContext = null;
  /** The file containing the variable definitions */
  private String varFile = null;
  /** List of map variables */
  private HashMap<String,String> mapVariables = null;

  public void init() {
    mapVariables = new HashMap<String,String>();
    
    // Sort out the path to the ARP file
    if ((varFile.startsWith("WEB-INF")) ||
        (varFile.startsWith("/WEB-INF"))) {
      varFile = servletContext.getRealPath(varFile);
    }
    
    try {
      loadVars(varFile);
    }
    catch(GuanxiException ge) {
    }
  }

  /**
   * Loads up the variables to use.
   *
   * @param varFile The full path and name of the vars file
   * @throws GuanxiException if an error occurs
   */
  private void loadVars(String varFile) throws GuanxiException {
    try {
      // Load up the var file
      VarsDocument varsDoc = VarsDocument.Factory.parse(new File(varFile));

      // Load any variables
      if ((varsDoc.getVars().getVarArray() != null) && (varsDoc.getVars().getVarArray().length > 0)) {
        MapVar[] mapVars = varsDoc.getVars().getVarArray();
        for (MapVar mapVar : mapVars) {
          mapVariables.put(mapVar.getName(), mapVar.getValue());
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

  /**
   * Interpolates a map variable if present in a map value.
   * Map variables start with the recognised token:
   * MAP_VARIABLE_TOKENuni.ac.uk
   * e.g.
   * ${uni.ac.uk}
   *
   * @param value the map value
   * @return the interpolated value if a map variable is present, otherwise the original value.
   * If a map variable is present but not defined, the original value including the token is
   * returned.${x}
   */
  public String interpolate(String value) {
    if ((value.startsWith(MAP_VARIABLE_TOKEN_START)) && (value.endsWith(MAP_VARIABLE_TOKEN_END))) {
      String var = value.substring(MAP_VARIABLE_TOKEN_START.length(), (value.length() - 1));
      if (mapVariables.get(var) != null) {
        return mapVariables.get(var);
      }
      else {
        return value;
      }
    }
    else {
      return value;
    }
  }

  public String getVarFile() { return varFile; }
  public void setVarFile(String varFile) { this.varFile = varFile; }
  public void setServletContext(ServletContext servletContext) { this.servletContext = servletContext; }
}
