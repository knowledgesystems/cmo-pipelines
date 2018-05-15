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

package org.mskcc.cmo.ks.darwin.pipeline;

import org.mskcc.cmo.ks.darwin.pipeline.mskimpactdemographics.MskimpactAgeWriter;
import org.mskcc.cmo.ks.darwin.pipeline.model.*;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspineclinical.*;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactbrainspinetimeline.*;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactdemographics.*;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpactgeniepatient.*;
import org.mskcc.cmo.ks.darwin.pipeline.mskimpact_medicaltherapy.*;
import org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chantclinical.*;
import org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chanttimeline.*;
import org.mskcc.cmo.ks.darwin.pipeline.util.DarwinUtils;

import java.util.*;
import org.springframework.batch.core.*;
import org.springframework.batch.item.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.support.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 *
 * @author jake
 */
@Configuration
@EnableBatchProcessing
@ComponentScan(basePackages="org.mskcc.cmo.ks.redcap")
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

    @Bean
    public Job mskimpactJob() {
        return jobBuilderFactory.get(MSKIMPACT_JOB)
                .start(mskimpactPatientDemographicsStep())
                .next(mskimpactTimelineBrainSpineStep())
                .next(mskimpactClinicalBrainSpineStep())
            //.next(mskimpactMedicalTherapyStep())
                .next(mskimpactGeniePatientClinicalStep())
                .next(mskimpactEmailStep())
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
                .<MskimpactPatientDemographics, MskimpactCompositeDemographics> chunk(chunkSize)
                .reader(mskimpactPatientDemographicsReader())
                .processor(mskimpactPatientDemographicsProcessor())
                .writer(mskimpactCompositeDemographicsWriter())
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
    public Step mskimpactMedicalTherapyStep() {
        return stepBuilderFactory.get("mskimpactMedicalTherapyStep")
                .<List<MskimpactMedicalTherapy>, MskimpactMedicalTherapy> chunk(chunkSize)
                .reader(mskimpactMedicalTherapyReader())
                .processor(mskimpactMedicalTherapyProcessor())
                .writer(mskimpactMedicalTherapyCompositeWriter())
                .build();
    }

    @Bean
    public Step skcm_mskcc_2015_chantClinicalStep() {
        return stepBuilderFactory.get("skcm_mskcc_2015_chantClinicalStep")
                .<Skcm_mskcc_2015_chantNormalizedClinicalRecord, String> chunk(chunkSize)
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
    public Step mskimpactGeniePatientClinicalStep() {
        return stepBuilderFactory.get("mskimpactGeniePatientClinicalStep")
                .<MskimpactNAACCRClinical, String> chunk(chunkSize)
                .reader(mskimpactGeniePatientReader())
                .processor(mskimpactGeniePatientProcessor())
                .writer(mskimpactGeniePatientWriter())
                .build();
    }

    @Bean
    public Step mskimpactEmailStep() {
        return stepBuilderFactory.get("mskimpactEmailStep")
        .tasklet(darwinEmailTasklet())
        .build();
    }

    @Bean
    @StepScope
    public Tasklet darwinEmailTasklet() {
        return new DarwinEmailTasklet();
    }

    @Bean
    @StepScope
    public DarwinUtils darwinUtils() {
        return new DarwinUtils();
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
    public ItemStreamWriter<MskimpactCompositeDemographics> mskimpactPatientDemographicsWriter() {
        return new MskimpactPatientDemographicsWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<MskimpactCompositeDemographics> mskimpactAgeWriter() {
        return new MskimpactAgeWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<MskimpactCompositeDemographics> mskimpactVitalStatusWriter() {
        return new MskimpactVitalStatusWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<MskimpactCompositeDemographics> mskimpactCompositeDemographicsWriter() {
        CompositeItemWriter writer = new CompositeItemWriter();
        List<ItemWriter> delegates = new ArrayList<>();
        delegates.add(mskimpactPatientDemographicsWriter());
        delegates.add(mskimpactAgeWriter());
        delegates.add(mskimpactVitalStatusWriter());
        writer.setDelegates(delegates);
        return writer;
    }

    @Bean
    @StepScope
    public ItemStreamReader<MskimpactBrainSpineTimeline> mskimpactTimelineBrainSpineReader() {
        return new MskimpactTimelineBrainSpineReader();
    }

    @Bean
    @StepScope
    public ItemStreamReader<List<MskimpactMedicalTherapy>> mskimpactMedicalTherapyReader() {
        return new MskimpactMedicalTherapyReader();
    }

    @Bean
    public MskimpactMedicalTherapyProcessor mskimpactMedicalTherapyProcessor() {
        return new MskimpactMedicalTherapyProcessor();
    }

    @Bean
    @StepScope
    public MskimpactMedicalTherapyClinicalWriter mskimpactMedicalTherapyClinicalWriter() {
        return new MskimpactMedicalTherapyClinicalWriter();
    }

    @Bean
    @StepScope
    public MskimpactMedicalTherapyTimelineWriter mskimpactMedicalTherapyTimelineWriter() {
        return new MskimpactMedicalTherapyTimelineWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<MskimpactMedicalTherapy> mskimpactMedicalTherapyCompositeWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(mskimpactMedicalTherapyClinicalWriter());
        writerDelegates.add(mskimpactMedicalTherapyTimelineWriter());
        CompositeItemWriter writer = new CompositeItemWriter<>();
        writer.setDelegates(writerDelegates);
        return writer;
    }


    @Bean
    @StepScope
    public ItemStreamReader<MskimpactNAACCRClinical> mskimpactGeniePatientReader() {
        return new MskimpactGeniePatientReader();
    }

    @Bean
    public MskimpactGeniePatientProcessor mskimpactGeniePatientProcessor() {
        return new MskimpactGeniePatientProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> mskimpactGeniePatientWriter() {
        return new MskimpactGeniePatientWriter();
    }

    // Beans for CompositeItemProcessor mskimpactTimelieBrainSpineProcessor delegation
    // This was necessary because if a delegate processor instance is not created through a bean instance, it's members do not get Autowired
    //
    @Bean
    public MskimpactTimelineBrainSpineModelToCompositeProcessor mskimpactTimelineBrainSpineModelToCompositeProcessorTypeStatus() {
        return new MskimpactTimelineBrainSpineModelToCompositeProcessor(BrainSpineTimelineType.STATUS);
    }

    @Bean
    public MskimpactTimelineBrainSpineCompositeToCompositeProcessor mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeTreatment() {
        return new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.TREATMENT);
    }

    @Bean
    public MskimpactTimelineBrainSpineCompositeToCompositeProcessor mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeSurgery() {
        return new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.SURGERY);
    }

    @Bean
    public MskimpactTimelineBrainSpineCompositeToCompositeProcessor mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeSpecimen() {
        return new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.SPECIMEN);
    }

    @Bean
    public MskimpactTimelineBrainSpineCompositeToCompositeProcessor mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeImaging() {
        return new MskimpactTimelineBrainSpineCompositeToCompositeProcessor(BrainSpineTimelineType.IMAGING);
    }

    @Bean
    @StepScope
    public CompositeItemProcessor mskimpactTimelineBrainSpineProcessor() {
        List<ItemProcessor> processorDelegates = new ArrayList<>();
        processorDelegates.add(mskimpactTimelineBrainSpineModelToCompositeProcessorTypeStatus());
        processorDelegates.add(mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeTreatment());
        processorDelegates.add(mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeSurgery());
        processorDelegates.add(mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeSpecimen());
        processorDelegates.add(mskimpactTimelineBrainSpineCompositeToCompositeProcessorTypeImaging());
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
        writerDelegates.add(treatmentWriter());
        writerDelegates.add(surgeryWriter());
        writerDelegates.add(specimenWriter());
        writerDelegates.add(imagingWriter());
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
