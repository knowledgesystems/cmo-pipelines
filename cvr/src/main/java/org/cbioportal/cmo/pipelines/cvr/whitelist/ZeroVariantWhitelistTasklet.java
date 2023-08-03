/*
 * Copyright (c) 2018, 2023 Memorial Sloan Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.whitelist;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CvrSampleListUtil;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.*;

/**
 *
 * @author ochoaa
 */
public class ZeroVariantWhitelistTasklet implements Tasklet {

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    private final Logger log = Logger.getLogger(ZeroVariantWhitelistTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        if (cvrSampleListUtil.getNewUnreportedSamplesWithZeroVariants().isEmpty()) {
            log.info("No new samples with zero variants to update whitelist with - skipping udpate to: " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
        }
        else {
            log.info("Updating " + CVRUtilities.ZERO_VARIANT_WHITELIST_FILE + " with " + cvrSampleListUtil.getNewUnreportedSamplesWithZeroVariants().size() + " new samples");
            Set<String> compiledWhitelistedSamplesWithZeroVariants = new HashSet<>(cvrSampleListUtil.getWhitelistedSamplesWithZeroVariants());
            compiledWhitelistedSamplesWithZeroVariants.addAll(cvrSampleListUtil.getNewUnreportedSamplesWithZeroVariants());
            File whitelistedSamplesFile = new File(stagingDirectory, CVRUtilities.ZERO_VARIANT_WHITELIST_FILE);
            String contents = String.join("\n", compiledWhitelistedSamplesWithZeroVariants) + "\n";
            Files.write(whitelistedSamplesFile.toPath(), contents.getBytes());
        }
        return RepeatStatus.FINISHED;
    }
}
