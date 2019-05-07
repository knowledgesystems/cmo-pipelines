/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline;

import org.mskcc.cmo.ks.ddp.source.composite.DDPCompositeRecord;
import org.mskcc.cmo.ks.ddp.pipeline.model.CompositeResult;

import java.util.*;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.batch.integration.async.*;

import java.util.concurrent.Future;
import java.util.concurrent.Executor;
/**
 *
 * @author ochoaa
 */
@Configuration
@EnableBatchProcessing
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
    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk}")
    private Integer chunkInterval;

    private final Logger LOG = Logger.getLogger(BatchConfiguration.class);

    @Bean
    public Job ddpCohortJob() {
        return jobBuilderFactory.get(DDP_COHORT_JOB)
                .start(ddpSeqDateStep())
                .next(ddpStep())
                .next(ddpSortStep())
                .next(ddpEmailStep())
                .build();
    }

    @Bean
    public Step ddpStep() {
        return stepBuilderFactory.get("ddpStep")
                .<DDPCompositeRecord, Future<CompositeResult>> chunk(chunkInterval)
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
    public SuppVitalStatusProcessor suppVitalStatusProcessor() {
        return new SuppVitalStatusProcessor();
    }

    @Bean
    @StepScope
    public SuppAgeProcessor suppAgeProcessor() {
        return new SuppAgeProcessor();
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
    public ItemStreamWriter<CompositeResult> suppAgeWriter() {
        return new SuppAgeWriter();
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
        delegates.add(timelineRadiationWriter());
        delegates.add(timelineChemoWriter());
        delegates.add(timelineSurgeryWriter());
        delegates.add(suppVitalStatusWriter());
        delegates.add(suppAgeWriter());
        delegates.add(suppNaaccrMappingsWriter());
        writer.setDelegates(delegates);
        return writer;
    }

    @Bean
    public Step ddpSeqDateStep() {
        return stepBuilderFactory.get("ddpSeqDateStep")
        .tasklet(ddpSeqDateTasklet())
        .build();
    }

    @Bean
    @StepScope
    public Tasklet ddpSeqDateTasklet() {
        return new DDPSeqDateTasklet();
    }

    @Bean
    public Step ddpSortStep() {
        return stepBuilderFactory.get("ddpSortStep")
        .tasklet(ddpSortTasklet())
        .build();
    }

    @Bean
    @StepScope
    public Tasklet ddpSortTasklet() {
        return new DDPSortTasklet();
    }

    @Bean
    public Step ddpEmailStep() {
        return stepBuilderFactory.get("ddpEmailStep")
        .tasklet(ddpEmailTasklet())
        .build();
    }

    @Bean
    @StepScope
    public Tasklet ddpEmailTasklet() {
        return new DDPEmailTasklet();
    }
}
