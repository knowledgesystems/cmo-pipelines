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

package org.cbioportal.cmo.pipelines.cvr.clinical;

import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.model.staging.CVRClinicalRecord;

import java.io.File;
import java.util.*;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author ochoaa
 */
public class GMLClinicalTasklet implements Tasklet {
    
    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Value("#{jobParameters[clinicalFilename]}")
    private String clinicalFilename;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    private List<CVRClinicalRecord> clinicalRecords = new ArrayList();
    private Logger LOG = Logger.getLogger(GMLClinicalTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        File clinicalFile = new File(stagingDirectory, clinicalFilename);
        try {
            loadClinicalDataGmlPatientSampleMapping(clinicalFile);
        } catch (Exception e) {
            LOG.error("Error loading clinical data from: " + clinicalFile.getName());
            throw new ItemStreamException(e);
        }
        return RepeatStatus.FINISHED;
    }

    private void loadClinicalDataGmlPatientSampleMapping(File clinicalFile) throws Exception {
        // load clinical file and create patient-sample mapping
        if (!clinicalFile.exists()) {
            throw new ItemStreamException("Could not find clinical file: " + clinicalFile.getName());
        }
        else {
            LOG.info("Loading clinical data from: " + clinicalFile.getName());
            DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
            DefaultLineMapper<CVRClinicalRecord> mapper = new DefaultLineMapper<>();
            mapper.setLineTokenizer(tokenizer);
            mapper.setFieldSetMapper(new CVRClinicalFieldSetMapper());
                        
            FlatFileItemReader<CVRClinicalRecord> reader = new FlatFileItemReader<>();
            reader.setResource(new FileSystemResource(clinicalFile));
            reader.setLineMapper(mapper);
            reader.setLinesToSkip(1);
            reader.open(new ExecutionContext());
            CVRClinicalRecord to_add;
            while ((to_add = reader.read()) != null) {
                cvrSampleListUtil.updateGmlPatientSampleMap(to_add.getPATIENT_ID(), to_add.getSAMPLE_ID());
                clinicalRecords.add(to_add);
                cvrSampleListUtil.addPortalSample(to_add.getSAMPLE_ID());
            }
            reader.close();
        }
        // updates portalSamplesNotInDmpList and dmpSamplesNotInPortal sample lists
        // portalSamples list is only updated if threshold check for max num samples to remove passes
        cvrSampleListUtil.updateSampleLists();
        updateSamplesRemovedList();
    }

    /**
     * Updates cvrSampleListUtil list 'samples removed'.
     * Removed samples are those which are no longer in the DMP master list but
     * still exist in the portal dataset. We do not want to accidentally reintroduce
     * any samples during the GML fetch that shouldn't be in the final dataset.
     */
    private void updateSamplesRemovedList() {
        for (CVRClinicalRecord record : clinicalRecords) {
            if (!cvrSampleListUtil.getPortalSamples().contains(record.getSAMPLE_ID())) {
                cvrSampleListUtil.addSampleRemoved(record.getSAMPLE_ID());
            }
        }
    }
}
