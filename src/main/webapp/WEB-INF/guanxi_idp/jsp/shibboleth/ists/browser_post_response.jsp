<%@ page import="org.guanxi.common.definitions.Shibboleth" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Browser/POST Response</title>
  </head>
  <body Onload="document.forms[0].submit()">
    <noscript>
      <p>
        <strong>Note:</strong> Since your browser does not support JavaScript,
        you must press the Continue button once to proceed.
      </p>
    </noscript>

    <form method="POST" action="<%= request.getParameter(Shibboleth.SHIRE) %>">
      <div>
        <input type="hidden" name="SAMLResponse" value="<%= request.getAttribute("saml_response") %>">
        <input type="hidden" name="TARGET" value="<%= request.getParameter("target") %>">
      </div>
      <noscript>
        <div>
          <input type="submit" value="Continue"/>
        </div>
      </noscript>
    </form>
  </body>
</html>