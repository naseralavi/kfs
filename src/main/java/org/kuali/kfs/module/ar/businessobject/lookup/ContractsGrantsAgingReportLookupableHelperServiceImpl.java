/*
 * The Kuali Financial System, a comprehensive financial management system for higher education.
 * 
 * Copyright 2005-2014 The Kuali Foundation
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kuali.kfs.module.ar.businessobject.lookup;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.kuali.kfs.integration.cg.ContractsAndGrantsBillingAgency;
import org.kuali.kfs.module.ar.ArConstants;
import org.kuali.kfs.module.ar.ArPropertyConstants;
import org.kuali.kfs.module.ar.businessobject.ContractsAndGrantsAgingReport;
import org.kuali.kfs.module.ar.businessobject.ContractsGrantsAgingOpenInvoicesReport;
import org.kuali.kfs.module.ar.businessobject.Customer;
import org.kuali.kfs.module.ar.businessobject.CustomerAgingReportDetail;
import org.kuali.kfs.module.ar.businessobject.CustomerCreditMemoDetail;
import org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument;
import org.kuali.kfs.module.ar.document.CustomerCreditMemoDocument;
import org.kuali.kfs.module.ar.document.service.CustomerCreditMemoDocumentService;
import org.kuali.kfs.module.ar.report.service.ContractsGrantsAgingReportService;
import org.kuali.kfs.module.ar.report.service.ContractsGrantsReportHelperService;
import org.kuali.kfs.module.ar.report.service.CustomerAgingReportService;
import org.kuali.kfs.module.ar.web.struts.ContractsGrantsAgingReportForm;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.util.KfsDateUtils;
import org.kuali.rice.core.api.datetime.DateTimeService;
import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.kuali.rice.core.web.format.DateFormatter;
import org.kuali.rice.core.web.format.Formatter;
import org.kuali.rice.kew.impl.document.search.DocumentSearchCriteriaBo;
import org.kuali.rice.kim.api.identity.Person;
import org.kuali.rice.kns.document.authorization.BusinessObjectRestrictions;
import org.kuali.rice.kns.lookup.KualiLookupableHelperServiceImpl;
import org.kuali.rice.kns.web.comparator.CellComparatorHelper;
import org.kuali.rice.kns.web.struts.form.LookupForm;
import org.kuali.rice.kns.web.ui.Column;
import org.kuali.rice.kns.web.ui.ResultRow;
import org.kuali.rice.krad.bo.BusinessObject;
import org.kuali.rice.krad.bo.PersistableBusinessObject;
import org.kuali.rice.krad.lookup.CollectionIncomplete;
import org.kuali.rice.krad.service.KualiModuleService;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.KRADConstants;
import org.kuali.rice.krad.util.ObjectUtils;
import org.kuali.rice.krad.util.UrlFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Lookupable Helper Service class for ContractsGrantsAgingReport.
 */
public class ContractsGrantsAgingReportLookupableHelperServiceImpl extends KualiLookupableHelperServiceImpl implements InitializingBean {
    private org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(ContractsGrantsAgingReportLookupableHelperServiceImpl.class);
    protected DateTimeService dateTimeService;
    protected ContractsGrantsAgingReportService contractsGrantsAgingReportService;
    protected ContractsGrantsReportHelperService contractsGrantsReportHelperService;
    protected KualiModuleService moduleService;
    protected CustomerAgingReportService customerAgingReportService;
    protected CustomerCreditMemoDocumentService customerCreditMemoDocumentService;

    private String customerNameLabel;
    private String customerNumberLabel;
    private String cutoffdate30Label;
    private String cutoffdate60Label;
    private String cutoffdate90Label;

    private KualiDecimal total0to30 = KualiDecimal.ZERO;
    private KualiDecimal total31to60 = KualiDecimal.ZERO;
    private KualiDecimal total61to90 = KualiDecimal.ZERO;
    private KualiDecimal total91toSYSPR = KualiDecimal.ZERO;
    private KualiDecimal totalSYSPRplus1orMore = KualiDecimal.ZERO;

    private KualiDecimal totalOpenInvoices = KualiDecimal.ZERO;
    private KualiDecimal totalCredits = KualiDecimal.ZERO;
    private KualiDecimal totalWriteOffs = KualiDecimal.ZERO;

    private String reportOption;
    private Date reportRunDate;
    private String customerNumber;
    private String customerName;
    private String accountNumber;
    private String accountChartCode;
    private String fundManager;
    private String proposalNumber;
    private String collector;
    private String awardDocumentNumber;
    private String markedAsFinal;
    private java.sql.Date awardEndDate;
    private String invoiceAmountFrom;
    private String invoiceAmountTo;
    private String invoiceNumber;


    private String orgCode;
    private String chartCode;
    private String nbrDaysForLastBucket;
    // default is 120 days
    private String cutoffdate91toSYSPRlabel;
    private String cutoffdateSYSPRplus1orMorelabel;
    private String agencyShortName = ArConstants.ContractsGrantsAgingReportFields.AGENCY_SHORT_NAME;
    private List<String> customers;

    /**
     * Get the search results that meet the input search criteria.
     *
     * @param fieldValues - Map containing prop name keys and search values
     * @return a List of found business objects
     */
    @Override
    public List getSearchResults(Map fieldValues) {
        List<ContractsAndGrantsAgingReport> results = new ArrayList<ContractsAndGrantsAgingReport>();
        setBackLocation((String) fieldValues.get(KFSConstants.BACK_LOCATION));
        setDocFormKey((String) fieldValues.get(KFSConstants.DOC_FORM_KEY));

        total0to30 = KualiDecimal.ZERO;
        total31to60 = KualiDecimal.ZERO;
        total61to90 = KualiDecimal.ZERO;
        total91toSYSPR = KualiDecimal.ZERO;
        totalSYSPRplus1orMore = KualiDecimal.ZERO;
        totalOpenInvoices = KualiDecimal.ZERO;
        totalWriteOffs = KualiDecimal.ZERO;
        totalCredits = KualiDecimal.ZERO;

        Map<String, ContractsAndGrantsAgingReport> knownCustomers = new HashMap<String, ContractsAndGrantsAgingReport>();
        ContractsAndGrantsAgingReport custDetail;

        try {
            java.util.Date today = getDateTimeService().getCurrentDate();
            String reportRunDateStr = (String) fieldValues.get(ArPropertyConstants.CustomerAgingReportFields.REPORT_RUN_DATE);

            reportRunDate = (ObjectUtils.isNull(reportRunDateStr) || reportRunDateStr.isEmpty()) ?
                                                today :
                                                getDateTimeService().convertToDate(reportRunDateStr);

            // retrieve filtered data according to the lookup
            Map<String, List<ContractsGrantsInvoiceDocument>> cgMapByCustomer = getContractsGrantsAgingReportService().filterContractsGrantsAgingReport(fieldValues, null, new java.sql.Date(reportRunDate.getTime()));

            // set dates for buckets
            Date cutoffdate30 = DateUtils.addDays(reportRunDate, -30);
            Date cutoffdate31 = DateUtils.addDays(reportRunDate, -31);
            Date cutoffdate60 = DateUtils.addDays(reportRunDate, -60);
            Date cutoffdate61 = DateUtils.addDays(reportRunDate, -61);
            Date cutoffdate90 = DateUtils.addDays(reportRunDate, -90);
            Date cutoffdate91 = DateUtils.addDays(reportRunDate, -91);
            Date cutoffdate120 = DateUtils.addDays(reportRunDate, -1 * Integer.parseInt(nbrDaysForLastBucket));
            Date cutoffdate121 = DateUtils.addDays(cutoffdate120, -1);

            if (!MapUtils.isEmpty(cgMapByCustomer)) {
                // 30 days
                computeFor0To30DaysByBillingChartAndOrg(cgMapByCustomer, new java.sql.Date(cutoffdate30.getTime()), new java.sql.Date(reportRunDate.getTime()), knownCustomers);
                // 60 days
                computeFor31To60DaysByBillingChartAndOrg(cgMapByCustomer, new java.sql.Date(cutoffdate60.getTime()), new java.sql.Date(cutoffdate31.getTime()), knownCustomers);
                // 90 days
                computeFor61To90DaysByBillingChartAndOrg(cgMapByCustomer, new java.sql.Date(cutoffdate90.getTime()), new java.sql.Date(cutoffdate61.getTime()), knownCustomers);
                // 120 days
                computeFor91ToSYSPRDaysByBillingChartAndOrg(cgMapByCustomer, new java.sql.Date(cutoffdate120.getTime()), new java.sql.Date(cutoffdate91.getTime()), knownCustomers);
                // 120 + older
                computeForSYSPRplus1orMoreDaysByBillingChartAndOrg(cgMapByCustomer, null, new java.sql.Date(cutoffdate121.getTime()), knownCustomers);
                // credits
                calculateTotalCreditsForCustomers(cgMapByCustomer, knownCustomers);

            }

            // prepare customer map.
            for (ContractsAndGrantsAgingReport detail : knownCustomers.values()) {
                // get agency name for customer
                ContractsAndGrantsBillingAgency agencyObj = getAgencyByCustomer(detail.getCustomerNumber());
                if (ObjectUtils.isNotNull(agencyObj)) {
                    detail.setReportingName(agencyObj.getReportingName());
                    detail.setAgencyNumber(agencyObj.getAgencyNumber());
                }

                // set total open invoices
                KualiDecimal amount = detail.getUnpaidBalance0to30().add(detail.getUnpaidBalance31to60()).add(detail.getUnpaidBalance61to90()).add(detail.getUnpaidBalance91toSYSPR().add(detail.getUnpaidBalanceSYSPRplus1orMore()));
                detail.setTotalOpenInvoices(amount);
                totalOpenInvoices = totalOpenInvoices.add(amount);

                // find total writeoff
                KualiDecimal writeOffAmt = getCustomerAgingReportService().findWriteOffAmountByCustomerNumber(detail.getCustomerNumber());
                if (ObjectUtils.isNotNull(writeOffAmt)) {
                    totalWriteOffs = totalWriteOffs.add(writeOffAmt);
                }
                else {
                    writeOffAmt = KualiDecimal.ZERO;
                }
                detail.setTotalWriteOff(writeOffAmt);

                // calculate total credits
                results.add(detail);
            }
        }
        catch (NumberFormatException | ParseException ex) {
            throw new RuntimeException("Could not parse report run date for lookup",ex);
        }

        return new CollectionIncomplete<ContractsAndGrantsAgingReport>(results, (long) results.size());
    }

    /**
     * @return a List of the names of fields which are marked in data dictionary as return fields.
     */
    @Override
    public List getReturnKeys() {
        return new ArrayList(fieldConversions.keySet());
    }

    /**
     * @return a List of the names of fields which are marked in data dictionary as return fields.
     */
    @Override
    protected Properties getParameters(BusinessObject bo, Map fieldConversions, String lookupImpl, List pkNames) {
        Properties parameters = new Properties();
        parameters.put(KRADConstants.DISPATCH_REQUEST_PARAMETER, KRADConstants.RETURN_METHOD_TO_CALL);
        parameters.put(KRADConstants.DOC_FORM_KEY, getDocFormKey());
        parameters.put(KRADConstants.REFRESH_CALLER, lookupImpl);
        if (ObjectUtils.isNotNull(getReferencesToRefresh())) {
            parameters.put(KRADConstants.REFERENCES_TO_REFRESH, getReferencesToRefresh());
        }

        for (Object o : getReturnKeys()) {
            String fieldNm = (String) o;

            Object fieldVal = ObjectUtils.getPropertyValue(bo, fieldNm);
            if (ObjectUtils.isNull(fieldVal)) {
                fieldVal = KRADConstants.EMPTY_STRING;
            }

            // Encrypt value if it is a secure field
            if (fieldConversions.containsKey(fieldNm)) {
                fieldNm = (String) fieldConversions.get(fieldNm);
            }

            if (getBusinessObjectAuthorizationService().attributeValueNeedsToBeEncryptedOnFormsAndLinks(bo.getClass(), fieldNm)) {
            }

            // need to format date in url
            if (fieldVal instanceof Date) {
                DateFormatter dateFormatter = new DateFormatter();
                fieldVal = dateFormatter.format(fieldVal);
            }

            parameters.put(fieldNm, fieldVal.toString());
        }

        return parameters;
    }

    /**
     * This method performs the lookup and returns a collection of lookup items
     *
     * @param lookupForm
     * @param kualiLookupable
     * @param resultTable
     * @param bounded
     * @return
     */
    @Override
    public Collection performLookup(LookupForm lookupForm, Collection resultTable, boolean bounded) {
        Collection displayList  = getSearchResults(lookupForm.getFieldsForLookup());

        // MJM get resultTable populated here
        if (bounded) {
            HashMap<String, Class> propertyTypes = new HashMap<String, Class>();

            boolean hasReturnableRow = false;

            Person user = GlobalVariables.getUserSession().getPerson();

            // iterate through result list and wrap rows with return url and action urls
            for (Object aDisplayList : displayList) {
                BusinessObject element = (BusinessObject) aDisplayList;

                BusinessObjectRestrictions businessObjectRestrictions = getBusinessObjectAuthorizationService().getLookupResultRestrictions(element, user);

                if (ObjectUtils.isNotNull(getColumns())) {
                    List<Column> columns = getColumns();
                    populateCutoffdateLabels();
                    for (Object column : columns) {

                        Column col = (Column) column;
                        Formatter formatter = col.getFormatter();

                        // pick off result column from result list, do formatting
                        Object prop = ObjectUtils.getPropertyValue(element, col.getPropertyName());

                        String propValue = ObjectUtils.getFormattedPropertyValue(element, col.getPropertyName(), col.getFormatter());
                        Class propClass = getPropertyClass(element, col.getPropertyName());

                        // formatters
                        if (ObjectUtils.isNotNull(prop)) {
                            propValue = getContractsGrantsReportHelperService().formatByType(prop, formatter);
                        }

                        // comparator


                        col.setComparator(CellComparatorHelper.getAppropriateComparatorForPropertyClass(propClass));
                        col.setValueComparator(CellComparatorHelper.getAppropriateValueComparatorForPropertyClass(propClass));

                        propValue = super.maskValueIfNecessary(element.getClass(), col.getPropertyName(), propValue, businessObjectRestrictions);
                        col.setPropertyValue(propValue);

                        // add correct label for sysparam
                        if (StringUtils.equals("unpaidBalance91toSYSPR", col.getPropertyName())) {
                            col.setColumnTitle(cutoffdate91toSYSPRlabel);
                        }
                        if (StringUtils.equals("unpaidBalanceSYSPRplus1orMore", col.getPropertyName())) {
                            col.setColumnTitle(cutoffdateSYSPRplus1orMorelabel);
                        }
                        if (StringUtils.equals("reportingName", col.getPropertyName())) {
                            col.setColumnTitle(agencyShortName);
                        }

                        if (StringUtils.isNotBlank(propValue)) {
                            // do not add link to the values in column "Customer Name"
                            if (StringUtils.equals(customerNameLabel, col.getColumnTitle())) {
                                col.setPropertyURL(getCustomerLookupUrl(element, col.getColumnTitle()));
                            }
                            else if (StringUtils.equals(customerNumberLabel, col.getColumnTitle())) {
                                col.setPropertyURL(getCustomerOpenInvoicesReportUrl(element, col.getColumnTitle(), lookupForm.getFieldsForLookup()));
                            }
                            else if (StringUtils.equals(ArConstants.ContractsGrantsAgingReportFields.TOTAL_CREDITS, col.getColumnTitle())) {
                                col.setPropertyURL(getCreditMemoDocSearchUrl(element, col.getColumnTitle()));
                            }
                            else if (StringUtils.equals(ArConstants.ContractsGrantsAgingReportFields.TOTAL_WRITEOFF, col.getColumnTitle())) {
                                col.setPropertyURL(getCustomerWriteoffSearchUrl(element, col.getColumnTitle()));
                            }
                            else if (StringUtils.equals(ArConstants.ContractsGrantsAgingReportFields.AGENCY_SHORT_NAME, col.getColumnTitle())) {
                                col.setPropertyURL(getAgencyInquiryUrl(element, col.getColumnTitle()));
                            }
                            else {
                                col.setPropertyURL(getCustomerOpenInvoicesReportUrl(element, col.getColumnTitle(), lookupForm.getFieldsForLookup()));
                            }
                        }

                    }

                    ResultRow row = new ResultRow(columns, KFSConstants.EMPTY_STRING, KFSConstants.EMPTY_STRING);
                    if (element instanceof PersistableBusinessObject) {
                        row.setObjectId(((PersistableBusinessObject) element).getObjectId());
                    }

                    boolean rowReturnable = isResultReturnable(element);
                    row.setRowReturnable(rowReturnable);
                    if (rowReturnable) {
                        hasReturnableRow = true;
                    }

                    resultTable.add(row);
                }

                lookupForm.setHasReturnableRow(hasReturnableRow);
            }


            if (displayList.size() != 0) {
                ((ContractsGrantsAgingReportForm) lookupForm).setTotal0to30(total0to30.toString());
                ((ContractsGrantsAgingReportForm) lookupForm).setTotal31to60(total31to60.toString());
                ((ContractsGrantsAgingReportForm) lookupForm).setTotal61to90(total61to90.toString());
                ((ContractsGrantsAgingReportForm) lookupForm).setTotal91toSYSPR(total91toSYSPR.toString());
                ((ContractsGrantsAgingReportForm) lookupForm).setTotalSYSPRplus1orMore(totalSYSPRplus1orMore.toString());
                ((ContractsGrantsAgingReportForm) lookupForm).setTotalOpenInvoices(totalOpenInvoices.toString());
                ((ContractsGrantsAgingReportForm) lookupForm).setTotalCredits(totalCredits.toString());
                ((ContractsGrantsAgingReportForm) lookupForm).setTotalWriteOffs(totalWriteOffs.toString());
            }
        }
        return displayList;
    }

    protected String getCustomerOpenInvoicesReportUrl(BusinessObject bo, String columnTitle, Map<String, String> fieldsMap) {
        Properties parameters = new Properties();

        ContractsAndGrantsAgingReport detail = (ContractsAndGrantsAgingReport) bo;

        parameters.put(KFSConstants.BUSINESS_OBJECT_CLASS_ATTRIBUTE, ContractsGrantsAgingOpenInvoicesReport.class.getName());
        parameters.put("lookupableImplementaionServiceName", "arContractsGrantsAgingOpenInvoicesReportLookupable");
        parameters.put(KFSConstants.DISPATCH_REQUEST_PARAMETER, KFSConstants.SEARCH_METHOD);
        parameters.put("reportName", ArConstants.ContractsGrantsAgingReportFields.OPEN_INVOCE_REPORT_NAME);
        parameters.put(KFSConstants.DOC_FORM_KEY, "88888888");

        if (ObjectUtils.isNotNull(fieldsMap) && !fieldsMap.isEmpty()) {
            for (String key : fieldsMap.keySet()) {
                String val = fieldsMap.get(key);
                // put if val is not blank or null
                if (ObjectUtils.isNotNull(val) && StringUtils.isNotEmpty(val)) {
                    parameters.put(key.toString(), fieldsMap.get(key).toString());
                }
            }
        }

        parameters.put(KFSPropertyConstants.CUSTOMER_NUMBER, detail.getCustomerNumber());
        parameters.put(KFSPropertyConstants.CUSTOMER_NAME, detail.getCustomerName());

        // Report Option
        if(ObjectUtils.isNotNull(reportOption)){
            parameters.put(ArPropertyConstants.REPORT_OPTION, reportOption);
        }
        // Report Run Date
        parameters.put(ArPropertyConstants.ContractsGrantsAgingReportFields.REPORT_RUN_DATE, getDateTimeService().toDateString(reportRunDate));

        // put bucket dates
        if (StringUtils.equals(columnTitle, customerNumberLabel)) {
            parameters.put(ArPropertyConstants.COLUMN_TITLE, KFSConstants.CustomerOpenItemReport.ALL_DAYS);
            parameters.put(ArPropertyConstants.START_DATE, KFSConstants.EMPTY_STRING);
            parameters.put(ArPropertyConstants.END_DATE, getDateTimeService().toDateString(reportRunDate));
        }
        else {
            if (StringUtils.equals(columnTitle, cutoffdate30Label)) {
                parameters.put(ArPropertyConstants.START_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -30)));
                parameters.put(ArPropertyConstants.END_DATE, getDateTimeService().toDateString(reportRunDate));
            }
            else if (StringUtils.equals(columnTitle, cutoffdate60Label)) {
                parameters.put(ArPropertyConstants.START_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -60)));
                parameters.put(ArPropertyConstants.END_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -31)));
            }
            else if (StringUtils.equals(columnTitle, cutoffdate90Label)) {
                parameters.put(ArPropertyConstants.START_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -90)));
                parameters.put(ArPropertyConstants.END_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -61)));
            }
            else if (StringUtils.equals(columnTitle, cutoffdate91toSYSPRlabel)) {
                parameters.put(ArPropertyConstants.START_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -120)));
                parameters.put(ArPropertyConstants.END_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -91)));
            }
            else if (StringUtils.equals(columnTitle, cutoffdateSYSPRplus1orMorelabel)) {
                parameters.put(ArPropertyConstants.START_DATE, KFSConstants.EMPTY_STRING);
                parameters.put(ArPropertyConstants.END_DATE, getDateTimeService().toDateString(DateUtils.addDays(reportRunDate, -121)));
                columnTitle = Integer.toString((Integer.parseInt(nbrDaysForLastBucket)) + 1) + " days and older";
            }
            else {
                parameters.put(ArPropertyConstants.START_DATE, KFSConstants.EMPTY_STRING);
                parameters.put(ArPropertyConstants.END_DATE, getDateTimeService().toDateString(reportRunDate));
            }
            parameters.put(ArPropertyConstants.COLUMN_TITLE, columnTitle);
        }
        return UrlFactory.parameterizeUrl("arContractsGrantsAgingOpenInvoicesReportLookup.do", parameters);
    }

    /**
     * This method returns the url for the paid invoice of the customer
     *
     * @param bo
     * @param columnTitle
     * @return Returns the url for the Payment Application search.
     */
    protected String getCreditMemoDocSearchUrl(BusinessObject bo, String columnTitle) {
        Properties params = new Properties();
        ContractsAndGrantsAgingReport detail = (ContractsAndGrantsAgingReport) bo;
//      Note
        params.put(KFSConstants.BUSINESS_OBJECT_CLASS_ATTRIBUTE, DocumentSearchCriteriaBo.class.getName());
        params.put(KFSConstants.DISPATCH_REQUEST_PARAMETER, KFSConstants.START_METHOD);
        params.put(KFSConstants.DOC_FORM_KEY, "88888888");
        params.put(KFSConstants.HIDE_LOOKUP_RETURN_LINK, KFSConstants.Booleans.TRUE);
        params.put(KFSConstants.DOCUMENT_TYPE_FULL_NAME, "CRM");
        params.put(ArPropertyConstants.CustomerFields.CUSTOMER_NUMBER, detail.getCustomerNumber());
        return UrlFactory.parameterizeUrl(KFSConstants.LOOKUP_ACTION, params);
    }

    /**
     * This method returns the url for the customer write off doc search
     *
     * @param bo
     * @param columnTitle
     * @return Returns the Url for the customer write off doc search
     */
    protected String getCustomerWriteoffSearchUrl(BusinessObject bo, String columnTitle) {
        Properties params = new Properties();
        ContractsAndGrantsAgingReport detail = (ContractsAndGrantsAgingReport) bo;
//        Note
        params.put(KFSConstants.BUSINESS_OBJECT_CLASS_ATTRIBUTE, DocumentSearchCriteriaBo.class.getName());
        params.put(KFSConstants.DISPATCH_REQUEST_PARAMETER, KFSConstants.START_METHOD);
        params.put(KFSConstants.DOC_FORM_KEY, "88888888");
        params.put(KFSConstants.HIDE_LOOKUP_RETURN_LINK, KFSConstants.Booleans.TRUE);
        params.put(KFSConstants.DOCUMENT_TYPE_FULL_NAME, "INVW");
        params.put(ArPropertyConstants.CustomerFields.CUSTOMER_NUMBER, detail.getCustomerNumber());
        return UrlFactory.parameterizeUrl(KFSConstants.LOOKUP_ACTION, params);
    }

    /**
     * This method returns the customer lookup url
     *
     * @param bo business object
     * @param columnTitle
     * @return Returns the url for the customer lookup
     */
    protected String getCustomerLookupUrl(BusinessObject bo, String columnTitle) {
        Properties params = new Properties();
        ContractsAndGrantsAgingReport detail = (ContractsAndGrantsAgingReport) bo;
        params.put(KFSConstants.BUSINESS_OBJECT_CLASS_ATTRIBUTE, Customer.class.getName());
        params.put(KFSConstants.RETURN_LOCATION_PARAMETER, "portal.do");
        params.put(KFSConstants.DISPATCH_REQUEST_PARAMETER, KFSConstants.START_METHOD);
        params.put(KFSConstants.DOC_FORM_KEY, "88888888");
        params.put(KFSConstants.HIDE_LOOKUP_RETURN_LINK, "true");
        params.put(ArPropertyConstants.CustomerFields.CUSTOMER_NUMBER, detail.getCustomerNumber());
        params.put(ArPropertyConstants.CustomerInvoiceWriteoffLookupResultFields.CUSTOMER_NAME, detail.getCustomerName());
        return UrlFactory.parameterizeUrl(KFSConstants.LOOKUP_ACTION, params);
    }

    /**
     * This method returns the Agency inquiry url
     *
     * @param bo business object
     * @param columnTitle
     * @return Returns the url for the Agency Inquiry
     */
    protected String getAgencyInquiryUrl(BusinessObject bo, String columnTitle) {
        Properties params = new Properties();
        ContractsAndGrantsAgingReport detail = (ContractsAndGrantsAgingReport) bo;
        params.put(KFSConstants.BUSINESS_OBJECT_CLASS_ATTRIBUTE, ContractsAndGrantsBillingAgency.class.getName());
        params.put(KFSConstants.DISPATCH_REQUEST_PARAMETER, "continueWithInquiry");
        params.put(KFSConstants.DOC_FORM_KEY, "88888888");
        params.put(KFSConstants.HIDE_LOOKUP_RETURN_LINK, "true");
        params.put(KFSPropertyConstants.AGENCY_NUMBER, detail.getAgencyNumber());
        return UrlFactory.parameterizeUrl(KFSConstants.INQUIRY_ACTION, params);
    }

    protected void populateCutoffdateLabels() {
        customerNameLabel = getDataDictionaryService().getDataDictionary().getBusinessObjectEntry(ContractsAndGrantsAgingReport.class.getName()).getAttributeDefinition(KFSConstants.CustomerAgingReport.CUSTOMER_NAME).getLabel();
        customerNumberLabel = getDataDictionaryService().getDataDictionary().getBusinessObjectEntry(ContractsAndGrantsAgingReport.class.getName()).getAttributeDefinition(KFSConstants.CustomerOpenItemReport.CUSTOMER_NUMBER).getLabel();
        cutoffdate30Label = getDataDictionaryService().getDataDictionary().getBusinessObjectEntry(ContractsAndGrantsAgingReport.class.getName()).getAttributeDefinition(KFSConstants.CustomerAgingReport.UNPAID_BALANCE_0_TO_30).getLabel();
        cutoffdate60Label = getDataDictionaryService().getDataDictionary().getBusinessObjectEntry(ContractsAndGrantsAgingReport.class.getName()).getAttributeDefinition(KFSConstants.CustomerAgingReport.UNPAID_BALANCE_31_TO_60).getLabel();
        cutoffdate90Label = getDataDictionaryService().getDataDictionary().getBusinessObjectEntry(ContractsAndGrantsAgingReport.class.getName()).getAttributeDefinition(KFSConstants.CustomerAgingReport.UNPAID_BALANCE_61_TO_90).getLabel();
    }

    /**
     * This method gets dateTimeService attribute.
     *
     * @return Returns the dateTimeService.
     */
    public DateTimeService getDateTimeService() {
        return dateTimeService;
    }

    /**
     * This method sets the dateTimeService attribute.
     *
     * @param dateTimeService The dateTimeService to set.
     */
    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    /**
     * Gets the total0to30 attribute.
     *
     * @return Returns the total0to30.
     */
    public KualiDecimal getTotal0to30() {
        return total0to30;
    }

    /**
     * Sets the total0to30 attribute value.
     *
     * @param total0to30 The total0to30 to set.
     */
    public void setTotal0to30(KualiDecimal total0to30) {
        this.total0to30 = total0to30;
    }

    /**
     * Gets the total31to60 attribute.
     *
     * @return Returns the total31to60.
     */
    public KualiDecimal getTotal31to60() {
        return total31to60;
    }

    /**
     * Sets the total31to60 attribute value.
     *
     * @param total31to60 The total31to60 to set.
     */
    public void setTotal31to60(KualiDecimal total31to60) {
        this.total31to60 = total31to60;
    }

    /**
     * Gets the total61to90 attribute.
     *
     * @return Returns the total61to90.
     */
    public KualiDecimal getTotal61to90() {
        return total61to90;
    }

    /**
     * Sets the total61to90 attribute value.
     *
     * @param total61to90 The total61to90 to set.
     */
    public void setTotal61to90(KualiDecimal total61to90) {
        this.total61to90 = total61to90;
    }

    /**
     * Gets the total91toSYSPR attribute.
     *
     * @return Returns the total91toSYSPR.
     */
    public KualiDecimal getTotal91toSYSPR() {
        return total91toSYSPR;
    }

    /**
     * Sets the total91toSYSPR attribute value.
     *
     * @param total91toSYSPR The total91toSYSPR to set.
     */
    public void setTotal91toSYSPR(KualiDecimal total91toSYSPR) {
        this.total91toSYSPR = total91toSYSPR;
    }

    /**
     * Gets the totalSYSPRplus1orMore attribute.
     *
     * @return Returns the totalSYSPRplus1orMore.
     */
    public KualiDecimal getTotalSYSPRplus1orMore() {
        return totalSYSPRplus1orMore;
    }

    /**
     * Sets the totalSYSPRplus1orMore attribute value.
     *
     * @param totalSYSPRplus1orMore The totalSYSPRplus1orMore to set.
     */
    public void setTotalSYSPRplus1orMore(KualiDecimal totalSYSPRplus1orMore) {
        this.totalSYSPRplus1orMore = totalSYSPRplus1orMore;
    }

    /**
     * This method...
     *
     * @param knownCustomers
     * @param customer
     * @return
     */
    protected ContractsAndGrantsAgingReport pickContractsGrantsAgingReportDetail(Map<String, ContractsAndGrantsAgingReport> knownCustomers, String customer) {
        ContractsAndGrantsAgingReport agingReportDetail = null;
        if (ObjectUtils.isNull(agingReportDetail = knownCustomers.get(customer))) {
            agingReportDetail = new ContractsAndGrantsAgingReport();
            agingReportDetail.setCustomerNumber(customer.substring(0, customer.indexOf('-')));
            agingReportDetail.setCustomerName(customer.substring(customer.indexOf('-') + 1));
            knownCustomers.put(customer, agingReportDetail);
        }
        return agingReportDetail;
    }

    /**
     * This method...
     *
     * @param agingReportDao
     * @param begin
     * @param end
     * @param knownCustomers
     */
    protected void computeFor0To30DaysByBillingChartAndOrg(Map<String, List<ContractsGrantsInvoiceDocument>> cgMapByCustomer, java.sql.Date begin, java.sql.Date end, Map<String, ContractsAndGrantsAgingReport> knownCustomers) {
        Set<String> customerIds = cgMapByCustomer.keySet();
        for (String customer : customerIds) {
            ContractsAndGrantsAgingReport agingReportDetail = pickContractsGrantsAgingReportDetail(knownCustomers, customer);
            KualiDecimal amount = calculateInvoiceAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            agingReportDetail.setUnpaidBalance0to30(amount);
            total0to30 = total0to30.add(amount);
        }
    }

    /**
     * This method...
     *
     * @param agingReportDao
     * @param begin
     * @param end
     * @param knownCustomers
     */
    protected void computeFor31To60DaysByBillingChartAndOrg(Map<String, List<ContractsGrantsInvoiceDocument>> cgMapByCustomer, java.sql.Date begin, java.sql.Date end, Map<String, ContractsAndGrantsAgingReport> knownCustomers) {
        Set<String> customerIds = cgMapByCustomer.keySet();
        for (String customer : customerIds) {
            ContractsAndGrantsAgingReport agingReportDetail = pickContractsGrantsAgingReportDetail(knownCustomers, customer);
            KualiDecimal amount = calculateInvoiceAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            KualiDecimal paymentAmt = calculatePaymentAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            agingReportDetail.setUnpaidBalance31to60(amount.subtract(paymentAmt));
            total31to60 = total31to60.add(amount);
        }
    }

    /**
     * This method...
     *
     * @param agingReportDao
     * @param begin
     * @param end
     * @param knownCustomers
     */
    protected void computeFor61To90DaysByBillingChartAndOrg(Map<String, List<ContractsGrantsInvoiceDocument>> cgMapByCustomer, java.sql.Date begin, java.sql.Date end, Map<String, ContractsAndGrantsAgingReport> knownCustomers) {
        Set<String> customerIds = cgMapByCustomer.keySet();
        for (String customer : customerIds) {
            ContractsAndGrantsAgingReport agingReportDetail = pickContractsGrantsAgingReportDetail(knownCustomers, customer);
            KualiDecimal amount = calculateInvoiceAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            KualiDecimal paymentAmt = calculatePaymentAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            agingReportDetail.setUnpaidBalance61to90(amount.subtract(paymentAmt));
            total61to90 = total61to90.add(amount);
        }
    }

    /**
     * This method...
     *
     * @param agingReportDao
     * @param begin
     * @param end
     * @param knownCustomers
     */
    protected void computeFor91ToSYSPRDaysByBillingChartAndOrg(Map<String, List<ContractsGrantsInvoiceDocument>> cgMapByCustomer, java.sql.Date begin, java.sql.Date end, Map<String, ContractsAndGrantsAgingReport> knownCustomers) {
        Set<String> customerIds = cgMapByCustomer.keySet();
        for (String customer : customerIds) {
            ContractsAndGrantsAgingReport agingReportDetail = pickContractsGrantsAgingReportDetail(knownCustomers, customer);
            KualiDecimal amount = calculateInvoiceAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            KualiDecimal paymentAmt = calculatePaymentAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            agingReportDetail.setUnpaidBalance91toSYSPR(amount.subtract(paymentAmt));
            total91toSYSPR = total91toSYSPR.add(amount);
        }
    }

    /**
     * This method...
     *
     * @param agingReportDao
     * @param begin
     * @param end
     * @param knownCustomers
     */
    protected void computeForSYSPRplus1orMoreDaysByBillingChartAndOrg(Map<String, List<ContractsGrantsInvoiceDocument>> cgMapByCustomer, java.sql.Date begin, java.sql.Date end, Map<String, ContractsAndGrantsAgingReport> knownCustomers) {
        Set<String> customerIds = cgMapByCustomer.keySet();
        for (String customer : customerIds) {
            ContractsAndGrantsAgingReport agingReportDetail = pickContractsGrantsAgingReportDetail(knownCustomers, customer);
            KualiDecimal amount = calculateInvoiceAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            KualiDecimal paymentAmt = calculatePaymentAmountForCustomer(cgMapByCustomer.get(customer), begin, end);
            agingReportDetail.setUnpaidBalanceSYSPRplus1orMore(amount.subtract(paymentAmt));
            totalSYSPRplus1orMore = totalSYSPRplus1orMore.add(amount);
        }
    }

    /**
     * This method calculates the total invoice amount for the customers.
     *
     * @param cgDocs
     * @param begin
     * @param end
     * @return
     */
    protected KualiDecimal calculateInvoiceAmountForCustomer(Collection<ContractsGrantsInvoiceDocument> cgDocs, java.sql.Date begin, java.sql.Date end) {
        KualiDecimal invoiceAmt = KualiDecimal.ZERO;
        if (!CollectionUtils.isEmpty(cgDocs)) {
            for (ContractsGrantsInvoiceDocument cgDoc : cgDocs) {
                if (ObjectUtils.isNotNull(cgDoc.getBillingDate())) {
                    if (ObjectUtils.isNotNull(begin)) {
                        if (KfsDateUtils.isSameDayOrLater(cgDoc.getBillingDate(), begin) && KfsDateUtils.isSameDayOrEarlier(cgDoc.getBillingDate(), end)) {
                            invoiceAmt = invoiceAmt.add(cgDoc.getTotalDollarAmount());
                        }
                    }
                    else {
                        if (KfsDateUtils.isSameDayOrEarlier(cgDoc.getBillingDate(), end)) {
                            invoiceAmt = invoiceAmt.add(cgDoc.getTotalDollarAmount());
                        }
                    }
                }
            }
        }
        return invoiceAmt;
    }

    /**
     * This method calculates the payment amount for the customers.
     *
     * @param cgDocs
     * @param begin
     * @param end
     * @return
     */
    protected KualiDecimal calculatePaymentAmountForCustomer(Collection<ContractsGrantsInvoiceDocument> cgDocs, java.sql.Date begin, java.sql.Date end) {
        KualiDecimal invoiceAmt = KualiDecimal.ZERO;
        if (!CollectionUtils.isEmpty(cgDocs)) {
            for (ContractsGrantsInvoiceDocument cgDoc : cgDocs) {
                if (ObjectUtils.isNotNull(cgDoc.getBillingDate())) {
                    if (ObjectUtils.isNotNull(begin)) {
                        if (KfsDateUtils.isSameDayOrLater(cgDoc.getBillingDate(), begin) && KfsDateUtils.isSameDayOrEarlier(cgDoc.getBillingDate(), end)) {
                            invoiceAmt = invoiceAmt.add(cgDoc.getPaymentAmount());
                        }
                    }
                    else {
                        if (KfsDateUtils.isSameDayOrEarlier(cgDoc.getBillingDate(), end)) {
                            invoiceAmt = invoiceAmt.add(cgDoc.getPaymentAmount());
                        }
                    }
                }
            }
        }
        return invoiceAmt;
    }

    /**
     * This method retrives the agecy for particular customer
     *
     * @param customerNumber
     * @return Returns the agency for the customer
     */
    protected ContractsAndGrantsBillingAgency getAgencyByCustomer(String customerNumber) {
        Map args = new HashMap();
        args.put(KFSPropertyConstants.CUSTOMER_NUMBER, customerNumber);
        return getModuleService().getResponsibleModuleService(ContractsAndGrantsBillingAgency.class).getExternalizableBusinessObject(ContractsAndGrantsBillingAgency.class, args);
    }

    /**
     * This method calculates the total credits for the customers.
     *
     * @param cgMapByCustomer
     * @param knownCustomers
     */
    protected void calculateTotalCreditsForCustomers(Map<String, List<ContractsGrantsInvoiceDocument>> cgMapByCustomer, Map<String, ContractsAndGrantsAgingReport> knownCustomers) {
        Set<String> customerIds = cgMapByCustomer.keySet();
        KualiDecimal credits = KualiDecimal.ZERO;
        for (String customer : customerIds) {
            ContractsAndGrantsAgingReport agingReportDetail = pickContractsGrantsAgingReportDetail(knownCustomers, customer);
            List<ContractsGrantsInvoiceDocument> cgDocs = cgMapByCustomer.get(customer);
            if (!CollectionUtils.isEmpty(cgDocs)) {
                credits = KualiDecimal.ZERO;
                for (ContractsGrantsInvoiceDocument cgDoc : cgDocs) {
                    Collection<CustomerCreditMemoDocument> creditDocs = customerCreditMemoDocumentService.getCustomerCreditMemoDocumentByInvoiceDocument(cgDoc.getDocumentNumber());
                    if (ObjectUtils.isNotNull(creditDocs) && !creditDocs.isEmpty()) {
                        for (CustomerCreditMemoDocument cm : creditDocs) {
                            for (CustomerCreditMemoDetail cmDetail : cm.getCreditMemoDetails()) {
                                if (ObjectUtils.isNotNull(cmDetail.getCreditMemoItemTotalAmount())) {
                                    credits = credits.add(cmDetail.getCreditMemoItemTotalAmount());
                                }
                            }
                        }
                    }
                }
            }
            agingReportDetail.setTotalCredits(credits);
            totalCredits = totalCredits.add(credits);
        }
    }

    /**
     * Some properties depend on parameters, so let's wait until the parameter service has been set
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        nbrDaysForLastBucket = getParameterService().getParameterValueAsString(CustomerAgingReportDetail.class,  ArConstants.CUSTOMER_INVOICE_AGE);
        // default is 120 days
        cutoffdate91toSYSPRlabel = "91-" + nbrDaysForLastBucket + " days";
        cutoffdateSYSPRplus1orMorelabel = Integer.toString((Integer.parseInt(nbrDaysForLastBucket)) + 1) + "+ days";
    }

    public ContractsGrantsAgingReportService getContractsGrantsAgingReportService() {
        return contractsGrantsAgingReportService;
    }

    public void setContractsGrantsAgingReportService(ContractsGrantsAgingReportService contractsGrantsAgingReportService) {
        this.contractsGrantsAgingReportService = contractsGrantsAgingReportService;
    }

    public ContractsGrantsReportHelperService getContractsGrantsReportHelperService() {
        return contractsGrantsReportHelperService;
    }

    public void setContractsGrantsReportHelperService(ContractsGrantsReportHelperService contractsGrantsReportHelperService) {
        this.contractsGrantsReportHelperService = contractsGrantsReportHelperService;
    }

    public KualiModuleService getModuleService() {
        return moduleService;
    }

    public void setModuleService(KualiModuleService moduleService) {
        this.moduleService = moduleService;
    }

    public CustomerCreditMemoDocumentService getCustomerCreditMemoDocumentService() {
        return customerCreditMemoDocumentService;
    }

    public void setCustomerCreditMemoDocumentService(CustomerCreditMemoDocumentService customerCreditMemoDocumentService) {
        this.customerCreditMemoDocumentService = customerCreditMemoDocumentService;
    }

    public CustomerAgingReportService getCustomerAgingReportService() {
        return customerAgingReportService;
    }

    public void setCustomerAgingReportService(CustomerAgingReportService customerAgingReportService) {
        this.customerAgingReportService = customerAgingReportService;
    }
}
