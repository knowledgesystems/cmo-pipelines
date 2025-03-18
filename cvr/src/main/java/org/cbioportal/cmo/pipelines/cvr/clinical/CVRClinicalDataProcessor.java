/*
 * Copyright (c) 2016, 2017, 2023, 2025 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.MskimpactSeqDate;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author heinsz
 */
public class CVRClinicalDataProcessor implements ItemProcessor<CVRClinicalRecord, CompositeClinicalRecord> {

    @Autowired
    private CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    Logger log = Logger.getLogger(CVRClinicalDataProcessor.class);

    @Override
    public CompositeClinicalRecord process(CVRClinicalRecord clinicalRecord) throws Exception {
        if (clinicalRecord == null) return null;
        List<String> record = new ArrayList<>();
        List<String> seqDateRecord = new ArrayList<>();
        for (String field : CVRClinicalRecord.getFieldNames()) {
            record.add(cvrUtilities.convertWhitespace(getFieldValue(clinicalRecord, field).toString().trim()));
        }
        for (String field : MskimpactSeqDate.getFieldNames()) {
            seqDateRecord.add(cvrUtilities.convertWhitespace(getFieldValue(clinicalRecord, field).toString().trim()));
        }
        CompositeClinicalRecord compRecord = new CompositeClinicalRecord();
        compRecord.setClinicalRecord(String.join("\t", record));
        compRecord.setSeqDateRecord(String.join("\t", seqDateRecord));
        return compRecord;
    }

    private String getFieldValue(CVRClinicalRecord record, String field) {
        switch (field) {
            case "SAMPLE_ID": return record.getSAMPLE_ID();
            case "PATIENT_ID": return record.getPATIENT_ID();
            case "CANCER_TYPE": return record.getCANCER_TYPE();
            case "SAMPLE_TYPE": return record.getSAMPLE_TYPE();
            case "SAMPLE_CLASS": return record.getSAMPLE_CLASS();
            case "METASTATIC_SITE": return record.getMETASTATIC_SITE();
            case "PRIMARY_SITE": return record.getPRIMARY_SITE();
            case "CANCER_TYPE_DETAILED": return record.getCANCER_TYPE_DETAILED();
            case "GENE_PANEL": return record.getGENE_PANEL();
            case "OTHER_PATIENT_ID": return record.getOTHER_PATIENT_ID();
            case "SO_COMMENTS": return record.getSO_COMMENTS();
            case "SAMPLE_COVERAGE": return record.getSAMPLE_COVERAGE();
            case "CYCLE_THRESHOLD": return record.getCYCLE_THRESHOLD();
            case "TUMOR_PURITY": return record.getTUMOR_PURITY();
            case "ONCOTREE_CODE": return record.getONCOTREE_CODE();
            case "PARTA_CONSENTED_12_245": return record.getPARTA_CONSENTED_12_245();
            case "PARTC_CONSENTED_12_245": return record.getPARTC_CONSENTED_12_245();
            case "MSI_COMMENT": return record.getMSI_COMMENT();
            case "MSI_SCORE": return record.getMSI_SCORE();
            case "MSI_TYPE": return record.getMSI_TYPE();
            case "INSTITUTE": return record.getINSTITUTE();
            case "SOMATIC_STATUS": return record.getSOMATIC_STATUS();
            case "ARCHER": return record.getARCHER();
            case "CVR_TMB_COHORT_PERCENTILE": return record.getCVR_TMB_COHORT_PERCENTILE();
            case "CVR_TMB_SCORE": return record.getCVR_TMB_SCORE();
            case "CVR_TMB_TT_COHORT_PERCENTILE": return record.getCVR_TMB_TT_COHORT_PERCENTILE();
            case "PATH_SLIDE_EXISTS": return record.getPATH_SLIDE_EXISTS();
            case "MSK_SLIDE_ID": return record.getMSK_SLIDE_ID();
            case "SEQ_DATE": return record.getSEQ_DATE();
            default: return "";
        }
    }
}
