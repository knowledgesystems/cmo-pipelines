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

package org.mskcc.cmo.ks.pipeline;

import org.mskcc.cmo.ks.pipeline.ddp.BatchConfiguration;
import org.apache.commons.cli.*;

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
public class DDPPipeline {

    private static Options getOptions(String[] args) {
        Options options = new Options();
        options.addOption("o", "output_directory", true, "Output directory")
                .addOption("c", "cohort_name", true, "Cohort name [mskimpact_ped]"); // TO-DO get mapping keys
        return options;
    }

    private static void help(Options options, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("CVRPipeline", options);
        System.exit(exitStatus);
    }

    private static void launchAuthorizedCohortsJob(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(DDPPipeline.class);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        Job job = ctx.getBean(BatchConfiguration.AUTH_COHORTS_JOB, Job.class);
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
    }

    private static void launchJob(String[] args, String cohortName, String outputDirectory) throws Exception {
        SpringApplication app = new SpringApplication(DDPPipeline.class);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("cohortName", cohortName)
                .addString("outputDirectory", outputDirectory)
                .toJobParameters();
        Job job = ctx.getBean(BatchConfiguration.PEDIATRIC_COHORT_JOB, Job.class);
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
    }

    public static void main(String[] args) throws Exception {
//        launchAuthorizedCohortsJob(args);
        Options options = DDPPipeline.getOptions(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("h") || !commandLine.hasOption("o") || !commandLine.hasOption("c")) {
            help(options, 1);
        }
        launchJob(args, commandLine.getOptionValue("c"), commandLine.getOptionValue("o"));
    }
}
