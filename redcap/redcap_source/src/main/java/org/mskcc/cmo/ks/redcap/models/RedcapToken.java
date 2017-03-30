/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cmo.ks.redcap.models;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "study_id",    
    "api_token",
    "stable_id",
    "my_first_instrument_complete"
})
public class RedcapToken {

    @JsonProperty("study_id")
    private String studyId;
    @JsonProperty("api_token")
    private String apiToken;
    @JsonProperty("stable_id")
    private String stableId;
    @JsonProperty("my_first_instrument_complete")
    private String myFirstInstrumentComplete;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    * 
    */
    public RedcapToken() {
    }

    /**
    * 
    * @param studyId
    * @param apiToken
    * @param stableId
    * @param myFirstInstrumentComplete
    */
    public RedcapToken(String studyId, String apiToken, String stableId, String myFirstInstrumentComplete) {
        this.studyId = studyId;
        this.apiToken = apiToken;
        this.stableId = stableId;
        this.myFirstInstrumentComplete = myFirstInstrumentComplete;
    }
    
    /**
    * 
    * @return
    * The studyId
    */
    @JsonProperty("study_id")
    public String getStudyId() {
        return studyId;
    }

    /**
    * 
    * @param studyId
    * The study_id
    */
    @JsonProperty("study_id")
    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    /**
    * 
    * @return
    * The apiToken
    */
    @JsonProperty("api_token")
    public String getApiToken() {
        return apiToken;
    }

    /**
    * 
    * @param apiToken
    * The api_token
    */
    @JsonProperty("api_token")
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
    
    /**
    * 
    * @return
    * The stableId
    */
    @JsonProperty("stable_id")
    public String getStableId() {
        return stableId;
    }

    /**
    * 
    * @param stableId
    * The stable_id
    */
    @JsonProperty("stable_id")
    public void setStableId(String stableId) {
        this.stableId = stableId;
    }    

    /**
    * 
    * @return
    * The myFirstInstrumentComplete
    */
    @JsonProperty("my_first_instrument_complete")
    public String getMyFirstInstrumentComplete() {
        return myFirstInstrumentComplete;
    }

    /**
    * 
    * @param myFirstInstrumentComplete
    * The my_first_instrument_complete
    */
    @JsonProperty("my_first_instrument_complete")
    public void setMyFirstInstrumentComplete(String myFirstInstrumentComplete) {
        this.myFirstInstrumentComplete = myFirstInstrumentComplete;
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
