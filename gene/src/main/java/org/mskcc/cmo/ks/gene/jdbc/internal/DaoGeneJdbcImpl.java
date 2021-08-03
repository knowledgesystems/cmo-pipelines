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

package org.mskcc.cmo.ks.gene.jdbc.internal;

import org.mskcc.cmo.ks.gene.jdbc.DaoGeneJdbc;
import org.mskcc.cmo.ks.gene.model.Gene;
import org.mskcc.cmo.ks.gene.model.GeneAlias;

import java.sql.*;
import java.util.*;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.dao.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ochoaa
 */
@Repository
public class DaoGeneJdbcImpl implements DaoGeneJdbc {

    @Resource(name="namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    public DaoGeneJdbcImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Gene addGene(Gene gene) throws DataAccessException {
        String SQL = "INSERT INTO gene " +
                "(entrez_gene_id, hugo_gene_symbol, genetic_entity_id, type, cytoband, length) " +
                "VALUES(:entrez_gene_id, :hugo_gene_symbol, :genetic_entity_id, :type, :cytoband, :length)";
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("entrez_gene_id", gene.getEntrezGeneId());
        paramMap.addValue("hugo_gene_symbol", gene.getHugoGeneSymbol().toUpperCase());
        paramMap.addValue("genetic_entity_id", gene.getGeneticEntityId());
        paramMap.addValue("type", gene.getType());
        paramMap.addValue("cytoband", gene.getCytoband());
        paramMap.addValue("length", gene.getLength());
        namedParameterJdbcTemplate.update(SQL, paramMap);

        // add gene aliases if not empty
        if (!gene.getAliases().isEmpty()) {
            addGeneAliases(gene);
        }

        return gene;
    }

    @Override
    public void addGeneAliases(Gene gene) throws DataAccessException {
        String SQL = "INSERT INTO gene_alias " +
                "(entrez_gene_id, gene_alias) " +
                "VALUES(:entrez_gene_id, :gene_alias)";
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("entrez_gene_id", gene.getEntrezGeneId());

        for (GeneAlias alias : gene.getAliases()) {
            paramMap.addValue("gene_alias", alias.getGeneAlias());
            namedParameterJdbcTemplate.update(SQL, paramMap);
        }
    }

    @Override
    public Integer addGeneGeneticEntity() throws DataAccessException {
        String SQL = "INSERT INTO genetic_entity (entity_type) VALUES(:val)";
        SqlParameterSource paramMap = new MapSqlParameterSource("val", "GENE");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(SQL, paramMap, keyHolder, new String[] {"ID"});
        Integer geneticEntityId =Integer.valueOf( keyHolder.getKeyList().get(0).get("GENERATED_KEY").toString());
        return geneticEntityId;
    }

    @Override
    public void updateGene(Gene gene) throws DataAccessException {
        String SQL = "UPDATE gene " +
                "SET hugo_gene_symbol = :hugo_gene_symbol, type = :type, cytoband = :cytoband, length = :length " +
                "WHERE entrez_gene_id = :entrez_gene_id";
        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("hugo_gene_symbol", gene.getHugoGeneSymbol());
        paramMap.addValue("type", gene.getType());
        paramMap.addValue("cytoband", gene.getCytoband());
        paramMap.addValue("length", gene.getLength());
        paramMap.addValue("entrez_gene_id", gene.getEntrezGeneId());
        namedParameterJdbcTemplate.update(SQL, paramMap);

        // only new gene aliases should be in list, shouldn't have duplicates after executing insert into 'gene_alias' table
        if (!gene.getAliases().isEmpty()) {
            addGeneAliases(gene);
        }
    }

    @Override
    public Gene getGene(Integer entrezGeneId) {
        String SQL = "SELECT " +
                    "gene.entrez_gene_id AS 'gene.entrezGeneId', " +
                    "gene.hugo_gene_symbol AS 'gene.hugoGeneSymbol', " +
                    "gene.genetic_entity_id as 'gene.geneticEntityId', " +
                    "gene.type AS 'gene.type', " +
                    "gene.cytoband AS 'gene.cytoband', " +
                    "gene.length AS 'gene.length' " +
                    "FROM gene " +
                    "WHERE gene.entrez_gene_id = :entrez_gene_id ";
        SqlParameterSource paramMap = new MapSqlParameterSource("entrez_gene_id", entrezGeneId);

        Gene gene = null;
        try {
            gene = (Gene) namedParameterJdbcTemplate.queryForObject(SQL, paramMap, geneRowMapper());
        }
        catch (EmptyResultDataAccessException ex) {}

        return gene;
    }

    @Override
    public Set<GeneAlias> getGeneAliases(Integer entrezGeneId) throws DataAccessException {
        String SQL = "SELECT DISTINCT gene_alias from gene_alias WHERE entrez_gene_id = :entrez_gene_id";
        SqlParameterSource paramMap = new MapSqlParameterSource("entrez_gene_id", entrezGeneId);
        List<String> aliases = namedParameterJdbcTemplate.queryForList(SQL, paramMap, String.class);

        Set<GeneAlias> geneAliases = new HashSet<>();
        for (String alias : aliases) {
            if (alias.contains(";")) {
                continue;
            }
            geneAliases.add(new GeneAlias(entrezGeneId, alias.toUpperCase()));
        }
        return geneAliases;
    }

    @Override
    public List<Gene> getAllGenes() {
        String SQL = "SELECT " +
                    "gene.entrez_gene_id AS 'gene.entrezGeneId', " +
                    "gene.hugo_gene_symbol AS 'gene.hugoGeneSymbol', " +
                    "gene.genetic_entity_id AS 'gene.geneticEntityId', " +
                    "gene.type AS 'gene.type', " +
                    "gene.cytoband AS 'gene.cytoband', " +
                    "gene.length AS 'gene.length' " +
                    "FROM gene " +
                    "GROUP BY gene.entrez_gene_id";

        List<Gene> genes = new ArrayList();
        try {
            genes = (List<Gene>) namedParameterJdbcTemplate.query(SQL, geneRowMapper());
        }
        catch (EmptyResultDataAccessException ex) {}

        return genes;
    }

    @Override
    public List<String> getAllAmbiguousHugoGeneSymbols() {
        String SQL = "SELECT DISTINCT gene.hugo_gene_symbol FROM gene GROUP BY gene.hugo_gene_symbol HAVING COUNT(*) > 1";
        List<String> ambiguousGeneSymbols = namedParameterJdbcTemplate.query(SQL, (ResultSet rs, int i) -> rs.getString(1));
        return ambiguousGeneSymbols;
    }

    @Override
    public List<Gene> getAllAmbiguousGenes() {
        String SQL = "SELECT " +
                    "gene.entrez_gene_id AS 'gene.entrezGeneId', " +
                    "gene.hugo_gene_symbol AS 'gene.hugoGeneSymbol', " +
                    "gene.genetic_entity_id AS 'gene.geneticEntityId', " +
                    "gene.type AS 'gene.type', " +
                    "gene.cytoband AS 'gene.cytoband', " +
                    "gene.length AS 'gene.length' " +
                    "FROM gene " +
                    "WHERE gene.hugo_gene_symbol IN (:ambiguous_genes_list)";
        SqlParameterSource paramMap = new MapSqlParameterSource("ambiguous_genes_list", getAllAmbiguousHugoGeneSymbols());

        List<Gene> ambiguousGenes = new ArrayList();
        try {
            ambiguousGenes = (List<Gene>) namedParameterJdbcTemplate.query(SQL, paramMap, geneRowMapper());
        }
        catch (EmptyResultDataAccessException ex) {}

        return ambiguousGenes;
    }

    @Override
    public void deleteGeneGeneticEntity(Integer geneticEntityId) throws DataAccessException {
        String SQL = "DELETE FROM genetic_entity WHERE id = :genetic_entity_id";
        SqlParameterSource paramMap = new MapSqlParameterSource("genetic_entity_id", geneticEntityId);
        namedParameterJdbcTemplate.update(SQL, paramMap);
    }

    @Override
    public Integer batchInsertGene(List<Gene> genes) throws Exception {
        String SQL = "INSERT INTO gene " +
                "(entrez_gene_id, hugo_gene_symbol, genetic_entity_id, type, cytoband, length) " +
                "VALUES(:entrezGeneId, :hugoGeneSymbol, :geneticEntityId, :type, :cytoband, :length)";
        int[] rows = namedParameterJdbcTemplate.batchUpdate(SQL, SqlParameterSourceUtils.createBatch(genes.toArray(new Gene[genes.size()])));
        return rows != null ? Arrays.stream(rows).sum() : 0;
    }

    @Override
    public Integer batchUpdateGene(List<Gene> genes) throws Exception {
        String SQL = "UPDATE gene " +
                "SET hugo_gene_symbol = :hugoGeneSymbol, type = :type, cytoband = :cytoband, length = :length " +
                "WHERE entrez_gene_id = :entrezGeneId";
        int[] rows = namedParameterJdbcTemplate.batchUpdate(SQL, SqlParameterSourceUtils.createBatch(genes.toArray(new Gene[genes.size()])));
        return rows != null ? Arrays.stream(rows).sum() : 0;
    }

    @Override
    public Integer batchInsertGeneAlias(List<GeneAlias> geneAliases) throws Exception {
        String SQL = "INSERT INTO gene_alias " +
                "(entrez_gene_id, gene_alias) " +
                "VALUES(:entrezGeneId, :geneAlias)";
        int[] rows = namedParameterJdbcTemplate.batchUpdate(SQL, SqlParameterSourceUtils.createBatch(geneAliases.toArray(new GeneAlias[geneAliases.size()])));
        return rows != null ? Arrays.stream(rows).sum() : 0;
    }

    /**
     * Gene row mapper.
     * @return
     */
    private RowMapper geneRowMapper() {
        return (RowMapper) (ResultSet rs, int i) -> {
            Map<String, BeanMap> beans_by_name = new HashMap();
            beans_by_name.put("gene", BeanMap.create(new Gene()));

            // go through each column from sql query and set respective bean properties accordingly
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            for (int index=1; index<=resultSetMetaData.getColumnCount(); index++) {
                // get the table name and the field name for the bean
                String table = resultSetMetaData.getColumnLabel(index).split("\\.")[0];
                String field = resultSetMetaData.getColumnLabel(index).split("\\.")[1];

                BeanMap beanMap = beans_by_name.get(table);
                if (rs.getObject(index) != null) {
                    beanMap.put(field, rs.getObject(index));
                }
            }
            Gene gene = (Gene) beans_by_name.get("gene").getBean();
            gene.setAliases(getGeneAliases(gene.getEntrezGeneId()));

            return gene;
        };
    }

}
