/*
 * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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
 * This file is part of cBioPortal.
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

package org.cbioportal.cmo.pipelines.importer.config.clinical;

import javax.annotation.Resource;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.logging.*;
import org.springframework.batch.core.*;

/**
 *
 * @author ochoaa
 */
public class ClinicalDataListener implements StepExecutionListener {
    
    @Resource(name="clinicalMetadata")
    MultiKeyMap clinicalMetadata;
    
    private static final Log LOG = LogFactory.getLog(ClinicalDataListener.class);
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        // get new attributes count
        int newClinicalAttributes = stepExecution.getExecutionContext().getInt("newClinicalAttributes", 0);
        if (newClinicalAttributes > 0) {
            LOG.info("New clinical attributes imported: " + newClinicalAttributes);
        }
        
        // insert counts into execution context for patient and sample clinical records
        stepExecution.getExecutionContext().put("patientCount", 0);
        stepExecution.getExecutionContext().put("sampleCount", 0);
        stepExecution.getExecutionContext().put("patientDataCount", 0);
        stepExecution.getExecutionContext().put("sampleDataCount", 0);        
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        
        // get counts for patient, sample, and clinical data records inserted
        int patientCount = stepExecution.getExecutionContext().getInt("patientCount");
        int sampleCount = stepExecution.getExecutionContext().getInt("sampleCount");
        int patientDataCount = stepExecution.getExecutionContext().getInt("patientDataCount");
        int sampleDataCount = stepExecution.getExecutionContext().getInt("sampleDataCount");
                
        if ((patientCount+sampleCount+patientDataCount+sampleDataCount) == 0) {
            LOG.error("No records were imported from: " + clinicalMetadata.get(stepName,"data_filename"));
        }
        else {
            // log the record counts
            LOG.info("Patient records imported: " + patientCount);
            LOG.info("Patient clinical data records imported: " + patientDataCount);
            LOG.info("Sample records imported: " + sampleCount);
            LOG.info("Sample clinical data records imported: " + sampleDataCount);
        }
        
        // log rollbacks and number of items skipped during current step execution
        LOG.info("Rollbacks during " + stepName + ": " + stepExecution.getRollbackCount());
        LOG.info("Items skipped " + stepName + ": " + stepExecution.getSkipCount());
        return ExitStatus.COMPLETED;
    }
}
