#! /usr/bin/env python

# imports
import os
import sys
import optparse
import urllib 
import json
import re
from datetime import datetime, timedelta

# Globals

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

DMP_USER = 'dmp.user_name'
DMP_PASSWORD = 'dmp.password'
DMP_SERVER = 'dmp.server_name'

CREATE_SESSION = 'dmp.tokens.create_session'

CONSUME_SAMPLE = 'dmp.tokens.consume_sample'
REQUEUE_SAMPLE = 'dmp.tokens.requeue.impact'

MASTERLIST_ROUTE = 'dmp.tokens.retrieve_master_list.route'
MASTERLIST_MSKIMPACT = 'dmp.tokens.retrieve_master_list.impact'
MASTERLIST_HEMEPACT = 'dmp.tokens.retrieve_master_list.heme'
MASTERLIST_RAINDANCE = 'dmp.tokens.retrieve_master_list.rdts'
MASTERLIST_ARCHER = 'dmp.tokens.retrieve_master_list.archer'

RETRIEVE_VARIANTS_MSKIMPMACT = 'dmp.tokens.retrieve_variants.impact'
RETRIEVE_VARIANTS_HEMEPACT = 'dmp.tokens.retrieve_variants.heme'
RETRIEVE_VARIANTS_RAINDANCE = 'dmp.tokens.retrieve_variants.rdts'
RETRIEVE_VARIANTS_ARCHER = 'dmp.tokens.retrieve_variants.archer'
RETRIEVE_VARIANTS_ACCESS = 'dmp.tokens.retrieve_variants.access'

REQUIRED_PROPERTIES = [
    DMP_USER,
    DMP_PASSWORD,
    DMP_SERVER,
    CREATE_SESSION,
    CONSUME_SAMPLE,
    REQUEUE_SAMPLE,
    MASTERLIST_ROUTE,
    MASTERLIST_MSKIMPACT,
    MASTERLIST_HEMEPACT,
    MASTERLIST_RAINDANCE,
    MASTERLIST_ARCHER,
    RETRIEVE_VARIANTS_MSKIMPMACT,
    RETRIEVE_VARIANTS_HEMEPACT,
    RETRIEVE_VARIANTS_RAINDANCE,
    RETRIEVE_VARIANTS_ARCHER,
    RETRIEVE_VARIANTS_ACCESS
]

SESSION_DATA_FILENAME = 'cvr_session_data.json'

CREATE_SESSION_SESSION_ID = 'session_id'
CREATE_SESSION_TIME_CREATED = 'time_created'
CREATE_SESSION_TIME_EXPIRED = 'time_expired'
CREATE_SESSION_EXPECTED_TIME_EXPIRED_TEXT = '3 hours from time created'

MASTERLIST_SIGNEDOUT_CASES = 'signedout-cases'
MASTERLIST_DMP_ID = 'dmp_assay_lbl'

REQUEUE_RESULT = 'result'

RETRIEVE_VARIANTS_DISCLAIMER = 'disclaimer'
RETRIEVE_VARIANTS_SAMPLE_COUNT = 'sample-count'
RETRIEVE_VARIANTS_RESULTS = 'results'
RETRIEVE_VARIANTS_META_DATA = 'meta-data'
RETRIEVE_VARIANTS_DMP_SAMPLE_ID = 'dmp_sample_id'

CONSUME_AFFECTED_ROWS = 'affectedRows'

DMP_STUDY_IDS = ['mskimpact', 'mskimpact_heme', 'mskraindance', 'mskarcher', 'mskaccess']
DMP_SAMPLE_ID_PATTERN = re.compile('P-\d*-T\d*-[IH|TB|TS|AH|AS|IM]+\d')

MASTERLIST_CHECK_ARG_DESCRIPTION = '[optional] Fetches masterlist for study and reports samples from samples file that are missing from masterlist.'
REQUEUE_SAMPLES_ARG_DESCRIPTION = '[optional] Requeues samples and reports whether requeue was successful or not. If requeue failed then attempts to determine if failure is due to sample(s) missing from study masterlist or if sample is already queued for next CVR fetch.'
RETRIEVE_VARIANTS_ARG_DESCRIPTION = '[optional] Retrieves data for queued samples for study and stores data for samples in list to user-specified CVR JSON file. Must be run with [-j | --cvr-json-file]'
CONSUME_SAMPLES_ARG_DESCRIPTION = '[optional] Consumes samples from samples file.'

# ------------------------------------------------------------------------------
class PortalProperties(object):
    def __init__(self, properties_filename):
        properties = self.parse_properties(properties_filename)
        self.dmp_user = properties[DMP_USER]
        self.dmp_password = properties[DMP_PASSWORD]
        self.dmp_server = properties[DMP_SERVER]
        self.create_session = properties[CREATE_SESSION]
        self.consume_sample = properties[CONSUME_SAMPLE]
        self.requeue_sample = properties[REQUEUE_SAMPLE]
        self.masterlist_route = properties[MASTERLIST_ROUTE]
        self.masterlist_mskimpact = properties[MASTERLIST_MSKIMPACT]
        self.masterlist_hemepact = properties[MASTERLIST_HEMEPACT]
        self.masterlist_raindance = properties[MASTERLIST_RAINDANCE]
        self.masterlist_archer = properties[MASTERLIST_ARCHER]
        self.retrieve_variants_mskimpact = properties[RETRIEVE_VARIANTS_MSKIMPMACT]
        self.retrieve_variants_hemepact = properties[RETRIEVE_VARIANTS_HEMEPACT]
        self.retrieve_variants_raindance = properties[RETRIEVE_VARIANTS_RAINDANCE]
        self.retrieve_variants_archer = properties[RETRIEVE_VARIANTS_ARCHER]
        self.retrieve_variants_access = properties[RETRIEVE_VARIANTS_ACCESS]

    def parse_properties(self, properties_filename):
        properties = {}
        with open(properties_filename, 'rU') as properties_file:
            for line in properties_file:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                property = map(str.strip, line.split('='))
                if len(property) != 2:
                    if property[0] in [DMP_USER, DMP_PASSWORD]:
                        property = [property[0], line[line.index('=')+1:len(line)]]
                    else:
                        continue
                properties[property[0]] = property[1]
        # validate parsed properties
        self.validate_parsed_properties(properties)
        return properties

    def validate_parsed_properties(self, properties):
        missing_properties = []
        for property_name in REQUIRED_PROPERTIES:
            if property_name not in properties or len(properties[property_name]) == 0:
                missing_properties.append(property_name)
        if len(missing_properties) > 0:
            print >> ERROR_FILE, 'Missing one or more required properties, please add the following to properties file:'
            for property_name in missing_properties:
                print >> ERROR_FILE, '\t%s' % (property_name)
            sys.exit(2)

    def get_study_masterlist_endpoint(self, study_id):
        if study_id == 'mskimpact':
            return self.masterlist_mskimpact
        if study_id == 'mskimpact_heme':
            return self.masterlist_hemepact
        if study_id == 'mskraindance':
            return self.masterlist_raindance
        if study_id == 'mskarcher':
            return self.masterlist_archer

    def get_study_retrieve_variants_endppoint(self, study_id):
        if study_id == 'mskimpact':
            return self.retrieve_variants_mskimpact
        if study_id == 'mskimpact_heme':
            return self.retrieve_variants_hemepact
        if study_id == 'mskraindance':
            return self.retrieve_variants_raindance
        if study_id == 'mskarcher':
            return self.retrieve_variants_archer
        if study_id == 'mskacess':
            return self.retrieve_variants_access

# ------------------------------------------------------------------------------
# functions
def create_and_store_session_data(portal_properties, session_data_file):
    '''
        Creates a CVR session.
    '''
    url = '%s%s/%s/%s/0' % (portal_properties.dmp_server, portal_properties.create_session, portal_properties.dmp_user, portal_properties.dmp_password)
    response = urllib.urlopen(url, urllib.urlencode({}))
    data = json.loads(response.read())

    # confirm that the 'time_expired' text matches expected text for this field
    # if it does not match then notify user and exit
    if not data[CREATE_SESSION_TIME_EXPIRED] == CREATE_SESSION_EXPECTED_TIME_EXPIRED_TEXT:
        print >> ERROR_FILE, 'The text in JSON \'%s\' does not match expected text:\n\tExpected text: %s\n\tActual text: %s' % (CREATE_SESSION_TIME_EXPIRED, CREATE_SESSION_EXPECTED_TIME_EXPIRED_TEXT, data[CREATE_SESSION_TIME_EXPIRED])
        sys.exit(2)

    # parse creation time and calculate expiration time as 3 hours from creation
    # creation time format: 2019_03_19 11:56:14
    time_created = datetime.strptime(data[CREATE_SESSION_TIME_CREATED], '%Y_%m_%d %H:%M:%S')
    time_expired = time_created + timedelta(hours = 3)

    session_data = {
        CREATE_SESSION_SESSION_ID:data[CREATE_SESSION_SESSION_ID],
        CREATE_SESSION_TIME_CREATED:str(time_created),
        CREATE_SESSION_TIME_EXPIRED:str(time_expired)
    }
    with open(session_data_file, 'w') as json_file:
        json.dump(session_data, json_file, indent = 4)
        print >> OUTPUT_FILE, 'CVR session data saved to: %s' % (session_data_file)
    return session_data

def load_session_data(session_data_file):
    '''
        Returns stored session data.
    '''
    with open(session_data_file, 'rU') as json_file:
        session_data = json.load(json_file)
        # parse the creation and expiration times as datetime objects
        session_data[CREATE_SESSION_TIME_CREATED] = datetime.strptime(session_data[CREATE_SESSION_TIME_CREATED], '%Y_%m_%d %H:%M:%S')
        session_data[CREATE_SESSION_TIME_EXPIRED] = datetime.strptime(session_data[CREATE_SESSION_TIME_EXPIRED], '%Y_%m_%d %H:%M:%S')
        return session_data

def get_session_data(portal_properties, session_data_file):
    '''
        Returns the CVR web service session data.
        
        A new session will be created and stored in the given session data file
        if one of the following is true:
            1. If the session data file does not exist
            2. If the stored session data has expired

        Otherwise, if the stored session data in the given session data file will be returned.

        The session data is stored to prevent locking user out from CVR DMP web service.
    '''

    # create and store new session data if session data file does not yet exist
    # or if the stored session data has expired
    if not os.path.exists(session_data_file):
        session_data = create_and_store_session_data(portal_properties, session_data_file)
    else:
        try:
            session_data = load_session_data(session_data_file)
            if datetime.now() > session_data[CREATE_SESSION_TIME_EXPIRED]:
                session_data = create_and_store_session_data(portal_properties, session_data_file)
        except:
            session_data = create_and_store_session_data(portal_properties, session_data_file)
    return session_data

def get_dmp_masterlist(portal_properties, session_data, study_id):
    '''
        Returns the master list for a given study id.
    '''
    if study_id == 'mskaccess':
        print >> OUTPUT_FILE, 'The CVR master list is not yet supported in the CVR web service for study mskaccess'
        return set()
    url = '%s%s/%s/%s' % (portal_properties.dmp_server, portal_properties.masterlist_route, session_data[CREATE_SESSION_SESSION_ID], portal_properties.get_study_masterlist_endpoint(study_id))
    print >> OUTPUT_FILE, 'Fetching masterlist for study \'%s\': %s' % (study_id, url)
    response = urllib.urlopen(url)
    data = json.loads(response.read())

    masterlist = set()
    for signed_out_case in data[MASTERLIST_SIGNEDOUT_CASES]:
        masterlist.add(signed_out_case[MASTERLIST_DMP_ID])
    if len(masterlist) == 0:
        print >> ERROR_FILE, 'There is something wrong with the masterlist for study \'%s\'' % (study_id)
        sys.exit(2)
    print >> OUTPUT_FILE, '\t--> study \'%s\' masterlist size: %s' % (study_id, str(len(masterlist)))
    return masterlist

def get_samples_missing_from_masterlist(study_masterlist, sample_ids):
    '''
        Returns list of samples missing from study masterlist.
    '''
    return [sample_id for sample_id in sample_ids if not sample_id in study_masterlist]

def requeue_sample(portal_properties, session_data, sample_id):
    '''
        Requeues sample and returns requeue status.
    '''
    url = '%s%s/%s/%s' % (portal_properties.dmp_server, portal_properties.requeue_sample, session_data[CREATE_SESSION_SESSION_ID], sample_id)
    response = urllib.urlopen(url)
    data = json.loads(response.read())
    return (int(data[REQUEUE_RESULT]) == 1)

def retrieve_study_variants_queue(portal_properties, session_data, study_id):
    '''
        Retrieves variants for given study and returns data plus list of all samples in current queue.
    '''
    url = '%s%s/%s/0' % (portal_properties.dmp_server, portal_properties.get_study_retrieve_variants_endppoint(study_id), session_data[CREATE_SESSION_SESSION_ID])
    response = urllib.urlopen(url)
    data = json.loads(response.read())

    sample_queue = set()
    results = []
    for sample_id,sample_data in data[RETRIEVE_VARIANTS_RESULTS].items():
        sample_queue.add(sample_id)
        results.append(sample_data)

    study_variants_data = {
        RETRIEVE_VARIANTS_DISCLAIMER:data[RETRIEVE_VARIANTS_DISCLAIMER],
        RETRIEVE_VARIANTS_SAMPLE_COUNT:int(data[RETRIEVE_VARIANTS_SAMPLE_COUNT]),
        RETRIEVE_VARIANTS_RESULTS:results
    }
    return study_variants_data,sample_queue

def get_samples_in_queue(sample_queue, sample_ids):
    '''
        Returns samples in sample queue for study.
    '''
    return [sample_id for sample_id in sample_ids if sample_id in sample_queue]

def consume_sample(portal_properties, session_data, sample_id):
    '''
        Consumes given sample and returns bool indicating whether sample consumed successfully.
    '''
    url = '%s%s/%s/%s' % (portal_properties.dmp_server, portal_properties.consume_sample, session_data[CREATE_SESSION_SESSION_ID], sample_id)
    response = urllib.urlopen(url)
    data = json.loads(response.read())
    return (int(data.get(CONSUME_AFFECTED_ROWS, '0')) == 1)

def load_samples_from_file(samples_file):
    '''
        Loads samples from given samples file.
    '''
    invalid_sample_ids = set()
    sample_ids = set()
    with open(samples_file, 'rU') as data_file:
        for row in data_file.readlines():
            sample_id = row.strip()
            if not DMP_SAMPLE_ID_PATTERN.match(sample_id):
                invalid_sample_ids.add(sample_id)
                continue
            sample_ids.add(sample_id.strip())
    # if any invalid DMP sample ids in file then report and exit
    if len(invalid_sample_ids) > 0:
        print >> ERROR_FILE, 'There are invalid DMP IDs in the samples file provided - remove samples before running the script again:\n\t%s' % ('\n\t'.join(list(invalid_sample_ids)))
        sys.exit(2)
    return sample_ids

def run_masterlist_check_mode(study_id, study_masterlist, sample_ids):
    '''
        Masterlist check mode.

        Reports samples missing from study masterlist.
    '''
    missing_samples = get_samples_missing_from_masterlist(study_masterlist, sample_ids)
    if len(missing_samples) > 0:
        missing_samples_str = '\n\t'.join(missing_samples)
        print >> OUTPUT_FILE, 'Found %s samples not in masterlist!:\n\t%s' % (str(len(missing_samples)), missing_samples_str)
    else:
        print >> OUTPUT_FILE, 'All samples exist in study masterlist!'

def run_requeue_samples_mode(portal_properties, session_data, study_id, study_masterlist, current_study_sample_queue, sample_ids):
    '''
        Requeue samples mode.

        Requeues samples from list. 

        Reasons a sample might fail to requeue:
            1. Sample is already in queueue for study
            2. Sample does not exist in study masterlist.
            3. Sample exists in masterlist but sample is not 
                signed out or is considered a failed sample. 
                ** These cases must be reported to CVR. **
    '''
    requeue_success_samples = set()
    requeue_failure_samples = set()
    for sample_id in sample_ids:
        if not requeue_sample(portal_properties, session_data, sample_id):
            requeue_failure_samples.add(sample_id)
        else:
            requeue_success_samples.add(sample_id)

    # determine the reason for these samples failing to requeue if any
    if len(requeue_failure_samples) > 0:

        # samples might fail to requeue if they do not exist in the study's master list
        # these samples should be reported to the CVR team
        samples_missing_from_masterlist = get_samples_missing_from_masterlist(study_masterlist, requeue_failure_samples)
        if len(samples_missing_from_masterlist) > 0:
            print >> ERROR_FILE, 'The following sample(s) failed to requeue and are missing from the study masterlist:\n\t%s\n\n\t--> Please report to CVR team!' % ('\n\t'.join(samples_missing_from_masterlist))
            for sample_id in samples_missing_from_masterlist:
                requeue_failure_samples.remove(sample_id)

        # samples might fail to requeue if they are already in study's current queue
        # treat samples from list that are already queued as successfully requeued samples
        # and remove from failed requeue set
        samples_already_in_queue = get_samples_in_queue(current_study_sample_queue, requeue_failure_samples)
        if len(samples_already_in_queue) > 0:
            print >> OUTPUT_FILE, 'Found samples in list that are already in current study queue:\n\t%s' % ('\n\t'.join(samples_already_in_queue))
            for sample_id in samples_already_in_queue:
                requeue_success_samples.add(sample_id)
                requeue_failure_samples.remove(sample_id)

        # samples that remain in requeue failure set are considered failed samples for an unknown reason and must be reported to CVR team
        if len(requeue_failure_samples) > 0:
            print >> OUTPUT_FILE, 'Requeue failures: %s' % (','.join(list(requeue_failure_samples)))
            print >> OUTPUT_FILE, 'Requeue successes: %s' % (','.join(list(requeue_success_samples)))
            print >> OUTPUT_FILE, requeue_success_samples
            print >> ERROR_FILE, 'The following sample(s) failed to requeue for an unknown reason:\n\t%s\n\n\t--> Please report to CVR team!' % ('\n\t'.join(list(requeue_failure_samples)))

    # if any samples requeued successfully then confirm whether they are in the new study queue
    if len(requeue_success_samples) > 0:
        # get updated CVR variants fetch results
        study_variants_data,new_sample_queue = retrieve_study_variants_queue(portal_properties, session_data, study_id)
        requeued_samples_in_queue = get_samples_in_queue(new_sample_queue, requeue_success_samples)
        # if size of sets match then all samples succeeded to requeue and exist in the updated CVR variant fetch results
        if len(requeued_samples_in_queue) == len(requeue_success_samples):
            print >> OUTPUT_FILE, 'All samples that successfully requeued exist in the updated CVR variant fetch results.'
        else:
            print >> ERROR_FILE, 'Some samples that requeued successfully are missing from the updated CVR variant fetch results.'
            samples_missing_from_masterlist = get_samples_missing_from_masterlist(study_masterlist, requeue_success_samples)
            if len(samples_missing_from_masterlist) > 0:
                print >> ERROR_FILE, 'The following sample(s) successfully requeued but are missing from the study masterlist:\n\t%s\n\n\t--> Please report to CVR team!' % ('\n\t'.join(samples_missing_from_masterlist))

def run_retrieve_variants_mode(portal_properties, session_data, study_id, current_study_sample_queue, sample_ids, cvr_json_file):
    '''
        Retrieves CVR variants results for study and filters results to samples in sample ids list.
    '''
    study_variants_data,current_study_sample_queue = retrieve_study_variants_queue(portal_properties, session_data, study_id)

    # report any samples that are missing from the current study queue
    samples_missing_from_queue = [sample_id for sample_id in sample_ids if not sample_id in current_study_sample_queue]
    if len(samples_missing_from_queue) > 0:
        print >> ERROR_FILE, 'The following sample(s) from the sample list are missing from the current study queue:\n\n\t%s' % ('\n\t'.join(samples_missing_from_queue))
        return

    # filter CVR variants results by samples in list of samples ids
    filtered_variants_by_samples = []
    for result in study_variants_data[RETRIEVE_VARIANTS_RESULTS]:
        if result[RETRIEVE_VARIANTS_META_DATA][RETRIEVE_VARIANTS_DMP_SAMPLE_ID] in sample_ids:
            filtered_variants_by_samples.append(result)

    filtered_cvr_variants_results = {
        RETRIEVE_VARIANTS_DISCLAIMER:study_variants_data[RETRIEVE_VARIANTS_DISCLAIMER],
        RETRIEVE_VARIANTS_SAMPLE_COUNT:len(filtered_variants_by_samples),
        RETRIEVE_VARIANTS_RESULTS:filtered_variants_by_samples
    }

    # save filtered results to provided CVR JSON data file
    with open(cvr_json_file, 'w') as json_file:
        json.dump(filtered_cvr_variants_results, json_file, indent = 4)
        print >> OUTPUT_FILE, 'Filtered CVR variants results written to: %s' % (cvr_json_file)

def run_consume_samples_mode(portal_properties, session_data, study_id, study_masterlist, current_study_sample_queue, sample_ids):
    '''
        Consumes samples in sample ids list.
    '''
    consume_success_samples = set()
    consume_failed_samples = set()
    for sample_id in sample_ids:
        if consume_sample(portal_properties, session_data, sample_id):
            consume_success_samples.add(sample_id)
        else:
            consume_failed_samples.add(sample_id)
    # get updated CVR variants fetch results
    study_variants_data,new_sample_queue = retrieve_study_variants_queue(portal_properties, session_data, study_id)

    # determine the reason for these samples failing to consume if any
    if len(consume_failed_samples) > 0:
        # samples might fail to consume if they do not exist in the study's master list
        # these samples should be reported to the CVR team
        samples_missing_from_masterlist = get_samples_missing_from_masterlist(study_masterlist, consume_failed_samples)
        if len(samples_missing_from_masterlist) > 0:
            print >> ERROR_FILE, 'The following sample(s) failed to consume and are missing from the study masterlist:\n%s\n\n\t--> Please report to CVR team!' % ('\n\t'.join(samples_missing_from_masterlist))
            for sample_id in samples_missing_from_masterlist:
                consume_failed_samples.remove(sample_id)

        # samples might fail to consume if they are already consumed and not in current study queue
        # confirm that these samples are not in the new sample queue and treat samples as successfully consumed 
        # samples and remove from failed consume set
        if len(consume_failed_samples) > 0:
            samples_already_consumed = [sample_id for sample_id in consume_failed_samples if not sample_id in new_sample_queue]
            if len(samples_already_consumed) > 0:
                print >> OUTPUT_FILE, 'Found samples in list that might have already been consumed for study:\n\t%s' % ('\n\t'.join(samples_already_consumed))
                for sample_id in samples_already_consumed:
                    consume_success_samples.add(sample_id)
                    consume_failed_samples.remove(sample_id)

        # samples that remain in requeue failure set are considered failed samples for an unknown reason and must be reported to CVR team
        if len(consume_failed_samples) > 0:
            print >> ERROR_FILE, 'The following sample(s) failed to consume for an unknown reason:\n%s\n\n\t--> Please report to CVR team!' % ('\n\t'.join(list(consume_failed_samples)))

    # if any samples requeued successfully then confirm whether they are in the new study queue
    if len(consume_success_samples) > 0:
        consumed_samples_in_queue = get_samples_in_queue(new_sample_queue, consume_success_samples)
        if len(consumed_samples_in_queue) == 0:
            print >> OUTPUT_FILE, 'All samples in list were successfully consumed.'
        else:
            print >> ERROR_FILE, 'Some samples that were successfully consumed are present in the updated CVR variant fetch results.'
            samples_missing_from_masterlist = get_samples_missing_from_masterlist(study_masterlist, consumed_samples_in_queue)
            if len(samples_missing_from_masterlist) > 0:
                print >> ERROR_FILE, 'The following sample(s) were consumed successfully but are present in the updated CVR variant fetch results and but are missing from the study masterlist:\n%s\n\n\t--> Please report to CVR team!' % ('\n\t'.join(consumed_samples_in_queue))
                for sample_id in samples_missing_from_masterlist:
                    consumed_samples_in_queue.remove(sample_id)

            # if there are still samples in set of consumed samples in queue then report issue to CVR team
            if len(consumed_samples_in_queue) > 0:
                print >> ERROR_FILE, 'The following sample(s) were consumed successfully but are present in the updated CVR variant fetch results for an unknown reason:\n%s\n\n\t--> Please report to CVR team!' % ('\n\t'.join(consumed_samples_in_queue))

def usage(parser, message):
    if message:
        print >> OUTPUT_FILE, message
    print >> OUTPUT_FILE, parser.print_help()
    sys.exit(2)

def main():
    # parse command line options
    parser = optparse.OptionParser()
    parser.add_option('-p', '--properties-file', action = 'store', dest = 'properties', help = '[required] Properties file')
    parser.add_option('-s', '--session-data-file', action = 'store', dest = 'sessionfile', help = '[required] File to store and/or load CVR web service session data from')
    parser.add_option('-i', '--study-id', action = 'store', dest = 'study', type = 'choice', choices = DMP_STUDY_IDS, help = '[required] DMP study id [%s]' % (', '.join(DMP_STUDY_IDS)))
    parser.add_option('-f', '--samples-file', action = 'store', dest = 'samplesfile', help = '[required] File containing line-delimited DMP sample ids')

    # available modes to run
    parser.add_option('-m', '--check-masterlist', action = 'store_true', dest = 'masterlistmode', default = False, help = MASTERLIST_CHECK_ARG_DESCRIPTION)
    parser.add_option('-r', '--requeue-samples', action = 'store_true', dest = 'requeuemode', default = False, help = REQUEUE_SAMPLES_ARG_DESCRIPTION)
    parser.add_option('-v', '--retrieve-variants', action = 'store_true', dest = 'retrievevarsmode', default = False, help = RETRIEVE_VARIANTS_ARG_DESCRIPTION)
    parser.add_option('-c', '--consume-samples', action = 'store_true', dest = 'consumemode', default = False, help = CONSUME_SAMPLES_ARG_DESCRIPTION)

    # additional arguments dependent on type of mode being run
    parser.add_option('-j', '--cvr-json-file', action = 'store', dest = 'cvrjson', help = 'CVR JSON file to save queued data to')

    (options, args) = parser.parse_args()
    
    # process the options
    properties_filename = options.properties
    session_data_file = options.sessionfile
    study_id = options.study
    samples_file = options.samplesfile

    # mode flags
    masterlist_mode = options.masterlistmode
    requeue_samples_mode = options.requeuemode
    retrieve_variants_mode = options.retrievevarsmode
    consume_samples_mode = options.consumemode

    # argument(s) dependent on type of mode
    cvr_json_file = options.cvrjson

    # check for required arguments
    if not properties_filename or not session_data_file or not study_id or not samples_file:
        usage(parser, 'Missing one or more required arguments. Exiting...')

    # check that at least one mode type is provided
    if not masterlist_mode and not requeue_samples_mode and not retrieve_variants_mode and not consume_samples_mode:
        usage(parser, 'Mode is undefined - please choose from available modes. Exiting...')

    # if running in retrieve variants mode then CVR JSON file argument must also be provided
    if retrieve_variants_mode and not cvr_json_file:
        usage(parser, 'Must provide a CVR JSON file as [-j | --cvr-json-file] when running in retrieve variants mode [-r | --retrieve-variants]. Exiting...')

    modes = []
    if masterlist_mode:
        modes.append('masterlist check mode')
    if requeue_samples_mode:
        modes.append('requeue samples mode')
    if retrieve_variants_mode:
        modes.append('retrieve variants mode')
    if consume_samples_mode:
        modes.append('consume samples mode')

    print >> OUTPUT_FILE, 'Running the following mode(s):'
    for mode in modes:
        print >> OUTPUT_FILE, '\t%s' % (mode)

    portal_properties = PortalProperties(properties_filename)
    session_data = get_session_data(portal_properties, session_data_file)
    study_masterlist = get_dmp_masterlist(portal_properties, session_data, study_id)
    current_study_cvr_variants_results,current_study_sample_queue = retrieve_study_variants_queue(portal_properties, session_data, study_id)
    sample_ids = load_samples_from_file(samples_file)

    if masterlist_mode:
        print >> OUTPUT_FILE, '\nStarting masterlist check mode...'
        run_masterlist_check_mode(study_id, study_masterlist, sample_ids)
    if requeue_samples_mode:
        print >> OUTPUT_FILE, '\nStarting requeue samples mode...'
        run_requeue_samples_mode(portal_properties, session_data, study_id, study_masterlist, current_study_sample_queue, sample_ids)
    if retrieve_variants_mode:
        print >> OUTPUT_FILE, '\nStarting the retrieve variants mode...'
        run_retrieve_variants_mode(portal_properties, session_data, study_id, current_study_sample_queue, sample_ids, cvr_json_file)
    if consume_samples_mode:
        print >> OUTPUT_FILE, '\nStarting the consume samples mode...'
        run_consume_samples_mode(portal_properties, session_data, study_id, study_masterlist, current_study_sample_queue, sample_ids)

if __name__ == '__main__':
    main()
