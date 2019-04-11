/*
 * Copyright (c) 2018-2019 Memorial Sloan-Kettering Cancer Center.
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

package org.mskcc.cmo.ks.ddp.pipeline;

import org.mskcc.cmo.ks.ddp.pipeline.util.DDPUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 *
 * @author ochoaa
 */
@Configuration
public class DDPConfiguration {

    @Value("${naaccr.ethnicity}")
    private String naaccrEthnicityFilename;

    @Value("${naaccr.race}")
    private String naaccrRaceFilename;

    @Value("${naaccr.sex}")
    private String naaccrSexFilename;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("#{${ddp.cohort.map}}")
    private Map<String, Integer> ddpCohortMap;

    @Bean(name="ddpCohortMap")
    public Map<String, Integer> ddpCohortMap(){
        return this.ddpCohortMap;
    }

    @Bean(name="naaccrEthnicityMap")
    public Map<String, String> naaccrEthnicityMap() throws Exception {
        URL naaccrEthnicityFile = resourceLoader.getResource(naaccrEthnicityFilename).getURL();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> mapLoaded = mapper.readValue(naaccrEthnicityFile, Map.class);
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : mapLoaded.entrySet()) {
            for (String label : entry.getValue()) {
                reverseMap.put(label.toUpperCase(), entry.getKey());
            }
        }
        DDPUtils.setNaaccrEthnicityMap(reverseMap);
        return reverseMap;
    }

    @Bean(name="naaccrRaceMap")
    public Map<String, String> naaccrRaceMap() throws Exception {
        URL naaccrRaceFile = resourceLoader.getResource(naaccrRaceFilename).getURL();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> mapLoaded = mapper.readValue(naaccrRaceFile, Map.class);
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : mapLoaded.entrySet()) {
            for (String label : entry.getValue()) {
                reverseMap.put(label.toUpperCase(), entry.getKey());
            }
        }
        DDPUtils.setNaaccrRaceMap(reverseMap);
        return reverseMap;
    }

    @Bean(name="naaccrSexMap")
    public Map<String, String> naaccrSexMap() throws Exception {
        URL naaccrSexFile = resourceLoader.getResource(naaccrSexFilename).getURL();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> mapLoaded = mapper.readValue(naaccrSexFile, Map.class);
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : mapLoaded.entrySet()) {
            for (String label : entry.getValue()) {
                reverseMap.put(label.toUpperCase(), entry.getKey());
            }
        }
        DDPUtils.setNaaccrSexMap(reverseMap);
        return reverseMap;
    }
}
