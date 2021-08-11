/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.gene.config;

import org.mskcc.cmo.ks.gene.*;
import org.mskcc.cmo.ks.gene.model.Gene;

import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.cbioportal.cmo.pipelines.common.util.CommonBatchConfig;

/**
 *
 * @author ochoaa
 */
@Configuration
@EnableBatchProcessing
@Import(CommonBatchConfig.class)
public class BatchConfiguration {

    public static final String GENE_DATA_JOB = "geneDataJob";

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.interval}")
    private int chunkInterval;

    @Bean
    public Job geneDataJob() {
        return jobBuilderFactory.get(GENE_DATA_JOB)
                .start(geneDataStep())
                .build();
    }

    @Bean
    public Step geneDataStep() {
        return stepBuilderFactory.get("geneDataStep")
                .<Gene, Gene> chunk(chunkInterval)
                .reader(geneDataReader())
                .processor(geneDataProcessor())
                .writer(geneDataWriter())
                .listener(geneDataListener())
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<Gene> geneDataReader() {
        return new GeneDataReader();
    }

    @Bean
    @StepScope
    public GeneDataProcessor geneDataProcessor() {
        return new GeneDataProcessor();
    }

    @Bean
    @StepScope
    public ItemStreamWriter<Gene> geneDataWriter() {
        return new GeneDataWriter();
    }

    @Bean
    public StepExecutionListener geneDataListener() {
        return new GeneDataListener();
    }
}
