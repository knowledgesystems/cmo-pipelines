/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
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
package org.cbioportal.cmo.pipelines.gdd.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
"sample_id",
"cancer_type",
"classification",
"evidence"
})
public class GDDResult {

@JsonProperty("sample_id")
private String sampleId;
@JsonProperty("cancer_type")
private String cancerType;
@JsonProperty("classification")
private List<GDDClassification> classification = new ArrayList<GDDClassification>();
@JsonProperty("evidence")
private GDDEvidence evidence;
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

/**
* No args constructor for use in serialization
* 
*/
public GDDResult() {
}

/**
* 
* @param cancerType
* @param evidence
* @param classification
* @param sampleId
*/
public GDDResult(String sampleId, String cancerType, List<GDDClassification> classification, GDDEvidence evidence) {
this.sampleId = sampleId;
this.cancerType = cancerType;
this.classification = classification;
this.evidence = evidence;
}

/**
* 
* @return
* The sampleId
*/
@JsonProperty("sample_id")
public String getSampleId() {
return sampleId;
}

/**
* 
* @param sampleId
* The sample_id
*/
@JsonProperty("sample_id")
public void setSampleId(String sampleId) {
this.sampleId = sampleId;
}

public GDDResult withSampleId(String sampleId) {
this.sampleId = sampleId;
return this;
}

/**
* 
* @return
* The cancerType
*/
@JsonProperty("cancer_type")
public String getCancerType() {
return cancerType;
}

/**
* 
* @param cancerType
* The cancer_type
*/
@JsonProperty("cancer_type")
public void setCancerType(String cancerType) {
this.cancerType = cancerType;
}

public GDDResult withCancerType(String cancerType) {
this.cancerType = cancerType;
return this;
}

/**
* 
* @return
* The classification
*/
@JsonProperty("classification")
public List<GDDClassification> getClassification() {
return classification;
}

/**
* 
* @param classification
* The classification
*/
@JsonProperty("classification")
public void setClassification(List<GDDClassification> classification) {
this.classification = classification;
}

public GDDResult withClassification(List<GDDClassification> classification) {
this.classification = classification;
return this;
}

/**
* 
* @return
* The evidence
*/
@JsonProperty("evidence")
public GDDEvidence getEvidence() {
return evidence;
}

/**
* 
* @param evidence
* The evidence
*/
@JsonProperty("evidence")
public void setEvidence(GDDEvidence evidence) {
this.evidence = evidence;
}

public GDDResult withEvidence(GDDEvidence evidence) {
this.evidence = evidence;
return this;
}

@Override
public String toString() {
return ToStringBuilder.reflectionToString(this);
}

@JsonAnyGetter
public Map<String, Object> getAdditionalProperties() {
return this.additionalProperties;
}

@JsonAnySetter
public void setAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
}

public GDDResult withAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
return this;
}

}
