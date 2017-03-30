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

package org.mskcc.cmo.ks;

import org.mskcc.cmo.ks.crdb.BatchConfiguration;

import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

/**
 * Pipeline for running the CRDB clinical data job.
 * 
 * @author ochoaa
 */

@SpringBootApplication
public class CRDBPipeline {

    private static Options getOptions(String[] args) {
        Options gnuOptions = new Options();
        gnuOptions.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("stage", "staging", true, "Staging directory");
        return gnuOptions;
    }

    private static void help(Options gnuOptions, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("CRDBPipeline", gnuOptions);
        System.exit(exitStatus);
    }

    private static void launchJob(String[] args, String stagingDirectory) throws Exception {
        SpringApplication app = new SpringApplication(CRDBPipeline.class);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);        

        Job crdbJob = ctx.getBean(BatchConfiguration.CRDB_IMPACT_JOB, Job.class);        
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("stagingDirectory", stagingDirectory)
                .toJobParameters();  
        JobExecution jobExecution = jobLauncher.run(crdbJob, jobParameters); 

        System.out.println("Shutting down CRDBPipeline.");
        ctx.close();        
    }
    
    public static void main(String[] args) throws Exception {
        Options gnuOptions = CRDBPipeline.getOptions(args);
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(gnuOptions, args);
        if (commandLine.hasOption("h") ||
            !commandLine.hasOption("stage")) {
            help(gnuOptions, 0);
        }
        launchJob(args, commandLine.getOptionValue("stage"));        
    }
}
