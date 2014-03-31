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
        <p>Enter your email to reset your password.</p>
        <form id="forgotPasswordForm" action="<c:url value="/forgot_password.do"/>" method="POST" novalidate>
            <div>
                <c:if test="${not empty success}">
                    <div class="flash">An email has been sent with password reset instructions.</div>
                </c:if>
                <input type="email" name="email" />
            </div>
            <button type="submit" class="button">Reset Password</button>
        </form>
    </article>
</div>
</body>
</html>
