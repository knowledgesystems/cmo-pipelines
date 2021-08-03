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
package org.mskcc.cmo.ks.redcap.pipeline;

import java.net.MalformedURLException;
import java.util.*;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.item.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
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
@EnableBatchProcessing
@ComponentScan(basePackages="org.mskcc.cmo.ks.redcap.source.internal")
@PropertySource("classpath:application.properties")
public class BatchConfiguration {
    public static final String REDCAP_EXPORT_JOB = "redcapExportJob";
    public static final String REDCAP_RAW_EXPORT_JOB = "redcapRawExportJob";
    public static final String REDCAP_IMPORT_JOB = "redcapImportJob";

    private final Logger log = Logger.getLogger(BatchConfiguration.class);

    @Value("${chunk}")
    private Integer chunkInterval;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    // Will keep calling clinicalDataStep or timelineDataStep based on the exit status from the clinicalDataStepListener
    @Bean
    public Job redcapExportJob() {
        return jobBuilderFactory.get(REDCAP_EXPORT_JOB)
                .start(exportClinicalDataStep())
                .next(exportTimelineDataStep())
                .build();
    }

    // Will keep calling clinicalDataStep or timelineDataStep based on the exit status from the clinicalDataStepListener
    @Bean
    public Job redcapRawExportJob() {
        return jobBuilderFactory.get(REDCAP_RAW_EXPORT_JOB)
                .start(exportRawClinicalDataStep())
                    .on("CLINICAL")
                    .to(exportRawClinicalDataStep())
                    .on("COMPLETED").end()
                    .on("TIMELINE")
                        .to(exportRawTimelineDataStep())
                        .on("TIMELINE")
                        .to(exportRawTimelineDataStep())
                        .on("COMPLETED").end()
                .build()
                .build();
    }

    @Bean
    public Job redcapImportJob() {
        return jobBuilderFactory.get(REDCAP_IMPORT_JOB)
                .start(importRedcapProjectDataStep())
                .build();
    }

    @Bean
    public Step exportClinicalDataStep() {
        return stepBuilderFactory.get("exportClinicalDataStep")
                .listener(exportClinicalDataStepListener())
                .<Map<String, String>, ClinicalDataComposite> chunk(chunkInterval)
                .reader(clinicalDataReader())
                .processor(clinicalDataProcessor())
                .writer(clinicalDataWriter())
                .build();
    }

    @Bean
    public Step exportRawClinicalDataStep() {
        return stepBuilderFactory.get("exportRawClinicalDataStep")
                .listener(exportRawClinicalDataStepListener())
                .<Map<String, String>, String> chunk(chunkInterval)
                .reader(clinicalDataReader())
                .processor(rawClinicalDataProcessor())
                .writer(rawClinicalDataWriter())
                .build();
    }

    @Bean
    public Step exportTimelineDataStep() {
        return stepBuilderFactory.get("exportTimelineDataStep")
                .listener(exportTimelineDataStepListener())
                .<Map<String, String>, String> chunk(chunkInterval)
                .reader(timelineReader())
                .processor(timelineProcessor())
                .writer(timelineWriter())
                .build();
    }

    @Bean
    public Step exportRawTimelineDataStep() {
        return stepBuilderFactory.get("exportRawTimelineDataStep")
                .listener(exportRawTimelineDataStepListener())
                .<Map<String, String>, String> chunk(chunkInterval)
                .reader(timelineReader())
                .processor(timelineProcessor())
                .writer(timelineWriter())
                .build();
    }

    @Bean
    public Step importRedcapProjectDataStep() {
        return stepBuilderFactory.get("importRedcapProjectDataStep")
                .tasklet(importRedcapProjectDataTasklet())
                .build();
    }

    @Bean
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

    @Bean
    @StepScope
    @SuppressWarnings("unchecked")
    public CompositeItemProcessor<Map<String, String>, ClinicalDataComposite> clinicalDataProcessor() {
        CompositeItemProcessor<Map<String, String>, ClinicalDataComposite> processor = new CompositeItemProcessor<>();
        //TODO combine clinical data processors into a single class in order to avoid the need for a mixed type list
        List delegates = new ArrayList();
        delegates.add(sampleProcessor());
        delegates.add(patientProcessor());
        processor.setDelegates(delegates);
        return processor;
    }

    @Bean
    @StepScope
    public RawClinicalDataProcessor rawClinicalDataProcessor() {
        return new RawClinicalDataProcessor();
    }

    @Bean
    @StepScope
    @SuppressWarnings("unchecked")
    public CompositeItemWriter<ClinicalDataComposite> clinicalDataWriter() {
        CompositeItemWriter<ClinicalDataComposite> writer = new CompositeItemWriter<>();
        //TODO combine clinical data writers into a single class in order to avoid the need for a mixed type list
        List delegates = new ArrayList();
        delegates.add(sampleWriter());
        delegates.add(patientWriter());
        writer.setDelegates(delegates);
        return writer;
    }

    @Bean
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
