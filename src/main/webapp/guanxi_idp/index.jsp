<%@ page import="java.util.Locale"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="java.io.File"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  ResourceBundle msg = ResourceBundle.getBundle("messages.idp_index", new Locale(request.getHeader("Accept-Language")));
  ResourceBundle siteMsg = ResourceBundle.getBundle("messages.site", new Locale(request.getHeader("Accept-Language")));
%>
<html>
  <head>
    <title><%= msg.getString("ID_PAGE_TITLE")%></title>
      <style type="text/css">
        <!--
        body {
          background-color: #FFFFFF;
          margin-left: 20px;
          margin-top: 20px;
          margin-right: 20px;
          margin-bottom: 20px;
          font-family:Verdana, Arial, Helvetica, sans-serif;
          background-image: url(images/watermark.gif);
        }
        -->
      </style>
  </head>
  <body>
  <%
    File keystoreFile = new File(getServletConfig().getServletContext().getRealPath("/WEB-INF/guanxi_idp/keystore/guanxi_idp.jks"));
    boolean isSetup = keystoreFile.exists();
  %>
  <div style="border:1px solid black; width:50%; height:20%; background-image:url(images/formback.gif); background-repeat:repeat-x repeat-y; margin: 0 auto;">
    <div style="padding:20px; margin: 0 auto;">
        <%= msg.getString("ID_IDP_MESSAGE")%>
    </div>
   </div>

  <br><br>

  <!-- Display the correct message depending on setup status -->
  <% if (isSetup) { %>
  <div style="border:1px solid black; width:50%; height:20%; background-image:url(images/formback.gif); background-repeat:repeat-x repeat-y; margin: 0 auto;">
    <div style="padding:20px; margin: 0 auto;">
      <%= msg.getString("ID_SETUP_DONE_MESSAGE")%>
    </div>
   </div>
    <% }
      else { %>
  <div style="border:1px solid black; width:50%; height:20%; background-image:url(images/formback.gif); background-repeat:repeat-x repeat-y; margin: 0 auto;">
    <div style="padding:20px; margin: 0 auto;">
      <%= msg.getString("ID_NOT_SETUP_MESSAGE")%><br><br>
      <a href="setup.setupIdP"><%= msg.getString("ID_SETUP_IDP")%></a><br><br>
    </div>
   </div>
  <% } %>

  <br><br>

  <div style="border:1px solid black; width:50%; height:20%; background-image:url(images/formback.gif); background-repeat:repeat-x repeat-y; margin: 0 auto;">
    <div style="padding:20px; margin: 0 auto;">
      <%= msg.getString("ID_DOC_MESSAGE")%><br><br>
      <a href="http://www.guanxi.uhi.ac.uk/index.php/Identity_Provider"><%= msg.getString("ID_DOC_LINK")%></a><br><br>
    </div>
   </div>

  <br><br>

   <div style="width:50%; margin: 0 auto;">
     <div align="left"><strong>Guanxi@<%= siteMsg.getString("ID_INSTITUTION")%></strong></div>
   </div>
  </body>
</html>
