<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head><title>Guanxi Setup Report</title>
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
    <%= request.getAttribute("setupStatus") %>
  </body>
</html>