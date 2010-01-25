<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  String url = (String)request.getAttribute("wbsso_acs_endpoint");
  url += "?SAMLEncoding=urn:oasis:names:tc:SAML:2.0:bindings:URL-Encoding:DEFLATE";
  url += "&SAMLResponse=" + (String)request.getAttribute("SAMLResponse");
  url += "&RelayState=" + (String)request.getAttribute("RelayState");
%>
<html>
  <head>
    <meta http-equiv="refresh" content="0;url=<%= url %>">
  </head>
</html>