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
/**
 *
 * @author jake
 */
@SpringBootApplication
public class DarwinPipeline {

    private static Logger log = Logger.getLogger(DarwinPipeline.class);

    private static Options getOptions(String[] args){
        Options gnuOptions = new Options();
        gnuOptions.addOption("h", "help", false, "shows this help document and quits.")
        .addOption("d", "directory", true, "Output directory")
        .addOption("s", "study", true, "Cancer Study ID");
        return gnuOptions;
    }

    private static void help(Options gnuOptions, int exitStatus){
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Darwin Pipeline", gnuOptions);
        System.exit(exitStatus);
    }

    private static void launchJob(String[] args, String outputDirectory, String studyID) throws Exception{
        SpringApplication app = new SpringApplication(DarwinPipeline.class);

        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);

        Job job;
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("outputDirectory", outputDirectory)
                .addString("studyID", studyID)
                .toJobParameters();
        switch (studyID) {
            case "mskimpact":
                job = ctx.getBean(BatchConfiguration.MSKIMPACT_JOB, Job.class);
                break;
            case "skcm_mskcc_2015_chant":
                job = ctx.getBean(BatchConfiguration.SKCM_MSKCC_2015_CHANT_JOB, Job.class);
                break;
            default:
                job = ctx.getBean(BatchConfiguration.MSK_JOB, Job.class);
                break;
        }
        if (job!=null){
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        }
        else{
            log.fatal("Failed to start DarwinPipeline");
        }

        log.info("Shutting down DarwinPipeline");
        ctx.close();
    }

    public static void main(String[] args) throws Exception{
        Options gnuOptions = DarwinPipeline.getOptions(args);
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(gnuOptions, args);
        if (commandLine.hasOption("h")||
            !commandLine.hasOption("directory")||
            !commandLine.hasOption("study")){
            help(gnuOptions, 0);
        }

        launchJob(args, commandLine.getOptionValue("directory"), commandLine.getOptionValue("study"));
    }
}
