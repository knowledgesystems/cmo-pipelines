/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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

    private static final char EXPORT_MODE = 'e';
    private static final char IMPORT_MODE = 'i';
    private static final char CHECK_MODE = 'c';
    private static final String ALL_VALID_MODES = "eic";

    private static Options getOptions(String[] args)
    {
        Options options = new Options();
        options.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("p", "redcap-project", true, "Redcap Project stable ID")
            .addOption("d", "directory", true, "Output directory")
            .addOption("m", "merge-datasources", false, "Flag for merging datasources for given stable ID")
            .addOption("i", "import-mode", false, "Import from directory to redcap-project (use one of { -i, -e, -c })")
            .addOption("e", "export-mode", false, "Export redcap-project to directory (use one of -i, -e, -c)")
            .addOption("c", "check-mode", false, "Check if redcap-project is present (use one of { -i, -e, -c })");
        return options;
    }

    private static void help(Options options, int exitStatus)
    {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("RedcapPipeline", options);
        System.exit(exitStatus);
    }

    private static void launchJob(String[] args, char executionMode, String project, String directory, boolean mergeClinicalDataSources) throws Exception
    {
        SpringApplication app = new SpringApplication(RedcapPipeline.class);
        ConfigurableApplicationContext ctx = app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("redcap_project", project)
                .addString("directory", directory)
                .addString("mergeClinicalDataSources", String.valueOf(mergeClinicalDataSources))
                .toJobParameters();
        Job redcapJob = null;
        if (executionMode == EXPORT_MODE) {
            redcapJob = ctx.getBean(BatchConfiguration.REDCAP_EXPORT_JOB, Job.class);
        }
        if (redcapJob != null) {
            JobExecution jobExecution = jobLauncher.run(redcapJob, jobParameters);
        }
    }

    public static char parseModeFromOptions(CommandLine commandLine) {
        PrintWriter errOut = new PrintWriter(System.err, true);
        boolean optionsAreValid = true;
        char mode = ' ';
        if (!commandLine.hasOption("redcap-project")) {
            errOut.println("error: -p (--redcap-project) argument must be provided");
            optionsAreValid = false;
        }
        if (commandLine.hasOption("import-mode")) {
            mode = IMPORT_MODE;
            if (commandLine.hasOption("export-mode") || commandLine.hasOption("check-mode")) {
                errOut.println("error: multiple modes selected. Use only one from { -i, -e, -c }");
                optionsAreValid = false;
            }
            if (!commandLine.hasOption("directory")) {
                errOut.println("error: import-mode requires a -d (--directory) argument to be provided");
                optionsAreValid = false;
            }
        } else if (commandLine.hasOption("export-mode")) {
            mode = EXPORT_MODE;
            if (commandLine.hasOption("import-mode") || commandLine.hasOption("check-mode")) {
                errOut.println("error: multiple modes selected. Use only one from { -i, -e, -c }");
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
        String project = commandLine.getOptionValue("redcap-project");
        String directory = commandLine.getOptionValue("directory");
        boolean mergeClinicalDataSources = commandLine.hasOption("merge-datasources");
        launchJob(args, executionMode, project, directory, mergeClinicalDataSources);
    }
}
