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

package org.cbioportal.cmo.pipelines.cvr.cna;

import org.cbioportal.cmo.pipelines.cvr.model.composite.CompositeCnaRecord;
import org.cbioportal.cmo.pipelines.cvr.*;
import org.cbioportal.cmo.pipelines.cvr.model.*;

import java.io.*;
import java.util.*;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.*;

/**
 *
 * @author heinsz
 */
public class CVRCnaDataReader implements ItemStreamReader<CompositeCnaRecord>{

    @Value("#{jobParameters[stagingDirectory]}")
    private String stagingDirectory;

    @Autowired
    public CVRUtilities cvrUtilities;

    @Autowired
    public CvrSampleListUtil cvrSampleListUtil;

    private MultiKeyMap cnaMap = new MultiKeyMap();
    private Set<String> genes = new HashSet<>();
    private Set<String> samples = new HashSet<>();
    private Set<String> newSamples = new HashSet<>();

    private List<CompositeCnaRecord> cnaRecords = new ArrayList();

    Logger log = Logger.getLogger(CVRCnaDataReader.class);

    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        CVRData cvrData = new CVRData();
        // load cvr data from cvr_data.json file
        File cvrFile = new File(stagingDirectory, cvrUtilities.CVR_FILE);
        try {
            cvrData = cvrUtilities.readJson(cvrFile);
        } catch (IOException e) {
            log.error("Error reading file: " + cvrFile.getName());
            throw new ItemStreamException(e);
        }

        for (CVRMergedResult result : cvrData.getResults()) {
            String sampleId = result.getMetaData().getDmpSampleId();
            if (!cvrSampleListUtil.getPortalSamples().contains(sampleId)) {
                cvrSampleListUtil.addSampleRemoved(sampleId);
                continue;
            }
            samples.add(sampleId);
            newSamples.add(sampleId);
            List<CVRCnvVariant> variants = result.getCnvVariants();
            for (CVRCnvVariant variant : variants) {
                if (variant.getClinicalSignedOut().equals("1")) {
                    genes.add(variant.getGeneId());
                    cnaMap.put(variant.getGeneId(), sampleId, resolveGeneFoldChange(variant.getGeneFoldChange()));
                }
            }
            List<CVRCnvIntragenicVariant> intragenicVariants = result.getCnvIntragenicVariants();
            for (CVRCnvIntragenicVariant variant : intragenicVariants) {
                genes.add(variant.getGeneId());
                cnaMap.put(variant.getGeneId(), sampleId, "-1.5");
            }
        }
        // load gene panel data
        File genePanelFile = new File(stagingDirectory, cvrUtilities.GENE_PANEL);
        if (!genePanelFile.exists()) {
            log.error("Cannot find gene panel file: " + genePanelFile.getName());
        }
        else {
            try {
                // load gene symbols from gene panel
                cvrUtilities.importGenePanel(genePanelFile.getCanonicalPath());
                genes.addAll(cvrUtilities.getGeneSymbols());
            } catch (Exception e) {
                log.error("Error loading gene panel data from: " + genePanelFile.getName());
                throw new ItemStreamException(e);
            }
        }

        // CNA data is processed on a gene per row basis, making it very different from the other data types.
        // This also means we can't exactly model it with a java class easily. For now, process CNA data as strings.
        processExistingCnaFile();
        makeNewRecordsList();
        makeRecordsList();
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
    }

    @Override
    public CompositeCnaRecord read() throws Exception {
        if (!cnaRecords.isEmpty()) {
            return cnaRecords.remove(0);
        }
        return null;
    }

    private String resolveGeneFoldChange(Double geneFoldChange) {
        if (geneFoldChange > 0) {
            return "2";
        }
        if (geneFoldChange < 0) {
            return "-2";
        }
        return "0";
    }

    private void makeRecordsList() {
        cnaRecords.add(new CompositeCnaRecord("",cvrUtilities.CNA_HEADER_HUGO_SYMBOL + "\t" + StringUtils.join(samples,"\t")));
        for (String gene : genes) {
            String line = gene;
            for (String sample : samples) {
                String cnaValue = "0";
                Object value = cnaMap.get(gene, sample);
                if (value != null) {
                    cnaValue = value.toString();
                }
                line = line + "\t" + cnaValue;
            }
            cnaRecords.add(new CompositeCnaRecord("", line));
        }
    }

    private void makeNewRecordsList() {
        cnaRecords.add(new CompositeCnaRecord(cvrUtilities.CNA_HEADER_HUGO_SYMBOL + "\t" + StringUtils.join(newSamples,"\t"), ""));
        for (String gene : genes) {
            String line = gene;
            for (String sample : newSamples) {
                String cnaValue = "0";
                Object value = cnaMap.get(gene, sample);
                if (value != null) {
                    cnaValue = value.toString();
                }
                line = line + "\t" + cnaValue;
            }
            cnaRecords.add(new CompositeCnaRecord(line, ""));
        }
    }

    private void processExistingCnaFile() {
        File cnaFile = new File(stagingDirectory, cvrUtilities.CNA_FILE);
        if (!cnaFile.exists()) {
            log.info("CNA file does not exist yet: " + cnaFile.getName());
            return;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(cnaFile));
            List<String> header = Arrays.asList(reader.readLine().split("\t"));
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> data = Arrays.asList(line.split("\t"));
                try {
                    for (int i = 1; i < header.size(); i++) {
                        if (!cvrSampleListUtil.getPortalSamples().contains(header.get(i))) {
                            cvrSampleListUtil.addSampleRemoved(header.get(i));
                            continue;
                        }
                        if (!cvrSampleListUtil.getNewDmpSamples().contains(header.get(i))) {
                            samples.add(header.get(i));
                            genes.add(data.get(0));
                            cnaMap.put(data.get(0), header.get(i), data.get(i));
                        }
                    }
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    String message = "# Fields in row " + data.get(0) + "=" + data.size() +" does not match # fields in header: " + header.size();
                    log.error(message);
                    throw new ItemStreamException(e);
                }

            }
        }
        catch (Exception e) {
            log.error("Error loading data from: " + cnaFile.getName());
        }
    }
}
