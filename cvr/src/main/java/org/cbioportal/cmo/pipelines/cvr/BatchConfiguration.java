/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr;

import java.util.*;
import org.cbioportal.cmo.pipelines.cvr.clinical.*;
import org.cbioportal.cmo.pipelines.cvr.cna.*;
import org.cbioportal.cmo.pipelines.cvr.consume.*;
import org.cbioportal.cmo.pipelines.cvr.fusion.*;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.cmo.pipelines.cvr.mutation.*;
import org.cbioportal.cmo.pipelines.cvr.seg.*;
import org.cbioportal.cmo.pipelines.cvr.sv.*;
import org.cbioportal.cmo.pipelines.cvr.variants.*;
import org.cbioportal.cmo.pipelines.cvr.genepanel.*;
import org.cbioportal.models.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

/**
 * @author heinsz
 */
@Configuration
@EnableBatchProcessing
@ComponentScan(basePackages="org.cbioportal.annotator")
public class BatchConfiguration {
    public static final String CVR_JOB = "cvrJob";
    public static final String JSON_JOB = "jsonJob";
    public static final String GML_JOB = "gmlJob";
    public static final String GML_JSON_JOB = "gmlJsonJob";
    public static final String CONSUME_SAMPLES_JOB = "consumeSamplesJob";
    public static final String EMAIL_UTIL= "EmailUtil";

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk}")
    private int chunkInterval;

    @Bean
    public Job gmlJob() {
        return jobBuilderFactory.get(GML_JOB)
                .start(gmlJsonStep())
                .next(gmlClinicalStep())
                .next(gmlMutationStep())
                .build();
    }

    @Bean
    public Job jsonJob() {
        return jobBuilderFactory.get(JSON_JOB)
                .start(clinicalStep())
                .next(mutationStep())
                .next(unfilteredMutationStep())
                .next(cnaStep())
                .next(svStep())
                .next(fusionStep())
//                .next(segStep())
                .build();
    }

    @Bean
    public Job cvrJob() {
        return jobBuilderFactory.get(CVR_JOB)
                .start(cvrJsonStep())
                .next(clinicalStep())
                .next(mutationStep())
                .next(unfilteredMutationStep())
                .next(cnaStep())
                .next(svStep())
                .next(fusionStep())
                .next(segStep())
                .next(genePanelStep())
                .build();
    }

    @Bean
    public Job gmlJsonJob() {
        return jobBuilderFactory.get(GML_JSON_JOB)
                .start(gmlMutationStep())
                .next(gmlClinicalStep())
                .build();
    }

    @Bean
    public Job consumeSamplesJob() {
        return jobBuilderFactory.get(CONSUME_SAMPLES_JOB)
                .start(consumeSampleStep())
                .build();
    }

    @Bean
    public GMLClinicalStepListener gmlClinicalStepListener() {
        return new GMLClinicalStepListener();
    }

    @Bean
    public Step gmlJsonStep() {
        return stepBuilderFactory.get("gmlJsonStep")
                .<GMLVariant, String> chunk(chunkInterval)
                .reader(gmlJsonReader())
                .processor(gmlJsonProcessor())
                .writer(gmlJsonWriter())
                .build();
    }

    @Bean
    public Step gmlMutationStep() {
        return stepBuilderFactory.get("gmlMutationStep")
                .<AnnotatedRecord, String> chunk(chunkInterval)
                .reader(gmlMutationReader())
                .processor(gmlMutationProcessor())
                .writer(gmlMutationDataWriter())
                .build();
    }

    @Bean
    public Step gmlClinicalStep() {
        return stepBuilderFactory.get("gmlClinicalStep")
                .listener(gmlClinicalStepListener())
                .<CVRClinicalRecord, CompositeClinicalRecord> chunk(chunkInterval)
                .reader(gmlClinicalDataReader())
                .processor(clinicalDataProcessor())
                .writer(allClinicalDataWriter())
                .build();
    }

    @Bean
    public Step cvrJsonStep() {
        return stepBuilderFactory.get("cvrJsonStep")
                .<CVRVariants, String> chunk(chunkInterval)
                .reader(cvrJsonreader())
                .processor(cvrJsonprocessor())
                .writer(cvrJsonwriter())
                .build();
    }

    @Bean
    public Step clinicalStep() {
        return stepBuilderFactory.get("clinicalStep")
                .<CVRClinicalRecord, CompositeClinicalRecord> chunk(chunkInterval)
                .reader(clinicalDataReader())
                .processor(clinicalDataProcessor())
                .writer(compositeClinicalDataWriter())
                .build();
    }

    @Bean
    public Step mutationStep() {
        return stepBuilderFactory.get("mutationStep")
                .<AnnotatedRecord, String> chunk(chunkInterval)
                .reader(mutationDataReader())
                .processor(mutationDataProcessor())
                .writer(mutationDataWriter())
                .build();
    }

    @Bean
    public Step unfilteredMutationStep() {
        return stepBuilderFactory.get("unfilteredMutationStep")
                .<AnnotatedRecord, String> chunk(chunkInterval)
                .reader(unfilteredMutationDataReader())
                .processor(mutationDataProcessor())
                .writer(unfilteredMutationDataWriter())
                .build();
    }

    @Bean
    public Step cnaStep() {
        return stepBuilderFactory.get("cnaStep")
                .<CompositeCnaRecord, CompositeCnaRecord> chunk(chunkInterval)
                .reader(cnaDataReader())
                .writer(compositeCnaDataWriter())
                .build();
    }

    @Bean
    public Step svStep() {
        return stepBuilderFactory.get("svStep")
                .<CVRSvRecord, CompositeSvRecord> chunk(chunkInterval)
                .reader(svDataReader())
                .processor(svDataProcessor())
                .writer(compositeSvDataWriter())
                .build();
    }

    @Bean
    public Step fusionStep() {
        return stepBuilderFactory.get("fusionStep")
                .<CVRFusionRecord, String> chunk(chunkInterval)
                .reader(fusionDataReader())
                .processor(fusionDataProcessor())
                .writer(fusionDataWriter())
                .build();
    }

    @Bean
    public Step segStep() {
        return stepBuilderFactory.get("segStep")
                .<CVRSegRecord, CompositeSegRecord> chunk(chunkInterval)
                .reader(segDataReader())
                .processor(segDataProcessor())
                .writer(compositeSegDataWriter())
                .build();
    }

    @Bean
    public Step genePanelStep() {
        return stepBuilderFactory.get("genePanelStep").<CVRGenePanelRecord, String> chunk(chunkInterval)
                .reader(genePanelReader())
                .processor(genePanelProcessor())
                .writer(genePanelWriter())
                .build();
    }

    @Bean
    public Step consumeSampleStep() {
        return stepBuilderFactory.get("consumeSampleStep")
                .<String, String> chunk(chunkInterval)
                .reader(consumeSampleReader())
                .writer(consumeSampleWriter())
                .build();
    }

    //Reader to get json data for GML
    @Bean
    @StepScope
    public ItemStreamReader<GMLVariant> gmlJsonReader() {
        return new GMLVariantsReader();
    }
    //Processor to data into proper format
    @Bean
    @StepScope
    public GMLVariantsProcessor gmlJsonProcessor() {
        return new GMLVariantsProcessor();
    }
    //Write json writer
    @Bean
    @StepScope
    public ItemStreamWriter<String> gmlJsonWriter() {
        return new GMLVariantsWriter();
    }

    @Bean
    @StepScope
    public ItemStreamReader<AnnotatedRecord> gmlMutationReader() {
        return new GMLMutationDataReader();
    }

    @Bean
    @StepScope
    public CVRMutationDataProcessor gmlMutationProcessor() {
        return new CVRMutationDataProcessor();
    }

    @Bean
    @StepScope
    public CVRMutationDataWriter gmlMutationDataWriter() {
        return new CVRMutationDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamReader<CVRClinicalRecord> gmlClinicalDataReader() {
        return new GMLClinicalDataReader();
    }

    // Reader to get json data from CVR
    @Bean
    @StepScope
    public ItemStreamReader<CVRVariants> cvrJsonreader() {
        return new CVRVariantsReader();
    }

    // Processor for processing the json data from CVR
    @Bean
    @StepScope
    public CVRVariantsProcessor cvrJsonprocessor() {
        return new CVRVariantsProcessor();
    }

    // Writer for writing out json from CVR to file
    @Bean
    @StepScope
    public ItemStreamWriter<String> cvrJsonwriter() {
        return new CVRVariantsWriter();
    }

    // Reader to read json file generated by step1

    @Bean
    @StepScope
    public ItemStreamReader<CVRClinicalRecord> clinicalDataReader() {
        return new CVRClinicalDataReader();
    }

    // Processor for clinical data
    @Bean
    @StepScope
    public CVRClinicalDataProcessor clinicalDataProcessor() {
        return new CVRClinicalDataProcessor();
    }

    // Writer for writing clinical data
    @Bean
    @StepScope
    public ItemStreamWriter<CompositeClinicalRecord> allClinicalDataWriter() {
        return new CVRClinicalDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeClinicalRecord> newClinicalDataWriter() {
        return new CVRNewClinicalDataWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<CompositeClinicalRecord> compositeClinicalDataWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(newClinicalDataWriter());
        writerDelegates.add(allClinicalDataWriter());
        CompositeItemWriter writer = new CompositeItemWriter<>();
        writer.setDelegates(writerDelegates);
        return writer;
    }

    // Reader to read json file generated by step1
    @Bean
    @StepScope
    public ItemStreamReader<AnnotatedRecord> mutationDataReader() {
        return new CVRMutationDataReader();
    }

    // Processor for mutation data
    @Bean
    @StepScope
    public CVRMutationDataProcessor mutationDataProcessor() {
        return new CVRMutationDataProcessor();
    }

    @Bean
    @StepScope
    public CVRMutationDataWriter mutationDataWriter() {
        return new CVRMutationDataWriter();
    }

    // Reader to read json file generated by step1
    @Bean
    @StepScope
    public ItemStreamReader<AnnotatedRecord> unfilteredMutationDataReader() {
        return new CVRUnfilteredMutationDataReader();
    }

    @Bean
    @StepScope
    public CVRUnfilteredMutationDataWriter unfilteredMutationDataWriter() {
        return new CVRUnfilteredMutationDataWriter();
    }

    // Reader to read json file generated by step1
    @Bean
    @StepScope
    public ItemStreamReader<CompositeCnaRecord> cnaDataReader() {
        return new CVRCnaDataReader();
    }

    // Writer for writing cna data
    @Bean
    @StepScope
    public ItemStreamWriter<CompositeCnaRecord> cnaDataWriter() {
        return new CVRCnaDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeCnaRecord> newCnaDataWriter() {
        return new CVRNewCnaDataWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<CompositeCnaRecord> compositeCnaDataWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(newCnaDataWriter());
        writerDelegates.add(cnaDataWriter());
        CompositeItemWriter writer = new CompositeItemWriter<>();
        writer.setDelegates(writerDelegates);
        return writer;
    }

    // Reader to read json file generated by step1
    @Bean
    @StepScope
    public ItemStreamReader<CVRSvRecord> svDataReader() {
        return new CVRSvDataReader();
    }

    // Processor for unfiltered mutation data
    @Bean
    @StepScope
    public CVRSvDataProcessor svDataProcessor() {
        return new CVRSvDataProcessor();
    }

    // Writer for writing unfiltered mutation data
    @Bean
    @StepScope
    public ItemStreamWriter<CompositeSvRecord> allSvDataWriter() {
        return new CVRSvDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeSvRecord> newSvDataWriter() {
        return new CVRNewSvDataWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<CompositeSvRecord> compositeSvDataWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(newSvDataWriter());
        writerDelegates.add(allSvDataWriter());
        CompositeItemWriter writer = new CompositeItemWriter<>();
        writer.setDelegates(writerDelegates);
        return writer;
    }

    @Bean
    @StepScope
    public ItemStreamReader<CVRFusionRecord> fusionDataReader() {
        return new CVRFusionDataReader();
    }

    @Bean
    @StepScope
    public CVRFusionDataProcessor fusionDataProcessor() {
        return new CVRFusionDataProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> fusionDataWriter() {
        return new CVRFusionDataWriter();
    }

    // Reader to read json file generated by step1
    @Bean
    @StepScope
    public ItemStreamReader<CVRSegRecord> segDataReader() {
        return new CVRSegDataReader();
    }

    // Processor for segment data
    @Bean
    @StepScope
    public CVRSegDataProcessor segDataProcessor() {
        return new CVRSegDataProcessor();
    }

    // Writer for writing segment data
    @Bean
    @StepScope
    public ItemStreamWriter<CompositeSegRecord> allSegDataWriter() {
        return new CVRSegDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeSegRecord> newSegDataWriter() {
        return new CVRNewSegDataWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<CompositeSegRecord> compositeSegDataWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(newSegDataWriter());
        writerDelegates.add(allSegDataWriter());
        CompositeItemWriter writer = new CompositeItemWriter<>();
        writer.setDelegates(writerDelegates);
        return writer;
    }

    @Bean
    @StepScope
    public ItemStreamReader<CVRGenePanelRecord> genePanelReader() {
        return new CVRGenePanelReader();
    }

    @Bean
    @StepScope
    public CVRGenePanelProcessor genePanelProcessor() {
        return new CVRGenePanelProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> genePanelWriter() {
        return new CVRGenePanelWriter();
    }

    @Bean
    @StepScope
    public ItemStreamReader<String> consumeSampleReader() {
        return new ConsumeSampleReader();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> consumeSampleWriter() {
        return new ConsumeSampleWriter();
    }
}
