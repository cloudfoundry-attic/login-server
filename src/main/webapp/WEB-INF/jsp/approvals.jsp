<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
<body>

<h1>Approvals</h1>

<p>Your active approvals:</p>

<form id="revokeApprovalsForm" action="approvals" method="post">
        <c:forEach items="${approvals}" var="approval" varStatus="status">
            <input type="checkbox" name="approvalToRevoke${status.count}" value='{"userName":"${approval.userName}", "clientId":"${approval.clientId}", "scope":"${approval.scope}", "expiresAt": ${approval.expiresAt.time}}'>
            &nbsp;&nbsp;client: ${approval.clientId}, scope: ${approval.scope} <br />
        </c:forEach>

        <p><input type="submit" value="Revoke"></p>
</form>

<p><a href="${logoutUrl}">Logout</a></p>

</body>
</html>