<%--
   - The Kuali Financial System, a comprehensive financial management system for higher education.
   - 
   - Copyright 2005-2014 The Kuali Foundation
   - 
   - This program is free software: you can redistribute it and/or modify
   - it under the terms of the GNU Affero General Public License as
   - published by the Free Software Foundation, either version 3 of the
   - License, or (at your option) any later version.
   - 
   - This program is distributed in the hope that it will be useful,
   - but WITHOUT ANY WARRANTY; without even the implied warranty of
   - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   - GNU Affero General Public License for more details.
   - 
   - You should have received a copy of the GNU Affero General Public License
   - along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>

<%@ include file="/jsp/sys/kfsTldHeader.jsp"%>

<%@ tag description="render the given field in the capital asset info object"%>

<%@ attribute name="capitalAssetAccountsGroupDetails" required="true" type="java.lang.Object"
	description="The capital asset info object containing the accounting lines being displayed"%>
<%@ attribute name="capitalAssetAccountsGroupDetailsName" required="true" description="The name of the capital asset accounts group object"%>	
<%@ attribute name="readOnly" required="false" description="Whether the capital asset accounting lines should be read only" %>	
<%@ attribute name="capitalAssetAccountsGroupDetailsIndex" required="true" description="Gives the capital asset accounts group index" %>	
	
<c:set var="attributes" value="${DataDictionary.CapitalAssetAccountsGroupDetails.attributes}" />		

<c:if test="${not empty capitalAssetAccountsGroupDetails}">
	<table datatable style="border-top: 1px solid rgb(153, 153, 153); width: 78%;" cellpadding="0" cellspacing="0" summary="Asset for Accounting Lines">
		<tr>
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.capitalAssetAccountLineNumber}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.sequenceNumber}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.financialDocumentLineTypeCode}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.chartOfAccountsCode}"
				useShortLabel="true"
				hideRequiredAsterisk="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.accountNumber}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.subAccountNumber}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.financialObjectCode}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.financialSubObjectCode}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.projectCode}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.organizationReferenceId}"
				useShortLabel="true" />
			<kul:htmlAttributeHeaderCell
				attributeEntry="${attributes.amount}"
				useShortLabel="true" />
		</tr>
		<c:forEach items="${capitalAssetAccountsGroupDetails}" var="assetAccountsGroupLine" varStatus="status">
			<tr>
				<td class="datacell center">
					<div align="center" valign="middle">
						<kul:htmlControlAttribute attributeEntry="${attributes.capitalAssetAccountLineNumber}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].capitalAssetAccountLineNumber" readOnly="true"/>					
					</div>		            
				</td>
				<td class="datacell center">
					<div align="center" valign="middle">
						<kul:htmlControlAttribute attributeEntry="${attributes.sequenceNumber}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].sequenceNumber" readOnly="true"/>					
					</div>		            
				</td>
				<td class="datacell center"><div>
					<c:set var="lineType" value="${assetAccountsGroupLine.financialDocumentLineTypeCode}" />
					<c:if test="${lineType eq KFSConstants.SOURCE_ACCT_LINE_TYPE_CODE}">
						<c:out value="${KFSConstants.SOURCE}" />
					</c:if>
					<c:if test="${lineType eq KFSConstants.TARGET_ACCT_LINE_TYPE_CODE}">
						<c:out value="${KFSConstants.TARGET}" />
					</c:if>
					</div></td>
					<td class="datacell center"><kul:htmlControlAttribute attributeEntry="${attributes.chartOfAccountsCode}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].chartOfAccountsCode" readOnly="true"/></td>
					<td class="datacell center"><kul:htmlControlAttribute attributeEntry="${attributes.accountNumber}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].accountNumber" readOnly="true"/></td>
					<td class="datacell center"><kul:htmlControlAttribute attributeEntry="${attributes.subAccountNumber}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].subAccountNumber" readOnly="true"/></td>
					<td class="datacell center"><kul:htmlControlAttribute attributeEntry="${attributes.financialObjectCode}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].financialObjectCode" readOnly="true"/></td>
					<td class="datacell center"><kul:htmlControlAttribute attributeEntry="${attributes.financialSubObjectCode}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].financialSubObjectCode" readOnly="true"/></td>
					<td class="datacell center"><kul:htmlControlAttribute attributeEntry="${attributes.projectCode}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].projectCode" readOnly="true"/></td>
					<td class="datacell center"><kul:htmlControlAttribute attributeEntry="${attributes.organizationReferenceId}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].organizationReferenceId" readOnly="true"/></td>
					<td class="datacell center">
						<div align="right" valign="middle">
							<kul:htmlControlAttribute attributeEntry="${attributes.amount}" property="${capitalAssetAccountsGroupDetailsName}[${status.index}].amount" readOnly="true"/>
						</div>							
					</td>
			</tr>
		</c:forEach>
	</table>
</c:if>	
