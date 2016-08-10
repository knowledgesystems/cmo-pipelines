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

import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for meta staging files.
 * @author ochoaa
 */
@Configuration
public class MetaDataConfiguration {

    @Bean(name="mutationsMetaData")
    public Map<String, String> mutationsMetaData() {
        Map<String, String> mutationsMap = new LinkedHashMap<>();
        mutationsMap.put("cancer_study_identifier", "<study_id>");
        mutationsMap.put("stable_id", "<study_id>_mutations");
        mutationsMap.put("genetic_alteration_type", "MUTATION_EXTENDED");
        mutationsMap.put("show_profile_in_analysis_tab", "true");
        mutationsMap.put("datatype", "MAF");
        mutationsMap.put("profile_name", "Mutations.");
        mutationsMap.put("profile_description", "Foundation mutations data for <study_id>");
        mutationsMap.put("data_filename", "data_mutations_extended.txt");
        
        return mutationsMap;
    }
    
    @Bean(name="cnaMetaData")
    public Map<String, String> cnaMetaData() {
        Map<String, String> cnaMap = new LinkedHashMap<>();
        cnaMap.put("cancer_study_identifier", "<study_id>");
        cnaMap.put("stable_id", "<study_id>_cna");
        cnaMap.put("genetic_alteration_type", "COPY_NUMBER_ALTERATION");
        cnaMap.put("show_profile_in_analysis_tab", "true");
        cnaMap.put("datatype", "DISCRETE");
        cnaMap.put("profile_name", "Putative copy-number from GISTIC");
        cnaMap.put("profile_description", "Putative copy-number from GISTIC 2.0. Values: -2 = homozygous deletion; -1 = hemizygous deletion; 0 = neutral / no change; 1 = gain; 2 = high level amplification");
        cnaMap.put("data_filename", "data_CNA.txt");
        
        return cnaMap;
    }    
    
    @Bean(name="fusionsMetaData")
    public Map<String, String> fusionsMetaData() {
        Map<String, String> fusionsMap = new LinkedHashMap<>();
        fusionsMap.put("cancer_study_identifier", "<study_id>");
        fusionsMap.put("stable_id", "<study_id>_mutations");
        fusionsMap.put("genetic_alteration_type", "FUSION");
        fusionsMap.put("show_profile_in_analysis_tab", "true");
        fusionsMap.put("datatype", "FUSION");
        fusionsMap.put("profile_name", "Fusions.");
        fusionsMap.put("profile_description", "Foundation fusions data for <study_id>");
        fusionsMap.put("data_filename", "data_fusions.txt");
        
        return fusionsMap;
    }    
    
}
