/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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
package org.mskcc.cmo.ks.darwin;

import org.mskcc.cmo.ks.darwin.pipeline.BatchConfiguration;
import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.apache.log4j.Logger;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
/**
 *
 * @author jake
 */
@SpringBootApplication
public class DarwinPipeline {

    private static Logger log = Logger.getLogger(DarwinPipeline.class);

    private static Options getOptions(String[] args){
        Options options = new Options();
        options.addOption("h", "help", false, "shows this help document and quits.")
                .addOption("d", "directory", true, "Output directory")
                .addOption("s", "study", true, "Cancer Study ID")
                .addOption("r", "current_demographics_rec_count", false, "Count of records in current demographics file. Used to sanity check num of records in latest demographics data fetch. Required only when not running the CAISIS only mode.")
                .addOption("c", "caisis_mode", false, "Flag to run the MSK CAISIS job only");
        return options;
    }

    private static void help(Options options, int exitStatus){
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Darwin Pipeline", options);
        System.exit(exitStatus);
    }

    private static void launchJob(String[] args, String outputDirectory, String studyID, String currentDemographicsRecCount) throws Exception{
        SpringApplication app = new SpringApplication(DarwinPipeline.class);
        app.setWebEnvironment(false);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);

        String jobName;
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("outputDirectory", outputDirectory)
                .addString("studyID", studyID)
                .addString("currentDemographicsRecCount", currentDemographicsRecCount)
                .toJobParameters();
        switch (studyID) {
            case "mskimpact":
                jobName = BatchConfiguration.MSKIMPACT_JOB;
                break;
            case "skcm_mskcc_2015_chant":
                jobName = BatchConfiguration.SKCM_MSKCC_2015_CHANT_JOB;
                break;
            default:
                jobName = BatchConfiguration.MSK_JOB;
                break;
        }
        Job job = ctx.getBean(jobName, Job.class);
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        if (!jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            log.error("DarwinPipeline Job '" + jobName + "' exited with status: " + jobExecution.getExitStatus());
            System.exit(2);
        }
    }

    private static void launchCaisisJob(String[] args, String outputDirectory, String studyID) throws Exception {
        SpringApplication app = new SpringApplication(DarwinPipeline.class);
        app.setWebEnvironment(false);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("outputDirectory", outputDirectory)
                .addString("studyID", studyID)
                .toJobParameters();
        Job job = ctx.getBean(BatchConfiguration.MSK_CAISIS_JOB, Job.class);
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        if (!jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            log.error("DarwinPipeline Job '" + BatchConfiguration.MSK_CAISIS_JOB + "' exited with status: " + jobExecution.getExitStatus());
            System.exit(2);
        }
    }

    public static void main(String[] args) throws Exception{
        Options options = DarwinPipeline.getOptions(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("h")||
            !commandLine.hasOption("directory")||
            !commandLine.hasOption("study") ||
            (!commandLine.hasOption("current_demographics_rec_count") && !commandLine.hasOption("caisis_mode"))){
            help(options, 0);
        }
        if (commandLine.hasOption("c")) {
            launchCaisisJob(args, commandLine.getOptionValue("d"), commandLine.getOptionValue("s"));
        } else {
            launchJob(args, commandLine.getOptionValue("d"), commandLine.getOptionValue("s"), commandLine.getOptionValue("r"));
        }
    }
}
