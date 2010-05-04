<%--
 Copyright 2007-2009 The Kuali Foundation
 
 Licensed under the Educational Community License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.opensource.org/licenses/ecl2.php
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
--%>
<%@ include file="/jsp/sys/kfsTldHeader.jsp"%>

<%@ attribute name="editingMode" required="false" description="used to decide if items may be edited" type="java.util.Map"%>
<%@ attribute name="hasSource" required="true" %>
<%@ attribute name="hasTarget" required="true" %>
<c:set var="readOnly" value="${!KualiForm.documentActions[Constants.KUALI_ACTION_CAN_EDIT]}" />

<kul:tab tabTitle="Transaction Lines" defaultOpen="true" tabErrorKey="${KFSConstants.TRANSACTION_LINE_ERRORS}">

 <div class="tab-container" align=center>
	<h3>Transaction Lines</h3>
	<c:if test="${hasSource}" >
		<endow:endowmentTransactionalLines isSource="true"/>
	</c:if>
	<c:if test="${hasTarget}" >
		<endow:endowmentTransactionalLines isSource="false"/>
	</c:if>
        
	</table>
</div>

</kul:tab>