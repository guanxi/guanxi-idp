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

package org.guanxi.idp.service.saml2;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.ServletContextAware;
import org.springframework.context.MessageSource;
import org.guanxi.xal.saml_2_0.protocol.ResponseDocument;
import org.guanxi.xal.saml_2_0.protocol.ResponseType;
import org.guanxi.xal.saml_2_0.assertion.*;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.AttributorAttribute;
import org.guanxi.xal.saml_1_0.assertion.NameIdentifierType;
import org.guanxi.idp.farm.attributors.Attributor;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.Utils;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.definitions.EduPerson;
import org.guanxi.common.definitions.SAML;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;

/**
 * SAML2 Web Browser SSO Single Sign-On Service.
 * This is the authentication request protocol endpoint at the identity provider to which the
 * <AuthnRequest> message (or artifact representing it) is delivered by the user agent.
 *
 * @author alistair
 */
public class WebBrowserSSO extends AbstractController implements ServletContextAware {
  /** Our logger */
  private static final Logger logger = Logger.getLogger(WebBrowserSSO.class.getName());
  /** The localised messages to use */
  private MessageSource messages = null;
  /** The JSP to use to POST the response to the SP */
  private String httpPOSTView = null;
  /** The JSP to display if an error occurs */
  private String errorView = null;
  /** The request attribute that holds the error message for the error view */
  private String errorViewDisplayVar = null;
  /** The Attributors to use to get attributes */
  private Attributor[] attributor = null;

  public void init() {}

  /**
   * Handles processing of an AuthnRequest message. If the request attribute:
   * wbsso-handler-error-message
   * is present then we must display the error text it contains and go no further.
   * 
   * @param request Servlet request
   * @param response Servlet response
   * @return the view to display
   * @throws Exception if an error occurs
   */
  public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    ModelAndView mAndV = new ModelAndView();

    GuanxiPrincipal gxPrincipal = (GuanxiPrincipal)request.getAttribute(Guanxi.REQUEST_ATTR_IDP_PRINCIPAL);
    String spProviderId = (String)request.getAttribute("entityID");

    // Display an error message if it exists and go no further
    if (request.getAttribute("wbsso-handler-error-message") != null) {
      logger.error("Displaying auth handler error");
      mAndV.setViewName(errorView);
      mAndV.getModel().put(errorViewDisplayVar, request.getAttribute("wbsso-handler-error-message"));
      return mAndV;
    }

    ResponseDocument responseDoc = ResponseDocument.Factory.newInstance();
    ResponseType wbssoResponse = responseDoc.addNewResponse();
    wbssoResponse.setIssueInstant(Calendar.getInstance());
    Utils.zuluXmlObject(wbssoResponse, 0);

    /*
    NameIdentifierDocument nameIDDoc = NameIdentifierDocument.Factory.newInstance();
    NameIdentifierType nameID = nameIDDoc.addNewNameIdentifier();
    nameID.setNameQualifier("https://idp.test.sgarbh.smo.uhi.ac.uk/shibboleth");
    nameID.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    nameID.setStringValue(gxPrincipal.getUniqueId());
    */

    NameIDType issuerNameID = NameIDType.Factory.newInstance();
    issuerNameID.setNameQualifier("https://idp.test.sgarbh.smo.uhi.ac.uk/shibboleth");
    issuerNameID.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    issuerNameID.setStringValue("https://idp.test.sgarbh.smo.uhi.ac.uk/shibboleth");

    // Get a new Assertion ready for the AttributeStatement nodes
    AssertionDocument assertionDoc = AssertionDocument.Factory.newInstance();
    AssertionType assertion = assertionDoc.addNewAssertion();
    assertion.setIssuer(issuerNameID);
    assertion.setIssueInstant(Calendar.getInstance());
    Utils.zuluXmlObject(assertion, 0);


    // HTTP POST or HTTP Artifact

    // Assemble the attributes
    UserAttributesDocument attributesDoc = UserAttributesDocument.Factory.newInstance();
    UserAttributesDocument.UserAttributes attributes = attributesDoc.addNewUserAttributes();
    for (org.guanxi.idp.farm.attributors.Attributor attr : attributor) {
      attr.getAttributes(gxPrincipal, spProviderId, attributes);
    }
    AttributeStatementDocument attrStatementDoc = addAttributesFromFarm(attributesDoc);

    // If a user has no attributes we shouldn't add an Assertion or Subject
    if (attrStatementDoc != null) {
      assertion.setAttributeStatementArray(new AttributeStatementType[] {attrStatementDoc.getAttributeStatement()});
      wbssoResponse.setAssertionArray(new org.guanxi.xal.saml_2_0.assertion.AssertionType[] {assertion});
    }

    // Sort out the namespaces for saving the Response
    HashMap<String, String> namespaces = new HashMap<String, String>();
    namespaces.put(SAML.NS_SAML_20_PROTOCOL, SAML.NS_PREFIX_SAML_20_PROTOCOL);
    namespaces.put(SAML.NS_SAML_20_ASSERTION, SAML.NS_PREFIX_SAML_20_ASSERTION);
    XmlOptions xmlOptions = new XmlOptions();
    xmlOptions.setSavePrettyPrint();
    xmlOptions.setSavePrettyPrintIndent(2);
    xmlOptions.setUseDefaultNamespace();
    xmlOptions.setSaveAggressiveNamespaces();
    xmlOptions.setSaveSuggestedPrefixes(namespaces);
    xmlOptions.setSaveNamespacesFirst();

    String b64SAMLResponse = Utils.base64((Document)responseDoc.newDomNode(xmlOptions));
      
    // Send the Response to the SP
    request.setAttribute("SAMLResponse", b64SAMLResponse);
    request.setAttribute("RelayState", request.getParameter("RelayState"));
    mAndV.setViewName(httpPOSTView);
    mAndV.getModel().put("wbsso_acs_endpoint", request.getAttribute("acsURL"));
    return mAndV;
  }

  // Setters
  public void setMessages(MessageSource messages) { this.messages = messages; }
  public void setHttpPOSTView(String httpPOSTView) { this.httpPOSTView = httpPOSTView; }
  public void setErrorView(String errorView) { this.errorView = errorView; }
  public void setErrorViewDisplayVar(String errorViewDisplayVar) { this.errorViewDisplayVar = errorViewDisplayVar; }
  public void setAttributor(Attributor[] attributor) { this.attributor = attributor; }





  private AttributeStatementDocument addAttributesFromFarm(UserAttributesDocument guanxiAttrFarmOutput) {
    AttributeStatementDocument attrStatementDoc = AttributeStatementDocument.Factory.newInstance();
    AttributeStatementType attrStatement = attrStatementDoc.addNewAttributeStatement();

    boolean hasAttrs = false;
    for (int c=0; c < guanxiAttrFarmOutput.getUserAttributes().getAttributeArray().length; c++) {
      hasAttrs = true;

      AttributorAttribute attributorAttr = guanxiAttrFarmOutput.getUserAttributes().getAttributeArray(c);

      // Has the attribute already been processed? i.e. does it have multiple values?
      AttributeType attribute = null;
      AttributeType[] existingAttrs = attrStatement.getAttributeArray();
      if (existingAttrs != null) {
        for (int cc=0; cc < existingAttrs.length; cc++) {
          if (existingAttrs[cc].getName().equals(attributorAttr.getName())) {
            attribute = existingAttrs[cc];
          }
        }
      }

      // New attribute, not yet processed
      if (attribute == null) {
        attribute = attrStatement.addNewAttribute();
        attribute.setName(attributorAttr.getName());
        //attribute.setAttributeNamespace(Shibboleth.NS_ATTRIBUTES);
      }

      XmlObject attrValue = attribute.addNewAttributeValue();

      // Deal with scoped eduPerson attributes
      if  ((attribute.getName().equals(EduPerson.EDUPERSON_SCOPED_AFFILIATION)) ||
           (attribute.getName().equals(EduPerson.EDUPERSON_TARGETED_ID))) {
        // Check if the scope is present...
        if (!attributorAttr.getValue().contains(EduPerson.EDUPERSON_SCOPED_DELIMITER)) {
          // ...if not, add the error scope
          logger.error(attribute.getName() + " has no scope, adding " + EduPerson.EDUPERSON_NO_SCOPE_DEFINED);
          attributorAttr.setValue(attributorAttr.getValue() + EduPerson.EDUPERSON_SCOPED_DELIMITER + EduPerson.EDUPERSON_NO_SCOPE_DEFINED);
        }
        String[] parts = attributorAttr.getValue().split(EduPerson.EDUPERSON_SCOPED_DELIMITER);
        Attr scopeAttribute = attrValue.getDomNode().getOwnerDocument().createAttribute(EduPerson.EDUPERSON_SCOPE_ATTRIBUTE);
        scopeAttribute.setValue(parts[1]);
        attrValue.getDomNode().getAttributes().setNamedItem(scopeAttribute);
        Text valueNode = attrValue.getDomNode().getOwnerDocument().createTextNode(parts[0]);
        attrValue.getDomNode().appendChild(valueNode);
      }
      else {
        Text valueNode = attrValue.getDomNode().getOwnerDocument().createTextNode(attributorAttr.getValue());
        attrValue.getDomNode().appendChild(valueNode);
      }

    }

    if (hasAttrs)
      return attrStatementDoc;
    else
      return null;
  }

}
