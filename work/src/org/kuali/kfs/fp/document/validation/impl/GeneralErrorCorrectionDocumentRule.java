/*
 * Copyright (c) 2004, 2005 The National Association of College and University Business Officers,
 * Cornell University, Trustees of Indiana University, Michigan State University Board of Trustees,
 * Trustees of San Joaquin Delta College, University of Hawai'i, The Arizona Board of Regents on
 * behalf of the University of Arizona, and the r*smart group.
 * 
 * Licensed under the Educational Community License Version 1.0 (the "License"); By obtaining,
 * using and/or copying this Original Work, you agree that you have read, understand, and will
 * comply with the terms and conditions of the Educational Community License.
 * 
 * You may obtain a copy of the License at:
 * 
 * http://kualiproject.org/license.html
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.kuali.module.financial.rules;

import org.kuali.Constants;
import org.kuali.KeyConstants;
import org.kuali.PropertyConstants;
import org.kuali.core.bo.AccountingLine;
import org.kuali.core.document.TransactionalDocument;
import org.kuali.core.util.GlobalVariables;
import org.kuali.core.util.ObjectUtils;
import org.kuali.core.util.SpringServiceLocator;
import org.kuali.module.chart.bo.ObjectCode;

/**
 * Business rule(s) applicable to 
 * <code>{@link GeneralErrorCorrectionDocument}</code> instances.
 * 
 * @author Kuali Financial Transactions Team (kualidev@oncourse.iu.edu)
 */
public class GeneralErrorCorrectionDocumentRule 
    extends TransactionalDocumentRuleBase 
    implements GeneralErrorCorrectionDocumentRuleConstants {

    /**
     * Convenience method for accessing the most-likely requested
     * security grouping
     *
     * @return String
     */
    protected String getDefaultSecurityGrouping() {
        return GENERAL_ERROR_CORRECTION_SECURITY_GROUPING;
    }

    /**
     * Convenience method for accessing <code>{@link KualiParameterRule}</code> 
     * restricting object type codes.
     * 
     * @return KualiParameterRule
     */
    protected KualiParameterRule getRestrictedObjectTypeCodesRule() {
        return getParameterRule(RESTRICTED_OBJECT_TYPE_CODES);
    }

    /**
     * Convenience method for accessing <code>{@link KualiParameterRule}</code> 
     * restricting object sub type codes.
     * 
     *
     * @return KualiParameterRule
     */
    protected KualiParameterRule getRestrictedObjectSubTypeCodesRule() {
        return getParameterRule(RESTRICTED_OBJECT_SUB_TYPE_CODES);
    }
    
    /**
     * Convenience method for accessing <code>{@link KualiParameterRule}</code> 
     * restricting object type codes under special circumstances.
     * 
     *
     * @return KualiParameterRule
     */
    protected KualiParameterRule getCombinedRestrictedObjectTypeCodesRule() {
        return getParameterRule(COMBINED_RESTRICTED_OBJECT_SUB_TYPE_CODES);
    }
    
    /**
     * Convenience method for accessing <code>{@link KualiParameterRule}</code> 
     * restricting object sub type codes under special circumstances.
     * 
     * @return KualiParameterRule
     */
    protected KualiParameterRule getCombinedRestrictedObjectSubTypeCodesRule() {
        return getParameterRule(COMBINED_RESTRICTED_OBJECT_SUB_TYPE_CODES);
    }

    /**
     * Uses default security grouping specified with the 
     * <code>getDefaultSecurityGrouping()</code> method to obtain a 
     * <code>{@link KualiDocumentRule}</code> instance for the given 
     * <code>parameterName</code>
     * 
     * @param parameterName
     * @return KualiParameterRule
     * @see #getDefaultSecurityGrouping()
     */
    protected KualiParameterRule getParameterRule(String parameterName) {
        return getParameterRule(getDefaultSecurityGrouping(), parameterName);
    }

    /**
     * Obtain access to an instance of <code>{@link KualiParameterRule}</code> 
     * for the given <code>securityGrouping</code> and 
     * <code>parameterName</code>
     *
     * @param securityGrouping
     * @param parameterName
     * @return KualiDocumentRule
     */
    protected KualiParameterRule getParameterRule(String securityGrouping,
                                                  String parameterName) {
        return SpringServiceLocator
            .getKualiConfigurationService()
            .getApplicationParameterRule(securityGrouping, parameterName);
    }

    /**
     * Helper method for business rules concerning 
     * <code>{@link AccountingLine}</code> instances.
     *
     * @param document
     * @param accountingLine
     * @return boolean pass or fail
     */
    private boolean processGenericAccountingLineBusinessRules(TransactionalDocument document,
                                                              AccountingLine accountingLine) {
        boolean retval = true;

        ObjectCode objectCode = accountingLine.getObjectCode();
        if (ObjectUtils.isNull(objectCode)) {
            accountingLine
                .refreshReferenceObject(PropertyConstants.OBJECT_CODE);
        }
        retval &= isObjectTypeAndObjectSubTypeAllowed(objectCode);
        
        return retval;
    }

    /**
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleBase#processCustomAddAccountingLineBusinessRule(TransactionalDocument document, AccountingLine accountingLine)
     */
    public boolean processCustomAddAccountingLineBusinessRules(TransactionalDocument document, 
                                                               AccountingLine accountingLine) {
        boolean retval = true;
        retval &= super
            .processCustomAddAccountingLineBusinessRules(document, 
                                                         accountingLine); 
        if (retval) {
            processGenericAccountingLineBusinessRules(document, accountingLine);
        }
        return retval;
    }
    
    
    /**
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleBase#processCustomReviewAccountingLineBusinessRule(TransactionalDocument document, AccountingLine accountingLine)
     */
    public boolean processCustomReviewAccountingLineBusinessRules(TransactionalDocument document, 
                                                                  AccountingLine accountingLine) {
        boolean retval = true;

        retval &= super
            .processCustomReviewAccountingLineBusinessRules(document, 
                                                            accountingLine);
        if (retval) {
            processGenericAccountingLineBusinessRules(document, accountingLine);
        }
        
        return retval;
    }
    
/**
 */
    protected boolean isObjectTypeAndObjectSubTypeAllowed(ObjectCode code) {
        boolean retval = true;

        retval &= getCombinedRestrictedObjectTypeCodesRule()
            .failsRule(code.getFinancialObjectTypeCode());
        
        if (retval) {
            retval &= getCombinedRestrictedObjectSubTypeCodesRule()
                .failsRule(code.getFinancialObjectTypeCode());
            
        }
        
        if (!retval) {
            // add message
            GlobalVariables.getErrorMap()
                .put(PropertyConstants.FINANCIAL_OBJECT_CODE,
                     KeyConstants.GeneralErrorCorrection
                     .ERROR_DOCUMENT_GENERAL_ERROR_CORRECTION_INVALID_OBJECT_SUB_TYPE_CODE,
                     new String[] {code.getFinancialObjectCode(), 
                                   code.getFinancialObjectSubTypeCode()});
        }

        return retval;
    }

    /**
     * Overrides to perform the universal rule in the super class in addition 
     * to General Error Correction specific rules. This method leverages the 
     * APC for checking restricted object type values.
     *
     * @see org.kuali.core.rule.AccountingLineRule#isObjectTypeAllowed(org.kuali.core.bo.AccountingLine)
     */
    public boolean isObjectTypeAllowed(AccountingLine accountingLine) {
        boolean valid = true;

        valid &= super.isObjectTypeAllowed(accountingLine);

        if (valid) {
            ObjectCode objectCode = accountingLine.getObjectCode();
            if (ObjectUtils.isNull(objectCode)) {
                accountingLine.refreshReferenceObject(PropertyConstants.OBJECT_CODE);
            }

            if (getRestrictedObjectTypeCodesRule()
                .failsRule(objectCode.getFinancialObjectTypeCode())) {
                valid = false;

                // add message
                GlobalVariables.getErrorMap()
                    .put(PropertyConstants.FINANCIAL_OBJECT_CODE,
                         KeyConstants.GeneralErrorCorrection
                         .ERROR_DOCUMENT_GENERAL_ERROR_CORRECTION_INVALID_OBJECT_TYPE_CODE_FOR_OBJECT_CODE,
                         new String[] {objectCode.getFinancialObjectCode(), 
                                       objectCode.getFinancialObjectTypeCode()});
            }
        }

        return valid;
    }

    /**
     * Overrides to perform the universal rule in the super class in addition 
     * to General Error Correction specific rules. This method leverages the 
     * APC for checking restricted object sub type values.
     *
     * @see org.kuali.core.rule.AccountingLineRule#isObjectSubTypeAllowed(org.kuali.core.bo.AccountingLine)
     */
    public boolean isObjectSubTypeAllowed(AccountingLine accountingLine) {
        boolean valid = true;

        valid &= super.isObjectSubTypeAllowed(accountingLine);

        if (valid) {
            ObjectCode objectCode = accountingLine.getObjectCode();
            if (ObjectUtils.isNull(objectCode)) {
                accountingLine
                    .refreshReferenceObject(PropertyConstants.OBJECT_CODE);
            }

            if (getRestrictedObjectSubTypeCodesRule()
                .failsRule(objectCode.getFinancialObjectSubTypeCode())
                || (getCombinedRestrictedObjectTypeCodesRule()
                    .failsRule(objectCode.getFinancialObjectTypeCode())
                    && getCombinedRestrictedObjectSubTypeCodesRule()
                    .failsRule(objectCode.getFinancialObjectTypeCode()))) {
                valid = false;

                // add message
                GlobalVariables.getErrorMap()
                    .put(PropertyConstants.FINANCIAL_OBJECT_CODE,
                         KeyConstants.GeneralErrorCorrection
                         .ERROR_DOCUMENT_GENERAL_ERROR_CORRECTION_INVALID_OBJECT_SUB_TYPE_CODE,
                         new String[] {objectCode.getFinancialObjectCode(), 
                                       objectCode.getFinancialObjectSubTypeCode()});
            }
        }

        return valid;
    }
}
