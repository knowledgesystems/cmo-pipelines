/*
 * Copyright (c) 2016, 2017, 2023 Memorial Sloan Kettering Cancer Center.
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

package org.mskcc.cmo.ks.darwin.pipeline.mskimpactdemographics;

import java.util.*;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactCompositeDemographics;
import org.mskcc.cmo.ks.darwin.pipeline.model.MskimpactPatientDemographics;
import org.mskcc.cmo.ks.darwin.pipeline.util.DarwinUtils;
import org.mskcc.cmo.ks.darwin.pipeline.util.VitalStatusUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jake
 */
public class MskimpactPatientDemographicsProcessor implements ItemProcessor<MskimpactPatientDemographics, MskimpactCompositeDemographics> {

    @Autowired
    private DarwinUtils darwinUtils;

    @Override
    public MskimpactCompositeDemographics process(final MskimpactPatientDemographics darwinPatientDemographics) throws Exception{
        // format results for demographics, age, and survival status
        List<String> patientDemographicsResult = new ArrayList<>();
        for(String field : MskimpactPatientDemographics.getPatientDemographicsFieldNames()){
            Object value = darwinPatientDemographics.getClass().getMethod("get"+field).invoke(darwinPatientDemographics);
            patientDemographicsResult.add(value != null ? darwinUtils.convertWhitespace(value.toString()) : "NA");
        }
        List<String> patientAgeResult = new ArrayList<>();
        for(String field : MskimpactPatientDemographics.getAgeFieldNames()){
            Object value = darwinPatientDemographics.getClass().getMethod("get"+field).invoke(darwinPatientDemographics);
            patientAgeResult.add(value != null ? darwinUtils.convertWhitespace(value.toString()) : "NA");
        }

        List<String> patientVitalStatusResult = VitalStatusUtils.getVitalStatusResult(darwinUtils, darwinPatientDemographics);

        MskimpactCompositeDemographics compositeRecord = new MskimpactCompositeDemographics();
        compositeRecord.setDemographicsResult(String.join("\t", patientDemographicsResult));
        compositeRecord.setAgeResult(String.join("\t", patientAgeResult));
        compositeRecord.setVitalStatusResult(String.join("\t", patientVitalStatusResult));
        return compositeRecord;
    }
}
