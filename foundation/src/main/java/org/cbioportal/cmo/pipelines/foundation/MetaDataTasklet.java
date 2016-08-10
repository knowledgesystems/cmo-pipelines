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

package org.cbioportal.cmo.pipelines.foundation;

import java.util.*;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 * Tasklet for loading meta data and writing meta staging files. 
 * @author ochoaa
 */
public class MetaDataTasklet implements Tasklet {

    @Value("#{jobParameters[outputDirectory]}")
    private String outputDirectory;
    
    @Value("#{jobParameters[cancerStudyId]}")
    private String cancerStudyIdentifier;

    @Resource(name="mutationsMetaData")    
    public Map<String, String> mutationsMetaData;

    @Resource(name="cnaMetaData")    
    public Map<String, String> cnaMetaData;

    @Resource(name="fusionsMetaData")    
    public Map<String, String> fusionsMetaData;    
    
    private static final Log LOG = LogFactory.getLog(MetaDataTasklet.class);
    
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
        writeMutationsMetaFile(executionContext);
        writeCnaMetaFile(executionContext);
        writeFusionsMetaFile(executionContext) ;
        
        return RepeatStatus.FINISHED;
    }
    
    /**
     * Load/Modify mutations meta data and write to meta file.
     * @param executionContext
     * @throws Exception 
     */
    private void writeMutationsMetaFile(ExecutionContext executionContext) throws Exception {
        String metaFilename = outputDirectory + "meta_mutations_extended.txt";
        
        List<String> mutationsData = new ArrayList();
        for (String key : mutationsMetaData.keySet()) {
            String value = mutationsMetaData.get(key).replace("<study_id>", cancerStudyIdentifier);
            mutationsData.add(key+":"+value);
        }

        LOG.info("Writing clinical meta file: " + metaFilename);
        writeMetaStagingFile(executionContext, metaFilename, mutationsData);
    }
    
    /**
     * Load/Modify CNA meta data and write to meta file.
     * @param executionContext
     * @throws Exception 
     */    
    private void writeCnaMetaFile(ExecutionContext executionContext) throws Exception {
        String metaFilename = outputDirectory + "meta_CNA.txt";
        
        List<String> cnaData = new ArrayList();
        for (String key : cnaMetaData.keySet()) {
            String value = cnaMetaData.get(key).replace("<study_id>", cancerStudyIdentifier);
            cnaData.add(key+":"+value);
        }

        LOG.info("Writing CNA meta file: " + metaFilename);
        writeMetaStagingFile(executionContext, metaFilename, cnaData);
    }  
    
    /**
     * Load/Modify fusions meta data and write to meta file.
     * @param executionContext
     * @throws Exception 
     */    
    private void writeFusionsMetaFile(ExecutionContext executionContext) throws Exception {
        String metaFilename = outputDirectory + "meta_fusions.txt";
        
        List<String> fusionsData = new ArrayList();
        for (String key : fusionsMetaData.keySet()) {
            String value = fusionsMetaData.get(key).replace("<study_id>", cancerStudyIdentifier);
            fusionsData.add(key+":"+value);
        }

        LOG.info("Writing mutations meta file: " + metaFilename);
        writeMetaStagingFile(executionContext, metaFilename, fusionsData);
    }      
    
    /**
     * Create and Write meta data file.
     * @param executionContext
     * @throws Exception 
     */    
    private void writeMetaStagingFile(ExecutionContext executionContext, String metaFilename, List<String> metaData) throws Exception {
        FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();
        PassThroughLineAggregator aggr = new PassThroughLineAggregator();

        try {       
            flatFileItemWriter.setLineAggregator(aggr);
            flatFileItemWriter.setResource(new FileSystemResource(metaFilename));
            flatFileItemWriter.open(executionContext);
            flatFileItemWriter.write(metaData);
            flatFileItemWriter.close();            
        }
        catch (Exception ex) {
            LOG.error("Error writing meta data file: " + metaFilename);
            ex.printStackTrace();
        }        

    }
    
}
