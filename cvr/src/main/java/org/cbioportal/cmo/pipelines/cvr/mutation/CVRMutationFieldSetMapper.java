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

package org.cbioportal.cmo.pipelines.cvr.mutation;

import org.cbioportal.models.MutationRecord;
import java.util.*;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 *
 * @author heinsz
 */
public class CVRMutationFieldSetMapper implements FieldSetMapper<MutationRecord> {

    @Override
    public MutationRecord mapFieldSet(FieldSet fs) throws BindException {
        MutationRecord record = new MutationRecord();
        Set<String> names = new HashSet(Arrays.asList(fs.getNames()));
        names.addAll(record.getHeader());

        for (String field : names) {
            String value = fs.readRawString(field);
            setFieldValue(record, field, value);
        }
        return record;
    }

    /**
     * Sets the field value in the MutationRecord.
     */
    private void setFieldValue(MutationRecord record, String field, String value) {
        switch (field.toUpperCase()) {
            case "HUGO_SYMBOL":
                record.setHUGO_SYMBOL(value);
                break;
            case "ENTREZ_GENE_ID":
                record.setENTREZ_GENE_ID(value);
                break;
            case "CENTER":
                record.setCENTER(value);
                break;
            case "NCBI_BUILD":
                record.setNCBI_BUILD(value);
                break;
            case "CHROMOSOME":
                record.setCHROMOSOME(value);
                break;
            case "START_POSITION":
                record.setSTART_POSITION(value);
                break;
            case "END_POSITION":
                record.setEND_POSITION(value);
                break;
            case "STRAND":
                record.setSTRAND(value);
                break;
            case "VARIANT_CLASSIFICATION":
                record.setVARIANT_CLASSIFICATION(value);
                break;
            case "VARIANT_TYPE":
                record.setVARIANT_TYPE(value);
                break;
            case "REFERENCE_ALLELE":
                record.setREFERENCE_ALLELE(value);
                break;
            case "TUMOR_SEQ_ALLELE1":
                record.setTUMOR_SEQ_ALLELE1(value);
                break;
            case "TUMOR_SEQ_ALLELE2":
                record.setTUMOR_SEQ_ALLELE2(value);
                break;
            case "DBSNP_RS":
                record.setDBSNP_RS(value);
                break;
            case "DBSNP_VAL_STATUS":
                record.setDBSNP_VAL_STATUS(value);
                break;
            case "TUMOR_SAMPLE_BARCODE":
                record.setTUMOR_SAMPLE_BARCODE(value);
                break;
            case "MATCHED_NORM_SAMPLE_BARCODE":
                record.setMATCHED_NORM_SAMPLE_BARCODE(value);
                break;
            case "MATCH_NORM_SEQ_ALLELE1":
                record.setMATCH_NORM_SEQ_ALLELE1(value);
                break;
            case "MATCH_NORM_SEQ_ALLELE2":
                record.setMATCH_NORM_SEQ_ALLELE2(value);
                break;
            case "TUMOR_VALIDATION_ALLELE1":
                record.setTUMOR_VALIDATION_ALLELE1(value);
                break;
            case "TUMOR_VALIDATION_ALLELE2":
                record.setTUMOR_VALIDATION_ALLELE2(value);
                break;
            case "MATCH_NORM_VALIDATION_ALLELE1":
                record.setMATCH_NORM_VALIDATION_ALLELE1(value);
                break;
            case "MATCH_NORM_VALIDATION_ALLELE2":
                record.setMATCH_NORM_VALIDATION_ALLELE2(value);
                break;
            case "VERIFICATION_STATUS":
                record.setVERIFICATION_STATUS(value);
                break;
            case "VALIDATION_STATUS":
                record.setVALIDATION_STATUS(value);
                break;
            case "MUTATION_STATUS":
                record.setMUTATION_STATUS(value);
                break;
            case "SEQUENCING_PHASE":
                record.setSEQUENCING_PHASE(value);
                break;
            case "SEQUENCE_SOURCE":
                record.setSEQUENCE_SOURCE(value);
                break;
            case "VALIDATION_METHOD":
                record.setVALIDATION_METHOD(value);
                break;
            case "SCORE":
                record.setSCORE(value);
                break;
            case "BAM_FILE":
                record.setBAM_FILE(value);
                break;
            case "SEQUENCER":
                record.setSEQUENCER(value);
                break;
            case "TUMOR_SAMPLE_UUID":
                record.setTUMOR_SAMPLE_UUID(value);
                break;
            case "MATCHED_NORM_SAMPLE_UUID":
                record.setMATCHED_NORM_SAMPLE_UUID(value);
                break;
            case "T_REF_COUNT":
                record.setT_REF_COUNT(value);
                break;
            case "T_ALT_COUNT":
                record.setT_ALT_COUNT(value);
                break;
            case "N_REF_COUNT":
                record.setN_REF_COUNT(value);
                break;
            case "N_ALT_COUNT":
                record.setN_ALT_COUNT(value);
                break;
            default:
                record.addAdditionalProperty(field, value);
                break;
        }
    }
}
