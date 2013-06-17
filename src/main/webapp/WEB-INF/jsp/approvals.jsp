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

<c:url var="baseUrl" value="/resources" />
<c:url var="rootUrl" value="/" />
<c:url var="authorizeUrl" value="/oauth/authorize" />

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
<title>Account Settings | Cloud Foundry</title>
<meta charset='utf-8'>
<meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'>
<meta content='VMware' name='author' />
<meta content='Copyright VMware 2011. All Rights Reserved.'
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
<meta content='' name='Description' />
<meta content='' name='keywords' />
<script type="text/javascript" src="${baseUrl}/javascripts/jquery.js"></script>
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
	$(document).ready(function() {
		$(".app-approval-container1").mouseenter(function() {
			$(this).find('.delete-icon').show();
			$(this).find('.edit-icon').show();
		});
		$(".app-approval-container1").mouseleave(function() {
			$(this).find('.delete-icon').hide();
			$(this).find('.edit-icon').hide();
		});
		$('a#firstlink').click(function() {
			$(this).children().hide();
			$(this).next().css({
				display : "block"
			});
		});
		$('a#cancel').click(function() {
			$(this).parent().parent().parent().css({
				display : "none"
			});
			$(this).parent().parent().parent().prev().children().first().css({
				display : "block"
			});
		});
		$('.app-approval-container2').click(function() {
			$(this).parent().css({
				display : "none"
			});
			$(this).parent().prev().children().first().css({
				display : "block"
			});
		});
		$(".app-approval-container2 input[type=checkbox]").click(function(e) {
			e.stopPropagation();
		});
		$(".app-approval-container2 input[type=submit]").click(function(e) {
			e.stopPropagation();
		});

		$('[title]').tooltip({
			tooltipClass : "tooltip"
		});
	});
	function deleteApprovalsFor(client) {
		$(
				'<form action="approvals/delete" method="post"><input name="clientId" value="' + client + '"></input></form>')
				.submit();
	}
</script>
</head>
<body id="micro">
	<div class="approvals">
		<a href='${links.home}/'><img
			alt="Cloud Foundry: The Industry's Open Platform As A Service"
			class="logo" src='${baseUrl}/images/logo_cloud_foundry_by_pivotal.png'
			width='414' height='70'></img> </a>
		<div style="float: right;">
			<ul class='super-nav'>
				<li><span>Welcome <a href="${rootUrl}profile"><strong>${fn:escapeXml(pageContext.request.userPrincipal.name)}</strong></a></span>
					/ <a href="${rootUrl}logout.do">Logout</a> &nbsp;</li>
			</ul>
		</div>
		<div class="bg-content-approvals">
			<c:if test="${error!=null}">
				<div class="error" title="${fn:escapeXml(error)}">
					<div class="content-title-approvals">
						<h2>Sorry</h2>
					</div>
					<div class="content-inner-approvals">
						<p>There was an error. The request for authorization was
							invalid.</p>
					</div>
				</div>
			</c:if>

			<c:if test="${error==null}">

				<div class="content-title-approvals">
					<h2>Account Settings</h2>
				</div>

				<div class="content-inner-approvals">
					<p class="right">
						Looking for <a href="http://micro.cloudfoundry.com">Micro</a> or <a
							href="http://support.cloudfoundry.com">Support</a> ?
					</p>

					<p>
						<strong>Username:</strong>
						${fn:escapeXml(pageContext.request.userPrincipal.name)}
					</p>
					<p>
						<strong>Password:</strong> ********** <a href="${links.passwd}">Reset
							password</a>
					</p>

					<c:if test="${! empty approvals}">
						<hr>

						<p>
							<strong>Application Approvals</strong>
						</p>
						<p>These applications have been granted access to your
							CloudFoundry.com account.</p>

						<c:forEach items="${approvals}" var="client">
							<form id="revokeApprovalsForm" action="approvals" method="post">
								<input type="hidden" name="clientId" value="${client.key}" /> <a
									href="#" id="firstlink">
									<div id="firstdiv" style="display: block">
										<div class="app-approval-container1">
											<div class='row'>
												<input class="delete-icon right" title="Delete"
													type='submit' name="delete" value="" /> <span
													class="linklike">${client.key}</span>
												<div class="edit-icon right" title="Edit "></div>
											</div>
										</div>
									</div>
								</a>
								<div id="seconddiv" style="display: none">
									<div class="app-approval-container2">
										<input class="btn-secondary-medium right" name="delete"
											type='submit' value="Delete" /> <span class="linklike">${client.key}</span>
										<div id="approvals-list-container">
											<br>

											<c:forEach items="${client.value}" var="approval">
												<div class="approvals-list-div">
													<input type="checkbox" name="checkedScopes"
														value="${approval.clientId}-${approval.scope}"
														${approval.status eq 'APPROVED' ? 'checked=checked' : '' }>
													<spring:message code="scope.${approval.scope}" />
												</div>
											</c:forEach>

											<input class="btn-primary-medium right" name="update"
												type="submit" value="Update"> <a id="cancel"
												style="cursor: pointer"><input
												class="btn-secondary-medium right" type='button'
												value="Cancel" /></a>

										</div>
									</div>
								</div>
							</form>
						</c:forEach>

					</c:if>
				</div>

			</c:if>

		</div>

		<div class="footer">
			&copy;
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