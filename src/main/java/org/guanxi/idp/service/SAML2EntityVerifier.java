/* CVS Header
   $
   $
*/

package org.guanxi.idp.service;

import org.guanxi.xal.saml_2_0.metadata.EntityDescriptorType;

import javax.servlet.http.HttpServletRequest;

public interface SAML2EntityVerifier {
  public boolean verify(EntityDescriptorType entityDescriptor, HttpServletRequest request);
}
