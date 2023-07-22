/*
 * Copyright (c) 2017, 2018, 2019, 2020, 2021, 2022, 2023 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan Kettering Cancer
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

import java.time.Instant;
import java.util.*;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.EmailUtil;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.BatchConfiguration;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.SessionConfiguration;
import org.cbioportal.cmo.pipelines.cvr.SessionFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * @author heinsz
 */
@SpringBootApplication
public class CVRPipeline {

    private static Logger log = Logger.getLogger(CVRPipeline.class);

    private static Options getOptions(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "shows this help document and quits.")
            .addOption("d", "directory", true, "The staging directory")
            .addOption("p", "private_directory", true, "The JSON directory")
            .addOption("j", "json", false, "To read or not to read. This can be used alone or in combination with --gml")
            .addOption("g", "gml", false, "Run germline job")
            .addOption("s", "skipSeg", false, "Flag to skip fetching seg data")
            .addOption("i", "study_id", true, "Study identifier (i.e., mskimpact, mskraindance, mskarcher, mskimpact_heme)")
            .addOption("t", "test", false, "Flag for running pipeline in testing mode so that samples are not requeued or consumed")
            .addOption("c", "consume_samples", true, "Path to CVR json filename")
            .addOption("r", "max_samples_to_remove", true, "The max number of samples that can be removed from data")
            .addOption("m", "master_list_does_not_exclude_samples", false, "Flag to cause samples to be accepted/kept even when they are not on the master list")
            .addOption("f", "force_annotation", false, "Flag for forcing reannotation of samples")
            .addOption("b", "block_zero_variant_warnings", false, "Flag to turn off warnings for samples with no variants")
            .addOption("n", "name_of_clinical_file", true, "Clinical filename.  Default is data_clinical.txt")
            .addOption("z", "drop_dead_instant", true, "Timepoint when http requests should cease and the fetch should quit/fail. e.g. 'Tue Aug 15 10:13:53 EDT 2023' default value is in the distant future");
        return options;
    }

    private static void help(Options options, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("CVRPipeline", options);
        System.exit(exitStatus);
    }

    private static int runCvrPipelineJob(String[] args, String directory, String privateDirectory, String studyId, Boolean json, Boolean gml,
            Boolean skipSeg, boolean testingMode, String dropDeadInstantString, Integer maxNumSamplesToRemove, boolean masterListDoesNotExcludeSamples, Boolean forceAnnotation, String clinicalFilename,
            Boolean stopZeroVariantWarnings) throws Exception {
        // log wether in testing mode or not
        if (testingMode) {
            log.warn("CvrPipelineJob running in TESTING MODE - samples will NOT be requeued.");
        } else {
            log.warn("CvrPipelineJob running in PRODUCTION MODE - samples will be requeued.");
        }
        if (masterListDoesNotExcludeSamples) {
            log.warn("CvrPipelineJob using masterListDoesNotExcludeSamples - samples accepted/kept even when they are not on the master list.");
        }
        // create app and get context (needed for bean access)
        SpringApplication app = new SpringApplication(CVRPipeline.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        // configure job
        // Job description from the BatchConfiguration
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("stagingDirectory", directory)
                .addString("privateDirectory", privateDirectory)
                .addString("studyId", studyId)
                .addString("testingMode", String.valueOf(testingMode))
                .addString("dropDeadInstantString", dropDeadInstantString)
                .addString("maxNumSamplesToRemove", String.valueOf(maxNumSamplesToRemove))
                .addString("masterListDoesNotExcludeSamples", String.valueOf(masterListDoesNotExcludeSamples))
                .addString("forceAnnotation", String.valueOf(forceAnnotation))
                .addString("clinicalFilename", clinicalFilename)
                .addString("stopZeroVariantWarnings", String.valueOf(stopZeroVariantWarnings))
                .addString("jsonMode", String.valueOf(json))
                .addString("gmlMode", String.valueOf(gml));
        String jobName;
        if (json) {
            if (gml) {
                jobName = BatchConfiguration.GML_JSON_JOB;
            } else {
                jobName = BatchConfiguration.JSON_JOB;
            }
        } else if (gml) {
            SessionFactory sessionFactory = ctx.getBean(SessionFactory.class);
            String sessionIdValue = sessionFactory.createGmlSessionAndGetId(dropDeadInstantString);
            String gmlMasterListSessionIdValue = sessionFactory.createCvrSessionAndGetId(dropDeadInstantString);
            builder.addString("sessionId", sessionIdValue);
            builder.addString("gmlMasterListSessionId", gmlMasterListSessionIdValue);
            jobName = BatchConfiguration.GML_JOB;
        } else {
            SessionFactory sessionFactory = ctx.getBean(SessionFactory.class);
            String sessionIdValue = sessionFactory.createCvrSessionAndGetId(dropDeadInstantString);
            builder.addString("sessionId", sessionIdValue);
            builder.addString("skipSeg", String.valueOf(skipSeg));
            jobName = BatchConfiguration.CVR_JOB;
        }
        // run job
        JobParameters jobParameters = builder.toJobParameters();
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        Job cvrJob = ctx.getBean(jobName, Job.class);
        JobExecution jobExecution = jobLauncher.run(cvrJob, jobParameters);
        if (!testingMode) {
            EmailUtil emailUtil = ctx.getBean(BatchConfiguration.EMAIL_UTIL, EmailUtil.class);
            checkExceptions(jobExecution, jobParameters, emailUtil);
        }
        log.info("Shutting down CVR Pipeline");
        return SpringApplication.exit(ctx);
    }

    private static int runConsumeSamplesJob(String[] args, String jsonFilename, boolean testingMode, String dropDeadInstantString, boolean gml) throws Exception {
        // log wether in testing mode or not
        if (testingMode) {
            log.warn("ConsumeSamplesJob running in TESTING MODE - samples will NOT be consumed.");
        }
        else {
            log.warn("ConsumeSamplesJob running in PRODUCTION MODE - samples will be consumed.");
        }
        // create app and get context (needed for bean access)
        SpringApplication app = new SpringApplication(CVRPipeline.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext ctx = app.run(args);
        // configure job
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("jsonFilename", jsonFilename)
                .addString("testingMode", String.valueOf(testingMode))
                .addString("dropDeadInstantString", dropDeadInstantString)
                .addString("gmlMode", String.valueOf(gml));
        SessionFactory sessionFactory = ctx.getBean(SessionFactory.class);
        if (jsonFilename.contains(CVRUtilities.CVR_FILE)) {
            String sessionIdValue = sessionFactory.createCvrSessionAndGetId(dropDeadInstantString);
            builder.addString("sessionId", sessionIdValue);
        } else {
            String gmlMasterListSessionIdValue = sessionFactory.createGmlSessionAndGetId(dropDeadInstantString);
            builder.addString("sessionId", gmlMasterListSessionIdValue);
        }
        JobParameters jobParameters = builder.toJobParameters();
        Job consumeJob = ctx.getBean(BatchConfiguration.CONSUME_SAMPLES_JOB, Job.class);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobExecution jobExecution = jobLauncher.run(consumeJob, jobParameters);
        if (!testingMode) {
            EmailUtil emailUtil = ctx.getBean(BatchConfiguration.EMAIL_UTIL, EmailUtil.class);
            checkExceptions(jobExecution, jobParameters, emailUtil);
        }
        log.info("Shutting down Consume Sample Job");
        return SpringApplication.exit(ctx);
    }

    private static void checkExceptions(JobExecution jobExecution, JobParameters jobParameters, EmailUtil emailUtil) {
        List<Throwable> exceptions = jobExecution.getAllFailureExceptions();
        if (exceptions.size() > 0) {
            emailUtil.sendErrorEmail("CVR Pipeline", exceptions, jobParameters.toString());
            System.exit(1);
        }
    }

    // any command line argument validity issues are handled here (with exception or exit through help()
    public static void validateArguments(Options options, CommandLine commandLine) {
        if (commandLine.hasOption("h")) {
            help(options, 0);
        }
        if (commandLine.hasOption("c")) {
            for (Option option : commandLine.getOptions()) {
                if (!option.getOpt().equals("t") && !option.getOpt().equals("c") && !option.getOpt().equals("g") && !option.getOpt().equals("z")) {
                    String error_message = String.format("The --consume_samples option is only compatible with the --test and --gml and --drop_dead_instant options. You used an incompatible option (--%s/-$s)", option.getLongOpt(), option.getOpt());
                    log.error(error_message);
                    help(options, 1);
                }
            }
        } else {
            // unless we are doing a consume job (which only requires a json filename) check for required arguments
            if (!commandLine.hasOption("d") || !commandLine.hasOption("p") || !commandLine.hasOption("i")) {
                help(options, 0);
            }
            if (commandLine.hasOption("r")) {
                try {
                    Integer.valueOf(commandLine.getOptionValue("r"));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Cannot parse argument as integer: " + commandLine.getOptionValue("r"));
                }
            }
        }
        if (commandLine.hasOption("z")) {
            String dateString = commandLine.getOptionValue("z");
            String dropDeadInstantString = InstantStringUtil.convertToIsoInstantFormat(dateString);
            if (dropDeadInstantString == null) {
                throw new RuntimeException("Cannot parse argument as valid date string for --drop_dead_instant: " + commandLine.getOptionValue("z"));
            }
            Instant instant = InstantStringUtil.createInstant(dropDeadInstantString);
            if (instant == null) {
                throw new RuntimeException("Instant could not be creatd from ISO_INSTANT formatted date time string : " + dropDeadInstantString);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = CVRPipeline.getOptions(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        validateArguments(options, commandLine);
        String dropDeadInstantString = "";
        if (commandLine.hasOption("z")) {
            String dateString = commandLine.getOptionValue("z");
            dropDeadInstantString = InstantStringUtil.convertToIsoInstantFormat(dateString);
        }
        int return_status = 0;
        if (commandLine.hasOption("c")) {
            return_status = runConsumeSamplesJob(args, commandLine.getOptionValue("c"), commandLine.hasOption("t"), dropDeadInstantString, commandLine.hasOption("g"));
        } else {
            Integer maxNumSamplesToRemove = CVRUtilities.DEFAULT_MAX_NUM_SAMPLES_TO_REMOVE;
            if (commandLine.hasOption("r")) {
                maxNumSamplesToRemove = Integer.valueOf(commandLine.getOptionValue("r"));
            }
            String clinicalFilename = CVRUtilities.DEFAULT_CLINICAL_FILE;
            if (commandLine.hasOption("n")) {
                clinicalFilename = commandLine.getOptionValue("n");
            }
            return_status = runCvrPipelineJob(args, commandLine.getOptionValue("d"), commandLine.getOptionValue("p"), commandLine.getOptionValue("i"),
                commandLine.hasOption("j"), commandLine.hasOption("g"), commandLine.hasOption("s"),
                commandLine.hasOption("t"), dropDeadInstantString, maxNumSamplesToRemove, commandLine.hasOption("m"), commandLine.hasOption("f"), clinicalFilename, commandLine.hasOption("b"));
        }
        System.exit(return_status);
    }
}
