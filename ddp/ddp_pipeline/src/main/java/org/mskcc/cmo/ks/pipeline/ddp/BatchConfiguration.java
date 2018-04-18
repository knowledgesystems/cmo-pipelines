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

package org.mskcc.cmo.ks.pipeline.ddp;

import org.mskcc.cmo.ks.pipeline.ddp.cohort.AuthorizedCohortsTasklet;
import org.mskcc.cmo.ks.ddp.source.composite.CompositePatient;

import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.*;
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

    public static final String AUTH_COHORTS_JOB = "authorizedCohortsJob";
    public static final String PEDIATRIC_COHORT_JOB = "pediatricJob";
    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk}")
    private Integer chunkInterval;

    private final Logger LOG = Logger.getLogger(BatchConfiguration.class);

    @Bean
    public Job authorizedCohortsJob() {
        return jobBuilderFactory.get(AUTH_COHORTS_JOB)
                .start(authorizedCohortsStep())
                .build();
    }

    @Bean
    public Step authorizedCohortsStep() {
        return stepBuilderFactory.get("authorizedCohortsStep")
                .tasklet(authorizedCohortsTasklet())
                .build();
    }

    @Bean
    @StepScope
    public Tasklet authorizedCohortsTasklet() {
        return new AuthorizedCohortsTasklet();
    }

    @Bean
    public Job pediatricJob() {
        return jobBuilderFactory.get(PEDIATRIC_COHORT_JOB)
                .start(pediatricStep())
                .build();
    }

    @Bean
    public Step pediatricStep() {
        return stepBuilderFactory.get("pediatricStep")
                .<CompositePatient, String> chunk(chunkInterval)
                .reader(pediatricReader())
                .processor(pediatricProcessor())
                .writer(pediatricWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<CompositePatient> pediatricReader() {
        return new PediatricReader();
    }

    @Bean
    @StepScope
    public PediatricProcessor pediatricProcessor() {
        return new PediatricProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<String> pediatricWriter() {
        return new PediatricWriter();
    }
}
