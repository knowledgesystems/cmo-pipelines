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

package org.mskcc.cmo.ks.ddp.source.model;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;

/**
 *
 * @author ochoaa
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"de_id",
"dmp_patient_id",
"dmp_sample_ids",
"mrn"
})
public class PatientIdentifiers implements Serializable {
    @JsonProperty("de_id")
    private String deId;
    @JsonProperty("dmp_patient_id")
    private String dmpPatientId;
    @JsonProperty("dmp_sample_ids")
    private List<String> dmpSampleIds;
    @JsonProperty("mrn")
    private String mrn;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("de_id")
    public String getDeId() {
        return deId;
    }

    @JsonProperty("de_id")
    public void setDeId(String deId) {
        this.deId = deId;
    }

    @JsonProperty("dmp_patient_id")
    public String getDmpPatientId() {
        return dmpPatientId;
    }

    @JsonProperty("dmp_patient_id")
    public void setDmpPatientId(String dmpPatientId) {
        this.dmpPatientId = dmpPatientId;
    }

    @JsonProperty("dmp_sample_ids")
    public List<String> getDmpSampleIds() {
        return dmpSampleIds;
    }

    @JsonProperty("dmp_sample_ids")
    public void setDmpSampleIds(List<String> dmpSampleIds) {
        this.dmpSampleIds = dmpSampleIds;
    }

    @JsonProperty("mrn")
    public String getMrn() {
        return mrn;
    }

    @JsonProperty("mrn")
    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
