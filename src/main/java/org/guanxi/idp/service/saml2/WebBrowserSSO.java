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

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.params.InclusiveNamespaces;
import org.guanxi.common.security.SecUtilsConfig;
import org.guanxi.idp.util.AttributeMap;
import org.guanxi.idp.util.VarEngine;
import org.guanxi.xal.idp.AttributorAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.context.MessageSource;
import org.guanxi.xal.saml_2_0.protocol.*;
import org.guanxi.xal.saml_2_0.assertion.*;
import org.guanxi.xal.idp.UserAttributesDocument;
import org.guanxi.xal.idp.IdpDocument;
import org.guanxi.idp.service.SSOBase;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.Utils;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.security.SecUtils;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.definitions.SAML;
import org.w3c.dom.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.security.cert.X509Certificate;
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
  /** Whether to encrypt attributes in the response */
  private boolean encryptAttributes;
  /** If encryptAttributes is true, don't encrypt for these SPs */
  private ArrayList<String> doNotEncryptAttributesFor;
  /** Sign the Assertion instead of the Response for these SPs */
  private ArrayList<String> signAssertionFor;
  /** The engine that handles variable interpolation */
  private VarEngine varEngine = null;

  /**
   * Called by Spring to initialise the service
   */
  public void init() {
    super.init();
    interpolateEncryptionIgnores();
    interpolateAssertionSigns();
  }

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
    String spEntityID = (String)request.getAttribute("entityID");

    // Display an error message if it exists and go no further
    if (request.getAttribute("wbsso-handler-error-message") != null) {
      if (request.getAttribute("wbsso-handler-error-message").equals(SAML.SAML2_STATUS_NO_PASSIVE)) {
        // Return SAML2 Response as we don't do passive
        ResponseDocument errorDoc = buidErrorResponse(idpEntityID,
                                                      (String)request.getAttribute("requestID"),
                                                      (String)request.getAttribute("acsURL"),
                                                      SAML.SAML2_STATUS_NO_PASSIVE);

        xmlOptions.setSaveSuggestedPrefixes(saml2Namespaces);

        try {
          String b64SAMLResponse = signAndEncodeResponse(request, errorDoc, mAndV);

          // Debug syphoning?
          if (idpConfig.getDebug() != null) {
            if (idpConfig.getDebug().getSypthonAttributeAssertions() != null) {
              if (idpConfig.getDebug().getSypthonAttributeAssertions().equals("yes")) {
                logger.info("=======================================================");
                logger.info("Error Response to SAML2 WBSSO request by " + spEntityID);
                logger.info("");
                StringWriter sw = new StringWriter();
                errorDoc.save(sw, xmlOptions);
                logger.info(sw.toString());
                logger.info("");
                logger.info("=======================================================");
              }
            }
          }

          request.setAttribute("SAMLResponse", b64SAMLResponse);
          request.setAttribute("RelayState", request.getParameter("RelayState"));
          mAndV.getModel().put("wbsso_acs_endpoint", request.getAttribute("acsURL"));
        }
        catch(GuanxiException ge) {
          logger.error("Could not sign Response", ge);
          mAndV.setViewName(errorView);
          mAndV.getModel().put(errorViewDisplayVar, messages.getMessage("error.could.not.sign.message",
                                                                        null, request.getLocale()));
        }

        return mAndV;
      }
      else {
        logger.error("Displaying auth handler error");
        mAndV.setViewName(errorView);
        mAndV.getModel().put(errorViewDisplayVar, request.getAttribute("wbsso-handler-error-message"));
        return mAndV;
      }
    } // if (request.getAttribute("wbsso-handler-error-message") != null)

    GuanxiPrincipal gxPrincipal = (GuanxiPrincipal)request.getAttribute(Guanxi.REQUEST_ATTR_IDP_PRINCIPAL);
    if (request.getAttribute("NameIDFormat") != null) {
      // Use the NameID format supplied in the request...
      gxPrincipal.setNameIDFormat((String)request.getAttribute("NameIDFormat"));
    }
    else {
      // ...or the default if none is specified
      //gxPrincipal.setNameIDFormat(nameQualifier);
      gxPrincipal.setNameIDFormat(nameQualifierFormat);
    }

    // We need this for reference in the Response
    String requestID = (String)request.getAttribute("requestID");

    // Response
    ResponseDocument responseDoc = ResponseDocument.Factory.newInstance();
    ResponseType wbssoResponse = responseDoc.addNewResponse();
    wbssoResponse.setID(generateStringID());
    wbssoResponse.setVersion("2.0");
    wbssoResponse.setDestination((String)request.getAttribute("acsURL"));
    wbssoResponse.setIssueInstant(Calendar.getInstance());
    wbssoResponse.setInResponseTo(requestID);
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
    assertion.setID(generateStringID());
    assertion.setIssueInstant(Calendar.getInstance());
    assertion.setIssuer(issuer);
    assertion.setVersion("2.0");
    Utils.zuluXmlObject(assertion, 0);

    // Response/Assertion/Subject
    SubjectType subject = assertion.addNewSubject();
    SubjectConfirmationType subjectConfirmation = subject.addNewSubjectConfirmation();
    subjectConfirmation.setMethod(SAML.URN_SAML2_CONFIRMATION_METHOD_BEARER);
    SubjectConfirmationDataType subjectConfirmationData = subjectConfirmation.addNewSubjectConfirmationData();
    subjectConfirmationData.setAddress(request.getLocalAddr());
    subjectConfirmationData.setInResponseTo(requestID);
    subjectConfirmationData.setNotOnOrAfter(Calendar.getInstance());
    subjectConfirmationData.setRecipient((String)request.getAttribute("acsURL"));
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
    authnStatement.addNewAuthnContext().setAuthnContextClassRef(SAML.URN_SAML2_PASSWORD_PROTECTED_TRANSPORT);
    authnStatement.setSessionIndex("860e1c78883a682e07697c494b0ff1641847b128ec28cc8b597fb");
    Utils.zuluXmlObject(authnStatement, 0);

    // Assemble the attributes
    UserAttributesDocument attributesDoc = UserAttributesDocument.Factory.newInstance();
    UserAttributesDocument.UserAttributes attributes = attributesDoc.addNewUserAttributes();
    // Before we do anything, see if we need to release a NameID
    processNameID(mapper, spEntityID, attributes);
    for (org.guanxi.idp.farm.attributors.Attributor attr : attributor) {
      attr.getAttributes(gxPrincipal, spEntityID, arpEngine, mapper, attributes);
    }
    AttributeStatementDocument attrStatementDoc = getSAML2AttributeStatementFromFarm(attributesDoc, spEntityID,
                                                                                     subject, gxPrincipal);

    // If a user has no attributes we shouldn't add an Assertion or Subject
    if (attrStatementDoc != null) {
      // Response/Assertion/AttributeStatement
      assertion.setAttributeStatementArray(new AttributeStatementType[] {attrStatementDoc.getAttributeStatement()});
    }

    // Sort out the namespaces for saving the Response
    xmlOptions.setSaveSuggestedPrefixes(saml2Namespaces);

    // Debug syphoning?
    if (idpConfig.getDebug() != null) {
      if (idpConfig.getDebug().getSypthonAttributeAssertions() != null) {
        if (idpConfig.getDebug().getSypthonAttributeAssertions().equals("yes")) {
          logger.info("=======================================================");
          logger.info("Response to SAML2 WBSSO request by " + spEntityID);
          logger.info("");
          StringWriter sw = new StringWriter();
          responseDoc.save(sw, xmlOptions);
          assertionDoc.save(sw, xmlOptions);
          logger.info(sw.toString());
          logger.info("");
          logger.info("=======================================================");
        }
      }
    }

    // Encrypt the attributes if required but ignore any SP that doesn't want encryption
    if ((encryptAttributes) && (!doNotEncryptAttributesFor.contains(spEntityID))) {
      // Get the SP's encryption key. We'll use this to encrypt the secret key for encrypting the attributes
      X509Certificate encryptionCert = getX509CertFromMetadata(getSPMetadata(spEntityID), ENTITY_SP, ENCRYPTION_CERT);
      if (encryptionCert != null) {
        addEncryptedAssertionsToResponse(encryptionCert, assertionDoc, responseDoc);
      }
      else {
        logger.warn("Attribute encryption is on but " + spEntityID + " has no encryption key");
        responseDoc.getResponse().addNewAssertion();
        responseDoc.getResponse().setAssertionArray(0, assertionDoc.getAssertion());
      }
    }
    else {
      responseDoc.getResponse().addNewAssertion();
      responseDoc.getResponse().setAssertionArray(0, assertionDoc.getAssertion());
    }

    try {
      // Sign the Response of the Assertion
      String b64SAMLResponse;
      if (signAssertionFor.contains(spEntityID)) {
        mAndV.setViewName(httpPOSTView);
        b64SAMLResponse = signAndEncodeAssertion(secUtilsConfig,
                (Document)responseDoc.newDomNode(xmlOptions),
                assertionDoc.getAssertion().getID());
      } else {
        b64SAMLResponse = signAndEncodeResponse(request, responseDoc, mAndV);
      }

      // Send the Response to the SP
      request.setAttribute("SAMLResponse", b64SAMLResponse);
      request.setAttribute("RelayState", request.getParameter("RelayState"));
      mAndV.getModel().put("wbsso_acs_endpoint", request.getAttribute("acsURL"));
    }
    catch(GuanxiException ge) {
      logger.error("Could not sign Response", ge);
      mAndV.setViewName(errorView);
      mAndV.getModel().put(errorViewDisplayVar, messages.getMessage("error.could.not.sign.message",
                                                                    null, request.getLocale()));
    }

    return mAndV;
  }

  private void interpolateEncryptionIgnores() {
    for (String spVar : doNotEncryptAttributesFor) {
      doNotEncryptAttributesFor.add(varEngine.interpolate(spVar));
      doNotEncryptAttributesFor.remove(spVar);
    }
  }

  private void interpolateAssertionSigns() {
    for (String spVar : signAssertionFor) {
      signAssertionFor.add(varEngine.interpolate(spVar));
      signAssertionFor.remove(spVar);
    }
  }

  private String signAndEncodeResponse(HttpServletRequest request, ResponseDocument responseDocument,
                                       ModelAndView mAndV) throws GuanxiException {

    // Do the binding quickstep
    if (request.getAttribute("responseBinding").equals(SAML.SAML2_BINDING_HTTP_POST)) {
      mAndV.setViewName(httpPOSTView);
      // Need to use newDomNode to preserve namespace information
      Document signedDoc = SecUtils.getInstance().saml2Sign(secUtilsConfig,
              (Document)responseDocument.newDomNode(xmlOptions),
              responseDocument.getResponse().getID());

      if (idpConfig.getDebug() != null) {
        if (idpConfig.getDebug().getSypthonAttributeAssertions() != null) {
          if (idpConfig.getDebug().getSypthonAttributeAssertions().equals("yes")) {

            logger.info("=======================================================");
            logger.info("Signed SAML2 Response");
            logger.info("");
            logger.info(printDocument(signedDoc));
            logger.info("");
            logger.info("=======================================================");
          }
        }
      }

      return Utils.base64(signedDoc);
    }
    else if (request.getAttribute("responseBinding").equals(SAML.SAML2_BINDING_HTTP_REDIRECT)) {
      mAndV.setViewName(httpRedirectView);
      String deflatedResponse = Utils.deflate(responseDocument.toString(), Utils.RFC1951_DEFAULT_COMPRESSION_LEVEL, Utils.RFC1951_NO_WRAP);
      String b64SAMLResponse = Utils.base64(deflatedResponse.getBytes());
      b64SAMLResponse = b64SAMLResponse.replaceAll(System.getProperty("line.separator"), "");
      try {
        b64SAMLResponse = URLEncoder.encode(b64SAMLResponse, "UTF-8");
        return b64SAMLResponse;
      }
      catch(UnsupportedEncodingException uee) {
        throw new GuanxiException(uee);
      }
    }
    else throw new GuanxiException("responseBinding " + request.getAttribute("responseBinding") + "not supported");
  }

  private String signAndEncodeAssertion(SecUtilsConfig config, Document inDocToSign, String elementIDToSign) throws GuanxiException {

    String keystoreType = config.getKeystoreType();
    String keystoreFile = config.getKeystoreFile();
    String keystorePass = config.getKeystorePass();
    String privateKeyAlias = config.getPrivateKeyAlias();
    String privateKeyPass = config.getPrivateKeyPass();
    String certificateAlias = config.getCertificateAlias();

    try {
      KeyStore ks = KeyStore.getInstance(keystoreType);
      FileInputStream fis = new FileInputStream(keystoreFile);
      ks.load(fis, keystorePass.toCharArray());
      fis.close();
      PrivateKey privateKey = (PrivateKey) ks.getKey(privateKeyAlias, privateKeyPass.toCharArray());
      String keyType = privateKey.getAlgorithm();
      if (keyType.equalsIgnoreCase("dsa")) {
        keyType = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
      }

      if (keyType.equalsIgnoreCase("rsa")) {
        keyType = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
      }

      XMLSignature sig = new XMLSignature(inDocToSign, "", keyType, "http://www.w3.org/2001/10/xml-exc-c14n#");
      NodeList nodes = inDocToSign.getDocumentElement().getChildNodes();

      Node assertionNode = null;
      for (int c = 0; c < nodes.getLength(); ++c) {
        assertionNode = nodes.item(c);
        if (assertionNode.getLocalName() != null && assertionNode.getLocalName().equals("Assertion")) {
          NamedNodeMap attrs = assertionNode.getAttributes();
          Node attr = attrs.getNamedItem("ID");
          if ((attr.getNodeValue() != null) && attr.getNodeValue().equals(elementIDToSign)) {
            break;
          }
        }
      }

      if (assertionNode == null) {
        throw new GuanxiException("cannot sign assertion as no Assertion node");
      }

      Node assertionSubjectNode;
      if (assertionNode.getChildNodes() != null) {
        NodeList childNodes = assertionNode.getChildNodes();
        assertionSubjectNode = childNodes.item(3); // 0:Assertion/1:Text/2:Issuer/3:Subject
      } else {
        throw new GuanxiException("cannot sign assertion as no Assertion child nodes");
      }

      assertionNode.insertBefore(sig.getElement(), assertionSubjectNode);
      Transforms transforms = new Transforms(sig.getDocument());
      transforms.addTransform("http://www.w3.org/2000/09/xmldsig#enveloped-signature");
      transforms.addTransform("http://www.w3.org/2001/10/xml-exc-c14n#");
      transforms.item(1).getElement().appendChild((new InclusiveNamespaces(inDocToSign, "#default saml samlp ds code kind rw typens")).getElement());
      if (elementIDToSign != null && !elementIDToSign.equals("")) {
        sig.addDocument("#" + elementIDToSign, transforms);
      } else {
        sig.addDocument("", transforms);
      }

      X509Certificate cert = (X509Certificate) ks.getCertificate(certificateAlias);
      sig.addKeyInfo(cert);
      sig.addKeyInfo(cert.getPublicKey());
      sig.sign(privateKey);

      if (idpConfig.getDebug() != null) {
        if (idpConfig.getDebug().getSypthonAttributeAssertions() != null) {
          if (idpConfig.getDebug().getSypthonAttributeAssertions().equals("yes")) {

            logger.info("=======================================================");
            logger.info("Signed SAML2 Assertion");
            logger.info("");
            logger.info(printDocument(inDocToSign));
            logger.info("");
            logger.info("=======================================================");
          }
        }
      }

      return Utils.base64(inDocToSign);
    } catch (Exception e) {
      throw new GuanxiException(e);
    }

  }

  /**
   * Determines whether to release the login userid of the GuanxiPrincipal as a
   * Subject/NameID in a SAML Response. If this needs to be done, the method
   * adds a dummy attribute called "__NAMEID__" which is picked up later and
   * converted to a Subject/NameID
   *
   * @param mapper the profile specific attribute mapper to use
   * @param relyingParty the entityID of the entity looking for attributes
   * @param attributes the attributes document that will hold the released attribute
   */
  private void processNameID(AttributeMap mapper, String relyingParty, UserAttributesDocument.UserAttributes attributes) {
    if (mapper.shouldReleaseNameID(relyingParty)) {
      AttributorAttribute attribute = attributes.addNewAttribute();
      attribute.setName("__NAMEID__");
    }
  }

  // https://stackoverflow.com/questions/2325388/what-is-the-shortest-way-to-pretty-print-a-org-w3c-dom-document-to-stdout
  private String printDocument(Document doc) throws GuanxiException  {
    ByteArrayOutputStream bos = null;
    try {
      bos = new ByteArrayOutputStream();
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
      transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(bos, "UTF-8")));
      return new String(bos.toByteArray());
    }
    catch(IOException ioe) {
      throw new GuanxiException(ioe);
    }
    catch(TransformerException te) {
      throw new GuanxiException(te);
    }
    finally {
      try {
        if (bos != null) {
          bos.close();
        }
      }
      catch(IOException ioe) {
        logger.error("printDocument can't close stream");
      }
    }
  }

  // Setters
  public void setMessages(MessageSource messages) { this.messages = messages; }
  public void setHttpPOSTView(String httpPOSTView) { this.httpPOSTView = httpPOSTView; }
  public void setHttpRedirectView(String httpRedirectView) { this.httpRedirectView = httpRedirectView; }
  public void setErrorView(String errorView) { this.errorView = errorView; }
  public void setErrorViewDisplayVar(String errorViewDisplayVar) { this.errorViewDisplayVar = errorViewDisplayVar; }
  public void setEncryptAttributes(boolean encryptAttributes) { this.encryptAttributes = encryptAttributes; }
  public void setDoNotEncryptAttributesFor(ArrayList<String> doNotEncryptAttributesFor) { this.doNotEncryptAttributesFor = doNotEncryptAttributesFor; }
  public void setSignAssertionFor(ArrayList<String> signAssertionFor) { this.signAssertionFor = signAssertionFor; }
  public void setVarEngine(VarEngine varEngine) { this.varEngine = varEngine; }
}
