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
import org.guanxi.xal.saml_2_0.protocol.ResponseDocument;
import org.guanxi.xal.saml_2_0.protocol.ResponseType;
import org.guanxi.xal.saml_2_0.protocol.AuthnRequestDocument;
import org.guanxi.xal.saml_2_0.assertion.*;
import org.guanxi.xal.saml_2_0.metadata.EntityDescriptorType;
import org.guanxi.xal.saml_2_0.metadata.KeyDescriptorType;
import org.guanxi.xal.saml_2_0.metadata.KeyTypes;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.AttributorAttribute;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.xal.w3.xmlenc.EncryptedDataType;
import org.guanxi.xal.w3.xmlenc.EncryptedDataDocument;
import org.guanxi.xal.w3.xmldsig.KeyInfoDocument;
import org.guanxi.xal.w3.xmldsig.X509DataType;
import org.guanxi.idp.farm.attributors.Attributor;
import org.guanxi.idp.service.SSOBase;
import org.guanxi.idp.util.AttributeMap;
import org.guanxi.idp.util.ARPEngine;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.Utils;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.metadata.SPMetadata;
import org.guanxi.common.entity.EntityFarm;
import org.guanxi.common.entity.EntityManager;
import org.guanxi.common.security.SecUtils;
import org.guanxi.common.security.SecUtilsConfig;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.definitions.EduPerson;
import org.guanxi.common.definitions.SAML;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
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
import java.util.HashMap;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;

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
  /** The JSP to display if an error occurs */
  private String errorView = null;
  /** The request attribute that holds the error message for the error view */
  private String errorViewDisplayVar = null;
  /** The Attributors to use to get attributes */
  private Attributor[] attributor = null;
  /** Our profile specific attribute mapper */
  protected AttributeMap mapper = null;
  /** Our ARP engine */
  protected ARPEngine arpEngine = null;

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

    String requestID = (String)request.getAttribute("requestID");

    ResponseDocument responseDoc = ResponseDocument.Factory.newInstance();
    ResponseType wbssoResponse = responseDoc.addNewResponse();
    wbssoResponse.setIssueInstant(Calendar.getInstance());
    Utils.zuluXmlObject(wbssoResponse, 0);

    NameIDType issuer = wbssoResponse.addNewIssuer();
    issuer.setFormat(SAML.URN_SAML2_NAMEID_FORMAT_ENTITY);
    issuer.setStringValue(idpEntityID);

    AssertionDocument assertionDoc = AssertionDocument.Factory.newInstance();
    AssertionType assertion = assertionDoc.addNewAssertion();
    assertion.setID(Utils.createNCNameID());
    assertion.setIssueInstant(Calendar.getInstance());
    assertion.setIssuer(issuer);
    Utils.zuluXmlObject(assertion, 0);

    SubjectType subject = assertion.addNewSubject();
    SubjectConfirmationType subjectConfirmation = subject.addNewSubjectConfirmation();
    subjectConfirmation.setMethod(SAML.URN_SAML2_CONFIRMATION_METHOD_BEARER);
    SubjectConfirmationDataType subjectConfirmationData = subjectConfirmation.addNewSubjectConfirmationData();
    subjectConfirmationData.setAddress(request.getLocalAddr());
    subjectConfirmationData.setInResponseTo(requestID);
    subjectConfirmationData.setNotOnOrAfter(Calendar.getInstance());
    subjectConfirmationData.setRecipient(spEntityID);
    Utils.zuluXmlObject(subjectConfirmationData, assertionTimeLimit);

    ConditionsType conditions = assertion.addNewConditions();
    conditions.setNotBefore(Calendar.getInstance());
    conditions.setNotOnOrAfter(Calendar.getInstance());
    AudienceRestrictionType audienceRestriction = conditions.addNewAudienceRestriction();
    audienceRestriction.addAudience(spEntityID);
    Utils.zuluXmlObject(conditions, assertionTimeLimit);

    AuthnStatementType authnStatement = assertion.addNewAuthnStatement();
    authnStatement.setAuthnInstant(Calendar.getInstance());
    authnStatement.setSessionIndex("");
    authnStatement.addNewSubjectLocality().setAddress(request.getLocalAddr());
    authnStatement.addNewAuthnContext().setAuthnContextDeclRef(SAML.URN_SAML2_PASSWORD_PROTECTED_TRANSPORT);
    Utils.zuluXmlObject(authnStatement, 0);

    // HTTP POST or HTTP Artifact

    // Assemble the attributes
    UserAttributesDocument attributesDoc = UserAttributesDocument.Factory.newInstance();
    UserAttributesDocument.UserAttributes attributes = attributesDoc.addNewUserAttributes();
    for (org.guanxi.idp.farm.attributors.Attributor attr : attributor) {
      attr.getAttributes(gxPrincipal, spEntityID, arpEngine, mapper, attributes);
    }
    AttributeStatementDocument attrStatementDoc = addAttributesFromFarm(attributesDoc);

    // If a user has no attributes we shouldn't add an Assertion or Subject
    if (attrStatementDoc != null) {
      assertion.setAttributeStatementArray(new AttributeStatementType[] {attrStatementDoc.getAttributeStatement()});
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

    // Get the SP's metadata...
    EntityFarm farm = (EntityFarm)getServletContext().getAttribute(Guanxi.CONTEXT_ATTR_IDP_ENTITY_FARM);
    EntityManager manager = farm.getEntityManagerForID(spEntityID);
    SPMetadata metadata = (SPMetadata)manager.getMetadata(spEntityID);
    EntityDescriptorType saml2Metadata = (EntityDescriptorType)metadata.getPrivateData();

    // ...and find its encryption key. We'll use this to encrypt the secret key for encrypting the attributes
    X509Certificate metadataCert = null;
    PublicKey keyEncryptKey = null;
    KeyDescriptorType[] keyDescriptors = saml2Metadata.getSPSSODescriptorArray(0).getKeyDescriptorArray();
    for (KeyDescriptorType keyDescriptor : keyDescriptors) {
      if (keyDescriptor.getUse().equals(KeyTypes.ENCRYPTION)) {
        byte[] spCertBytes = keyDescriptor.getKeyInfo().getX509DataArray(0).getX509CertificateArray(0);
        CertificateFactory certFactory = CertificateFactory.getInstance("x.509");
        ByteArrayInputStream certByteStream = new ByteArrayInputStream(spCertBytes);
        metadataCert = (X509Certificate)certFactory.generateCertificate(certByteStream);
        certByteStream.close();
        keyEncryptKey = metadataCert.getPublicKey();
      }
    }

    // Generate a secret key
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(128);
    SecretKey secretKey = keyGenerator.generateKey();

    XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_OAEP);
    keyCipher.init(XMLCipher.WRAP_MODE, keyEncryptKey);

    Document domAssertionDoc = (Document)assertionDoc.newDomNode(xmlOptions);
    EncryptedKey encryptedKey = keyCipher.encryptKey(domAssertionDoc, secretKey);

    // specify the element to encrypt
    Element elementToEncrypt = domAssertionDoc.getDocumentElement();

    XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_128);
    xmlCipher.init(XMLCipher.ENCRYPT_MODE, secretKey);

    // add key info to encrypted data element
    EncryptedData encryptedDataElement = xmlCipher.getEncryptedData();
    KeyInfo keyInfo = new KeyInfo(domAssertionDoc);
    keyInfo.add(encryptedKey);
    encryptedDataElement.setKeyInfo(keyInfo);

    encryptedDataElement.getKeyInfo().getBaseURI();

    // do the actual encryption
    boolean encryptContentsOnly = false;
    xmlCipher.doFinal(domAssertionDoc, elementToEncrypt, encryptContentsOnly);

    EncryptedDataDocument encryptedDataDoc = EncryptedDataDocument.Factory.parse(domAssertionDoc);
    wbssoResponse.addNewEncryptedAssertion().setEncryptedData(encryptedDataDoc.getEncryptedData());

    // Get the config ready for signing
    SecUtilsConfig secUtilsConfig = new SecUtilsConfig();
    secUtilsConfig.setKeystoreFile(credsConfig.getKeystoreFile());
    secUtilsConfig.setKeystorePass(credsConfig.getKeystorePassword());
    secUtilsConfig.setKeystoreType("JKS");
    secUtilsConfig.setPrivateKeyAlias(credsConfig.getPrivateKeyAlias());
    secUtilsConfig.setPrivateKeyPass(credsConfig.getPrivateKeyPassword());
    secUtilsConfig.setCertificateAlias(credsConfig.getCertificateAlias());

    EncryptedDataType e = responseDoc.getResponse().getEncryptedAssertionArray(0).getEncryptedData();
    NodeList nodes = e.getKeyInfo().getDomNode().getChildNodes();
    Node node = null;
    for (int c=0; c < nodes.getLength(); c++) {
      node = nodes.item(c);
      if (node.getLocalName() != null) {
        if (node.getLocalName().equals("EncryptedKey")) break;
      }
    }
    KeyInfoDocument k = KeyInfoDocument.Factory.newInstance();
    X509DataType x = k.addNewKeyInfo().addNewX509Data();
    StringWriter sw = new StringWriter();
    PEMWriter pemWriter = new PEMWriter(sw);
    pemWriter.writeObject(metadataCert);
    pemWriter.close();
    String s = sw.toString();
    s = s.replaceAll("-----BEGIN CERTIFICATE-----", "");
    s = s.replaceAll("-----END CERTIFICATE-----", "");
    x.addNewX509Certificate().setStringValue(s);
    node.appendChild(node.getOwnerDocument().importNode(k.getKeyInfo().getDomNode(), true));

    // Break out to DOM land to get the SAML Response signed...
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
  public void setMapper(AttributeMap mapper) { this.mapper = mapper; }
  public void setArpEngine(ARPEngine arpEngine) { this.arpEngine = arpEngine; }



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
