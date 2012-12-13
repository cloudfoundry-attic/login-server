<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
<body>

<h1>Approvals</h1>

<p>Your active approvals:</p>

<form action="approvals" method="post">
    <ul>
        <%
            for (String client : ((String) request.getAttribute("clients")).split(",")) {
        %>
        <br /><li>Scopes approved/delegated for client: <b><%= client %></b></li><br />
        <%
            for (String scope : ((String) request.getAttribute(client)).split(",")) {
        %>
        <input type="checkbox" name="<%= String.format("%s.%s", client, scope) %>">&nbsp;&nbsp;<%= scope %><br />
        <%
                }
            }
        %>
    </ul>

    <p><input type="submit" value="Revoke"></p>
</form>

<p><a href="${logoutUrl}">Logout</a></p>

</body>
</html>