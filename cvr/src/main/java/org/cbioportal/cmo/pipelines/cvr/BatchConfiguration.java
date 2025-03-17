/*
 * Copyright (c) 2016 - 2022, 2024, 2025 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr;

import java.net.MalformedURLException;
import java.util.*;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.clinical.*;
import org.cbioportal.cmo.pipelines.cvr.cna.*;
import org.cbioportal.cmo.pipelines.cvr.consume.*;
import org.cbioportal.cmo.pipelines.cvr.genepanel.*;
import org.cbioportal.cmo.pipelines.cvr.linkedimpactcase.*;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRGenePanelRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSegRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRSvRecord;
import org.cbioportal.cmo.pipelines.cvr.model.staging.LinkedMskimpactCaseRecord;
import org.cbioportal.cmo.pipelines.cvr.mutation.*;
import org.cbioportal.cmo.pipelines.cvr.requeue.*;
import org.cbioportal.cmo.pipelines.cvr.samplelist.CvrSampleListsTasklet;
import org.cbioportal.cmo.pipelines.cvr.seg.*;
import org.cbioportal.cmo.pipelines.cvr.smile.SmilePublisherTasklet;
import org.cbioportal.cmo.pipelines.cvr.sv.*;
import org.cbioportal.cmo.pipelines.cvr.variants.*;
import org.cbioportal.cmo.pipelines.cvr.whitelist.ZeroVariantWhitelistTasklet;
import org.cbioportal.models.*;
import org.mskcc.cmo.messaging.Gateway;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
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
@ComponentScan(basePackages = {"org.cbioportal.annotator", "org.mskcc.cmo.messaging", "org.mskcc.cmo.common.*"})
@EnableBatchProcessing(executionContextSerializerRef = "jacksonSerializer")
public class BatchConfiguration {
    public static final String CVR_JOB = "cvrJob";
    public static final String JSON_JOB = "jsonJob";
    public static final String GML_JOB = "gmlJob";
    public static final String GML_JSON_JOB = "gmlJsonJob";
    public static final String CONSUME_SAMPLES_JOB = "consumeSamplesJob";
    public static final String EMAIL_UTIL= "EmailUtil";

    @Value("${chunk}")
    private int chunkInterval;

    private final Logger log = Logger.getLogger(BatchConfiguration.class);

    @Autowired
    private Gateway messagingGateway;

    @Bean
    public Gateway messagingGateway() throws Exception {
        try {
            messagingGateway.connect();
        } catch (Exception e) {
            log.warn("Unable to connect to NATS server, no samples will be published.");
        }
        return messagingGateway;
    }

    @Bean
    public CVRUtilities cvrUtilities() {
        return new CVRUtilities();
    }

    @Bean
    public SvUtilities svUtilites() {
        return new SvUtilities();
    }

    @Bean
    public Job gmlJob(JobRepository jobRepository,
                      @Qualifier("cvrSampleListsStep") Step cvrSampleListsStep,
                      @Qualifier("gmlJsonStep") Step gmlJsonStep,
                      @Qualifier("gmlClinicalStep") Step gmlClinicalStep,
                      @Qualifier("gmlMutationStep") Step gmlMutationStep,
                      @Qualifier("gmlSvStep") Step gmlSvStep) {
        return new JobBuilder(GML_JOB, jobRepository)
                .start(cvrSampleListsStep)
                .next(gmlJsonStep)
                .next(gmlClinicalStep)
                .next(gmlMutationStep)
                .next(gmlSvStep)
                .build();
    }

    @Bean
    public Job jsonJob(JobRepository jobRepository,
                       @Qualifier("cvrJsonJobFlow") Flow cvrJsonJobFlow) {
        return new JobBuilder(JSON_JOB, jobRepository)
                .start(cvrJsonJobFlow)
                .build().build();
    }

    @Bean
    public Job cvrJob(JobRepository jobRepository,
                      @Qualifier("cvrResponseStep") Step cvrResponseStep,
                      @Qualifier("cvrJobFlow") Flow cvrJobFlow) {
        return new JobBuilder(CVR_JOB, jobRepository)
                .start(cvrResponseStep)
                .next(checkCvrResponse())
                    .on("RUN")
                    .to(cvrJobFlow)
                .from(checkCvrResponse())
                    .on("STOP").end()
                .build().build();
    }

    @Bean
    public Job gmlJsonJob(JobRepository jobRepository,
                          @Qualifier("cvrSampleListsStep") Step cvrSampleListsStep,
                          @Qualifier("gmlClinicalStep") Step gmlClinicalStep,
                          @Qualifier("gmlMutationStep") Step gmlMutationStep,
                          @Qualifier("gmlSvStep") Step gmlSvStep) {
        return new JobBuilder(GML_JSON_JOB, jobRepository)
                .start(cvrSampleListsStep)
                .next(gmlClinicalStep)
                .next(gmlMutationStep)
                .next(gmlSvStep)
                .build();
    }

    @Bean
    public Job consumeSamplesJob(JobRepository jobRepository,
                                 @Qualifier("consumeSampleStep") Step consumeSampleStep,
                                 @Qualifier("smilePublisherStep") Step smilePublisherStep) {
        return new JobBuilder(CONSUME_SAMPLES_JOB, jobRepository)
                .start(consumeSampleStep)
                .next(smilePublisherStep)
                .build();
    }

    @Bean(name = "cvrJsonJobFlow")
    public Flow cvrJsonJobFlow(@Qualifier("cvrSampleListsStep") Step cvrSampleListsStep,
                               @Qualifier("linkedMskimpactCaseFlow") Flow linkedMskimpactCaseFlow,
                               @Qualifier("clinicalStep") Step clinicalStep,
                               @Qualifier("mutationsStepFlow") Flow mutationsStepFlow,
                               @Qualifier("cnaStepFlow") Flow cnaStepFlow,
                               @Qualifier("svStepFlow") Flow svStepFlow,
                               @Qualifier("genePanelStep") Step genePanelStep) {
        return new FlowBuilder<Flow>("cvrJsonJobFlow")
                .start(cvrSampleListsStep)
                .next(linkedMskimpactCaseFlow)
                .next(clinicalStep)
                .next(mutationsStepFlow)
                .next(cnaStepFlow)
                .next(svStepFlow)
                .next(genePanelStep)
                .build();
    }

    @Bean(name = "cvrJobFlow")
    public Flow cvrJobFlow(@Qualifier("cvrSampleListsStep") Step cvrSampleListsStep,
                           @Qualifier("cvrJsonStep") Step cvrJsonStep,
                           @Qualifier("linkedMskimpactCaseFlow") Flow linkedMskimpactCaseFlow,
                           @Qualifier("clinicalStep") Step clinicalStep,
                           @Qualifier("mutationsStepFlow") Flow mutationsStepFlow,
                           @Qualifier("cnaStepFlow") Flow cnaStepFlow,
                           @Qualifier("svStepFlow") Flow svStepFlow,
                           @Qualifier("segmentStepFlow") Flow segmentStepFlow,
                           @Qualifier("genePanelStep") Step genePanelStep,
                           @Qualifier("cvrRequeueStep") Step cvrRequeueStep,
                           @Qualifier("zeroVariantWhitelistFlow") Flow zeroVariantWhitelistFlow) {
        return new FlowBuilder<Flow>("cvrJobFlow")
                .start(cvrSampleListsStep)
                .next(cvrJsonStep)
                .next(linkedMskimpactCaseFlow)
                .next(clinicalStep)
                .next(mutationsStepFlow)
                .next(cnaStepFlow)
                .next(svStepFlow)
                .next(segmentStepFlow)
                .next(genePanelStep)
                .next(cvrRequeueStep)
                .next(zeroVariantWhitelistFlow)
                .build();
    }

    @Bean(name = "linkedMskimpactCaseFlow")
    public Flow linkedMskimpactCaseFlow(@Qualifier("linkedMskimpactCaseStep") Step linkedMskimpactCaseStep) {
        return new FlowBuilder<Flow>("linkedMskimpactCaseFlow")
                .start(linkedMskimpactCaseDecider()).on("RUN").to(linkedMskimpactCaseStep)
                .from(linkedMskimpactCaseDecider()).on("SKIP").end().build();
    }

    @Bean(name = "mutationsStepFlow")
    public Flow mutationsStepFlow(@Qualifier("mutationStep") Step mutationStep,
                                  @Qualifier("nonSignedoutMutationStep") Step nonSignedoutMutationStep) {
        return new FlowBuilder<Flow>("mutationsStepFlow")
                .start(mutationsStepExecutionDecider())
                    .on("RUN")
                        .to(mutationStep)
                        .next(nonSignedoutMutationStep)
                .from(mutationsStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean(name = "cnaStepFlow")
    public Flow cnaStepFlow(@Qualifier("cnaStep") Step cnaStep) {
        return new FlowBuilder<Flow>("cnaStepFlow")
                .start(cnaStepExecutionDecider())
                    .on("RUN")
                        .to(cnaStep)
                .from(cnaStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean(name = "svStepFlow")
    public Flow svStepFlow(@Qualifier("svStep") Step svStep) {
        return new FlowBuilder<Flow>("svStepFlow")
                .start(svStepExecutionDecider())
                    .on("RUN")
                        .to(svStep)
                .from(svStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean(name = "segmentStepFlow")
    public Flow segmentStepFlow(@Qualifier("segStep") Step segStep) {
        return new FlowBuilder<Flow>("segmentStepFlow")
                .start(segStepExecutionDecider())
                    .on("RUN")
                        .to(segStep)
                .from(segStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean(name = "zeroVariantWhitelistFlow")
    public Flow zeroVariantWhitelistFlow(@Qualifier("zeroVariantWhitelistStep") Step zeroVariantWhitelistStep) {
        // use datatype = mutations since whitelist is for mutations data
        return new FlowBuilder<Flow>("zeroVariantWhitelistFlow")
                .start(mutationsStepExecutionDecider())
                    .on("RUN")
                        .to(zeroVariantWhitelistStep)
                .from(mutationsStepExecutionDecider())
                    .on("SKIP")
                        .end()
                .build();
    }

    @Bean(name = "gmlJsonStep")
    public Step gmlJsonStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("gmlJsonStep", jobRepository)
                .<GMLVariant, String> chunk(chunkInterval, transactionManager)
                .reader(gmlJsonReader())
                .processor(gmlJsonProcessor())
                .writer(gmlJsonWriter())
                .build();
    }

    @Bean(name = "gmlMutationStep")
    public Step gmlMutationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("gmlMutationStep", jobRepository)
                .<AnnotatedRecord, String> chunk(chunkInterval, transactionManager)
                .reader(gmlMutationReader())
                .processor(mutationDataProcessor())
                .writer(mutationDataWriter())
                .build();
    }

    @Bean(name = "gmlClinicalStep")
    public Step gmlClinicalStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("gmlClinicalTasklet") Tasklet gmlClinicalTasklet) {
        return new StepBuilder("gmlClinicalStep", jobRepository)
                .tasklet(gmlClinicalTasklet, transactionManager)
                .build();
    }

    @Bean(name = "gmlClinicalTasklet")
    @StepScope
    public Tasklet gmlClinicalTasklet() {
        return new GMLClinicalTasklet();
    }

    @Bean(name = "gmlSvStep")
    public Step gmlSvStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("gmlSvStep", jobRepository)
                .<CVRSvRecord, String> chunk(chunkInterval, transactionManager)
                .reader(gmlSvDataReader())
                .processor(svDataProcessor())
                .writer(svDataWriter())
                .build();
    }

    @Bean(name = "cvrJsonStep")
    public Step cvrJsonStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("cvrJsonStep", jobRepository)
                .listener(cvrResponseListener())
                .<CvrResponse, String> chunk(chunkInterval, transactionManager)
                .reader(cvrJsonReader())
                .processor(cvrJsonProcessor())
                .writer(cvrJsonWriter())
                .build();
    }

    @Bean(name = "clinicalStep")
    public Step clinicalStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("clinicalStep", jobRepository)
                .<CVRClinicalRecord, CompositeClinicalRecord> chunk(chunkInterval, transactionManager)
                .reader(clinicalDataReader())
                .processor(clinicalDataProcessor())
                .writer(compositeClinicalDataWriter())
                .build();
    }

    @Bean(name = "linkedMskimpactCaseStep")
    public Step linkedMskimpactCaseStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("linkedMskimpactCaseStep", jobRepository)
                .<LinkedMskimpactCaseRecord, String> chunk(chunkInterval, transactionManager)
                .reader(linkedMskimpactCaseReader())
                .processor(linkedMskimpactCaseProcessor())
                .writer(linkedMskimpactCaseWriter())
                .build();
    }

    @Bean(name = "mutationStep")
    public Step mutationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("mutationStep", jobRepository)
                .<AnnotatedRecord, String> chunk(chunkInterval, transactionManager)
                .reader(mutationDataReader())
                .processor(mutationDataProcessor())
                .writer(mutationDataWriter())
                .build();
    }

    @Bean(name = "nonSignedoutMutationStep")
    public Step nonSignedoutMutationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("nonSignedoutMutationStep", jobRepository)
                .<AnnotatedRecord, String> chunk(chunkInterval, transactionManager)
                .reader(nonSignedoutMutationDataReader())
                .processor(mutationDataProcessor())
                .writer(mutationDataWriter())
                .build();
    }

    @Bean(name = "cnaStep")
    public Step cnaStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("cnaStep", jobRepository)
                .<String, String> chunk(chunkInterval, transactionManager)
                .reader(cnaDataReader())
                .writer(cnaDataWriter())
                .build();
    }

    @Bean(name = "svStep")
    public Step svStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("svStep", jobRepository)
                .<CVRSvRecord, String> chunk(chunkInterval, transactionManager)
                .reader(svDataReader())
                .processor(svDataProcessor())
                .writer(svDataWriter())
                .build();
    }

    @Bean(name = "segStep")
    public Step segStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("segStep", jobRepository)
                .<CVRSegRecord, String> chunk(chunkInterval, transactionManager)
                .reader(segDataReader())
                .processor(segDataProcessor())
                .writer(segDataWriter())
                .build();
    }

    @Bean(name = "genePanelStep")
    public Step genePanelStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("genePanelStep", jobRepository)
                .<CVRGenePanelRecord, String> chunk(chunkInterval, transactionManager)
                .reader(genePanelReader())
                .processor(genePanelProcessor())
                .writer(genePanelWriter())
                .build();
    }

    @Bean(name = "cvrRequeueStep")
    public Step cvrRequeueStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("cvrRequeueTasklet") Tasklet cvrRequeueTasklet) {
        return new StepBuilder("cvrRequeueStep", jobRepository)
                .listener(cvrRequeueListener())
                .tasklet(cvrRequeueTasklet, transactionManager)
                .build();
    }

    @Bean(name = "smilePublisherStep")
    public Step smilePublisherStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("smilePublisherTasklet") Tasklet smilePublisherTasklet) {
        return new StepBuilder("smilePublisherStep", jobRepository)
                .tasklet(smilePublisherTasklet, transactionManager)
                .build();
    }

    @Bean(name = "consumeSampleStep")
    public Step consumeSampleStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("consumeSampleStep", jobRepository)
                .<String, String> chunk(chunkInterval, transactionManager)
                .reader(consumeSampleReader())
                .writer(consumeSampleWriter())
                .build();
    }

    @Bean(name = "cvrSampleListsStep")
    public Step cvrSampleListsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("cvrSampleListsTasklet") Tasklet cvrSampleListsTasklet) {
        return new StepBuilder("cvrSampleListsStep", jobRepository)
                .tasklet(cvrSampleListsTasklet, transactionManager)
                .build();
    }

    @Bean(name = "cvrResponseStep")
    public Step cvrResponseStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("cvrResponseTasklet") Tasklet cvrResponseTasklet) {
        return new StepBuilder("cvrResponseStep", jobRepository)
                .tasklet(cvrResponseTasklet, transactionManager)
                .build();
    }

    @Bean(name = "zeroVariantWhitelistStep")
    public Step zeroVariantWhitelistStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, @Qualifier("zeroVariantWhitelistTasklet") Tasklet zeroVariantWhitelistTasklet) {
        return new StepBuilder("zeroVariantWhitelistStep", jobRepository)
                .tasklet(zeroVariantWhitelistTasklet, transactionManager)
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
    public ItemStreamReader<CvrResponse> cvrJsonReader() {
        return new CVRVariantsReader();
    }

    // Processor for processing the json data from CVR
    @Bean
    @StepScope
    public CVRVariantsProcessor cvrJsonProcessor() {
        return new CVRVariantsProcessor();
    }

    // Writer for writing out json from CVR to file
    @Bean
    @StepScope
    public ItemStreamWriter<String> cvrJsonWriter() {
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
    public ItemStreamReader<String> cnaDataReader() {
        return new CVRCnaDataReader();
    }

    // Writer for writing cna data
    @Bean
    @StepScope
    public ItemStreamWriter<String> cnaDataWriter() {
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
    public ItemStreamWriter<String> svDataWriter() {
        return new CVRSvDataWriter();
    }

    @Bean
    @StepScope
    public ItemStreamReader<CVRSvRecord> gmlSvDataReader() {
        return new GMLSvDataReader();
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
    public ItemStreamWriter<String> segDataWriter() {
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

    @Bean(name = "cvrSampleListsTasklet")
    @StepScope
    public Tasklet cvrSampleListsTasklet() {
        return new CvrSampleListsTasklet();
    }

    @Bean(name = "cvrRequeueTasklet")
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

    @Bean(name = "smilePublisherTasklet")
    @StepScope
    public Tasklet smilePublisherTasklet() {
        return new SmilePublisherTasklet();
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

    @Bean(name = "cvrResponseTasklet")
    @StepScope
    public Tasklet cvrResponseTasklet() {
        return new CvrResponseTasklet();
    }

    @Bean(name = "zeroVariantWhitelistTasklet")
    @StepScope
    public Tasklet zeroVariantWhitelistTasklet() {
        return new ZeroVariantWhitelistTasklet();
    }

    @Bean
    public JobExecutionDecider checkCvrResponse() {
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
    public JobExecutionDecider svStepExecutionDecider() {
        return decideStepExecutionByDatatypeForStudyId("sv");
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

    @Bean
    public ExecutionContextSerializer jacksonSerializer() {
        return new Jackson2ExecutionContextStringSerializer();
    }
}
