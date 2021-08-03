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

package org.mskcc.cmo.ks;

import org.mskcc.cmo.ks.gene.config.BatchConfiguration;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * @author ochoaa
 */

@SpringBootApplication
public class GeneDataPipeline {

    private final static Logger LOG = Logger.getLogger(GeneDataPipeline.class);

    private static void launchGeneDataJob(String[] args, String geneDataFileName, String geneLengthDataFileName, String notificationFileName) throws Exception {
        SpringApplication app = new SpringApplication(GeneDataPipeline.class);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("geneDataFileName", geneDataFileName)
                .addString("geneLengthDataFileName", geneLengthDataFileName)
                .addString("notificationFileName", notificationFileName)
                .toJobParameters();
        Job geneDataJob = ctx.getBean(BatchConfiguration.GENE_DATA_JOB, Job.class);
        JobExecution jobExecution = jobLauncher.run(geneDataJob, jobParameters);
        if (!jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            LOG.error("GeneDataPipeline job failed with exit status: " + jobExecution.getExitStatus());
            System.exit(1);
        }
    }

    private static Options getOptions(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("d", "gene-data-filename", true, "The gene data filename (Homo_sapiens.gene_info)")
            .addOption("l", "gene-length-filename", true, "The gene length data filename (GFF file)")
            .addOption("n", "notification-filename", true, "The notification filename to write results from update [optional]");
        return options;
    }

    private static void help(Options options, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("GeneDataPipeline", options);
        System.exit(exitStatus);
    }

    public static void main(String[] args) throws Exception {
        Options options = GeneDataPipeline.getOptions(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("h") || !commandLine.hasOption("d") || !commandLine.hasOption("l")) {
            help(options, 0);
        }
        launchGeneDataJob(args, commandLine.getOptionValue("d"), commandLine.getOptionValue("l"), commandLine.hasOption("n") ? commandLine.getOptionValue("n") : null);
    }
}
