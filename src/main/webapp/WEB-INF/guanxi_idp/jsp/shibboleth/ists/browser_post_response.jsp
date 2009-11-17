<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ page import="org.guanxi.common.definitions.Shibboleth" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title><fmt:message key="idp.shibb.page.title"/></title>
    <style type="text/css">
      <!--
      body {
        background-color: #FFFFFF;
        margin-left: 20px;
        margin-top: 20px;
        margin-right: 20px;
        margin-bottom: 20px;
        font-family:Verdana, Arial, Helvetica, sans-serif;
        background-image: url(<%= request.getContextPath() %>/guanxi_idp/images/watermark.gif);
      }
      -->
    </style>
  </head>
  <body Onload="document.forms[0].submit()">
    <div style="border:1px solid black; width:50%; height:10%; background-image:url(<%= request.getContextPath() %>/guanxi_idp/images/formback.gif); background-repeat:repeat-x repeat-y; margin: 0 auto;">
<div style="padding:20px; margin: 0 auto;">
<fmt:message key="idp.shibb.message"/>
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
    </div>
    </div>
  </body>
</html>