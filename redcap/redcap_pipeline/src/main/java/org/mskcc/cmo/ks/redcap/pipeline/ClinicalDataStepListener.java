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

package org.mskcc.cmo.ks.redcap.pipeline;

import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author heinsz
 */
public class ClinicalDataStepListener implements StepExecutionListener {
    private final Logger log = Logger.getLogger(ClinicalDataStepListener.class);
    @Autowired
    public ClinicalDataSource clinicalDataSource;

    @Override
    public void beforeStep(StepExecution se) {
        log.info("Starting a clinical data step");
    }

    @Override
    public ExitStatus afterStep(StepExecution se) {
        log.info("Checking if more data to process...");
        boolean writeClinicalSample = (boolean)se.getExecutionContext().get("writeClinicalSample");
        boolean writeClinicalPatient = (boolean)se.getExecutionContext().get("writeClinicalPatient");
        String stableId = se.getJobParameters().getString("stableId");
        if (writeClinicalSample) {
            List<String> clinicalSampleFiles = (List<String>)se.getJobExecution().getExecutionContext().get("clinicalSampleFiles");
            if (clinicalSampleFiles == null) {
                clinicalSampleFiles = new ArrayList<>();
            }
            clinicalSampleFiles.add((String)se.getExecutionContext().get("clinicalSampleFile"));
            se.getJobExecution().getExecutionContext().put("clinicalSampleFiles", clinicalSampleFiles);
        }
        if (writeClinicalPatient) {
            List<String> clinicalPatientFiles = (List<String>)se.getJobExecution().getExecutionContext().get("clinicalPatientFiles");
            if (clinicalPatientFiles == null) {
                clinicalPatientFiles = new ArrayList<>();
            }
            clinicalPatientFiles.add((String)se.getExecutionContext().get("clinicalPatientFile"));
            se.getJobExecution().getExecutionContext().put("clinicalPatientFiles", clinicalPatientFiles);
        }
        if (clinicalDataSource.hasMoreClinicalData(stableId)) {
            return new ExitStatus("CLINICAL");
        }
        if (clinicalDataSource.hasMoreTimelineData(stableId)) {
            return new ExitStatus("TIMELINE");
        }
        return ExitStatus.COMPLETED;
    }
}
