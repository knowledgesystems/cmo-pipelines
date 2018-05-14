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

/**
 *
 * @author ochoaa
 */
@Configuration
@EnableBatchProcessing
@ComponentScan(basePackages = "org.mskcc.cmo.ks.ddp.source")
public class BatchConfiguration {

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
                .start(ddpStep())
                .next(ddpEmailStep())
                .build();
    }

    @Bean
    public Step ddpStep() {
        return stepBuilderFactory.get("ddpStep")
                .<DDPCompositeRecord, CompositeResult> chunk(chunkInterval)
                .reader(ddpReader())
                .processor(ddpCompositeProcessor())
                .writer(ddpCompositeWriter())
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
    public CompositeItemWriter<CompositeResult> ddpCompositeWriter() {
        CompositeItemWriter<CompositeResult> writer = new CompositeItemWriter<>();
        List<ItemWriter<? super CompositeResult>> delegates = new ArrayList<>();
        delegates.add(clinicalWriter());
        delegates.add(timelineRadiationWriter());
        delegates.add(timelineChemoWriter());
        delegates.add(timelineSurgeryWriter());
        writer.setDelegates(delegates);
        return writer;
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
