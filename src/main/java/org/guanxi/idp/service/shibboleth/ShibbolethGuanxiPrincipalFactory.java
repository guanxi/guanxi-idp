/* CVS Header
   $
   $
*/

package org.guanxi.idp.service.shibboleth;

import org.guanxi.common.GuanxiPrincipalFactory;
import org.guanxi.common.GuanxiPrincipal;
import org.guanxi.common.Utils;
import org.guanxi.common.definitions.Shibboleth;

import javax.servlet.http.HttpServletRequest;

public class ShibbolethGuanxiPrincipalFactory implements GuanxiPrincipalFactory {
  private String cookieName = null;

  public GuanxiPrincipal createNewGuanxiPrincipal(HttpServletRequest request) {
    GuanxiPrincipal gxPrincipal = new GuanxiPrincipal();

    // Set the session ID... This will be the NameIdentifier
    gxPrincipal.setID(Utils.getUniqueID());
    // ...only allow the current SP to access it...
    gxPrincipal.setProviderID(request.getParameter(Shibboleth.PROVIDER_ID));

    return gxPrincipal;
  }

  public void setCookieName(String cookieName) { this.cookieName = cookieName; }
  public String getCookieName() { return cookieName; }
}
