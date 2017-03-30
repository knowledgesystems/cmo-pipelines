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

package org.mskcc.cmo.ks.darwin.pipeline;

import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspineclinical.MskimpactBrainSpineClinicalWriter;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspineclinical.MskimpactBrainSpineClinicalReader;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspineclinical.MskimpactBrainSpineClinicalProcessor;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline.MskimpactTimelineBrainSpineReader;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline.MskimpactTimelineBrainSpineModelToCompositeProcessor;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline.MskimpactTimelineBrainSpineCompositeToCompositeProcessor;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline.MskimpactTimelineBrainSpineWriter;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactdemographics.MskimpactPatientDemographicsProcessor;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactdemographics.MskimpactPatientDemographicsWriter;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactdemographics.MskimpactPatientDemographicsReader;
import org.mskcc.cmo.ks.darwin.pipeline.model.*;
import org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chanttimeline.Skcm_mskcc_2015_chantTimelineWriter;
import org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chanttimeline.Skcm_mskcc_2015_chantTimelineReader;
import org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chanttimeline.Skcm_mskcc_2015_chantTimelineProcessor;
import org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chantclinical.*;

import java.util.*;
import org.springframework.batch.core.*;
import org.springframework.batch.item.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
/**
 *
 * @author jake
 */
@Configuration
@EnableBatchProcessing
@ComponentScan(basePackages="org.mskcc.cmo.ks.redcap.source")
public class BatchConfiguration {

    public static final String MSKIMPACT_JOB = "mskimpactJob";
    public static final String MSK_JOB = "mskJob";
    public static final String SKCM_MSKCC_2015_CHANT_JOB = "skcm_mskcc_2015_chantJob";

    @Value("${darwin.chunk_size}")
    private Integer chunkSize;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public static enum BrainSpineTimelineType {
        STATUS("Status"),
        TREATMENT("Treatment"),
        SURGERY("Surgery"),
        SPECIMEN("Specimen"),
        IMAGING("Imaging");

        private String type;

        BrainSpineTimelineType(String type) {this.type = type;}
        public String toString() {return type;}
    }

    @Bean
    public Job mskimpactJob() {
        return jobBuilderFactory.get(MSKIMPACT_JOB)
                .start(mskimpactPatientDemographicsStep())
                .next(mskimpactTimelineBrainSpineStep())
                .next(mskimpactClinicalBrainSpineStep())
                .build();
    }

    @Bean
    public Job mskJob() {
        return jobBuilderFactory.get(MSK_JOB)
                .start(mskimpactPatientDemographicsStep())
                .build();
    }

    @Bean
    public Job skcm_mskcc_2015_chantJob() {
        return jobBuilderFactory.get(SKCM_MSKCC_2015_CHANT_JOB)
                .start(skcm_mskcc_2015_chantClinicalStep())
                .next(skcm_mskcc_2015_chantTimelineStep())
                .build();
    }

    @Bean
    public Step mskimpactPatientDemographicsStep() {
        return stepBuilderFactory.get("mskimpactPatientDemographicsStep")
                .<MskimpactPatientDemographics, String> chunk(chunkSize)
                .reader(mskimpactPatientDemographicsReader())
                .processor(mskimpactPatientDemographicsProcessor())
                .writer(mskimpactPatientDemographicsWriter())
                .build();
    }

    @Bean
    public Step mskimpactTimelineBrainSpineStep() {
        return stepBuilderFactory.get("mskimpactTimelineBrainSpineStep")
                .<MskimpactBrainSpineTimeline, MskimpactBrainSpineCompositeTimeline> chunk(chunkSize)
                .reader(mskimpactTimelineBrainSpineReader())
                .processor(mskimpactTimelineBrainSpineProcessor())
                .writer(mskimpactTimelineBrainSpineWriter())
                .build();
    }

    @Bean
    public Step mskimpactClinicalBrainSpineStep() {
        return stepBuilderFactory.get("mskimpactClinicalBrainSpineStep")
                .<MskimpactBrainSpineClinical, String> chunk(chunkSize)
                .reader(readerDarwinClinicalBrainSpine())
                .processor(processorDarwinClinicalBrainSpine())
                .writer(writerDarwinClinicalBrainSpine())
                .build();
    }

    @Bean
    public Step skcm_mskcc_2015_chantClinicalStep() {
        return stepBuilderFactory.get("skcm_mskcc_2015_chantClinicalStep")
                .<Skcm_mskcc_2015_chantClinicalRecord, String> chunk(chunkSize)
                .reader(skcm_mskcc_2015_chantClinicalReader())
                .processor(skcm_mskcc_2015_chantClinicalCompositeProcessor())
                .writer(skcm_mskcc_2015_chantClinicalCompositeWriter())
                .build();
    }

    @Bean
    public Step skcm_mskcc_2015_chantTimelineStep() {
        return stepBuilderFactory.get("skcm_mskcc_2015_chantTimelineStep")
                .<Skcm_mskcc_2015_chantTimelineRecord, String> chunk(chunkSize)
                .reader(skcm_mskcc_2015_chantTimelineReader())
                .processor(skcm_mskcc_2015_chantTimelineProcessor())
                .writer(skcm_mskcc_2015_chantTimelineWriter())
               .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<MskimpactPatientDemographics> mskimpactPatientDemographicsReader() {
        return new MskimpactPatientDemographicsReader();
    }



    @Bean
    public MskimpactPatientDemographicsProcessor mskimpactPatientDemographicsProcessor() {
        return new MskimpactPatientDemographicsProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> mskimpactPatientDemographicsWriter() {
        return new MskimpactPatientDemographicsWriter();
    }

    @Bean
    @StepScope
    public ItemStreamReader<MskimpactBrainSpineTimeline> mskimpactTimelineBrainSpineReader() {
        return new MskimpactTimelineBrainSpineReader();
    }

    @Bean
    @StepScope
    public CompositeItemProcessor mskimpactTimelineBrainSpineProcessor() {
        List<ItemProcessor> processorDelegates = new ArrayList<>();
        processorDelegates.add(new MskimpactTimelineBrainSpineModelToCompositeProcessor(BrainSpineTimelineType.STATUS));
        processorDelegates.add(new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.SPECIMEN));
        processorDelegates.add(new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.TREATMENT));
        processorDelegates.add(new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.IMAGING));
        processorDelegates.add(new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.SURGERY));
        CompositeItemProcessor processor = new CompositeItemProcessor<>();
        processor.setDelegates(processorDelegates);
        return processor;
    }

    @Bean
    @StepScope
    public ItemStreamWriter<MskimpactBrainSpineCompositeTimeline> statusWriter() {
        return new MskimpactTimelineBrainSpineWriter(BrainSpineTimelineType.STATUS);
    }

    @Bean
    @StepScope
    public ItemStreamWriter<MskimpactBrainSpineCompositeTimeline> specimenWriter() {
        return new MskimpactTimelineBrainSpineWriter(BrainSpineTimelineType.SPECIMEN);
    }

    @Bean
    @StepScope
    public ItemStreamWriter<MskimpactBrainSpineCompositeTimeline> surgeryWriter() {
        return new MskimpactTimelineBrainSpineWriter(BrainSpineTimelineType.SURGERY);
    }

    @Bean
    @StepScope
    public ItemStreamWriter<MskimpactBrainSpineCompositeTimeline> treatmentWriter() {
        return new MskimpactTimelineBrainSpineWriter(BrainSpineTimelineType.TREATMENT);
    }

    @Bean
    @StepScope
    public ItemStreamWriter<MskimpactBrainSpineCompositeTimeline> imagingWriter() {
        return new MskimpactTimelineBrainSpineWriter(BrainSpineTimelineType.IMAGING);
    }

    @Bean
    @StepScope
    public CompositeItemWriter<MskimpactBrainSpineCompositeTimeline> mskimpactTimelineBrainSpineWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(statusWriter());
        writerDelegates.add(surgeryWriter());
        writerDelegates.add(imagingWriter());
        writerDelegates.add(specimenWriter());
        writerDelegates.add(treatmentWriter());
        CompositeItemWriter writer = new CompositeItemWriter<>();
        writer.setDelegates(writerDelegates);
        return writer;
    }

    @Bean
    @StepScope
    public ItemStreamReader<MskimpactBrainSpineClinical> readerDarwinClinicalBrainSpine() {
        return new MskimpactBrainSpineClinicalReader();
    }

    @Bean
    public MskimpactBrainSpineClinicalProcessor processorDarwinClinicalBrainSpine() {
        return new MskimpactBrainSpineClinicalProcessor();
    }

    @Bean
    @StepScope
    public MskimpactBrainSpineClinicalWriter writerDarwinClinicalBrainSpine() {
        return new MskimpactBrainSpineClinicalWriter();
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantClinicalReader skcm_mskcc_2015_chantClinicalReader() {
        return new Skcm_mskcc_2015_chantClinicalReader();
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantClinicalSampleProcessor skcm_mskcc_2015_chantClinicalSampleProcessor() {
        return new Skcm_mskcc_2015_chantClinicalSampleProcessor();
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantClinicalPatientProcessor skcm_mskcc_2015_chantClinicalPatientProcessor() {
        return new Skcm_mskcc_2015_chantClinicalPatientProcessor();
    }

    @Bean
    @StepScope
    public CompositeItemProcessor skcm_mskcc_2015_chantClinicalCompositeProcessor() {
        List<ItemProcessor> processorDelegates = new ArrayList<>();
        processorDelegates.add(skcm_mskcc_2015_chantClinicalSampleProcessor());
        processorDelegates.add(skcm_mskcc_2015_chantClinicalPatientProcessor());
        CompositeItemProcessor processor = new CompositeItemProcessor<>();
        processor.setDelegates(processorDelegates);
        return processor;
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantClinicalSampleWriter skcm_mskcc_2015_chantClinicalSampleWriter() {
        return new Skcm_mskcc_2015_chantClinicalSampleWriter();
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantClinicalPatientWriter skcm_mskcc_2015_chantClinicalPatientWriter() {
        return new Skcm_mskcc_2015_chantClinicalPatientWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<Skcm_mskcc_2015_chantClinicalCompositeRecord> skcm_mskcc_2015_chantClinicalCompositeWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(skcm_mskcc_2015_chantClinicalSampleWriter());
        writerDelegates.add(skcm_mskcc_2015_chantClinicalPatientWriter());
        CompositeItemWriter writer = new CompositeItemWriter<>();
        writer.setDelegates(writerDelegates);
        return writer;
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantTimelineReader skcm_mskcc_2015_chantTimelineReader() {
        return new Skcm_mskcc_2015_chantTimelineReader();
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantTimelineProcessor skcm_mskcc_2015_chantTimelineProcessor() {
        return new Skcm_mskcc_2015_chantTimelineProcessor();
    }

    @Bean
    @StepScope
    public Skcm_mskcc_2015_chantTimelineWriter skcm_mskcc_2015_chantTimelineWriter() {
        return new Skcm_mskcc_2015_chantTimelineWriter();
    }

}
