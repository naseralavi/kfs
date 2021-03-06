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
package org.kuali.kfs.module.bc.document.web.struts;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.kuali.kfs.coa.service.OrganizationService;
import org.kuali.kfs.module.bc.BCConstants;
import org.kuali.kfs.module.bc.BCPropertyConstants;
import org.kuali.kfs.module.bc.businessobject.BudgetConstructionHeader;
import org.kuali.kfs.module.bc.document.service.BudgetDocumentService;
import org.kuali.kfs.module.bc.document.service.SalarySettingService;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.sys.identity.KfsKimAttributes;
import org.kuali.rice.kim.api.services.IdentityManagementService;
import org.kuali.rice.kns.service.BusinessObjectDictionaryService;
import org.kuali.rice.krad.service.PersistenceService;
import org.kuali.rice.krad.util.GlobalVariables;


/**
 * This class...
 */
public class BudgetConstructionSelectionForm extends BudgetExpansionForm {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(BudgetConstructionSelectionForm.class);

    protected BudgetConstructionHeader budgetConstructionHeader;
    protected boolean hideDetails = false;
    protected boolean accountReportsExist;
    protected boolean rootApprover;
    protected boolean sessionInProgressDetected = false;

    public BudgetConstructionSelectionForm() {
        super();
        setBudgetConstructionHeader(new BudgetConstructionHeader());
    }

    /**
     * @see org.kuali.rice.kns.web.struts.form.KualiForm#populate(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public void populate(HttpServletRequest request) {

        super.populate(request);

        final List REFRESH_FIELDS = Collections.unmodifiableList(Arrays.asList(new String[] { "chartOfAccounts", "account", "subAccount", "budgetConstructionAccountReports" }));
        SpringContext.getBean(PersistenceService.class).retrieveReferenceObjects(this.getBudgetConstructionHeader(), REFRESH_FIELDS);

        SpringContext.getBean(BusinessObjectDictionaryService.class).performForceUppercase(this.getBudgetConstructionHeader());
    }

    /**
     * Gets the budgetConstructionHeader attribute.
     *
     * @return Returns the budgetConstructionHeader.
     */
    public BudgetConstructionHeader getBudgetConstructionHeader() {
        return budgetConstructionHeader;
    }

    /**
     * Sets the budgetConstructionHeader attribute value.
     *
     * @param budgetConstructionHeader The budgetConstructionHeader to set.
     */
    public void setBudgetConstructionHeader(BudgetConstructionHeader budgetConstructionHeader) {
        this.budgetConstructionHeader = budgetConstructionHeader;
    }

    /**
     * Gets the hideDetails attribute.
     *
     * @return Returns the hideDetails.
     */
    public boolean isHideDetails() {
        return hideDetails;
    }

    /**
     * Sets the hideDetails attribute value.
     *
     * @param hideDetails The hideDetails to set.
     */
    public void setHideDetails(boolean hideDetails) {
        this.hideDetails = hideDetails;
    }

    /**
     * Gets the accountReportsExist attribute.
     *
     * @return Returns the accountReportsExist.
     */
    public boolean isAccountReportsExist() {
        accountReportsExist = false;

        if (this.budgetConstructionHeader.getAccountNumber() != null && this.budgetConstructionHeader.getChartOfAccountsCode() != null) {
            if (SpringContext.getBean(BudgetDocumentService.class).isAccountReportsExist(this.budgetConstructionHeader.getChartOfAccountsCode(), this.budgetConstructionHeader.getAccountNumber())){
                accountReportsExist = true;
            }
        }
        return accountReportsExist;
    }

    /**
     * Sets the accountReportsExist attribute value.
     *
     * @param accountReportsExist The accountReportsExist to set.
     */
    public void setAccountReportsExist(boolean accountReportsExist) {
        this.accountReportsExist = accountReportsExist;
    }

    /**
     * Gets the salarySettingDisabled attribute.
     *
     * @return Returns the salarySettingDisabled.
     */
    public boolean isSalarySettingDisabled() {
        return SpringContext.getBean(SalarySettingService.class).isSalarySettingDisabled();
    }

    /**
     * Checks whether the user has permission to do payrate import/export
     */
    public boolean getCanPerformPayrateImportExport() {
        String[] rootOrg = SpringContext.getBean(OrganizationService.class).getRootOrganizationCode();
        Map<String,String> qualification = new HashMap<String,String>();
        qualification.put(BCPropertyConstants.ORGANIZATION_CHART_OF_ACCOUNTS_CODE, rootOrg[0]);
        qualification.put(KfsKimAttributes.ORGANIZATION_CODE, rootOrg[1]);

        return SpringContext.getBean(IdentityManagementService.class).isAuthorized(GlobalVariables.getUserSession().getPerson().getPrincipalId(), BCConstants.BUDGET_CONSTRUCTION_NAMESPACE, BCConstants.KimApiConstants.IMPORT_EXPORT_PAYRATE_PERMISSION_NAME, qualification);
    }

    /**
     * Gets the sessionInProgressDetected attribute.
     * @return Returns the sessionInProgressDetected.
     */
    public boolean isSessionInProgressDetected() {
        return sessionInProgressDetected;
    }

    /**
     * Sets the sessionInProgressDetected attribute value.
     * @param sessionInProgressDetected The sessionInProgressDetected to set.
     */
    public void setSessionInProgressDetected(boolean sessionInProgressDetected) {
        this.sessionInProgressDetected = sessionInProgressDetected;
    }

}

