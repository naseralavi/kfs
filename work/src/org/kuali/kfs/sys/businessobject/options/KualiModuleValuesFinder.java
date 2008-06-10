/*
 * Copyright 2007 The Kuali Foundation.
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
package org.kuali.kfs.lookup.keyvalues;

import java.util.ArrayList;
import java.util.List;

import org.kuali.core.KualiModule;
import org.kuali.core.lookup.keyvalues.KeyValuesBase;
import org.kuali.core.service.KualiModuleService;
import org.kuali.core.web.ui.KeyLabelPair;
import org.kuali.kfs.context.SpringContext;

/**
 * Value Finder for Units Of Measure.
 */
public class KualiModuleValuesFinder extends KeyValuesBase {

    /**
     * Returns code/description pairs of all Units Of Measure.
     * 
     * @see org.kuali.core.lookup.keyvalues.KeyValuesFinder#getKeyValues()
     */
    public List<KeyLabelPair> getKeyValues() {
        KualiModuleService moduleService = SpringContext.getBean(KualiModuleService.class);
        List<KualiModule> results = moduleService.getInstalledModules();
        List<KeyLabelPair> labels = new ArrayList<KeyLabelPair>( results.size() );
        labels.add(new KeyLabelPair("", ""));
        for (KualiModule module : results) {
            labels.add(new KeyLabelPair(module.getModuleCode(), module.getModuleName()));
        }
        return labels;
    }
}
