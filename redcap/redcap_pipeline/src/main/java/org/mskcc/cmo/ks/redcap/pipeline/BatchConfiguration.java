/*
 * Copyright (c) 2016, 2024 Memorial Sloan Kettering Cancer Center.
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
package org.mskcc.cmo.ks.redcap.pipeline;

import java.net.MalformedURLException;
import java.util.*;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

/**
 *
 * @author heinsz
 */

@Configuration
@ComponentScan(basePackages="org.mskcc.cmo.ks.redcap.source.internal")
@PropertySource("classpath:application.properties")
public class BatchConfiguration {
    public static final String REDCAP_EXPORT_JOB = "redcapExportJob";
    public static final String REDCAP_RAW_EXPORT_JOB = "redcapRawExportJob";
    public static final String REDCAP_IMPORT_JOB = "redcapImportJob";

    private final Logger log = Logger.getLogger(BatchConfiguration.class);

    @Value("${chunk}")
    private Integer chunkInterval;

    @Bean(name = "redcapExportJob")
    public Job redcapExportJob(@Qualifier("redcapJobRepository") JobRepository jobRepository,
                               @Qualifier("exportClinicalDataStep") Step exportClinicalDataStep,
                               @Qualifier("exportTimelineDataStep") Step exportTimelineDataStep) {
        return new JobBuilder(REDCAP_EXPORT_JOB, jobRepository)
                .preventRestart()
                .start(exportClinicalDataStep)
                .next(exportTimelineDataStep)
                .build();
    }

    // Will keep calling clinicalDataStep or timelineDataStep based on the exit status from the clinicalDataStepListener
    @Bean(name = "redcapRawExportJob")
    public Job redcapRawExportJob(@Qualifier("redcapJobRepository") JobRepository jobRepository,
                                  @Qualifier("exportRawClinicalDataStep") Step exportRawClinicalDataStep,
                                  @Qualifier("exportRawTimelineDataStep") Step exportRawTimelineDataStep) {
        return new JobBuilder(REDCAP_RAW_EXPORT_JOB, jobRepository)
                .preventRestart()
                .start(exportRawClinicalDataStep)
                    .on("CLINICAL")
                    .to(exportRawClinicalDataStep)
                    .on("COMPLETED").end()
                    .on("TIMELINE")
                        .to(exportRawTimelineDataStep)
                        .on("TIMELINE")
                        .to(exportRawTimelineDataStep)
                        .on("COMPLETED").end()
                .build()
                .build();
    }

    @Bean(name = "redcapImportJob")
    public Job redcapImportJob(@Qualifier("redcapJobRepository") JobRepository jobRepository, @Qualifier("importRedcapProjectDataStep") Step importRedcapProjectDataStep) {
        return new JobBuilder(REDCAP_IMPORT_JOB, jobRepository)
                .preventRestart()
                .start(importRedcapProjectDataStep)
                .build();
    }

    @Bean
    protected Step exportClinicalDataStep(@Qualifier("redcapJobRepository") JobRepository jobRepository,
                                          @Qualifier("redcapTransactionManager") PlatformTransactionManager transactionManager,
                                          @Qualifier("clinicalDataProcessor") CompositeItemProcessor<Map<String, String>, ClinicalDataComposite> processor,
                                          @Qualifier("clinicalDataWriter") CompositeItemWriter<ClinicalDataComposite> writer) {
        return new StepBuilder("exportClinicalDataStep", jobRepository)
                .listener(exportClinicalDataStepListener())
                .<Map<String, String>, ClinicalDataComposite> chunk(chunkInterval, transactionManager)
                .reader(clinicalDataReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    protected Step exportRawClinicalDataStep(@Qualifier("redcapJobRepository") JobRepository jobRepository,
                                             @Qualifier("redcapTransactionManager") PlatformTransactionManager transactionManager,
                                             @Qualifier("rawClinicalDataProcessor") ItemProcessor<Map<String, String>, String> processor,
                                             @Qualifier("rawClinicalDataWriter") ItemStreamWriter<String> writer) {
        return new StepBuilder("exportRawClinicalDataStep", jobRepository)
                .listener(exportRawClinicalDataStepListener())
                .<Map<String, String>, String> chunk(chunkInterval, transactionManager)
                .reader(clinicalDataReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    protected Step exportTimelineDataStep(@Qualifier("redcapJobRepository") JobRepository jobRepository,
                                          @Qualifier("redcapTransactionManager") PlatformTransactionManager transactionManager,
                                          @Qualifier("timelineProcessor") TimelineProcessor processor,
                                          @Qualifier("timelineWriter") TimelineWriter writer) {
        return new StepBuilder("exportTimelineDataStep", jobRepository)
                .listener(exportTimelineDataStepListener())
                .<Map<String, String>, String> chunk(chunkInterval, transactionManager)
                .reader(timelineReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    protected Step exportRawTimelineDataStep(@Qualifier("redcapJobRepository") JobRepository jobRepository,
                                             @Qualifier("redcapTransactionManager") PlatformTransactionManager transactionManager,
                                             @Qualifier("timelineProcessor") TimelineProcessor processor,
                                             @Qualifier("timelineWriter") TimelineWriter writer) {
        return new StepBuilder("exportRawTimelineDataStep", jobRepository)
                .listener(exportRawTimelineDataStepListener())
                .<Map<String, String>, String> chunk(chunkInterval, transactionManager)
                .reader(timelineReader())
                .processor(processor)
                .writer(writer)
                .build();
    }


    @Bean
    protected Step importRedcapProjectDataStep(@Qualifier("redcapJobRepository") JobRepository jobRepository,
                                               @Qualifier("redcapTransactionManager") PlatformTransactionManager transactionManager,
                                               @Qualifier("importRedcapProjectData") Tasklet tasklet) {
        return new StepBuilder("importRedcapProjectDataStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean(name = "importRedcapProjectData")
    @StepScope
    public Tasklet importRedcapProjectDataTasklet() {
        return new ImportRedcapProjectDataTasklet();
    }

    // clinical data processor / writers / listeners
    @Bean
    @StepScope
    public ItemStreamReader<Map<String, String>> clinicalDataReader() {
        return new ClinicalDataReader();
    }

    // Using a composite processor pattern to avoid having to hit redcap api more than needed.
    // Sample processor/writer leads into the patient processor writer - the composite result object is passed along
    // which contains the data necessary for the next processor/writer and the result of the processors.
    // The writers pull out the data they need from the composite result.

    @Bean(name = "clinicalDataProcessor")
    @StepScope
    public CompositeItemProcessor<Map<String, String>, ClinicalDataComposite> clinicalDataProcessor() {
        CompositeItemProcessor<Map<String, String>, ClinicalDataComposite> processor = new CompositeItemProcessor<>();
        //TODO combine clinical data processors into a single class in order to avoid the need for a mixed type list
        List delegates = new ArrayList();
        delegates.add(sampleProcessor());
        delegates.add(patientProcessor());
        processor.setDelegates(delegates);
        return processor;
    }

    @Bean(name = "rawClinicalDataProcessor")
    @StepScope
    public RawClinicalDataProcessor rawClinicalDataProcessor() {
        return new RawClinicalDataProcessor();
    }

    @Bean(name = "clinicalDataWriter")
    @StepScope
    public CompositeItemWriter<ClinicalDataComposite> clinicalDataWriter() {
        CompositeItemWriter<ClinicalDataComposite> writer = new CompositeItemWriter<>();
        //TODO combine clinical data writers into a single class in order to avoid the need for a mixed type list
        List delegates = new ArrayList();
        delegates.add(sampleWriter());
        delegates.add(patientWriter());
        writer.setDelegates(delegates);
        return writer;
    }

    @Bean(name = "rawClinicalDataWriter")
    @StepScope
    public ItemStreamWriter<String> rawClinicalDataWriter() {
        return new RawClinicalDataWriter();
    }

    @Bean
    @StepScope
    public ClinicalSampleDataProcessor sampleProcessor() {
        ClinicalSampleDataProcessor sampleProcessor = new ClinicalSampleDataProcessor();
        return sampleProcessor;
    }

    @Bean
    @StepScope
    public ClinicalPatientDataProcessor patientProcessor() {
        ClinicalPatientDataProcessor patientProcessor = new ClinicalPatientDataProcessor();
        return patientProcessor;
    }

    @Bean
    @StepScope
    public ClinicalSampleDataWriter sampleWriter() {
        ClinicalSampleDataWriter sampleWriter = new ClinicalSampleDataWriter();
        return sampleWriter;
    }

    @Bean
    @StepScope
    public ClinicalPatientDataWriter patientWriter() {
        ClinicalPatientDataWriter patientWriter = new ClinicalPatientDataWriter();
        return patientWriter;
    }

    @Bean
    public ClinicalDataStepListener exportClinicalDataStepListener() {
        return new ClinicalDataStepListener();
    }

    @Bean
    public RawClinicalDataStepListener exportRawClinicalDataStepListener() {
        return new RawClinicalDataStepListener();
    }

    // timeline processor / writers / listeners
    @Bean
    @StepScope
    public ItemStreamReader<Map<String, String>> timelineReader() {
        return new TimelineReader();
    }

    @Bean
    @StepScope
    public TimelineProcessor timelineProcessor() {
        return new TimelineProcessor();
    }

    @Bean
    @StepScope
    public TimelineWriter timelineWriter() {
        return new TimelineWriter();
    }

    @Bean
    public TimelineDataStepListener exportTimelineDataStepListener() {
        return new TimelineDataStepListener();
    }

    @Bean
    public RawTimelineDataStepListener exportRawTimelineDataStepListener() {
        return new RawTimelineDataStepListener();
    }

    // general spring batch configuration
    @Value("org/springframework/batch/core/schema-drop-sqlite.sql")
    private Resource dropRepositoryTables;

    @Value("org/springframework/batch/core/schema-sqlite.sql")
    private Resource dataRepositorySchema;

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

    @Bean
    public DataSource dataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:repository.sqlite");
        return dataSource;
    }

    @Bean(name = "redcapTransactionManager")
    public PlatformTransactionManager getTransactionManager() {
        return new ResourcelessTransactionManager();
    }

    @Bean(name = "redcapJobRepository")
    public JobRepository getJobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource());
        factory.setTransactionManager(getTransactionManager());
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
