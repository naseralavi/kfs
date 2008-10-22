/*
 * Copyright 2008 The Kuali Foundation.
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
package org.kuali.kfs.pdp.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.pdp.PdpPropertyConstants;
import org.kuali.kfs.pdp.businessobject.CustomerProfile;
import org.kuali.kfs.pdp.businessobject.DisbursementNumberRange;
import org.kuali.kfs.pdp.businessobject.FormatProcess;
import org.kuali.kfs.pdp.businessobject.FormatSelection;
import org.kuali.kfs.pdp.service.FormatProcessService;
import org.kuali.rice.kns.bo.user.UniversalUser;
import org.kuali.rice.kns.service.BusinessObjectService;

/**
 * This class provides methods for the format process.
 */
public class FormatProcessServiceImpl implements FormatProcessService {
    
    private BusinessObjectService businessObjectService;
    
    /**
     * @see org.kuali.kfs.pdp.service.FormatProcessService#getDataForFormat(org.kuali.rice.kns.bo.user.UniversalUser)
     */
    public FormatSelection getDataForFormat(UniversalUser user) {
        String campusCode = user.getCampusCode();
        Date formatStartDate = getFormatProcessStartDate(campusCode);

        FormatSelection formatSelection = new FormatSelection();
        formatSelection.setCampus(campusCode);
        formatSelection.setStartDate(formatStartDate);
        
        //if format process not started yet populate the other data as well
        if (formatStartDate == null) {
            formatSelection.setCustomerList((List)this.businessObjectService.findAll(CustomerProfile.class));
            formatSelection.setRangeList((List) this.businessObjectService.findAll(DisbursementNumberRange.class));
        }

        return formatSelection;
    }
    
   
    /**
     * @see org.kuali.kfs.pdp.service.FormatProcessService#getFormatProcessStartDate(java.lang.String)
     */
    public Date getFormatProcessStartDate(String campus) {
        Map primaryKeys = new HashMap();
        primaryKeys.put(PdpPropertyConstants.PHYS_CAMPUS_PROCESS_CODE, campus);
        FormatProcess fp = (FormatProcess) this.businessObjectService.findByPrimaryKey(FormatProcess.class, primaryKeys);
        
        if (fp != null) {
            return new Date(fp.getBeginFormat().getTime());
        }
        else {
            return null;
        }
    }
  
    /**
     * Sets the business object service
     * 
     * @param businessObjectService
     */
    public void setBusinessObjectService(BusinessObjectService businessObjectService) {
        this.businessObjectService = businessObjectService;
    }



}
