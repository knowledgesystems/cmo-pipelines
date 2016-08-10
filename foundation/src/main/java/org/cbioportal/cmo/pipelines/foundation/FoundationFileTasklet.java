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

import org.cbioportal.cmo.pipelines.foundation.model.CaseType;
import org.cbioportal.cmo.pipelines.foundation.model.ClientCaseInfoType;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.bind.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author ochoaa
 */
public class FoundationFileTasklet implements Tasklet {
    
    @Value("#{jobParameters[sourceDirectory]}")
    private String sourceDir;
        
    private static final Log LOG = LogFactory.getLog(FoundationFileTasklet.class);
    
    @Override
    public RepeatStatus execute(StepContribution stepContext, ChunkContext chunkContext) throws Exception {        
        
        File sourceDirectory = new File(sourceDir);    
        
        List<CaseType> fmiCaseList = new ArrayList();        
        for (File xmlFile : sourceDirectory.listFiles()) {
            if (xmlFile.isDirectory() || !xmlFile.getName().endsWith(".xml")) {
                LOG.info("Skipping sub-directory or file in source directory: " + xmlFile.getName());
                continue;
            }           
            
            List<CaseType> newCases = new ArrayList();
            try {
                LOG.info("Extracting case data from: " + xmlFile.getName());
                JAXBContext context = JAXBContext.newInstance(ClientCaseInfoType.class);
                Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
                Path xmlFilePath = Paths.get(xmlFile.getCanonicalPath());
                ClientCaseInfoType cci = (ClientCaseInfoType) jaxbUnmarshaller.unmarshal(new FileInputStream(xmlFilePath.toFile()));
                newCases = cci.getCases().getCase();                
            } 
            catch (JAXBException | IOException ex){
                LOG.error("Error processing file: " + xmlFile.getName() + " " + ex.getMessage());
                ex.printStackTrace();
            }
            
            // add new cases to list
            fmiCaseList.addAll(newCases);        
        }
        
        // add list of cases to the execution context
        LOG.info("Adding " + fmiCaseList.size() + " cases to execution context.");
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("fmiCaseList", fmiCaseList);
        return RepeatStatus.FINISHED;
    }
}
