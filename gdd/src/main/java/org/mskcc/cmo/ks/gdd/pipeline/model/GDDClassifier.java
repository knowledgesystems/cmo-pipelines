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


/**
* Genome Directed Diagnosis Schema
* <p>
* Output format description for GDD web service
* 
*/
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
"version",
"result"
})
public class GDDClassifier {

/**
* 
* (Required)
* 
*/
@JsonProperty("version")
private String version;
/**
* 
* (Required)
* 
*/
@JsonProperty("result")
private List<GDDResult> result = new ArrayList<GDDResult>();
@JsonIgnore
private Map<String, Object> additionalProperties = new HashMap<String, Object>();

/**
* No args constructor for use in serialization
* 
*/
public GDDClassifier() {
}

/**
* 
* @param result
* @param version
*/
public GDDClassifier(String version, List<GDDResult> result) {
this.version = version;
this.result = result;
}

/**
* 
* (Required)
* 
* @return
* The version
*/
@JsonProperty("version")
public String getVersion() {
return version;
}

/**
* 
* (Required)
* 
* @param version
* The version
*/
@JsonProperty("version")
public void setVersion(String version) {
this.version = version;
}

public GDDClassifier withVersion(String version) {
this.version = version;
return this;
}

/**
* 
* (Required)
* 
* @return
* The result
*/
@JsonProperty("result")
public List<GDDResult> getResult() {
return result;
}

/**
* 
* (Required)
* 
* @param result
* The result
*/
@JsonProperty("result")
public void setResult(List<GDDResult> result) {
this.result = result;
}

public GDDClassifier withResult(List<GDDResult> result) {
this.result = result;
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

public GDDClassifier withAdditionalProperty(String name, Object value) {
this.additionalProperties.put(name, value);
return this;
}

}
