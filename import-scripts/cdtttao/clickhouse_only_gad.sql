INSERT INTO TABLE genetic_alteration_derived
SELECT
    sample_unique_id,
    cancer_study_identifier,
    hugo_gene_symbol,
    replaceOne(stable_id, concat(sd.cancer_study_identifier, '_'), '') as profile_type,
    alteration_value
FROM
    (SELECT
         sample_id,
         hugo_gene_symbol,
         stable_id,
         alteration_value
    FROM
        (SELECT
            g.hugo_gene_symbol AS hugo_gene_symbol,
            gp.stable_id as stable_id,
            arrayMap(x -> (x = '' ? NULL : x), splitByString(',', assumeNotNull(substring(ga.values, 1, -1)))) AS alteration_value,
            arrayMap(x -> (x = '' ? NULL : toInt32(x)), splitByString(',', assumeNotNull(substring(gps.ordered_sample_list, 1, -1)))) AS sample_id
        FROM
            genetic_alteration ga
            JOIN genetic_profile gp ON ga.genetic_profile_id=gp.genetic_profile_id
            JOIN genetic_profile_samples gps ON gp.genetic_profile_id = gps.genetic_profile_id
            JOIN gene g ON ga.genetic_entity_id = g.genetic_entity_id
        WHERE
             gp.genetic_alteration_type NOT IN ('GENERIC_ASSAY', 'MUTATION_EXTENDED', 'MUTATION_UNCALLED', 'STRUCTURAL_VARIANT'))
            ARRAY JOIN alteration_value, sample_id
    WHERE alteration_value != 'NA') AS subquery
        JOIN sample_derived sd ON sd.internal_id = subquery.sample_id;

OPTIMIZE TABLE genetic_alteration_derived;
