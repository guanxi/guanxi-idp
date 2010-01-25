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

import org.springframework.web.servlet.ModelAndView;
import org.springframework.context.MessageSource;
import org.guanxi.xal.saml_2_0.protocol.*;
import org.guanxi.xal.saml_2_0.assertion.*;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.xal.w3.xmlenc.EncryptedDataType;
import org.guanxi.xal.w3.xmlenc.EncryptedDataDocument;
import org.guanxi.xal.w3.xmldsig.KeyInfoDocument;
import org.guanxi.xal.w3.xmldsig.X509DataType;
import org.guanxi.idp.service.SSOBase;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.Utils;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.security.SecUtils;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.definitions.SAML;
import org.apache.xmlbeans.XmlException;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.keys.KeyInfo;
import org.w3c.dom.*;
import org.bouncycastle.openssl.PEMWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Calendar;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.io.StringWriter;
import java.net.URLEncoder;

/**
 * SAML2 Web Browser SSO Single Sign-On Service.
 * This is the authentication request protocol endpoint at the identity provider to which the
 * <AuthnRequest> message (or artifact representing it) is delivered by the user agent.
 *
 * @author alistair
 */
public class WebBrowserSSO extends SSOBase {
  /** The JSP to use to POST the response to the SP */
  private String httpPOSTView = null;
  /** The JSP to use to GET the response to the SP */
  private String httpRedirectView = null;
  /** The JSP to display if an error occurs */
  private String errorView = null;
  /** The request attribute that holds the error message for the error view */
  private String errorViewDisplayVar = null;

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

    idpConfig = (IdpDocument.Idp)getServletContext().getAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG);
    loadPersona(request);

    GuanxiPrincipal gxPrincipal = (GuanxiPrincipal)request.getAttribute(Guanxi.REQUEST_ATTR_IDP_PRINCIPAL);
    String spEntityID = (String)request.getAttribute("entityID");

    // Display an error message if it exists and go no further
    if (request.getAttribute("wbsso-handler-error-message") != null) {
      logger.error("Displaying auth handler error");
      mAndV.setViewName(errorView);
      mAndV.getModel().put(errorViewDisplayVar, request.getAttribute("wbsso-handler-error-message"));
      return mAndV;
    }

    // We need this for reference in the Response
    String requestID = (String)request.getAttribute("requestID");

    // Response
    ResponseDocument responseDoc = ResponseDocument.Factory.newInstance();
    ResponseType wbssoResponse = responseDoc.addNewResponse();
    wbssoResponse.setIssueInstant(Calendar.getInstance());
    Utils.zuluXmlObject(wbssoResponse, 0);

    // Response/Issuer
    NameIDType issuer = wbssoResponse.addNewIssuer();
    issuer.setFormat(SAML.URN_SAML2_NAMEID_FORMAT_ENTITY);
    issuer.setStringValue(idpEntityID);

    // Response/Status
    StatusDocument statusDoc = StatusDocument.Factory.newInstance();
    StatusType status = statusDoc.addNewStatus();
    StatusCodeType topLevelStatusCode = status.addNewStatusCode();
    topLevelStatusCode.setValue(SAML.SAML2_STATUS_SUCCESS);
    wbssoResponse.setStatus(status);

    // Response/Assertion
    AssertionDocument assertionDoc = AssertionDocument.Factory.newInstance();
    AssertionType assertion = assertionDoc.addNewAssertion();
    assertion.setID(Utils.createNCNameID());
    assertion.setIssueInstant(Calendar.getInstance());
    assertion.setIssuer(issuer);
    Utils.zuluXmlObject(assertion, 0);

    // Response/Assertion/Subject
    SubjectType subject = assertion.addNewSubject();
    SubjectConfirmationType subjectConfirmation = subject.addNewSubjectConfirmation();
    subjectConfirmation.setMethod(SAML.URN_SAML2_CONFIRMATION_METHOD_BEARER);
    SubjectConfirmationDataType subjectConfirmationData = subjectConfirmation.addNewSubjectConfirmationData();
    subjectConfirmationData.setAddress(request.getLocalAddr());
    subjectConfirmationData.setInResponseTo(requestID);
    subjectConfirmationData.setNotOnOrAfter(Calendar.getInstance());
    subjectConfirmationData.setRecipient(spEntityID);
    Utils.zuluXmlObject(subjectConfirmationData, assertionTimeLimit);

    // Response/Assertion/Conditions
    ConditionsType conditions = assertion.addNewConditions();
    conditions.setNotBefore(Calendar.getInstance());
    conditions.setNotOnOrAfter(Calendar.getInstance());
    AudienceRestrictionType audienceRestriction = conditions.addNewAudienceRestriction();
    audienceRestriction.addAudience(spEntityID);
    Utils.zuluXmlObject(conditions, assertionTimeLimit);

    // Response/Assertion/AuthnStatement
    AuthnStatementType authnStatement = assertion.addNewAuthnStatement();
    authnStatement.setAuthnInstant(Calendar.getInstance());
    authnStatement.setSessionIndex("");
    authnStatement.addNewSubjectLocality().setAddress(request.getLocalAddr());
    authnStatement.addNewAuthnContext().setAuthnContextDeclRef(SAML.URN_SAML2_PASSWORD_PROTECTED_TRANSPORT);
    Utils.zuluXmlObject(authnStatement, 0);

    // Assemble the attributes
    UserAttributesDocument attributesDoc = UserAttributesDocument.Factory.newInstance();
    UserAttributesDocument.UserAttributes attributes = attributesDoc.addNewUserAttributes();
    for (org.guanxi.idp.farm.attributors.Attributor attr : attributor) {
      attr.getAttributes(gxPrincipal, spEntityID, arpEngine, mapper, attributes);
    }
    AttributeStatementDocument attrStatementDoc = getSAML2AttributeStatementFromFarm(attributesDoc, spEntityID);

    // If a user has no attributes we shouldn't add an Assertion or Subject
    if (attrStatementDoc != null) {
      // Response/Assertion/AttributeStatement
      assertion.setAttributeStatementArray(new AttributeStatementType[] {attrStatementDoc.getAttributeStatement()});
    }

    // Sort out the namespaces for saving the Response
    xmlOptions.setSaveSuggestedPrefixes(saml2Namespaces);

    // Get the SP's encryption key. We'll use this to encrypt the secret key for encrypting the attributes
    X509Certificate encryptionCert = getX509CertFromMetadata(getSPMetadata(spEntityID), ENTITY_SP, ENCRYPTION_CERT);
    PublicKey keyEncryptKey = encryptionCert.getPublicKey();

    // Generate a secret key
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(128);
    SecretKey secretKey = keyGenerator.generateKey();

    XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_OAEP);
    keyCipher.init(XMLCipher.WRAP_MODE, keyEncryptKey);

    Document domAssertionDoc = (Document)assertionDoc.newDomNode(xmlOptions);
    EncryptedKey encryptedKey = keyCipher.encryptKey(domAssertionDoc, secretKey);

    Element elementToEncrypt = domAssertionDoc.getDocumentElement();

    XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_128);
    xmlCipher.init(XMLCipher.ENCRYPT_MODE, secretKey);

    // Add KeyInfo to the EncryptedData element
    EncryptedData encryptedDataElement = xmlCipher.getEncryptedData();
    KeyInfo keyInfo = new KeyInfo(domAssertionDoc);
    keyInfo.add(encryptedKey);
    encryptedDataElement.setKeyInfo(keyInfo);

    // Encrypt the assertion
    xmlCipher.doFinal(domAssertionDoc, elementToEncrypt, false);

    // Go back into XMLBeans land...
    EncryptedDataDocument encryptedDataDoc = EncryptedDataDocument.Factory.parse(domAssertionDoc);
    // ...and add the encrypted assertion to the response
    wbssoResponse.addNewEncryptedAssertion().setEncryptedData(encryptedDataDoc.getEncryptedData());

    // Look for the Response/EncryptedAssertion/EncryptedData/KeyInfo/EncryptedKey node...
    EncryptedDataType encryptedData = responseDoc.getResponse().getEncryptedAssertionArray(0).getEncryptedData();
    NodeList nodes = encryptedData.getKeyInfo().getDomNode().getChildNodes();
    Node encryptedKeyNode = null;
    for (int c=0; c < nodes.getLength(); c++) {
      encryptedKeyNode = nodes.item(c);
      if (encryptedKeyNode.getLocalName() != null) {
        if (encryptedKeyNode.getLocalName().equals("EncryptedKey")) break;
      }
    }

    // ...get a new KeyInfo ready...
    KeyInfoDocument keyInfoDoc = KeyInfoDocument.Factory.newInstance();
    X509DataType x509Data = keyInfoDoc.addNewKeyInfo().addNewX509Data();

    // ...and a useable version of the SP's encryption certificate...
    StringWriter sw = new StringWriter();
    PEMWriter pemWriter = new PEMWriter(sw);
    pemWriter.writeObject(encryptionCert);
    pemWriter.close();
    String x509 = sw.toString();
    x509 = x509.replaceAll("-----BEGIN CERTIFICATE-----", "");
    x509 = x509.replaceAll("-----END CERTIFICATE-----", "");

    // ...add the encryption cert to the new KeyInfo...
    x509Data.addNewX509Certificate().setStringValue(x509);

    // ...and insert it into Response/EncryptedAssertion/EncryptedData/KeyInfo/EncryptedKey
    encryptedKeyNode.appendChild(encryptedKeyNode.getOwnerDocument().importNode(keyInfoDoc.getKeyInfo().getDomNode(), true));

    // Break out to DOM land to get the SAML Response signed...
    if (request.getAttribute("binding").equals("HTTP-POST")) {
      Document signedDoc = null;
      try {
        // Need to use newDomNode to preserve namespace information
        signedDoc = SecUtils.getInstance().sign(secUtilsConfig, (Document)responseDoc.newDomNode(xmlOptions), "");
        // ...and go back to XMLBeans land when it's ready
        responseDoc = ResponseDocument.Factory.parse(signedDoc);
      }
      catch(GuanxiException ge) {
        logger.error("Could not sign AuthnRequest", ge);
        mAndV.setViewName(errorView);
        mAndV.getModel().put(errorViewDisplayVar, messages.getMessage("error.could.not.sign.message",
                                                                      null, request.getLocale()));
        return mAndV;
      }
      catch(XmlException xe) {
        logger.error("Couldn't convert signed AuthnRequest back to XMLBeans", xe);
        mAndV.setViewName(errorView);
        mAndV.getModel().put(errorViewDisplayVar, messages.getMessage("error.could.not.sign.message",
                                                                      null, request.getLocale()));
        return mAndV;
      }
    }

    // Do the profile quickstep
    String b64SAMLResponse = null;
    if (request.getAttribute("binding").equals("HTTP-POST")) {
      b64SAMLResponse = Utils.base64((Document)responseDoc.newDomNode(xmlOptions));
      mAndV.setViewName(httpPOSTView);
    }
    else if (request.getAttribute("binding").equals("HTTP-Redirect")) {
      String deflatedResponse = Utils.deflate(responseDoc.toString(), Utils.RFC1951_DEFAULT_COMPRESSION_LEVEL, Utils.RFC1951_NO_WRAP);
      b64SAMLResponse = Utils.base64(deflatedResponse.getBytes());
      b64SAMLResponse = b64SAMLResponse.replaceAll(System.getProperty("line.separator"), "");
      b64SAMLResponse = URLEncoder.encode(b64SAMLResponse, "UTF-8");
      mAndV.setViewName(httpRedirectView);
    }

    // Send the Response to the SP
    request.setAttribute("SAMLResponse", b64SAMLResponse);
    request.setAttribute("RelayState", request.getParameter("RelayState"));
    mAndV.getModel().put("wbsso_acs_endpoint", request.getAttribute("acsURL"));
    return mAndV;
  }

  // Setters
  public void setMessages(MessageSource messages) { this.messages = messages; }
  public void setHttpPOSTView(String httpPOSTView) { this.httpPOSTView = httpPOSTView; }
  public void setHttpRedirectView(String httpRedirectView) { this.httpRedirectView = httpRedirectView; }
  public void setErrorView(String errorView) { this.errorView = errorView; }
  public void setErrorViewDisplayVar(String errorViewDisplayVar) { this.errorViewDisplayVar = errorViewDisplayVar; }
}
