/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline;

import org.mskcc.cmo.ks.ddp.source.DDPDataSource;
import org.mskcc.cmo.ks.ddp.source.composite.CompositePatient;
import org.mskcc.cmo.ks.ddp.source.model.CohortPatient;
import org.mskcc.cmo.ks.ddp.source.model.PatientIdentifiers;
import org.mskcc.cmo.ks.ddp.pipeline.util.DDPPatientListUtil;

import com.google.common.base.Strings;
import java.util.*;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.*;

/**
 *
 * @author ochoaa
 */
public class PediatricReader implements ItemStreamReader<CompositePatient> {

    @Value("#{jobParameters[cohortName]}")
    private String cohortName;

    @Resource(name = "cohortMapping")
    private Map<String, Integer> cohortMapping;

    @Autowired
    private DDPDataSource ddpDataSource;

    @Autowired
    private DDPPatientListUtil ddpPatientListUtil;

    private List<CompositePatient> cohortPatients;

    private Logger LOG = Logger.getLogger(PediatricReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        Integer cohortId = cohortMapping.get(cohortName);
        if (cohortId == null) {
            throw new ItemStreamException("Cohort not known by name: " + cohortName);
        }
        List<CompositePatient> patients = new ArrayList();
        try {
            patients = getDmpPatientsByCohortId(cohortId);
        }
        catch (Exception e) {
            throw new ItemStreamException("Error fetching patients by cohort name: " + cohortName);
        }
        LOG.info("Fetched " + patients.size()+  " patients with dmp ids for cohort: " + cohortName);
        this.cohortPatients = patients;
    }

    private List<CompositePatient> getDmpPatientsByCohortId(Integer cohortId) throws Exception {
        List<CohortPatient> patients = ddpDataSource.getPatientsByCohort(cohortId);
        LOG.info("Fetched " + patients.size()+  " active patients for cohort: " + cohortName);

        List<CompositePatient> compositePatients = new ArrayList();
        for (CohortPatient patient : patients) {
            PatientIdentifiers pids;
            try {
                pids = ddpDataSource.getPatientIdentifiers(patient.getPID().toString());
            }
            catch (Exception e) {
                LOG.error("Failed to get dmp id for patient '" + patient.getPID() + "' -- skipping");
                ddpPatientListUtil.addPatientsMissingDMPId(patient.getPID());
                continue;
            }
            if (pids != null && !Strings.isNullOrEmpty(pids.getDmpPatientId())) {
                compositePatients.add(new CompositePatient(pids.getDmpPatientId(), pids.getDmpSampleIds(), patient));
            } else {
                LOG.error("Failed to get dmp id for patient '" + patient.getPID() + "' -- skipping");
                ddpPatientListUtil.addPatientsMissingDMPId(patient.getPID());
            }
        }
        return compositePatients;
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public CompositePatient read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!cohortPatients.isEmpty()) {
            return cohortPatients.remove(0);
        }
        return null;
    }
}
