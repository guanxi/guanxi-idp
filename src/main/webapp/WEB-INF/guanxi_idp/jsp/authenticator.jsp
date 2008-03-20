<%@ page import="org.guanxi.idp.service.shibboleth.AuthHandler" %>
<%@ page import="java.util.Enumeration" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head><title><fmt:message key="authenticator.page.title"/></title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
    <style type="text/css">
      <!--
      body {
        background-color: #FFFFFF;
        margin-left: 20px;
        margin-top: 20px;
        margin-right: 20px;
        margin-bottom: 20px;
        font-family:Verdana, Arial, Helvetica, sans-serif;
        background-image: url(guanxi_idp/images/watermark.gif);
      }
      -->
    </style>
  </head>
 <body>

 <div style="width:167; height:91; margin: 0 auto; background-image:url(guanxi_idp/images/logo.gif);"></div>
 <br>

 <div style="margin: 0 auto; text-align: center; width:400px; height:80px;">
  <fmt:message key="authentictor.login.message"/>
 </div>

  <div style="border:1px solid black; width:30%; height:125px; background-image:url(guanxi_idp/images/formback.gif); background-repeat:repeat-x repeat-y; margin: 0 auto;">
  	<div style="padding:20px; margin: 0 auto;">

     <form method="post" action="<%= request.getAttribute(AuthHandler.FORM_ACTION_ATTRIBUTE)%>">
      <input type="text" name="userid">&nbsp;<fmt:message key="authenticator.label.username"/><br>
      <input type="password" name="password">&nbsp;<fmt:message key="authenticator.label.password"/><br>
      <input type="hidden" name="guanxi:mode" value="authenticate"><br>
      <input type="submit" name="submit" value="<fmt:message key="authenticator.login.button.text"/>">

      <%
        Enumeration atts = request.getAttributeNames();
        while (atts.hasMoreElements()) {
          String name = (String)atts.nextElement();
          if (name.startsWith(AuthHandler.REQUIRED_PARAM_PREFIX)) {
            %>
              <input type="hidden" name="<%= name.replaceAll(AuthHandler.REQUIRED_PARAM_PREFIX, "") %>" value="<%= request.getAttribute(name) %>">
            <%
          }

        }
      %>
    </form>
    
   </div>
  </div>
  <div style="width:30%; margin: 0 auto;">
    <div align="left"><strong>Guanxi@<fmt:message key="institution.display.name"/></strong></div>
  </div>
 </body>
</html>