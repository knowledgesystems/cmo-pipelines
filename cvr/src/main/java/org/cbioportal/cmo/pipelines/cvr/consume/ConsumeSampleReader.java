/*
 * Copyright (c) 2017 Memorial Sloan-Kettering Cancer Center.
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

package org.cbioportal.cmo.pipelines.cvr.consume;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Deque;
import org.apache.log4j.Logger;
import org.cbioportal.cmo.pipelines.cvr.CVRUtilities;
import org.cbioportal.cmo.pipelines.cvr.model.CVRData;
import org.cbioportal.cmo.pipelines.cvr.model.CVRMergedResult;
import org.cbioportal.cmo.pipelines.cvr.model.GMLData;
import org.cbioportal.cmo.pipelines.cvr.model.GMLResult;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author ochoaa
 */
public class ConsumeSampleReader implements ItemStreamReader<String> {

    @Value("#{jobParameters[jsonFilename]}")
    private String jsonFilename;

    @Value("#{jobParameters[gmlMode]}")
    private Boolean gmlMode;

    @Autowired
    public CVRUtilities cvrUtilities;

    private Deque<String> cvrSampleList = new LinkedList<>();

    Logger log = Logger.getLogger(ConsumeSampleReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {

        // load cvr data from cvr_data.json file
        File jsonFile = new File(jsonFilename);
        if (gmlMode) {
            this.cvrSampleList = loadSamplesFromGmlJson(jsonFile);
        }
        else {
            this.cvrSampleList = loadSamplesFromJson(jsonFile);
        }
    }

    private Deque<String> loadSamplesFromJson(File cvrFile) {
        CVRData cvrData = new CVRData();
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }
        // add samples to list
        Deque<String> sampleList = new LinkedList<>();
        for (CVRMergedResult result : cvrData.getResults()) {
            sampleList.add(result.getMetaData().getDmpSampleId());
        }
        return sampleList;
    }

    private Deque<String> loadSamplesFromGmlJson(File cvrGmlFile) {
        GMLData gmlData = new GMLData();
        try {
            gmlData = cvrUtilities.readGMLJson(cvrGmlFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrGmlFile);
            throw new ItemStreamException(e);
        }
        Deque<String> sampleList = new LinkedList<>();
        for (GMLResult result : gmlData.getResults()) {
            sampleList.add(result.getMetaData().getDmpSampleId());
        }
       return sampleList;
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!cvrSampleList.isEmpty()) {
            return cvrSampleList.pollFirst();
        }
        return null;
    }

}
