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

package org.mskcc.cmo.ks.ddp.source.internal;

import org.mskcc.cmo.ks.ddp.source.DDPDataSource;
import org.mskcc.cmo.ks.ddp.source.model.*;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public class DDPDataSourceImpl implements DDPDataSource {

    @Autowired
    DDPRepository ddpRepository;

    @Override
    public List<Cohort> getAuthorizedCohorts() throws Exception {
        return ddpRepository.getAuthorizedCohorts();
    }

    @Override
    public List<CohortPatient> getPatientRecordsByCohortId(Integer cohortId) throws Exception {
        return ddpRepository.getPatientsByCohort(cohortId);
    }

    public static void main(String[] args) throws Exception {}

    @Override
    public PatientDemographics getPatientDemographics(String patientId) throws Exception {
        return ddpRepository.getPatientDemographics(patientId);
    }

    @Override
    public List<PatientDiagnosis> getPatientDiagnoses(String patientId) throws Exception {
        return ddpRepository.getPatientDiagnoses(patientId);
    }

    @Override
    public PatientIdentifiers getPatientIdentifiers(String patientId) throws Exception {
        return ddpRepository.getPatientIdentifiers(patientId);
    }

    @Override
    public List<Radiation> getPatientRadiationProcedures(String patientId) {
        return ddpRepository.getPatientRadiationProcedures(patientId);
    }

    @Override
    public List<Chemotherapy> getPatientChemoProcedures(String patientId) {
        return ddpRepository.getPatientChemoProcedures(patientId);
    }

    @Override
    public List<Surgery> getPatientSurgicalProcedures(String patientId) {
        return ddpRepository.getPatientSurgicalProcedures(patientId);
    }
}
