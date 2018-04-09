/*
 * Copyright (c) 2016 - 2018 Memorial Sloan-Kettering Cancer Center.
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

import java.io.PrintWriter;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.pipeline.BatchConfiguration;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.mskcc.cmo.ks.redcap.source.MetadataManager;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * @author heinsz
 */

@SpringBootApplication
public class RedcapPipeline {

    private static final char EXPORT_MODE = 'e';
    private static final char IMPORT_MODE = 'i';
    private static final char CHECK_MODE = 'c';
    private static final String ALL_VALID_MODES = "eic";

    private static final Logger log = Logger.getLogger(RedcapPipeline.class);

    private static Options getOptions(String[] args)
    {
        Options options = new Options();
        options.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("p", "redcap-project-title", true, "RedCap project title (required for import-mode)")
            .addOption("s", "stable-id", true, "Stable id for cancer study (required for export-mode)")
            .addOption("d", "directory", true, "Output directory (required for export-mode)")
            .addOption("f", "filename", true, "Input filename (required for input-mode)")
            .addOption("r", "raw-data", false, "Export data without manipulation (no merging of data sources or splitting of attribute types)")
            .addOption("i", "import-mode", false, "Import from directory to redcap-project (use one of { -i, -e, -c })")
            .addOption("e", "export-mode", false, "Export either redcap-project-title or stable-id to directory (use one of -i, -e, -c)")
            .addOption("c", "check-mode", false, "Check if either redcap-project-title or stable-id is present in RedCap (use one of { -i, -e, -c })");
        return options;
    }

    private static void help(Options options, int exitStatus)
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("RedcapPipeline", options);
        System.exit(exitStatus);
    }

    private static void checkIfProjectOrStableIdExistsAndExit(String[] args, CommandLine commandLine)
    {
        SpringApplication app = new SpringApplication(RedcapPipeline.class);
        app.setWebEnvironment(false);
        ConfigurableApplicationContext ctx = app.run(args);
        String projectTitle = commandLine.getOptionValue("redcap-project-title");
        String stableId = commandLine.getOptionValue("stable-id");
        ClinicalDataSource clinicalDataSource = ctx.getBean(ClinicalDataSource.class);
        String message = "project " + projectTitle + " does not exists in RedCap";
        int exitStatusCode = 1;
        if (projectTitle != null) {
            // checking if projectTitle exists in RedCap
            if (clinicalDataSource.projectExists(projectTitle)) {
                message = "project " + projectTitle + " exists in RedCap";
                exitStatusCode = 0;
            }
        } else {
            // checking if any project for stableId exists in RedCap
            message = "no project for stable-id " + stableId + " exists in RedCap";
            if (clinicalDataSource.projectsExistForStableId(stableId)) {
                message = "projects for stable-id " + stableId + " exist in RedCap";
                exitStatusCode = 0;
            }
        }
        log.info(message + " : exiting with status code " + Integer.toString(exitStatusCode));
        System.exit(exitStatusCode);
    }

    private static void launchJob(String[] args, char executionMode, CommandLine commandLine) throws Exception
    {
        SpringApplication app = new SpringApplication(RedcapPipeline.class);
        app.setWebEnvironment(false);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParametersBuilder builder = new JobParametersBuilder();
        Job redcapJob = null;
        if (executionMode == EXPORT_MODE) {
            String stableId = commandLine.getOptionValue("stable-id");
            String redcapProjectTitle = commandLine.getOptionValue("redcap-project-title");
            ClinicalDataSource clinicalDataSource = ctx.getBean(ClinicalDataSource.class);
            MetadataManager metadataManager = ctx.getBean(MetadataManager.class);
            if (commandLine.hasOption("stable-id")) {
                if (!clinicalDataSource.projectsExistForStableId(stableId)) {
                    log.error("no project for stable-id " + stableId + " exists in RedCap");
                    System.exit(1);
                }
                // checks if study-id has overrides in CDD - if not, use defaults
                if (!metadataManager.checkOverridesExist(stableId)) {
                    log.error("no metadata for stable-id " + stableId + " exists in CDD, exporing using default metadata values");
                } else {
                    metadataManager.setOverrideStudyId(stableId);
                }
                builder.addString("stableId", stableId);
            }
            if (commandLine.hasOption("redcap-project-title")) {
                if (!clinicalDataSource.projectExists(redcapProjectTitle)) {
                    log.error("no project with title " + redcapProjectTitle + " exists in RedCap");
                    System.exit(1);
                }
                builder.addString("redcapProjectTitle", redcapProjectTitle);
            }
            builder.addString("directory", commandLine.getOptionValue("directory"));
            builder.addString("rawData", String.valueOf(commandLine.hasOption("raw-data")));
            if (commandLine.hasOption("raw-data")) {
                redcapJob = ctx.getBean(BatchConfiguration.REDCAP_RAW_EXPORT_JOB, Job.class);
            } else {
                redcapJob = ctx.getBean(BatchConfiguration.REDCAP_EXPORT_JOB, Job.class);
            }
        } else if (executionMode == IMPORT_MODE) {
            redcapJob = ctx.getBean(BatchConfiguration.REDCAP_IMPORT_JOB, Job.class);
            builder.addString("filename", commandLine.getOptionValue("filename"))
                    .addString("redcapProjectTitle", commandLine.getOptionValue("redcap-project-title"));
        }
        if (redcapJob != null) {
            JobExecution jobExecution = jobLauncher.run(redcapJob, builder.toJobParameters());
            if (!jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
                log.error("RedcapPipeline job failed with exit status: " + jobExecution.getExitStatus());
                System.exit(1);
            }
        }        
    }

    public static char parseModeFromOptions(CommandLine commandLine)
    {
        PrintWriter errOut = new PrintWriter(System.err, true);
        boolean optionsAreValid = true;
        char mode = ' ';
        if (commandLine.hasOption("import-mode")) {
            mode = IMPORT_MODE;
            if (commandLine.hasOption("export-mode") || commandLine.hasOption("check-mode")) {
                errOut.println("error: multiple modes selected. Use only one from { -i, -e, -c }");
                optionsAreValid = false;
            }
            if (!commandLine.hasOption("redcap-project-title")) {
                errOut.println("error: -p (--redcap-project-title) argument must be provided");
                optionsAreValid = false;
            }
            if (!commandLine.hasOption("filename")) {
                errOut.println("error: import-mode requires a -f (--filename) argument to be provided");
                optionsAreValid = false;
            }
            if (commandLine.hasOption("raw-data")) {
                errOut.println("error: the --raw-data option can only be used with export-mode");
                optionsAreValid = false;
            }
        } else if (commandLine.hasOption("export-mode")) {
            mode = EXPORT_MODE;
            if (commandLine.hasOption("import-mode") || commandLine.hasOption("check-mode")) {
                errOut.println("error: multiple modes selected. Use only one from { -i, -e, -c }");
                optionsAreValid = false;
            }
            if (!commandLine.hasOption("redcap-project-title") && !commandLine.hasOption("stable-id")) {
                errOut.println("error: one of -p (--redcap-project-title) or -s (--stable-id) must be provided for export-mode");
                optionsAreValid = false;
            }
            if (commandLine.hasOption("redcap-project-title") && commandLine.hasOption("stable-id")) {
                errOut.println("error: only one of -p (--redcap-project-title) or -s (--stable-id) can be provided for export-mode");
                optionsAreValid = false;
            }
            if (!commandLine.hasOption("raw-data") && commandLine.hasOption("redcap-project-title")) {
                errOut.println("error: export of single project must use the --raw-data flag : standard export of a single project is not currently supported");
                optionsAreValid = false;
            }
            if (!commandLine.hasOption("directory")) {
                errOut.println("error: import-mode requires a -d (--directory) argument to be provided");
                optionsAreValid = false;
            }
        } else if (commandLine.hasOption("check-mode")) {
            mode = CHECK_MODE;
            if (commandLine.hasOption("import-mode") || commandLine.hasOption("export-mode")) {
                errOut.println("error: multiple modes selected. Use only one from { -i, -e, -c }");
                optionsAreValid = false;
            }
            if (!commandLine.hasOption("redcap-project-title") && !commandLine.hasOption("stable-id")) {
                errOut.println("error: one of -p (--redcap-project-title) or -s (--stable-id) must be provided for check-mode");
                optionsAreValid = false;
            }
            if (commandLine.hasOption("redcap-project-title") && commandLine.hasOption("stable-id")) {
                errOut.println("error: only one of -p (--redcap-project-title) or -s (--stable-id) can be provided for check-mode");
                optionsAreValid = false;
            }
            if (commandLine.hasOption("raw-data")) {
                errOut.println("error: the --raw-data option can only be used with export-mode");
                optionsAreValid = false;
            }
        } else {
            errOut.println("error: no mode selected. Use only one from { -i, -e, -c }");
            optionsAreValid = false;
        }
        if (!optionsAreValid) {
            return ' '; //invalid options specified
        }
        return mode;
    }

    public static void main(String[] args) throws Exception
    {
        Options options = RedcapPipeline.getOptions(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("help")) {
            help(options, 0);
        }
        char executionMode = parseModeFromOptions(commandLine);
        if (ALL_VALID_MODES.indexOf(executionMode) == -1) {
            help(options, 1);
        }
        if (executionMode == CHECK_MODE) {
            checkIfProjectOrStableIdExistsAndExit(args, commandLine);
        }
        launchJob(args, executionMode, commandLine);
    }
}
