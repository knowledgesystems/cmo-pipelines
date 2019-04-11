/*
 * Copyright (c) 2018-2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp;

import org.mskcc.cmo.ks.ddp.pipeline.BatchConfiguration;

import com.google.common.base.Strings;
import java.io.File;
import java.util.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
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
public class DDPPipeline {

    private static final Logger LOG = Logger.getLogger(DDPPipeline.class);

    private static Options getOptions(Map<String, Integer> ddpCohortMap, String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "Shows this help document and quits.")
                .addOption("f", "fetch_data", true, "List of comma delimited additional data to fetch [diagnosis,radiation,chemotherapy,surgery] (demographics is always included)") // TODO do not hard code this
                .addOption("o", "output_directory", true, "Output directory")
                .addOption("c", "cohort_name", true, "Cohort name [" + StringUtils.join(ddpCohortMap.keySet(), " | ") + "]")
                .addOption("s", "subset_file", true, "File containing patient ID's to subset by")
                .addOption("e", "excluded_patients_file", true, "File containg patient ID's to exclude")
                .addOption("t", "test", false, "Run pipeline in test mode");
        return options;
    }

    private static void help(Options options, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("DDP Pipeline", options);
        System.exit(exitStatus);
    }

    private static void launchJob(ConfigurableApplicationContext ctx, String[] args,
                String cohortName,
                String subsetFilename,
                String excludedPatientsFilename,
                String outputDirectory,
                Boolean testMode,
                Boolean includeDiagnosis,
                Boolean includeRadiation,
                Boolean includeChemotherapy,
                Boolean includeSurgery) throws Exception {
        // TO-DO: Set up job that generates file containing line-delimited list of patient IDs
        // by calling cohort endpoint with user-specified cohort name
        // NOTE:  the use-case of this is meant to generate list of patient IDs in DDP pediatric cohort
        // which we will use to subset MSK-IMPACT clinical/genomic data
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("cohortName", cohortName)
                .addString("subsetFilename", subsetFilename)
                .addString("excludedPatientsFilename", excludedPatientsFilename)
                .addString("outputDirectory", outputDirectory)
                .addString("testMode", String.valueOf(testMode))
                .addString("includeDiagnosis", String.valueOf(includeDiagnosis))
                .addString("includeRadiation", String.valueOf(includeRadiation))
                .addString("includeChemotherapy", String.valueOf(includeChemotherapy))
                .addString("includeSurgery", String.valueOf(includeSurgery))
                .toJobParameters();
        Job job = ctx.getBean(BatchConfiguration.DDP_COHORT_JOB, Job.class);
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);
        if (!jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            LOG.error("DDPPipeline job '" + BatchConfiguration.DDP_COHORT_JOB +
                    "' failed with exit status: " + jobExecution.getExitStatus());
            System.exit(1);
        }
    }

    /**
     * Helper functions to validate inputs.
     */
    private static Boolean isValidCohort(Map<String, Integer> ddpCohortMap, String cohortName) {
        return ddpCohortMap.containsKey(cohortName);
    }
    private static Boolean isValidFile(String filename) {
        File f = new File(filename);
        return (f.exists() && f.isFile());
    }
    private static Boolean isValidDirectory(String directory) {
        File d = new File(directory);
        return (d.exists() && d.isDirectory());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(DDPPipeline.class);
        ConfigurableApplicationContext ctx = app.run(args);
        Map<String, Integer> ddpCohortMap = ctx.getBean("ddpCohortMap", Map.class);

        Options options = DDPPipeline.getOptions(ddpCohortMap, args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("h") || !commandLine.hasOption("o") ||
                (!commandLine.hasOption("c") && !commandLine.hasOption("s"))) {
            help(options, 1);
        }
        // parse input arguments
        String[] fetchOptions = {};
        if (commandLine.hasOption("f")) {
            fetchOptions = commandLine.getOptionValue("f").split(",");
        }
        String cohortName = commandLine.hasOption("c") ? commandLine.getOptionValue("c") : "";
        String subsetFilename = commandLine.hasOption("s") ? commandLine.getOptionValue("s") : "";
        String excludedPatientsFilename = commandLine.hasOption("e") ? commandLine.getOptionValue("e") : "";
        String outputDirectory = commandLine.getOptionValue("o");
        boolean foundInvalidFetchOption = false;
        boolean includeDiagnosis = false;
        boolean includeRadiation = false;
        boolean includeChemotherapy = false;
        boolean includeSurgery = false;
        // note we will have at least one data set to fetch because this is a required argument
        for (String fetchOption : fetchOptions) {
            if ("diagnosis".equals(fetchOption)) {
                includeDiagnosis = true;
            } else if ("radiation".equals(fetchOption)) {
                includeRadiation = true;
            } else if ("chemotherapy".equals(fetchOption)) {
                includeChemotherapy = true;
            } else if ("surgery".equals(fetchOption)) {
                includeSurgery = true;
            } else {
                System.out.println("Fetch option '" + fetchOption + "' is unknown - please provide valid fetch option!");
                foundInvalidFetchOption = true;
            }
        }
        if (foundInvalidFetchOption) {
            help(options, 2);
        }
        if (!Strings.isNullOrEmpty(cohortName) && !isValidCohort(ddpCohortMap, cohortName)) {
            System.out.println("Cohort name provided is unknown - please provide valid cohort name!");
            help(options,2);
        }
        if (!Strings.isNullOrEmpty(subsetFilename) && !isValidFile(subsetFilename)) {
            System.out.println("No such file: " + subsetFilename);
            help(options,2);
        }
        if (!Strings.isNullOrEmpty(excludedPatientsFilename) && !isValidFile(excludedPatientsFilename)) {
            System.out.println("No such file: " + excludedPatientsFilename);
            help(options,2);
        }
        if (!isValidDirectory(outputDirectory)) {
            System.out.println("No such directory: " + outputDirectory);
            help(options,2);
        }
        launchJob(ctx, args, cohortName, subsetFilename, excludedPatientsFilename, outputDirectory, commandLine.hasOption("t"),
            includeDiagnosis, includeRadiation, includeChemotherapy, includeSurgery);
    }
}
