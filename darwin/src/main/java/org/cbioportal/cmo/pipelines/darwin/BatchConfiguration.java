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

package org.cbioportal.cmo.pipelines.darwin;

import org.cbioportal.cmo.pipelines.darwin.model.TimelineBrainSpineComposite;
import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactPatientDemographics;
import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactPatientIcdoRecord;
import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactTimelineBrainSpine;
import org.cbioportal.cmo.pipelines.darwin.model.MSK_ImpactClinicalBrainSpine;
import org.cbioportal.cmo.pipelines.darwin.model.StudyIDRecord;

import org.springframework.batch.core.*;
import org.springframework.batch.item.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.configuration.annotation.StepScope;
/**
 *
 * @author jake
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    public static final String MSK_IMPACT_JOB = "msk_ImpactJob";
    public static final String STUDYID_JOB = "studyIDJob";
      
    @Autowired
    public JobBuilderFactory jobBuilderFactory;
    
    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job msk_ImpactJob(){
        return jobBuilderFactory.get(MSK_IMPACT_JOB)
                .start(stepDPD())
                .next(stepDTBS())
                //.next(stepDPIR())
                .next(stepDCBS())
                .build();
    }
    
    @Bean
    public Step stepDPD(){
        return stepBuilderFactory.get("stepDPD")
                .<MSK_ImpactPatientDemographics, String> chunk(10)
                .reader(readerDPD())
                .processor(processorDPD())
                .writer(writerDPD())
                .build();
    }
    
    @Bean
    @StepScope
    public ItemStreamReader<MSK_ImpactPatientDemographics> readerDPD(){
        return new MSK_ImpactPatientDemographicsReader();
    }
    
    
    
    @Bean
    public MSK_ImpactPatientDemographicsProcessor processorDPD()
    {
        return new MSK_ImpactPatientDemographicsProcessor();
    }
    
    @Bean
    @StepScope
    public ItemStreamWriter<String> writerDPD()
    {
        return new MSK_ImpactPatientDemographicsWriter();
    }
    
    @Bean
    public Step stepDTBS(){
        return stepBuilderFactory.get("stepDTBS")
                .<MSK_ImpactTimelineBrainSpine, String> chunk(10)
                .reader(readerDTBS())
                .processor(processorDTBS())
                .writer(writerDTBS())
                .build();
    }
    
    @Bean
    @StepScope
    public ItemStreamReader<MSK_ImpactTimelineBrainSpine> readerDTBS(){
        return new MSK_ImpactTimelineBrainSpineReader();
    }
    
    @Bean
    @StepScope
    public ItemProcessor processorDTBS(){
        return new MSK_ImpactTimelineBrainSpineCompositeProcessor();
    }
    
    @Bean
    @StepScope
    public ItemStreamWriter<String> writerDTBS(){
        return new MSK_ImpactTimelineBrainSpineCompositeWriter();
    }
    
    @Bean
    public Step stepDPIR(){
        return stepBuilderFactory.get("stepDPIR")
                .<MSK_ImpactPatientIcdoRecord, String> chunk(10)
                .reader(readerDPIR())
                .processor(processorDPIR())
                .writer(writerDPIR())
                .build();
    }
    
    @Bean
    @StepScope
    public ItemStreamReader<MSK_ImpactPatientIcdoRecord> readerDPIR(){
        return new MSK_ImpactPatientIcdoReader();
    }
    
    @Bean
    public MSK_ImpactPatientIcdoProcessor processorDPIR(){
        return new MSK_ImpactPatientIcdoProcessor();
    }
    
    @Bean
    @StepScope
    public ItemStreamWriter<String> writerDPIR(){
        return new MSK_ImpactPatientIcdoWriter();
    }
    
    @Bean
    public Step stepDCBS(){
        return stepBuilderFactory.get("stepDCBS")
                .<MSK_ImpactClinicalBrainSpine, String> chunk(10)
                .reader(readerDCBS())
                .processor(processorDCBS())
                .writer(writerDCBS())
                .build();
    }
    
    @Bean
    @StepScope
    public ItemStreamReader<MSK_ImpactClinicalBrainSpine> readerDCBS(){
        return new MSK_ImpactClinicalBrainSpineReader();
    }
    
    @Bean
    public MSK_ImpactClinicalBrainSpineProcessor processorDCBS(){
        return new MSK_ImpactClinicalBrainSpineProcessor();
    }
    
    @Bean
    @StepScope
    public ItemStreamWriter<String> writerDCBS(){
        return new MSK_ImpactClinicalBrainSpineWriter();
    }
    
}
