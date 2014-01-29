<%--

    Cloud Foundry 2012.02.03 Beta
    Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.

    This product is licensed to you under the Apache License, Version 2.0 (the "License").
    You may not use this product except in compliance with the License.

    This product includes a number of subcomponents with
    separate copyright notices and license terms. Your use of these
    subcomponents is subject to the terms and conditions of the
    subcomponent's license, as noted in the LICENSE file.

--%>
<%@ page session="false"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<c:url var="baseUrl" value="/resources/pivotal" />
<c:url var="rootUrl" value="/" />

<html class='no-js' dir='ltr' lang='en'>
<head>
<title>Success | Cloud Foundry</title>
<meta charset='utf-8'>
<meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'>
<meta content='Pivotal Software, Inc' name='author' />
<meta content='Copyright 2013 Pivotal Software Inc. All Rights Reserved.' 
    name='copyright' />
<link href='${rootUrl}favicon.ico' rel='shortcut icon' />
<meta content='all' name='robots' />
<link href='${baseUrl}/stylesheets/print.css' media='print'
    rel='stylesheet' type='text/css' />
<link href='${baseUrl}/stylesheets/style.css' media='screen'
    rel='stylesheet' type='text/css' />
<link href='${baseUrl}/stylesheets/error.css' media='screen'
    rel='stylesheet' type='text/css' />

<meta content='' name='Description' />
<meta content='' name='keywords' />
<style type='text/css'>
img.gsc-branding-img,img.gsc-branding-img-noclear,img.gcsc-branding-img,img.gcsc-branding-img-noclear
    {
    display: none;
}

.gs-result .gs-title,.gs-result .gs-title * {
    color: #0094d4;
}
</style>
<script type="text/javascript" src="${baseUrl}/javascripts/jquery.js"></script>
<script src="//use.typekit.net/zwc8anl.js"></script>
<script>
  try { Typekit.load(); } catch (e) { }
</script>
</head>
<body>
    <div class="container">
        <div class="header">
            <a style="text-decoration: none;" href='${rootUrl}'><div class="image-logo"></div></a>
            <div class="logo"><a style="text-decoration: none;" href='${rootUrl}'>PIVOTAL</a></div>
        </div>

        <div class="main-content error-main-content">
            <h1>Sorry!</h1>
            <div class="h2">There was a problem. Please try to <a href="${links.login}">login</a> again.</div>
        </div>
    </div>

    <div class='footer' title="Version: ${app.version}, Commit: ${commit_id}, Timestamp: ${timestamp}, UAA: ${links.uaa}">
        <div class='copyright'>
            &copy;
            <fmt:formatDate value="<%=new java.util.Date()%>" pattern="yyyy" />
            Pivotal Software, Inc. - All rights reserved
        </div>
        <div class='powered-by'>
            Powered by
            <div class='logo'>
                Pivotal
            </div>
        </div>
    </div>

    <c:if test="${not empty analytics}">
        <script>
            (function(i, s, o, g, r, a, m) {
                i['GoogleAnalyticsObject'] = r;
                i[r] = i[r] || function() {
                    (i[r].q = i[r].q || []).push(arguments)
                }, i[r].l = 1 * new Date();
                a = s.createElement(o), m = s.getElementsByTagName(o)[0];
                a.async = 1;
                a.src = g;
                m.parentNode.insertBefore(a, m)
            })(window, document, 'script',
                    '//www.google-analytics.com/analytics.js', 'ga');

            ga('create', '${analytics.code}', '${analytics.domain}');
            ga('send', 'pageview');
        </script>
    </c:if>
</body>
</html>
