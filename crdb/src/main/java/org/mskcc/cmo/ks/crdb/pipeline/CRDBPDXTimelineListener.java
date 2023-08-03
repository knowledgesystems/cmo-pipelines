/*
 * Copyright (c) 2019, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

package org.mskcc.cmo.ks.crdb.pipeline;

import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.EmailUtil;
import org.mskcc.cmo.ks.crdb.pipeline.model.CRDBPDXTimelineDataset;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author ochoaa
 */
public class CRDBPDXTimelineListener implements StepExecutionListener {

    @Autowired
    private EmailUtil emailUtil;

    @Value("${email.sender}")
    private String sender;

    @Value("${pdx.email.recipient}")
    private String pdxRecipient;

    @Value("${email.recipient}")
    private String defaultRecipient;

    @Value("${email.subject}")
    private String pdxEmailSubject;

    @Autowired
    private CRDBPDXTimelineProcessor pdxTimelineProcessor;

    private final Logger LOG = Logger.getLogger(CRDBPDXTimelineListener.class);

    @Override
    public void beforeStep(StepExecution se) {}

    @Override
    public ExitStatus afterStep(StepExecution se) {
        List<CRDBPDXTimelineDataset> nullStartDateTimelinePatients = (List<CRDBPDXTimelineDataset>) se.getExecutionContext().get("nullStartDateTimelinePatients");
        List<String> crdbPdxTimelineFieldOrder = (List<String>) se.getJobExecution().getExecutionContext().get("crdbPdxTimelineFieldOrder");

        if (nullStartDateTimelinePatients.size() > 0) {
            StringBuilder body = new StringBuilder();
            body.append("The following PDX timeline records had invalid START_DATE values - these need to be integers.\n\n");
            body.append("PATIENT_ID\tPDX_ID\tEVENT_TYPE\tEVENT_TYPE_DETAILED\n");

            Set<String> records = new HashSet<>();
            for (CRDBPDXTimelineDataset record : nullStartDateTimelinePatients) {
                String[] recordDetails = {record.getPATIENT_ID(), record.getPDX_ID(), record.getEVENT_TYPE(), record.getEVENT_TYPE_DETAILED()};
                records.add(String.join("\t", recordDetails));
            }
            body.append(String.join("\n", records))
                .append("\n");
            emailUtil.sendEmail(sender, new String[]{pdxRecipient, defaultRecipient}, pdxEmailSubject, body.toString());
        }
        return ExitStatus.COMPLETED;
    }

}
