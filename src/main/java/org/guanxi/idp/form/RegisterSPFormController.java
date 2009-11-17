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

import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindException;
import org.springframework.context.MessageSource;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.xal.idp.ServiceProvider;
import org.guanxi.common.definitions.Guanxi;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

public class RegisterSPFormController extends SimpleFormController {
  /** The localised messages */
  private MessageSource messageSource = null;

  /**
   * Called once, just before the HTML form is displayed for the first time.
   * It's here that we initialise the ControlPanelForm to tell the form what
   * it can do with the job.
   *
   * @param request Standard HttpServletRequest
   * @return Instance of ControlPanelForm
   * @throws javax.servlet.ServletException
   */
  protected Object formBackingObject(HttpServletRequest request) throws ServletException {
    return new RegisterSP();
  }

  /**
   * Handles input from the web form to register a new SP
   *
   * @param request Standard issue HttpServletRequest
   * @param response Standard issue HttpServletResponse
   * @throws ServletException
   */
  @SuppressWarnings("unchecked")
  public ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response,
                               Object command, BindException errors) throws ServletException {

    RegisterSP form = (RegisterSP)command;

    IdpDocument.Idp idpConfig = (IdpDocument.Idp)getServletContext().getAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG);

    ServiceProvider sp = idpConfig.addNewServiceProvider();
    sp.setName(form.getProviderId());
    sp.setIdentity(form.getIdentity());
    sp.setCreds(form.getCreds());

    XmlOptions xmlOptions = new XmlOptions();
    xmlOptions.setSavePrettyPrint();
    xmlOptions.setSavePrettyPrintIndent(2);
    xmlOptions.setUseDefaultNamespace();

    IdpDocument idpDoc = (IdpDocument)getServletContext().getAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG_DOC);
    try {
      idpDoc.save(new File(getServletContext().getRealPath("/WEB-INF/guanxi_idp/config/idp.xml")), xmlOptions);
    }
    catch(IOException ioe) {
    }

    getServletContext().setAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG, idpConfig);

    ModelAndView mAndV = new ModelAndView(getSuccessView(), errors.getModel());
    mAndV.getModel().put("message", messageSource.getMessage("register.sp.success.message",
                                                             new Object[]{form.getProviderId()},
                                                             request.getLocale()));

    return mAndV;
  }

  public void setMessageSource(MessageSource messageSource) { this.messageSource = messageSource; }
}
