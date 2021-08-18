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

import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.pipeline.util.MetaFileUtil;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author heinsz
 */
public class TimelineDataStepListener implements StepExecutionListener {

    @Autowired
    public ClinicalDataSource clinicalDataSource;

    private final List<String> standardTimelineDataFields = new ArrayList<String>(Arrays.asList("PATIENT_ID", "START_DATE", "STOP_DATE", "EVENT_TYPE"));
    private final Logger log = Logger.getLogger(TimelineDataStepListener.class);

    @Override
    public void beforeStep(StepExecution se) {
        se.getExecutionContext().put("standardTimelineDataFields", standardTimelineDataFields);
        log.info("Starting a timeline data step");
    }

    @Override
    public ExitStatus afterStep(StepExecution se) {
        boolean writeTimelineData = (boolean)se.getExecutionContext().get("writeTimelineData");
        String stableId = se.getJobParameters().getString("stableId");
        String directory = se.getJobParameters().getString("directory");
        if (writeTimelineData) {
            String timelineFilename = (String)se.getExecutionContext().get("timelineFile");
            try {
                MetaFileUtil.createMetaTimelineFile(directory, timelineFilename, stableId);
            } catch (IOException ex) {
                log.error("Error writing meta timeline file: " + stableId);
                throw new ItemStreamException(ex);
            }
        }
        return ExitStatus.COMPLETED;
    }

}
