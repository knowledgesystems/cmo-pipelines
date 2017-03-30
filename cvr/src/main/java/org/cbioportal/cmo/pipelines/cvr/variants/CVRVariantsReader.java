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

package org.cbioportal.cmo.pipelines.cvr.variants;

import java.util.*;
import org.cbioportal.cmo.pipelines.cvr.model.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author heinsz
 */

public class CVRVariantsReader implements ItemStreamReader<CVRVariants> {

    @Value("#{jobParameters[sessionId]}")
    private String sessionId;

    @Value("${dmp.server_name}")
    private String dmpServerName;

    @Value("${dmp.tokens.retrieve_variants}")
    private String dmpRetreiveVariants;

    private List<CVRVariants> cvrVariants = new ArrayList<CVRVariants>();

    // Calls cbio_retrieve_variants against CVR web service
    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        String dmpUrl = dmpServerName + dmpRetreiveVariants + "/" + sessionId + "/0";
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();
        ResponseEntity<CVRVariants> responseEntity = restTemplate.exchange(dmpUrl, HttpMethod.GET, requestEntity, CVRVariants.class);
        cvrVariants.add(responseEntity.getBody());
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    // The cvrVariants list will never have more than 1 CVRVariants object in it to process
    @Override
    public CVRVariants read() throws Exception {
        if (!cvrVariants.isEmpty()) {
            return cvrVariants.remove(0);
        }
        return null;
    }

    private HttpEntity getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return new HttpEntity<Object>(headers);
    }
}
