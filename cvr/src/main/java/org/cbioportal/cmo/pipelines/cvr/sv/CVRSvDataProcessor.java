/*
 * Copyright (c) 2016, 2017, 2025 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.sv;

import java.util.*;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author heinsz
 */
public class CVRSvDataProcessor implements ItemProcessor<CVRSvRecord, String> {

    @Autowired
    private CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Override
    public String process(CVRSvRecord i) throws Exception {
        List<String> record = new ArrayList<>();
        for (String field : i.getFieldNames()) {
            String queryField = field;
            if (field == "Class") {
                queryField = "SV_Class";
            }
            record.add(cvrUtilities.convertWhitespace(getFieldValue(i, queryField).toString()));
        }
        return String.join("\t", record);
    }

    private String getFieldValue(CVRSvRecord record, String field) {
        switch (field) {
            case "Sample_ID": return record.getSample_ID();
            case "SV_Status": return record.getSV_Status();
            case "Site1_Hugo_Symbol": return record.getSite1_Hugo_Symbol();
            case "Site2_Hugo_Symbol": return record.getSite2_Hugo_Symbol();
            case "Site1_Ensembl_Transcript_Id": return record.getSite1_Ensembl_Transcript_Id();
            case "Site2_Ensembl_Transcript_Id": return record.getSite2_Ensembl_Transcript_Id();
            case "Site1_Entrez_Gene_Id": return record.getSite1_Entrez_Gene_Id();
            case "Site2_Entrez_Gene_Id": return record.getSite2_Entrez_Gene_Id();
            case "Site1_Region_Number": return record.getSite1_Region_Number();
            case "Site2_Region_Number": return record.getSite2_Region_Number();
            case "Site1_Region": return record.getSite1_Region();
            case "Site2_Region": return record.getSite2_Region();
            case "Site1_Chromosome": return record.getSite1_Chromosome();
            case "Site2_Chromosome": return record.getSite2_Chromosome();
            case "Site1_Contig": return record.getSite1_Contig();
            case "Site2_Contig": return record.getSite2_Contig();
            case "Site1_Position": return record.getSite1_Position();
            case "Site2_Position": return record.getSite2_Position();
            case "Site1_Description": return record.getSite1_Description();
            case "Site2_Description": return record.getSite2_Description();
            case "Site2_Effect_On_Frame": return record.getSite2_Effect_On_Frame();
            case "NCBI_Build": return record.getNCBI_Build();
            case "SV_Class": return record.getSV_Class();
            case "Tumor_Split_Read_Count": return record.getTumor_Split_Read_Count();
            case "Tumor_Paired_End_Read_Count": return record.getTumor_Paired_End_Read_Count();
            case "Event_Info": return record.getEvent_Info();
            case "Breakpoint_Type": return record.getBreakpoint_Type();
            case "Connection_Type": return record.getConnection_Type();
            case "Annotation": return record.getAnnotation();
            case "DNA_Support": return record.getDNA_Support();
            case "RNA_Support": return record.getRNA_Support();
            case "SV_Length": return record.getSV_Length();
            case "Normal_Read_Count": return record.getNormal_Read_Count();
            case "Tumor_Read_Count": return record.getTumor_Read_Count();
            case "Normal_Variant_Count": return record.getNormal_Variant_Count();
            case "Tumor_Variant_Count": return record.getTumor_Variant_Count();
            case "Normal_Paired_End_Read_Count": return record.getNormal_Paired_End_Read_Count();
            case "Normal_Split_End_Read_Count": return record.getNormal_Split_End_Read_Count();
            case "Comments": return record.getComments();
            default: return "";
        }
    }
}
