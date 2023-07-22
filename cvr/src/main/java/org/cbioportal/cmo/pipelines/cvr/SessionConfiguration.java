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

package org.cbioportal.cmo.pipelines.cvr;

import java.time.Instant;
import java.util.*;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.common.util.HttpClientWithTimeoutAndRetry;
import org.cbioportal.cmo.pipelines.common.util.InstantStringUtil;
import org.cbioportal.cmo.pipelines.cvr.model.CVRSession;
import org.cbioportal.cmo.pipelines.cvr.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

/**
 *
 * @author heinsz
 */

@Configuration
public class SessionConfiguration {

    @Value("${dmp.tokens.retrieve_variants.impact}")
    private String retrieveVariantsImpact;

    @Value("${dmp.tokens.retrieve_variants.rdts}")
    private String retrieveVariantsRaindance;

    @Value("${dmp.tokens.retrieve_variants.heme}")
    private String retrieveVariantsHeme;

    @Value("${dmp.tokens.retrieve_variants.archer}")
    private String retrieveVariantsArcher;

    @Value("${dmp.tokens.retrieve_variants.access}")
    private String retrieveVariantsAccess;

    @Value("${dmp.tokens.retrieve_master_list.impact}")
    private String dmpMasterListImpact;

    @Value("${dmp.tokens.retrieve_master_list.rdts}")
    private String dmpMasterListRaindance;

    @Value("${dmp.tokens.retrieve_master_list.heme}")
    private String dmpMasterListHeme;

    @Value("${dmp.tokens.retrieve_master_list.archer}")
    private String dmpMasterListArcher;

    @Value("${dmp.tokens.retrieve_master_list.access}")
    private String dmpMasterListAccess;

    private Logger log = Logger.getLogger(SessionConfiguration.class);

    @Bean
    public SessionFactory sessionFactory() {
        return new SessionFactory();
    }

    /**
     * Maps a study id to it's dmp retrieve variants token.
     * @return
     */
    @Bean(name="retrieveVariantTokensMap")
    public Map<String, String> retrieveVariantTokensMap() {
        Map<String, String> map = new HashMap<>();
        map.put("mskimpact", retrieveVariantsImpact);
        map.put("mskraindance", retrieveVariantsRaindance);
        map.put("mskimpact_heme", retrieveVariantsHeme);
        map.put("mskarcher", retrieveVariantsArcher);
        map.put("mskaccess", retrieveVariantsAccess);

        return map;
    }

    @Bean(name="masterListTokensMap")
    public Map<String, String> masterListTokensMap() {
        Map<String, String> map = new HashMap<>();
        map.put("mskimpact", dmpMasterListImpact);
        map.put("mskraindance", dmpMasterListRaindance);
        map.put("mskimpact_heme", dmpMasterListHeme);
        map.put("mskarcher", dmpMasterListArcher);
        map.put("mskaccess", dmpMasterListAccess);

        return map;
    }

}
