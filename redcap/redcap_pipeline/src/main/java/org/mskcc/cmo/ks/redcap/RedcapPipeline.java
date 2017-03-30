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
package org.mskcc.cmo.ks.redcap;

import org.mskcc.cmo.ks.redcap.pipeline.BatchConfiguration;

import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

/**
 *
 * @author heinsz
 */

@SpringBootApplication
public class RedcapPipeline {
    
    private static Options getOptions(String[] args)
    {
        Options gnuOptions = new Options();
        gnuOptions.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("p", "redcap_project", true, "Redcap Project stable ID")
            .addOption("d", "directory", true, "Output directory")
            .addOption("m", "merge-datasources", false, "Flag for merging datasources for given stable ID");

        return gnuOptions;
    }

    private static void help(Options gnuOptions, int exitStatus)
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("RedcapPipeline", gnuOptions);
        System.exit(exitStatus);
    }

    private static void launchJob(String[] args, String project, String directory, boolean mergeClinicalDataSources) throws Exception
    {
        SpringApplication app = new SpringApplication(RedcapPipeline.class);      
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("redcap_project", project)
                .addString("directory", directory)
                .addString("mergeClinicalDataSources", String.valueOf(mergeClinicalDataSources))
                .toJobParameters();
             
        Job redcapJob= ctx.getBean(BatchConfiguration.REDCAP_JOB, Job.class);
        JobExecution jobExecution = jobLauncher.run(redcapJob, jobParameters);    

    }
    
    public static void main(String[] args) throws Exception
    {        
        Options gnuOptions = RedcapPipeline.getOptions(args);
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(gnuOptions, args);
        if (commandLine.hasOption("h") ||
            !commandLine.hasOption("directory") ||
            !commandLine.hasOption("redcap_project")) {
            help(gnuOptions, 0);
        }
        launchJob(args, commandLine.getOptionValue("redcap_project"), commandLine.getOptionValue("directory"), commandLine.hasOption("m"));
    }    
}
