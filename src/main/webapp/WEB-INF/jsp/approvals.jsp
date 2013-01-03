<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
<body>

<h1>Approvals</h1>

<form id="revokeApprovalsForm" action="approvals" method="post">
    <p>Your active approvals:</p>
    <c:forEach items="${approvals}" var="approval" varStatus="status">
            <input type="checkbox" name="toRevoke${status.count}" value='{"userName":"${approval.userName}", "clientId":"${approval.clientId}", "status":"${approval.status}", "scope":"${approval.scope}", "expiresAt": ${approval.expiresAt.time}}'>
            &nbsp;&nbsp;client: ${approval.clientId}, scope: ${approval.scope} <br />
        </c:forEach>

    <p>Your active denials:</p>
    <c:forEach items="${denials}" var="denial" varStatus="status">
        <input type="checkbox" name="toRevoke${status.count}" value='{"userName":"${denial.userName}", "clientId":"${denial.clientId}", "status":"${denial.status}", "scope":"${denial.scope}", "expiresAt": ${denial.expiresAt.time}}'>
        &nbsp;&nbsp;client: ${denial.clientId}, scope: ${denial.scope} <br />
    </c:forEach>

    <p><input type="submit" value="Revoke"></p>
</form>

<p><a href="${logoutUrl}">Logout</a></p>

</body>
</html>