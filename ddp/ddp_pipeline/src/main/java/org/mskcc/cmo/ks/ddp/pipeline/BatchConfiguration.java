/*
 * Copyright (c) 2018 - 2023, 2024 Memorial Sloan Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline;

import java.net.MalformedURLException;
import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.CompositeResult;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.integration.async.*;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
/**
 *
 * @author ochoaa
 */
@Configuration
@ComponentScan(basePackages = "org.mskcc.cmo.ks.ddp.source")
@EnableAsync
public class BatchConfiguration {

    @Value("${async.DDP.thread.pool.size}")
    private String asyncDDPThreadPoolSize;

    @Value("${async.DDP.thread.pool.max}")
    private String asyncDDPThreadPoolMax;

    @Value("${processor.thread.pool.size}")
    private String processorThreadPoolSize;

    @Value("${processor.thread.pool.max}")
    private String processorThreadPoolMax;

    @Bean(name = "asyncDDPRequestsThreadPoolTaskExecutor")
    @StepScope
    public ThreadPoolTaskExecutor asyncDDPRequestsThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(Integer.parseInt(asyncDDPThreadPoolSize));
        threadPoolTaskExecutor.setMaxPoolSize(Integer.parseInt(asyncDDPThreadPoolMax));
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "processorThreadPoolTaskExecutor")
    @StepScope
    public ThreadPoolTaskExecutor processorThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(Integer.parseInt(processorThreadPoolSize));
        threadPoolTaskExecutor.setMaxPoolSize(Integer.parseInt(processorThreadPoolMax));
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean
    @StepScope
    public ItemProcessor<DDPCompositeRecord, Future<CompositeResult>> asyncItemProcessor() {
        AsyncItemProcessor<DDPCompositeRecord, CompositeResult> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(processorThreadPoolTaskExecutor());
        asyncItemProcessor.setDelegate(ddpCompositeProcessor());
        return asyncItemProcessor;
    }

    @Bean
    ItemWriter<Future<CompositeResult>> asyncItemWriter() {
        AsyncItemWriter<CompositeResult> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(ddpCompositeWriter());
        return asyncItemWriter;
    }

    public static final String DDP_COHORT_JOB = "ddpCohortJob";

    @Value("${chunk}")
    private Integer chunkInterval;

    private final Logger LOG = Logger.getLogger(BatchConfiguration.class);

    @Bean
    public Job ddpCohortJob(JobRepository jobRepository,
                            @Qualifier("ddpSeqDateStep") Step ddpSeqDateStep,
                            @Qualifier("ddpStep") Step ddpStep,
                            @Qualifier("ddpSortStep") Step ddpSortStep,
                            @Qualifier("ddpEmailStep") Step ddpEmailStep) {
        return new JobBuilder(DDP_COHORT_JOB, jobRepository)
                .start(ddpSeqDateStep)
                .next(ddpStep)
                .next(ddpSortStep)
                .next(ddpEmailStep)
                .build();
    }

    @Bean(name = "ddpStep")
    public Step ddpStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("ddpStep", jobRepository)
                .<DDPCompositeRecord, Future<CompositeResult>> chunk(chunkInterval, transactionManager)
                .reader(ddpReader())
                .processor(asyncItemProcessor())
                .writer(asyncItemWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<DDPCompositeRecord> ddpReader() {
        return new DDPReader();
    }

    @Bean
    @StepScope
    public DDPCompositeProcessor ddpCompositeProcessor() {
        return new DDPCompositeProcessor();
    }

    @Bean
    @StepScope
    public ClinicalProcessor clinicalProcessor() {
        return new ClinicalProcessor();
    }

    @Bean
    @StepScope
    public AgeAtSeqDateProcessor ageAtSeqDateProcessor() {
        return new AgeAtSeqDateProcessor();
    }

    @Bean
    @StepScope
    public SuppVitalStatusProcessor suppVitalStatusProcessor() {
        return new SuppVitalStatusProcessor();
    }

    @Bean
    @StepScope
    public SuppNaaccrMappingsProcessor suppNaaccrMappingsProcessor() {
        return new SuppNaaccrMappingsProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeResult> clinicalWriter() {
        return new ClinicalWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeResult> timelineRadiationWriter() {
        return new TimelineRadiationWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeResult> ageAtSeqDateWriter() {
        return new AgeAtSeqDateWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeResult> timelineChemoWriter() {
        return new TimelineChemoWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeResult> timelineSurgeryWriter() {
        return new TimelineSurgeryWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeResult> suppVitalStatusWriter() {
        return new SuppVitalStatusWriter();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<CompositeResult> suppNaaccrMappingsWriter() {
        return new SuppNaaccrMappingsWriter();
    }

    @Bean
    @StepScope
    public CompositeItemWriter<CompositeResult> ddpCompositeWriter() {
        CompositeItemWriter<CompositeResult> writer = new CompositeItemWriter<>();
        List<ItemWriter<? super CompositeResult>> delegates = new ArrayList<>();
        delegates.add(clinicalWriter());
        delegates.add(ageAtSeqDateWriter());
        delegates.add(timelineRadiationWriter());
        delegates.add(timelineChemoWriter());
        delegates.add(timelineSurgeryWriter());
        delegates.add(suppVitalStatusWriter());
        delegates.add(suppNaaccrMappingsWriter());
        writer.setDelegates(delegates);
        return writer;
    }

    @Bean(name = "ddpSeqDateStep")
    public Step ddpSeqDateStep(JobRepository jobRepository,
                               @Qualifier("ddpSeqDateTasklet") Tasklet ddpSeqDateTasklet,
                               PlatformTransactionManager transactionManager) {
        return new StepBuilder("ddpSeqDateStep", jobRepository)
        .tasklet(ddpSeqDateTasklet, transactionManager)
        .build();
    }

    @Bean(name = "ddpSeqDateTasklet")
    @StepScope
    public Tasklet ddpSeqDateTasklet() {
        return new DDPSeqDateTasklet();
    }

    @Bean(name = "ddpSortStep")
    public Step ddpSortStep(JobRepository jobRepository,
                            @Qualifier("ddpSortTasklet") Tasklet ddpSortTasklet,
                            PlatformTransactionManager transactionManager) {
        return new StepBuilder("ddpSortStep", jobRepository)
        .tasklet(ddpSortTasklet, transactionManager)
        .build();
    }

    @Bean(name = "ddpSortTasklet")
    @StepScope
    public Tasklet ddpSortTasklet() {
        return new DDPSortTasklet();
    }

    @Bean(name = "ddpEmailStep")
    public Step ddpEmailStep(JobRepository jobRepository,
                             @Qualifier("ddpEmailTasklet") Tasklet ddpEmailTasklet,
                             PlatformTransactionManager transactionManager) {
        return new StepBuilder("ddpEmailStep", jobRepository)
        .tasklet(ddpEmailTasklet, transactionManager)
        .build();
    }

    @Bean(name = "ddpEmailTasklet")
    @StepScope
    public Tasklet ddpEmailTasklet() {
        return new DDPEmailTasklet();
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
