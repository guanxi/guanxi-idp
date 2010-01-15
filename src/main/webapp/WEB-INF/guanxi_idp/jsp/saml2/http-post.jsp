<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <body Onload="document.forms[0].submit()">
    <form method="POST" action="<%= request.getAttribute("wbsso_acs_endpoint") %>">
      <input type="hidden" name="SAMLResponse" value="<%= request.getAttribute("SAMLResponse") %>">
      <input type="hidden" name="RelayState" value="<%= request.getAttribute("RelayState") %>">
    </form>
  </body>
</html>