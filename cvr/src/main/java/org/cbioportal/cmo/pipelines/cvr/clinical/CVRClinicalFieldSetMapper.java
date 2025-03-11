/*
 * Copyright (c) 2016 - 2017, 2025 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import java.util.List;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 *
 * @author heinsz
 */
public class CVRClinicalFieldSetMapper implements  FieldSetMapper<CVRClinicalRecord> {

    Logger log = Logger.getLogger(CVRClinicalFieldSetMapper.class);

    @Override
    public CVRClinicalRecord mapFieldSet(FieldSet fs) throws BindException {
        CVRClinicalRecord record = new CVRClinicalRecord();
        //List<String> fields = CVRClinicalRecord.getFieldNames();
        String[] fields = fs.getNames(); // get the names in the order they are in in the file so the field names match the values
        String sampleId = fs.readString(0);
        if ("P-0055905-T02-IM7".equals(sampleId)) {
            log.info("READING THE CLINICAL FILE AND FOUND...");
            log.info("Found: '" + sampleId + "'");
            log.info("Number of fields: " + fs.getFieldCount());
            log.info("Field at 0: '" + fields[0] + "'");
            log.info("Value at 0: '" + fs.readString(0) + "'");
            log.info("Field at 1: '" + fields[1] + "'");
            log.info("Value at 1: '" + fs.readString(1) + "'");
            log.info("Field at 2: '" + fields[2] + "'");
            log.info("Value at 2: '" + fs.readString(2) + "'");
            log.info("Field at 3: '" + fields[3] + "'");
            log.info("Value at 3: '" + fs.readString(3) + "'");
            log.info("Field at 4: '" + fields[4] + "'");
            log.info("Value at 4: '" + fs.readString(4) + "'");
            log.info("Field at 5: '" + fields[5] + "'");
            log.info("Value at 5: '" + fs.readString(5) + "'");
            log.info("Field at 6: '" + fields[6] + "'");
            log.info("Value at 6: '" + fs.readString(6) + "'");
            log.info("Field at 7: '" + fields[7] + "'");
            log.info("Value at 7: '" + fs.readString(7) + "'");
            log.info("Field at 8: '" + fields[8] + "'");
            log.info("Value at 8: '" + fs.readString(8) + "'");
            log.info("Field at 9: '" + fields[9] + "'");
            log.info("Value at 9: '" + fs.readString(9) + "'");
            log.info("Field at 10: '" + fields[10] + "'");
            log.info("Value at 10: '" + fs.readString(10) + "'");
            log.info("Field at 11: '" + fields[11] + "'");
            log.info("Value at 11: '" + fs.readString(11) + "'");
            log.info("Field at 21: '" + fields[21] + "'");
            //log.info("Value at 21: '" + fs.readString(21) + "'");
        }
        /*if ("P-0055905-T02-IM7".equals(sampleId)) {
            log.info("READING THE CLINICAL FILE AND FOUND...");
            log.info("Found: '" + sampleId + "'");
            log.info("Number of fields: " + fs.getFieldCount());
            log.info("Field at 0: '" + fields.get(0) + "'");
            log.info("Value at 0: '" + fs.readString(0) + "'");
            log.info("Field at 1: '" + fields.get(1) + "'");
            log.info("Value at 1: '" + fs.readString(1) + "'");
            log.info("Field at 2: '" + fields.get(2) + "'");
            log.info("Value at 2: '" + fs.readString(2) + "'");
            log.info("Field at 3: '" + fields.get(3) + "'");
            log.info("Value at 3: '" + fs.readString(3) + "'");
            log.info("Field at 4: '" + fields.get(4) + "'");
            log.info("Value at 4: '" + fs.readString(4) + "'");
            log.info("Field at 5: '" + fields.get(5) + "'");
            log.info("Value at 5: '" + fs.readString(5) + "'");
            log.info("Field at 6: '" + fields.get(6) + "'");
            log.info("Value at 6: '" + fs.readString(6) + "'");
            log.info("Field at 7: '" + fields.get(7) + "'");
            log.info("Value at 7: '" + fs.readString(7) + "'");
            log.info("Field at 8: '" + fields.get(8) + "'");
            log.info("Value at 8: '" + fs.readString(8) + "'");
            log.info("Field at 9: '" + fields.get(9) + "'");
            log.info("Value at 9: '" + fs.readString(9) + "'");
            log.info("Field at 10: '" + fields.get(10) + "'");
            log.info("Value at 10: '" + fs.readString(10) + "'");
            log.info("Field at 11: '" + fields.get(11) + "'");
            log.info("Value at 11: '" + fs.readString(11) + "'");
            log.info("Field at 21: '" + fields.get(21) + "'");
            //log.info("Value at 21: '" + fs.readString(21) + "'");
        }*/
        for (int i = 0; i < fields.length; i++) {
            setFieldValue(record, fields[i], fs.readString(i));
        }
        return record;
    }

    /*
     * Sets the field value in the CVRClinicalRecord.
     */
    private void setFieldValue(CVRClinicalRecord record, String field, String value) {
        switch (field) {
            case "SAMPLE_ID":
                record.setSAMPLE_ID(value);
                break;
            case "PATIENT_ID":
                record.setPATIENT_ID(value);
                break;
            case "CANCER_TYPE":
                record.setCANCER_TYPE(value);
                break;
            case "SAMPLE_TYPE":
                record.setSAMPLE_TYPE(value);
                break;
            case "SAMPLE_CLASS":
                record.setSAMPLE_CLASS(value);
                break;
            case "METASTATIC_SITE":
                record.setMETASTATIC_SITE(value);
                break;
            case "PRIMARY_SITE":
                record.setPRIMARY_SITE(value);
                break;
            case "CANCER_TYPE_DETAILED":
                record.setCANCER_TYPE_DETAILED(value);
                break;
            case "GENE_PANEL":
                record.setGENE_PANEL(value);
                break;
            case "OTHER_PATIENT_ID":
                record.setOTHER_PATIENT_ID(value);
                break;
            case "SO_COMMENTS":
                record.setSO_COMMENTS(value);
                break;
            case "SAMPLE_COVERAGE":
                record.setSAMPLE_COVERAGE(value);
                break;
            case "CYCLE_THRESHOLD":
                record.setCYCLE_THRESHOLD(value);
                break;
            case "TUMOR_PURITY":
                record.setTUMOR_PURITY(value);
                break;
            case "ONCOTREE_CODE":
                record.setONCOTREE_CODE(value);
                break;
            case "PARTA_CONSENTED_12_245":
                record.setPARTA_CONSENTED_12_245(value);
                break;
            case "PARTC_CONSENTED_12_245":
                record.setPARTC_CONSENTED_12_245(value);
                break;
            case "MSI_COMMENT":
                record.setMSI_COMMENT(value);
                break;
            case "MSI_SCORE":
                record.setMSI_SCORE(value);
                break;
            case "MSI_TYPE":
                record.setMSI_TYPE(value);
                break;
            case "INSTITUTE":
                record.setINSTITUTE(value);
                break;
            case "SOMATIC_STATUS":
                record.setSOMATIC_STATUS(value);
                break;
            case "ARCHER":
                record.setARCHER(value);
                break;
            case "CVR_TMB_COHORT_PERCENTILE":
                record.setCVR_TMB_COHORT_PERCENTILE(value);
                break;
            case "CVR_TMB_SCORE":
                record.setCVR_TMB_SCORE(value);
                break;
            case "CVR_TMB_TT_COHORT_PERCENTILE":
                record.setCVR_TMB_TT_COHORT_PERCENTILE(value);
                break;
            case "PATH_SLIDE_EXISTS":
                record.setPATH_SLIDE_EXISTS(value);
                break;
            case "MSK_SLIDE_ID":
                record.setMSK_SLIDE_ID(value);
                break;
            default:
                log.info("No set method exists for '" + field + "'");
                break;
        }
    }
}
