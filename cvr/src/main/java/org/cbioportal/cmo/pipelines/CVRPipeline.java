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

package org.cbioportal.cmo.pipelines;

import org.cbioportal.cmo.pipelines.cvr.BatchConfiguration;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.SessionConfiguration;
import org.cbioportal.cmo.pipelines.common.util.EmailUtil;

import java.util.*;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
            .addOption("j", "json", false, "To read or not to read. This can be used alone or in combination with --gml")
            .addOption("g", "gml", false, "Run germline job")
            .addOption("s", "skipSeg", false, "Flag to skip fetching seg data")
            .addOption("i", "study_id", true, "Study identifier (i.e., mskimpact, mskraindance, mskarcher, mskimpact_heme)")
            .addOption("t", "test", false, "Flag for running pipeline in testing mode so that samples are not consumed")
            .addOption("c", "consume_samples", true, "Path to CVR json filename")
            .addOption("r", "max_samples_to_remove", true, "The max number of samples that can be removed from data")
            .addOption("f", "force_annotation", false, "Flag for forcing reannotation of samples")
            .addOption("b", "block_zero_variant_warnings", false, "Flag to turn off warnings for samples with no variants")
            .addOption("n", "name_of_clinical_file", true, "Clinical filename.  Default is data_clinical.txt");
        return options;
    }

    private static void help(Options options, int exitStatus) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("CVRPipeline", options);
        System.exit(exitStatus);
    }

    private static void launchCvrPipelineJob(String[] args, String directory, String studyId, Boolean json, Boolean gml,
            Boolean skipSeg, boolean testingMode, Integer maxNumSamplesToRemove, Boolean forceAnnotation, String clinicalFilename,
            Boolean stopZeroVariantWarnings) throws Exception {
        // log wether in testing mode or not
        if (testingMode) {
            log.warn("CvrPipelineJob running in TESTING MODE - samples will NOT be requeued.");
        }
        else {
            log.warn("CvrPipelineJob running in PRODUCTION MODE - samples will be requeued.");
        }
        SpringApplication app = new SpringApplication(CVRPipeline.class);
        ConfigurableApplicationContext ctx= app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);

        // Job description from the BatchConfiguration
        String jobName;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("stagingDirectory", directory)
                .addString("studyId", studyId)
                .addString("testingMode", String.valueOf(testingMode))
                .addString("maxNumSamplesToRemove", String.valueOf(maxNumSamplesToRemove))
                .addString("forceAnnotation", String.valueOf(forceAnnotation))
                .addString("clinicalFilename", clinicalFilename)
                .addString("stopZeroVariantWarnings", String.valueOf(stopZeroVariantWarnings))
                .addString("jsonMode", String.valueOf(json))
                .addString("gmlMode", String.valueOf(gml));
        if (json) {
            if (gml) {
                jobName = BatchConfiguration.GML_JSON_JOB;
            }
            else {
                jobName = BatchConfiguration.JSON_JOB;
            }
        }
        else if (gml) {
            // SessionID is gotten from a spring bean in the SessionConfiguration and passed through here as a param
            builder.addString("sessionId", ctx.getBean(SessionConfiguration.GML_SESSION, String.class));
            jobName = BatchConfiguration.GML_JOB;
        }
        else {
            // SessionID is gotten from a spring bean in the SessionConfiguration and passed through here as a param
            builder.addString("sessionId", ctx.getBean(SessionConfiguration.SESSION_ID, String.class))
                    .addString("skipSeg", String.valueOf(skipSeg));
            jobName = BatchConfiguration.CVR_JOB;
        }
        // configure job and run
        Job cvrJob = ctx.getBean(jobName, Job.class);
        JobParameters jobParameters = builder.toJobParameters();
        JobExecution jobExecution = jobLauncher.run(cvrJob, jobParameters);
        EmailUtil emailUtil = ctx.getBean(BatchConfiguration.EMAIL_UTIL, EmailUtil.class);
        checkExceptions(jobExecution, jobParameters, emailUtil);
        log.info("Shutting down CVR Pipeline");
    }

    private static void launchConsumeSamplesJob(String[] args, String jsonFilename, boolean testingMode, boolean gml) throws Exception {
        // log wether in testing mode or not
        if (testingMode) {
            log.warn("ConsumeSamplesJob running in TESTING MODE - samples will NOT be consumed.");
        }
        else {
            log.warn("ConsumeSamplesJob running in PRODUCTION MODE - samples will be consumed.");
        }

        SpringApplication app = new SpringApplication(CVRPipeline.class);
        ConfigurableApplicationContext ctx= app.run(args);
        JobLauncher jobLauncher = ctx.getBean(JobLauncher.class);
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("jsonFilename", jsonFilename)
                .addString("testingMode", String.valueOf(testingMode))
                .addString("gmlMode", String.valueOf(gml));
        if (jsonFilename.contains(CVRUtilities.CVR_FILE)) {
            builder.addString("sessionId", ctx.getBean(SessionConfiguration.SESSION_ID, String.class));
        }
        else {
            builder.addString("sessionId", ctx.getBean(SessionConfiguration.GML_SESSION, String.class));
        }
        JobParameters jobParameters = builder.toJobParameters();
        Job consumeJob = ctx.getBean(BatchConfiguration.CONSUME_SAMPLES_JOB, Job.class);
        EmailUtil emailUtil = ctx.getBean(BatchConfiguration.EMAIL_UTIL, EmailUtil.class);
        JobExecution jobExecution = jobLauncher.run(consumeJob, jobParameters);
        checkExceptions(jobExecution, jobParameters, emailUtil);
        log.info("Shutting down Consume Sample Job");
    }

    private static void checkExceptions(JobExecution jobExecution, JobParameters jobParameters, EmailUtil emailUtil) {
        List<Throwable> exceptions = jobExecution.getAllFailureExceptions();
        if (exceptions.size() > 0) {
            emailUtil.sendErrorEmail("CVR Pipeline", exceptions, jobParameters.toString());
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = CVRPipeline.getOptions(args);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        if (commandLine.hasOption("h") ||
                ((!commandLine.hasOption("d") || !commandLine.hasOption("i")) && !commandLine.hasOption("c"))) {
            help(options, 0);
        }
        if (commandLine.hasOption("c")) {
            for (Option option : commandLine.getOptions()) {
                if (!option.getOpt().equals("t") && !option.getOpt().equals("c") && !option.getOpt().equals("g")) {
                    String error_message = "The --consume_samples option is only compatible with the --test and --gml options. " +
                                    "You used an incompatible option (--" + option.getLongOpt() + "/-" + option.getOpt() + ")";
                    log.error(error_message);
                    help(options,1);
                }
            }
            launchConsumeSamplesJob(args, commandLine.getOptionValue("c"), commandLine.hasOption("t"), commandLine.hasOption("g"));
        }
        else {
            Integer maxNumSamplesToRemove = CVRUtilities.DEFAULT_MAX_NUM_SAMPLES_TO_REMOVE;
            try {
                if (commandLine.hasOption("r")) {
                    maxNumSamplesToRemove = Integer.valueOf(commandLine.getOptionValue("r"));
                }
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                throw new RuntimeException("Cannot parse argument as integer: " + commandLine.getOptionValue("r"));
            }
            String clinicalFilename = CVRUtilities.DEFAULT_CLINICAL_FILE;
            if (commandLine.hasOption("n")) {
                clinicalFilename = commandLine.getOptionValue("n");
            }
            launchCvrPipelineJob(args, commandLine.getOptionValue("d"), commandLine.getOptionValue("i"),
                commandLine.hasOption("j"), commandLine.hasOption("g"), commandLine.hasOption("s"),
                commandLine.hasOption("t"), maxNumSamplesToRemove, commandLine.hasOption("f"), clinicalFilename, commandLine.hasOption("b"));
        }
    }
}
