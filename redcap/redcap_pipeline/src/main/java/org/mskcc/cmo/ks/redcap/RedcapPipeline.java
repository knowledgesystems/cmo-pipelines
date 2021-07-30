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
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.mskcc.cmo.ks.redcap.pipeline.BatchConfiguration;
import org.mskcc.cmo.ks.redcap.pipeline.util.JobParameterUtils;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.mskcc.cmo.ks.redcap.source.MetadataManager;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * @author heinsz
 */

@SpringBootApplication
public class RedcapPipeline {

    private static ClinicalDataSource clinicalDataSource;
    private static MetadataManager metadataManager;
    private static JobParameterUtils jobParameterUtils;

    private static Set<String> maskProjectArgument = new HashSet<String>();
    private static Set<String> projectSetForStableId = new HashSet<String>();

    private static final char EXPORT_MODE = 'e';
    private static final char IMPORT_MODE = 'i';
    private static final char CHECK_MODE = 'c';
    private static final char UNDETERMINED_MODE = ' ';
    private static final String OPTION_HELP= "help";
    private static final String OPTION_REDCAP_PROJECT_TITLE = "redcap-project-title";
    private static final String OPTION_STABLE_ID = "stable-id";
    private static final String OPTION_DIRECTORY = "directory";
    private static final String OPTION_FILENAME = "filename";
    private static final String OPTION_RAW_DATA = "raw-data";
    private static final String OPTION_KEEP_EXISTING_PROJECT_DATA = "keep-existing-project-data";
    private static final String OPTION_MASK_REDCAP_PROJECTS = "mask-redcap-projects";
    private static final String OPTION_IMPORT_MODE = "import-mode";
    private static final String OPTION_EXPORT_MODE = "export-mode";
    private static final String OPTION_CHECK_MODE = "check-mode";

    private static final Logger log = Logger.getLogger(RedcapPipeline.class);

    private static Options getOptions(String[] args) {
        Options options = new Options();
        options.addOption("h", OPTION_HELP, false, "shows this help document and quits.")
            .addOption("p", OPTION_REDCAP_PROJECT_TITLE, true, "RedCap project title (required for import-mode)")
            .addOption("s", OPTION_STABLE_ID, true, "Stable id for cancer study (required for export-mode)")
            .addOption("d", OPTION_DIRECTORY, true, "Output directory (required for export-mode)")
            .addOption("f", OPTION_FILENAME, true, "Input filename (required for input-mode)")
            .addOption("r", OPTION_RAW_DATA, false, "Export data without manipulation (no merging of data sources or splitting of attribute types)")
            .addOption("k", OPTION_KEEP_EXISTING_PROJECT_DATA, false, "During import, disable the deletion of existing project records which are not present in the imported file (can not be used when the project record name field is an autonumbered record_id)")
            .addOption("m", OPTION_MASK_REDCAP_PROJECTS, true, "Export (or check) of data will not include the data for these redcap project titles. (can not be used with --" + OPTION_REDCAP_PROJECT_TITLE + ")")
            .addOption("i", OPTION_IMPORT_MODE, false, "Import from file to redcap-project (use one of { -i, -e, -c })")
            .addOption("e", OPTION_EXPORT_MODE, false, "Export either --" + OPTION_REDCAP_PROJECT_TITLE + " or --" + OPTION_STABLE_ID + " to directory (use one of -i, -e, -c)")
            .addOption("c", OPTION_CHECK_MODE, false, "Check if either --" + OPTION_REDCAP_PROJECT_TITLE + " or --" + OPTION_STABLE_ID + " is present in RedCap (use one of { -i, -e, -c })");
        return options;
    }

    private static void help(Options options, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("RedcapPipeline", options);
        System.exit(exitStatus);
    }

    private static List<Boolean> checkIfProjectsExist(List<String> projectNames) {
        ArrayList<Boolean> projectExists = new ArrayList<>();
        for (String projectName : projectNames) {
            if (projectName == null) {
                projectExists.add(false);
            } else {
                projectExists.add(clinicalDataSource.projectExists(projectName));
            }
        }
        return projectExists;
    }

    private static Boolean checkIfProjectsExistForStableId() {
        Set<String> unmaskedProjects = new HashSet<String>();
        for (String projectName : projectSetForStableId) {
            if (!maskProjectArgument.contains(projectName)) {
                unmaskedProjects.add(projectName);
            }
        }
        return unmaskedProjects.size() > 0;
    }

    private static void checkIfProjectOrStableIdExistsAndExit(CommandLine commandLine) {
        String projectTitle = commandLine.getOptionValue(OPTION_REDCAP_PROJECT_TITLE);
        String stableId = commandLine.getOptionValue(OPTION_STABLE_ID);
        String message = "project " + projectTitle + " does not exists in RedCap";
        int exitStatusCode = 1;
        if (projectTitle != null) {
            // checking if projectTitle exists in RedCap
            List<Boolean> projectExists = checkIfProjectsExist(Collections.singletonList(projectTitle));
            if (projectExists.get(0)) {
                message = "project " + projectTitle + " exists in RedCap";
                exitStatusCode = 0;
            }
        } else {
            // checking if any project for stableId exists in RedCap
            message = "no project for stable-id " + stableId + " exists in RedCap";
            // Both the passed in argument for masked project names and the actual redcap project list have been stored in static data members
            if (checkIfProjectsExistForStableId()) {
                message = "projects for stable-id " + stableId + " exist in RedCap";
                exitStatusCode = 0;
            }
        }
        log.info(message + " : exiting with status code " + Integer.toString(exitStatusCode));
        System.exit(exitStatusCode);
    }

    private static Set<String> getProjectTitlesForStableId(String stableId) {
        Set<String> projectTitleSet = new HashSet<>();
        ListIterator<String> clinicalProjectIterator = clinicalDataSource.getClinicalProjectTitleIterator(stableId);
        while (clinicalProjectIterator.hasNext()) {
            projectTitleSet.add(clinicalProjectIterator.next());
        }
        ListIterator<String> timelineProjectIterator = clinicalDataSource.getTimelineProjectTitleIterator(stableId);
        while (timelineProjectIterator.hasNext()) {
            projectTitleSet.add(timelineProjectIterator.next());
        }
        return projectTitleSet;
    }

    private static void launchJob(ConfigurableApplicationContext ctx, char executionMode, CommandLine commandLine) throws Exception {
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParametersBuilder builder = new JobParametersBuilder();
        Job redcapJob = null;
        if (executionMode == EXPORT_MODE) {
            String stableId = commandLine.getOptionValue(OPTION_STABLE_ID);
            String redcapProjectTitle = commandLine.getOptionValue(OPTION_REDCAP_PROJECT_TITLE);
            if (commandLine.hasOption(OPTION_STABLE_ID)) {
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
            if (commandLine.hasOption(OPTION_REDCAP_PROJECT_TITLE)) {
                if (!clinicalDataSource.projectExists(redcapProjectTitle)) {
                    log.error("no project with title " + redcapProjectTitle + " exists in RedCap");
                    System.exit(1);
                }
                builder.addString("redcapProjectTitle", redcapProjectTitle);
            }
            builder.addString(OPTION_DIRECTORY, commandLine.getOptionValue(OPTION_DIRECTORY));
            builder.addString("rawData", String.valueOf(commandLine.hasOption(OPTION_RAW_DATA)));
            List<String> listOfMaskedProjects = new ArrayList<>(maskProjectArgument);
            jobParameterUtils.setListOfMaskedProjects(listOfMaskedProjects);
            if (commandLine.hasOption(OPTION_RAW_DATA)) {
                redcapJob = ctx.getBean(BatchConfiguration.REDCAP_RAW_EXPORT_JOB, Job.class);
            } else {
                redcapJob = ctx.getBean(BatchConfiguration.REDCAP_EXPORT_JOB, Job.class);
            }
        } else if (executionMode == IMPORT_MODE) {
            redcapJob = ctx.getBean(BatchConfiguration.REDCAP_IMPORT_JOB, Job.class);
            builder.addString(OPTION_FILENAME, commandLine.getOptionValue(OPTION_FILENAME))
                    .addString("redcapProjectTitle", commandLine.getOptionValue(OPTION_REDCAP_PROJECT_TITLE))
                    .addString("keepExistingProjectData", String.valueOf(commandLine.hasOption(OPTION_KEEP_EXISTING_PROJECT_DATA)));
        }
        if (redcapJob != null) {
            JobExecution jobExecution = jobLauncher.run(redcapJob, builder.toJobParameters());
            if (!jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
                log.error("RedcapPipeline job failed with exit status: " + jobExecution.getExitStatus());
                System.exit(1);
            }
        }
    }

    private static char parseModeFromOptions(CommandLine commandLine) {
        PrintWriter errOut = new PrintWriter(System.err, true);
        char mode = determineModeFromOptions(commandLine, errOut);
        if (mode == UNDETERMINED_MODE) {
            return UNDETERMINED_MODE;
        }
        boolean optionsAreValid = true;
        if (anyOptionWasUsedIllegally(commandLine, mode, errOut)) {
            optionsAreValid = false;
        }
        if (anyRequiredOptionWasMissing(commandLine, mode, errOut)) {
            optionsAreValid = false;
        }
        if (!optionsAreValid) {
            return UNDETERMINED_MODE;
        }
        return mode;
    }

    private static char determineModeFromOptions(CommandLine commandLine, PrintWriter errOut) {
        char mode = UNDETERMINED_MODE;
        int numberOfSelectedModes = 0;
        if (commandLine.hasOption(OPTION_EXPORT_MODE)) {
            mode = EXPORT_MODE;
            numberOfSelectedModes += 1;
        }
        if (commandLine.hasOption(OPTION_IMPORT_MODE)) {
            mode = IMPORT_MODE;
            numberOfSelectedModes += 1;
        }
        if (commandLine.hasOption(OPTION_CHECK_MODE)) {
            mode = CHECK_MODE;
            numberOfSelectedModes += 1;
        }
        if (numberOfSelectedModes > 1) {
            errOut.println("error: multiple modes selected. Use only one from { -i, -e, -c }");
            return UNDETERMINED_MODE;
        }
        if (numberOfSelectedModes == 0) {
            errOut.println("error: no mode selected. Use only one from { -i, -e, -c }");
            return UNDETERMINED_MODE;
        }
        return mode;
    }

    private static boolean anyOptionWasUsedIllegally(CommandLine commandLine, char mode, PrintWriter errOut) {
        boolean detectedIllegalUse = false;
        if (commandLine.hasOption(OPTION_REDCAP_PROJECT_TITLE) && commandLine.hasOption(OPTION_STABLE_ID)) {
            errOut.println("error: only one of -p (--" + OPTION_REDCAP_PROJECT_TITLE + ") or -s (--" + OPTION_STABLE_ID + ") can be provided");
            detectedIllegalUse = true;
        }
        if (commandLine.hasOption(OPTION_MASK_REDCAP_PROJECTS) && commandLine.hasOption(OPTION_REDCAP_PROJECT_TITLE)) {
            errOut.println("error: the --" + OPTION_MASK_REDCAP_PROJECTS + " option can not be used with the --" + OPTION_REDCAP_PROJECT_TITLE + " option");
            detectedIllegalUse = true;
        }
        if (commandLine.hasOption(OPTION_STABLE_ID) && mode != EXPORT_MODE && mode != CHECK_MODE) {
            errOut.println("error: the --" + OPTION_STABLE_ID + " option can only be used with export-mode or check-mode");
            detectedIllegalUse = true;
        }
        if (commandLine.hasOption(OPTION_MASK_REDCAP_PROJECTS) && mode != EXPORT_MODE && mode != CHECK_MODE) {
            errOut.println("error: the --" + OPTION_MASK_REDCAP_PROJECTS + " option can only be used with export-mode or check-mode");
            detectedIllegalUse = true;
        }
        if (commandLine.hasOption(OPTION_DIRECTORY) && mode != EXPORT_MODE) {
            errOut.println("error: the --" + OPTION_DIRECTORY + " option can only be used with export-mode");
            detectedIllegalUse = true;
        }
        if (commandLine.hasOption(OPTION_FILENAME) && mode != IMPORT_MODE) {
            errOut.println("error: the --" + OPTION_FILENAME + " option can only be used with import-mode");
            detectedIllegalUse = true;
        }
        if (commandLine.hasOption(OPTION_RAW_DATA) && mode != EXPORT_MODE) {
            errOut.println("error: the --" + OPTION_RAW_DATA + " option can only be used with export-mode");
            detectedIllegalUse = true;
        }
        if (commandLine.hasOption(OPTION_KEEP_EXISTING_PROJECT_DATA) && mode != IMPORT_MODE) {
            errOut.println("error: the --" + OPTION_KEEP_EXISTING_PROJECT_DATA + " option can only be used with import-mode");
            detectedIllegalUse = true;
        }
        return detectedIllegalUse;
    }

    private static boolean anyRequiredOptionWasMissing(CommandLine commandLine, char mode, PrintWriter errOut) {
        boolean detectedIllegalUse = false;
        switch (mode) {
        case IMPORT_MODE:
            if (!commandLine.hasOption(OPTION_REDCAP_PROJECT_TITLE)) {
                errOut.println("error: import-mode requires a -p (--" + OPTION_REDCAP_PROJECT_TITLE + ") argument to be provided");
                detectedIllegalUse = true;
            }
            if (!commandLine.hasOption(OPTION_FILENAME)) {
                errOut.println("error: import-mode requires a -f (--" + OPTION_FILENAME + ") argument to be provided");
                detectedIllegalUse = true;
            }
            break;
        case EXPORT_MODE:
            if (!commandLine.hasOption(OPTION_REDCAP_PROJECT_TITLE) && !commandLine.hasOption(OPTION_STABLE_ID)) {
                errOut.println("error: export-mode requires one of -p (--" + OPTION_REDCAP_PROJECT_TITLE + ") or -s (--" + OPTION_STABLE_ID + ") to be provided");
                detectedIllegalUse = true;
            }
            if (commandLine.hasOption(OPTION_REDCAP_PROJECT_TITLE) && !commandLine.hasOption(OPTION_RAW_DATA)) {
                errOut.println("error: export-mode with the -p (--" + OPTION_REDCAP_PROJECT_TITLE + ") option requires the use of the -r (--" + OPTION_RAW_DATA + ") option as well");
                detectedIllegalUse = true;
            }
            if (!commandLine.hasOption(OPTION_DIRECTORY)) {
                errOut.println("error: export-mode requires a -d (--" + "OPTION_DIRECTORY" + ") argument to be provided");
                detectedIllegalUse = true;
            }
            break;
        case CHECK_MODE:
            if (!commandLine.hasOption(OPTION_REDCAP_PROJECT_TITLE) && !commandLine.hasOption(OPTION_STABLE_ID)) {
                errOut.println("error: check-mode requires one of -p (--" + OPTION_REDCAP_PROJECT_TITLE + ") or -s (--" + OPTION_STABLE_ID + ") to be provided");
                detectedIllegalUse = true;
            }
            break;
        }
        return detectedIllegalUse;
    }

    private static void exitIfMaskProjectsAreNotFound(CommandLine commandLine) {
        StringBuilder errorMessageBuilder = new StringBuilder();
        String maskProjectListString = commandLine.getOptionValue(OPTION_MASK_REDCAP_PROJECTS, "");
        String[] projectNameArgument = maskProjectListString.split(",");
        for (String projectName : projectNameArgument) {
            String trimmedProjectName = projectName.trim();
            if (maskProjectArgument.contains(trimmedProjectName)) {
                errorMessageBuilder.append("Duplicated project name given in argument to --" + OPTION_MASK_REDCAP_PROJECTS + " : '" + trimmedProjectName + "'\n");
                continue;
            }
            if (trimmedProjectName.length() > 0) {
                maskProjectArgument.add(trimmedProjectName);
            }
        }
        String stableId = commandLine.getOptionValue(OPTION_STABLE_ID);
        projectSetForStableId = getProjectTitlesForStableId(stableId);
        for (String projectName : maskProjectArgument) {
            if (!projectSetForStableId.contains(projectName)) {
                errorMessageBuilder.append("Non-existent project name '" + projectName + "' was given for option --" + OPTION_MASK_REDCAP_PROJECTS + "\n");
                continue;
            }
        }
        if (errorMessageBuilder.length() > 0) {
            log.error(errorMessageBuilder.toString());
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = RedcapPipeline.getOptions(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption(OPTION_HELP)) {
            help(options, 0);
        }
        char executionMode = parseModeFromOptions(commandLine);
        if (executionMode == UNDETERMINED_MODE) {
            help(options, 1);
        }
        SpringApplication app = new SpringApplication(RedcapPipeline.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        // get necessary beans from context
        clinicalDataSource = ctx.getBean(ClinicalDataSource.class);
        metadataManager = ctx.getBean(MetadataManager.class);
        jobParameterUtils = ctx.getBean(JobParameterUtils.class);
        exitIfMaskProjectsAreNotFound(commandLine);
        if (executionMode == CHECK_MODE) {
            checkIfProjectOrStableIdExistsAndExit(commandLine);
        }
        launchJob(ctx, executionMode, commandLine);
    }
}
