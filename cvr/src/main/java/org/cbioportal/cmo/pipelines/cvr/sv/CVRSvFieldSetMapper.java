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

package org.cbioportal.cmo.pipelines.cvr.sv;

import java.util.List;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 *
 * @author heinsz
 */
public class CVRSvFieldSetMapper implements  FieldSetMapper<CVRSvRecord> {

    Logger log = Logger.getLogger(CVRSvFieldSetMapper.class);

    @Override
    public CVRSvRecord mapFieldSet(FieldSet fs) throws BindException {
        CVRSvRecord record = new CVRSvRecord();
        List<String> fields = CVRSvRecord.getFieldNames();
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            if (field == "Class") {
                field = "SV_Class";
            }
            String value = (i < fs.getFieldCount()) ? fs.readString(i).trim() : "";  // default to empty string if out of bounds
            setFieldValue(record, field, value);
        }
        return record;
    }

    /*
     * Sets the field value in the CVRSvRecord.
     */
    private void setFieldValue(CVRSvRecord record, String field, String value) {
        switch (field) {
            case "Sample_ID":
                record.setSample_ID(value);
                break;
            case "SV_Status":
                record.setSV_Status(value);
                break;
            case "Site1_Hugo_Symbol":
                record.setSite1_Hugo_Symbol(value);
                break;
            case "Site2_Hugo_Symbol":
                record.setSite2_Hugo_Symbol(value);
                break;
            case "Site1_Ensembl_Transcript_Id":
                record.setSite1_Ensembl_Transcript_Id(value);
                break;
            case "Site2_Ensembl_Transcript_Id":
                record.setSite2_Ensembl_Transcript_Id(value);
                break;
            case "Site1_Entrez_Gene_Id":
                record.setSite1_Entrez_Gene_Id(value);
                break;
            case "Site2_Entrez_Gene_Id":
                record.setSite2_Entrez_Gene_Id(value);
                break;
            case "Site1_Region_Number":
                record.setSite1_Region_Number(value);
                break;
            case "Site2_Region_Number":
                record.setSite2_Region_Number(value);
                break;
            case "Site1_Region":
                record.setSite1_Region(value);
                break;
            case "Site2_Region":
                record.setSite2_Region(value);
                break;
            case "Site1_Chromosome":
                record.setSite1_Chromosome(value);
                break;
            case "Site2_Chromosome":
                record.setSite2_Chromosome(value);
                break;
            case "Site1_Contig":
                record.setSite1_Contig(value);
                break;
            case "Site2_Contig":
                record.setSite2_Contig(value);
                break;
            case "Site1_Position":
                record.setSite1_Position(value);
                break;
            case "Site2_Position":
                record.setSite2_Position(value);
                break;
            case "Site1_Description":
                record.setSite1_Description(value);
                break;
            case "Site2_Description":
                record.setSite2_Description(value);
                break;
            case "Site2_Effect_On_Frame":
                record.setSite2_Effect_On_Frame(value);
                break;
            case "NCBI_Build":
                record.setNCBI_Build(value);
                break;
            case "SV_Class":
                record.setSV_Class(value);
                break;
            case "Tumor_Split_Read_Count":
                record.setTumor_Split_Read_Count(value);
                break;
            case "Tumor_Paired_End_Read_Count":
                record.setTumor_Paired_End_Read_Count(value);
                break;
            case "Event_Info":
                record.setEvent_Info(value);
                break;
            case "Breakpoint_Type":
                record.setBreakpoint_Type(value);
                break;
            case "Connection_Type":
                record.setConnection_Type(value);
                break;
            case "Annotation":
                record.setAnnotation(value);
                break;
            case "DNA_Support":
                record.setDNA_Support(value);
                break;
            case "RNA_Support":
                record.setRNA_Support(value);
                break;
            case "SV_Length":
                record.setSV_Length(value);
                break;
            case "Normal_Read_Count":
                record.setNormal_Read_Count(value);
                break;
            case "Tumor_Read_Count":
                record.setTumor_Read_Count(value);
                break;
            case "Normal_Variant_Count":
                record.setNormal_Variant_Count(value);
                break;
            case "Tumor_Variant_Count":
                record.setTumor_Variant_Count(value);
                break;
            case "Normal_Paired_End_Read_Count":
                record.setNormal_Paired_End_Read_Count(value);
                break;
            case "Normal_Split_End_Read_Count":
                record.setNormal_Split_End_Read_Count(value);
                break;
            case "Comments":
                record.setComments(value);
                break;
            default:
                log.info("No set method exists for " + field);
                break;
        }
    }
}
