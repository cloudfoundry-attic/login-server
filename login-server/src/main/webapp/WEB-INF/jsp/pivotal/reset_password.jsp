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
    <article class="container">
        <p>Enter a new password below.</p>
        <form id="resetPasswordForm" action="<c:url value="/reset_password.do"/>" method="POST" novalidate>
            <c:if test="${not empty message}">
            <div class="flash">${message}</div>
            </c:if>
            <div>
                <input type="password" name="password" />
            </div>
            <div>
                <input type="password" name="password_confirmation" />
            </div>
            <input type="hidden" name="code" value="${code}"/>
            <button type="submit" class="button">Change Password</button>
        </form>
    </article>
</div>
</body>
</html>
