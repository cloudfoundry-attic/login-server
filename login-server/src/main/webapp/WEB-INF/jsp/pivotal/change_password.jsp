<%--

    Cloud Foundry
    Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.

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

<!DOCTYPE html>
<html lang='en'>
<head>
    <title>Pivotal</title>
    <meta charset='utf-8'>
</head>
<body id="micro">
<div class="content">
    <header>
        <div class="header-link">
            <a href="${rootUrl}logout.do" class="h4">Sign out</a>
        </div>
    </header>
    <article class="container">
        <p>Change your password below.</p>
        <form id="changePasswordForm" action="<c:url value="/change_password.do"/>" method="POST" novalidate>
            <c:if test="${not empty message}">
                <div class="flash">${message}</div>
            </c:if>
            <div>
                <label for="currentPassword">Current password</label>: <input type="password" name="current_password" id="currentPassword" />
            </div>
            <div>
                <label for="newPassword">New password</label>: <input type="password" name="new_password" id="newPassword" />
            </div>
            <div>
                <label for="confirmPassword">Confirm password</label>: <input type="password" name="confirm_password" id="confirmPassword" />
            </div>
            <button type="submit" class="button">Change password</button>
        </form>
    </article>
</div>
</body>
</html>
