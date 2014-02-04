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
<c:url var="baseUrl" value="/resources/oss" />

<!DOCTYPE html>
<!--[if IE]>  <![endif]-->
<!--[if lt IE 7 ]> <html lang="en" dir="ltr" class="no-js old_ie ie6"> <![endif]-->
<!--[if IE 7 ]> <html lang="en" dir="ltr" class="no-js old_ie ie7"> <![endif]-->
<!--[if IE 8 ]> <html lang="en" dir="ltr" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]> <html lang="en" dir="ltr" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]> ><! <![endif]-->
<html class='no-js' dir='ltr' lang='en'>
<!-- <![endif] -->
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
<link href='${baseUrl}/stylesheets/login.css' media='screen'
	rel='stylesheet' type='text/css' />
<!--[if IE 9 ]> <link href="${baseUrl}/stylesheets/ie9.css" media="screen" rel="stylesheet" type="text/css" /> <![endif]-->
<!--[if lt IE 9 ]> <link href="${baseUrl}/stylesheets/ie.css" media="screen" rel="stylesheet" type="text/css" /> <![endif]-->
<!--[if lt IE 8 ]> <link href="${baseUrl}/stylesheets/ie7.css" media="screen" rel="stylesheet" type="text/css" /> <![endif]-->
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
<script type="text/javascript"
	src="${baseUrl}/javascripts/placeholder.js"></script>
<script type="text/javascript">
	$(document).ready(function() {
		$('form:first *:input[type!=hidden]:first').focus();
    Placeholders.init({
      live: true, //Apply to future and modified elements too
      hideOnFocus: true //Hide the placeholder when the element receives focus
    });
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
<c:if test="${autoRedirect}">
   <script type="text/javascript">
	   setTimeout(function () {
	      window.location.href = "saml/discovery?returnIDParam=idp&entityID=${entityID}";
	   }, 1000);
   </script>
</c:if>
</head>
<c:if test="!${autoRedirect}">
	<body id="micro">
		<div class="splash">
			<a href='${links.home}/'><img
				alt="Cloud Foundry: The Industry's Open Platform As A Service"
				class="logo" src='${baseUrl}/images/logo-cloudfoundry.png'></img> </a>
			<div class="splash-box">
				<div class="container">
					<form id="loginForm" name="loginForm"
						action="<c:url value="/login.do"/>" method="POST" novalidate>
						<div>
							<c:if test="${not empty param.error}">
								<div class="alert base alert-error alert-inline">
									<div>Unable to verify, please try again:</div>
								</div>
							</c:if>
							<p class="intro-text">Log in to Pivotal CF:</p>
							<c:forEach items="${prompts}" var="prompt">
								<c:if test="${'passcode' != prompt.key}">
					            	<spring:message code="prompt.${prompt.key}"
					                	text="${prompt.value[1]}" var="text"/>
					              	<input id='${prompt.key}' type='${prompt.value[0]}' ${prompt.value[0]=='password'?'autocomplete="off"':''}
					                	name='${prompt.key}' placeholder='${text}' />
				                </c:if>
							</c:forEach>
						</div>
						<button type="submit" class="button-orange">Log in</button>
						<span class="button-alt"><a href="${links.passwd}">Forgot password &raquo;</a><a href="${links.register}">Create an account &raquo;</a></span>
					</form>
	                <c:if test="${saml}">
					  <p><a href="saml/discovery?returnIDParam=idp&entityID=${entityID}">Sign in with your organization's credentials.</a></p>
	                </c:if>
				</div>
			</div>
	        <div class="footer"
	            title="Version: ${app.version}, Commit: ${commit_id}, Timestamp: ${timestamp}, UAA: ${links.uaa}">
	            &copy;
	            <fmt:formatDate value="<%=new java.util.Date()%>" pattern="yyyy" />
	            Pivotal Software, Inc. All rights reserved.
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
