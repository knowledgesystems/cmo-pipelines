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

package org.cbioportal.cmo.pipelines.gdd;

import org.cbioportal.cmo.pipelines.gdd.model.*;

import org.springframework.http.*;
import org.springframework.batch.item.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
/**
 * @author Benjamin Gross
 */
public class GDDClassifierReader implements ItemStreamReader<GDDResult>
{
    @Value("${gdd.url}")
    private String gddURL;

    @Value("#{jobParameters[maf]}")
    private String maf;

    @Value("#{jobParameters[cna]}")
    private String cna;

    @Value("#{jobParameters[seg]}")
    private String seg;

    @Value("#{jobParameters[sv]}")
    private String sv;

    @Value("#{jobParameters[clinical]}")
    private String clinical;

    private List<GDDResult> results;
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException
    {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity();  
        ResponseEntity<GDDClassifier> responseEntity =
            restTemplate.exchange(gddURL, HttpMethod.POST, requestEntity, GDDClassifier.class);  
        this.results = responseEntity.getBody().getResult();
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> getRequestEntity()
    {
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("maf", new FileSystemResource(maf));
        map.add("cna", new FileSystemResource(cna));
        map.add("seg", new FileSystemResource(seg));
        map.add("sv", new FileSystemResource(sv));
        map.add("clinical", new FileSystemResource(clinical));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<LinkedMultiValueMap<String, Object>>(map, headers);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public GDDResult read() throws Exception
    {
        // necessary until bug that generates duplicate results in classifier script "generate_feature_table.R" gets fixed
        if (!results.isEmpty() && results.get(0).getSampleId() != null) {            
            return results.remove(0);            
        }
        return null;
    } 
}
