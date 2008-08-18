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

package org.guanxi.idp;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.RequestHandledEvent;
import org.guanxi.common.definitions.Guanxi;
import org.guanxi.common.GuanxiException;
import org.guanxi.common.metadata.SPMetadataManager;
import org.guanxi.common.job.GuanxiJobConfig;
import org.guanxi.xal.idp.IdpDocument;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.openssl.PEMWriter;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.servlet.ServletContext;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Date;
import java.io.*;
import java.math.BigInteger;
import java.text.ParseException;

public class Bootstrap implements ApplicationListener, ApplicationContextAware, ServletContextAware {
  private static final Logger logger = Logger.getLogger(Bootstrap.class.getName());
  /** Spring ApplicationContext */
  @SuppressWarnings("unused")
  private ApplicationContext applicationContext = null;
  /** The servlet context */
  private ServletContext servletContext = null;
  private String configFile = null;
  /** If this instance of an Engine loads the BouncyCastle security provider then it should unload it */
  private boolean okToUnloadBCProvider = false;
  /** The background jobs to start */
  private GuanxiJobConfig[] gxJobs = null;
  /** Where the metadata manager get initialised and saved */
  private String metadataCacheFile = null;

  public void setConfigFile(String configFile) { this.configFile = configFile; }
  public String getConfigFile() { return configFile; }
  public void setGxJobs(GuanxiJobConfig[] gxJobs) { this.gxJobs = gxJobs; }
  public String getMetadataCacheFile() { return metadataCacheFile; }
  public void setMetadataCacheFile(String metadataCacheFile) { this.metadataCacheFile = metadataCacheFile; }

  /**
   * Initialise the interceptor
   */
  public void init() {
    try {
      /* If we try to add the BouncyCastle provider but another Guanxi::SP running
       * in another webapp in the same container has already done so, then we'll get
       * -1 returned from the method, in which case, we should leave unloading of the
       * provider to the particular Guanxi::SP that loaded it.
       */
      if ((Security.addProvider(new BouncyCastleProvider())) != -1) {
        // We've loaded it, so we should unload it
        okToUnloadBCProvider = true;
      }

      IdpDocument configDoc = IdpDocument.Factory.parse(new File(servletContext.getRealPath(configFile)));
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG_DOC, configDoc);
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG, configDoc.getIdp());

      // Sort out the cookie's age
      int cookieAge = -1;
      String cookieMaxAge = configDoc.getIdp().getCookie().getAge().getStringValue();
      String cookieAgeUnits = configDoc.getIdp().getCookie().getAge().getUnits().toString();
      if (cookieAgeUnits.equals("seconds")) cookieAge = Integer.parseInt(cookieMaxAge);
      else if (cookieAgeUnits.equals("minutes")) cookieAge = Integer.parseInt(cookieMaxAge) * 60;
      else if (cookieAgeUnits.equals("hours")) cookieAge = Integer.parseInt(cookieMaxAge) * 3600;
      else if (cookieAgeUnits.equals("days")) cookieAge = Integer.parseInt(cookieMaxAge) * 86400;
      else if (cookieAgeUnits.equals("weeks")) cookieAge = Integer.parseInt(cookieMaxAge) * 604800;
      else if (cookieAgeUnits.equals("months")) cookieAge = Integer.parseInt(cookieMaxAge) * 2419200;
      else if (cookieAgeUnits.equals("years")) cookieAge = Integer.parseInt(cookieMaxAge) * 29030400;
      else if (cookieAgeUnits.equals("transient")) cookieAge = -1;

      String cookieDomain = (configDoc.getIdp().getCookie().getDomain() == null) ? "" :
                            configDoc.getIdp().getCookie().getDomain();

      // Register the IdP's ID and cookie details in case we're embedded
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_ID, configDoc.getIdp().getID());
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_PREFIX, configDoc.getIdp().getCookie().getPrefix());
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_NAME,
                                  configDoc.getIdp().getCookie().getPrefix() +
                                  configDoc.getIdp().getID());
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_DOMAIN, cookieDomain);
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_COOKIE_AGE, new Integer(cookieAge));

      setup();

      startJobs();
    }
    catch(Exception e) {
    }
  }

  /**
   * Called by Spring to give us the ApplicationContext
   *
   * @param applicationContext Spring ApplicationContext
   * @throws org.springframework.beans.BeansException
   */
  public void setApplicationContext(ApplicationContext applicationContext) throws org.springframework.beans.BeansException {
    this.applicationContext = applicationContext;
  }

  /**
   * Sets the servlet context
   * @param servletContext The servlet context
   */
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /**
   * Called by Spring when application events occur. At the moment we handle:
   * ContextClosedEvent
   * ContextRefreshedEvent
   * RequestHandledEvent
   *
   * This is where we inject the job controllers into the application context, each one
   * under it's own key.
   *
   * @param applicationEvent Spring application event
   */
  public void onApplicationEvent(ApplicationEvent applicationEvent) {
    if (applicationEvent instanceof ContextStartedEvent) {
      try {
        loadMetadata();
      }
      catch (Exception e) {
        logger.error("Unable to load cached metadata", e);
      }
    }

    if (applicationEvent instanceof ContextStoppedEvent) {
      try {
        saveMetadata();
      }
      catch (Exception e) {
        logger.error("Unable to save metadata to cache", e);
      }
    }

    if (applicationEvent instanceof ContextRefreshedEvent) {
      logger.info("Bootstrap init");
    }

    if (applicationEvent instanceof ContextClosedEvent) {
      if (okToUnloadBCProvider) {
        Provider[] providers = Security.getProviders();

        /* Although addProvider() returns the ID of the newly installed provider,
         * we can't rely on this. If another webapp removes a provider from the list of
         * installed providers, all the other providers shuffle up the list by one, thus
         * invalidating the ID we got from addProvider().
         */
        try {
          for (int i=0; i < providers.length; i++) {
            if (providers[i].getName().equalsIgnoreCase(Guanxi.BOUNCY_CASTLE_PROVIDER_NAME)) {
              Security.removeProvider(Guanxi.BOUNCY_CASTLE_PROVIDER_NAME);
            }
          }
        }
        catch(SecurityException se) {
          /* We'll end up here if a security manager is installed and it refuses us
           * permission to remove the BouncyCastle provider
           */
        }
      }
    }

    if (applicationEvent instanceof RequestHandledEvent) {
    }
  }

  private void setup() throws GuanxiException {
    String KEYSTORE_FILE = "/WEB-INF/guanxi_idp/keystore/guanxi_idp.jks";
    String KEYSTORE_TYPE = "jks";
    String KEYSTORE_PRIVATE_KEY_ALIAS = "idp";

    if (!keystoreExists(servletContext.getRealPath(KEYSTORE_FILE))) {
      Random randomNumberGenerator = new Random();
      String certCN = getCNPrefix() + String.valueOf(randomNumberGenerator.nextInt());
      String keystorePassword = String.valueOf(randomNumberGenerator.nextInt());

      /* Create the keystore. Note that the keystore password and key entry password
       * must be the same for Tomcat to load the keystore
       */
      try {
        if (createSelfSignedKeystore(certCN, servletContext.getRealPath(KEYSTORE_FILE),
                                     keystorePassword, keystorePassword, KEYSTORE_PRIVATE_KEY_ALIAS)) {
          createConfigFile(certCN, certCN, KEYSTORE_TYPE, servletContext.getRealPath(KEYSTORE_FILE),
                           keystorePassword, KEYSTORE_PRIVATE_KEY_ALIAS, keystorePassword, KEYSTORE_PRIVATE_KEY_ALIAS);
        }
      }
      catch(IOException ioe) {
        throw new GuanxiException(ioe);
      }
    }
  }

  private String getCNPrefix() {
    // Are we running inside Bodington?
    File markerFile = new File(servletContext.getRealPath("WEB-INF/bodington.properties"));
    if (markerFile.exists())
      return "BODGUANXI-";
    else
      return "GUANXI-";
  }

  private boolean keystoreExists(String keystoreFile) {
    return new File(keystoreFile).exists();
  }

  public boolean createSelfSignedKeystore(String cn, String keystoreFile, String keystorePassword,
                                          String privateKeyPassword, String privateKeyAlias) {
    KeyStore ks = null;

    try {
      ks = KeyStore.getInstance("JKS");
      ks.load(null, null);

      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
      keyGen.initialize(1024, new SecureRandom());
      KeyPair keypair = keyGen.generateKeyPair();
      PrivateKey privkey = keypair.getPrivate();
      PublicKey pubkey = keypair.getPublic();

      Hashtable<DERObjectIdentifier, String> attrs = new Hashtable<DERObjectIdentifier, String>();
      Vector<DERObjectIdentifier> ordering = new Vector<DERObjectIdentifier>();
      ordering.add(X509Name.CN);
      attrs.put(X509Name.CN, cn);
      X509Name issuerDN = new X509Name(ordering, attrs);
      X509Name subjectDN = new X509Name(ordering, attrs);

      Date validFrom = new Date();
      validFrom.setTime(validFrom.getTime() - (10 * 60 * 1000));
      Date validTo = new Date();
      validTo.setTime(validTo.getTime() + (20 * (24 * 60 * 60 * 1000)));

      X509V3CertificateGenerator x509 = new X509V3CertificateGenerator();
      x509.setSignatureAlgorithm("SHA1withDSA");
      x509.setIssuerDN(issuerDN);
      x509.setSubjectDN(subjectDN);
      x509.setPublicKey(pubkey);
      x509.setNotBefore(validFrom);
      x509.setNotAfter(validTo);
      x509.setSerialNumber(new BigInteger(128, new Random()));

      X509Certificate[] cert = new X509Certificate[1];
      cert[0] = x509.generate(privkey, "BC");
      java.security.cert.Certificate[] chain = new java.security.cert.Certificate[1];
      chain[0] = cert[0];

      ks.setKeyEntry(privateKeyAlias, privkey, privateKeyPassword.toCharArray(), cert);
      ks.setKeyEntry(privateKeyAlias, privkey, privateKeyPassword.toCharArray(), chain);
      ks.store(new FileOutputStream(keystoreFile), keystorePassword.toCharArray());

      String IDP_RFC_CERT = "WEB-INF/guanxi_idp/keystore/guanxi_idp_cert.txt";

      PEMWriter pemWriter = new PEMWriter(new FileWriter(servletContext.getRealPath(IDP_RFC_CERT)));
      pemWriter.writeObject(cert[0]);
      pemWriter.close();

      return true;
    }
    catch(Exception se) {
      return false;
    }
  }

  private void createConfigFile(String issuer, String nameQualifier, String ksType,
                                String ksFile, String ksPassword, String privKeyAlias,
                                String privKeyPassword, String certAlias) throws IOException {

    String SSO_CONFIG_FILE = "/WEB-INF/guanxi_idp/config/idp.xml";
    String KEYSTORE_KEY_TYPE = "dsa";

    IdpDocument idpDoc = null;
    try {
      idpDoc = IdpDocument.Factory.parse(new File(servletContext.getRealPath(SSO_CONFIG_FILE)));
    }
    catch(XmlException xe) {
      logger.error("Can't create config file", xe);
      return;
    }

    IdpDocument.Idp idp = idpDoc.getIdp();

    idp.getServiceProviderArray(0).setIdentity("exampleIdentity");
    idp.getServiceProviderArray(0).setCreds("exampleCreds");
    idp.getServiceProviderArray(0).setName("REPLACE_WITH_PROVIDER_ID_OF_SERVICE_PROVIDER");

    idp.getIdentityArray(0).setName("exampleIdentity");
    idp.getIdentityArray(0).setNameQualifier(nameQualifier);
    idp.getIdentityArray(0).setIssuer(issuer);

    idp.getCredsArray(0).setName("exampleCreds");
    idp.getCredsArray(0).setKeystoreType("jks");
    idp.getCredsArray(0).setKeyType(ksType);
    idp.getCredsArray(0).setKeystoreFile(ksFile);
    idp.getCredsArray(0).setKeystorePassword(ksPassword);
    idp.getCredsArray(0).setPrivateKeyAlias(privKeyAlias);
    idp.getCredsArray(0).setPrivateKeyPassword(privKeyPassword);
    idp.getCredsArray(0).setCertificateAlias(certAlias);
    idp.getCredsArray(0).setKeyType(KEYSTORE_KEY_TYPE);

    XmlOptions xmlOptions = new XmlOptions();
    xmlOptions.setSavePrettyPrint();
    xmlOptions.setSavePrettyPrintIndent(2);
    xmlOptions.setUseDefaultNamespace();

    idpDoc.save(new File(servletContext.getRealPath(SSO_CONFIG_FILE)), xmlOptions);

    servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG_DOC, idpDoc);
    servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG, idpDoc.getIdp());
  }

  private void startJobs() {
    try {
      // Get a new scheduler
      Scheduler scheduler = new StdSchedulerFactory().getScheduler();
      // Start it up. This won't start any jobs though.
      scheduler.start();
      
      for (GuanxiJobConfig gxJob : gxJobs) {
        // Need a new JobDetail to hold custom data to send to the job we're controlling
        JobDetail jobDetail = new JobDetail(gxJob.getKey(), Scheduler.DEFAULT_GROUP, Class.forName(gxJob.getJobClass()));

        // Create a new JobDataMap for custom data to be sent to the job...
        JobDataMap jobDataMap = new JobDataMap();
        // ...and add the job's custom config object
        jobDataMap.put(GuanxiJobConfig.JOB_KEY_JOB_CONFIG, gxJob);

        // Put the job's custom data in it's JobDetail
        jobDetail.setJobDataMap(jobDataMap);

        /* Tell the scheduler when this job will run. Nothing will happen
         * until the start method is called.
         */
        Trigger trigger = new CronTrigger(gxJob.getKey(), Scheduler.DEFAULT_GROUP, gxJob.getCronLine());

        // Start the job
        scheduler.scheduleJob(jobDetail, trigger);

        if (gxJob.isStartImmediately()) {
          scheduler.triggerJob(gxJob.getKey(), Scheduler.DEFAULT_GROUP);
        }
      }
    }
    catch(ClassNotFoundException cnfe) {
      logger.error("Error locating job class", cnfe);
    }
    catch(SchedulerException se) {
      logger.error("Job scheduling error", se);
    }
    catch(ParseException pe) {
      logger.error("Error parsing job cronline", pe);
    }
  }

  private void loadMetadata() throws IOException, XmlException {
    File metadata_file = new File(metadataCacheFile);
    if (metadata_file.exists()) {
      InputStream in = new FileInputStream(metadata_file);
      try {
        SPMetadataManager.getManager(servletContext).read(in);
      }
      finally {
        in.close();
      }
    }
  }

  private void saveMetadata() throws IOException {
    File metadata_file = new File(metadataCacheFile);
    if (metadata_file.exists()) {
      OutputStream out = new FileOutputStream(metadata_file);
      try {
        SPMetadataManager.getManager(servletContext).write(out);
      }
      finally {
        out.close();
      }
    }
  }
}
