import sys
import os
import optparse
import datetime
import MySQLdb
import traceback
# ------------------------------------------------------------------------------
# globals
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# fields in portal.properties
CGDS_DATABASE_HOST = 'db.host'
CGDS_DATABASE_NAME = 'db.portal_db_name'
CGDS_DATABASE_USER = 'db.user'
CGDS_DATABASE_PW = 'db.password'
MYSQL_PORT = 3306

GENETIC_PROFILES = {}
CANCER_STUDIES = {}
SAMPLES = {}
GENE_PANELS = {}
GENES = {}
REFERENCE_GENOMES = {}

MIRNA_ORPHAN_GENE_PANELS = set()
MIRNA_GENE_ALIASES_OKAY_TO_REMOVE = set()
MIRNA_GENES_OKAY_TO_REMOVE = set()

MIRNA_COMPILED_DB_REFERENCE_SETS_MAP = {}
MIRNA_GENE_DB_RECORD_REFERENCES = {}
# key  => entrez_gene_id
# value => {
#         <db_table_name> : {
#             'CANCER_STUDIES': set(),
#             'GENETIC_PROFILES': set(),
#             'GENESETS': set(),
#               etc....
#         }
#     }
MIRNA_GENE_DB_TABLE_NAMES = [
    'mutation_event',
    'mutation_count_by_keyword',
    'cna_event',
    'genetic_alteration',
    'gistic_to_gene',
    'mut_sig',
    'protein_array_target',
    'gene_panel',
    'geneset',
    'reference_genome',
    'cosmic_mutation',
    'uniprot_id_mapping',
    'sanger_cancer_census']

# tables affected by changes
# mutation via mutation_event
# sample_cna_event via cna_event
# gistic via gistic_to_gene
# protein_array_data via protein_array_target
# gene_panel_list via gene_panel
# geneset_gene via geneset
# reference_genome_gene via reference_genome

MIRNA_GENE_DB_REFERENCE_TYPES = [
    'CANCER_STUDIES',
    'GENETIC_PROFILES',
    'GENE_PANELS',
    'GENESETS',
    'REFERENCE_GENOMES',
    'COSMIC_MUTATIONS',
    'UNIPROT_ID_MAPPING',
    'SANGER_CANCER_CENSUSES'
]

MIRNA_GENE_FILE_HEADER = ['HUGO_GENE_SYMBOL', 'ENTREZ_GENE_ID', 'IS_ALIAS']

# ------------------------------------------------------------------------------
# class defintions
class PortalProperties(object):
    def __init__(self, properties_filename):
        properties = self.parse_properties(properties_filename)
        self.cgds_database_host = properties[CGDS_DATABASE_HOST]
        self.cgds_database_name = properties[CGDS_DATABASE_NAME]
        self.cgds_database_user = properties[CGDS_DATABASE_USER]
        self.cgds_database_pw = properties[CGDS_DATABASE_PW]

    def parse_properties(self, properties_filename):
        properties = {}
        with open(properties_filename, 'rU') as properties_file:
            for line in properties_file:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                property = map(str.strip, line.split('='))
                if len(property) != 2:
                    continue
                properties[property[0]] = property[1]
        # error check
        if (CGDS_DATABASE_HOST not in properties or len(properties[CGDS_DATABASE_HOST]) == 0 or
            CGDS_DATABASE_NAME not in properties or len(properties[CGDS_DATABASE_NAME]) == 0 or
            CGDS_DATABASE_USER not in properties or len(properties[CGDS_DATABASE_USER]) == 0 or
            CGDS_DATABASE_PW not in properties or len(properties[CGDS_DATABASE_PW]) == 0):
            print properties
            print >> ERROR_FILE, 'Missing one or more required properties, please check property file'
            sys.exit(2)
        return properties

class Gene(object):
    def __init__(self, hugo_gene_symbol, entrez_gene_id, genetic_entity_id, protein_type, is_alias):
        self.hugo_gene_symbol = hugo_gene_symbol
        self.entrez_gene_id = entrez_gene_id
        self.genetic_entity_id = genetic_entity_id
        self.protein_type = protein_type
        self.is_alias = is_alias

class Mutation(object):
    def __init__(self, mutation_event_id, entrez_gene_id, genetic_profile_id, sample_id, cancer_study_id):
        self.mutation_event_id = mutation_event_id
        self.entrez_gene_id = entrez_gene_id
        self.genetic_profile_id = genetic_profile_id
        self.sample_id = sample_id
        self.cancer_study_id = cancer_study_id

class MutationCountByKeyword(object):
    def __init__(self, genetic_profile_id, entrez_gene_id, keyword, cancer_study_id):
        self.genetic_profile_id = genetic_profile_id
        self.entrez_gene_id = entrez_gene_id
        self.keyword = keyword
        self.cancer_study_id = cancer_study_id

class CNAEvent(object):
    def __init__(self, cna_event_id, entrez_gene_id, genetic_profile_id, sample_id, cancer_study_id):
        self.cna_event_id = cna_event_id
        self.entrez_gene_id = entrez_gene_id
        self.genetic_profile_id = genetic_profile_id
        self.sample_id = sample_id
        self.cancer_study_id = cancer_study_id

class GeneticAlteration(object):
    def __init__(self, genetic_entity_id, entrez_gene_id, genetic_profile_id, cancer_study_id, ordered_sample_list):
        self.genetic_entity_id = genetic_entity_id
        self.entrez_gene_id = entrez_gene_id
        self.genetic_profile_id = genetic_profile_id
        self.cancer_study_id = cancer_study_id
        self.ordered_sample_list = ordered_sample_list

class GisticToGene(object):
    def __init__(self, gistic_roi_id, entrez_gene_id, cancer_study_id):
        self.gistic_roi_id = gistic_roi_id
        self.entrez_gene_id = entrez_gene_id
        self.cancer_study_id = cancer_study_id
        self.genetic_profile_id = ''

class MutSig(object):
    def __init__(self, entrez_gene_id, cancer_study_id):
        self.entrez_gene_id = entrez_gene_id
        self.cancer_study_id = cancer_study_id
        self.genetic_profile_id = ''

class ProteinArrayData(object):
    def __init__(self, protein_array_id, entrez_gene_id, cancer_study_id, sample_id):
        self.protein_array_id = protein_array_id
        self.entrez_gene_id = entrez_gene_id
        self.cancer_study_id = cancer_study_id
        self.sample_id = sample_id
        self.genetic_profile_id = ''

class GenePanel(object):
    def __init__(self, gene_panel_id, entrez_gene_id, genetic_profile_id, sample_id, cancer_study_id):
        self.gene_panel_id = gene_panel_id
        self.entrez_gene_id = entrez_gene_id
        self.genetic_profile_id = genetic_profile_id
        self.sample_id = sample_id
        self.cancer_study_id = cancer_study_id

class Geneset(object):
    def __init__(self, geneset_id, entrez_gene_id, genetic_entity_id):
        self.geneset_id = geneset_id
        self.entrez_gene_id = entrez_gene_id
        self.genetic_entity_id = genetic_entity_id

class ReferenceGenome(object):
    def __init__(self, reference_genome_id, entrez_gene_id):
        self.reference_genome_id = reference_genome_id
        self.entrez_gene_id = entrez_gene_id

class CosmicMutation(object):
    def __init__(self, cosmic_mutation_id, entrez_gene_id):
        self.cosmic_mutation_id = cosmic_mutation_id
        self.entrez_gene_id = entrez_gene_id

class UniprotIdMapping(object):
    def __init__(self, uniprot_id, entrez_gene_id, uniprot_accession):
        self.uniprot_id = uniprot_id
        self.entrez_gene_id = entrez_gene_id
        self.uniprot_accession = uniprot_accession

# ------------------------------------------------------------------------------
# db functions
def establish_db_connection(properties):
    '''
        Establishes database connection.
    '''
    try:
        connection = MySQLdb.connect(host=properties.cgds_database_host, port=MYSQL_PORT,
                            user=properties.cgds_database_user,
                            passwd=properties.cgds_database_pw,
                            db=properties.cgds_database_name)
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return connection

def get_db_cursor(db_connection):
    '''
        Return db cursor.
    '''
    if db_connection is not None:
        cursor = db_connection.cursor()
        return cursor
    else:
        print >> OUTPUT_FILE, 'Error connecting to database, exiting'
        sys.exit(2)

def get_mirna_gene_records(cursor):
    '''
        Find records in `gene` where entrez gene id is > 0 and hugo gene symbol like 'MIR*'.
    '''
    sql_statement = 'SELECT hugo_gene_symbol, entrez_gene_id, genetic_entity_id, type FROM gene WHERE (hugo_gene_symbol LIKE "MIR%" OR hugo_gene_symbol LIKE "HSA-MIR%") AND entrez_gene_id > 0'

    mirna_genes = []
    try:
        cursor.execute(sql_statement)
        for row in cursor.fetchall():
            mirna_genes.append(Gene(row[0].upper(), int(row[1]), int(row[2]), row[3].lower(), False))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    print >> OUTPUT_FILE, 'Found ' + str(len(mirna_genes)) + ' gene miRNA records'
    return mirna_genes

def get_mirna_gene_records_by_entrez_ids(cursor, entrez_gene_ids):
    '''
        Given set of entrez gene ids, return matching records found in `gene`.

        Do not filter out any records where hugo gene symbol is not like 'MIR%'. These might have
        records in `gene_alias` which meet the miRNA candidate criteria where hugo symbol like 'MIR%'
        and has a positive entrez gene id.
    '''
    sql_statement = 'SELECT hugo_gene_symbol, entrez_gene_id, genetic_entity_id, type FROM gene WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(entrez_gene_ids))

    mirna_genes = []
    try:
        cursor.execute(sql_statement, tuple(entrez_gene_ids))
        for row in cursor.fetchall():
            mirna_genes.append(Gene(row[0].upper(), int(row[1]), int(row[2]), row[3].lower(), False))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    print >> OUTPUT_FILE, 'Found ' + str(len(mirna_genes)) + ' gene miRNA records'
    return mirna_genes

def get_mirna_gene_alias_records(cursor):
    '''
        Find records in `gene_alias` where entrez gene id is not in the
        set of miRNA records found in primary gene table `gene`.
    '''
    sql_statement = 'SELECT gene_alias.gene_alias, gene_alias.entrez_gene_id, gene.genetic_entity_id, gene.type FROM gene_alias JOIN gene ON gene.entrez_gene_id = gene_alias.entrez_gene_id WHERE (gene_alias.gene_alias LIKE "MIR%" OR gene_alias.gene_alias LIKE "HSA-MIR%") AND gene_alias.entrez_gene_id > 0 AND gene_alias.entrez_gene_id NOT IN (SELECT entrez_gene_id FROM gene WHERE entrez_gene_id > 0 AND (hugo_gene_symbol LIKE "MIR%" OR hugo_gene_symbol LIKE "HSA-MIR%"))'
    mirna_genes = []
    try:
        cursor.execute(sql_statement)
        for row in cursor.fetchall():
            mirna_genes.append(Gene(row[0].upper(), int(row[1]), int(row[2]), row[3].lower(), True))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    print >> OUTPUT_FILE, 'Found ' + str(len(mirna_genes)) + ' gene alias miRNA records'

    # also get miRNA parent records from `gene` table
    mirna_genes.extend(get_mirna_gene_alias_parent_records(cursor, mirna_genes))
    return mirna_genes

def get_mirna_gene_alias_records_by_entrez_gene_ids(cursor, entrez_gene_ids):
    '''
        Given set of entrez gene ids, return matching miRNA candidate records in `gene_alias`.

        Filter out any records that do not meet the miRNA candididate criteria where hugo symbol like 'MIR%'
        and has a positive entrez gene id.

        There's no need to additionally fetch the parent records in `gene` for `gene_alias` records found since
        a set of genes would have already been fetched from `gene` for given set of entrez gene ids supplied.
    '''
    entrez_gene_ids_string = '(' + ','.join(map(str, entrez_gene_ids)) + ')'
    sql_statement = 'SELECT gene_alias.gene_alias, gene_alias.entrez_gene_id, gene.genetic_entity_id, gene.type FROM gene_alias JOIN gene ON gene.entrez_gene_id = gene_alias.entrez_gene_id WHERE (gene_alias.gene_alias LIKE "MIR%" OR gene_alias.gene_alias LIKE "HSA-MIR%") AND gene_alias.entrez_gene_id IN ' + entrez_gene_ids_string

    mirna_genes = []
    try:
        cursor.execute(sql_statement)
        for row in cursor.fetchall():
            mirna_genes.append(Gene(row[0].upper(), int(row[1]), int(row[2]), row[3].lower(), True))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    print >> OUTPUT_FILE, 'Found ' + str(len(mirna_genes)) + ' gene alias miRNA records'
    return mirna_genes

def get_mirna_gene_alias_parent_records(cursor, mirna_gene_aliases):
    '''
        Get the parent genes from `gene` for given miRNA gene alias records.
    '''
    alias_entrez_gene_ids = list(set(map(lambda x: x.entrez_gene_id, mirna_gene_aliases)))
    sql_statement = 'SELECT hugo_gene_symbol, entrez_gene_id, genetic_entity_id, type FROM gene WHERE entrez_gene_id in (%s)' % ','.join(['%s'] * len(alias_entrez_gene_ids))

    mirna_genes = []
    try:
        cursor.execute(sql_statement, tuple(alias_entrez_gene_ids))
        for row in cursor.fetchall():
            mirna_genes.append(Gene(row[0].upper(), int(row[1]), int(row[2]), row[3].lower(), False))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    print >> OUTPUT_FILE, 'Found ' + str(len(mirna_genes)) + ' gene alias parent `gene` records'
    return mirna_genes

def get_field_value_from_table(cursor, table_name, id_col_name, query_field_name, internal_id):
    '''
        Given a table, identifier column name, query field name, and internal id, return the stable id.
    '''
    sql_statement = 'SELECT %s FROM %s WHERE %s = %s' % (id_col_name.upper(), table_name, query_field_name.upper(), internal_id)
    try:
        cursor.execute(sql_statement)
        return cursor.fetchone()[0]
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)

def get_mirna_mutation_references(cursor, mutation_event_ids, entrez_gene_ids):
    '''
            Search for references to `mutation` records linked to `mutation_event` miRNA records.
    '''
    sql_statement = 'SELECT mutation.mutation_event_id, mutation.entrez_gene_id, mutation.genetic_profile_id, mutation.sample_id, genetic_profile.cancer_study_id FROM mutation JOIN genetic_profile ON genetic_profile.genetic_profile_id = mutation.genetic_profile_id WHERE mutation.mutation_event_id IN (%s) AND mutation.entrez_gene_id IN (%s)' % (','.join(['%s'] * len(mutation_event_ids)), ','.join(['%s'] * len(entrez_gene_ids)))

    # initialize mutation event ids w/empty lists to keep track of how many mutation records are associated with them if any at all
    mirna_mutation_events_map = dict(zip(mutation_event_ids, [[] for x in range(len(mutation_event_ids))]))
    ignored_mutation_event_ids = set()
    try:
        query_values = mutation_event_ids[:]
        query_values.extend(entrez_gene_ids)
        cursor.execute(sql_statement, tuple(query_values))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                ignored_mutation_event_ids.add(int(row[0]))
                continue
            mirna_mutation_events_map[int(row[0])].append(Mutation(int(row[0]), int(row[1]), int(row[2]), int(row[3]), int(row[4])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    # filter out mutation events which can be ignored since the primary gene hugo symbol linked to its entrez gene id in `gene`
    # is not a miRNA symbol so there isn't any cleanup to do
    if len(ignored_mutation_event_ids) > 0:
        print >> OUTPUT_FILE, 'Ignoring mutation events linked to at least one non-alias, non-miRNA gene: ' + str(len(ignored_mutation_event_ids)) + ' mutation events...'
        for mutation_event_id in ignored_mutation_event_ids:
            del mirna_mutation_events_map[mutation_event_id]
    return mirna_mutation_events_map

def get_mirna_mutation_event_references(cursor, mirna_gene_map):
    '''
        Search for records in `mutation_event` linked to given set of miRNA records.
    '''
    sql_statement = 'SELECT mutation_event_id FROM mutation WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    mutation_event_ids = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            mutation_event_ids.append(int(row[0]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return mutation_event_ids

def get_mirna_sample_cna_event_references(cursor, cna_event_ids, entrez_gene_ids):
    '''
        Search for references to given set of cna event ids in table `sample_cna_event`.
    '''
    sql_statement = 'SELECT sample_cna_event.cna_event_id, cna_event.entrez_gene_id, sample_cna_event.genetic_profile_id, sample_cna_event.sample_id, genetic_profile.cancer_study_id FROM sample_cna_event JOIN cna_event on cna_event.cna_event_id = sample_cna_event.cna_event_id JOIN genetic_profile ON genetic_profile.genetic_profile_id = sample_cna_event.genetic_profile_id WHERE sample_cna_event.cna_event_id IN (%s) AND cna_event.entrez_gene_id IN (%s)' % (','.join(['%s'] * len(cna_event_ids)), ','.join(['%s'] * len(entrez_gene_ids)))

    # initialize cna event ids w/empty lists to keep track of how many `sample_cna_event` records are associated with them if any at all
    mirna_cna_events_map = dict(zip(cna_event_ids, [[] for x in range(len(cna_event_ids))]))
    ignored_cna_event_ids = set()
    try:
        query_values = cna_event_ids[:]
        query_values.extend(entrez_gene_ids)
        cursor.execute(sql_statement, tuple(query_values))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                ignored_cna_event_ids.add(int(row[0]))
                continue
            mirna_cna_events_map[int(row[0])].append(CNAEvent(int(row[0]), int(row[1]), int(row[2]), int(row[3]), int(row[4])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    # filter out cna events which can be ignored since the primary gene hugo symbol linked to its entrez gene id in `gene`
    # is not a miRNA symbol so there isn't any cleanup to do
    if len(ignored_cna_event_ids) > 0:
        print >> OUTPUT_FILE, 'Ignoring cna events linked to at least one non-alias, non-miRNA gene: ' + str(len(ignored_cna_event_ids)) + ' cna events...'
        for cna_event_id in ignored_cna_event_ids:
            del mirna_cna_events_map[cna_event_id]
    return mirna_cna_events_map

def get_mirna_cna_event_references(cursor, mirna_gene_map):
    '''
        Search for records in `cna_event` linked to given set of miRNA records.
    '''
    sql_statement = 'SELECT cna_event_id FROM cna_event WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    cna_event_ids = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            cna_event_ids.append(int(row[0]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return cna_event_ids

def get_mirna_mutation_count_by_keyword_references(cursor, mirna_gene_map):
    '''
        Search for references to `mutation_count_by_keyword` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT mutation_count_by_keyword.genetic_profile_id, mutation_count_by_keyword.entrez_gene_id, mutation_count_by_keyword.keyword, genetic_profile.cancer_study_id FROM mutation_count_by_keyword JOIN genetic_profile on genetic_profile.genetic_profile_id = mutation_count_by_keyword.genetic_profile_id WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    mutation_count_by_keyword_records = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            mutation_count_by_keyword_records.append(MutationCountByKeyword(int(row[0]), entrez_gene_id, row[2], int(row[3])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return mutation_count_by_keyword_records

def get_mirna_genetic_alteration_references(cursor, mirna_gene_map):
    '''
        Search for references to `genetic_alteration` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT genetic_alteration.genetic_entity_id, gene.entrez_gene_id, genetic_alteration.genetic_profile_id, genetic_profile.cancer_study_id, genetic_alteration.values FROM genetic_alteration JOIN gene on gene.genetic_entity_id = genetic_alteration.genetic_entity_id JOIN genetic_profile ON genetic_profile.genetic_profile_id = genetic_alteration.genetic_profile_id WHERE gene.entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    genetic_alteration_records = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            genetic_alteration_records.append(GeneticAlteration(int(row[0]), int(row[1]), int(row[2]), int(row[3]), row[4]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return genetic_alteration_records

def get_mirna_gistic_to_gene_references(cursor, mirna_gene_map):
    '''
        Search for references to `gistic` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT gistic_roi_id FROM gistic_to_gene WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    gistic_roi_ids = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            gistic_roi_ids.append(int(row[0]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return gistic_roi_ids

def get_mirna_gistic_references(cursor, gistic_roi_ids, entrez_gene_ids):
    '''
        Search for references to given set of gistic roi ids in table `gistic`.
    '''
    sql_statement = 'SELECT gistic.gistic_roi_id, gistic_to_gene.entrez_gene_id, gistic.cancer_study_id FROM gistic JOIN gistic_to_gene ON gistic_to_gene.gistic_roi_id = gistic.gistic_roi_id WHERE gistic.gistic_roi_id IN (%s) AND gistic_to_gene.entrez_gene_id IN (%s)' % (','.join(['%s'] * len(gistic_roi_ids)), ','.join(['%s'] * len(entrez_gene_ids)))

    # initialize gistic to gene records w/empty lists to keep track of how many `gistic` records are associated with them if any at all
    mirna_gistic_to_gene_records_map = dict(zip(gistic_roi_ids, [[] for x in range(len(gistic_roi_ids))]))
    ignored_gistic_to_gene_roi_ids = set()
    try:
        query_values = gistic_roi_ids[:]
        query_values.extend(entrez_gene_ids)
        cursor.execute(sql_statement, tuple(query_values))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                ignored_gistic_to_gene_roi_ids.add(int(row[0]))
                continue
            mirna_gistic_to_gene_records_map[int(row[0])].append(GisticToGene(int(row[0]), int(row[1]), int(row[2])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    # filter out gistic to gene records which can be ignored since the primary gene hugo symbol linked to its entrez gene id in `gene`
    # is not a miRNA symbol so there isn't any cleanup to do
    if len(ignored_gistic_to_gene_roi_ids) > 0:
        print >> OUTPUT_FILE, 'Ignoring gistic to gene records linked to at least one non-alias, non-miRNA gene: ' + str(len(ignored_gistic_to_gene_roi_ids)) + ' gistic to gene records...'
        for gistic_roi_id in ignored_gistic_to_gene_roi_ids:
            del mirna_gistic_to_gene_records_map[gistic_roi_id]
    return mirna_gistic_to_gene_records_map

def get_mirna_mut_sig_references(cursor, mirna_gene_map):
    '''
        Search for references to `mut_sig` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT entrez_gene_id, cancer_study_id FROM mut_sig WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    mut_sig_records = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[0])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            mut_sig_records.append(MutSig(int(row[0]), int(row[1])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return mut_sig_records

def get_mirna_protein_array_target_references(cursor,  mirna_gene_map):
    '''
        Search for references to `protein_array_target` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT protein_array_id FROM protein_array_target WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    protein_array_ids = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            protein_array_ids.append(int(row[0]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return protein_array_ids

def get_mirna_protein_array_data_references(cursor, protein_array_ids, entrez_gene_ids):
    '''
        Search for references to given set of protein array ids in table `protein_array_target`.
        (protein_array_id, entrez_gene_id, cancer_study_id, sample_id)
    '''
    sql_statement = 'SELECT protein_array_target.protein_array_id, protein_array_target.entrez_gene_id, protein_array_data.cancer_study_id, protein_array_data.sample_id FROM protein_array_data JOIN protein_array_target on protein_array_target.protein_array_id = protein_array_data.protein_array_id WHERE protein_array_target.protein_array_id IN (%s) AND protein_array_target.entrez_gene_id IN (%s)' % (','.join(['%s'] * len(protein_array_ids)), ','.join(['%s'] * len(entrez_gene_ids)))

    # initialize protein array target records w/empty lists to keep track of how many `protein_array_data` records are associated with them if any at all
    mirna_protein_array_target_records_map = dict(zip(protein_array_ids, [[] for x in range(len(protein_array_ids))]))
    ignored_protein_array_ids = set()
    try:
        query_values = protein_array_ids[:]
        query_values.extend(entrez_gene_ids)
        cursor.execute(sql_statement, tuple(protein_array_ids))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                ignored_protein_array_ids.add(int(row[0]))
                continue
            mirna_protein_array_target_records_map[int(row[0])].append(ProteinArrayData(int(row[0]), int(row[1]), int(row[2]), int(row[3])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    # filter out protein array target records which can be ignored since the primary gene hugo symbol linked to its entrez gene id in `gene`
    # is not a miRNA symbol so there isn't any cleanup to do
    if len(ignored_protein_array_ids) > 0:
        print >> OUTPUT_FILE, 'Ignoring protein array target records linked to at least one non-alias, non-miRNA gene: ' + str(len(ignored_protein_array_ids)) + ' `protein_array_target` records...'
        for protein_array_id in ignored_protein_array_ids:
            del mirna_protein_array_target_records_map[protein_array_id]
    return mirna_protein_array_target_records_map

def get_mirna_gene_panel_references(cursor, mirna_gene_map):
    '''
        Search for references to `gene_panel` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT DISTINCT gene_panel.internal_id FROM gene_panel JOIN gene_panel_list ON gene_panel_list.internal_id = gene_panel.internal_id  WHERE gene_panel_list.gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    gene_panel_ids = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            gene_panel_ids.append(int(row[0]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return gene_panel_ids

def get_mirna_sample_profile_references(cursor, gene_panel_ids, entrez_gene_ids):
    '''
        Search for references to given set of gene panel ids in table `sample_profile`.
    '''
    sql_statement = 'SELECT sample_profile.panel_id, gene_panel_list.gene_id, sample_profile.genetic_profile_id, sample_profile.sample_id, genetic_profile.cancer_study_id FROM sample_profile JOIN gene_panel_list ON gene_panel_list.internal_id = sample_profile.panel_id JOIN genetic_profile ON genetic_profile.genetic_profile_id = sample_profile.genetic_profile_id WHERE sample_profile.panel_id IN (%s) AND gene_panel_list.gene_id IN (%s)' % (','.join(['%s'] * len(gene_panel_ids)), ','.join(['%s'] * len(entrez_gene_ids)))

    # initialize gene panel id records w/empty lists to keep track of how many `gene_panel_list` records are associated with them if any at all
    mirna_gene_panel_records_map = dict(zip(gene_panel_ids, [[] for x in range(len(gene_panel_ids))]))
    try:
        query_values = gene_panel_ids[:]
        query_values.extend(entrez_gene_ids)
        cursor.execute(sql_statement, tuple(query_values))
        # cursor.execute(sql_statement)
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            mirna_gene_panel_records_map[int(row[0])].append(GenePanel(int(row[0]), int(row[1]), int(row[2]), int(row[3]), int(row[4])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return mirna_gene_panel_records_map

def get_mirna_geneset_gene_references(cursor, mirna_gene_map):
    '''
        Search for references to `geneset_gene` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT DISTINCT geneset_id FROM geneset_gene WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    geneset_ids = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            geneset_ids.append(int(row[0]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return geneset_ids

def get_mirna_geneset_references(cursor, geneset_ids, entrez_gene_ids):
    '''
        Search for references to `geneset` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT geneset.id, geneset_gene.entrez_gene_id, geneset.genetic_entity_id FROM geneset JOIN geneset_gene ON  geneset_gene.geneset_id = geneset.id WHERE geneset.id IN (%s) AND geneset_gene.entrez_gene_id IN (%s)' % (','.join(['%s'] * len(geneset_ids)), ','.join(['%s'] * len(entrez_gene_ids)))

    # initialize geneset gene records w/empty lists to keep track of how many `gene_panel_list` records are associated with them if any at all
    mirna_geneset_gene_records_map = dict(zip(geneset_ids, [[] for x in range(len(geneset_ids))]))
    try:
        query_values = geneset_ids[:]
        query_values.extend(entrez_gene_ids)
        cursor.execute(sql_statement, tuple(query_values))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            mirna_geneset_gene_records_map[int(row[0])].append(Geneset(int(row[0]), int(row[1]), int(row[2])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return mirna_geneset_gene_records_map

def get_mirna_reference_genome_references(cursor, reference_genome_ids, entrez_gene_ids):
    '''
        Search for references to `reference_genome` records linked to given a set of reference genome ids.
    '''
    sql_statement = 'SELECT reference_genome.reference_genome_id, reference_genome_gene.entrez_gene_id FROM reference_genome JOIN reference_genome_gene ON  reference_genome_gene.reference_genome_id = reference_genome.reference_genome_id WHERE reference_genome.reference_genome_id IN (%s) AND reference_genome_gene.entrez_gene_id IN (%s)' % (','.join(['%s'] * len(reference_genome_ids)), ','.join(['%s'] * len(entrez_gene_ids)))

    # initialize reference genome records w/empty lists to keep track of how many `reference_genome_gene` records are associated with them if any at all
    mirna_reference_genome_record_map = dict(zip(reference_genome_ids, [[] for x in range(len(reference_genome_ids))]))
    try:
        query_values = reference_genome_ids[:]
        query_values.extend(entrez_gene_ids)
        cursor.execute(sql_statement, tuple(query_values))
        for row in cursor.fetchall():
            # if parent hugo gene symbol in `gene` table does not start with MIR then
            # even though entrez gene id is linked to miRNA record, it is linked in the alias table
            # so there wouldn't be anything to fix or update for this record - skip such cases
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            mirna_reference_genome_record_map[int(row[0])].append(ReferenceGenome(int(row[0]), int(row[1])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return mirna_reference_genome_record_map

def get_mirna_reference_genome_gene_references(cursor, mirna_gene_map):
    '''
        Search for references to `reference_genome_gene` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT DISTINCT reference_genome_id FROM reference_genome_gene WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    reference_genome_ids = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            reference_genome_ids.append(int(row[0]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return reference_genome_ids

def get_mirna_cosmic_mutation_references(cursor, mirna_gene_map):
    '''
        Search for references to `cosmic_mutation` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT cosmic_mutation_id, entrez_gene_id FROM cosmic_mutation WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    cosmic_mutation_records = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            cosmic_mutation_records.append(CosmicMutation(int(row[0]), int(row[1])))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return cosmic_mutation_records

def get_mirna_uniprot_id_mapping_references(cursor, mirna_gene_map):
    '''
        Search for references to `uniprot_id_mapping` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT uniprot_id, entrez_gene_id, uniprot_acc FROM uniprot_id_mapping WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    uniprot_id_mapping_records = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            entrez_gene_id = int(row[1])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            uniprot_id_mapping_records.append(UniprotIdMapping(row[0], int(row[1]), row[2]))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return uniprot_id_mapping_records

def get_mirna_sanger_cancer_census_references(cursor, mirna_gene_map):
    '''
        Search for references to `sanger_cancer_census` records linked to given miRNA records.
    '''
    sql_statement = 'SELECT entrez_gene_id FROM sanger_cancer_census WHERE entrez_gene_id IN (%s)' % ','.join(['%s'] * len(mirna_gene_map.keys()))

    sanger_cancer_census_records = []
    try:
        cursor.execute(sql_statement, tuple(mirna_gene_map.keys()))
        for row in cursor.fetchall():
            entrez_gene_id = int(row[0])
            main_gene_symbol = get_hugo_gene_symbol(cursor, entrez_gene_id)
            if not main_gene_symbol.upper().startswith('MIR') and not main_gene_symbol.upper().startswith('HSA-MIR'):
                continue
            sanger_cancer_census_records.append(entrez_gene_id)
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)
    return sanger_cancer_census_records

def delete_mirna_gene_alias_record(cursor, gene_alias):
    '''
        Given an entrez gene id and gene alias, delete the matching record from `gene_alias`.
    '''
    sql_statement = 'DELETE FROM gene_alias WHERE entrez_gene_id = %s AND gene_alias = "%s"' % (gene_alias.entrez_gene_id, gene_alias.hugo_gene_symbol)
    try:
        cursor.execute(sql_statement)
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)

def delete_mirna_gene_record(cursor, entrez_gene_id):
    '''
        Given an entrez gene id, delete the matching record from `gene`.
    '''
    sql_statement = 'DELETE FROM gene WHERE entrez_gene_id = %s' % entrez_gene_id
    try:
        cursor.execute(sql_statement)
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)

def delete_mirna_genetic_entity_record(cursor, genetic_entity_id):
    '''
        Given a genetic entity id, delete the matching record from `genetic_entity`.
    '''
    sql_statement = 'DELETE FROM genetic_entity WHERE id = %s' % genetic_entity_id
    try:
        cursor.execute(sql_statement)
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)

def delete_records_from_table(cursor, table_name, field_name, list_of_ids):
    '''
        Given a table name and field name, delete records in give list of ids.
    '''
    sql_statement = 'DELETE FROM %s WHERE %s IN (%s)' % (table_name, field_name, ','.join(['%s'] * len(list_of_ids)))
    try:
        cursor.execute(sql_statement, tuple(list_of_ids))
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        sys.exit(2)

# ------------------------------------------------------------------------------
# helper functions
def generate_mirna_gene_record_map(cursor, genes, gene_aliases):
    '''
        Construct map of entrez gene id to list of miRNA candidate genes for deletion.
        The list will contain one non-alias record directly from `gene` and all
        associated records in `gene_alias` that match the miRNA gene criteria where
        hugo symbol (gene alias symbol) matching 'MIR%'.

        Any negative entrez gene ids will be reported and ignored since they come from the
        miRNA mapping reference data for the cBioPortal.

        mirna_gene_map = {
            key => <entrez_gene_id>
            value => [list containing record from `gene` and associated records from `gene_alias` for <entrez_gene_id>]
        }
    '''
    mirna_gene_map = {}

    # add genes from `gene` list - these are not aliases so there should only be
    # one gene in each list per entrez gene id. any duplicates will be ignored
    for gene in genes:
        # update GENES global map
        update_genes_map(cursor, gene.entrez_gene_id)
        mirna_gene_map[gene.entrez_gene_id] = [gene]

    # add genes from `gene_alias` list - check that no duplicates are added
    # to list of genes for each entrez gene id
    for gene in gene_aliases:
        # update GENES global map
        update_genes_map(cursor, gene.entrez_gene_id)
        gene_list = mirna_gene_map.get(gene.entrez_gene_id, [])

        # update list of genes for entrez gene id if necessary
        if not gene.hugo_gene_symbol in [g.hugo_gene_symbol for g in gene_list]:
            gene_list.append(gene)
        mirna_gene_map[gene.entrez_gene_id] = gene_list

    # sanity check that each list of genes for every entrez gene id in map
    # has at least one miRNA gene associated with it - delete entrez gene ids
    # from map if they do not have at least one miRNA deletion candiate in list
    non_mirna_candidate_entrez_ids = [entrez_gene_id for entrez_gene_id,genes in mirna_gene_map.items() if has_at_least_one_mirna_deletion_candidate_gene(genes) == False]
    if len(non_mirna_candidate_entrez_ids) > 0:
        print >> OUTPUT_FILE, 'Found ' + str(len(non_mirna_candidate_entrez_ids)) + ' entrez gene ids without at least one miRNA gene candidate for deletion.'
        print >> OUTPUT_FILE, '\tIgnoring the following entrez gene ids: ' + ','.join(map(str, non_mirna_candidate_entrez_ids))
        for entrez_gene_id in non_mirna_candidate_entrez_ids:
            del mirna_gene_map[entrez_gene_id]
    return mirna_gene_map

def get_all_mirna_gene_records_from_db(db_connection):
    '''
        Get all miRNA genes from `gene` and `gene_alias` and consolidate by entrez_gene_id.

    '''
    # get miRNA genes from `gene` then append list of aliases found
    cursor = get_db_cursor(db_connection)
    genes = get_mirna_gene_records(cursor)
    gene_aliases = get_mirna_gene_alias_records(cursor)
    return generate_mirna_gene_record_map(cursor, genes, gene_aliases)

def get_all_mirna_gene_records_from_entrez_list(db_connection, entrez_gene_ids):
    '''
        Given a list of entrez gene ids, return map of all miRNA genes from `gene` and `gene_alias`.
    '''
    cursor = get_db_cursor(db_connection)
    genes = get_mirna_gene_records_by_entrez_ids(cursor, entrez_gene_ids)
    gene_aliases = get_mirna_gene_alias_records_by_entrez_gene_ids(cursor, entrez_gene_ids)
    return generate_mirna_gene_record_map(cursor, genes, gene_aliases)

def get_genetic_profile_stable_id(cursor, genetic_profile_id):
    if not genetic_profile_id in GENETIC_PROFILES.keys():
        GENETIC_PROFILES[genetic_profile_id] = get_field_value_from_table(cursor, 'genetic_profile', 'stable_id', 'genetic_profile_id', genetic_profile_id)
    return GENETIC_PROFILES[genetic_profile_id]

def get_reference_genome_stable_id(cursor, reference_genome_id):
    if not reference_genome_id in REFERENCE_GENOMES.keys():
        species = get_field_value_from_table(cursor, 'reference_genome', 'species', 'reference_genome_id', reference_genome_id)
        name = get_field_value_from_table(cursor, 'reference_genome', 'name', 'reference_genome_id', reference_genome_id)
        build_name = get_field_value_from_table(cursor, 'reference_genome', 'build_name', 'reference_genome_id', reference_genome_id)
        REFERENCE_GENOMES[reference_genome_id] = '-'.join([species, name, build_name])
    return REFERENCE_GENOMES[reference_genome_id]

def get_sample_stable_id(cursor, sample_id):
    if not sample_id in SAMPLES.keys():
        SAMPLES[sample_id] = get_field_value_from_table(cursor, 'sample', 'stable_id', 'internal_id', sample_id)
    return SAMPLES[sample_id]

def get_cancer_study_identifier(cursor, cancer_study_id):
    if not cancer_study_id in CANCER_STUDIES.keys():
        CANCER_STUDIES[cancer_study_id] = get_field_value_from_table(cursor, 'cancer_study', 'cancer_study_identifier', 'cancer_study_id', cancer_study_id)
    return CANCER_STUDIES[cancer_study_id]

def get_gene_panel_stable_id(cursor, gene_panel_id):
    if not gene_panel_id in GENE_PANELS.keys():
        GENE_PANELS[gene_panel_id] = get_field_value_from_table(cursor, 'gene_panel', 'stable_id', 'internal_id', gene_panel_id)
    return GENE_PANELS[gene_panel_id]

def get_hugo_gene_symbol(cursor, entrez_gene_id):
    if not entrez_gene_id in GENES.keys():
        GENES[entrez_gene_id] = get_field_value_from_table(cursor, 'gene', 'hugo_gene_symbol', 'entrez_gene_id', entrez_gene_id)
    return GENES[entrez_gene_id]

def has_at_least_one_non_alias_non_mirna_gene(genes):
    for gene in genes:
        if not gene.hugo_gene_symbol.upper().startswith('MIR') and not gene.hugo_gene_symbol.upper().startswith('HSA-MIR') and not gene.is_alias:
            return True
    return False

def has_at_least_one_mirna_deletion_candidate_gene(genes):
    for gene in genes:
        if gene.entrez_gene_id > 0 and (gene.hugo_gene_symbol.upper().startswith('MIR') or gene.hugo_gene_symbol.upper().startswith('HSA-MIR')):
            return True
    return False


# calling this update functions makes calls to their corresponding "get" functions
# which also update their respective global mappings of internal ids to stable ids
def update_gene_panels_map(cursor, gene_panel_id):
    get_gene_panel_stable_id(cursor, gene_panel_id)

def update_cancer_studies_map(cursor, cancer_study_id):
    get_cancer_study_identifier(cursor, cancer_study_id)

def update_genetic_profiles_map(cursor, genetic_profile_id):
    get_genetic_profile_stable_id(cursor, genetic_profile_id)

def update_reference_genomes_map(cursor, reference_genome_id):
    get_reference_genome_stable_id(cursor, reference_genome_id)

def update_genes_map(cursor, entrez_gene_id):
    get_hugo_gene_symbol(cursor, entrez_gene_id)

def format_mirna_db_references_report(db_connection, compiled_mirna_db_reference_sets):
    '''
        Format report of miRNA database references.

        Reference types:
          - CANCER_STUDIES
          - GENETIC_PROFILES
          - GENE_PANELS
          - GENESETS
          - REFERENCE_GENOMES
          - COSMIC_MUTATIONS
          - UNIPROT_ID_MAPPING
          - SANGER_CANCER_CENSUSES
    '''
    cursor = get_db_cursor(db_connection)
    print >> OUTPUT_FILE, '\n\n ---------------------------------------------\n miRNA GENE REFERENCES REPORT'
    for ref_type in MIRNA_GENE_DB_REFERENCE_TYPES:
        ref_set = compiled_mirna_db_reference_sets.get(ref_type, set())
        if len(ref_set) == 0:
            print >> OUTPUT_FILE, 'No ' + ref_type + ' references affected by miRNA gene record removal - continuing...\n'
            continue
        # get stable ids for ref set...
        if ref_type == 'CANCER_STUDIES':
            ref_set_stable_ids = map(lambda x: get_cancer_study_identifier(cursor, x), ref_set)
        elif ref_type == 'GENETIC_PROFILES':
            ref_set_stable_ids = map(lambda x: get_genetic_profile_stable_id(cursor, x), ref_set)
        elif ref_type == 'GENE_PANELS':
            ref_set_stable_ids = map(lambda x: get_gene_panel_stable_id(cursor, x), ref_set)
        elif ref_type == 'REFERENCE_GENOMES':
            ref_set_stable_ids = map(lambda x: get_reference_genome_stable_id(cursor, x), ref_set)
        elif ref_type in ['COSMIC_MUTATIONS', 'UNIPROT_ID_MAPPING', 'SANGER_CANCER_CENSUSES']:
            # uniprot id values are already unique - fetching of another stable id is not necessary
            # `cosmic_mutation` and `sanger_cancer_census` do not have stable ids so just return the
            # ref_set value as list of strings
            ref_set_stable_ids = map(str, ref_set)
        else:
            print >> ERROR_FILE, 'Unknown ref_type: ' + ref_type
            sys.exit(2)
        ref_set_stable_ids.sort()
        print >> OUTPUT_FILE, ref_type + ':  ' + ', '.join(ref_set_stable_ids) + '\n'

def save_mirna_gene_records_removed(mirna_gene_filename):
    '''
        Save tab-delimited file containing records that were removed from `gene`, `gene_alias`.
    '''
    if len(MIRNA_GENES_OKAY_TO_REMOVE) == 0 and len(MIRNA_GENE_ALIASES_OKAY_TO_REMOVE) == 0:
        print >> OUTPUT_FILE, 'No miRNA-linked records from `gene` or `gene_alias` were removed.'
        return

    with open(mirna_gene_filename, 'w') as mirna_gene_file:
        mirna_gene_file.write('\t'.join(MIRNA_GENE_FILE_HEADER) + '\n')
        for gene in MIRNA_GENES_OKAY_TO_REMOVE:
            record = [gene.hugo_gene_symbol, str(gene.entrez_gene_id), 'NO']
            mirna_gene_file.write('\t'.join(record) + '\n')
        for gene in MIRNA_GENE_ALIASES_OKAY_TO_REMOVE:
            record = [gene.hugo_gene_symbol, str(gene.entrez_gene_id), 'YES']
            mirna_gene_file.write('\t'.join(record) + '\n')
    print >> OUTPUT_FILE, 'All miRNA-linked records removed (or candidates from removal) from `gene` and `gene_alias` were saved to: ' + mirna_gene_filename

# ------------------------------------------------------------------------------
# main functions

def find_mirna_gene_refs_in_sanger_cancer_censuses(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `sanger_cancer_census`.
    '''
    cursor = get_db_cursor(db_connection)
    sanger_cancer_census_records = get_mirna_sanger_cancer_census_references(cursor, mirna_gene_map)
    if len(sanger_cancer_census_records) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `sanger_cancer_census` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(sanger_cancer_census_records)) +' miRNA-linked records in `sanger_cancer_census`'

    distinct_entrez_ids = set(map(lambda x: x.entrez_gene_id, sanger_cancer_census_records))
    print >> OUTPUT_FILE, 'Mapped miRNA-linked records in `sanger_cancer_census` to ' + str(len(distinct_entrez_ids)) + ' distinct miRNA entrez gene ids'

    # update MIRNA_GENE_DB_RECORD_REFERENCES map
    for entrez_gene_id in sanger_cancer_census_records:
        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})

        mirna_db_sanger_cancer_census_records = mirna_db_references.get('sanger_cancer_census', {})
        sanger_cancer_census_set = mirna_db_sanger_cancer_census_records.get('SANGER_CANCER_CENSUSES', set())

        # this is a little redundant but want to keep the structure consistent for all db tables
        sanger_cancer_census_set.add(entrez_gene_id)
        mirna_db_sanger_cancer_census_records['SANGER_CANCER_CENSUSES'] = sanger_cancer_census_set
        mirna_db_references['sanger_cancer_census'] = mirna_db_sanger_cancer_census_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    # no need to clean up db table manually due to cascade deletion from `gene`
    if enable_db_deletions:
        print >> OUTPUT_FILE, 'Removing records from `sanger_cancer_census`...'
        delete_records_from_table(cursor, 'sanger_cancer_census', 'entrez_gene_id', distinct_entrez_ids)
        db_connection.commit()

def find_mirna_gene_refs_in_uniprot_id_mapping_mutations(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `uniprot_id_mapping`.
    '''
    cursor = get_db_cursor(db_connection)
    uniprot_id_mapping_records = get_mirna_uniprot_id_mapping_references(cursor, mirna_gene_map)
    if len(uniprot_id_mapping_records) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `uniprot_id_mapping` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(uniprot_id_mapping_records)) +' miRNA-linked records in `uniprot_id_mapping`'

    distinct_entrez_ids = set(map(lambda x: x.entrez_gene_id, uniprot_id_mapping_records))
    print >> OUTPUT_FILE, 'Mapped miRNA-linked records in `uniprot_id_mapping` to ' + str(len(distinct_entrez_ids)) + ' distinct miRNA entrez gene ids'

    # update MIRNA_GENE_DB_RECORD_REFERENCES map
    for record in uniprot_id_mapping_records:
        entrez_gene_id = record.entrez_gene_id
        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})

        mirna_db_uniprot_id_mapping_records = mirna_db_references.get('uniprot_id_mapping', {})
        uniprot_id_mapping_set = mirna_db_uniprot_id_mapping_records.get('UNIPROT_ID_MAPPING', set())

        # update set of uniprot ids for entrez gene id
        uniprot_id_mapping_set.add(record.uniprot_id)
        mirna_db_uniprot_id_mapping_records['UNIPROT_ID_MAPPING'] = uniprot_id_mapping_set
        mirna_db_references['uniprot_id_mapping'] = mirna_db_uniprot_id_mapping_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        print >> OUTPUT_FILE, 'Removing records from `uniprot_id_mapping`...'
        delete_records_from_table(cursor, 'uniprot_id_mapping', 'entrez_gene_id', distinct_entrez_ids)
        db_connection.commit()

def find_mirna_gene_refs_in_cosmic_mutations(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `cosmic_mutation`.
    '''
    cursor = get_db_cursor(db_connection)
    cosmic_mutation_records = get_mirna_cosmic_mutation_references(cursor, mirna_gene_map)
    if len(cosmic_mutation_records) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `cosmic_mutation` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(cosmic_mutation_records)) +' miRNA-linked records in `cosmic_mutation`'

    distinct_entrez_ids = set(map(lambda x: x.entrez_gene_id, cosmic_mutation_records))
    print >> OUTPUT_FILE, 'Mapped miRNA-linked records in `cosmic_mutation` to ' + str(len(distinct_entrez_ids)) + ' distinct miRNA entrez gene ids'

    # update MIRNA_GENE_DB_RECORD_REFERENCES map
    for record in cosmic_mutation_records:
        entrez_gene_id = record.entrez_gene_id
        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})

        mirna_db_cosmic_mutation_records = mirna_db_references.get('cosmic_mutation', {})
        cosmic_mutations_set = mirna_db_cosmic_mutation_records.get('COSMIC_MUTATIONS', set())

        # update set of cosmic mutation ids for entrez gene id
        cosmic_mutations_set.add(record.cosmic_mutation_id)
        mirna_db_cosmic_mutation_records['COSMIC_MUTATIONS'] = cosmic_mutations_set
        mirna_db_references['cosmic_mutation'] = mirna_db_cosmic_mutation_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        print >> OUTPUT_FILE, 'Removing records from `cosmic_mutation`...'
        delete_records_from_table(cursor, 'cosmic_mutation', 'entrez_gene_id', distinct_entrez_ids)
        db_connection.commit()

def find_mirna_gene_refs_in_reference_genomes(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `reference_genome`, `reference_genome_gene`.
    '''
    cursor = get_db_cursor(db_connection)
    reference_genome_ids = get_mirna_reference_genome_gene_references(cursor, mirna_gene_map)
    if len(reference_genome_ids) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `reference_genome`, `reference_genome_gene` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(reference_genome_ids)) + ' miRNA-linked records in `reference_genome`'
    mirna_reference_genome_records_map = get_mirna_reference_genome_references(cursor, reference_genome_ids, mirna_gene_map.keys())

    # keep count of orphan records in `reference_genome` (has no references to data in `reference_genome_gene`)
    # update MIRNA_GENE_DB_RECORD_REFERENCES with affected reference genome ids
    orphan_records_count = 0
    for reference_genome_id,reference_genome_records in mirna_reference_genome_records_map.items():
        # update REFERENCE_GENOMES global map
        update_reference_genomes_map(cursor, reference_genome_id)

        if len(reference_genome_records) == 0:
            orphan_records_count += 1
            continue
        entrez_gene_id = reference_genome_records[0].entrez_gene_id

        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        mirna_db_reference_genome_records = mirna_db_references.get('reference_genome', {})
        reference_genome_set = mirna_db_reference_genome_records.get('REFERENCE_GENOMES', set())
        reference_genome_set.add(reference_genome_id)

        mirna_db_reference_genome_records['REFERENCE_GENOMES'] = reference_genome_set
        mirna_db_references['reference_genome'] = mirna_db_reference_genome_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `reference_genome` miRNA records with no references in `reference_genome_gene`.'

    # no need to clean up db table manually due to cascade deletion from `gene`
    if enable_db_deletions:
        print >> OUTPUT_FILE, 'Removing records from `reference_genome_gene` will happen automatically on delete cascade from `gene`'

def find_mirna_gene_refs_in_genesets(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `geneset`, `geneset_gene`.
    '''
    cursor = get_db_cursor(db_connection)
    geneset_ids = get_mirna_geneset_gene_references(cursor, mirna_gene_map)
    if len(geneset_ids) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `geneset`, `geneset_gene` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(geneset_ids)) + ' miRNA-linked records in `geneset`'
    mirna_geneset_records_map = get_mirna_geneset_references(cursor, geneset_ids, mirna_gene_map.keys())

    # keep count of orphan records in `geneset` (has no references to data in `geneset_gene`)
    # update MIRNA_GENE_DB_RECORD_REFERENCES with affected reference genome ids
    orphan_records_count = 0
    for geneset_id,geneset_gene_records in mirna_geneset_records_map.items():
        if len(geneset_gene_records) == 0:
            orphan_records_count += 1
            continue
        entrez_gene_id = geneset_gene_records[0].entrez_gene_id

        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        mirna_db_geneset_records = mirna_db_references.get('geneset', {})
        geneset_set = mirna_db_geneset_records.get('GENESETS', set())
        geneset_set.add(geneset_id)

        mirna_db_geneset_records['GENESETS'] = geneset_set
        mirna_db_references['geneset'] = mirna_db_geneset_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `geneset` miRNA records with no references in `geneset_gene`.'

    # no need to clean up db table manually due to cascade deletion from `gene`
    if enable_db_deletions:
        print >> OUTPUT_FILE, ' Removing records from `geneset_gene` will happen automatically on delete cascade from `gene`'

def find_mirna_gene_refs_in_gene_panels(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `gene_panel`, `gene_panel_list`.
    '''
    cursor = get_db_cursor(db_connection)
    gene_panel_ids = get_mirna_gene_panel_references(cursor, mirna_gene_map)
    if len(gene_panel_ids) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `gene_panel`, `gene_panel_list`, `sample_profile` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(gene_panel_ids)) + ' miRNA-linked records in `gene_panel`'
    mirna_gene_panel_records_map = get_mirna_sample_profile_references(cursor, gene_panel_ids, mirna_gene_map.keys())

    # keep count of orphan records in `gene_panel` (has no references to data in `sample_profile`)
    # update MIRNA_GENE_DB_RECORD_REFERENCES with affected reference genome ids
    orphan_records_count = 0
    for gene_panel_id,sample_profile_records in mirna_gene_panel_records_map.items():
        # update global GENE_PANELS map
        update_gene_panels_map(cursor, gene_panel_id)

        if len(sample_profile_records) == 0:
            orphan_records_count += 1
            MIRNA_ORPHAN_GENE_PANELS.add(gene_panel_id)
            continue
        entrez_gene_id = sample_profile_records[0].entrez_gene_id

        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        mirna_db_gene_panel_records = mirna_db_references.get('gene_panel', {})
        genetic_profile_set = mirna_db_gene_panel_records.get('GENETIC_PROFILES', set())
        cancer_study_set = mirna_db_gene_panel_records.get('CANCER_STUDIES', set())

        # update genetic profile set, cancer study set for miRNA-linked gene
        for record in sample_profile_records:
            # update global CANCER_STUDIES, GENETIC_PROFILES maps
            update_genetic_profiles_map(cursor, record.genetic_profile_id)
            update_cancer_studies_map(cursor, record.cancer_study_id)

            genetic_profile_set.add(record.genetic_profile_id)
            cancer_study_set.add(record.cancer_study_id)
        mirna_db_gene_panel_records['GENETIC_PROFILES'] = genetic_profile_set
        mirna_db_gene_panel_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['gene_panel'] = mirna_db_gene_panel_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `gene_panel` miRNA records with no references in `sample_profile`.'

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        if len(mirna_gene_panel_records_map) == 0:
            # if all gene panel miRNA-linked records are associated with a non-alias, non-miRNA record in `gene`
            # then there is nothing to delete from the database since entrez gene ids for these records
            # will persist even after the removal of miRNA aliases linked to these entrez gene ids
            # OR all these gene panel records are orphans with no references in child tables
            return
        print >> OUTPUT_FILE, 'Removing records from `sample_profile`, `gene_panel_list`, `gene_panel`...'
        delete_records_from_table(cursor, 'sample_profile', 'panel_id', mirna_gene_panel_records_map.keys())
        delete_records_from_table(cursor, 'gene_panel_list', 'internal_id', mirna_gene_panel_records_map.keys())
        delete_records_from_table(cursor, 'gene_panel', 'internal_id', mirna_gene_panel_records_map.keys())
        db_connection.commit()

def find_mirna_gene_refs_in_protein_array_targets(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `protein_array_target`, `protein_array_data`, `protein_array_info`.
    '''
    cursor = get_db_cursor(db_connection)
    protein_array_ids = get_mirna_protein_array_target_references(cursor, mirna_gene_map)
    if len(protein_array_ids) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `protein_array_target`, `protein_array_data` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(protein_array_ids)) + ' miRNA-linked records in `protein_array_target`'
    mirna_protein_array_data_records_map = get_mirna_protein_array_data_references(cursor, protein_array_ids, mirna_gene_map.keys())

    # keep count of orphan records in `protein_array_info` (has no references to data in `protein_array_data`)
    # update MIRNA_GENE_DB_RECORD_REFERENCES with affected reference genome ids
    orphan_records_count = 0
    for protein_array_id,protein_array_records in mirna_protein_array_data_records_map.items():
        if len(protein_array_records) == 0:
            orphan_records_count += 1
            continue
        entrez_gene_id = protein_array_records[0].entrez_gene_id

        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        mirna_db_protein_array_target_records = mirna_db_references.get('protein_array_target', {})
        cancer_study_set = mirna_db_protein_array_target_records.get('CANCER_STUDIES', set())

        # update cancer study set for miRNA-linked gene
        # note: there aren't any genetic profiles to update, only cancer study set
        # 'GENETIC_PROFILES' will always be an empty set for db table `protein_array_target`
        for record in protein_array_records:
            # update global CANCER_STUDIES map
            update_cancer_studies_map(cursor, record.cancer_study_id)

            cancer_study_set.add(record.cancer_study_id)
        mirna_db_protein_array_target_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['protein_array_target'] = mirna_db_protein_array_target_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `protein_array_info` miRNA records with no references in `protein_array_target`.'

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        if len(mirna_protein_array_data_records_map) == 0:
            # if all protein array miRNA-linked records are associated with a non-alias, non-miRNA record in `gene`
            # then there is nothing to delete from the database since entrez gene ids for these records
            # will persist even after the removal of miRNA aliases linked to these entrez gene ids
            # OR all these protein array records are orphans with no references in child tables
            return
        print >> OUTPUT_FILE, ' Removing records from `protein_array_target`, `protein_array_data`, and `protein_array_info`'
        delete_records_from_table(cursor, 'protein_array_target', 'protein_array_id', mirna_protein_array_data_records_map.keys())
        delete_records_from_table(cursor, 'protein_array_data', 'protein_array_id', mirna_protein_array_data_records_map.keys())
        delete_records_from_table(cursor, 'protein_array_info', 'protein_array_id', mirna_protein_array_data_records_map.keys())
        db_connection.commit()

def find_mirna_gene_refs_in_mut_sigs(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `mut_sig`.
    '''
    cursor = get_db_cursor(db_connection)
    mut_sig_records = get_mirna_mut_sig_references(cursor, mirna_gene_map)
    if len(mut_sig_records) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `mut_sig` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(mut_sig_records)) +' miRNA-linked records in `mut_sig`'

    distinct_entrez_ids = set(map(lambda x: x.entrez_gene_id, mut_sig_records))
    print >> OUTPUT_FILE, 'Mapped miRNA-linked records in `mut_sig` to ' + str(len(distinct_entrez_ids)) + ' distinct miRNA entrez gene ids'

    # update MIRNA_GENE_DB_RECORD_REFERENCES map
    for record in mut_sig_records:
        entrez_gene_id = record.entrez_gene_id
        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})

        mirna_db_mut_sig_records = mirna_db_references.get('mut_sig', {})
        cancer_study_set = mirna_db_mut_sig_records.get('CANCER_STUDIES', set())
        # update global CANCER_STUDIES map
        update_cancer_studies_map(cursor, record.cancer_study_id)

        # update cancer study set for miRNA-linked gene
        # note: there aren't any genetic profiles to update, only cancer study set
        # 'GENETIC_PROFILES' will always be an empty set for db table `mut_sig`
        cancer_study_set.add(record.cancer_study_id)
        mirna_db_mut_sig_records['GENETIC_PROFILES'] = set()
        mirna_db_mut_sig_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['mut_sig'] = mirna_db_mut_sig_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        print >> OUTPUT_FILE, 'Removing records from `mut_sig`...'
        delete_records_from_table(cursor, 'mut_sig', 'entrez_gene_id', distinct_entrez_ids)
        db_connection.commit()

def find_mirna_gene_refs_in_gistic_to_genes(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `gistic_to_gene`, `gistic`.
    '''
    cursor = get_db_cursor(db_connection)
    gistic_roi_ids = get_mirna_gistic_to_gene_references(cursor, mirna_gene_map)
    if len(gistic_roi_ids) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `gistic_to_gene`, `gistic` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(gistic_roi_ids)) + ' miRNA-linked records in `gistic_to_gene`'
    mirna_gistic_to_gene_records_map = get_mirna_gistic_references(cursor, gistic_roi_ids, mirna_gene_map.keys())

    # keep count of orphan records in `gistic_to_gene` (has no references to data in `gistic`)
    # update MIRNA_GENE_DB_RECORD_REFERENCES with affected reference genome ids
    orphan_records_count = 0
    for gistic_roi_id,gistic_records in mirna_gistic_to_gene_records_map.items():
        if len(gistic_records) == 0:
            orphan_records_count += 1
            continue
        entrez_gene_id = gistic_records[0].entrez_gene_id

        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        mirna_db_gistic_to_gene_records = mirna_db_references.get('gistic_to_gene', {})
        cancer_study_set = mirna_db_gistic_to_gene_records.get('CANCER_STUDIES', set())

        # update cancer study set for miRNA-linked gene
        # note: there aren't any genetic profiles to update, only cancer study set
        # 'GENETIC_PROFILES' will always be an empty set for db table `gistic_to_gene`
        for record in gistic_records:
            # update global CANCER_STUDIES maps
            update_cancer_studies_map(cursor, record.cancer_study_id)

            cancer_study_set.add(record.cancer_study_id)
        mirna_db_gistic_to_gene_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['gistic_to_gene'] = mirna_db_gistic_to_gene_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `gistic_to_gene` records with no references in `gistic`.'

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        if len(mirna_gistic_to_gene_records_map) == 0:
            # if all gistic miRNA-linked records are associated with a non-alias, non-miRNA record in `gene`
            # then there is nothing to delete from the database since entrez gene ids for these records
            # will persist even after the removal of miRNA aliases linked to these entrez gene ids
            # OR all these gistic records are orphans with no references in child tables
            return
        print >> OUTPUT_FILE, 'Removing records from `gistic`, `gistic_to_gene`...'
        delete_records_from_table(cursor, 'gistic_to_gene', 'gistic_roi_id', mirna_gistic_to_gene_records_map.keys())
        delete_records_from_table(cursor, 'gistic', 'gistic_roi_id', mirna_gistic_to_gene_records_map.keys())
        db_connection.commit()

def find_mirna_gene_refs_in_genetic_alterations(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `genetic_alteration`.
    '''
    cursor = get_db_cursor(db_connection)
    genetic_alteration_records = get_mirna_genetic_alteration_references(cursor, mirna_gene_map)
    if len(genetic_alteration_records) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `genetic_alteration` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(genetic_alteration_records)) +' miRNA-linked records in `genetic_alteration`'

    distinct_genetic_entity_ids = set(map(lambda x: x.genetic_entity_id, genetic_alteration_records))
    print >> OUTPUT_FILE, 'Mapped miRNA-linked records in `genetic_alteration` to ' + str(len(distinct_genetic_entity_ids)) + ' distinct miRNA genetic entity ids'

    # update MIRNA_GENE_DB_RECORD_REFERENCES map
    for record in genetic_alteration_records:
        entrez_gene_id = record.entrez_gene_id
        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})

        mirna_db_genetic_alteration_records = mirna_db_references.get('genetic_alteration', {})
        genetic_profile_set = mirna_db_genetic_alteration_records.get('GENETIC_PROFILES', set())
        cancer_study_set = mirna_db_genetic_alteration_records.get('CANCER_STUDIES', set())

        # update genetic profile set, cancer study set for miRNA-linked gene
        # update global CANCER_STUDIES, GENETIC_PROFILES maps
        update_genetic_profiles_map(cursor, record.genetic_profile_id)
        update_cancer_studies_map(cursor, record.cancer_study_id)

        genetic_profile_set.add(record.genetic_profile_id)
        cancer_study_set.add(record.cancer_study_id)
        mirna_db_genetic_alteration_records['GENETIC_PROFILES'] = genetic_profile_set
        mirna_db_genetic_alteration_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['genetic_alteration'] = mirna_db_genetic_alteration_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        print >> OUTPUT_FILE, 'Removing records from `genetic_alteration`...'
        delete_records_from_table(cursor, 'genetic_alteration', 'genetic_entity_id', distinct_genetic_entity_ids)
        db_connection.commit()

def find_mirna_gene_refs_in_cna_events(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to reocrds in `cna_event`, `sample_cna_event`.
    '''
    cursor = get_db_cursor(db_connection)
    cna_event_ids = get_mirna_cna_event_references(cursor, mirna_gene_map)
    if len(cna_event_ids) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `cna_event`, `sample_cna_event` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(cna_event_ids)) + ' miRNA-linked records in `cna_event`'
    mirna_cna_events_map = get_mirna_sample_cna_event_references(cursor, cna_event_ids, mirna_gene_map.keys())

    # keep count of orphan records in `cna_event` (has no references to data in `sample_cna_event`)
    # update MIRNA_GENE_DB_RECORD_REFERENCES with affected reference genome ids
    orphan_records_count = 0
    for cna_event_id,sample_cna_events in mirna_cna_events_map.items():
        if len(sample_cna_events) == 0:
            orphan_records_count += 1
            continue
        entrez_gene_id = sample_cna_events[0].entrez_gene_id

        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        mirna_db_cna_event_records = mirna_db_references.get('cna_event', {})
        genetic_profile_set = mirna_db_cna_event_records.get('GENETIC_PROFILES', set())
        cancer_study_set = mirna_db_cna_event_records.get('CANCER_STUDIES', set())

        # update genetic profile set, cancer study set for miRNA-linked gene
        for record in sample_cna_events:
            # update global CANCER_STUDIES, GENETIC_PROFILES maps
            update_genetic_profiles_map(cursor, record.genetic_profile_id)
            update_cancer_studies_map(cursor, record.cancer_study_id)

            genetic_profile_set.add(record.genetic_profile_id)
            cancer_study_set.add(record.cancer_study_id)
        mirna_db_cna_event_records['GENETIC_PROFILES'] = genetic_profile_set
        mirna_db_cna_event_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['cna_event'] = mirna_db_cna_event_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `cna_event` records with no references in `sample_cna_event`.'

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        if len(mirna_cna_events_map) == 0:
            # if all cna event miRNA-linked records are associated with a non-alias, non-miRNA record in `gene`
            # then there is nothing to delete from the database since entrez gene ids for these records
            # will persist even after the removal of miRNA aliases linked to these entrez gene ids
            # OR all these cna event records are orphans with no references in child tables
            return
        print >> OUTPUT_FILE, 'Removing records from `sample_cna_event` and `cna_event`...'
        delete_records_from_table(cursor, 'sample_cna_event', 'cna_event_id', mirna_cna_events_map.keys())
        delete_records_from_table(cursor, 'cna_event', 'cna_event_id', mirna_cna_events_map.keys())
        db_connection.commit()

def find_mirna_gene_refs_in_mutation_count_by_keywords(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA genes linked to records in `mutation_count_by_keyword`.
    '''
    cursor = get_db_cursor(db_connection)
    mutation_count_by_keyword_records = get_mirna_mutation_count_by_keyword_references(cursor, mirna_gene_map)
    if len(mutation_count_by_keyword_records) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `mutation_count_by_keyword` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(mutation_count_by_keyword_records)) +' miRNA-linked records in `mutation_count_by_keyword`'

    distinct_entrez_ids = set(map(lambda x: x.entrez_gene_id, mutation_count_by_keyword_records))
    print >> OUTPUT_FILE, 'Mapped miRNA-linked records in `mutation_count_by_keyword` to ' + str(len(distinct_entrez_ids)) + ' distinct miRNA entrez gene ids'

    # update MIRNA_GENE_DB_RECORD_REFERENCES map
    for record in mutation_count_by_keyword_records:
        entrez_gene_id = record.entrez_gene_id
        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})

        mirna_db_mutation_count_by_keyword_records = mirna_db_references.get('mutation_count_by_keyword', {})
        genetic_profile_set = mirna_db_mutation_count_by_keyword_records.get('GENETIC_PROFILES', set())
        cancer_study_set = mirna_db_mutation_count_by_keyword_records.get('CANCER_STUDIES', set())

        # update global CANCER_STUDIES, GENETIC_PROFILES maps
        update_genetic_profiles_map(cursor, record.genetic_profile_id)
        update_cancer_studies_map(cursor, record.cancer_study_id)

        # update genetic profile set, cancer study set for miRNA-linked gene
        genetic_profile_set.add(record.genetic_profile_id)
        cancer_study_set.add(record.cancer_study_id)
        mirna_db_mutation_count_by_keyword_records['GENETIC_PROFILES'] = genetic_profile_set
        mirna_db_mutation_count_by_keyword_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['mutation_count_by_keyword'] = mirna_db_mutation_count_by_keyword_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    # no need to clean up db table manually due to cascade deletion from `gene`
    if enable_db_deletions:
        print >> OUTPUT_FILE, ' Removing records from `mutation_count_by_keyword` will happen automatically on delete cascade from `gene`'

def find_mirna_gene_refs_in_mutation_events(db_connection, mirna_gene_map, enable_db_deletions):
    '''
        Get miRNA mutation event ids associated mutation records.
    '''
    cursor = get_db_cursor(db_connection)
    mutation_event_ids = get_mirna_mutation_event_references(cursor, mirna_gene_map)
    if len(mutation_event_ids) == 0:
        # nothing to do, return
        print >> OUTPUT_FILE, '\n\nNothing to clean up from `mutation_event`, `mutation` - continuing...'
        return
    else:
        print >> OUTPUT_FILE, '\n\nFound ' + str(len(mutation_event_ids)) + ' miRNA records in `mutation_event`'
    mirna_mutation_events_map = get_mirna_mutation_references(cursor, mutation_event_ids, mirna_gene_map.keys())

    # keep count of orphan records in `mutation_event` (has no references to data in `mutation`)
    # update MIRNA_GENE_DB_RECORD_REFERENCES with affected reference genome ids
    orphan_records_count = 0
    for mutation_event_id,mutations in mirna_mutation_events_map.items():
        if len(mutations) == 0:
            orphan_records_count += 1
            continue
        entrez_gene_id = mutations[0].entrez_gene_id

        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        mirna_db_mutation_event_records = mirna_db_references.get('mutation_event', {})
        genetic_profile_set = mirna_db_mutation_event_records.get('GENETIC_PROFILES', set())
        cancer_study_set = mirna_db_mutation_event_records.get('CANCER_STUDIES', set())

        # update genetic profile set, cancer study set for miRNA-linked gene
        for record in mutations:
            # update global CANCER_STUDIES, GENETIC_PROFILES maps
            update_genetic_profiles_map(cursor, record.genetic_profile_id)
            update_cancer_studies_map(cursor, record.cancer_study_id)

            genetic_profile_set.add(record.genetic_profile_id)
            cancer_study_set.add(record.cancer_study_id)
        mirna_db_mutation_event_records['GENETIC_PROFILES'] = genetic_profile_set
        mirna_db_mutation_event_records['CANCER_STUDIES'] = cancer_study_set
        mirna_db_references['mutation_event'] = mirna_db_mutation_event_records
        MIRNA_GENE_DB_RECORD_REFERENCES[entrez_gene_id] = mirna_db_references

    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `mutation_event` records with no references in `mutation`.'

    # remove records referencing miRNA genes from db table(s)
    if enable_db_deletions:
        if len(mirna_mutation_events_map) == 0:
            # if all mutation event miRNA-linked records are associated with a non-alias, non-miRNA record in `gene`
            # then there is nothing to delete from the database since entrez gene ids for these records
            # will persist even after the removal of miRNA aliases linked to these entrez gene ids
            # OR all these mutation event records are orphans with no references in child tables
            return
        print >> OUTPUT_FILE, 'Removing records from database table `mutation` and `mutation_event`...'
        delete_records_from_table(cursor, 'mutation', 'mutation_event_id', mirna_mutation_events_map.keys())
        delete_records_from_table(cursor, 'mutation_event', 'mutation_event_id', mirna_mutation_events_map.keys())
        db_connection.commit()

def find_mirna_gene_refs_in_gene_aliases(mirna_gene_map, enable_db_deletions):
    '''
        Find genes that are okay to remove since they have at least one non-alias gene linked to a miRNA record.
    '''
    for entrez_gene_id,genes in mirna_gene_map.items():
        # if there is at least one gene in genes that is not an alias and is not miRNA
        # then alias is okay to remove from database.
        # do not want to flag non-alias, non-miRNA  or non-alias miRNA genes for removal yet since other db tables
        # may reference this entrez gene id
        if has_at_least_one_non_alias_non_mirna_gene(genes):
            for gene in genes:
                if gene.is_alias and (gene.hugo_gene_symbol.upper().startswith('MIR') or gene.hugo_gene_symbol.upper().startswith('HSA-MIR')):
                    MIRNA_GENE_ALIASES_OKAY_TO_REMOVE.add(gene)

def report_mirna_gene_references_in_db(db_connection, mirna_gene_map):
    '''
        Identify records in tables `gene` and `gene_alias` that would be okay to remove.
    '''
    # keep track of reference type and their corresponding set of references (i.e., CANCER_STUDIES - set of affected cancer studies)
    # as well as any orphan miRNA genes with no references in any database table
    compiled_mirna_db_reference_sets = {}
    orphan_records_count = 0
    for entrez_gene_id,genes in mirna_gene_map.items():
        # update lists of records in `gene` and `gene_alias` for removal
        for gene in genes:
            if gene.is_alias and (gene.hugo_gene_symbol.upper().startswith('MIR') or gene.hugo_gene_symbol.upper().startswith('HSA-MIR')):
                MIRNA_GENE_ALIASES_OKAY_TO_REMOVE.add(gene)
            elif not gene.is_alias and (gene.hugo_gene_symbol.upper().startswith('MIR') or gene.hugo_gene_symbol.upper().startswith('HSA-MIR')):
                MIRNA_GENES_OKAY_TO_REMOVE.add(gene)

        # if there is at least one gene linked to entrez gene id that is not an alias and
        # is not a miRNA gene then removal if the alias miRNA gene(s) will not affect
        # any studies or reference data sets
        if has_at_least_one_non_alias_non_mirna_gene(genes):
            continue

        # if there aren't any non-alias, non-miRNA gene then check whether there exists
        # any references to this entrez gene id in any database tables checked
        mirna_db_references = MIRNA_GENE_DB_RECORD_REFERENCES.get(entrez_gene_id, {})
        total_references = 0
        for db_table,db_table_references in mirna_db_references.items():
            # db_table_references= {
            #       key = keyword indicating one of following types of reference set:
            #           - CANCER_STUDIES
            #           - GENETIC_PROFILES
            #           - GENE_PANELS
            #           - GENESETS
            #           - REFERENCE_GENOMES
            #           - COSMIC_MUTATIONS
            #           - UNIPROT_ID_MAPPING
            #           - SANGER_CANCER_CENSUSES
            #       value = set(), contains internal ids of type of set
            #           i.e., set of cancer study ids if key = 'CANCER_STUDIES'
            #   }
            for ref_type,ref_set in db_table_references.items():
                total_references += len(ref_set)

                # update reference set for current reference type
                complete_ref_set = compiled_mirna_db_reference_sets.get(ref_type, set())
                complete_ref_set.update(ref_set)
                compiled_mirna_db_reference_sets[ref_type] = complete_ref_set

        # keep count of orphan miRNA genes with no references in any database tables checked
        if total_references == 0:
            orphan_records_count += 1

    # udpate compiled_mirna_db_reference_sets map with orphaned gene panels if any exist
    if len(MIRNA_ORPHAN_GENE_PANELS) > 0:
        gene_panel_set = compiled_mirna_db_reference_sets.get('GENE_PANELS', set())
        gene_panel_set.update(MIRNA_ORPHAN_GENE_PANELS)
        compiled_mirna_db_reference_sets['GENE_PANELS'] = gene_panel_set

    # report total orphan gene record count if any
    if orphan_records_count > 0:
        print >> OUTPUT_FILE, 'Found ' + str(orphan_records_count) + ' `gene` / `gene_alias` records with no references in any database table.'

    # format miRNA gene record removal report
    format_mirna_db_references_report(db_connection, compiled_mirna_db_reference_sets)

def remove_mirna_genes_from_db(db_connection, mirna_gene_map, enable_db_deletions):
    print >> OUTPUT_FILE, '\n\n---------------------------------------------------------------------'
    if len(MIRNA_GENE_ALIASES_OKAY_TO_REMOVE) > 0:
        cursor = get_db_cursor(db_connection)
        print >> OUTPUT_FILE, 'Found ' + str(len(MIRNA_GENE_ALIASES_OKAY_TO_REMOVE)) + ' miRNA `gene_alias` records for removal...'
        if enable_db_deletions:
            print >> OUTPUT_FILE, 'Removing records from `gene_alias`...'
            for gene_alias in MIRNA_GENE_ALIASES_OKAY_TO_REMOVE:
                delete_mirna_gene_alias_record(cursor, gene_alias)
                db_connection.commit()
    if len(MIRNA_GENES_OKAY_TO_REMOVE) > 0:
        cursor = get_db_cursor(db_connection)
        print >> OUTPUT_FILE, 'Found ' + str(len(MIRNA_GENES_OKAY_TO_REMOVE)) + ' miRNA `gene` records for removal...'
        if enable_db_deletions:
            print >> OUTPUT_FILE, 'Removing records from `gene`...'
            for gene in MIRNA_GENES_OKAY_TO_REMOVE:
                delete_mirna_gene_record(cursor, gene.entrez_gene_id)
                delete_mirna_genetic_entity_record(cursor, gene.genetic_entity_id)
                db_connection.commit()

def usage(parser, message):
    if message:
        print >> ERROR_FILE, message
    print >> OUTPUT_FILE, parser.print_help()
    sys.exit(2)

def main():
    # parse command line options
    parser = optparse.OptionParser()
    parser.add_option('-p', '--properties-file', action = 'store', dest = 'properties', help = 'portal properties file [required]')
    parser.add_option('-f', '--filename', action = 'store', dest = 'entrezfile', help = 'file containing line-delimited list of miRNA-linked entrez gene ids to search for references in database [optional, default = automatically find miRNA candidates for deletion in database]')
    parser.add_option('-l', '--list', action = 'store', dest = 'entrezlist', help = 'comma-delimited list of miRNA-linked entrez gene ids to search for references in database [optional, default = automatically find miRNA candidates for deletion in database]')
    parser.add_option('-e', '--enable-db-deletion', action = 'store_true', dest = 'enabledels', default = False, help = 'enables database deletions [default = False]')
    parser.add_option('-o', '--mirna-gene-output-file', action = 'store', dest = 'mirnagenefile', help = 'output file to store list of genes and gene alias candidates for removal / candidates that were removed')

    (options, args) = parser.parse_args()
    properties_filename = options.properties
    entrez_filename = options.entrezfile
    entrez_list = options.entrezlist
    enable_db_deletions = options.enabledels
    mirna_gene_filename = options.mirnagenefile

    # properties file is required to get db properties and establish db connection
    if not properties_filename or not mirna_gene_filename:
        usage(parser, 'Must supply a properties file and a miRNA-gene output file')

    # user may supply a file containing entrez gene ids or a comma-delimited list
    # but not both.
    # if neither are provided then script to automatically find a set of candidate
    # miRNA genes or gene aliases to search against database for references in tables
    if entrez_list and entrez_filename:
        usage(parser, 'Cannot use both [-l | --list] and [-f | --filename]. Must use either one or none. None will result in the script searching database for candidate miRNA-linked genes to delete')

    # log whether db deletions are enabled or not
    if not enable_db_deletions:
        print >> OUTPUT_FILE, 'Database deletions are disabled. If this was a mistake, please use [-e | --enable-db-deletion] to enable deletions from database.\n'
    else:
        print >> OUTPUT_FILE, 'Database deletions are enabled. All miRNA-linked genes will be removed from database tables where referenced.\n'

    # parse properties and establish db connection
    properties = PortalProperties(properties_filename)
    db_connection = establish_db_connection(properties)

    # get map of miRNA-linked entrez gene ids to list of genes/gene aliases from user-supplied list of
    # entrez gene ids or directly from database
    if entrez_list or entrez_filename:
        entrez_gene_ids = []
        if entrez_list:
            entrez_gene_ids = map(lambda x: int(x.strip()), entrez_list.split(','))
        else:
            with open(entrez_filename, 'rU') as entrez_file:
                entrez_gene_ids = map(lambda x: int(x.strip()), [row for row in entrez_file.readlines() if row.strip() != ''])
        # if any negative entrez gene ids were supplied by user, print error and exit
        negative_entrez_gene_ids = [entrez_gene_id for entrez_gene_id in entrez_gene_ids if entrez_gene_id < 0]
        if len(negative_entrez_gene_ids) > 0:
            print >> ERROR_FILE, 'Negative entrez gene ids are not permitted. Only positive entrez gene ids are allowed to be supplied. Exiting...'
            sys.exit(2)
        mirna_gene_map = get_all_mirna_gene_records_from_entrez_list(db_connection, entrez_gene_ids)
    else:
        mirna_gene_map = get_all_mirna_gene_records_from_db(db_connection)

    # confirm that miRNA gene map is not empty - if so then this indicates that there aren't any miRNA gene records in `gene` or `gene_alias`
    if len(mirna_gene_map) == 0:
        if entrez_list or entrez_filename:
            print >> OUTPUT_FILE, 'Given entrez gene ids either no longer exist in `gene` or `gene_alias` OR they exist as non-alias, non-miRNA records in `gene` and do not require removal.'
        else:
            print >> OUTPUT_FILE, 'Did not find any miRNA-linked records in `gene` or aliases in `gene_alias`.'
        print >> OUTPUT_FILE, 'Nothing to cleanup - exiting...'
        return

    print >> OUTPUT_FILE, 'There are ' + str(len(mirna_gene_map.keys())) + ' unique miRNA gene / gene alias entrez gene ids'
    # references gene.entrez_gene_id on delete cascade, no need to clean up manually
    find_mirna_gene_refs_in_gene_aliases(mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_mutation_count_by_keywords(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_gene_panels(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_genesets(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_reference_genomes(db_connection, mirna_gene_map, enable_db_deletions)

    # need to clean up manually bc of potential for orphan records to be left behind and
    # to prevent FKC errors when gene / gene_alias records are removed
    find_mirna_gene_refs_in_mutation_events(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_cna_events(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_genetic_alterations(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_gistic_to_genes(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_mut_sigs(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_protein_array_targets(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_cosmic_mutations(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_uniprot_id_mapping_mutations(db_connection, mirna_gene_map, enable_db_deletions)
    find_mirna_gene_refs_in_sanger_cancer_censuses(db_connection, mirna_gene_map, enable_db_deletions)

    # report all miRNA-linked gene references found in database
    report_mirna_gene_references_in_db(db_connection, mirna_gene_map)

    # remove miRNA genes or miRNA gene aliases from database if database deletions are enabled
    if enable_db_deletions:
        remove_mirna_genes_from_db(db_connection, mirna_gene_map, enable_db_deletions)

    # save tab-delimited file containing `gene` and `gene_alias` records removed (or candidates for removal)
    save_mirna_gene_records_removed(mirna_gene_filename)

if __name__ == '__main__':
	main()
