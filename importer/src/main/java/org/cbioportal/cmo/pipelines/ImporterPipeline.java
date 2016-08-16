/*
 * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
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
 * This file is part of cBioPortal.
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

package org.cbioportal.cmo.pipelines;

import org.cbioportal.cmo.pipelines.importer.config.BatchConfiguration;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

/**
 *
 * @author ochoaa
 */
@SpringBootApplication
public class ImporterPipeline {

    private static final Log LOG = LogFactory.getLog(ImporterPipeline.class);   
    
    private static Options getOptions(String[] args) {
        Options gnuOptions = new Options();
        gnuOptions.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("s", "staging", true, "Staging directory");
        return gnuOptions;
    }

    private static void help(Options gnuOptions, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("ImporterPipeline", gnuOptions);
        System.exit(exitStatus);
    }

    private static void launchJob(String[] args, String stagingDirectory) throws Exception {

        if (!stagingDirectory.endsWith(File.separator)) stagingDirectory += File.separator;

        SpringApplication app = new SpringApplication(ImporterPipeline.class);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        
        Job batchImporterJob = ctx.getBean(BatchConfiguration.BATCH_IMPORTER_JOB, Job.class);
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("stagingDirectory", stagingDirectory)
                .addDate("date", new Date())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncher.run(batchImporterJob, jobParameters);
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Import failed on cancer study: " + jobExecution.getExecutionContext().get("cancerStudyId"));
            }
        }
        else if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            if (LOG.isErrorEnabled()) {
                LOG.info("Import complete for cancer study: " + jobExecution.getExecutionContext().get("cancerStudyId"));
            }
        }
          
        System.out.println("Shutting down ImporterPipeline.");
        ctx.close();
    }

    public static void main(String[] args) throws Exception {
        Options gnuOptions = ImporterPipeline.getOptions(args);
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(gnuOptions, args);
        if (commandLine.hasOption("h") ||
            !commandLine.hasOption("s")) {
            help(gnuOptions, 0);
        }
        launchJob(args, commandLine.getOptionValue("s"));

    }
    
}
