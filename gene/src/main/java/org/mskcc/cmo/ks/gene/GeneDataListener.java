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

package org.mskcc.cmo.ks.gene;

import java.util.*;
import java.io.*;

import org.apache.commons.logging.*;
import org.mskcc.cmo.ks.gene.model.Gene;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author ochoaa
 */
public class GeneDataListener implements StepExecutionListener {

    @Value("${DATABASE_NAME}")
    private String DATABASE_NAME;

    private static final Log LOG = LogFactory.getLog(GeneDataListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {}

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String notificationFileName = (String) stepExecution.getJobParameters().getString("notificationFileName");

        int genesAdded = (int) stepExecution.getExecutionContext().getInt("genesAdded", 0);
        int genesUpdated = (int) stepExecution.getExecutionContext().getInt("genesUpdated", 0);
        int geneAliasesAdded = (int) stepExecution.getExecutionContext().getInt("geneAliasesAdded", 0);

        StringBuilder builder = new StringBuilder();
        if ((genesAdded + genesUpdated + geneAliasesAdded) == 0) {
            builder.append("Nothing to update in database: ")
                    .append(DATABASE_NAME);
        }
        else {
            builder.append("Gene update results for database: ")
                    .append(DATABASE_NAME)
                    .append("\n\tNew genes added to database: ")
                    .append(genesAdded)
                    .append("\n\tTotal genes updated in database: ")
                    .append(genesUpdated)
                    .append("\n\tNew aliases added to database: ")
                    .append(geneAliasesAdded);
        }
        builder.append("\n\n");
        LOG.info(builder.toString());

        // report ambiguous genes from database not found in NCBI human gene info file
        Map<Integer, Gene> ambiguousGenesToReport = (Map<Integer, Gene>) stepExecution.getExecutionContext().get("ambiguousGenesToReport");

        if (!ambiguousGenesToReport.isEmpty()) {
            LOG.info("Found " + ambiguousGenesToReport.size() + " ambiguous genes to report.");
            builder.append("Ambiguous genes report: ")
                    .append(ambiguousGenesToReport.size())
                    .append(" ambiguous genes found in ")
                    .append(DATABASE_NAME)
                    .append("\n\nAmbiguous genes were found in the database that no longer exist in the NCBI human gene info file. ")
                    .append("These genes may have been discontinued and replaced with new entrez gene IDs and/or hugo gene symbols. ")
                    .append("Please verify on NCBI (https://www.ncbi.nlm.nih.gov/gene/).\n\n");

            builder.append("Hugo_Gene_Symbol\tEntrez_Gene_Id\n");
            for (Integer entrezGeneId : ambiguousGenesToReport.keySet()) {
                builder.append(ambiguousGenesToReport.get(entrezGeneId).getHugoGeneSymbol())
                        .append("\t")
                        .append(entrezGeneId)
                        .append("\n");
            }
        }
        else {
            LOG.info("No ambiguous or discontinued genes to report.");
            builder.append("No ambiguous or likely discontinued genes to report.");
        }

        if (notificationFileName != null) {
            File notificationFile = new File(notificationFileName);
            try {
                org.apache.commons.io.FileUtils.writeStringToFile(notificationFile, builder.toString());
            } catch (IOException ex) {
                LOG.error("Error writing results to notification file!");
                throw new ItemStreamException(ex);
            }
        }

        return ExitStatus.COMPLETED;
    }

}
