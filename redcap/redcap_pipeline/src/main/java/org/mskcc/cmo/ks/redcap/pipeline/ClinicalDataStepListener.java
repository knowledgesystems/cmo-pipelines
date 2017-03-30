/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cmo.ks.redcap.pipeline;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
/**
 *
 * @author heinsz
 */
public class ClinicalDataStepListener implements StepExecutionListener {
    private final Logger log = Logger.getLogger(ClinicalDataStepListener.class);
    @Autowired
    public ClinicalDataSource clinicalDataSource;    
    @Override
    public void beforeStep(StepExecution se) {
        log.info("Starting a clinical data step");
    }

    @Override
    public ExitStatus afterStep(StepExecution se) {
        log.info("Checking if more data to process...");
        if (clinicalDataSource.hasMoreClinicalData()) {
            return new ExitStatus("CLINICAL");
        }
        if (clinicalDataSource.hasMoreTimelineData()) {
            return new ExitStatus("TIMELINE");
        }
        return ExitStatus.COMPLETED;
    }
}
