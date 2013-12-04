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
<html class='no-js' dir='ltr' lang='en'>
<head>
<title>Access Confirmation | Cloud Foundry</title>
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
<link href='${baseUrl}/stylesheets/permissions.css' media='screen'
    rel='stylesheet' type='text/css' />
<link href='${baseUrl}/stylesheets/access_confirmation.css' media='screen'
    rel='stylesheet' type='text/css' />
<meta content='' name='Description' />
<meta content='' name='keywords' />
<script type="text/javascript" src="${baseUrl}/javascripts/jquery.js"></script>
<script src="//use.typekit.net/zwc8anl.js"></script>
<script>
  try { Typekit.load(); } catch (e) { }
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
    function toggle(source) {
        checkboxes = document.getElementsByClassName('requests');
        for ( var i in checkboxes)
            checkboxes[i].checked = source.checked;
    }
    $('.allrequests').live(
            'click',
            function() {
                $(this).children().toggleClass('inactive');
                if ($('.individualrequests').children().hasClass('inactive')) {
                    $('.individualrequests').children().removeClass('inactive')
                            .addClass('approvals-list-div');
                } else if ($('.individualrequests').children().hasClass(
                        'approvals-list-div')) {
                    $('.individualrequests').children().removeClass(
                            'approvals-list-div').addClass('inactive');
                }
            });
</script>
</head>
<body>
    <div class="container container-large">
        <div class="header">
            <div class="image-logo"></div>
            <div class="logo">PIVOTAL</div>
            <div class="header-link">
                <a href="${rootUrl}logout.do" class="h4">Sign out</a>
            </div>
        </div>

        <div class="main-content access-confirmation">
            <h1>Application Authorization</h1>

            <div class="app-info">
                <h2>${client_id}</h2>
                <a href='${redirect_uri}' class="h2">${redirect_uri}</a>
            </div>

            <form id="confirmationForm" name="confirmationForm" action="${authorizeUrl}" method="POST">
                <div class="permissions-container">

                    <p>
                        ${client_id} has requested permission to access your
                        Pivotal account. If you do not recognize this
                        application or its URL (<a href="${redirect_uri}">${redirect_uri}</a>),
                        you should click deny below. The application will not see
                        your password.
                    </p>
                    <c:set var="count" value="0" />

                    <div class="permissions">
                        <c:if
                            test="${(! empty undecided_scopes) && (! empty approved_scopes || ! empty denied_scopes)}">
                            <p>
                                <strong>New Requests</strong>
                            </p>
                        </c:if>
                        <c:if test="${(! empty undecided_scopes)}">
                            <c:forEach items="${undecided_scopes}" var="scope">
                                <div class="approvals-list-div">
                                    <input type="checkbox" checked="checked"
                                        name="scope.${count}" id="checkbox.scope.${count}" value="${scope['code']}">
                                    <label for="checkbox.scope.${count}"></label>
                                    <spring:message code="${scope['code']}"
                                        text="${scope['text']}" />
                                </div>
                                <c:set var="count" value="${count + 1}" />
                            </c:forEach>
                        </c:if>
                        <c:if
                            test="${(! empty approved_scopes) || (! empty denied_scopes)}">
                            <p>
                                <strong>Existing Permissions</strong>
                            </p>
                        </c:if>
                        <c:if
                            test="${(approved_scopes != null) && (! empty approved_scopes)}">
                            <c:forEach items="${approved_scopes}" var="scope">
                                <div class="approvals-list-div">
                                    <input type="checkbox" checked="checked"
                                        name="scope.${count}" id="checkbox.scope.${count}" value="${scope['code']}">
                                    <label for="checkbox.scope.${count}"></label>
                                    <spring:message code="${scope['code']}"
                                        text="${scope['text']}" />
                                </div>
                                <c:set var="count" value="${count + 1}" />
                            </c:forEach>
                        </c:if>
                        <c:if
                            test="${(denied_scopes != null) && (! empty denied_scopes)}">
                            <c:forEach items="${denied_scopes}" var="scope">
                                <div class="approvals-list-div">
                                    <input type="checkbox" name="scope.${count}"
                                        value="${scope['code']}">
                                    <label for="checkbox.scope.${count}"></label>
                                    <spring:message code="${scope['code']}"
                                        text="${scope['text']}" />
                                </div>
                                <c:set var="count" value="${count + 1}" />
                            </c:forEach>
                        </c:if>
                    </div>

                    <p>
                        You can change your approval of permissions or revoke
                        access for this application at any time from account settings.
                        By approving access, you agree to ${client_id}'s terms of
                        service and privacy policy.
                    </p>
                </div>
                <div class="actions-authorize">
                    <input name="${options.confirm.key}" value="${options.confirm.value}" type="hidden" />
                    <button class="btn-primary btn-small" type="submit">Authorize</button>
                </div>
            </form>

            <form id="denialForm" name="denialForm" action="${authorizeUrl}" method="POST">
                <div class="actions-deny">
                    <input name="${options.deny.key}" value="${options.deny.value}" type="hidden" />
                    <button class="btn-link" type="submit">Deny</button>
                </div>
            </form>
            <div class="clearfix"></div>
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
