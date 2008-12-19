<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ page import="org.guanxi.xal.idp.IdpDocument" %>
<%@ page import="org.guanxi.common.definitions.Guanxi" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  IdpDocument.Idp idpConfig = (IdpDocument.Idp)application.getAttribute(Guanxi.CONTEXT_ATTR_IDP_CONFIG);
%>
<html>
  <head>
    <title><fmt:message key="register.sp.page.title"/></title>
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
        background-image: url( ../../../guanxi_idp/images/watermark.gif );
      }
      -->
    </style>
  </head>
 <body>

  <div style="border:1px solid black; width:30%; background-image:url(../../../guanxi_idp/images/formback.gif); background-repeat:repeat-x repeat-y; margin: 0 auto;">
  	<div style="padding:20px; margin: 0 auto;">

     <fmt:message key="register.sp.label"/>:<br /><br />
     <form:form method="post" commandName="registerSP">
       <fmt:message key="register.sp.label.providerid"/>:<br />
       <form:input path="providerId" size="50"/>
       <br /><form:errors path="providerId" /><br /><br />

       <fmt:message key="register.sp.label.identity"/>:<br />
       <select name="identity">
         <% for (int c=0; c < idpConfig.getIdentityArray().length; c++) {
           String name = idpConfig.getIdentityArray(c).getName();
         %>
         <option value="<%= name %>"><%= name %></option>
         <% } %>
       </select>
       <br /><form:errors path="identity" />
       <br /><br />

       <fmt:message key="register.sp.label.creds"/>:<br />
       <select name="creds">
         <% for (int c=0; c < idpConfig.getCredsArray().length; c++) {
           String name = idpConfig.getCredsArray(c).getName();
         %>
         <option value="<%= name %>"><%= name %></option>
         <% } %>
       </select>
       <br /><form:errors path="creds" />

       <br /><br />

       <input type="submit" value="<fmt:message key="register.sp.submit.button.register.sp"/>" />
       <input type="hidden" name="mode" value="sp" />
     </form:form>

   </div>
  </div>
 
 </body>
</html>