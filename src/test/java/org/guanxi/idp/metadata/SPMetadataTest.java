/* CVS Header
   $
   $
*/

package org.guanxi.idp.metadata;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.guanxi.idp.job.SAML2MetadataParser;
import org.guanxi.common.job.GuanxiJobConfig;
import org.guanxi.common.job.SAML2MetadataParserConfig;
import org.guanxi.common.metadata.SPMetadata;
import org.guanxi.common.entity.EntityFarm;
import org.guanxi.common.entity.EntityManager;
import org.guanxi.common.definitions.Guanxi;
import org.quartz.*;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.BaseCalendar;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.io.File;
import java.util.HashMap;

/**
 * Unit test to give the metadata management a good going over
 */
public class SPMetadataTest extends MetadataTest {
  @Test
  public void test() {
    try {
      // Initialise Spring
      XmlWebApplicationContext ctx = new XmlWebApplicationContext();
      ctx.setConfigLocations(metadataConfigFiles);
      ctx.setServletContext(servletContext);
      ctx.refresh();

      // Get the parser job from Spring and reconfigure it with the test settings
      SAML2MetadataParserConfig config = (SAML2MetadataParserConfig)ctx.getBean("idpUKFederationMetadataParser");
      String metadataURL = "file:///" + new File(SPMetadataTest.class.getResource("/metadata.xml").getPath()).getCanonicalPath();
      config.setMetadataURL(metadataURL);
      config.setWho("TEST");
      config.setKey("TEST_KEY");
      config.setCronLine("10 0/59 * * * ?");
      config.setServletContext(servletContext);
      config.setSigned(false);
      config.init();

      // Get the metdata farm from Spring and reconfigure it with the test settings
      EntityFarm farm = (EntityFarm)ctx.getBean("idpEntityFarm");
      HashMap<String, EntityManager> managers = new HashMap<String, EntityManager>();
      managers.put(metadataURL, (EntityManager)ctx.getBean("idpEntityManager"));
      farm.setEntityManagers(managers);
      servletContext.setAttribute(Guanxi.CONTEXT_ATTR_IDP_ENTITY_FARM, farm);

      // Initialise the test job settings
      JobDetail jobDetail = new JobDetail("TEST_KEY", Scheduler.DEFAULT_GROUP,
                                          Class.forName("org.guanxi.idp.job.SAML2MetadataParser"));
      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put(GuanxiJobConfig.JOB_KEY_JOB_CONFIG, config);
      jobDetail.setJobDataMap(jobDataMap);

      // Get Quartz ready
      Trigger trigger = new CronTrigger(config.getKey(), Scheduler.DEFAULT_GROUP);
      Scheduler scheduler = new StdSchedulerFactory().getScheduler();

      // Get a new bundle ready. We don't care about dates as we'll run the job manually
      TriggerFiredBundle bundle = new TriggerFiredBundle(jobDetail, trigger, new BaseCalendar(),
                                                         false, null, null, null, null);

      // Get the job and its context ready...
      SAML2MetadataParser parserJob = new SAML2MetadataParser();
      // ...and run the job
      parserJob.execute(new JobExecutionContext(scheduler, bundle, new SAML2MetadataParser()));

      // Test the metadata management
      File metadataCacheFile = new File(config.getMetadataCacheFile());
      Assert.assertTrue(metadataCacheFile.exists());
      metadataCacheFile.delete();
      EntityManager manager = farm.getEntityManagerForSource(metadataURL);
      Assert.assertNotNull(manager);
      manager = farm.getEntityManagerForID("urn:bond:hq");
      Assert.assertNotNull(manager);
      SPMetadata spMetadata = (SPMetadata)manager.getMetadata("urn:bond:hq");
      Assert.assertNotNull(spMetadata);
      Assert.assertEquals("urn:bond:hq", spMetadata.getEntityID());
      Assert.assertEquals("https://bond.hq.ac.uk/SSO/SAML/BrowserPost", spMetadata.getAssertionConsumerServiceURLs()[0]);
    }
    catch(Exception e) {
      fail(e.getMessage());
    }
  }
}
