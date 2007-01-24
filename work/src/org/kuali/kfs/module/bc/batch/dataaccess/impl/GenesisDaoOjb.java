/*
 * Copyright 2006-2007 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.module.budget.dao.ojb;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;
import java.util.Date;
import java.math.*;

import org.kuali.module.budget.dao.*;
import org.kuali.module.budget.bo.*;
import org.kuali.module.budget.document.*;
import org.kuali.module.financial.bo.FiscalYearFunctionControl;
import org.kuali.module.financial.bo.FunctionControlCode;
import org.kuali.core.dao.DocumentHeaderDao;
import org.kuali.core.dao.DocumentDao;
import org.kuali.core.document.DocumentHeader;
import org.kuali.core.document.*;
import org.kuali.Constants;
import org.kuali.Constants.BudgetConstructionConstants;
import org.kuali.Constants.ParameterValues;
import org.kuali.PropertyConstants;
import org.kuali.core.util.*;
import org.kuali.module.gl.bo.Balance;
import org.kuali.module.chart.bo.*;

import org.apache.ojb.broker.query.*;
import org.springmodules.orm.ojb.support.PersistenceBrokerDaoSupport;
import org.apache.log4j.*;

//  we need this so we can create a document
import org.kuali.core.document.Document;
import org.kuali.core.service.DateTimeService;
import org.kuali.core.service.DocumentService;
import org.kuali.core.util.SpringServiceLocator;
import org.kuali.core.util.SpringServiceLocator.*;
import edu.iu.uis.eden.exception.WorkflowException;
import org.kuali.core.exceptions.*;
import org.kuali.core.workflow.service.WorkflowDocumentService;
import org.kuali.core.service.DocumentTypeService;
import org.kuali.core.workflow.service.*;

import org.kuali.core.bo.*;
import org.kuali.core.datadictionary.*;
import org.kuali.module.chart.service.*;


public class GenesisDaoOjb extends PersistenceBrokerDaoSupport 
             implements GenesisDao {
    /*
     *   These routines are written to try to mitigate the performance hit that
     *   comes from using OJB as opposed to JDBC (pass-through SQL).  Pass-through
     *   SQL in Kuali could lead to database-dependencies in the code, and tie Kuali
     *   to a specific RDBMS.
     *   OJB is not really suited for batch, where rows are fetched, inserted, and
     *   updated in big bunches as opposed to a few at a time.
     *   (1)  OJB in "lazy evaluation mode" (the Kuali standard for performance 
     *        reasons) will only return the row from the main table regardless of 
     *        how many "reference descriptor" joins and/or "collection descriptor"
     *        joins there may be in the OJB repository file.  So, if I query table A and
     *        reference table B, my query (in batch) might return 10,000 A rows in
     *        a single call.  None of the matching B fields will be filled in in the
     *        DAO.  If I then try to access a B field in a given instance of the DAO,
     *        Spring will do a query to fetch the relevant B row.  In essence, in batch
     *        I would do a single DB call to get the 10,000 rows of A, and 10,000 DB
     *        calls to fill in the fields from B, one for each row of A.
     *   (2)  This routine tries to do joins in java, in memory, by using what Oracle
     *        calls a "hash join".  If we want to join A and B on a key, we will get
     *        the relevant fields from A and B on separate DB calls (one for A and one
     *        for B), and create a hash map on the join key from the results.  We can
     *        then iterate through either A or B and get the relevant fields from the
     *        other table by employing the hash key.  This should be fast, since hash
     *        tables are designed for fast access.
     *   (3)  We will only store when absolutely necessary to minimize data base access.
     *        So where in Oracle we would do an UPDATE A.. WHERE (EXISTS (SELECT 1 FROM B
     *        WHERE A matches B) or an INSERT A (SELECT ... FROM A, B WHERE A = B), we will 
     *        get all the candidate rows from both A and B, and store individually to do
     *        INSERT or UPDATE.  (There seems to be now way in OJB to store more than
     *        one row at a time.)  This may lead to a lot of database calls that operate
     *        on a single row.  We can only try to minimize this problem.  We can't
     *        get around it.  
     *   This is the impression of the coder.  If anyone has other suggestions, please
     *   let us know.
     *   (One alternative might be to have many different class-desriptor tags in the
     *    OJB repository file representing table A, one for each join to table B.  If
     *    we could override lazy evaluation at the class-descriptor level, we could 
     *    code some batch-specific joins that would get everything we need in one call.
     *    The problem with this is that the A/B descriptions would then be in multiple
     *    tags, and changing them would be labor-intensive and error-prone.  But OJB
     *    repositories allow headers, so we could get around this by using an entity to 
     *    describe the A fields.  The entity would be in one place, so changes to the A
     *    fields could also be made in one place.  The foreignkey field-ref tag B fields
     *    are repeated in every description anyway, so things aren't always in one place
     *    to begin with.)
     */

    private FiscalYearFunctionControl fiscalYearFunctionControl;
    private FunctionControlCode functionControlCode;
    
    /*  turn on the logger for the persistence broker */
    private static Logger LOG = org.apache.log4j.Logger.getLogger(GenesisDaoOjb.class);


    // @@TODO maybe it isn't worth moving these home-coming queen values somewhere else
    //        maybe we don't need the second one at all
    public final static Long DEFAULT_VERSION_NUMBER = new Long(1);
    public final static Integer MAXIMUM_ORGANIZATION_TREE_DEPTH = new Integer(1000);

    /*
     *  this is old stuff which we may not use--we'll see
     */
    
    /*  these constants should be in PropertyConstants */
    public final static String BUDGET_FLAG_PROPERTY_NAME = "financialSystemFunctionControlCode";
    public final static String BUDGET_FLAG_VALUE = "financialSystemFunctionActiveIndicator";
    public final static String BUDGET_CZAR_CHART = "UA";
    public final static String FINANCIAL_CHART_PROPERTY = "chartOfAccountsCode"; 
    public final static String BUDGET_CZAR_ORG = "BUDU";
    public final static String ORG_CODE_PROPERTY = "organizationCode";
    public final static String FISCAL_OFFICER_ID_PROPERTY = "accountFiscalOfficerSystemIdentifier";
    public final static String ACCOUNT_CLOSED_INDICATOR_PROPERTY = "accountClosedIndicator";

    private DateTimeService dateTimeService; 
    private DocumentService documentService; 
    private WorkflowDocumentService workflowDocumentService;
    private DocumentDao documentDao;
    
    public final Map<String,String> getBudgetConstructionControlFlags (Integer universityFiscalYear)
    {
        /*  return the flag names and the values for all the BC flags for the fiscal year */
        
        /*  the key to the map returned will be the name of the flag
         *  the entry will be the flag's value 
         */
        Map<String, String> controlFlags = new HashMap();
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(Constants.UNIVERSITY_FISCAL_YEAR_PROPERTY_NAME,
                                         universityFiscalYear); 
        String[] queryAttr = {BUDGET_FLAG_PROPERTY_NAME,BUDGET_FLAG_VALUE};
        ReportQueryByCriteria queryID = 
            new ReportQueryByCriteria(FiscalYearFunctionControl.class, 
                                        queryAttr, criteriaID);
        Iterator Results = 
            getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(queryID);
        /* fill in the map */
        while (Results.hasNext())
        {
          String[] mapValues = (String []) ((Object []) Results.next());
          controlFlags.put(mapValues[0],mapValues[1]);
        };
        return controlFlags;        
    }
    
    public boolean getBudgetConstructionControlFlag(Integer universityFiscalYear, String FlagID)
    {
        /*  return true if a flag is on, false if it is not */
        String Result;
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(Constants.UNIVERSITY_FISCAL_YEAR_PROPERTY_NAME,
                                         universityFiscalYear);
        criteriaID.addEqualTo(BUDGET_FLAG_PROPERTY_NAME,FlagID);
        String[] queryAttr = {BUDGET_FLAG_VALUE};
        ReportQueryByCriteria queryID = 
            new ReportQueryByCriteria(FiscalYearFunctionControl.class, queryAttr, criteriaID, true);
        Iterator Results = getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(
                             queryID);
        // TODO@ we need to create an exception, put a try around this block, and log errors
        Result = (String) ((Object[]) Results.next()) [0];
        return (Result == Constants.ParameterValues.YES);
            
    }
    
    public final String getBudgetConstructionInitiatorID()
    {
        //@TODO: The constants and field names below should come from constants files
        //  the chart and department should be budget construction constants
        //  the others should be kuali constants
        final String DEFAULT_ID = "666-666-66";
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(FINANCIAL_CHART_PROPERTY, BUDGET_CZAR_CHART);
        criteriaID.addEqualTo(ORG_CODE_PROPERTY,BUDGET_CZAR_ORG);
        criteriaID.addColumnEqualTo(ACCOUNT_CLOSED_INDICATOR_PROPERTY,
                Constants.ParameterValues.NO);
        String[] queryAttr = {FISCAL_OFFICER_ID_PROPERTY};
        ReportQueryByCriteria queryID = 
            new ReportQueryByCriteria(Account.class, queryAttr, criteriaID, true);
        Iterator Results = getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(queryID);
        if (!Results.hasNext())
        {
            return DEFAULT_ID;
        }
        else
        {  
           String retID = (String) ((Object[]) Results.next())[0];  
           return retID;
        }
    }
    
    /*
     *   these routines are used to create and set the control flags for budget
     *   construction
     */
    
    public void setControlFlagsAtTheStartOfGenesis(Integer BaseYear)
    {
        Integer RequestYear = BaseYear+1;
        //
        // first we have to eliminate anything for the new year that's there now
        getPersistenceBrokerTemplate().clearCache();
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,RequestYear);
        QueryByCriteria queryID = new QueryByCriteria(FiscalYearFunctionControl.class,
                                      criteriaID);
        getPersistenceBrokerTemplate().deleteByQuery(queryID);
        getPersistenceBrokerTemplate().clearCache();
       // 
       //  the default values (except for the BUDGET_CONSTRUCTION_GENESIS_RUNNING flag)
       //  come from the function control code table
       FiscalYearFunctionControl SLF;
       criteriaID = QueryByCriteria.CRITERIA_SELECT_ALL;
       String[] attrQ = {PropertyConstants.FINANCIAL_SYSTEM_FUNCTION_CONTROL_CODE,
                         PropertyConstants.FINANCIAL_SYSTEM_FUNCTION_DEFAULT_INDICATOR};
       ReportQueryByCriteria rptQueryID = new ReportQueryByCriteria(FunctionControlCode.class,
                                       attrQ,criteriaID);
       Integer sqlFunctionControlCode     = 0;
       Integer sqlFunctionActiveIndicator = 1;
       // run the query
       Iterator Results = 
            getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(rptQueryID);
       while (Results.hasNext())
       {
           SLF = new FiscalYearFunctionControl();
           Object[] resultFields = (Object[]) Results.next();
           String flagTag     = (String) resultFields[sqlFunctionControlCode];
 //          String flagDefault = (String) resultFields[sqlFunctionActiveIndicator];
 //  apparently OJB is smart enough to bring this in as a boolean
           boolean flagDefault = (Boolean) resultFields[sqlFunctionActiveIndicator];
           SLF.setUniversityFiscalYear(RequestYear);
           LOG.debug("\nfiscal year has been set");
           SLF.setFinancialSystemFunctionControlCode(flagTag);
           LOG.debug("\nfunction code has been set");
           SLF.setVersionNumber(DEFAULT_VERSION_NUMBER);
           LOG.debug(String.format("\nversion number set to %d",
                                  SLF.getVersionNumber()));
           if (flagTag.equals( 
               BudgetConstructionConstants.BUDGET_CONSTRUCTION_GENESIS_RUNNING))
           {
               SLF.setFinancialSystemFunctionActiveIndicator(true);
           }
           else
           {
//               SLF.setFinancialSystemFunctionActiveIndicator(
//                       ((flagDefault == Constants.ParameterValues.YES)? true : false));
                 SLF.setFinancialSystemFunctionActiveIndicator(flagDefault);
           }
           LOG.debug("\nabout to store the result");
           getPersistenceBrokerTemplate().store(SLF);
       }
    }
    
    public void setControlFlagsAtTheEndOfGenesis(Integer BaseYear)
    {
        Integer RequestYear = BaseYear + 1;
        resetExistingFlags(BaseYear,
                           BudgetConstructionConstants.CURRENT_FSCL_YR_CTRL_FLAGS);
        resetExistingFlags(RequestYear,
                           BudgetConstructionConstants.NEXT_FSCL_YR_CTRL_FLAGS_AFTER_GENESIS);
    }
    
    //  this method just reads the existing flags and changes their values
    //  based on the configureation constants
    public void resetExistingFlags (Integer Year,
                                    HashMap<String,String> configValues)
    {
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,Year);
        QueryByCriteria queryID = 
            new QueryByCriteria(FiscalYearFunctionControl.class,criteriaID);
        Iterator Results = 
            getPersistenceBrokerTemplate().getIteratorByQuery(queryID);
        while (Results.hasNext())
        {
          LOG.debug("\nbefore call to next() and cast");  
          FiscalYearFunctionControl SLF = (FiscalYearFunctionControl) Results.next();
          LOG.debug("\nafter call to next()");
          String mapKey = SLF.getFinancialSystemFunctionControlCode();
          String newValue = configValues.get(mapKey);
          SLF.setFinancialSystemFunctionActiveIndicator(
                  ((newValue.equals(ParameterValues.YES))? true : false));
          LOG.debug("\nabout to store the result");
          getPersistenceBrokerTemplate().store(SLF);
          LOG.debug("\nafter store");
        }
    }
    
    /*
     *    @@TODO:
     *    these are some test routines which should be removed in production
     */
    public void testBCDocumentCreation(Integer BaseYear)
    {
        Integer RequestYear = BaseYear+1;
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,BaseYear);
        criteriaID.addEqualTo(PropertyConstants.BALANCE_TYPE_CODE,
                              Constants.BALANCE_TYPE_BASE_BUDGET);
        criteriaID.addLessThan("ROWNUM",new Integer(4));
        String[] queryAttr = {PropertyConstants.CHART_OF_ACCOUNTS_CODE,
                PropertyConstants.ACCOUNT_NUMBER,
                PropertyConstants.SUB_ACCOUNT_NUMBER,
                PropertyConstants.OBJECT_CODE,
                PropertyConstants.SUB_OBJECT_CODE,
                PropertyConstants.BALANCE_TYPE_CODE,
                PropertyConstants.OBJECT_TYPE_CODE,
                PropertyConstants.ACCOUNT_LINE_ANNUAL_BALANCE_AMOUNT,
                PropertyConstants.BEGINNING_BALANCE_LINE_AMOUNT};
        ReportQueryByCriteria queryID = 
        new ReportQueryByCriteria(Balance.class, queryAttr, criteriaID, true);
        LOG.info("\nGL Query started: "+String.format("%tT",new Date()));
        Iterator Results = 
            getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(queryID);
        LOG.info("\nGL Query finished: "+String.format("%tT",new Date()));
        while (Results.hasNext())
        {
          Object[] ReturnList = (Object []) Results.next();
          String HeaderTestKey = buildHeaderTestKeyFromSQLResults(ReturnList);
          bCHdrFromGL.put(HeaderTestKey, newBCHdrBusinessObject(RequestYear, ReturnList));
        }
        LOG.info(String.format("\nNumber of BC headers: %d",bCHdrFromGL.size()));
        createNewBCDocuments(BaseYear);
    }
    
    /*
     *   here are the routines which freeze accounting at the beginning of
     *   budget construction (so updates can be done in parallel, or updates
     *   for the budget year only can be done without affecting the current
     *   chart of accounts).
     *   These routines only run once, at genesis.
     */
    
    //   public routines
    
    public void createChartForNextBudgetCycle()
    {
      // first we have to remove what's there
      // (the documentation says deleteByQuery (1) ignores object references and (2) does
      //  not synchronize the cache.  so, we clear the cache before and after.)
        getPersistenceBrokerTemplate().clearCache();
        Criteria criteriaID = QueryByCriteria.CRITERIA_SELECT_ALL;
        QueryByCriteria killAcctQuery = 
            new QueryByCriteria(BudgetConstructionAccountReports.class);
        killAcctQuery.setCriteria(criteriaID);
        getPersistenceBrokerTemplate().deleteByQuery(killAcctQuery);
        QueryByCriteria killOrgQuery =
            new QueryByCriteria(BudgetConstructionOrganizationReports.class);
        killOrgQuery.setCriteria(criteriaID);
        getPersistenceBrokerTemplate().deleteByQuery(killOrgQuery);
        getPersistenceBrokerTemplate().clearCache();
      // build the account table
        buildNewAccountReportsTo();
      // build the organization table  
        buildNewOrganizationReportsTo();
    }
    
    //  private working methods for the BC chart update
    
    private void buildNewAccountReportsTo()
    {
        
        //  All active accounts are loaded into the budget accounting table
        
        Integer sqlChartOfAccountsCode = 0;
        Integer sqlAccountNumber = 1;
        Integer sqlReportsToChartofAccountsCode = 2;
        Integer sqlOrganizationCode = 3;
        
        Long accountsAdded = new Long(0);
        
        Criteria criteriaID = new Criteria();
        criteriaID.addNotEqualTo(PropertyConstants.ACCOUNT_CLOSED_INDICATOR,
                              Constants.ParameterValues.YES);
        String[] queryAttr = {PropertyConstants.CHART_OF_ACCOUNTS_CODE,
                              PropertyConstants.ACCOUNT_NUMBER,
                              PropertyConstants.CHART_OF_ACCOUNTS_CODE,
                              PropertyConstants.ORGANIZATION_CODE};
       ReportQueryByCriteria queryID = 
       new ReportQueryByCriteria(Account.class, queryAttr, criteriaID, true);
       Iterator Results = getPersistenceBrokerTemplate().getIteratorByQuery(queryID);
       while (Results.hasNext())
       {
           Object[] ReturnList = (Object[]) Results.next();
           // just save this stuff, one at a time
           // it isn't needed for anything else
           BudgetConstructionAccountReports acctRpts = 
               new BudgetConstructionAccountReports();
           acctRpts.setChartOfAccountsCode((String) ReturnList[sqlChartOfAccountsCode]);
           acctRpts.setAccountNumber((String) ReturnList[sqlAccountNumber]);
           acctRpts.setReportsToChartOfAccountsCode((String)
                    ReturnList[sqlReportsToChartofAccountsCode]);
           acctRpts.setReportsToOrganizationCode((String)
                    ReturnList[sqlOrganizationCode]);
           acctRpts.setVersionNumber(DEFAULT_VERSION_NUMBER);
           getPersistenceBrokerTemplate().store(acctRpts);
           accountsAdded = accountsAdded + 1;
       }
       LOG.info(String.format("Account reporting lines added to budget construction %d",
                accountsAdded));
    }
    
    private void buildNewOrganizationReportsTo()
    {
      
        //  all active organizations are loaded into the budget construction
        //  organization table
        
        Integer sqlChartOfAccountsCode          = 0;
        Integer sqlOrganizationCode             = 1;
        Integer sqlReportsToChartOfAccountsCode = 2;
        Integer sqlReportsToOrganizationCode    = 3;
        Integer sqlResponsibilityCenterCode     = 4;

        Long organizationsAdded = new Long(0);
        
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.ORGANIZATION_ACTIVE_INDICATOR,
                              Constants.ParameterValues.YES);
        String[] queryAttr = {PropertyConstants.CHART_OF_ACCOUNTS_CODE,
                              PropertyConstants.ORGANIZATION_CODE,
                              PropertyConstants.REPORTS_TO_CHART_OF_ACCOUNTS_CODE,
                              PropertyConstants.REPORTS_TO_ORGANIZATION_CODE,
                              PropertyConstants.RESPONSIBILITY_CENTER_CODE};
       ReportQueryByCriteria queryID = 
       new ReportQueryByCriteria(Org.class, queryAttr, criteriaID, true);
       Iterator Results = getPersistenceBrokerTemplate().getIteratorByQuery(queryID);
       while (Results.hasNext())
       {
           Object[] ReturnList = (Object[]) Results.next();
           // just save this stuff, one at a time
           // it isn't needed for anything else
           BudgetConstructionOrganizationReports orgRpts = 
               new BudgetConstructionOrganizationReports();
           orgRpts.setChartOfAccountsCode((String) ReturnList[sqlChartOfAccountsCode]);
           orgRpts.setOrganizationCode((String) ReturnList[sqlOrganizationCode]);
           orgRpts.setReportsToChartOfAccountsCode((String)
                    ReturnList[sqlReportsToChartOfAccountsCode]);
           orgRpts.setReportsToOrganizationCode((String)
                    ReturnList[sqlReportsToOrganizationCode]);
           orgRpts.setResponsibilityCenterCode((String)
                    ReturnList[sqlResponsibilityCenterCode]);
           orgRpts.setVersionNumber(DEFAULT_VERSION_NUMBER);
           getPersistenceBrokerTemplate().store(orgRpts);
           organizationsAdded = organizationsAdded + 1;
       }
       LOG.info(String.format("Organization reporting lines added to budget construction %d",
                organizationsAdded));
    }
    
    /*
     *   these are the routines that build the security organization hierarchy
     *   -- they run every time the budget construction update process runs
     *   -- they are designed to pick up any changes made to the BC account and BC
     *      organization tables
     *   -- based on changes, they will adjust the security levels of accounts in the BC
     *      header.  for a header at the level of an organization that is no longer valid,
     *      the level will return to the account manager level.  for a header at the level
     *      of an organization that has changed its location in the hierarchy, the new
     *      level will be added to the header
     *   -- this process only affects accounts in the budget construction pending
     *      general ledger, and it is assumed that all updates to the PBGL have been
     *      finished when this process runs.       
     */

    private HashMap<String,BudgetConstructionAccountReports> acctRptsToMap =
        new HashMap(BudgetConstructionConstants.ESTIMATED_NUMBER_OF_FINANCIAL_ACCOUNTS);
    private HashMap<String,BudgetConstructionOrganizationReports> orgRptsToMap =
        new HashMap(BudgetConstructionConstants.ESTIMATED_NUMBER_OF_ACTIVE_ORGANIZATIONS);
    private HashMap<String,BudgetConstructionAccountOrganizationHierarchy> acctOrgHierMap =
        new HashMap(BudgetConstructionConstants.BUDGETED_ACCOUNTS_TIMES_AVERAGE_REPORTING_TREE_SIZE);
    private BudgetConstructionHeader budgetConstructionHeader; 
    
    private Integer nHeadersBackToZero      = 0;
    private Integer nHeadersSwitchingLevels = 0;
        

    // public method
    
    public void rebuildOrganizationHierarchy(Integer BaseYear)
    {
        // ********
        // this routine REQUIRES that pending GL is complete
        // we only build a hierarchy for accounts that exist in the GL
        // ********
        
        Integer RequestYear = BaseYear + 1;
        
        //
        // first we have to clear out what's there for the coming fiscal year
        // again, we clear the cache after doing a deleteByQuery
        getPersistenceBrokerTemplate().clearCache();
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,RequestYear);
        QueryByCriteria killOrgHierQuery = 
            new QueryByCriteria(BudgetConstructionAccountOrganizationHierarchy.class,
                                criteriaID);
        killOrgHierQuery.setCriteria(criteriaID);
        getPersistenceBrokerTemplate().deleteByQuery(killOrgHierQuery);
        getPersistenceBrokerTemplate().clearCache();
        //
        // read the entire account reports to table, and build a hash map for the
        // join with the PBGL accounts
        readAcctReportsTo();
        // read the entire organization reports to table, and build a hash map for
        // getting the organization tree
        readOrgReportsTo();
        //
        //  we query the budget construction header and loop through the results
        //  we build a hierarchy for every account we find
        //  we reset level of any account which no longer exists in the hierarchy
        criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,RequestYear);
        QueryByCriteria queryID = new QueryByCriteria(BudgetConstructionHeader.class,
                                      criteriaID);
        Iterator Results = getPersistenceBrokerTemplate().getIteratorByQuery(queryID);
        while (Results.hasNext())
        {
           BudgetConstructionHeader extantBCHdr = (BudgetConstructionHeader) Results.next();
           buildAcctOrgHierFromAcctRpts(acctRptsToMap.get(
                   getAcctRptsToKeyFromBCHdr(extantBCHdr)), RequestYear);
           updateBudgetConstructionHeaderAsNeeded(extantBCHdr);
        }
    }
    
    //  private utility methods

    private void buildAcctOrgHierFromAcctRpts(BudgetConstructionAccountReports acctRpts,
            Integer RequestYear)
    {
        // part of the key of the budget construction header is a sub account
        // so, our algorithm could visit the same account more than once
        // if the hierarchy for this account is already built, we skip this routine
        String inKey = getOrgHierarchyKeyFromAcctRpts(acctRpts);
        if (acctOrgHierMap.get(inKey) != null)
        {
            return;
        }
        Integer orgLevel = 0;
        // the organization the account directly reports to is at level 0
        BudgetConstructionAccountOrganizationHierarchy acctOrgHier;
        acctOrgHier =
            new BudgetConstructionAccountOrganizationHierarchy();
        acctOrgHier.setUniversityFiscalYear(RequestYear);
        acctOrgHier.setChartOfAccountsCode(acctRpts.getChartOfAccountsCode());
        acctOrgHier.setAccountNumber(acctRpts.getAccountNumber());
        acctOrgHier.setOrganizationLevelCode(orgLevel);
        acctOrgHier.setVersionNumber(DEFAULT_VERSION_NUMBER);
        acctOrgHier.setOrganizationChartOfAccountsCode(acctRpts.getReportsToChartOfAccountsCode());
        acctOrgHier.setOrganizationCode(acctRpts.getReportsToOrganizationCode());
        // save the new row
        getPersistenceBrokerTemplate().store(acctOrgHier);
        // save the new row in a hash map so we can merge with the budget header
        String mapKey = getOrgHierarchyKey(acctOrgHier);
        acctOrgHierMap.put(mapKey,acctOrgHier);
        // now we have to loop to assign the hierarchy
        // (especially before testing, we need to be on the look out for infinite
        //@@TODO:
        //  loops.  assertions are verboten, so we'll just code a high value for
        //  the level limit, instead of using a potentially infinite while loop)
        while (orgLevel < MAXIMUM_ORGANIZATION_TREE_DEPTH)
        {
            // find the current organization in the BC organization reports to table
            String orgKey = getOrgRptsToKeyFromAcctOrgHier(acctOrgHier);
            if (noNewMapEntryNeeded(orgRptsToMap.get(orgKey)))
            {
                // get out if we have found the root of the reporting tree
                break;
            }
            orgLevel = orgLevel+1;
            BudgetConstructionOrganizationReports orgRpts =
                orgRptsToMap.get(orgKey);
            acctOrgHier = 
                new BudgetConstructionAccountOrganizationHierarchy();
            acctOrgHier.setUniversityFiscalYear(RequestYear);
            acctOrgHier.setChartOfAccountsCode(acctRpts.getChartOfAccountsCode());
            acctOrgHier.setAccountNumber(acctRpts.getAccountNumber());
            acctOrgHier.setOrganizationLevelCode(orgLevel);
            acctOrgHier.setVersionNumber(DEFAULT_VERSION_NUMBER);
            acctOrgHier.setOrganizationChartOfAccountsCode(
                        orgRpts.getReportsToChartOfAccountsCode());
            acctOrgHier.setOrganizationCode(orgRpts.getReportsToOrganizationCode());
            // save the new row
            getPersistenceBrokerTemplate().store(acctOrgHier);
            // save the new row in a hash map so we can merge with the budget header
            mapKey = getOrgHierarchyKey(acctOrgHier);
            acctOrgHierMap.put(mapKey,acctOrgHier);
        }
        if (orgLevel >= MAXIMUM_ORGANIZATION_TREE_DEPTH)
        {
            LOG.warn(String.format("%s/%s reports to more than %d organizations",
                     acctRpts.getChartOfAccountsCode(),
                     acctRpts.getAccountNumber(),
                     MAXIMUM_ORGANIZATION_TREE_DEPTH));
        }
    }
    
    private String getAcctRptsToKey(
            BudgetConstructionAccountReports acctRpts)
    {
        String TestKey = new String();
        TestKey = acctRpts.getChartOfAccountsCode()+
                  acctRpts.getAccountNumber();
        return TestKey;
    }
    
    private String getAcctRptsToKeyFromBCHdr(
                   BudgetConstructionHeader bCHdr)
    {
        String TestKey = new String();
        TestKey = bCHdr.getChartOfAccountsCode()+
                  bCHdr.getAccountNumber();
        return TestKey;
    }
    
    private String getOrgHierarchyKey(
            BudgetConstructionAccountOrganizationHierarchy orgHier)
    {
        String TestKey = new String();
        TestKey = orgHier.getChartOfAccountsCode()+
                  orgHier.getAccountNumber()+
                  orgHier.getOrganizationChartOfAccountsCode()+
                  orgHier.getOrganizationCode();
        return TestKey;
    }
    
    private String getOrgHierarchyKeyFromAcctRpts(
            BudgetConstructionAccountReports acctRpts)
    {
        String TestKey = new String();
        TestKey = acctRpts.getChartOfAccountsCode()+
                  acctRpts.getAccountNumber()+
                  acctRpts.getReportsToChartOfAccountsCode()+
                  acctRpts.getReportsToOrganizationCode();
        return TestKey;
    }
 
    private String getOrgHierarchyKeyFromBCHeader(
                   BudgetConstructionHeader bCHdr)
    {
        String TestKey = new String();
        TestKey = bCHdr.getChartOfAccountsCode()+
                  bCHdr.getAccountNumber()+
                  bCHdr.getOrganizationLevelChartOfAccountsCode()+
                  bCHdr.getOrganizationLevelOrganizationCode();
        return TestKey;
    }
    
    private String getOrgRptsToKey(
            BudgetConstructionOrganizationReports orgRpts)
    {
        String TestKey = new String();
        TestKey = orgRpts.getChartOfAccountsCode()+
                  orgRpts.getOrganizationCode();
        return TestKey;
    }
    
    private String getOrgRptsToKeyFromAcctOrgHier(
            BudgetConstructionAccountOrganizationHierarchy acctOrgHier)
    {
        String TestKey = new String();
        TestKey = acctOrgHier.getOrganizationChartOfAccountsCode()+
                  acctOrgHier.getOrganizationCode();
        return TestKey;
    }
    
    private boolean noNewMapEntryNeeded(BudgetConstructionOrganizationReports orgRpts)
    {
        // no new entry is needed if either the chart or the organization 
        // which this organization reports to is null
        // or if the organization reports to itself
        String rptsToChart = orgRpts.getReportsToChartOfAccountsCode();
        if (rptsToChart == null)
        {
            return true;
        }
        String rptsToOrg = orgRpts.getReportsToOrganizationCode();
        if (rptsToOrg == null) 
        {
            return true;
        }
        String thisChart = orgRpts.getChartOfAccountsCode();
        String thisOrg   = orgRpts.getOrganizationCode();
        return ((thisChart == rptsToChart)&&(thisOrg == rptsToOrg));
    }
    
    private void readAcctReportsTo()
    {
        // we will use a report query, to bypass the "persistence" bureaucracy
        // we will use the OJB class as a convenient container object in the hashmap
        Integer sqlChartOfAccountsCode          = 0;
        Integer sqlAccountNumber                = 1;
        Integer sqlReportsToChartofAccountsCode = 2;
        Integer sqlOrganizationCode             = 3;
        Criteria criteriaID = ReportQueryByCriteria.CRITERIA_SELECT_ALL;
        String[] queryAttr = {PropertyConstants.CHART_OF_ACCOUNTS_CODE,
                              PropertyConstants.ACCOUNT_NUMBER,
                              PropertyConstants.REPORTS_TO_CHART_OF_ACCOUNTS_CODE,
                              PropertyConstants.REPORTS_TO_ORGANIZATION_CODE};
        ReportQueryByCriteria queryID = 
            new ReportQueryByCriteria(BudgetConstructionAccountReports.class,
                                      queryAttr,criteriaID);
        Iterator Results = 
            getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(queryID);
        while (Results.hasNext())
        {
            Object[] ReturnList = (Object[]) Results.next();
            BudgetConstructionAccountReports acctRpts = 
                new BudgetConstructionAccountReports();
            acctRpts.setChartOfAccountsCode((String) ReturnList[sqlChartOfAccountsCode]);
            acctRpts.setAccountNumber((String) ReturnList[sqlAccountNumber]);
            acctRpts.setReportsToChartOfAccountsCode((String)
                     ReturnList[sqlReportsToChartofAccountsCode]);
            acctRpts.setReportsToOrganizationCode((String)
                     ReturnList[sqlOrganizationCode]);
            String TestKey = getAcctRptsToKey(acctRpts);
            acctRptsToMap.put(TestKey,acctRpts);
        }
       LOG.info("\nAccount Reports To for Organization Hierarchy:"); 
       LOG.info(String.format("\nNumber of account-reports-to rows: %d",
                acctRptsToMap.size()));        
   }
    
   private void readOrgReportsTo()
   {
       // we will use a report query, to bypass the "persistence" bureaucracy
       // we will use the OJB class as a convenient container object in the hashmap
       Integer sqlChartOfAccountsCode          = 0;
       Integer sqlOrganizationCode             = 1;
       Integer sqlReportsToChartofAccountsCode = 2;
       Integer sqlReportsToOrganizationCode    = 3;
       Criteria criteriaID = ReportQueryByCriteria.CRITERIA_SELECT_ALL;
       String[] queryAttr = {PropertyConstants.CHART_OF_ACCOUNTS_CODE,
                             PropertyConstants.ORGANIZATION_CODE,
                             PropertyConstants.REPORTS_TO_CHART_OF_ACCOUNTS_CODE,
                             PropertyConstants.REPORTS_TO_ORGANIZATION_CODE};
       ReportQueryByCriteria queryID = 
           new ReportQueryByCriteria(BudgetConstructionOrganizationReports.class,
                                     queryAttr,criteriaID);
       Iterator Results = 
           getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(queryID);
       while (Results.hasNext())
       {
           Object[] ReturnList = (Object[]) Results.next();
           BudgetConstructionOrganizationReports orgRpts = 
               new BudgetConstructionOrganizationReports();
           orgRpts.setChartOfAccountsCode((String) ReturnList[sqlChartOfAccountsCode]);
           orgRpts.setOrganizationCode((String) ReturnList[sqlOrganizationCode]);
           orgRpts.setReportsToChartOfAccountsCode((String)
                    ReturnList[sqlReportsToChartofAccountsCode]);
           orgRpts.setReportsToOrganizationCode((String)
                    ReturnList[sqlReportsToOrganizationCode]);
           String TestKey = getOrgRptsToKey(orgRpts);
           orgRptsToMap.put(TestKey,orgRpts);
       }
      LOG.info("\nOrganization Reports To for Organization Hierarchy:"); 
      LOG.info(String.format("\nNumber of organization-reports-to rows: %d",
               orgRptsToMap.size()));        
   }
   
   private void updateBudgetConstructionHeaderAsNeeded(
                BudgetConstructionHeader bCHdr)
   {
      // we will only update if the level has changed or if the organization at 
      // at the level indicated has disappeared
      String mapKey = getOrgHierarchyKeyFromBCHeader(bCHdr);
      BudgetConstructionAccountOrganizationHierarchy acctOrgHier =
          acctOrgHierMap.get(mapKey);
      if (acctOrgHier == null)
      {
          // we have to return to the zero level and the organization to which
          // the account directly reports
          nHeadersBackToZero = nHeadersBackToZero+1;
          String acctKey = getAcctRptsToKeyFromBCHdr(bCHdr);
          BudgetConstructionAccountReports acctRpts = acctRptsToMap.get(acctKey);
          bCHdr.setOrganizationLevelChartOfAccountsCode(
                  acctRpts.getReportsToChartOfAccountsCode());
          bCHdr.setOrganizationLevelOrganizationCode(
                  acctRpts.getReportsToOrganizationCode());
          bCHdr.setOrganizationLevelCode(new Integer(0));
          getPersistenceBrokerTemplate().store(bCHdr);
          return;
      }
     Integer levelFromHierarchy = acctOrgHier.getOrganizationLevelCode();
     Integer levelFromHeader    = bCHdr.getOrganizationLevelCode();
     if (levelFromHierarchy != levelFromHeader)
     {
         bCHdr.setOrganizationLevelCode(levelFromHierarchy);
         getPersistenceBrokerTemplate().store(bCHdr);
         nHeadersSwitchingLevels = nHeadersSwitchingLevels+1;
     }
   }
   
   
   
    
    /*
     *  here are the routines we will use for updating budget construction GL
     *  
     */
    // maps (hash maps) to return the results of the GL call
    // --pBGLFromGL contains all the rows returned, stuffed into an object that can be 
    //   saved to the pending budget construction general ledger
    // --bCHdrFromGL contains one entry for each potentially new key for the budget
    //   construction header table.
    private HashMap<String,PendingBudgetConstructionGeneralLedger>  pBGLFromGL =
        new HashMap(BudgetConstructionConstants.ESTIMATED_PENDING_GENERAL_LEDGER_ROWS);
    private HashMap<String,BudgetConstructionHeader> bCHdrFromGL =
            new HashMap(BudgetConstructionConstants.ESTIMATED_BUDGET_CONSTRUCTION_DOCUMENT_COUNT);
    private HashMap<String,String> CurrentPBGLDocNumbers = 
            new HashMap(BudgetConstructionConstants.ESTIMATED_BUDGET_CONSTRUCTION_DOCUMENT_COUNT);
    // these are the indexes for each of the fields returned in the select list
    // of the SQL statement
    private Integer sqlChartOfAccountsCode = 0;
    private Integer sqlAccountNumber = 1;
    private Integer sqlSubAccountNumber = 2;
    private Integer sqlObjectCode = 3;
    private Integer sqlSubObjectCode = 4;
    private Integer sqlBalanceTypeCode = 5;
    private Integer sqlObjectTypeCode = 6;
    private Integer sqlAccountLineAnnualBalanceAmount = 7;
    private Integer sqlBeginningBalanceLineAmount = 8;
    
    private Integer nGLHeadersAdded  = new Integer(0);
    private Integer nGLRowsAdded     = new Integer(0);
    private Integer nGLRowsUpdated   = new Integer(0);
    private Integer nCurrentPBGLRows = new Integer(0);
    private Integer nGLBBRowsZeroNet = new Integer(0);
    private Integer nGLBBRowsRead    = new Integer(0);
    private Integer nGLBBKeysRead    = new Integer(0);
    
    // public methods
    
    public void clearHangingBCLocks (Integer BaseYear)
    {
        // this routine cleans out any locks that might remain from people leaving
        // the application abnormally (for example, Fire! Fire!).  it assumes that
        // people are shut out of the application during a batch run, and that all
        // work prior to the batch run has either been committed or lost.
        BudgetConstructionHeader lockedDocuments;
        //
        Integer RequestYear = BaseYear+1;
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                RequestYear);
        Criteria lockID = new Criteria();
        Criteria tranLockID = new Criteria();
        //@@TODO:  add these to the PropertyConstants or at least to 
        //         BudgetConstructionConstants?
        if (BudgetConstructionConstants.DEFAULT_BUDGET_HEADER_LOCK_IDS == null)
        { 
            //  make sure that a NULL test is used in case = NULL is not supported
            //  by the database
            lockID.addNotNull("budgetLockUserIdentifier");
            tranLockID.addNotNull("budgetTransactionLockUserIdentifier");
        }
        else
        {
            lockID.addNotEqualTo("budgetLockUserIdentifier",
                          BudgetConstructionConstants.DEFAULT_BUDGET_HEADER_LOCK_IDS);
            tranLockID.addNotEqualTo("budgetTransactionLockUserIdentifier",
                    BudgetConstructionConstants.DEFAULT_BUDGET_HEADER_LOCK_IDS);
        };
        lockID.addOrCriteria(tranLockID);
        criteriaID.addAndCriteria(lockID);
        //
        QueryByCriteria queryID = 
            new QueryByCriteria(BudgetConstructionHeader.class, criteriaID);
        Iterator Results = getPersistenceBrokerTemplate().getIteratorByQuery(queryID);
        //  now just loop through and change the locks
        while (Results.hasNext())
        {
            lockedDocuments = (BudgetConstructionHeader) Results.next();
            lockedDocuments.setBudgetLockUserIdentifier(BudgetConstructionConstants.DEFAULT_BUDGET_HEADER_LOCK_IDS);
            lockedDocuments.setBudgetTransactionLockUserIdentifier(BudgetConstructionConstants.DEFAULT_BUDGET_HEADER_LOCK_IDS);
            getPersistenceBrokerTemplate().store(lockedDocuments);
        }
    }

    public void initialLoadToPBGL(Integer BaseYear)
    {
        // we will probably want several of these
        // or we will want to overload one, so people can leave their current
        // data in BC when they run genesis for the new year 
        clearBothYearsPBGL(BaseYear);
        // we have to clean out account reports to
        // it is not fiscal year-specific
        // this implies that last year's data can't be there, because the
        // organization hierarchy will have changed
        readGLForPBGL(BaseYear);
        createNewBCDocuments(BaseYear);
        addNewGLRowsToPBGL(BaseYear);
        writeFinalDiagnosticCounts();
    }
    
    public void updateToPBGL(Integer BaseYear)
    {
        readGLForPBGL(BaseYear);
     //   updateCurrentPBGL(BaseYear);
     //   createNewBCDocuments(BaseYear);
     //   addNewGLRowsToPBGL(BaseYear);
        writeFinalDiagnosticCounts();
    }
    
    //
    //  two test routines to display the field values in the two business objects
    //  produced from the GL read.  these are primarily here for initial testing
    private void info()
    { 
        if (! LOG.isEnabledFor(Level.INFO))
           {
            return;
           };
       //  print one header row   
       boolean printARow = true;  
       Integer nullCharts = new Integer(0);
       for (Map.Entry<String,BudgetConstructionHeader> bcHeaderRows : 
           bCHdrFromGL.entrySet())
       {
           BudgetConstructionHeader toPrint = bcHeaderRows.getValue();
           if (printARow)
           {    
           LOG.info("\n\nA sample BC header row\n");
           LOG.info(String.format("\nDocument Number = %s",toPrint.getDocumentNumber()));
           LOG.info(String.format("\nUniversity Fiscal Year = %d",
                                  toPrint.getUniversityFiscalYear()));
           LOG.info(String.format("\nChart: %s",toPrint.getChartOfAccountsCode()));
           LOG.info(String.format("\nAccount: %s",toPrint.getAccountNumber()));
           LOG.info(String.format("\nSubaccount: %s",toPrint.getSubAccountNumber()));
           LOG.info(String.format("\nOrganization Level Code: %d",
                    toPrint.getOrganizationLevelCode()));
           LOG.info(String.format("\nChart of Level: %s",
                    toPrint.getOrganizationLevelChartOfAccountsCode()));
           LOG.info(String.format("\nOrganization of Level: %s",
                    toPrint.getOrganizationLevelOrganization()));
           LOG.info(String.format("\nLock User ID: %s",
                    toPrint.getBudgetLockUserIdentifier()));
           LOG.info(String.format("\nTransaction Lock User ID: %s",
                    toPrint.getBudgetTransactionLockUserIdentifier()));
           LOG.info(String.format("\nVersion Number: %d",
                    toPrint.getVersionNumber()));
             printARow = false;
           }
           nullCharts = nullCharts+
                        ((toPrint.getChartOfAccountsCode() == null)? 1 : 0);
       }
       LOG.info(String.format("\nNumber of null charts = %d",nullCharts));
       // print one PBGL row
       for (Map.Entry<String,PendingBudgetConstructionGeneralLedger> pBGLRows : 
           pBGLFromGL.entrySet())
       {
           PendingBudgetConstructionGeneralLedger toPrint = pBGLRows.getValue();
           LOG.info("\n\nA sample PBGL row\n");
           LOG.info(String.format("\nDocument Number = %s",
                    toPrint.getDocumentNumber()));
           LOG.info(String.format("\nUniversity Fiscal Year = %d",
                   toPrint.getUniversityFiscalYear()));
           LOG.info(String.format("\nChart: %s",
                   toPrint.getChartOfAccountsCode()));
           LOG.info(String.format("\nAccount: %s",
                   toPrint.getAccountNumber()));
           LOG.info(String.format("\nSub Account: %s",
                   toPrint.getSubAccountNumber()));
           LOG.info(String.format("\nObject Code: %s",
                   toPrint.getFinancialObjectCode()));
           LOG.info(String.format("\nSubobject Code: %s",
                   toPrint.getFinancialSubObjectCode()));
           LOG.info(String.format("\nBalance Type: %s",
                   toPrint.getFinancialBalanceTypeCode()));
           LOG.info(String.format("\nObject Type: %s",
                   toPrint.getFinancialObjectTypeCode()));
           LOG.info(String.format("\nBase Amount: %s",
                   toPrint.getFinancialBeginningBalanceLineAmount().toString()));
           LOG.info(String.format("\nRequest Amount: %s",
                   toPrint.getAccountLineAnnualBalanceAmount().toString()));
           LOG.info(String.format("\nVersion Number: %d",
                   toPrint.getVersionNumber()));
           break;
       }
     }
    
    private void debug()
    { 
        if (! LOG.isEnabledFor(Level.DEBUG))
           {
            return;
           };
       //  print one header row    
       for (Map.Entry<String,BudgetConstructionHeader> bcHeaderRows : 
           bCHdrFromGL.entrySet())
       {
           BudgetConstructionHeader toPrint = bcHeaderRows.getValue();
           LOG.debug("\n\nA sample BC header row\n");
           LOG.debug(String.format("\nDocument Number = %s",toPrint.getDocumentNumber()));
           LOG.debug(String.format("\nUniversity Fiscal Year = %d",
                                  toPrint.getUniversityFiscalYear()));
           LOG.debug(String.format("\nChart: %s",toPrint.getChartOfAccountsCode()));
           LOG.debug(String.format("\nAccount: %s",toPrint.getAccountNumber()));
           LOG.debug(String.format("\nSubaccount: %s",toPrint.getSubAccountNumber()));
           LOG.debug(String.format("\nOrganization Level Code: %d",
                    toPrint.getOrganizationLevelCode()));
           LOG.debug(String.format("\nChart of Level: %s",
                    toPrint.getOrganizationLevelChartOfAccountsCode()));
           LOG.debug(String.format("\nOrganization of Level: %s",
                    toPrint.getOrganizationLevelOrganization()));
           LOG.debug(String.format("\nLock User ID: %s",
                    toPrint.getBudgetLockUserIdentifier()));
           LOG.debug(String.format("\nTransaction Lock User ID: %s",
                    toPrint.getBudgetTransactionLockUserIdentifier()));
           LOG.debug(String.format("\nVersion Number: %d",
                   toPrint.getVersionNumber()));
           break;
       }
       // print one PBGL row
       for (Map.Entry<String,PendingBudgetConstructionGeneralLedger> pBGLRows : 
           pBGLFromGL.entrySet())
       {
           PendingBudgetConstructionGeneralLedger toPrint = pBGLRows.getValue();
           LOG.debug("\n\nA sample PBGL row\n");
           LOG.debug(String.format("\nDocument Number = %s",
                    toPrint.getDocumentNumber()));
           LOG.debug(String.format("\nUniversity Fiscal Year = %d",
                   toPrint.getUniversityFiscalYear()));
           LOG.debug(String.format("\nChart: %s",
                   toPrint.getChartOfAccountsCode()));
           LOG.debug(String.format("\nAccount: %s",
                   toPrint.getAccountNumber()));
           LOG.debug(String.format("\nSub Account: %s",
                   toPrint.getSubAccountNumber()));
           LOG.debug(String.format("\nObject Code: %s",
                   toPrint.getFinancialObjectCode()));
           LOG.debug(String.format("\nSubobject Code: %s",
                   toPrint.getFinancialSubObjectCode()));
           LOG.debug(String.format("\nBalance Type: %s",
                   toPrint.getFinancialBalanceTypeCode()));
           LOG.debug(String.format("\nObject Type: %s",
                   toPrint.getFinancialObjectTypeCode()));
           LOG.debug(String.format("\nBase Amount: %s",
                     toPrint.getFinancialBeginningBalanceLineAmount().toString()));
           LOG.debug(String.format("\nRequest Amount: %s",
                   toPrint.getAccountLineAnnualBalanceAmount().toString()));
           LOG.debug(String.format("\nVersion Number: %d",
                   toPrint.getVersionNumber()));
           break;
       }
     }
    
    //
    //
    // private working methods

    //
    private void addNewGLRowsToPBGL(Integer BaseYear)
    {
        // this method adds the GL rows not yet in PBGL to PBGL
        for (Map.Entry<String,PendingBudgetConstructionGeneralLedger> newPBGLRows :
             pBGLFromGL.entrySet())
        {
             PendingBudgetConstructionGeneralLedger rowToAdd = newPBGLRows.getValue();
             String headerKey = buildHeaderTestKeyFromPBGL(rowToAdd);
             // the document number should be in either the new document headers just
             // added or in the list of document numbers from the existing PBGL rows
             // if it isn't, we issue a warning and go on, but this is an error
             String docNumber = findRowDocumentNumber(headerKey);
             if (docNumber == null)
             {
                 LOG.warn(String.format("\nno document number found for BB GL key (%s,%s,%s)",
                          rowToAdd.getChartOfAccountsCode(),
                          rowToAdd.getAccountNumber(),
                          rowToAdd.getSubAccountNumber()));
                 continue;
             }
             nGLHeadersAdded = nGLHeadersAdded+1;
             rowToAdd.setDocumentNumber(docNumber);
             getPersistenceBrokerTemplate().store(rowToAdd);
        }
    }
    //
    // these two methods build the GL field string that triggers creation of a new
    // pending budget construction general ledger row
    private String buildGLTestKeyFromPBGL (PendingBudgetConstructionGeneralLedger
            pendingBudgetConstructionGeneralLedger)
    {
       String PBGLTestKey = new String();
       PBGLTestKey = pendingBudgetConstructionGeneralLedger.getChartOfAccountsCode()+
                         pendingBudgetConstructionGeneralLedger.getAccountNumber()+
                         pendingBudgetConstructionGeneralLedger.getSubAccountNumber()+
                         pendingBudgetConstructionGeneralLedger.getFinancialObjectCode()+
                         pendingBudgetConstructionGeneralLedger.getFinancialSubObjectCode()+
                         pendingBudgetConstructionGeneralLedger.getFinancialBalanceTypeCode()+
                         pendingBudgetConstructionGeneralLedger.getFinancialObjectTypeCode();
       return PBGLTestKey;
    }
    private String buildGLTestKeyFromSQLResults (Object[] sqlResult)
    {
        String GLTestKey = new String();
        GLTestKey = (String) sqlResult[sqlChartOfAccountsCode]+
                    (String) sqlResult[sqlAccountNumber]+
                    (String) sqlResult[sqlSubAccountNumber]+
                    (String) sqlResult[sqlObjectCode]+
                    (String) sqlResult[sqlSubObjectCode]+
                    (String) sqlResult[sqlBalanceTypeCode]+
                    (String) sqlResult[sqlObjectTypeCode];
        return GLTestKey;
    }
    //
    // these two methods build the GL field string that triggers creation of a new
    // budget construction header
    public String buildHeaderTestKeyFromPBGL (PendingBudgetConstructionGeneralLedger
            pendingBudgetConstructionGeneralLedger)
            {
               String headerBCTestKey = new String();
               headerBCTestKey = pendingBudgetConstructionGeneralLedger.getChartOfAccountsCode()+
                                 pendingBudgetConstructionGeneralLedger.getAccountNumber()+
                                 pendingBudgetConstructionGeneralLedger.getSubAccountNumber();
               return headerBCTestKey;
            }
    private String buildHeaderTestKeyFromSQLResults (Object[] sqlResult)
    {
        String headerBCTestKey = new String();
        headerBCTestKey = (String) sqlResult[sqlChartOfAccountsCode]+
                          (String) sqlResult[sqlAccountNumber]+
                          (String) sqlResult[sqlSubAccountNumber];
        return headerBCTestKey;
    }
    
    private void clearBaseYearPBGL(Integer BaseYear)
    {
        // remove rows from the base year
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                BaseYear);
        QueryByCriteria queryID = 
            new QueryByCriteria(PendingBudgetConstructionGeneralLedger.class,
                    criteriaID);
        LOG.info(String.format("delete PBGL started at %tT for %d",dateTimeService.getCurrentDate(),
                BaseYear));
        getPersistenceBrokerTemplate().deleteByQuery(queryID);
        LOG.info(String.format("delete PBGL ended at %tT",dateTimeService.getCurrentDate()));
    }
    
    private void clearBothYearsPBGL(Integer BaseYear)
    {
        clearBaseYearPBGL(BaseYear);
        clearRequestYearPBGL(BaseYear);
    }
    
    private void clearRequestYearPBGL(Integer BaseYear)
    {
        Integer RequestYear = BaseYear + 1;
        // remove rows from the request year
        Criteria criteriaID = new Criteria();
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                RequestYear);
        QueryByCriteria queryID = 
            new QueryByCriteria(PendingBudgetConstructionGeneralLedger.class,
                    criteriaID);
        LOG.info(String.format("\ndelete PBGL started at %tT for %d",dateTimeService.getCurrentDate(),
                RequestYear));
        getPersistenceBrokerTemplate().deleteByQuery(queryID);
        LOG.info(String.format("\ndelete PBGL ended at %tT",dateTimeService.getCurrentDate()));
    }
    
    private void createNewBCDocuments(Integer BaseYear)
    {
        // if there are no documents, just return and skip all the set up
        if (bCHdrFromGL.size() == 0)
        {
            return;
        }
        Integer RequestYear = BaseYear + 1;
        // now we have to get the document service and the workflow service
        //@@TODO:
        //documentService = SpringServiceLocator.getDocumentService();
        //workflowDocumentService = SpringServiceLocator.getWorkflowDocumentService();
        // this routine reads the list of GL document accounting keys from
        // the GL that are not in the budget, and creates both a workflow
        // document and a budget construction header for them
        for (Map.Entry<String,BudgetConstructionHeader> newBCHdrRows :
            bCHdrFromGL.entrySet())
        {
            BudgetConstructionHeader headerGL = newBCHdrRows.getValue();
            String headerGLKey                = newBCHdrRows.getKey();
            Document bCDocument;
            try
            {
              //@@TODO:  it looks like these things need to be saved.
              //         should we just save the objects in an array and
              //         store them at the end?
              //
              //  the return from the getNewDocument is a new instance of the BC Header
              //  document cast as a document.  our BCHeader document, as all documents do,
              //  extends documentBase.  so, we will set the document base fields, then
              //  we will set the header specific values from the header object  
               bCDocument = 
               documentService.getNewDocument(BudgetConstructionConstants.BUDGET_CONSTRUCTION_DOCUMENT_NAME);
               LOG.info(String.format("\nDocument Number: %s",bCDocument.getDocumentNumber()));
               LOG.info(String.format("\ninstance of BudgetConstructionDocument? %s",
                        (bCDocument.getDocumentHeader().getWorkflowDocument() instanceof
                                BudgetConstructionDocument)));
               LOG.info(String.format("\ndocument class of workflow document: %s",
                       bCDocument.getDocumentHeader().getWorkflowDocument().getClass().getName()));
               LOG.info(String.format("\ndocument class of documentHeader in document: %s",
                                      bCDocument.getDocumentHeader().getClass().getName()));
               
            }
            catch (WorkflowException exxx)
            {
               LOG.warn(String.format("\nerror %s creating BC document for:\n"+
                                      "Chart : %s\nAccount: %s\nSubaccount: %s\n",
                                      exxx.getMessage(),
                                      headerGL.getChartOfAccountsCode(),
                                      headerGL.getAccountNumber(),
                                      headerGL.getSubAccountNumber()));
               // the document header will have no document number
               // the relevant GL rows will NOT be written to PBGL
               // we can try again on the next run
               continue;
            }
            //  save the new document number so we can
            //  store the detail PBGL rows later.
            //  the BC pending GL entry rows
            //  (the tests use the document header to get the document number.)
            String documentID = bCDocument.getDocumentHeader().getDocumentNumber();
            bCDocument.setDocumentNumber(documentID);
            CurrentPBGLDocNumbers.put(headerGLKey,documentID);  
            headerGL.setDocumentNumber(documentID);
            //  before we store, we need to set the Budget Construction header fields
            //  in the new of a document that we just got
            nGLHeadersAdded = nGLHeadersAdded+1;
            // @@TODO:
            //  we also need to store the Kuali document header (in the Kuali doc
            //  header table, not the BC docheader table)
            // when the table associated with the budget construction document is filled
            // (ld_bcnstr_hdr_t), the auto-update=object on the reference-id to the
            // fp_doc_header_t table will automatically store the generic kuali document
            // header
            DocumentHeader kualiDocHeader = bCDocument.getDocumentHeader();
            kualiDocHeader.setOrganizationDocumentNumber(Integer.toString(RequestYear));
            kualiDocHeader.setFinancialDocumentStatusCode(
                           BudgetConstructionConstants.BUDGET_CONSTRUCTION_DOCUMENT_INITIAL_STATUS);
//            kualiDocHeader.setFinancialDocumentStatusCode(
//                           Constants.INITIAL_KUALI_DOCUMENT_STATUS_CD);
            kualiDocHeader.setFinancialDocumentTotalAmount(KualiDecimal.ZERO);
            kualiDocHeader.setFinancialDocumentDescription(String.format("%s %d %s %s",
                           BudgetConstructionConstants.BUDGET_CONSTRUCTION_DOCUMENT_DESCRIPTION,
                           headerGL.getUniversityFiscalYear(),
                           headerGL.getChartOfAccounts(),headerGL.getAccountNumber()));
            kualiDocHeader.setExplanation(
                    BudgetConstructionConstants.BUDGET_CONSTRUCTION_DOCUMENT_DESCRIPTION);
            //  we need to set the values in the new document object
            BudgetConstructionDocument bCDocumentPtr = 
                    createNewBCDocumentSetUp(headerGL,bCDocument);
            getPersistenceBrokerTemplate().store(bCDocumentPtr);
            /*
             *   the hierarchy is apparently as follows.
             *   Document objects have a header (getHeader) and a (transient) workflowDocument
             *   (getWorkflowDocument).  the header refers to the Kuali document header table.
             *   the workflowDocument points to an object of the class of document being created,
             *   and this object will be saved to the table which the object is mapped to in the
             *   OJB repository.
             */
            //  we are simply going to store the budget construction document
            //  genesis is under transactional control, and workflow will not be able to
            //  read and route our document until our transaction commits.  therefore, we will
            //  actually route the document in a separate step.
 //           try
 //           {
 //               TransactionalDocument tDocument = (TransactionalDocument) bCDocument;
 //               documentService.saveDocument(tDocument);
 //               documentService.routeDocument(tDocument,"created by Genesis",null);
 //           }
 //           catch (WorkflowException exxx)
 //           {
 //              LOG.warn(String.format("\nerror %s \nsaving BC document for:\n"+
 //                                     "Chart : %s\nAccount: %s\nSubaccount: %s\n",
 //                                     exxx.getMessage(),
 //                                     headerGL.getChartOfAccountsCode(),
 //                                     headerGL.getAccountNumber(),
 //                                     headerGL.getSubAccountNumber()));
               // the document header will have no document number
               // the relevant GL rows will NOT be written to PBGL
               // we can try again on the next run
 //              continue;
 //           }
        }
    }
    
    private BudgetConstructionDocument createNewBCDocumentSetUp(BudgetConstructionHeader BCHdr, 
                 Document bCDocument) 
    {   
        /*
         * workflow will save our budget construction header row for us, because the
         * the document class name of the document we fetched with getNewDocument is
         * mapped to the Budget Construction Header table in the OJB repository.
         * we cast the generic document object as a Budget Construction Document so
         * we can set the fields
         */
        BudgetConstructionDocument bCDocumentPtr = 
            (BudgetConstructionDocument) bCDocument;
         bCDocumentPtr.setUniversityFiscalYear(BCHdr.getUniversityFiscalYear());
         bCDocumentPtr.setChartOfAccountsCode(BCHdr.getChartOfAccountsCode());
         bCDocumentPtr.setAccountNumber(BCHdr.getAccountNumber());
         bCDocumentPtr.setSubAccountNumber(BCHdr.getSubAccountNumber());
         bCDocumentPtr.setOrganizationLevelCode(BCHdr.getOrganizationLevelCode());
         bCDocumentPtr.setOrganizationLevelChartOfAccountsCode(
                           BCHdr.getOrganizationLevelChartOfAccountsCode());
         bCDocumentPtr.setOrganizationLevelOrganizationCode(
                           BCHdr.getOrganizationLevelOrganizationCode());
         bCDocumentPtr.setBudgetLockUserIdentifier(
                           BCHdr.getBudgetLockUserIdentifier());
         bCDocumentPtr.setBudgetTransactionLockUserIdentifier(
                           BCHdr.getBudgetLockUserIdentifier());
         return bCDocumentPtr;
    }
    
    private String findRowDocumentNumber(String headerKey)
    {
        String documentNumber = CurrentPBGLDocNumbers.get(headerKey);
        
        if (documentNumber == null)
        {
            BudgetConstructionHeader headerFromGL = bCHdrFromGL.get(headerKey);
            if (headerFromGL == null)
            {
               return documentNumber;
            }
            // doing it this way will detect a GL row with a document number that
            // is still null
            documentNumber = headerFromGL.getDocumentNumber();
        }
        return documentNumber;
    }
    
    private BudgetConstructionHeader newBCHdrBusinessObject(Integer RequestYear,
            Object[] sqlResult)
    {
       BudgetConstructionHeader hDrBC = new BudgetConstructionHeader();
       // document number will be set in the service--we don't yet know whether this
       // will require a new document
       hDrBC.setUniversityFiscalYear(RequestYear);
       hDrBC.setChartOfAccountsCode((String) sqlResult[sqlChartOfAccountsCode]);
       hDrBC.setAccountNumber((String) sqlResult[sqlAccountNumber]);
       hDrBC.setSubAccountNumber((String) sqlResult[sqlSubAccountNumber]);
       // at the account level, this is the same as the chart
       // all new headers start at the account level, and this object will only be used
       // if a new header is required.
       //  the organization and chart are always NULL at the account level
       hDrBC.setOrganizationLevelChartOfAccountsCode(BudgetConstructionConstants.INITIAL_ORGANIZATION_LEVEL_CHART_OF_ACCOUNTS_CODE);
       hDrBC.setOrganizationLevelOrganizationCode(BudgetConstructionConstants.INITIAL_ORGANIZATION_LEVEL_ORGANIZATION_CODE);
       hDrBC.setOrganizationLevelCode(BudgetConstructionConstants.INITIAL_ORGANIZATION_LEVEL_CODE);
       hDrBC.setBudgetTransactionLockUserIdentifier(BudgetConstructionConstants.DEFAULT_BUDGET_HEADER_LOCK_IDS);
       hDrBC.setBudgetLockUserIdentifier(BudgetConstructionConstants.DEFAULT_BUDGET_HEADER_LOCK_IDS);
       hDrBC.setVersionNumber(DEFAULT_VERSION_NUMBER);
   //  we need to initialize the document number to null, and set it later
   //  if it doesn't get set later, the code will detect the failure and issue a warning    
       hDrBC.setDocumentNumber(null);
   //  return a new header
       return hDrBC;
    }
    
    private PendingBudgetConstructionGeneralLedger newPBGLBusinessObject(Integer RequestYear,
                                                                         Object[] sqlResult)
    {
       PendingBudgetConstructionGeneralLedger PBGLObj = new PendingBudgetConstructionGeneralLedger();
     /*  
      * the document number will be set later if we have to store this in a new document
      * a new row in an existing document will take it's document number from the existing document
      * otherwise (existing document, existing row), the only field in this that will be
      * the beginning balance amount
     */  
       PBGLObj.setUniversityFiscalYear(RequestYear);
       PBGLObj.setChartOfAccountsCode((String) sqlResult[sqlChartOfAccountsCode]);
       PBGLObj.setAccountNumber((String) sqlResult[sqlAccountNumber]);
       PBGLObj.setSubAccountNumber((String) sqlResult[sqlSubAccountNumber]);
       PBGLObj.setFinancialObjectCode((String) sqlResult[sqlObjectCode]);
       PBGLObj.setFinancialSubObjectCode((String) sqlResult[sqlSubObjectCode]);
       PBGLObj.setFinancialBalanceTypeCode((String) sqlResult[sqlBalanceTypeCode]);
       PBGLObj.setFinancialObjectTypeCode((String) sqlResult[sqlObjectTypeCode]);
       KualiDecimal BaseAmount = 
           (KualiDecimal) sqlResult[sqlBeginningBalanceLineAmount];
       BaseAmount = 
           BaseAmount.add((KualiDecimal) sqlResult[sqlAccountLineAnnualBalanceAmount]);
       PBGLObj.setFinancialBeginningBalanceLineAmount(BaseAmount);
       PBGLObj.setAccountLineAnnualBalanceAmount(KualiDecimal.ZERO);
       //  ObjectID is set in the BusinessObjectBase on insert and update
       //  but, we must set the version number
       PBGLObj.setVersionNumber(DEFAULT_VERSION_NUMBER);
       return PBGLObj;
    }
    
    private void readGLForPBGL(Integer BaseYear)
    {
        // we apparently need to configure the log file in order to use it
        // @@TODO: should these be a "weak hash map", to optimize memory use?
       Integer RequestYear = BaseYear + 1;
        //
        //  set up a report query to fetch all the GL rows we are going to need
        Criteria criteriaID = new Criteria();
        // we only pick up a single balance type
        // we also use an integer fiscal year
        // *** this is a point of change if either of these criteria change ***
        // @@TODO We should regularize the sources for these constants
        // they should probably all come from GL (although UNIV_FISCAL_YR is generic)
        // we should add the two hard-wired strings at the bottom to GLConstants
        criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                BaseYear);
        criteriaID.addEqualTo(PropertyConstants.BALANCE_TYPE_CODE,
                              Constants.BALANCE_TYPE_BASE_BUDGET);
        String[] queryAttr = {PropertyConstants.CHART_OF_ACCOUNTS_CODE,
                              PropertyConstants.ACCOUNT_NUMBER,
                              PropertyConstants.SUB_ACCOUNT_NUMBER,
                              PropertyConstants.OBJECT_CODE,
                              PropertyConstants.SUB_OBJECT_CODE,
                              PropertyConstants.BALANCE_TYPE_CODE,
                              PropertyConstants.OBJECT_TYPE_CODE,
                              PropertyConstants.ACCOUNT_LINE_ANNUAL_BALANCE_AMOUNT,
                              PropertyConstants.BEGINNING_BALANCE_LINE_AMOUNT};
        ReportQueryByCriteria queryID = 
            new ReportQueryByCriteria(Balance.class, queryAttr, criteriaID, true);
        //
        // set up the hashmaps by iterating through the results
        
        // @@TODO this should be in a try/catch structure.  We should catch a 
        //        SQL error, write it to the log, and raise a more generic error
        //        ("error reading GL Balance Table in BC batch"), and throw that
        LOG.info("\nGL Query started: "+String.format("%tT",dateTimeService.getCurrentDate()));
        Iterator Results = getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(queryID);
        LOG.info("\nGL Query finished: "+String.format("%tT",dateTimeService.getCurrentDate()));
        while (Results.hasNext())
        {
            Object[] ReturnList = (Object []) Results.next();
            LOG.debug(String.format("\nfields returned = %d\n",ReturnList.length));
            LOG.debug(String.format("\nvalue in last field = %s\n",
                    ReturnList[sqlBeginningBalanceLineAmount].toString()));
            //
            //  exclude any rows where the amounts add to 0
            //  (we don't do it in the WHERE clause to be certain we are ANSI standard)
            KualiDecimal BaseAmount = 
                (KualiDecimal) ReturnList[sqlBeginningBalanceLineAmount];
            BaseAmount = 
                BaseAmount.add((KualiDecimal) ReturnList[sqlAccountLineAnnualBalanceAmount]);
            if (BaseAmount.isZero())
            {
                nGLBBRowsRead = nGLBBRowsRead+1;
                nGLBBRowsZeroNet = nGLBBRowsZeroNet+1;
                continue;
            }
            //  
            //  we always need to build a new PGBL object
            //  we have selected the entire key from GL_BALANCE_T
            //  @@TODO we should throw an exception if the key already exists
            //  this means the table has changed and this code needs to be re-written
            String GLTestKey = buildGLTestKeyFromSQLResults(ReturnList);
            pBGLFromGL.put(GLTestKey,
                     newPBGLBusinessObject(RequestYear,ReturnList));
            //  we only add a BC Header Object for a unique chart/account/subaccount
            String HeaderTestKey = buildHeaderTestKeyFromSQLResults(ReturnList);
            if (bCHdrFromGL.get(HeaderTestKey) == null)
            {
                // add a BCHeader object
                bCHdrFromGL.put(HeaderTestKey,
                          newBCHdrBusinessObject(RequestYear,ReturnList));
            }
        }
        nGLBBRowsRead = nGLBBRowsRead+pBGLFromGL.size();
        nGLBBKeysRead = bCHdrFromGL.size();
        LOG.info("\nHash maps built: "+String.format("%tT",dateTimeService.getCurrentDate()));
        LOG.info(String.format("\nGL detail hashmap size = %d",pBGLFromGL.size()));
        LOG.info(String.format("\nGL keys hashmap size = %d",bCHdrFromGL.size()));
        info();
    }
    
    private void removeDuplicateHeader(PendingBudgetConstructionGeneralLedger currentPBGLInstance)
    {
        String TestKey = buildHeaderTestKeyFromPBGL(currentPBGLInstance);
        bCHdrFromGL.remove(TestKey);
    }
    
    private void saveCurrentPGBLDocNumber(PendingBudgetConstructionGeneralLedger currentPBGLInstance)
    {
       String testKey = buildHeaderTestKeyFromPBGL(currentPBGLInstance);
       if (CurrentPBGLDocNumbers.get(testKey) == null)
       {
           CurrentPBGLDocNumbers.put(testKey,currentPBGLInstance.getDocumentNumber());
       }
    }    
    
    private void updateBaseBudgetAmount(PendingBudgetConstructionGeneralLedger currentPBGLInstance)
    {
       String TestKey = buildGLTestKeyFromPBGL(currentPBGLInstance);
       if (!pBGLFromGL.containsKey(TestKey))
       {
           return;
       }
       PendingBudgetConstructionGeneralLedger matchFromGL = pBGLFromGL.get(TestKey);
       KualiDecimal baseFromCurrentGL = 
           matchFromGL.getFinancialBeginningBalanceLineAmount();
       KualiDecimal baseFromPBGL = 
           currentPBGLInstance.getFinancialBeginningBalanceLineAmount();
       // remove the candidate GL from the hash list
       // it won't match with anything else
       // it should NOT be inserted into the PBGL table
       pBGLFromGL.remove(TestKey);
       if (baseFromCurrentGL == baseFromPBGL)
       {
           // no need to update--false alarm
           return;
       }
       // update the base amount and store the updated PBGL row
       nGLRowsUpdated =nGLRowsUpdated+1;
       currentPBGLInstance.setFinancialBeginningBalanceLineAmount(baseFromCurrentGL);
       getPersistenceBrokerTemplate().store(currentPBGLInstance);
    }
    
    private void updateCurrentPBGL(Integer BaseYear)
    {
       Integer RequestYear = BaseYear+1;
       
       
       // what we are going to do here is what Oracle calls a hash join
       //
       // we will merge the current PBGL rows with the GL detail, and 
       // replace the amount on each current PBGL row which matches from
       // the GL row, and remove the GL row 
       //
       // we will compare the GL Key row with the the current PBLG row,
       // and if the keys are the same, we will eliminate the GL key row
       //
       // we will save the document number of the PBGL row before we store it
       // back, so we can use a current document number for those GL detail rows
       // (basically new object classes) which belong to a current document but
       // are not in PBGL.
       //
       //  fetch the current BC header rows
       Criteria criteriaID = new Criteria();
       criteriaID.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,RequestYear);
       QueryByCriteria queryID = 
           new QueryByCriteria(PendingBudgetConstructionGeneralLedger.class,
                               criteriaID);
       Iterator Results = getPersistenceBrokerTemplate().getIteratorByQuery(queryID);
       //  loop through the results
       while (Results.hasNext())
       {
           nCurrentPBGLRows = nCurrentPBGLRows+1;
           PendingBudgetConstructionGeneralLedger currentPBGLInstance =
               (PendingBudgetConstructionGeneralLedger) Results.next();
           // first save the doc number
           saveCurrentPGBLDocNumber(currentPBGLInstance);
           // remove any new header rows which match this PBGL row on the
           // header accounting key.  A BC header (and a document header) 
           // already exist if such a match occurs.
           removeDuplicateHeader(currentPBGLInstance);
           // update the base amount and store the result if necessary
           updateBaseBudgetAmount(currentPBGLInstance);
       }
    }
    
    private void writeFinalDiagnosticCounts()
    {
        LOG.info(String.format("\n\nRun Statistics\n\n"));
        LOG.info(String.format("\nGeneral Ledger BB Keys read: %d",
                                nGLBBKeysRead));
        LOG.info(String.format("\nGeneral Ledger BB Rows read: %d",
                 nGLBBRowsRead));
        LOG.info(String.format("\nExisting Pending General Ledger rows: %d",
                 nCurrentPBGLRows));
        LOG.info(String.format("\nof these..."));
        LOG.info(String.format("\nnew BC headers written: %d",
                 nGLHeadersAdded));
        LOG.info(String.format("\nnew PBGL rows written: %d",
                 nGLRowsAdded));
        LOG.info(String.format("\ncurrent PBGL amounts updated: %d",
                 nGLRowsUpdated));
        
    }
    
    //  
    //  this routine is designed to route BC documents outside of transactional
    //  control.  the documents have already been saved in the single genesis
    //  transaction.  here, we fetch them all, and route the ones which are not
    //  yet in workflow.
    //
    public void routeNewBCDocuments(Integer BaseYear)
    {
        Integer RequestYear = BaseYear+1;
        Long bCDocumentsRead   = new Long(0);
        Long bCDocumentsRouted = new Long(0);
        Collection<BudgetConstructionDocument> holdbCDocument;
        DocumentHeader holdDocumentHeader = new DocumentHeader();
        // first, get all the documents from the budget construction header table
        Criteria bCCriteria    = new Criteria();
        bCCriteria.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                RequestYear);
        QueryByCriteria bCDocumentQuery = 
                 new QueryByCriteria(BudgetConstructionDocument.class, bCCriteria);
        holdbCDocument = (Collection<BudgetConstructionDocument>)
            getPersistenceBrokerTemplate().getCollectionByQuery(bCDocumentQuery);
        Iterator Results = holdbCDocument.iterator();
        while (Results.hasNext())
        {
           BudgetConstructionDocument bCDocument = (BudgetConstructionDocument) Results.next(); 
           String documentNumber = bCDocument.getDocumentNumber(); 
           bCDocumentsRead = bCDocumentsRead+1;
           /*
           try
           {
               if (workflowDocumentService.workflowDocumentExists(documentNumber))
               {
                   continue;
               }
           }
           catch (IllegalArgumentException iaex)
           {
               LOG.warn("\ndocument "+documentNumber+
                        " triggered on search: \n"+iaex.getMessage());
               continue;
           }
           */
           // fetch the document: it needs to be routed
           // make a clone of the document header
           // we are going to want to override the status that routing inserts
           // once the document is routed
           // cloneDocumentHeader(holdDocumentHeader,bCDocument.getDocumentHeader());
           holdDocumentHeader = bCDocument.getDocumentHeader();
           try
           {
               KualiWorkflowDocument kualiWorkflowDocument =
                   workflowDocumentService.createWorkflowDocument(
                           Long.valueOf(bCDocument.getDocumentHeader().getDocumentNumber()),
                           GlobalVariables.getUserSession().getUniversalUser());
               GlobalVariables.getUserSession().setWorkflowDocument(kualiWorkflowDocument);
               bCDocument.getDocumentHeader().setWorkflowDocument(kualiWorkflowDocument);
//               getPersistenceBrokerTemplate().store(bCDocument.getDocumentHeader());
//               getPersistenceBrokerTemplate().store(bCDocument);
               documentService.routeDocument((Document) bCDocument,"created by Genesis",null);
           }
           catch (ValidationException vex)
           {
               LOG.warn("\ndocument "+documentNumber+ 
                       " triggered on route: \n"+vex.getMessage());
              continue;
           }
           catch (WorkflowException wex)
           {
               LOG.warn("\ndocument "+documentNumber+
                       " triggered on route: \n"+wex.getMessage());
              continue;
           }
           bCDocumentsRouted = bCDocumentsRouted+1;
           //  change the routing code
           getPersistenceBrokerTemplate().store(holdDocumentHeader);
        }
        LOG.info(String.format("\nBudget documents read for %d: %d",
                               RequestYear,bCDocumentsRead));
        LOG.info(String.format("\nBudget documents routed for %d: %d",
                 RequestYear,bCDocumentsRouted));
    }
    
    private void cloneDocumentHeader(DocumentHeader DHTo, DocumentHeader DHFrom)
    {
        DHTo.setCorrectedByDocumentId(DHFrom.getCorrectedByDocumentId());
        DHTo.setDocumentFinalDate(DHFrom.getDocumentFinalDate());
        DHTo.setExplanation(DHFrom.getExplanation());
        DHTo.setExtendedAttributeValues(DHFrom.getExtendedAttributeValues());
        DHTo.setFinancialDocumentDescription(DHFrom.getFinancialDocumentDescription());
        DHTo.setFinancialDocumentInErrorNumber(DHFrom.getFinancialDocumentInErrorNumber());
        DHTo.setFinancialDocumentStatusCode(BudgetConstructionConstants.BUDGET_CONSTRUCTION_DOCUMENT_INITIAL_STATUS);
        DHTo.setFinancialDocumentTemplateNumber(
                DHFrom.getFinancialDocumentTemplateNumber());
        DHTo.setFinancialDocumentTotalAmount(
                DHFrom.getFinancialDocumentTotalAmount());
        DHTo.setNotes(DHFrom.getNotes());
        DHTo.setObjectId(DHFrom.getObjectId());
        DHTo.setOrganizationDocumentNumber(DHFrom.getOrganizationDocumentNumber());
        DHTo.setVersionNumber(DHFrom.getVersionNumber());
        DHTo.setWorkflowDocument(DHFrom.getWorkflowDocument());
    }
    
    
    //  @@TODO:
    //  this routine had the same problem as version 1: an iterator is cleaned up
    //  when a persistence broker is closed.
    
    public void routeNewBCDocumentsVersion2 (Integer BaseYear)
    {
        Integer RequestYear = BaseYear+1;
        Long bCDocumentsRead   = new Long(0);
        Long bCDocumentsRouted = new Long(0);
        DocumentHeader holdDocHeader = new DocumentHeader();
        // first, get all the document numbers from the budget construction header table
        Criteria bCCriteria    = new Criteria();
        bCCriteria.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                RequestYear);
        String[] queryAttr = {PropertyConstants.DOCUMENT_NUMBER};
        ReportQueryByCriteria bCDocumentQuery = 
                 new ReportQueryByCriteria(BudgetConstructionDocument.class,
                                           queryAttr, bCCriteria);
        Iterator Results = 
            getPersistenceBrokerTemplate().getReportQueryIteratorByQuery(bCDocumentQuery);
        // this iterator does not persist, so the closing of the broker on every
        // transaction will not destroy it
        while (Results.hasNext())
        {
           String documentNumber = (String) ((Object[]) Results.next())[0]; 
           bCDocumentsRead = bCDocumentsRead + 1;
           try
           {
               if (workflowDocumentService.workflowDocumentExists(documentNumber))
               {
                   continue;
               }
           }
           catch (IllegalArgumentException iaex)
           {
               LOG.warn("\ndocument "+documentNumber+
                        " triggered on search: \n"+iaex.getMessage());
               continue;
           }
           // fetch the document: it needs to be routed
           // try to route the document
           Criteria bCDocCriteria = new Criteria();
           bCDocCriteria.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                RequestYear);
           bCDocCriteria.addEqualTo(PropertyConstants.DOCUMENT_NUMBER,documentNumber);
           QueryByCriteria bCDocQuery = new QueryByCriteria(
                   BudgetConstructionDocument.class,bCDocCriteria);
           BudgetConstructionDocument bCDocument = (BudgetConstructionDocument)
               getPersistenceBrokerTemplate().getObjectByQuery(bCDocQuery);
           // make a clone of the document header
           // we are going to want to override the status that routing inserts
           // once the document is routed
           cloneDocumentHeader(holdDocHeader,bCDocument.getDocumentHeader());
           try
           {
               documentService.routeDocument((Document) bCDocument,null,null);
           }
           catch (ValidationException vex)
           {
               LOG.warn("\ndocument "+documentNumber+ 
                       " triggered on route: \n"+vex.getMessage());
              continue;
           }
           catch (WorkflowException wex)
           {
               LOG.warn("\ndocument "+documentNumber+
                       " triggered on route: \n"+wex.getMessage());
              continue;
           }
           bCDocumentsRouted = bCDocumentsRouted+1;
           //  change the routing code
           getPersistenceBrokerTemplate().store(holdDocHeader);
        }
        LOG.info(String.format("\nBudget documents read for %d: %d",
                               RequestYear,bCDocumentsRead));
        LOG.info(String.format("\nBudget documents routed for %d: %d",
                 RequestYear,bCDocumentsRouted));
    }

    //@@TODO:
    // this is the old, non-working version, which should be removed unless we
    // need to tweak it if our document number method does not work
    private void routeNewBCDocumentsVersion1(Integer BaseYear)
    {
        Integer RequestYear = BaseYear+1;
        Long bCDocumentsRead   = new Long(0);
        Long bCDocumentsRouted = new Long(0);
        Criteria bCCriteria    = new Criteria();
        bCCriteria.addEqualTo(PropertyConstants.UNIVERSITY_FISCAL_YEAR,
                RequestYear);
        QueryByCriteria bCDocumentQuery = 
                 new QueryByCriteria(BudgetConstructionDocument.class,bCCriteria);
        Iterator Results = getPersistenceBrokerTemplate().getIteratorByQuery(bCDocumentQuery);
        while (Results.hasNext())
        {
           BudgetConstructionDocument bCDocument = 
               (BudgetConstructionDocument) Results.next();
           bCDocumentsRead = bCDocumentsRead + 1;
           try
           {
               if (workflowDocumentService.workflowDocumentExists(
                       bCDocument.getDocumentHeader().getDocumentNumber()))
               {
                   continue;
               }
           }
           catch (IllegalArgumentException iaex)
           {
               LOG.warn("\ndocument bCDocument.getDocumentHeader().getDocumentNumber()"+
                        " triggered on search: \n"+iaex.getMessage());
               continue;
           }
           // create and route the workflow document
           try
           {
               // we have to create a transient KualiWorkflowDocument object with the 
               // correct document number
               documentService.routeDocument((Document) bCDocument,"created by Genesis",null);
           }
           catch (ValidationException vex)
           {
               LOG.warn("\ndocument bCDocument.getDocumentHeader().getDocumentNumber()"+
                       " triggered on route: \n"+vex.getMessage());
              continue;
           }
           catch (WorkflowException wex)
           {
               LOG.warn("\ndocument bCDocument.getDocumentHeader().getDocumentNumber()"+
                       " triggered on route: \n"+wex.getMessage());
              continue;
           }
           bCDocumentsRouted = bCDocumentsRouted+1;
           //  change the routing code
           DocumentHeader documentHeader = bCDocument.getDocumentHeader();
           documentHeader.setFinancialDocumentStatusCode(
                   BudgetConstructionConstants.BUDGET_CONSTRUCTION_DOCUMENT_INITIAL_STATUS);
           getPersistenceBrokerTemplate().store(documentHeader);
        }
        LOG.info(String.format("\nBudget documents read for %d: %d",
                               RequestYear,bCDocumentsRead));
        LOG.info(String.format("\nBudget documents routed for %d: %d",
                 RequestYear,bCDocumentsRouted));
    }
    
    public void setDocumentService(DocumentService documentService)
    {
        this.documentService = documentService;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }
    public void setWorkflowDocumentService(WorkflowDocumentService workflowDocumentService)
    {
        this.workflowDocumentService = workflowDocumentService;
    }
}