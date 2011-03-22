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

package org.guanxi.idp.service;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.keys.KeyInfo;
import org.bouncycastle.openssl.PEMWriter;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.Utils;
import org.guanxi.xal.idp.*;
import org.guanxi.xal.saml_2_0.metadata.EntityDescriptorType;
import org.guanxi.xal.saml_2_0.metadata.KeyDescriptorType;
import org.guanxi.xal.saml_2_0.metadata.KeyTypes;
import org.guanxi.xal.saml_2_0.assertion.*;
import org.guanxi.common.definitions.Shibboleth;
import org.guanxi.common.definitions.SAML;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.definitions.EduPersonOID;
import org.guanxi.common.entity.EntityFarm;
import org.guanxi.common.entity.EntityManager;
import org.guanxi.common.metadata.SPMetadata;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.security.SecUtilsConfig;
import org.guanxi.idp.util.AttributeMap;
import org.guanxi.idp.util.ARPEngine;
import org.guanxi.idp.farm.attributors.Attributor;
import org.guanxi.xal.saml_2_0.protocol.*;
import org.guanxi.xal.w3.xmldsig.KeyInfoDocument;
import org.guanxi.xal.w3.xmldsig.X509DataType;
import org.guanxi.xal.w3.xmlenc.EncryptedDataDocument;
import org.guanxi.xal.w3.xmlenc.EncryptedDataType;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.context.ServletContextAware;
import org.springframework.context.MessageSource;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.HashMap;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;

/**
 * Base class for SSO profile handlers
 * 
 * @author alistair
 */
public abstract class SSOBase extends AbstractController implements ServletContextAware {
  protected static final String ENTITY_IDP = "ENTITY_IDP";
  protected static final String ENTITY_SP = "ENTITY_SP";
  protected static final String SIGNING_CERT = "SIGNING_CERT";
  protected static final String ENCRYPTION_CERT = "ENCRYPTION_CERT";

  /** Our logger */
  protected Logger logger = null;
  /** The localised messages to use */
  protected MessageSource messages = null;
  /** Our config */
  protected IdpDocument.Idp idpConfig;
  /** The name of the default SP entry in the config file to use */
  protected String defaultSPEntry = null;
  /** The number of seconds assertions should be valid for */
  protected int assertionTimeLimit;
  /** The entityID to use when sending a SAML Response */
  protected String idpEntityID = null;
  /** The name qualifier to use when sending a SAML Response */
  protected String nameQualifier = null;
  /** The name qualifier format to use when sending a SAML Response */
  protected String nameQualifierFormat = null;
  /** The signing credentials to use */
  protected Creds credsConfig = null;
  /** The security information to use when signing a response */
  protected SecUtilsConfig secUtilsConfig = null;
  /** SAML2 namesapces for writing SAML */
  protected HashMap<String, String> saml2Namespaces = null;
  /** XML output options */
  protected XmlOptions xmlOptions = null;
  /** Our profile specific attribute mapper */
  protected AttributeMap mapper = null;
  /** Our ARP engine */
  protected ARPEngine arpEngine = null;
  /** The Attributors to use to get attributes */
  protected org.guanxi.idp.farm.attributors.Attributor[] attributor = null;

  public void init() {
    logger = Logger.getLogger(this.getClass().getName());
    saml2Namespaces = new HashMap<String, String>();
    saml2Namespaces.put(SAML.NS_SAML_20_PROTOCOL, SAML.NS_PREFIX_SAML_20_PROTOCOL);
    saml2Namespaces.put(SAML.NS_SAML_20_ASSERTION, SAML.NS_PREFIX_SAML_20_ASSERTION);
    xmlOptions = new XmlOptions();
    xmlOptions.setSavePrettyPrint();
    xmlOptions.setSavePrettyPrintIndent(2);
    xmlOptions.setUseDefaultNamespace();
    xmlOptions.setSaveAggressiveNamespaces();
    xmlOptions.setSaveNamespacesFirst();    
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
  public abstract ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception;

  /**
   * Loads the identity and credential information based on the relying party.
   * These determine what entityID and signing credentials to use.
   *
   * @param request Servlet request
   */
  protected void loadPersona(HttpServletRequest request) {
    /* Now load the appropriate identity and creds from the config file.
     * We'll either use the default or the ones that the particular SP
     * needs to be sent.
     */
    String spID = null;
    ServiceProvider[] spList = idpConfig.getServiceProviderArray();
    for (int c=0; c < spList.length; c++) {
      if (spList[c].getName().equals(request.getParameter(Shibboleth.PROVIDER_ID))) {
        spID = request.getParameter(Shibboleth.PROVIDER_ID);
      }
    }
    if (spID == null) {
      // No specific requirement for this SP so use the default identity and creds
      spID = defaultSPEntry;
    }

    // Now we've sorted the SP id to use, load the identity and creds
    for (int c=0; c < spList.length; c++) {
      if (spList[c].getName().equals(spID)) {
        String identityToUse = spList[c].getIdentity();
        String credsToUse = spList[c].getCreds();

        // We've found the <service-provider> node so look for the corresponding <identity> node
        org.guanxi.xal.idp.Identity[] ids = idpConfig.getIdentityArray();
        for (int cc=0; cc < ids.length; cc++) {
          if (ids[cc].getName().equals(identityToUse)) {
            idpEntityID = ids[cc].getIssuer();
            nameQualifier = ids[cc].getNameQualifier();
            nameQualifierFormat = ids[cc].getFormat();
          }
        }

        // Look for the corresponding <creds> node
        org.guanxi.xal.idp.Creds[] creds = idpConfig.getCredsArray();
        for (int ccc=0; ccc < creds.length; ccc++) {
          if (creds[ccc].getName().equals(credsToUse)) {
            credsConfig = creds[ccc];
          }
        }
      }
    }

    // Initialise the signing information
    secUtilsConfig = new SecUtilsConfig();
    secUtilsConfig.setKeystoreFile(credsConfig.getKeystoreFile());
    secUtilsConfig.setKeystorePass(credsConfig.getKeystorePassword());
    secUtilsConfig.setKeystoreType("JKS");
    secUtilsConfig.setPrivateKeyAlias(credsConfig.getPrivateKeyAlias());
    secUtilsConfig.setPrivateKeyPass(credsConfig.getPrivateKeyPassword());
    secUtilsConfig.setCertificateAlias(credsConfig.getCertificateAlias());
  }

  /**
   * Loads a Service Provider's metadata from the store.
   *
   * @param spEntityID the SP's entityID
   * @return EntityDescriptorType for the SP
   */
  protected EntityDescriptorType getSPMetadata(String spEntityID) {
    EntityFarm farm = (EntityFarm)getServletContext().getAttribute(Guanxi.CONTEXT_ATTR_IDP_ENTITY_FARM);
    EntityManager manager = farm.getEntityManagerForID(spEntityID);
    SPMetadata metadata = (SPMetadata)manager.getMetadata(spEntityID);
    return (EntityDescriptorType)metadata.getPrivateData();
  }

  /**
   * Extracts the appropriate X509 certificate from an entity's SAML2 metadata
   *
   * @param saml2Metadata the entity's SAML2 metadata
   * @param entityType the type of the entity. ENTITY_IDP or ENTITY_SP
   * @param certType the type of cert to get. SIGNING_CERT or ENCRYPTION_CERT
   * @return the X509Certificate or null if it doesn't recognise a param
   * @throws GuanxiException if an error occurs
   */
  protected X509Certificate getX509CertFromMetadata(EntityDescriptorType saml2Metadata, String entityType, String certType) throws GuanxiException {
    try {
      KeyTypes.Enum keyType;
      if (certType.equals(SIGNING_CERT)) {
        keyType = KeyTypes.SIGNING;
      }
      else if (certType.equals(ENCRYPTION_CERT)) {
        keyType = KeyTypes.ENCRYPTION;
      }
      else {
        return null;
      }

      KeyDescriptorType[] keyDescriptors = null;
      if (entityType.equals(ENTITY_IDP)) {
        keyDescriptors = saml2Metadata.getIDPSSODescriptorArray(0).getKeyDescriptorArray();
      }
      else if (entityType.equals(ENTITY_SP)) {
        keyDescriptors = saml2Metadata.getSPSSODescriptorArray(0).getKeyDescriptorArray();
      }
      else {
        return null;
      }
      
      for (KeyDescriptorType keyDescriptor : keyDescriptors) {
        if (keyDescriptor.getUse() != null) {
          if (keyDescriptor.getUse().equals(keyType)) {
            return getCertFromKeyDescriptor(keyDescriptor);
          }
        }
      }

      /* If we get here there are no keys specifically marked for this type of usage
       * so use the first one in the list
       */
      return getCertFromKeyDescriptor(keyDescriptors[0]);
    }
    catch(Exception e) {
      throw new GuanxiException(e);
    }
  }

  /**
   * Create a SAML2 AttributeStatement based on the attributes released from an Attributor farm
   *
   * @param guanxiAttrFarmOutput the attributes released from an Attributor farm
   * @param entityID the entityID of the relying party that wants the attributes
   * @return AttributeStatementDocument containing the AttributeStatement
   */
  protected AttributeStatementDocument getSAML2AttributeStatementFromFarm(UserAttributesDocument guanxiAttrFarmOutput,
                                                                          String entityID,
                                                                          SubjectType subject,
                                                                          GuanxiPrincipal gxPrincipal) {
    AttributeStatementDocument attrStatementDoc = AttributeStatementDocument.Factory.newInstance();
    AttributeStatementType attrStatement = attrStatementDoc.addNewAttributeStatement();

    boolean hasAttrs = false;
    for (int c=0; c < guanxiAttrFarmOutput.getUserAttributes().getAttributeArray().length; c++) {
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
      XmlObject attrValue = null;
      if (attribute == null) {
        if (attributorAttr.getName().equalsIgnoreCase("__NAMEID__")) {
          NameIDType nameID = subject.addNewNameID();
          nameID.setFormat(gxPrincipal.getNameIDFormat());
          nameID.setStringValue(gxPrincipal.getName());
        }
        else {
          hasAttrs = true;
          attribute = attrStatement.addNewAttribute();
          attribute.setName(EduPersonOID.ATTRIBUTE_NAME_PREFIX + attributorAttr.getName());
          attribute.setFriendlyName(attributorAttr.getFriendlyName());
          attribute.setNameFormat(SAML.SAML2_ATTRIBUTE_NAME_FORMAT_URI);
          attrValue = attribute.addNewAttributeValue();
        }
      }

      // Deal with scoped eduPerson attributes
      if (attribute != null) {
        if (attribute.getName().equals(EduPersonOID.ATTRIBUTE_NAME_PREFIX + EduPersonOID.OID_EDUPERSON_TARGETED_ID)) {
          NameIDDocument nameIDDoc = NameIDDocument.Factory.newInstance();
          NameIDType nameID = nameIDDoc.addNewNameID();
          nameID.setFormat(SAML.SAML2_ATTRIBUTE_FORMAT_NAMEID_PERSISTENT);
          nameID.setNameQualifier(nameQualifier);
          nameID.setSPNameQualifier(entityID);
          // For SAML2 we need to remove the scope from the value
          if (attributorAttr.getValue().contains("@")) {
            attributorAttr.setValue(attributorAttr.getValue().split("@")[0]);
          }
          nameID.setStringValue(attributorAttr.getValue());
  
          attrValue.getDomNode().appendChild(attrValue.getDomNode().getOwnerDocument().importNode(nameID.getDomNode(), true));
        }
        else {
          Text valueNode = attrValue.getDomNode().getOwnerDocument().createTextNode(attributorAttr.getValue());
          attrValue.getDomNode().appendChild(valueNode);
        }
      }
    } // for (int c=0; c < guanxiAttrFarmOutput.getUserAttributes().getAttributeArray().length; c++)

    if (hasAttrs)
      return attrStatementDoc;
    else
      return null;
  }

  /**
   * Adds encrypted assertions to a SAML2 Response
   *
   * @param encryptionCert the X509 certificate to use for encrypting the assertions
   * @param assertionDoc the assertions to encrypt
   * @param responseDoc the SAML2 Response to add the encrypted assertions to
   * @throws GuanxiException if an error occurs
   */
  protected void addEncryptedAssertionsToResponse(X509Certificate encryptionCert, AssertionDocument assertionDoc, ResponseDocument responseDoc) throws GuanxiException {
    try {
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
      responseDoc.getResponse().addNewEncryptedAssertion().setEncryptedData(encryptedDataDoc.getEncryptedData());

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
    }
    catch(NoSuchAlgorithmException nsae) {
      logger.error("AES encryption not available");
      throw new GuanxiException(nsae);
    }
    catch(XMLEncryptionException xea) {
      logger.error("RSA_OAEP error with WRAP_MODE");
      throw new GuanxiException(xea);
    }
    catch(Exception e) {
      logger.error("Error encyrpting the assertion");
      throw new GuanxiException(e);
    }
  }

  protected ResponseDocument buidErrorResponse(String entityID, String requestID, String acsURL, String statusCode) {
    // Response
    ResponseDocument responseDoc = ResponseDocument.Factory.newInstance();
    ResponseType wbssoResponse = responseDoc.addNewResponse();
    wbssoResponse.setID(generateStringID());
    wbssoResponse.setVersion("2.0");
    wbssoResponse.setDestination(acsURL);
    wbssoResponse.setIssueInstant(Calendar.getInstance());
    wbssoResponse.setInResponseTo(requestID);
    Utils.zuluXmlObject(wbssoResponse, 0);

    // Response/Issuer
    NameIDType issuer = wbssoResponse.addNewIssuer();
    issuer.setFormat(SAML.URN_SAML2_NAMEID_FORMAT_ENTITY);
    issuer.setStringValue(entityID);

    // Response/Status
    StatusDocument statusDoc = StatusDocument.Factory.newInstance();
    StatusType status = statusDoc.addNewStatus();
    StatusCodeType topLevelStatusCode = status.addNewStatusCode();
    topLevelStatusCode.setValue(SAML.SAML2_STATUS_RESPONDER);
    StatusCodeType secondLevelStatusCode = topLevelStatusCode.addNewStatusCode();
    secondLevelStatusCode.setValue(statusCode);
    wbssoResponse.setStatus(status);

    return responseDoc;
  }

  protected String generateStringID() {
    SecureRandom random = new SecureRandom();
    return "gx" + new BigInteger(130, random).toString(32);
  }

  /**
   * Extracts the X509 cenrtificate from a KeyDescriptor
   *
   * @param keyDescriptor the KeyDescriptor containing the X509 certificate
   * @return X509Certificate
   * @throws GuanxiException if an error occurs
   */
  private X509Certificate getCertFromKeyDescriptor(KeyDescriptorType keyDescriptor) throws GuanxiException {
    try {
      byte[] spCertBytes = keyDescriptor.getKeyInfo().getX509DataArray(0).getX509CertificateArray(0);
      CertificateFactory certFactory = CertificateFactory.getInstance("x.509");
      ByteArrayInputStream certByteStream = new ByteArrayInputStream(spCertBytes);
      X509Certificate metadataCert = (X509Certificate)certFactory.generateCertificate(certByteStream);
      certByteStream.close();
      return metadataCert;
    }
    catch(CertificateException ce) {
      logger.error("can't get x509 from KeyDescriptor");
      throw new GuanxiException(ce);
    }
    catch(IOException ioe) {
      logger.error("can't close cert byte stream");
      throw new GuanxiException(ioe);
    }
  }

  // Setters
  public void setDefaultSPEntry(String defaultSPEntry) { this.defaultSPEntry = defaultSPEntry; }
  public void setAssertionTimeLimit(int assertionTimeLimit) { this.assertionTimeLimit = assertionTimeLimit; }
  public void setMapper(AttributeMap mapper) { this.mapper = mapper; }
  public void setArpEngine(ARPEngine arpEngine) { this.arpEngine = arpEngine; }
  public void setAttributor(Attributor[] attributor) { this.attributor = attributor; }
}
