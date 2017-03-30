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
    "record_id",
    "external_column_header",
    "normalized_column_header",
    "display_name",
    "descriptions",
    "datatype",
    "attribute_type",
    "priority",
    "note",
    "redcap_id",
    "my_first_instrument_complete"
})
public class RedcapAttributeMetadata {

    @JsonProperty("record_id")
    private Long recordId;
    @JsonProperty("external_column_header")
    private String externalColumnHeader;
    @JsonProperty("normalized_column_header")
    private String normalizedColumnHeader;
    @JsonProperty("display_name")
    private String displayName;
    @JsonProperty("descriptions")
    private String descriptions;
    @JsonProperty("datatype")
    private String datatype;
    @JsonProperty("attribute_type")
    private String attributeType;
    @JsonProperty("priority")
    private String priority;
    @JsonProperty("note")
    private String note;
    @JsonProperty("redcap_id")
    private String redcapId;
    @JsonProperty("my_first_instrument_complete")
    private String myFirstInstrumentComplete;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    * 
    */
    public RedcapAttributeMetadata() {}

    /**
    * 
    * @param recordId
    * @param normalizedColumnHeader
    * @param myFirstInstrumentComplete
    * @param externalColumnHeader
    * @param priority
    * @param attributeType
    * @param datatype
    * @param displayName
    * @param note
    * @param redcapId
    * @param descriptions
    */
    public RedcapAttributeMetadata(Long recordId, String externalColumnHeader, String normalizedColumnHeader, String displayName, String descriptions, String datatype, String attributeType, String priority, String note, String redcapId, String myFirstInstrumentComplete) {
        this.recordId = recordId;
        this.externalColumnHeader = externalColumnHeader;
        this.normalizedColumnHeader = normalizedColumnHeader;
        this.displayName = displayName;
        this.descriptions = descriptions;
        this.datatype = datatype;
        this.attributeType = attributeType;
        this.priority = priority;
        this.note = note;
        this.redcapId = redcapId;
        this.myFirstInstrumentComplete = myFirstInstrumentComplete;
    }

    /**
    * 
    * @return
    * The recordId
    */
    @JsonProperty("record_id")
    public Long getRecordId() {
        return recordId;
    }

    /**
    * 
    * @param recordId
    * The record_id
    */
    @JsonProperty("record_id")
    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    /**
    * 
    * @return
    * The externalColumnHeader
    */
    @JsonProperty("external_column_header")
    public String getExternalColumnHeader() {
        return externalColumnHeader;
    }

    /**
    * 
    * @param externalColumnHeader
    * The external_column_header
    */
    @JsonProperty("external_column_header")
    public void setExternalColumnHeader(String externalColumnHeader) {
        this.externalColumnHeader = externalColumnHeader;
    }

    /**
    * 
    * @return
    * The normalizedColumnHeader
    */
    @JsonProperty("normalized_column_header")
    public String getNormalizedColumnHeader() {
        return normalizedColumnHeader;
    }

    /**
    * 
    * @param normalizedColumnHeader
    * The normalized_column_header
    */
    @JsonProperty("normalized_column_header")
    public void setNormalizedColumnHeader(String normalizedColumnHeader) {
        this.normalizedColumnHeader = normalizedColumnHeader;
    }

    /**
    * 
    * @return
    * The displayName
    */
    @JsonProperty("display_name")
    public String getDisplayName() {
        return displayName;
    }

    /**
    * 
    * @param displayName
    * The display_name
    */
    @JsonProperty("display_name")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
    * 
    * @return
    * The descriptions
    */
    @JsonProperty("descriptions")
    public String getDescriptions() {
        return descriptions;
    }

    /**
    * 
    * @param descriptions
    * The descriptions
    */
    @JsonProperty("descriptions")
    public void setDescriptions(String descriptions) {
        this.descriptions = descriptions;
    }

    /**
    * 
    * @return
    * The datatype
    */
    @JsonProperty("datatype")
    public String getDatatype() {
        return datatype;
    }

    /**
    * 
    * @param datatype
    * The datatype
    */
    @JsonProperty("datatype")
    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    /**
    * 
    * @return
    * The attributeType
    */
    @JsonProperty("attribute_type")
    public String getAttributeType() {
        return attributeType;
    }

    /**
    * 
    * @param attributeType
    * The attribute_type
    */
    @JsonProperty("attribute_type")
    public void setAttributeType(String attributeType) {
        this.attributeType = attributeType;
    }

    /**
    * 
    * @return
    * The priority
    */
    @JsonProperty("priority")
    public String getPriority() {
        return priority;
    }

    /**
    * 
    * @param priority
    * The priority
    */
    @JsonProperty("priority")
    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
    * 
    * @return
    * The note
    */
    @JsonProperty("note")
    public String getNote() {
        return note;
    }

    /**
    * 
    * @param note
    * The note
    */
    @JsonProperty("note")
    public void setNote(String note) {
        this.note = note;
    }

    /**
    * 
    * @return
    * The note
    */
    @JsonProperty("redcap_id")
    public String getRedcapId() {
        return redcapId;
    }

    /**
    * 
    * @param note
    * The note
    */
    @JsonProperty("redcap_id")
    public void setRedcapId(String redcapId) {
        this.redcapId = redcapId;
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
