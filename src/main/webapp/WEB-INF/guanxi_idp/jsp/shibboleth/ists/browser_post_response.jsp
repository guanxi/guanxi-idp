<%@ page import="org.guanxi.common.definitions.Shibboleth" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Browser/POST Response</title>
  </head>
  <body Onload="document.forms[0].submit()">
    <form method="POST" action="<%= request.getParameter(Shibboleth.SHIRE) %>">
      <input type="hidden" name="SAMLResponse" value="<%= request.getAttribute("saml_response") %>">
      <input type="hidden" name="TARGET" value="<%= request.getParameter("target") %>">
    </form>
  </body>
</html>