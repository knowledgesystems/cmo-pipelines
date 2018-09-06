/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

import java.io.File;
import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemStreamException;

/**
 *
 * @author ochoaa
 */
public class CVRClinicalDataListener implements StepExecutionListener {

    private static Logger log = Logger.getLogger(CVRClinicalDataListener.class);

    @Override
    public void beforeStep(StepExecution se) {}

    @Override
    public ExitStatus afterStep(StepExecution se) {
        File clinicalFile = (File) se.getExecutionContext().get("clinicalFile");
        int clinicalRecordsWritten = se.getExecutionContext().getInt("clinicalRecordsWritten");
        if (clinicalRecordsWritten == 0) {
            String errorMessage = "No data written to clinical file! '" + clinicalFile.getName() + "'" +
                    " - check JSON for invalid patient IDs and/or masterlist for applicable studies. " +
                    "Removing file and exiting...";
            log.error(errorMessage);
            clinicalFile.delete();
            throw new ItemStreamException(errorMessage);
        }
        return ExitStatus.COMPLETED;
    }

}
