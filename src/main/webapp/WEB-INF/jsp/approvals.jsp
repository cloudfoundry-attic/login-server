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
<title>Access Confirmation | Cloud Foundry</title>
<meta charset='utf-8'>
<meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'>
<meta content='VMware' name='author' />
<meta content='Copyright VMware 2011. All Rights Reserved.'
	name='copyright' />
<link href='${baseUrl}/favicon.ico' rel='shortcut icon' />
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
  $(document).ready(function(){
    $("a#vendor").click(function(){
      $(this).next().slideToggle();
    });
  });
</script>
</head>
<body id="micro">
	<div class="approvals">
		<a href='${links.home}'><img
			alt="Cloud Foundry: The Industry's Open Platform As A Service"
			class="logo-approvals" src='${baseUrl}/images/logo_header_cloudfoundry.png'
			width='373' height='70'></img> </a>
		<div style="float: right;">
			<ul class='super-nav'>
				<li><span>Welcome <a href="/approvals"><strong>${fn:escapeXml(pageContext.request.userPrincipal.name)}</strong></a></span>
					/ <c:url value="/logout.do" var="url" /> <a
					href="${fn:escapeXml(url)}">Logout</a> &nbsp;</li>
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
          <p class="right">Looking for <a href="http://micro.cloudfoundry.com" target="_blank">Micro</a> or <a href="http://support.cloudfoundry.com" target="_blank">Support</a> ?</p>

          <p><strong>Username:</strong> ${fn:escapeXml(pageContext.request.userPrincipal.name)}</p>
          <p><strong>Password:</strong> ********** <a href="http://my.cloudfoundry.com/passwd" target="_blank">Reset password</a></p>

          <hr>

          <p><strong>Application Approvals</strong></p>
			    <p>These applications have been granted access to your CloudFoundry.com account. Click them to see what you've approved.</p>

          <form id="revokeApprovalsForm" action="approvals" method="post">
              <c:forEach items="${approvals}" var="client">
                  <div class="app-approval-container">
                    <a id="vendor"><div class="app-approval-title">${client.key}</div></a>
                      <div id="approvals-list-container">
                      <br>
                      <c:forEach items="${client.value}" var="approval">
                        <div class="approvals-list-div">
                          <input type="checkbox" name="checkedScopes" value="${approval.clientId}-${approval.scope}" ${approval.status eq 'APPROVED' ? 'checked=checked' : '' }>
                                    <spring:message code="scope.${approval.scope}"/>
                        </div>
                      </c:forEach>
                      </div>
                  </div>
              </c:forEach>

            <p>
              <input class="btn-primary-medium right" type="submit" value="Update">
            </p>
          </form>
        </div>

			</c:if>

		</div>

		<div class="footer">
			Copyright &copy;
			<fmt:formatDate value="<%=new java.util.Date()%>" pattern="yyyy" />
			VMware, Inc. All rights reserved.
		</div>
	</div>
	<script>
		var _gaq = _gaq || [];
		_gaq.push([ '_setAccount', 'UA-22181585-1' ]);
		_gaq.push([ '_trackPageview' ]);
		(function() {
			var ga = document.createElement('script');
			ga.type = 'text/javascript';
			ga.async = true;
			ga.src = ('https:' == document.location.protocol ? 'https://ssl'
					: 'http://www')
					+ '.google-analytics.com/ga.js';
			var s = document.getElementsByTagName('script')[0];
			s.parentNode.insertBefore(ga, s);
		})();
	</script>
	<script type="text/javascript"
		src="//www.vmware.com/files/templates/inc/s_code_vmw.js"></script>
</body>
</html>