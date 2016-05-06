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

import org.cbioportal.cmo.pipelines.gdd.BatchConfiguration;

import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

/**
 * @author Benjamin Gross
 */
@SpringBootApplication
public class GDDPipeline
{

    private static Options getOptions(String[] args)
    {
        Options gnuOptions = new Options();
        gnuOptions.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("maf", "maf", true, "MAF file")
            .addOption("cna", "cna", true, "CNA file")
            .addOption("seg", "seg", true, "SEG file")
            .addOption("sv", "sv", true, "SV file")
            .addOption("cl", "cl", true, "CLINICAL file")
            .addOption("stage", "staging", true, "Staging filename");

        return gnuOptions;
    }

    private static void help(Options gnuOptions, int exitStatus)
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("GDDPipeline", gnuOptions);
        System.exit(exitStatus);
    }

    private static void launchJob(String[] args, String maf, String cna,
                                  String seg, String sv,
                                  String clinical, String stagingFile) throws Exception
    {
        SpringApplication app = new SpringApplication(GDDPipeline.class);
        ConfigurableApplicationContext ctx= app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);

        Job gddJob = ctx.getBean(BatchConfiguration.GDD_JOB, Job.class);
        JobParameters jobParameters = new JobParametersBuilder()
    		.addString("maf", maf)
            .addString("cna", cna)
            .addString("seg", seg)
            .addString("sv", sv)
            .addString("clinical", clinical)
            .addString("stagingFile", stagingFile)
    		.toJobParameters();  
        JobExecution jobExecution = jobLauncher.run(gddJob, jobParameters);
    }
    
    public static void main(String[] args) throws Exception
    {        
        Options gnuOptions = GDDPipeline.getOptions(args);
        CommandLineParser parser = new GnuParser();
        CommandLine commandLine = parser.parse(gnuOptions, args);
        if (commandLine.hasOption("h") ||
            !commandLine.hasOption("maf") ||
            !commandLine.hasOption("cna") ||
            !commandLine.hasOption("seg") ||
            !commandLine.hasOption("sv") ||
            !commandLine.hasOption("cl") ||
            !commandLine.hasOption("stage")) {
            help(gnuOptions, 0);
        }
        if (args.length < 1) {
            String maf = "/home/ochoaa/data/gdd/data_mutations_extended.txt";
            String cna = "/home/ochoaa/data/gdd/data_CNA.txt";
            String seg = "/home/ochoaa/data/gdd/data_seg.seg";
            String sv = "/home/ochoaa/data/gdd/data_SV.txt";
            String cl = "/home/ochoaa/data/gdd/data_clinical.txt";
            String stage = "/home/ochoaa/data/gdd/Results/data_gdd.txt";
            launchJob(args, maf,cna, seg, sv, cl, stage);
        }
        else {
            launchJob(args, commandLine.getOptionValue("maf"),
                  commandLine.getOptionValue("cna"), commandLine.getOptionValue("seg"),
                  commandLine.getOptionValue("sv"), commandLine.getOptionValue("cl"),
                  commandLine.getOptionValue("stage"));            
        }
    }
}
