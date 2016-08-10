/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal CMO-Pipelines.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.cbioportal.cmo.pipelines.foundation;

import org.cbioportal.cmo.pipelines.foundation.model.CaseType;
import org.cbioportal.cmo.pipelines.foundation.model.CopyNumberAlterationType;
import org.cbioportal.cmo.pipelines.foundation.util.FoundationUtils;

import java.util.*;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.*;

/**
 *
 * @author ochoaa
 */
public class CnaDataReader implements ItemStreamReader<String> {
    
    private List<String> foundationCnaRowData;
    private static final Log LOG = LogFactory.getLog(CnaDataReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        Map<String, CaseType>  fmiCaseMap = (Map<String, CaseType> ) executionContext.get("fmiCaseMap");
        this.foundationCnaRowData = generateCnaRowData(fmiCaseMap);
    }

    /**
     * Generate map of gene to CNA data 
     * @param fmiCaseMap
     * @return 
     */
    private List<String> generateCnaRowData(Map<String, CaseType> fmiCaseMap) {
        Set<String> geneList = new HashSet<>();
        MultiKeyMap cnaMap = new MultiKeyMap(); 
        
        int noCnaCount = 0; // keep track of how many cases don't have copy number data
        for (CaseType ct : fmiCaseMap.values()) {
            List<CopyNumberAlterationType> cnaTypeList = ct.getVariantReport().getCopyNumberAlterations().getCopyNumberAlteration();
            if (cnaTypeList != null) {
                for (CopyNumberAlterationType cnaType : cnaTypeList) {
                    cnaMap.put(cnaType.getGene(), ct.getCase(), FoundationUtils.resolveCnaType(cnaType));
                    geneList.add(cnaType.getGene());
                }
            }
            else {
                noCnaCount++;
            }            
        }
        if (noCnaCount > 0) {
            LOG.info("Number of cases without CNA data: " + noCnaCount);
        }
        
        // format row data for CNA file                
        List<String> cnaRowData = new ArrayList();
        for (String gene : geneList) {            
            List<String> geneCnaData = new ArrayList();
            geneCnaData.add(gene);
            for (String caseId : fmiCaseMap.keySet()) {
                if (cnaMap.containsKey(gene, caseId)) {
                    geneCnaData.add((String) cnaMap.get(gene, caseId));
                }
                else {
                    geneCnaData.add("0");
                }
                
            }
            cnaRowData.add(StringUtils.join(geneCnaData, "\t"));            
        }
        
        return cnaRowData;
    }
        
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!foundationCnaRowData.isEmpty()) {
            return foundationCnaRowData.remove(0);
        }
        return null;
    }
    
}
