<%--
 Copyright 2006-2007 The Kuali Foundation.
 
 Licensed under the Educational Community License, Version 1.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.opensource.org/licenses/ecl1.php
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
--%>
<%@ include file="/jsp/kfs/kfsTldHeader.jsp"%>

<%@ attribute name="documentAttributes" required="true" type="java.util.Map"
              description="The DataDictionary entry containing attributes for this doc's fields." %>
<%@ attribute name="vendorQuoteAttributes" required="true" type="java.util.Map"
              description="The DataDictionary entry containing attributes for this row's fields." %>

<c:set var="amendmentEntry"
	value="${(not empty KualiForm.editingMode['amendmentEntry'])}" />

<kul:tab tabTitle="Quote" defaultOpen="false"
	tabErrorKey="${PurapConstants.QUOTE_TAB_ERRORS}">
	<div class="tab-container" align=center><!--  if (fullEntryMode or amendmentEntry), then display the addLine -->
	<table cellpadding="0" cellspacing="0" class="datatable"
		summary="Items Section">
		
		<!-- if status is OPEN or vendor list is not empty -->
		<c:if test="${KualiForm.document.statusCode eq 'QUOT'}">
		<tr>
			<td colspan="5" class="subhead">
				<span class="subhead-left">General Information</span>
				<span class="subhead-right">
					<html:image
	property="methodToCall.printQuoteList"
	src="${ConfigProperties.externalizable.images.url}tinybutton-prntquolist.gif"
	alt="print quote list" title="print quote list"
	styleClass="tinybutton" />
				</span>
			</td>
		</tr>
        <tr>
             <th align=right valign=middle class="bord-l-b">
                 <div align="right"><kul:htmlAttributeLabel attributeEntry="${documentAttributes.purchaseOrderQuoteDueDate}" /></div>
             </th>
             <td align=left valign=middle class="datacell">
                 <kul:htmlControlAttribute attributeEntry="${documentAttributes.purchaseOrderQuoteDueDate}" property="document.purchaseOrderQuoteDueDate" />
             </td>
             <th align=right valign=middle class="bord-l-b" rowspan="2">
                 <div align="right"><kul:htmlAttributeLabel attributeEntry="${documentAttributes.purchaseOrderQuoteVendorNoteText}" /></div>
             </th>
             <td align=left valign=middle class="datacell" rowspan="2" colspan="2">
                 <kul:htmlControlAttribute attributeEntry="${documentAttributes.purchaseOrderQuoteVendorNoteText}" property="document.purchaseOrderQuoteVendorNoteText" />
             </td>
        </tr>
        <tr>
             <th align=right valign=middle class="bord-l-b">
                 <div align="right"><kul:htmlAttributeLabel attributeEntry="${documentAttributes.purchaseOrderQuoteTypeCode}" /></div>
             </th>
             <td align=left valign=middle class="datacell">
                 <kul:htmlControlAttribute attributeEntry="${documentAttributes.purchaseOrderQuoteTypeCode}" property="document.purchaseOrderQuoteTypeCode" />
             </td>
        </tr>

        <tr>
             <td colspan="5">&nbsp;</td>
        </tr>

        <tr>
			<td colspan="5" class="subhead">
				<span class="subhead-left">New Vendor</span>
			</td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorName}" /></div>
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorName}" property="newPurchaseOrderVendorQuote.vendorName" />
            </td>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorHeaderGeneratedIdentifier}" /></div>
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorHeaderGeneratedIdentifier}" property="newPurchaseOrderVendorQuote.vendorHeaderGeneratedIdentifier" />
            </td>
            <td rowspan="9"><html:image
	property="methodToCall.addVendor"
	src="${ConfigProperties.externalizable.images.url}tinybutton-addvendor.gif"
	alt="add vendor" title="add vendor"
	styleClass="tinybutton" />
			</td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorLine1Address}" /></div>
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorLine1Address}" property="newPurchaseOrderVendorQuote.vendorLine1Address" />
            </td>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorPhoneNumber}" /></div>
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorPhoneNumber}" property="newPurchaseOrderVendorQuote.vendorPhoneNumber" />
            </td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorLine2Address}" /></div>
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorLine2Address}" property="newPurchaseOrderVendorQuote.vendorLine2Address" />
            </td>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorFaxNumber}" /></div>
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorFaxNumber}" property="newPurchaseOrderVendorQuote.vendorFaxNumber" />
            </td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorCityName}" />
                &nbsp;/&nbsp;
                <kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorStateCode}" /></div>
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorCityName}" property="newPurchaseOrderVendorQuote.vendorCityName" />
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorStateCode}" property="newPurchaseOrderVendorQuote.vendorStateCode" />
            </td>
            <td colspan="2">&nbsp;</td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorPostalCode}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorPostalCode}" property="newPurchaseOrderVendorQuote.vendorPostalCode" />
            </td>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteTransmitTypeCode}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteTransmitTypeCode}" property="newPurchaseOrderVendorQuote.purchaseOrderQuoteTransmitTypeCode" />
            </td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.vendorAttentionName}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.vendorAttentionName}" property="newPurchaseOrderVendorQuote.vendorAttentionName" />
            </td>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteTransmitDate}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteTransmitDate}" property="newPurchaseOrderVendorQuote.purchaseOrderQuoteTransmitDate" />
            </td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.purchaseOrderQuotePriceExpirationDate}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.purchaseOrderQuotePriceExpirationDate}" property="newPurchaseOrderVendorQuote.purchaseOrderQuotePriceExpirationDate" />
            </td>
            <td colspan="2">&nbsp;</td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteStatusCode}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteStatusCode}" property="newPurchaseOrderVendorQuote.purchaseOrderQuoteStatusCode" />
            </td>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteRankNumber}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteRankNumber}" property="newPurchaseOrderVendorQuote.purchaseOrderQuoteRankNumber" />
            </td>
        </tr>
        <tr>
            <th align=right valign=middle class="bord-l-b">
                Award:
            </th>
            <td align=left valign=middle class="datacell">
				&nbsp;
            </td>
            <th align=right valign=middle class="bord-l-b">
                <div align="right"><kul:htmlAttributeLabel attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteAwardDate}" />
            </th>
            <td align=left valign=middle class="datacell">
                <kul:htmlControlAttribute attributeEntry="${vendorQuoteAttributes.purchaseOrderQuoteAwardDate}" property="newPurchaseOrderVendorQuote.purchaseOrderQuoteAwardDate" />
            </td>
        </tr>

		<logic:iterate indexId="ctr" name="KualiForm" property="document.purchaseOrderVendorQuotes" id="quoteLine">
		    <purap:quoteVendor
		        documentAttributes="${DataDictionary.KualiPurchaseOrderDocument.attributes}"
		        vendorQuoteAttributes="${DataDictionary.PurchaseOrderVendorQuote.attributes}"
		        ctr="${ctr}" /> 
		</logic:iterate>

		<tr>
			<td colspan="5">
				<div align="center">
					<html:image
	property="methodToCall.completeQuote"
	src="${ConfigProperties.externalizable.images.url}tinybutton-completequote.gif"
	alt="complete quote" title="complete quote"
	styleClass="tinybutton" />
					<html:image
	property="methodToCall.cancelQuote"
	src="${ConfigProperties.externalizable.images.url}tinybutton-cancelquote.gif"
	alt="cancel quote" title="cancel quote"
	styleClass="tinybutton" />
				</div>
			</td>
		</tr>
		</c:if>
		<c:if test="${not (KualiForm.document.statusCode eq 'QUOT')}">
		<tr>
			<td colspan="5" class="subhead">
				<span class="subhead-right">
					<html:image
	property="methodToCall.initiateQuote"
	src="${ConfigProperties.externalizable.images.url}tinybutton-initiatequote.gif"
	alt="initiate quote" title="initiate quote"
	styleClass="tinybutton" />
				</span>
			</td>
		</tr>
		</c:if>

	</table>

	</div>
</kul:tab>
