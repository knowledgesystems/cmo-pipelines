/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author heinsz
 */
public class MetaFileStepTasklet implements Tasklet {

    @Value("#{jobParameters[redcapProject]}")
    private String cancerStudyIdentifier;

    @Value("#{jobParameters[directory]}")
    private String directory;
    
    @Value("#{jobParameters[mergeClinicalDataSources]}")
    private boolean mergeClinicalDataSources;

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        ExecutionContext jobExecutionContext = cc.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        List<String> clinicalSampleFiles = (List<String>)jobExecutionContext.get("clinicalSampleFiles");
        List<String> clinicalPatientFiles = (List<String>)jobExecutionContext.get("clinicalPatientFiles");
        List<String> timelineFiles = (List<String>)jobExecutionContext.get("timelineFiles");
        for (String clinicalSampleFile : clinicalSampleFiles) {
            createMetaFile(clinicalSampleFile, "SAMPLE_ATTRIBUTES", "clinical");
        }
        for (String clinicalPatientFile : clinicalPatientFiles) {
            createMetaFile(clinicalPatientFile, "PATIENT_ATTRIBUTES", "clinical");
        }
        // only want to ever create one timeline file
        if (!timelineFiles.isEmpty() && mergeClinicalDataSources) {
            createMetaFile("data_timeline.txt", "TIMELINE", "timeline");
        }
        else {
            for (String timelineFile : timelineFiles) {
                createMetaFile(timelineFile, "TIMELINE", "timeline");
            }
        }
        return RepeatStatus.FINISHED;
    }

    private void createMetaFile(String clinicalFilename, String datatype, String replacementContext) throws Exception {
        List<String> rows = new ArrayList<>();
        rows.add("cancer_study_identifier: " + cancerStudyIdentifier);
        rows.add("genetic_alteration_type: CLINICAL");
        rows.add("datatype: " + datatype);
        rows.add("data_filename: " + clinicalFilename);
        Path file = Paths.get(directory, clinicalFilename.replace("data_" + replacementContext, "meta_" + replacementContext));
        Files.write(file, rows, StandardCharsets.UTF_8);
    }

}
