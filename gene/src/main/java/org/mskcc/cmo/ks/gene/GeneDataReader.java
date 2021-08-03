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

package org.mskcc.cmo.ks.gene;

import org.mskcc.cmo.ks.gene.model.Gene;
import org.mskcc.cmo.ks.gene.model.GeneAlias;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.annotation.Resource;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.*;
import org.springframework.batch.item.file.mapping.*;
import org.springframework.batch.item.file.transform.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

/**
 *
 * @author ochoaa
 */
public class GeneDataReader implements ItemStreamReader<Gene> {

    private final String GENE_ID_COLUMN = "GeneID";
    private final String SYMBOL_COLUMN = "Symbol";
    private final String MAIN_SYMBOL_COLUMN = "Symbol_from_nomenclature_authority"; // default to this value if not '-'
    private final String SYNONYMS_COLUMN = "Synonyms"; // also locus_tag is used for aliases
    private final String LOCUS_TAG_COLUMN = "LocusTag";
    private final String TYPE_OF_GENE_COLUMN = "type_of_gene";
    private final String CYTOBAND_COLUMN = "map_location";
    private final String MIR_GENE_NAME_PATTERN = "MIR";
    private final String PROTEIN_CODING_GENE_TYPE = "protein-coding";

    private final Integer GENETIC_REGION_INDEX = 2;
    private final Integer GENETIC_INFORMATION_INDEX = 8;
    private final Integer GENE_START_POS_INDEX = 3;
    private final Integer GENE_END_POS_INDEX = 4;


    private final Pattern ENTREZ_ID_PATTERN = Pattern.compile(".*GeneID:([0-9]*).*");

    @Value("#{jobParameters[geneDataFileName]}")
    private String geneDataFileName;

    @Value("#{jobParameters[geneLengthDataFileName]}")
    private String geneLengthDataFileName;

    @Resource(name="ambiguousGenesMap")
    private Map<Integer, Gene> ambiguousGenesMap;

    private  Map<Integer, Integer> geneLengthDataMap = new HashMap<>();
    private List<Gene> geneRecords = new ArrayList();

    private final static Logger LOG = Logger.getLogger(GeneDataReader.class);

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            this.geneLengthDataMap = loadGeneLengthData();
        } catch (Exception ex) {
            LOG.error("Error loading gene length data from: " + geneLengthDataFileName);
            throw new ItemStreamException(ex);
        }
        try {
            this.geneRecords = loadHumanGeneData();
        } catch (Exception ex) {
            LOG.error("Error loading data from: " + geneDataFileName);
            throw new ItemStreamException(ex);
        }
        executionContext.put("ambiguousGenesToReport", ambiguousGenesMap);
    }

    /**
     * Loads human gene data from Homo_sapiens.gene_info.
     *
     * @return List<Gene>
     * @throws FileNotFoundException
     * @throws ItemStreamException
     */
    private List<Gene> loadHumanGeneData() throws Exception {
        LOG.info("Loading data from: " + geneDataFileName);
        File geneInfoFile = new File(geneDataFileName);
        // init line mapper and field tokenizer
        final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer(DelimitedLineTokenizer.DELIMITER_TAB);
        DefaultLineMapper<Properties> lineMapper = new DefaultLineMapper();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper((FieldSet fs) -> fs.getProperties());

        // set reader resources and load data
        FlatFileItemReader<Properties> reader = new FlatFileItemReader();
        reader.setResource(new FileSystemResource(geneInfoFile));
        reader.setLineMapper(lineMapper);
        reader.setLinesToSkip(1);
        reader.setSkippedLinesCallback((String line) -> {
            tokenizer.setNames(line.split(DelimitedLineTokenizer.DELIMITER_TAB));
        });
        reader.open(new ExecutionContext());

        List<Gene> geneFileRecords = new ArrayList();
        Properties record;
        while ((record = reader.read()) != null) {
            Integer entrezGeneId = Integer.valueOf(record.getProperty(GENE_ID_COLUMN));
            String hugoGeneSymbol = record.getProperty(MAIN_SYMBOL_COLUMN).equals("-") ? record.getProperty(SYMBOL_COLUMN) : record.getProperty(MAIN_SYMBOL_COLUMN);
            String type = record.getProperty(TYPE_OF_GENE_COLUMN);
            String cytoband = record.getProperty(CYTOBAND_COLUMN);

            // get synonyms from 'synonyms' column and 'locus tag' column, then construct list of GeneAlias objects
            List<String> synonyms = new ArrayList();
            if (!record.getProperty(SYNONYMS_COLUMN).equals("-")) {
                synonyms.addAll(Arrays.asList(record.getProperty(SYNONYMS_COLUMN).split("[\\|\\;]")));
            }
            if (!record.getProperty(LOCUS_TAG_COLUMN).equals("-")) {
                synonyms.add(record.getProperty(LOCUS_TAG_COLUMN));
            }
            // skip record if gene is a microRNA
            if (isMirGeneRecord(hugoGeneSymbol, synonyms, type)) {
                continue;
            }
            Set<GeneAlias> aliases = new HashSet<>();
            for (String alias : synonyms) {
                aliases.add(new GeneAlias(entrezGeneId, alias.trim().toUpperCase()));
            }
            geneFileRecords.add(new Gene(entrezGeneId, hugoGeneSymbol.toUpperCase(), type, cytoband, geneLengthDataMap.get(entrezGeneId), aliases));
            // remove gene record from ambiguous gene map if exists (indicating that it's a valid record in NCBI gene file)
            // remaining ambiguous genes in this map will be reported to data curation team as likely discontinued records
            // that should be removed from the database
            ambiguousGenesMap.remove(entrezGeneId);
        }
        reader.close();

        return geneFileRecords;
    }

    /**
     * Checks whether record is a microRNA based on primary hugo symbol and gene synonyms.
     * @param hugoGeneSymbol
     * @param synonyms
     * @return
     */
    private boolean isMirGeneRecord(String hugoGeneSymbol, List<String> synonyms, String type) {
        // don't filter out records where gene type is "protein-coding",
        if (type.equalsIgnoreCase(PROTEIN_CODING_GENE_TYPE)) {
            return Boolean.FALSE;
        }
        boolean isMirGene = (hugoGeneSymbol.toUpperCase().startsWith(MIR_GENE_NAME_PATTERN));
        // check synonyms if hugo gene symbol doesn't match MIR_GENE_NAME_PATTERN
        if (!isMirGene) {
            for (String synonym : synonyms) {
                if (synonym.toUpperCase().startsWith(MIR_GENE_NAME_PATTERN)) {
                    isMirGene = Boolean.TRUE;
                    break;
                }
            }
        }
        return isMirGene;
    }

    /**
     * Load and calculate gene length information from GFF file.
     * This file does not contain a header - the following is the relevant information to extract:
     *      col 3: genetic region (i.e., exon, CDS, gene, mRNA, transcript,
     *      col 4: gene start position
     *      col 5: gene end position
     *      col 9: ';'-delimited genetic information, only the 'GeneID' (entrez gene id) will be extracted
     *          ex:
     *              ID=id1;Parent=rna0;Dbxref=GeneID:100287102,Genbank:NR_046018.2,HGNC:HGNC:37102;gbkey=misc_RNA;gene=DDX11L1;product=DEAD/H-box helicase 11 like 1;transcript_id=NR_046018.2
     *
     * The gene length is calculated by calculating the difference between the max gene position found and min gene position found.
     *
     * @return Map<Integer, Integer>
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Map<Integer, Integer> loadGeneLengthData() throws Exception {
        LOG.info("Loading data from: " + geneLengthDataFileName);
        Map<Integer, Set<Integer>> genePositionData = new HashMap<>();
        BufferedReader buf = new BufferedReader(new FileReader(new File(geneLengthDataFileName)));
        String line;
        while ((line = buf.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t");
            if (!parts[GENETIC_REGION_INDEX].equalsIgnoreCase("exon") && !parts[GENETIC_REGION_INDEX].equalsIgnoreCase("cds")) {
                continue;
            }
            // extract entrez gene id from genetic information
            Matcher matcher = ENTREZ_ID_PATTERN.matcher(parts[GENETIC_INFORMATION_INDEX]);
            Integer entrezGeneId = matcher.find() ? Integer.valueOf(matcher.group(1)) : null;

            // make sure entrez gene id was extracted successfully, throw exception if value couldn't be parsed
            if (entrezGeneId == null) {
                LOG.error("Could not extract entrez gene id from row: \n\t" + line);
                throw new ItemStreamException("Check logic for extracting entrez gene id from gene length data file.");
            }
            // update gene position map with exon/cds position data
            Set<Integer> positions = genePositionData.getOrDefault(entrezGeneId, new HashSet<>());
            positions.addAll(Arrays.asList(new Integer[]{Integer.valueOf(parts[GENE_START_POS_INDEX]), Integer.valueOf(parts[GENE_END_POS_INDEX])}));
            genePositionData.put(entrezGeneId, positions);
        }
        buf.close();

        // get the gene length by calculating the difference between the max gene position and min gene position
        Map<Integer, Integer> geneLengthDataMap = new HashMap<>();
        for (Integer entrezGeneId : genePositionData.keySet()) {
            Integer length = Collections.max(genePositionData.get(entrezGeneId)) - Collections.min(genePositionData.get(entrezGeneId)) + 1;
            geneLengthDataMap.put(entrezGeneId, length);
        }
        return geneLengthDataMap;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {}

    @Override
    public void close() throws ItemStreamException {}

    @Override
    public Gene read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!geneRecords.isEmpty()) {
            return geneRecords.remove(0);
        }
        return null;
    }

}
