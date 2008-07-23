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

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.context.ServletContextAware;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.xal.idp.ServiceProvider;

import javax.servlet.ServletContext;

public class RegisterSPFormValidator implements Validator, ServletContextAware {
  /** The servlet context */
  private ServletContext servletContext = null;

  /**
   * Sets the servlet context
   * @param servletContext The servlet context
   */
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @SuppressWarnings("unchecked")
  public boolean supports(Class clazz) {
    return clazz.equals(RegisterSP.class);
  }

  public void validate(Object obj, Errors errors) {
    RegisterSP form = (RegisterSP)obj;

    if (checkForDuplicateSP(form.getProviderId())) {
      errors.rejectValue("providerId", "register.sp.error.duplicate.providerId");
    }

    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "providerId", "error.field.required");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "identity", "error.field.required");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "creds", "error.field.required");
  }

  private boolean checkForDuplicateSP(String providerId) {
    IdpDocument.Idp idpConfig = (IdpDocument.Idp)servletContext.getAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG);
    ServiceProvider[] spList = idpConfig.getServiceProviderArray();
    for (ServiceProvider sp : spList) {
      if (sp.getName().equals(providerId)) {
        return true;
      }
    }

    return false;
  }
}
