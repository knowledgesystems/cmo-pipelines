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

import org.cbioportal.cmo.pipelines.importer.JobExecutionListenerImpl;
import org.cbioportal.cmo.pipelines.importer.config.tasklet.CancerStudyTasklet;

import org.apache.commons.logging.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

/**
 *
 * @author ochoaa
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    public static final String BATCH_IMPORTER_JOB = "batchImporterJob";
    public static final String IMPORT_CANCER_STUDY_JOB = "importCancerStudy";
    public static final String IMPORT_CLINICAL_DATA_JOB = "importClinicalDataJob";
    public static final String IMPORT_CAISES_CLINICAL_XML_JOB = "importCaisesClinicalXmlJob";
    public static final String IMPORT_TIMELINE_DATA_JOB = "importTimelineDataJob";
    public static final String IMPORT_PROFILE_DATA_JOB = "importProfileDataJob";
    public static final String IMPORT_COPY_NUMBER_SEGMENT_DATA_JOB = "importCopyNumberSegmentDataJob";
    public static final String IMPORT_GISTIC_DATA_JOB = "importGisticDataJob";
    public static final String IMPORT_MUT_SIG_DATA_JOB = "importMutSigDataJob";
        
    public static final String INITIALIZE_DB_JOB = "initializeDatabaseJob";
    
    @Autowired
    public JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    
    private static final Log LOG = LogFactory.getLog(BatchConfiguration.class);   
    
    @Bean
    public Job batchImporterJob() throws Exception {
        return jobBuilderFactory.get(BATCH_IMPORTER_JOB)
                .incrementer(new RunIdIncrementer())
                .listener(listener())
                .start(importCancerStudy())
                .build();
    }
    
    @Bean
    public JobExecutionListener listener() {
        return new JobExecutionListenerImpl();
    }

    
    @Bean
    public Step importCancerStudy() throws Exception {
        return stepBuilderFactory.get("importCancerStudy")
                .tasklet(cancerStudyTasklet())                
                .build();
    }
    
    
    @Bean
    @StepScope
    public Tasklet cancerStudyTasklet() {
        return new CancerStudyTasklet();
    }
    
}
