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
<c:url var="authorizeUrl" value="/oauth/authorize" />

<!DOCTYPE html>
<html class='no-js' dir='ltr' lang='en'>
<head>
<title>Account Settings | Pivotal</title>
<meta charset='utf-8'>
<meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'>
<meta content='Pivotal Software, Inc' name='author' />
<meta
 content='Copyright 2013 Pivotal Software Inc. All Rights Reserved.'
 name='copyright' />
<link href='${baseUrl}/images/favicon.ico' rel='shortcut icon' />
<meta content='all' name='robots' />
<link href='${baseUrl}/stylesheets/print.css' media='print'
 rel='stylesheet' type='text/css' />
<link href='${baseUrl}/stylesheets/style.css' media='screen'
 rel='stylesheet' type='text/css' />
<link href='${baseUrl}/stylesheets/font-awesome.css' media='screen'
 rel='stylesheet' type='text/css' />
<link href='${baseUrl}/stylesheets/permissions.css' media='screen'
 rel='stylesheet' type='text/css' />
<link href='${baseUrl}/stylesheets/approvals.css' media='screen'
 rel='stylesheet' type='text/css' />
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

    function deleteApprovalsFor(client) {
        $(
                '<form action="approvals/delete" method="post"><input name="clientId" value="' + client + '"></input></form>')
                .submit();
    }

    $(function() {
        var showPermissions = function(e) {
            var $approvedAppDiv = $(e.target).parents('.approved-app');
            $approvedAppDiv.find('.permissions-container').toggleClass('hidden');
            $approvedAppDiv.find('.revoke-access-action').toggleClass('hidden');

            $approvedAppActions = $approvedAppDiv.find('.approved-app-actions');

            if($approvedAppActions.hasClass('hidden')) {
                setTimeout(function() {
                    $approvedAppActions.removeClass('hidden');
                }, 200);

            } else {
                $approvedAppActions.addClass('hidden');
            }

            return false;
        }

        $('i.icon-edit-sign').click(showPermissions);
        $('.actions-cancel button').click(showPermissions);
    });
</script>
<script src="//use.typekit.net/zwc8anl.js"></script>
<script>
  try { Typekit.load(); } catch (e) { }
</script>
</head>
<body>
 <div class="container container-large">
  <div class="header">
   <a style="text-decoration: none;" href='${rootUrl}'><div
     class="image-logo"></div></a>
   <div class="logo">
    <a style="text-decoration: none;" href='${rootUrl}'>PIVOTAL</a>
   </div>
   <div class="header-link">
    <a href="${rootUrl}logout.do" class="h4">Sign out</a>
   </div>
  </div>

  <div class="main-content approvals-container">
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
    <h1>Account Settings</h1>

    <div class="account-info">
     <div class="h2">Username:
      ${fn:escapeXml(pageContext.request.userPrincipal.name)}</div>
     <div class="h2">
      Password: ********** <a href="<c:url value="/change_password"/>">Change password</a>
     </div>
    </div>

    <c:if test="${! empty approvals}">
     <c:set var="count" value="0" />

     <div class="application-approvals">
      <h2>Applications Approval</h2>
      <p>These applications have been granted access to your
       account.</p>

      <c:forEach items="${approvals}" var="client">
       <form action="profile" method="post">
        <input type="hidden" name="clientId" value="${client.key}" />

        <div class="approved-app">
         <span class="h2 app-name">${client.key}</span>
         <div class="approved-app-actions">
          <i class="icon-edit-sign"></i>
          <button class="btn-link" title="Delete" type='submit'
           name="delete">
           <i class="icon-remove-sign"></i>
          </button>
         </div>

         <button class="revoke-access-action btn-link hidden"
          title="Delete" type='submit' name="delete">Revoke
          access</button>

         <div class="permissions-container hidden">
          <div class="permissions">
           <c:forEach items="${client.value}" var="approval">
            <div class="approvals-list-div">
             <input type="checkbox" name="checkedScopes"
              id="checkbox.scope.${count}"
              value="${approval.clientId}-${approval.scope}"
              ${approval.status eq 'APPROVED' ? 'checked=checked' : '' }>
             <label for="checkbox.scope.${count}"></label>
             <spring:message code="scope.${approval.scope}" text="scope.${approval.scope}" />
            </div>

            <c:set var="count" value="${count + 1}" />
           </c:forEach>
          </div>

          <div class="actions-update">
           <button class="btn-primary btn-small" name="update"
            type="submit">Update</button>
          </div>

          <div class="actions-cancel">
           <button class="btn-link" type="submit">Cancel</button>
          </div>

          <div class="clearfix"></div>
         </div>
        </div>
       </form>
      </c:forEach>
     </div>
    </c:if>
   </c:if>
  </div>
 </div>
 <div class='footer'
  title="Version: ${app.version}, Commit: ${commit_id}, Timestamp: ${timestamp}, UAA: ${links.uaa}">
  <div class='copyright'>
   &copy;
   <fmt:formatDate value="<%=new java.util.Date()%>" pattern="yyyy" />
   Pivotal Software, Inc. - All rights reserved
  </div>
  <div class='powered-by'>
   Powered by
   <div class='logo'>Pivotal</div>
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
