/*
 * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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
 * This file is part of cBioPortal.
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

package org.cbioportal.cmo.pipelines.importer.config;

import org.springframework.context.annotation.*;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 *This is where all of the data type meta data is going to go 
 * @author ochoaa
 */
@Configuration
public class MetadataConfiguration {
    
    @Bean(name="clinicalMetadata")
    public MultiKeyMap clinicalMetadata() {
        MultiKeyMap clinicalMetadataMap = new MultiKeyMap();
        
        // set up clinical step metadata
        clinicalMetadataMap.put("clinicalStep", "meta_filename", "meta_clinical.txt");
        clinicalMetadataMap.put("clinicalStep", "attribute_type", "MIXED");
        clinicalMetadataMap.put("clinicalStep", "data_filename", "data_clinical.txt");
        
        // set up clinical patient step metadata
        clinicalMetadataMap.put("clinicalPatientStep", "meta_filename", "meta_clinical_patient.txt");
        clinicalMetadataMap.put("clinicalPatientStep", "attribute_type", "PATIENT");
        clinicalMetadataMap.put("clinicalPatientStep", "data_filename", "data_clinical_patient.txt");
        
        // set up clinical sample step metadata
        clinicalMetadataMap.put("clinicalSampleStep", "meta_filename", "meta_clinical_sample.txt");
        clinicalMetadataMap.put("clinicalSampleStep", "attribute_type", "SAMPLE");
        clinicalMetadataMap.put("clinicalSampleStep", "data_filename", "data_clinical_sample.txt");        
        
        return clinicalMetadataMap;
    }

}
