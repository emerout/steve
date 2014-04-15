<%@ include file="/WEB-INF/jsp/00-header.jsp" %>
<script type="text/javascript">
$(document).ready(function() {
<%@ include file="/WEB-INF/jsp/00-js-snippets/getReservationIds.js" %>
});
</script>
<div class="left-menu">
<ul>
	<li><a href="${servletPath}/ChangeAvailability">Change Availability</a></li>
	<li><a href="${servletPath}/ChangeConfiguration">Change Configuration</a></li>
	<li><a href="${servletPath}/ClearCache">Clear Cache</a></li>
	<li><a href="${servletPath}/GetDiagnostics">Get Diagnostics</a></li>
	<li><a href="${servletPath}/RemoteStartTransaction">Remote Start Transaction</a></li>
	<li><a href="${servletPath}/RemoteStopTransaction">Remote Stop Transaction</a></li>
	<li><a href="${servletPath}/Reset">Reset</a></li>
	<li><a href="${servletPath}/UnlockConnector">Unlock Connector</a></li>
	<li><a href="${servletPath}/UpdateFirmware">Update Firmware</a></li>
	<hr>
	<li><a href="${servletPath}/ReserveNow">Reserve Now</a></li>
	<li><a class="highlight" href="${servletPath}/CancelReservation">Cancel Reservation</a></li>
	<li><a href="${servletPath}/DataTransfer">Data Transfer</a></li>
	<li><a href="${servletPath}/GetConfiguration">Get Configuration</a></li>
	<li><a href="${servletPath}/GetLocalListVersion">Get Local List Version</a></li>
	<li><a href="${servletPath}/SendLocalList">Send Local List</a></li>
</ul>
</div>
<div class="op15-content">
<form method="POST" action="${servletPath}/CancelReservation">
<%@ include file="00-cp-single.jsp" %>
<section><span>Parameters</span></section>
<table class="userInput">
<tr><td>ID of the Existing Reservation:</td><td><select name="reservationId" id="reservationId" required disabled></select></td></tr>
</table>
<div class="submit-button"><input type="submit" value="Perform"></div>
</form>
</div>
<%@ include file="/WEB-INF/jsp/00-footer.jsp" %>