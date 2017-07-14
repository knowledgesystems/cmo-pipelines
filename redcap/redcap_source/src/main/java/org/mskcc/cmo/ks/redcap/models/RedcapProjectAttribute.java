/*
 * Copyright (c) 2016 - 2017 Memorial Sloan-Kettering Cancer Center.
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
    "field_name",
    "form_name",
    "section_header",
    "field_type",
    "field_label",
    "select_choices_or_calculations",
    "field_note",
    "text_validation_type_or_show_slider_number",
    "text_validation_min",
    "text_validation_max",
    "identifier",
    "branching_logic",
    "required_field",
    "custom_alignment",
    "question_number",
    "matrix_group_name",
    "matrix_ranking",
    "field_annotation"
})
public class RedcapProjectAttribute {

    @JsonProperty("field_name")
    private String fieldName;
    @JsonProperty("form_name")
    private String formName;
    @JsonProperty("section_header")
    private String sectionHeader;
    @JsonProperty("field_type")
    private String fieldType;
    @JsonProperty("field_label")
    private String fieldLabel;
    @JsonProperty("select_choices_or_calculations")
    private String selectChoicesOrCalculations;
    @JsonProperty("field_note")
    private String fieldNote;
    @JsonProperty("text_validation_type_or_show_slider_number")
    private String textValidationTypeOrShowSliderNumber;
    @JsonProperty("text_validation_min")
    private String textValidationMin;
    @JsonProperty("text_validation_max")
    private String textValidationMax;
    @JsonProperty("identifier")
    private String identifier;
    @JsonProperty("branching_logic")
    private String branchingLogic;
    @JsonProperty("required_field")
    private String requiredField;
    @JsonProperty("custom_alignment")
    private String customAlignment;
    @JsonProperty("question_number")
    private String questionNumber;
    @JsonProperty("matrix_group_name")
    private String matrixGroupName;
    @JsonProperty("matrix_ranking")
    private String matrixRanking;
    @JsonProperty("field_annotation")
    private String fieldAnnotation;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
    * No args constructor for use in serialization
    *
    */
    public RedcapProjectAttribute() {
    }

    /**
    *
    * @param matrixGroupName
    * @param fieldType
    * @param selectChoicesOrCalculations
    * @param formName
    * @param fieldNote
    * @param textValidationMax
    * @param customAlignment
    * @param fieldLabel
    * @param textValidationMin
    * @param questionNumber
    * @param sectionHeader
    * @param textValidationTypeOrShowSliderNumber
    * @param matrixRanking
    * @param requiredField
    * @param fieldName
    * @param identifier
    * @param fieldAnnotation
    * @param branchingLogic
    */
    public RedcapProjectAttribute(String fieldName, String formName, String sectionHeader, String fieldType, String fieldLabel, String selectChoicesOrCalculations, String fieldNote, String textValidationTypeOrShowSliderNumber, String textValidationMin, String textValidationMax, String identifier, String branchingLogic, String requiredField, String customAlignment, String questionNumber, String matrixGroupName, String matrixRanking, String fieldAnnotation) {
        this.fieldName = fieldName;
        this.formName = formName;
        this.sectionHeader = sectionHeader;
        this.fieldType = fieldType;
        this.fieldLabel = fieldLabel;
        this.selectChoicesOrCalculations = selectChoicesOrCalculations;
        this.fieldNote = fieldNote;
        this.textValidationTypeOrShowSliderNumber = textValidationTypeOrShowSliderNumber;
        this.textValidationMin = textValidationMin;
        this.textValidationMax = textValidationMax;
        this.identifier = identifier;
        this.branchingLogic = branchingLogic;
        this.requiredField = requiredField;
        this.customAlignment = customAlignment;
        this.questionNumber = questionNumber;
        this.matrixGroupName = matrixGroupName;
        this.matrixRanking = matrixRanking;
        this.fieldAnnotation = fieldAnnotation;
    }

    /**
    *
    * @return
    * The fieldName
    */
    @JsonProperty("field_name")
    public String getFieldName() {
        return fieldName;
    }

    /**
    *
    * @param fieldName
    * The field_name
    */
    @JsonProperty("field_name")
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
    *
    * @return
    * The formName
    */
    @JsonProperty("form_name")
    public String getFormName() {
        return formName;
    }

    /**
    *
    * @param formName
    * The form_name
    */
    @JsonProperty("form_name")
    public void setFormName(String formName) {
        this.formName = formName;
    }

    /**
    *
    * @return
    * The sectionHeader
    */
    @JsonProperty("section_header")
    public String getSectionHeader() {
        return sectionHeader;
    }

    /**
    *
    * @param sectionHeader
    * The section_header
    */
    @JsonProperty("section_header")
    public void setSectionHeader(String sectionHeader) {
        this.sectionHeader = sectionHeader;
    }

    /**
    *
    * @return
    * The fieldType
    */
    @JsonProperty("field_type")
    public String getFieldType() {
        return fieldType;
    }

    /**
    *
    * @param fieldType
    * The field_type
    */
    @JsonProperty("field_type")
    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    /**
    *
    * @return
    * The fieldLabel
    */
    @JsonProperty("field_label")
    public String getFieldLabel() {
        return fieldLabel;
    }

    /**
    *
    * @param fieldLabel
    * The field_label
    */
    @JsonProperty("field_label")
    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    /**
    *
    * @return
    * The selectChoicesOrCalculations
    */
    @JsonProperty("select_choices_or_calculations")
    public String getSelectChoicesOrCalculations() {
        return selectChoicesOrCalculations;
    }

    /**
    *
    * @param selectChoicesOrCalculations
    * The select_choices_or_calculations
    */
    @JsonProperty("select_choices_or_calculations")
    public void setSelectChoicesOrCalculations(String selectChoicesOrCalculations) {
        this.selectChoicesOrCalculations = selectChoicesOrCalculations;
    }

    /**
    *
    * @return
    * The fieldNote
    */
    @JsonProperty("field_note")
    public String getFieldNote() {
        return fieldNote;
    }

    /**
    *
    * @param fieldNote
    * The field_note
    */
    @JsonProperty("field_note")
    public void setFieldNote(String fieldNote) {
        this.fieldNote = fieldNote;
    }

    /**
    *
    * @return
    * The textValidationTypeOrShowSliderNumber
    */
    @JsonProperty("text_validation_type_or_show_slider_number")
    public String getTextValidationTypeOrShowSliderNumber() {
        return textValidationTypeOrShowSliderNumber;
    }

    /**
    *
    * @param textValidationTypeOrShowSliderNumber
    * The text_validation_type_or_show_slider_number
    */
    @JsonProperty("text_validation_type_or_show_slider_number")
    public void setTextValidationTypeOrShowSliderNumber(String textValidationTypeOrShowSliderNumber) {
        this.textValidationTypeOrShowSliderNumber = textValidationTypeOrShowSliderNumber;
    }

    /**
    *
    * @return
    * The textValidationMin
    */
    @JsonProperty("text_validation_min")
    public String getTextValidationMin() {
        return textValidationMin;
    }

    /**
    *
    * @param textValidationMin
    * The text_validation_min
    */
    @JsonProperty("text_validation_min")
    public void setTextValidationMin(String textValidationMin) {
        this.textValidationMin = textValidationMin;
    }

    /**
    *
    * @return
    * The textValidationMax
    */
    @JsonProperty("text_validation_max")
    public String getTextValidationMax() {
        return textValidationMax;
    }

    /**
    *
    * @param textValidationMax
    * The text_validation_max
    */
    @JsonProperty("text_validation_max")
    public void setTextValidationMax(String textValidationMax) {
        this.textValidationMax = textValidationMax;
    }

    /**
    *
    * @return
    * The identifier
    */
    @JsonProperty("identifier")
    public String getIdentifier() {
        return identifier;
    }

    /**
    *
    * @param identifier
    * The identifier
    */
    @JsonProperty("identifier")
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
    *
    * @return
    * The branchingLogic
    */
    @JsonProperty("branching_logic")
    public String getBranchingLogic() {
        return branchingLogic;
    }

    /**
    *
    * @param branchingLogic
    * The branching_logic
    */
    @JsonProperty("branching_logic")
    public void setBranchingLogic(String branchingLogic) {
        this.branchingLogic = branchingLogic;
    }

    /**
    *
    * @return
    * The requiredField
    */
    @JsonProperty("required_field")
    public String getRequiredField() {
        return requiredField;
    }

    /**
    *
    * @param requiredField
    * The required_field
    */
    @JsonProperty("required_field")
    public void setRequiredField(String requiredField) {
        this.requiredField = requiredField;
    }

    /**
    *
    * @return
    * The customAlignment
    */
    @JsonProperty("custom_alignment")
    public String getCustomAlignment() {
        return customAlignment;
    }

    /**
    *
    * @param customAlignment
    * The custom_alignment
    */
    @JsonProperty("custom_alignment")
    public void setCustomAlignment(String customAlignment) {
        this.customAlignment = customAlignment;
    }

    /**
    *
    * @return
    * The questionNumber
    */
    @JsonProperty("question_number")
    public String getQuestionNumber() {
        return questionNumber;
    }

    /**
    *
    * @param questionNumber
    * The question_number
    */
    @JsonProperty("question_number")
    public void setQuestionNumber(String questionNumber) {
        this.questionNumber = questionNumber;
    }

    /**
    *
    * @return
    * The matrixGroupName
    */
    @JsonProperty("matrix_group_name")
    public String getMatrixGroupName() {
        return matrixGroupName;
    }

    /**
    *
    * @param matrixGroupName
    * The matrix_group_name
    */
    @JsonProperty("matrix_group_name")
    public void setMatrixGroupName(String matrixGroupName) {
        this.matrixGroupName = matrixGroupName;
    }

    /**
    *
    * @return
    * The matrixRanking
    */
    @JsonProperty("matrix_ranking")
    public String getMatrixRanking() {
        return matrixRanking;
    }

    /**
    *
    * @param matrixRanking
    * The matrix_ranking
    */
    @JsonProperty("matrix_ranking")
    public void setMatrixRanking(String matrixRanking) {
        this.matrixRanking = matrixRanking;
    }

    /**
    *
    * @return
    * The fieldAnnotation
    */
    @JsonProperty("field_annotation")
    public String getFieldAnnotation() {
        return fieldAnnotation;
    }

    /**
    *
    * @param fieldAnnotation
    * The field_annotation
    */
    @JsonProperty("field_annotation")
    public void setFieldAnnotation(String fieldAnnotation) {
        this.fieldAnnotation = fieldAnnotation;
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
