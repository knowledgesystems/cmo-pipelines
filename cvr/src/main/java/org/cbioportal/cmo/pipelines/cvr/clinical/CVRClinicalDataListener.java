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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.batch.core.*;
import org.springframework.beans.factory.annotation.*;

public class CVRClinicalDataListener implements StepExecutionListener {
    
    Logger log = Logger.getLogger(CVRClinicalDataListener.class);

    @Autowired
    public ClinicalDataSource clinicalDataSource;
    
    @Autowired
    public CVRUtilities cvrUtilities;

    @Value("${redcapProjetId}")
    public String redcapProjectId;

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stagingDirectory = (String)stepExecution.getJobExecution().getJobParameters().getParameters().get("stagingDirectory").getValue();
        File stagingFile = new File(stagingDirectory, cvrUtilities.CLINICAL_FILE);
        if (stagingFile.exists()) {
            try {
                String clinicalDataFilename = stagingFile.getCanonicalPath();
                clinicalDataSource.importClinicalDataFile(redcapProjectId, clinicalDataFilename);
            } catch (IOException e) {
                log.error("Error: could not persist clinical file \"" + cvrUtilities.CLINICAL_FILE + "\" in directory \"" + stagingDirectory + "\" to RedCap : IO error locating/reading file");
            }
        } else {
            log.error("Error: could not persist clinical file \"" + cvrUtilities.CLINICAL_FILE + "\" in directory \"" + stagingDirectory + "\" to RedCap : file does not exist");
        }
        return ExitStatus.COMPLETED;
    }
    
}
