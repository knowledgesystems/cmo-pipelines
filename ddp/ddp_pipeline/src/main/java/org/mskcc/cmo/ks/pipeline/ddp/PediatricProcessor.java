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

package org.mskcc.cmo.ks.pipeline.ddp;

import java.util.ArrayList;
import java.util.List;
import org.mskcc.cmo.ks.ddp.source.DDPDataSource;
import org.mskcc.cmo.ks.ddp.source.composite.CompositePatient;
import org.mskcc.cmo.ks.pipeline.ddp.model.ClinicalPatient;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author ochoaa
 */
public class PediatricProcessor implements ItemProcessor<CompositePatient, String> {
    @Autowired
    private DDPDataSource ddpDataSource;

    @Override
    public String process(CompositePatient compositePatient) throws Exception {
        compositePatient.setPatientDemographics(ddpDataSource.getPatientDemographics(compositePatient.getCohortPatientData().getMRN()));
        compositePatient.setPatientDiagnosis(ddpDataSource.getPatientDiagnoses(compositePatient.getCohortPatientData().getMRN()));

        ClinicalPatient patient = new ClinicalPatient(compositePatient);
        return constructRecord(patient);
    }

    // TO-DO: add method to get field names, invoke reflection on patient object to get record
    private String constructRecord(ClinicalPatient patient) {
        List<String> record = new ArrayList();
        record.add(patient.getPatientId());
        record.add(patient.getSex());
        record.add(patient.getAge());
        record.add(patient.getOsStatus());
        record.add(patient.getOsMonths());
        return StringUtils.join(record, "\t");
    }
}
