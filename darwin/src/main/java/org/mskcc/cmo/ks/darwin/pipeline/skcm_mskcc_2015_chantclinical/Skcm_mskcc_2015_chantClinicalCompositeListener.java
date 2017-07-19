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

package org.mskcc.cmo.ks.darwin.pipeline.skcm_mskcc_2015_chantclinical;

import java.io.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.batch.core.*;
import org.springframework.beans.factory.annotation.*;

public class Skcm_mskcc_2015_chantClinicalCompositeListener implements StepExecutionListener {
    Logger log = Logger.getLogger(Skcm_mskcc_2015_chantClinicalCompositeListener.class);

    @Autowired
    public ClinicalDataSource clinicalDataSource;
    
    @Value("${redcap.project_title_for_darwin_skcm_2015_chant_clinical_sample_import}")
    public String redcapProjectTitle;

    @Value("${darwin.skcm_mskcc_2015_chant_clinical_sample_filename}")
    private String clinicalSampleFilename;

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String outputDirectory = stepExecution.getJobExecution().getJobParameters().getString("outputDirectory");
        File stagingFile = new File(outputDirectory, clinicalSampleFilename);
        if (stagingFile.exists()) {
            try {
                String filename = stagingFile.getCanonicalPath();
                clinicalDataSource.importClinicalDataFile(redcapProjectTitle, filename);
            } catch (IOException e) {
                log.error("Error: could not persist data file \"" + clinicalSampleFilename + "\" in directory \"" + outputDirectory + "\" to RedCap : IO error locating/reading file");
            }
        } else {
            log.error("Error: could not persist data file \"" + clinicalSampleFilename + "\" in directory \"" + outputDirectory + "\" to RedCap : file does not exist");
        }
        return ExitStatus.COMPLETED;
    }

}
