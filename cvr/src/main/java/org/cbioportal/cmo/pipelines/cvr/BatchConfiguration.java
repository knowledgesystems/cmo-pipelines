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

import java.net.MalformedURLException;
import org.cbioportal.cmo.pipelines.cvr.model.staging.LinkedMskimpactCaseRecord;
import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeSvRecord;
import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeSegRecord;
import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeCnaRecord;
import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSegRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRGenePanelRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRFusionRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.clinical.*;
import org.cbioportal.cmo.pipelines.cvr.cna.*;
import org.cbioportal.cmo.pipelines.cvr.consume.*;
import org.cbioportal.cmo.pipelines.cvr.fusion.*;
import org.cbioportal.cmo.pipelines.cvr.genepanel.*;
import org.cbioportal.cmo.pipelines.cvr.linkedimpactcase.*;
import org.cbioportal.cmo.pipelines.cvr.samplelist.CvrSampleListsTasklet;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.cmo.pipelines.cvr.mutation.*;
import org.cbioportal.cmo.pipelines.cvr.requeue.*;
import org.cbioportal.cmo.pipelines.cvr.seg.*;
import org.cbioportal.cmo.pipelines.cvr.sv.*;
import org.cbioportal.cmo.pipelines.cvr.variants.*;
import org.cbioportal.cmo.pipelines.cvr.whitelist.ZeroVariantWhitelistTasklet;
import org.cbioportal.models.*;

import java.util.*;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

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

    private final Logger log = Logger.getLogger(BatchConfiguration.class);

    @Bean
    public CVRUtilities cvrUtilities() {
        return new CVRUtilities();
    }

    @Bean
    public Job gmlJob() {
        return jobBuilderFactory.get(GML_JOB)
                .start(cvrSampleListsStep())
                .next(gmlJsonStep())
                .next(gmlClinicalStep())
                .next(gmlMutationStep())
                .next(gmlFusionStep())
                .build();
    }

    @Bean
    public Job jsonJob() {
        return jobBuilderFactory.get(JSON_JOB)
                .start(cvrJsonJobFlow())
                .build().build();
    }

    @Bean
    public Job cvrJob() {
        return jobBuilderFactory.get(CVR_JOB)
                .start(cvrResponseStep())
                .next(checkCvrReponse())
                    .on("RUN")
                    .to(cvrJobFlow())
                .from(checkCvrReponse())
                    .on("STOP").end()
                .build().build();
    }

    @Bean
    public Job gmlJsonJob() {
        return jobBuilderFactory.get(GML_JSON_JOB)
                .start(cvrSampleListsStep())
                .next(gmlClinicalStep())
                .next(gmlMutationStep())
                .next(gmlFusionStep())
                .build();
    }

    @Bean
    public Job consumeSamplesJob() {
        return jobBuilderFactory.get(CONSUME_SAMPLES_JOB)
                .start(consumeSampleStep())
                .build();
    }

    @Bean
    public Flow cvrJsonJobFlow() {
        return new FlowBuilder<Flow>("cvrJsonJobFlow")
                .start(cvrSampleListsStep())
                .next(linkedMskimpactCaseFlow())
                .next(clinicalStep())
                .next(mutationsStepFlow())
                .next(cnaStepFlow())
                .next(svFusionsStepFlow())
                .next(genePanelStep())
                .build();
    }

    @Bean
    public Flow cvrJobFlow() {
        return new FlowBuilder<Flow>("cvrJobFlow")
                .start(cvrSampleListsStep())
                .next(cvrJsonStep())
                .next(linkedMskimpactCaseFlow())
                .next(clinicalStep())
                .next(mutationsStepFlow())
                .next(cnaStepFlow())
                .next(svFusionsStepFlow())
                .next(segmentStepFlow())
                .next(genePanelStep())
                .next(cvrRequeueStep())
                .next(zeroVariantWhitelistFlow())
                .build();
    }

    @Bean
    public Flow linkedMskimpactCaseFlow() {
        return new FlowBuilder<Flow>("linkedMskimpactCaseFlow")
                .start(linkedMskimpactCaseDecider()).on("RUN").to(linkedMskimpactCaseStep())
                .from(linkedMskimpactCaseDecider()).on("SKIP").end().build();
    }

    @Bean
    public Flow mutationsStepFlow() {
        return new FlowBuilder<Flow>("mutationsStepFlow")
                .start(mutationsStepExecutionDecider())
                    .on("RUN")
                        .to(mutationStep())
                        .next(nonSignedoutMutationStep())
                .from(mutationsStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean
    public Flow cnaStepFlow() {
        return new FlowBuilder<Flow>("cnaStepFlow")
                .start(cnaStepExecutionDecider())
                    .on("RUN")
                        .to(cnaStep())
                .from(cnaStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean
    public Flow svFusionsStepFlow() {
        return new FlowBuilder<Flow>("svFusionsStepFlow")
                .start(svFusionsStepExecutionDecider())
                    .on("RUN")
                        .to(svStep())
                        .next(fusionStep())
                .from(svFusionsStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean
    public Flow segmentStepFlow() {
        return new FlowBuilder<Flow>("segmentStepFlow")
                .start(segStepExecutionDecider())
                    .on("RUN")
                        .to(segStep())
                .from(segStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean
    public Flow zeroVariantWhitelistFlow() {
        // use datatype = mutations since whitelist is for mutations data
        return new FlowBuilder<Flow>("zeroVariantWhitelistFlow")
                .start(mutationsStepExecutionDecider())
                    .on("RUN")
                        .to(zeroVariantWhitelistStep())
                .from(mutationsStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
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
                .processor(mutationDataProcessor())
                .writer(mutationDataWriter())
                .build();
    }

    @Bean
    public Step gmlClinicalStep() {
        return stepBuilderFactory.get("gmlClinicalStep")
                .tasklet(gmlClinicalTasklet())
                .build();
    }

    @Bean
    @StepScope
    public Tasklet gmlClinicalTasklet() {
        return new GMLClinicalTasklet();
    }

    @Bean
    public Step gmlFusionStep() {
        return stepBuilderFactory.get("gmlFusionStep")
                .<CVRFusionRecord, String> chunk(chunkInterval)
                .reader(gmlFusionDataReader())
                .processor(fusionDataProcessor())
                .writer(fusionDataWriter())
                .build();
    }

    @Bean
    public Step cvrJsonStep() {
        return stepBuilderFactory.get("cvrJsonStep")
                .listener(cvrResponseListener())
                .<CvrResponse, String> chunk(chunkInterval)
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
    public Step linkedMskimpactCaseStep() {
        return stepBuilderFactory.get("linkedMskimpactCaseStep")
                .<LinkedMskimpactCaseRecord, String> chunk(chunkInterval)
                .reader(linkedMskimpactCaseReader())
                .processor(linkedMskimpactCaseProcessor())
                .writer(linkedMskimpactCaseWriter())
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
    public Step nonSignedoutMutationStep() {
        return stepBuilderFactory.get("nonSignedoutMutationStep")
                .<AnnotatedRecord, String> chunk(chunkInterval)
                .reader(nonSignedoutMutationDataReader())
                .processor(mutationDataProcessor())
                .writer(mutationDataWriter())
                .build();
    }

    @Bean
    public Step cnaStep() {
        return stepBuilderFactory.get("cnaStep")
                .<CompositeCnaRecord, CompositeCnaRecord> chunk(chunkInterval)
                .reader(cnaDataReader())
                .writer(cnaDataWriter())
                .build();
    }

    @Bean
    public Step svStep() {
        return stepBuilderFactory.get("svStep")
                .<CVRSvRecord, CompositeSvRecord> chunk(chunkInterval)
                .reader(svDataReader())
                .processor(svDataProcessor())
                .writer(svDataWriter())
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
                .writer(segDataWriter())
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
    public Step cvrRequeueStep() {
        return stepBuilderFactory.get("cvrRequeueStep")
                .listener(cvrRequeueListener())
                .tasklet(cvrRequeueTasklet())
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

    @Bean
    public Step cvrSampleListsStep() {
        return stepBuilderFactory.get("cvrSampleListsStep")
                .tasklet(cvrSampleListsTasklet())
                .build();
    }

    @Bean
    public Step cvrResponseStep() {
        return stepBuilderFactory.get("cvrResponseStep")
                .tasklet(cvrResponseTasklet())
                .build();
    }

    @Bean
    public Step zeroVariantWhitelistStep() {
        return stepBuilderFactory.get("zeroVariantWhitelistStep")
                .tasklet(zeroVariantWhitelistTasklet())
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

    // Reader to get json data from CVR
    @Bean
    @StepScope
    public ItemStreamReader<CvrResponse> cvrJsonreader() {
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
    public ItemStreamWriter<CompositeClinicalRecord> clinicalDataWriter() {
        return new CVRClinicalDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeClinicalRecord> seqDateClinicalDataWriter() {
        return new CVRSeqDateClinicalDataWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<CompositeClinicalRecord> compositeClinicalDataWriter() {
        List<ItemStreamWriter> writerDelegates = new ArrayList<>();
        writerDelegates.add(seqDateClinicalDataWriter());
        writerDelegates.add(clinicalDataWriter());
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
    public ItemStreamReader<AnnotatedRecord> nonSignedoutMutationDataReader() {
        return new CVRNonSignedoutMutationDataReader();
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
    public ItemStreamReader<LinkedMskimpactCaseRecord> linkedMskimpactCaseReader() {
        return new LinkedMskimpactCaseReader();
    }

    @Bean
    public LinkedMskimpactCaseProcessor linkedMskimpactCaseProcessor() {
        return new LinkedMskimpactCaseProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> linkedMskimpactCaseWriter() {
        return new LinkedMskimpactCaseWriter();
    }

    // Reader to read json file generated by step1
    @Bean
    @StepScope
    public ItemStreamReader<CVRSvRecord> svDataReader() {
        return new CVRSvDataReader();
    }

    @Bean
    @StepScope
    public CVRSvDataProcessor svDataProcessor() {
        return new CVRSvDataProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeSvRecord> svDataWriter() {
        return new CVRSvDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamReader<CVRFusionRecord> fusionDataReader() {
        return new CVRFusionDataReader();
    }

    @Bean
    @StepScope
    public ItemStreamReader<CVRFusionRecord> gmlFusionDataReader() {
        return new GMLFusionDataReader();
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
    public ItemStreamWriter<CompositeSegRecord> segDataWriter() {
        return new CVRSegDataWriter();
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
    public Tasklet cvrSampleListsTasklet() {
        return new CvrSampleListsTasklet();
    }

    @Bean
    @StepScope
    public Tasklet cvrRequeueTasklet() {
        return new CvrRequeueTasklet();
    }

    @Bean
    public StepExecutionListener cvrRequeueListener() {
        return new CvrRequeueListener();
    }

    @Bean
    public StepExecutionListener cvrResponseListener() {
        return new CvrResponseListener();
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

    @Bean
    public JobExecutionDecider linkedMskimpactCaseDecider() {
        return new JobExecutionDecider() {
            @Override
            public FlowExecutionStatus decide(JobExecution je, StepExecution se) {
                String studyId = je.getJobParameters().getString("studyId");
                if (studyId.equals("mskarcher")) {
                    return new FlowExecutionStatus("RUN");
                }
                else {
                    return new FlowExecutionStatus("SKIP");
                }
            }
        };
    }

    @Bean
    @StepScope
    public Tasklet cvrResponseTasklet() {
        return new CvrResponseTasklet();
    }

    @Bean
    @StepScope
    public Tasklet zeroVariantWhitelistTasklet() {
        return new ZeroVariantWhitelistTasklet();
    }

    @Bean
    public JobExecutionDecider checkCvrReponse() {
        return new JobExecutionDecider() {
            @Override
            public FlowExecutionStatus decide(JobExecution je, StepExecution se) {
                String studyId = je.getJobParameters().getString("studyId");
                Integer sampleCount = (Integer) je.getExecutionContext().get("sampleCount");
                if (sampleCount > 0) {
                    return new FlowExecutionStatus("RUN");
                }
                else {
                    log.warn("No new samples to process for study " + studyId + " - exiting gracefully...");
                    return new FlowExecutionStatus("STOP");
                }
            }
        };
    }

    /**
     * Decides whether step(s) for given datatype should execute for current study.
     * @param datatype
     * @return
     */
    public JobExecutionDecider decideStepExecutionByDatatypeForStudyId(String datatype) {
        return new JobExecutionDecider() {
            @Override
            public FlowExecutionStatus decide(JobExecution je, StepExecution se) {
                String studyId = je.getJobParameters().getString("studyId");
                if (!CVRUtilities.DATATYPES_TO_SKIP_BY_STUDY.containsKey(studyId) ||
                        !CVRUtilities.DATATYPES_TO_SKIP_BY_STUDY.get(studyId).contains(datatype)) {
                    return new FlowExecutionStatus("RUN");
                }
                else {
                    return new FlowExecutionStatus("SKIP");
                }
            }
        };
    }

    @Bean
    public JobExecutionDecider mutationsStepExecutionDecider() {
        return decideStepExecutionByDatatypeForStudyId("mutations");
    }

    @Bean
    public JobExecutionDecider cnaStepExecutionDecider() {
        return decideStepExecutionByDatatypeForStudyId("cna");
    }

    @Bean
    public JobExecutionDecider svFusionsStepExecutionDecider() {
        return decideStepExecutionByDatatypeForStudyId("sv-fusions");
    }

    @Bean
    public JobExecutionDecider segStepExecutionDecider() {
        return decideStepExecutionByDatatypeForStudyId("seg");
    }

    // general spring batch configuration
    @Value("org/springframework/batch/core/schema-drop-sqlite.sql")
    private Resource dropRepositoryTables;

    @Value("org/springframework/batch/core/schema-sqlite.sql")
    private Resource dataRepositorySchema;

    /**
     * Spring Batch datasource.
     * @return DataSource
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:repository.sqlite");
        return dataSource;
    }

    /**
     * Spring Batch datasource initializer.
     * @param dataSource
     * @return DataSourceInitializer
     * @throws MalformedURLException
     */
    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) throws MalformedURLException {
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.addScript(dropRepositoryTables);
        databasePopulator.addScript(dataRepositorySchema);
        databasePopulator.setIgnoreFailedDrops(true);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator);
        return initializer;
    }

    /**
     * Spring Batch job repository.
     * @return JobRepository
     * @throws Exception
     */
    private JobRepository getJobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource());
        factory.setTransactionManager(getTransactionManager());
        factory.afterPropertiesSet();
        return (JobRepository) factory.getObject();
    }

    /**
     * Spring Batch transaction manager.
     * @return PlatformTransactionManager
     */
    private PlatformTransactionManager getTransactionManager() {
        return new ResourcelessTransactionManager();
    }

    /**
     * Spring Batch job launcher.
     * @return JobLauncher
     * @throws Exception
     */
    public JobLauncher getJobLauncher() throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(getJobRepository());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

}
