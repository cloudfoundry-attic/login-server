<%--

    Cloud Foundry 2012.02.03 Beta
    Copyright (c) [2013] GoPivotal, Inc. All Rights Reserved.

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
<c:url var="baseUrl" value="/resources" />
<c:choose>
	<c:when test="${pageContext.request.userPrincipal['class'].name =='org.springframework.security.providers.ExpiringUsernameAuthenticationToken'}">
		<c:url var="username" value="${fn:escapeXml(pageContext.request.userPrincipal.principal.value)}" />
	</c:when>
	<c:otherwise>
		<c:url var="username" value="${fn:escapeXml(pageContext.request.userPrincipal.name)}" />
	</c:otherwise>
</c:choose>

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
<title>Success | Cloud Foundry</title>
<meta charset='utf-8'>
<meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'>
<meta content='GoPivotal' name='author' />
<meta content='Copyright GoPivotal 2013. All Rights Reserved.'
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
</head>
<body id="micro">
	<div class="splash">
		<a href='${links.home}'><img
			alt="Cloud Foundry: The Industry's Open Platform As A Service"
			class="logo"
			src='${baseUrl}/images/logo-cloudfoundry.png'></img> </a>
		<a href='http://www.gopivotal.com' target='_blank'><img
			alt="Pivotal"
			id="pivotal-logo"
			src='${baseUrl}/images/logo-pivotal.png'></img> </a>
		<div style="float: right;">
			<ul class='super-nav'>
				<li><span>Welcome <a href="${rootUrl}profile"><strong>${username}</strong></a></span>
					/ <a href="${rootUrl}logout.do">Logout</a> &nbsp;</li>
			</ul>
		</div>
		<div class="splash-box">
			<div class="container">
				<h2>Success</h2>

				<p>Your account login is working and you have authenticated.</p>
				<p>
					You can... 
					<ul>
						<li><a href="${links.passwd}">Reset</a> your password</li>
						<li><a href="https://micro.cloudfoundry.com/eula">Manage</a> your Micro Cloud Foundry domain names</li>
						<li><a href="${rootUrl}profile">Proceed</a> to your account settings</li>
						<li><a href="http://support.cloudfoundry.com/home">Submit</a> a support ticket</li>
					</ul>
				</p>

				<c:if test="${error!=null}">
					<div class="error" title="${fn:escapeXml(error)}">
						<p>But there was an error.</p>
					</div>
				</c:if>

			</div>
		</div>
		<div class="footer">
			Copyright &copy;
			<fmt:formatDate value="<%=new java.util.Date()%>" pattern="yyyy" />
			GoPivotal, Inc. All rights reserved.
		</div>
	</div>
	<cf:if test="${not empty analytics}">
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
	</cf:if>
</body>
</html>
