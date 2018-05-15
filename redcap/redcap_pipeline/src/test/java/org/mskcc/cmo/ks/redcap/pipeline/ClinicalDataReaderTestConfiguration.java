/*
 * Copyright (c) 2017-2018 Memorial Sloan-Kettering Cancer Center.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.mskcc.cmo.ks.redcap.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mskcc.cmo.ks.redcap.models.RedcapAttributeMetadata;
import org.mskcc.cmo.ks.redcap.models.RedcapProjectAttribute;
import org.mskcc.cmo.ks.redcap.pipeline.ClinicalDataReader;
import org.mskcc.cmo.ks.redcap.pipeline.TimelineReader;
import org.mskcc.cmo.ks.redcap.pipeline.util.RedcapUtils;
import org.mskcc.cmo.ks.redcap.pipeline.util.JobParameterUtils;
import org.mskcc.cmo.ks.redcap.source.ClinicalDataSource;
import org.mskcc.cmo.ks.redcap.source.internal.ClinicalDataSourceRedcapImpl;
import org.mskcc.cmo.ks.redcap.source.internal.CDDSessionManager;
import org.mskcc.cmo.ks.redcap.source.internal.MetadataCache;
import org.mskcc.cmo.ks.redcap.source.internal.MetadataManagerRedcapImpl;
import org.mskcc.cmo.ks.redcap.source.internal.RedcapRepository;
import org.mskcc.cmo.ks.redcap.source.internal.RedcapSessionManager;
import org.mskcc.cmo.ks.redcap.source.MetadataManager;
import org.mskcc.cmo.ks.redcap.util.ValueNormalizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.*;

@Configuration
public class ClinicalDataReaderTestConfiguration {

    public static final String REDCAP_STABLE_ID = "mskimpact";

    public static final String METADATA_TOKEN = "MetadataToken";
    public static final String NAMESPACE_TOKEN = "NamespaceToken";

    public static final String MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TITLE = "GbmSampleProjectTitle";
    public static final String MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TOKEN = "GbmSampleProjectToken";
    public static final String MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_INSTRUMENT_NAME = "my_first_instrument";
    public static final String MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TITLE = "GbmPatientProjectTitle";
    public static final String MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TOKEN = "GbmPatientProjectToken";
    public static final String MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_INSTRUMENT_NAME = "my_first_instrument";
    public static final String MSKIMPACT_MASKED_CLINICAL_PROJECT_TITLE = "ExcludedClinicalProjectTitle";
    public static final String MSKIMPACT_MASKED_CLINICAL_PROJECT_TOKEN = "ExcludedClinicalProjectToken";
    public static final String MSKIMPACT_MASKED_CLINICAL_PROJECT_INSTRUMENT_NAME = "my_first_instrument";
    public static final String MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TITLE = "SecondExcludedClinicalProjectTitle";
    public static final String MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TOKEN = "SecondExcludedClinicalProjectToken";
    public static final String MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_INSTRUMENT_NAME = "my_first_instrument";
    public static final String MSKIMPACT_TIMELINE_PROJECT_TITLE = "TimelineProjectTitle";
    public static final String MSKIMPACT_TIMELINE_PROJECT_TOKEN = "TimelineProjectToken";
    public static final String MSKIMPACT_TIMELINE_PROJECT_INSTRUMENT_NAME = "my_first_instrument";
    public static final String MSKIMPACT_MASKED_TIMELINE_PROJECT_TITLE = "MaskedTimelineProjectTitle";
    public static final String MSKIMPACT_MASKED_TIMELINE_PROJECT_TOKEN = "MaskedTimelineProjectToken";
    public static final String MSKIMPACT_MASKED_TIMELINE_INSTRUMENT_NAME = "my_first_instrument";

    @Bean
    public JobParameterUtils jobParameterUtils() {
        return new JobParameterUtils();
    }

    @Bean
    public ClinicalDataReader clinicalDataReader() {
        return new ClinicalDataReader();
    }

    @Bean
    public TimelineReader timelineReader() {
        return new TimelineReader();
    }
    
    @Bean
    public MetadataManager metadataManager() {
        return new MetadataManagerRedcapImpl();
    }

    @Bean 
    public CDDSessionManager cddSesssionManager() {
        CDDSessionManager cddSessionManager = Mockito.mock(CDDSessionManager.class);
        RedcapAttributeMetadata[] mockReturnForGetMetadata = makeMockMetadata();
        Mockito.when(cddSessionManager.getRedcapMetadata()).thenReturn(mockReturnForGetMetadata);
        return cddSessionManager;
    }

    @Bean
    public MetadataCache metadataCache() {
        return new MetadataCache();
    }

    @Bean
    public ClinicalDataSource clinicalDataSource() {
        return new ClinicalDataSourceRedcapImpl();
    }

    @Bean
    public RedcapRepository redcapRepository() {
        return new RedcapRepository();
    }

    @Bean
    public RedcapSessionManager redcapSessionManager() {
        RedcapSessionManager mockRedcapSessionManager = Mockito.mock(RedcapSessionManager.class);
        configureMockRedcapSessionManager(mockRedcapSessionManager);
        return mockRedcapSessionManager;
    }

    @Bean
    public RedcapUtils redcapUtils() {
        return new RedcapUtils();
    }

    @Bean
    public ValueNormalizer valueNormalizer() {
        return new ValueNormalizer();
    }
    
    @Bean
    public Properties jobParameters() {
        Properties properties = new Properties();
        properties.setProperty("rawData", "false");
        properties.setProperty("stableId", REDCAP_STABLE_ID);
        return properties;
    }

    private void configureMockRedcapSessionManager(RedcapSessionManager mockRedcapSessionManager) {
        //configure token requests
        Mockito.when(mockRedcapSessionManager.getTokenByProjectTitle(ArgumentMatchers.eq(MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TITLE))).thenReturn(MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TOKEN);
        Mockito.when(mockRedcapSessionManager.getTokenByProjectTitle(ArgumentMatchers.eq(MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TITLE))).thenReturn(MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TOKEN);
        Mockito.when(mockRedcapSessionManager.getTokenByProjectTitle(ArgumentMatchers.eq(MSKIMPACT_MASKED_CLINICAL_PROJECT_TITLE))).thenReturn(MSKIMPACT_MASKED_CLINICAL_PROJECT_TOKEN);
        Mockito.when(mockRedcapSessionManager.getTokenByProjectTitle(ArgumentMatchers.eq(MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TITLE))).thenReturn(MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TOKEN);
        Mockito.when(mockRedcapSessionManager.getTokenByProjectTitle(ArgumentMatchers.eq(MSKIMPACT_TIMELINE_PROJECT_TITLE))).thenReturn(MSKIMPACT_TIMELINE_PROJECT_TOKEN);
        Mockito.when(mockRedcapSessionManager.getTokenByProjectTitle(ArgumentMatchers.eq(MSKIMPACT_MASKED_TIMELINE_PROJECT_TITLE))).thenReturn(MSKIMPACT_MASKED_TIMELINE_PROJECT_TOKEN);
        Mockito.when(mockRedcapSessionManager.getTimelineTokenMapByStableId(ArgumentMatchers.eq(REDCAP_STABLE_ID))).thenReturn(makeMockTimelineTokenMap());
        Mockito.when(mockRedcapSessionManager.getClinicalTokenMapByStableId(ArgumentMatchers.eq(REDCAP_STABLE_ID))).thenReturn(makeMockClinicalTokenMap());
        //configure redcap data requests
        Mockito.when(mockRedcapSessionManager.getRedcapAttributeByToken(ArgumentMatchers.eq(MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TOKEN))).thenReturn(makeMockGbmSampleAttributesData());
        Mockito.when(mockRedcapSessionManager.getRedcapAttributeByToken(ArgumentMatchers.eq(MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TOKEN))).thenReturn(makeMockGbmPatientAttributesData());
        Mockito.when(mockRedcapSessionManager.getRedcapAttributeByToken(ArgumentMatchers.eq(MSKIMPACT_MASKED_CLINICAL_PROJECT_TOKEN))).thenReturn(makeMockGbmSampleAttributesData());
        Mockito.when(mockRedcapSessionManager.getRedcapAttributeByToken(ArgumentMatchers.eq(MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TOKEN))).thenReturn(makeMockGbmSampleAttributesData());
        Mockito.when(mockRedcapSessionManager.getRedcapAttributeByToken(ArgumentMatchers.eq(MSKIMPACT_TIMELINE_PROJECT_TOKEN))).thenReturn(makeMockTimelineAttributesData());
        Mockito.when(mockRedcapSessionManager.getRedcapAttributeByToken(ArgumentMatchers.eq(MSKIMPACT_MASKED_TIMELINE_PROJECT_TOKEN))).thenReturn(makeMockTimelineAttributesData());

        Mockito.when(mockRedcapSessionManager.redcapDataTypeIsTimeline(ArgumentMatchers.eq(MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TITLE))).thenReturn(false);
        Mockito.when(mockRedcapSessionManager.redcapDataTypeIsTimeline(ArgumentMatchers.eq(MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TITLE))).thenReturn(false);
        Mockito.when(mockRedcapSessionManager.redcapDataTypeIsTimeline(ArgumentMatchers.eq(MSKIMPACT_MASKED_CLINICAL_PROJECT_TITLE))).thenReturn(false);
        Mockito.when(mockRedcapSessionManager.redcapDataTypeIsTimeline(ArgumentMatchers.eq(MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TITLE))).thenReturn(false);
        Mockito.when(mockRedcapSessionManager.redcapDataTypeIsTimeline(ArgumentMatchers.eq(MSKIMPACT_TIMELINE_PROJECT_TITLE))).thenReturn(true);
        Mockito.when(mockRedcapSessionManager.redcapDataTypeIsTimeline(ArgumentMatchers.eq(MSKIMPACT_MASKED_TIMELINE_PROJECT_TITLE))).thenReturn(true);

        Mockito.when(mockRedcapSessionManager.getRedcapDataForProjectByToken(ArgumentMatchers.eq(MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TOKEN))).thenReturn(makeMockGbmSampleData());
        Mockito.when(mockRedcapSessionManager.getRedcapDataForProjectByToken(ArgumentMatchers.eq(MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TOKEN))).thenReturn(makeMockGbmPatientData());
        Mockito.when(mockRedcapSessionManager.getRedcapDataForProjectByToken(ArgumentMatchers.eq(MSKIMPACT_MASKED_CLINICAL_PROJECT_TOKEN))).thenReturn(makeMockMaskedSampleData());
        Mockito.when(mockRedcapSessionManager.getRedcapDataForProjectByToken(ArgumentMatchers.eq(MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TOKEN))).thenReturn(makeSecondMockMaskedSampleData());
        Mockito.when(mockRedcapSessionManager.getRedcapDataForProjectByToken(ArgumentMatchers.eq(MSKIMPACT_TIMELINE_PROJECT_TOKEN))).thenReturn(makeMockTimelineData());
        Mockito.when(mockRedcapSessionManager.getRedcapDataForProjectByToken(ArgumentMatchers.eq(MSKIMPACT_MASKED_TIMELINE_PROJECT_TOKEN))).thenReturn(makeMockMaskedTimelineData());    
    }

    private Map<String, String> makeMockClinicalTokenMap() {
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put(MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TITLE, MSKIMPACT_GBM_SAMPLE_CLINICAL_PROJECT_TOKEN);
        returnMap.put(MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TITLE, MSKIMPACT_GBM_PATIENT_CLINICAL_PROJECT_TOKEN);
        returnMap.put(MSKIMPACT_MASKED_CLINICAL_PROJECT_TITLE, MSKIMPACT_MASKED_CLINICAL_PROJECT_TOKEN);
        returnMap.put(MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TITLE, MSKIMPACT_SECOND_MASKED_CLINICAL_PROJECT_TOKEN);
        return returnMap;
    }

    private Map<String, String> makeMockTimelineTokenMap() {
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put(MSKIMPACT_TIMELINE_PROJECT_TITLE, MSKIMPACT_TIMELINE_PROJECT_TOKEN);
        returnMap.put(MSKIMPACT_MASKED_TIMELINE_PROJECT_TITLE, MSKIMPACT_MASKED_TIMELINE_PROJECT_TOKEN);
        return returnMap;
    }

    private RedcapAttributeMetadata[] makeMockMetadata() {
        RedcapAttributeMetadata[] metadata = new RedcapAttributeMetadata[6];
        metadata[0] = new RedcapAttributeMetadata();
        metadata[0].setNormalizedColumnHeader("PATIENT_ID");
        metadata[0].setAttributeType("PATIENT");
        metadata[0].setPriority("1");
        metadata[0].setDisplayName("Patient Id");
        metadata[0].setDatatype("STRING");
        metadata[0].setDescriptions("This identifies a patient");
        metadata[1] = new RedcapAttributeMetadata();
        metadata[1].setNormalizedColumnHeader("CANCER_TYPE");
        metadata[1].setAttributeType("SAMPLE");
        metadata[1].setPriority("1");
        metadata[1].setDisplayName("cancer type");
        metadata[1].setDatatype("STRING");
        metadata[1].setDescriptions("cancer type");
        metadata[2] = new RedcapAttributeMetadata();
        metadata[2].setNormalizedColumnHeader("AGE");
        metadata[2].setAttributeType("PATIENT");
        metadata[2].setPriority("1");
        metadata[2].setDisplayName("AGE");
        metadata[2].setDatatype("STRING");
        metadata[2].setDescriptions("Patient age in years");
        //Added the following for getSampleHeader() / getPatientHeader() tests
        metadata[3] = new RedcapAttributeMetadata();
        metadata[3].setNormalizedColumnHeader("SAMPLE_ID");
        metadata[3].setAttributeType("SAMPLE");
        metadata[3].setPriority("1");
        metadata[3].setDisplayName("Sample Id");
        metadata[3].setDatatype("STRING");
        metadata[3].setDescriptions("This identifies a sample");
        metadata[4] = new RedcapAttributeMetadata();
        metadata[4].setNormalizedColumnHeader("START_DATE");
        metadata[4].setAttributeType("PATIENT");
        metadata[4].setPriority("1");
        metadata[4].setDisplayName("Start Date");
        metadata[4].setDatatype("NUMBER");
        metadata[4].setDescriptions("start date");
        metadata[5] = new RedcapAttributeMetadata();
        metadata[5].setNormalizedColumnHeader("STOP_DATE");
        metadata[5].setAttributeType("PATIENT");
        metadata[5].setPriority("1");
        metadata[5].setDisplayName("Stop Date");
        metadata[5].setDatatype("NUMBER");
        metadata[5].setDescriptions("stop date");
        return metadata;
    }

    private RedcapAttributeMetadata[] makeMockNamespace() {
        RedcapAttributeMetadata[] namespace = new RedcapAttributeMetadata[0];
        return namespace;
    }

    private RedcapProjectAttribute[] makeMockGbmSampleAttributesData() {
        RedcapProjectAttribute[] attributeArray = new RedcapProjectAttribute[4];
        RedcapProjectAttribute sampleIdAttribute = new RedcapProjectAttribute();
        sampleIdAttribute.setFieldName("sample_id");
        sampleIdAttribute.setFormName("my_first_instrument");
        attributeArray[0] = sampleIdAttribute;
        RedcapProjectAttribute patientIdAttribute = new RedcapProjectAttribute();
        patientIdAttribute.setFieldName("patient_id");
        patientIdAttribute.setFormName("my_first_instrument");
        attributeArray[1] = patientIdAttribute;
        RedcapProjectAttribute cancerTypeAttribute = new RedcapProjectAttribute();
        cancerTypeAttribute.setFieldName("cancer_type");
        cancerTypeAttribute.setFormName("my_first_instrument");
        attributeArray[2] = cancerTypeAttribute;
        RedcapProjectAttribute myFirstInstrumentCompleteAttribute = new RedcapProjectAttribute();
        myFirstInstrumentCompleteAttribute.setFieldName("my_first_instrument_complete");
        myFirstInstrumentCompleteAttribute.setFormName("my_first_instrument");
        attributeArray[3] = myFirstInstrumentCompleteAttribute;
        return attributeArray;
    }

    private RedcapProjectAttribute[] makeMockGbmPatientAttributesData() {
        RedcapProjectAttribute[] attributeArray = new RedcapProjectAttribute[3];
        RedcapProjectAttribute patientIdAttribute = new RedcapProjectAttribute();
        patientIdAttribute.setFieldName("patient_id");
        patientIdAttribute.setFormName("my_first_instrument");
        attributeArray[0] = patientIdAttribute;
        RedcapProjectAttribute ageAttribute = new RedcapProjectAttribute();
        ageAttribute.setFieldName("age");
        ageAttribute.setFormName("my_first_instrument");
        attributeArray[1] = ageAttribute;
        RedcapProjectAttribute myFirstInstrumentCompleteAttribute = new RedcapProjectAttribute();
        myFirstInstrumentCompleteAttribute.setFieldName("my_first_instrument_complete");
        myFirstInstrumentCompleteAttribute.setFormName("my_first_instrument");
        attributeArray[2] = myFirstInstrumentCompleteAttribute;
        return attributeArray;
    }

    private RedcapProjectAttribute[] makeMockTimelineAttributesData() {
        RedcapProjectAttribute[] attributeArray = new RedcapProjectAttribute[3];
        RedcapProjectAttribute patientIdAttribute = new RedcapProjectAttribute();
        patientIdAttribute.setFieldName("patient_id");
        patientIdAttribute.setFormName("my_first_instrument");
        attributeArray[0] = patientIdAttribute;
        RedcapProjectAttribute startDateAttribute = new RedcapProjectAttribute();
        startDateAttribute.setFieldName("start_date");
        startDateAttribute.setFormName("my_first_instrument");
        attributeArray[1] = startDateAttribute;
        RedcapProjectAttribute stopDateAttribute = new RedcapProjectAttribute();
        stopDateAttribute.setFieldName("stop_date");
        stopDateAttribute.setFormName("my_first_instrument");
        attributeArray[2] = stopDateAttribute;
        return attributeArray;
    }

 
    private JsonNode[] makeMockGbmSampleData() {
        String mockReturnJsonString =
                "[" +
                    "{\"sample_id\":\"P-0000001-T01-IM6\",\"patient_id\":\"P-0000001\",\"cancer_type\":\"GBM\"}," +
                    "{\"sample_id\":\"P-0000002-T01-IM6\",\"patient_id\":\"P-0000002\",\"cancer_type\":\"GBM\"}," +
                    "{\"sample_id\":\"P-0000003-T01-IM6\",\"patient_id\":\"P-0000003\",\"cancer_type\":\"GBM\"}" +
                "]";
        return makeMockReturnForGetData(mockReturnJsonString);
    }

    private JsonNode[] makeMockMaskedSampleData() {
        String mockReturnJsonString =
                "[" +
                    "{\"sample_id\":\"P-0000004-T01-IM6\",\"patient_id\":\"P-0000004\",\"cancer_type\":\"GBM\"}," +
                    "{\"sample_id\":\"P-0000005-T01-IM6\",\"patient_id\":\"P-0000005\",\"cancer_type\":\"GBM\"}," +
                    "{\"sample_id\":\"P-0000006-T01-IM6\",\"patient_id\":\"P-0000006\",\"cancer_type\":\"GBM\"}" +
                "]";
        return makeMockReturnForGetData(mockReturnJsonString);
    }
   
    private JsonNode[] makeSecondMockMaskedSampleData() {
        String mockReturnJsonString =
                "[" +
                    "{\"sample_id\":\"P-0000007-T01-IM6\",\"patient_id\":\"P-0000007\",\"cancer_type\":\"GBM\"}," +
                    "{\"sample_id\":\"P-0000008-T01-IM6\",\"patient_id\":\"P-0000008\",\"cancer_type\":\"GBM\"}," +
                    "{\"sample_id\":\"P-0000009-T01-IM6\",\"patient_id\":\"P-0000009\",\"cancer_type\":\"GBM\"}" +
                "]";
        return makeMockReturnForGetData(mockReturnJsonString);
    }
   
    private JsonNode[] makeMockTimelineData() {
        String mockReturnJsonString =
                "[" +
                    "{\"patient_id\":\"P-0000001\",\"start_date\":\"5\",\"stop_date\":\"20\"}," +
                    "{\"patient_id\":\"P-0000002\",\"start_date\":\"12\",\"stop_date\":\"87\"}," +
                    "{\"patient_id\":\"P-0000003\",\"start_date\":\"13\",\"stop_date\":\"24\"}" +
                "]";
        return makeMockReturnForGetData(mockReturnJsonString);
    }

    private JsonNode[] makeMockGbmPatientData() {
        String mockReturnJsonString =
                "[" +
                    "{\"patient_id\":\"P-0000001\",\"age\":\"29\"}," +
                    "{\"patient_id\":\"P-0000002\",\"age\":\"48\"}," +
                    "{\"patient_id\":\"P-0000003\",\"age\":\"71\"}" +
                "]";
        return makeMockReturnForGetData(mockReturnJsonString);
    }

    private JsonNode[] makeMockMaskedTimelineData() {
        String mockReturnJsonString =
                "[" +
                    "{\"patient_id\":\"P-0000004\",\"start_date\":\"5\",\"stop_date\":\"20\"}," +
                    "{\"patient_id\":\"P-0000005\",\"start_date\":\"12\",\"stop_date\":\"87\"}," +
                    "{\"patient_id\":\"P-0000006\",\"start_date\":\"13\",\"stop_date\":\"24\"}" +
                "]";
        return makeMockReturnForGetData(mockReturnJsonString);
    }

    private JsonNode[] makeMockReturnForGetData(String mockReturnJsonString) {
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> jsonNodeList = new ArrayList<JsonNode>();
        try {
            Iterator<JsonNode> nodeIterator = mapper.readTree(mockReturnJsonString).elements();
            while (nodeIterator.hasNext()) {
                jsonNodeList.add(nodeIterator.next());
            }
            return jsonNodeList.toArray(new JsonNode[jsonNodeList.size()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
