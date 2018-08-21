/*
 * Copyright (c) 2016-2017 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.crdb;

import org.mskcc.cmo.ks.crdb.model.*;
import org.mskcc.cmo.ks.crdb.pipeline.util.CRDBUtils;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

/**
 * Configuration for running the CRDB clinical data fetcher.
 *
 * @author ochoaa
 */

@Configuration
@EnableBatchProcessing
public class BatchConfiguration
{
    public static final String CRDB_IMPACT_JOB = "crdbImpactJob";
    public static final String CRDB_PDX_JOB = "crdbPDXJob";

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public CRDBUtils crdbUtils() {
        return new CRDBUtils();
    }

    @Bean
    public Job crdbImpactJob() {
        return jobBuilderFactory.get(CRDB_IMPACT_JOB)
            .start(crdbSurveyStep())
            .next(crdbDatasetStep())
            .build();
    }

    @Bean
    public Job crdbPDXJob() { 
        return jobBuilderFactory.get(CRDB_PDX_JOB)
            .start(crdbPDXClinicalSampleStep())
            .next(crdbPDXClinicalPatientStep())
            .next(crdbPDXTimelineStep())
            .next(crdbPDXSourceToDestinationMappingStep())
            .build();
    }

    /**
     * Step 1 reads, processes, and writes the CRDB Survey query results
     */
    @Bean
    public Step crdbSurveyStep() {
        return stepBuilderFactory.get("crdbSurveyStep")
            .<CRDBSurvey, String> chunk(10)
            .reader(crdbSurveyReader())
            .processor(crdbSurveyProcessor())
            .writer(crdbSurveyWriter())
            .build();
    }

    @Bean
    @StepScope
	public ItemStreamReader<CRDBSurvey> crdbSurveyReader() {
		return new CRDBSurveyReader();
	}

    @Bean
    public CRDBSurveyProcessor crdbSurveyProcessor() {
        return new CRDBSurveyProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> crdbSurveyWriter() {
        return new CRDBSurveyWriter();
    }

    /**
     * Step 2 reads, processes, and writes the CRDB Dataset query results
     */
    @Bean
    public Step crdbDatasetStep() {
        return stepBuilderFactory.get("crdbDatasetStep")
            .<CRDBDataset, String> chunk(10)
            .reader(crdbDatasetReader())
            .processor(crdbDatasetProcessor())
            .writer(crdbDatasetWriter())
            .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<CRDBDataset> crdbDatasetReader() {
	return new CRDBDatasetReader();
    }

    @Bean
    public CRDBDatasetProcessor crdbDatasetProcessor() {
        return new CRDBDatasetProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> crdbDatasetWriter() {
        return new CRDBDatasetWriter();
    }

    @Bean
    public Step crdbPDXSourceToDestinationMappingStep() {
        return stepBuilderFactory.get("crdbPDXSourceToDestinationMappingStep")
            .<CRDBPDXSourceToDestinationMapping, String> chunk(10)
            .reader(crdbPDXSourceToDestinationMappingReader())
            .processor(crdbPDXSourceToDestinationMappingProcessor())
            .writer(crdbPDXSourceToDestinationMappingWriter())
            .build();
    }
    
    @Bean
    @StepScope
    public ItemStreamReader<CRDBPDXSourceToDestinationMapping> crdbPDXSourceToDestinationMappingReader() {
        return new CRDBPDXSourceToDestinationMappingReader();
    }
    
    @Bean
    @StepScope
    public CRDBPDXSourceToDestinationMappingProcessor crdbPDXSourceToDestinationMappingProcessor() {
        return new CRDBPDXSourceToDestinationMappingProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> crdbPDXSourceToDestinationMappingWriter() {
        return new CRDBPDXSourceToDestinationMappingWriter();
    }
    
    @Bean
    public Step crdbPDXClinicalSampleStep() {
        return stepBuilderFactory.get("crdbPDXClinicalSampleStep")
            .<CRDBPDXClinicalSampleDataset, String> chunk(10)
            .reader(crdbPDXClinicalSampleReader())
            .processor(crdbPDXClinicalSampleProcessor())
            .writer(crdbPDXClinicalSampleWriter())
            .build();
    }
    
    @Bean
    @StepScope
    public ItemStreamReader<CRDBPDXClinicalSampleDataset> crdbPDXClinicalSampleReader() {
        return new CRDBPDXClinicalSampleReader();
    }
    
    @Bean
    @StepScope
    public CRDBPDXClinicalSampleProcessor crdbPDXClinicalSampleProcessor() {
        return new CRDBPDXClinicalSampleProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> crdbPDXClinicalSampleWriter() {
        return new CRDBPDXClinicalSampleWriter();
    }
    
    @Bean
    public Step crdbPDXClinicalPatientStep() {
        return stepBuilderFactory.get("crdbPDXClinicalPatientStep")
            .<CRDBPDXClinicalPatientDataset, String> chunk(10)
            .reader(crdbPDXClinicalPatientReader())
            .processor(crdbPDXClinicalPatientProcessor())
            .writer(crdbPDXClinicalPatientWriter())
            .build();
    } 
    
    @Bean
    @StepScope
    public ItemStreamReader<CRDBPDXClinicalPatientDataset> crdbPDXClinicalPatientReader() {
        return new CRDBPDXClinicalPatientReader();
    }
    
    @Bean
    @StepScope
    public CRDBPDXClinicalPatientProcessor crdbPDXClinicalPatientProcessor() {
        return new CRDBPDXClinicalPatientProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> crdbPDXClinicalPatientWriter() {
        return new CRDBPDXClinicalPatientWriter();
    }
    
    @Bean
    public Step crdbPDXTimelineStep() {
        return stepBuilderFactory.get("crdbPDXTimelineStep")
            .<CRDBPDXTimelineDataset, String> chunk(10)
            .reader(crdbPDXTimelineReader())
            .processor(crdbPDXTimelineProcessor())
            .writer(crdbPDXTimelineWriter())
            .build();
    }
    
    @Bean
    @StepScope
    public ItemStreamReader<CRDBPDXTimelineDataset> crdbPDXTimelineReader() {
        return new CRDBPDXTimelineReader();
    }
    
    @Bean
    @StepScope
    public CRDBPDXTimelineProcessor crdbPDXTimelineProcessor() {
        return new CRDBPDXTimelineProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> crdbPDXTimelineWriter() {
        return new CRDBPDXTimelineWriter();
    }
}

