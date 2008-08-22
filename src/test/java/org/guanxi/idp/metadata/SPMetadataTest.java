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
import org.guanxi.common.metadata.SPMetadataManager;
import org.guanxi.common.metadata.Metadata;
import org.guanxi.common.metadata.SPMetadata;
import org.quartz.*;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.BaseCalendar;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Unit test to give the metadata management a good going over
 */
public class SPMetadataTest extends MetadataTest {
  @Test
  public void test() {
    try {
      SAML2MetadataParserConfig config = new SAML2MetadataParserConfig();
      config.setMetadataURL("file:///" + new File(SPMetadataTest.class.getResource("/metadata.xml").getPath()).getCanonicalPath());
      config.setWho("TEST");
      config.setKey("TEST_KEY");
      config.setCronLine("10 0/59 * * * ?");
      config.setServletContext(servletContext);
      config.setLog(Logger.getLogger(SPMetadataTest.class));

      JobDetail jobDetail = new JobDetail("TEST_KEY", Scheduler.DEFAULT_GROUP,
                                          Class.forName("org.guanxi.idp.job.SAML2MetadataParser"));
      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put(GuanxiJobConfig.JOB_KEY_JOB_CONFIG, config);
      jobDetail.setJobDataMap(jobDataMap);

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
      SPMetadataManager manager = SPMetadataManager.getManager(servletContext);
      Assert.assertNotNull(manager);

      SPMetadata spMetadata = manager.getMetadata("urn:mace:ac.uk:sdss.ac.uk:provider:service:target.iay.org.uk");
      Assert.assertNotNull(spMetadata);

      Assert.assertEquals("urn:mace:ac.uk:sdss.ac.uk:provider:service:target.iay.org.uk", spMetadata.getEntityID());
    }
    catch(Exception e) {
      fail(e.getMessage());
    }
  }
}
