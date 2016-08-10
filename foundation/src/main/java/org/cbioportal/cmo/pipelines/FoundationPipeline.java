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

package org.cbioportal.cmo.pipelines;

import org.cbioportal.cmo.pipelines.foundation.BatchConfiguration;

import java.io.*;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

/**
 * Pipeline for transforming Foundation XML data to staging files.
 * 
 * @author Prithi Chakrapani, ochoaa
 */
@SpringBootApplication
public class FoundationPipeline {
    
    private static final Log LOG = LogFactory.getLog(FoundationPipeline.class);
    
    private static Options getOptions(String[] args) {
        Options gnuOptions = new Options();
        gnuOptions.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("s", "source", true, "Foundation .XML source directory" )
            .addOption ("o", "output", true, "Output directory for writing staging files")
            .addOption ("c", "cancer_study_id", true, "Cancer study identifier for meta data");
         
        return gnuOptions;
    }   
    
    private static void help(Options gnuOptions, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("FoundationPipeline", gnuOptions);        
        System.exit(exitStatus);
    }  
            
    private static void launchJob(String[] args, String sourceDirectory, String outputDirectory, String cancerStudyId) throws Exception {
        // set up application context and job launcher
        SpringApplication app = new SpringApplication(FoundationPipeline.class);
        ConfigurableApplicationContext ctx= app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);

        // configure job parameters and launch job
        Job foundationJob = ctx.getBean(BatchConfiguration.FOUNDATION_JOB, Job.class);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("sourceDirectory", sourceDirectory)
                .addString("outputDirectory", outputDirectory)
                .addString ("cancerStudyId", cancerStudyId)
                .toJobParameters();  
        JobExecution jobExecution = jobLauncher.run(foundationJob, jobParameters);
        
        // close job after completion 
        LOG.info("Closing FoundationPipeline with status: " + jobExecution.getStatus());
        ctx.close();
    }      
    
    public static void main(String[] args) throws Exception {        
        Options gnuOptions = FoundationPipeline.getOptions(args);
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(gnuOptions, args);
        if (commandLine.hasOption("h") ||
            !commandLine.hasOption("source") ||
            !commandLine.hasOption("output") ||
            !commandLine.hasOption("cancer_study_id"))  {
            for (String a : args) {
                System.out.println(a);
            }
            help(gnuOptions, 0);        
        }
        
        // light pre-processing for source directory and output directory paths
        String sourceDirectory = commandLine.getOptionValue("source");
        String outputDirectory = commandLine.getOptionValue("output");
        if (!sourceDirectory.endsWith(File.separator)) sourceDirectory += File.separator;
        if (!outputDirectory.endsWith(File.separator)) outputDirectory += File.separator;

        launchJob(args, sourceDirectory, outputDirectory, commandLine.getOptionValue("cancer_study_id"));
    }
}
    

