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

<c:url var="rootUrl" value="/" />
<c:url var="baseUrl" value="/resources/pivotal" />

<!DOCTYPE html>
<html class='no-js' dir='ltr' lang='en'>
<head>
<title>Cloud Foundry</title>
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
<link href='${baseUrl}/stylesheets/login.css' media='screen'
    rel='stylesheet' type='text/css' />
<style media='screen' type='text/css'>
.js-hide {
    display: none;
}

.js-show {
    display: block;
}

.fouc-fix {
    display: none;
}
</style>
<meta content='' name='Description' />
<meta content='' name='keywords' />

<script type="text/javascript" src="${baseUrl}/javascripts/jquery.js"></script>
<script type="text/javascript">
    $(document).ready(function() {
        $('form:first *:input[type!=hidden]:first').focus();
    });
</script>
<script type="text/javascript">
    (function() {
        // force ssl if cf.com
        var loc = window.location;
        if (loc.hostname.indexOf('cloudfoundry.com') >= 0
                && loc.protocol == "http:") {
            window.location = "https://" + loc.host + loc.pathname + loc.search
                    + loc.hash;
        }
    })();
</script>
<script src="//use.typekit.net/zwc8anl.js"></script>
<script>
  try { Typekit.load(); } catch (e) { }
</script>
<c:if test="${autoRedirect}">
   <script type="text/javascript">
	   setTimeout(function () {
	      window.location.href = "saml/discovery?returnIDParam=idp&entityID=${entityID}";
	   }, 1000);
   </script>
</c:if>
</head>
<c:if test="!${autoRedirect}">
	<body>
	    <div class="container">
	        <div class="header">
	            <a style="text-decoration: none;" href='${rootUrl}'><div class="image-logo"></div></a>
	            <div class="logo"><a style="text-decoration: none;" href='${rootUrl}'>PIVOTAL</a></div>
	            <div class="header-link">
	                <a href="${links.passwd}" class="h4">Forgot Password</a>
	            </div>
	        </div>
	
	        <div class="main-content">
	            <form id="loginForm" name="loginForm"
	                                action="<c:url value="/login.do"/>" method="POST" novalidate>
	                <div class="error-messages<c:if test="${empty param.error}"> hidden</c:if>">
	                    Unable to verify, please try again:
	                </div>
	
	                <c:forEach items="${prompts}" var="prompt">
	                  <c:if test="${'passcode' != prompt.key}">
	                    <div class="fields-wrapper">
	                      <spring:message code="prompt.${prompt.key}"
	                        text="${prompt.value[1]}" var="text"/>
	                      <input id='${prompt.key}' type='${prompt.value[0]}' ${prompt.value[0]=='password'?'autocomplete="off"':''}
	                        name='${prompt.key}' placeholder='${text}' />
	                    </div>
	                  </c:if>
	                </c:forEach>
	
	                <div class="fields-wrapper">
	                    <button type="submit" class="btn-primary">Sign in</button>
	                </div>
	            </form>
	            
	            <div class="fields-wrapper">
	            <c:if test="${saml}">
	               <p><a href="saml/discovery?returnIDParam=idp&entityID=${entityID}">Sign in with your organization's credentials.</a></p>
	            </c:if>
	            </div>
	            
	        </div>
	
	        <div class="not-a-member">
	            <div class="h1">Not a member yet?</div>
	            <p class="h4">
	                Join <a href="${links.registerNetwork}">Pivotal Network</a> for enterprise,<br />
	                or <a href="${links.register}">Pivotal Web Services</a> for hosted solutions.
	            </p>
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
	
	    <%-- Clear out session scoped attributes, don't leak info --%>
	    <c:if
	        test="${not empty sessionScope['SPRING_SECURITY_LAST_EXCEPTION']}">
	        <c:set scope="session" var="SPRING_SECURITY_LAST_EXCEPTION"
	            value="${null}" />
	    </c:if>
	    <c:if test="${not empty sessionScope['SPRING_SECURITY_LAST_USERNAME']}">
	        <c:set scope="session" var="SPRING_SECURITY_LAST_USERNAME"
	            value="${null}" />
	    </c:if>
	
	    <!--
	                                Start of DoubleClick Floodlight Tag: Please do not remove
	                                Activity name of this tag: Micro Cloud Foundry - Landing Page Arrival
	                                URL of the webpage where the tag is expected to be placed: https://www.cloudfoundry.com/micro
	                                This tag must be placed between the <body> and </body> tags, as close as possible to the opening tag.
	                                Creation Date: 08/18/2011
	                                -->
	    <script type="text/javascript">
	        var axel = Math.random() + "";
	        var a = axel * 10000000000000;
	        document
	                .write('<iframe src="https://fls.doubleclick.net/activityi;src=2645750;type=cloud806;cat=micro467;ord='
	                        + a
	                        + '?" width="1" height="1" frameborder="0" style="display:none"></iframe>');
	    </script>
	    <noscript>
	        <iframe
	            src="https://fls.doubleclick.net/activityi;src=2645750;type=cloud806;cat=micro467;ord=1?"
	            width="1" height="1" frameborder="0" style="display: none"></iframe>
	    </noscript>
	    <!-- End of DoubleClick Floodlight Tag: Please do not remove -->
	
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
</c:if>
</html>
