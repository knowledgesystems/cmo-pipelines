#! /usr/bin/env python
# ------------------------------------------------------------------------------
# Utility script which adds metadata headers to specified file(s)
# Metadata is pulled from google spreadsheets
# Four lines added at the top (display name, dscriptions, datatype, priority)
# Changes only made if all input  files are valid
# The following properties must be specified in the a properties file:
# google.id
# google.pw
# google.spreadsheet
# google.clinical_attributes_worksheet
# ------------------------------------------------------------------------------
import argparse
import gdata.docs.client
import gdata.docs.service
import gdata.spreadsheet.service
import httplib2
import os
import shutil
import sys
import tempfile
import time

from clinicalfile_utils import write_data, write_header_line, get_header
from oauth2client import client
from oauth2client.client import flow_from_clientsecrets
from oauth2client.file import Storage
from oauth2client.tools import run_flow, argparser
# ------------------------------------------------------------------------------
# globals
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# fields in portal.properties
GOOGLE_ID = 'google.id'
GOOGLE_PW = 'google.pw'
GOOGLE_SPREADSHEET = 'google.spreadsheet'
GOOGLE_WORKSHEET = 'google.clinical_attributes_worksheet'
CLINICAL_ATTRIBUTES_WORKSHEET = 'clinical_attributes'

# column constants on google spreadsheet
DATATYPE_KEY = 'datatype'
DESCRIPTIONS_KEY = 'descriptions'
DISPLAY_NAME_KEY = 'displayname'
NORMALIZED_COLUMN_HEADER_KEY = 'normalizedcolumnheader'
ATTRIBUTE_TYPE_KEY = 'attributetype'
PRIORITY_KEY = 'priority'
# ------------------------------------------------------------------------------
# class definitions
class PortalProperties(object):
    def __init__(self, google_id, google_pw, google_spreadsheet, google_worksheet):
        self.google_id = google_id
        self.google_pw = google_pw
        self.google_spreadsheet = google_spreadsheet
        self.google_worksheet = google_worksheet
# ------------------------------------------------------------------------------
# logs into google spreadsheet client
def get_gdata_credentials(secrets, creds, scope, force=False):
    storage = Storage(creds)
    credentials = storage.get()
    if credentials.access_token_expired:
        credentials.refresh(httplib2.Http())
    if credentials is None or credentials.invalid or force:
      credentials = run_flow(flow_from_clientsecrets(secrets, scope=scope), storage, argparser.parse_args([]))
    return credentials

def google_login(secrets, creds, user, pw, app_name):
    credentials = get_gdata_credentials(secrets, creds, ["https://spreadsheets.google.com/feeds"], False)
    client = gdata.spreadsheet.service.SpreadsheetsService(additional_headers={'Authorization' : 'Bearer %s' % credentials.access_token})
    # google spreadsheet
    client.email = user
    client.password = pw
    client.source = app_name
    client.ProgrammaticLogin()
    return client
# ------------------------------------------------------------------------------
# given a feed & feed name, returns its id
def get_feed_id(feed, name):
    to_return = ''
    for entry in feed.entry:
        if entry.title.text.strip() == name:
            id_parts = entry.id.text.split('/')
            to_return = id_parts[len(id_parts) - 1]
    return to_return
# ------------------------------------------------------------------------------
def get_worksheet_feed(client, ss, ws):
    ss_id = get_feed_id(client.GetSpreadsheetsFeed(), ss)
    ws_id = get_feed_id(client.GetWorksheetsFeed(ss_id), ws)
    return client.GetListFeed(ss_id, ws_id)
# ------------------------------------------------------------------------------
# create PortalProperties instance based on portal.properties files
def get_portal_properties(portal_properties_filename):
    properties = {}
    portal_properties_file = open(portal_properties_filename, 'r')
    for line in portal_properties_file:
        line = line.strip()
        if len(line) == 0 or line.startswith('#'):
            continue
        property = line.split('=')
        if (len(property) != 2):
            print >> ERROR_FILE, 'Skipping invalid entry in property file: ' + line
            continue
        properties[property[0]] = property[1].strip()
    portal_properties_file.close()
    # check that required properties are set
    if (GOOGLE_ID not in properties or len(properties[GOOGLE_ID]) == 0 or
        GOOGLE_PW not in properties or len(properties[GOOGLE_PW]) == 0 or
        GOOGLE_SPREADSHEET not in properties or len(properties[GOOGLE_SPREADSHEET]) == 0):
        print >> ERROR_FILE, 'Missing one or more required properties, please check property file'
        return None
    return PortalProperties(properties[GOOGLE_ID],
                            properties[GOOGLE_PW],
                            properties[GOOGLE_SPREADSHEET],
                            properties[GOOGLE_WORKSHEET])
# ------------------------------------------------------------------------------
# returns dictionary where
# key: normalized column header (ie SAMPLE_ID)
# value: dictionary of metadata {'DISPLAY_NAME':display_name, 'DESCRIPTION':description, 'DATATYPE':datatype, 'PRIORITY':priority]
# (ie {'DISPLAY_NAME':Sample Id, 'DESCRIPTION':Unique sample identifier, 'DATATYPE':String, 'PRIORITY':1})
def get_clinical_attribute_metadata_map(google_spreadsheet, client, header):
    metadata_mapping = {}
    print 'Getting clinical attributes metadata from google spreadsheet'
    header_set = set(header)
    clinical_attributes_worksheet_feed = get_worksheet_feed(client, google_spreadsheet, CLINICAL_ATTRIBUTES_WORKSHEET)
    for entry in clinical_attributes_worksheet_feed.entry:
        normalized_column_header = entry.custom[NORMALIZED_COLUMN_HEADER_KEY].text.rstrip()
        display_name = entry.custom[DISPLAY_NAME_KEY].text.rstrip()
        descriptions = entry.custom[DESCRIPTIONS_KEY].text.rstrip()
        datatype = entry.custom[DATATYPE_KEY].text.rstrip()
        attribute_type = entry.custom[ATTRIBUTE_TYPE_KEY].text.rstrip()
        priority = entry.custom[PRIORITY_KEY].text
        if normalized_column_header is not None and normalized_column_header in header_set:
            metadata_mapping[normalized_column_header] = {
                'DISPLAY_NAME' : display_name,
                'DESCRIPTIONS' : descriptions,
                'DATATYPE' : datatype,
                'ATTRIBUTE_TYPE' : attribute_type,
                'PRIORITY' : priority}
    return metadata_mapping
# ------------------------------------------------------------------------------
def write_headers(header, metadata_dictionary, output_file, is_mixed_attribute_types_format):
    name_line = []
    description_line = []
    datatype_line = []
    attribute_type_line = []
    priority_line = []
    for attribute in header:
        if attribute in metadata_dictionary:
            name_line.append(metadata_dictionary[attribute]['DISPLAY_NAME'])
            description_line.append(metadata_dictionary[attribute]['DESCRIPTIONS'])
            datatype_line.append(metadata_dictionary[attribute]['DATATYPE'])
            attribute_type_line.append(metadata_dictionary[attribute]['ATTRIBUTE_TYPE'])
            priority_line.append(metadata_dictionary[attribute]['PRIORITY'])
        else:
            # if attribute not in google worksheet, use defaults
            name_line.append(attribute.replace("_", " ").title())
            description_line.append(attribute.replace("_", " ").title())
            datatype_line.append('STRING')
            attribute_type_line.append('SAMPLE')
            priority_line.append('1')
    write_header_line(name_line, output_file)
    write_header_line(description_line, output_file)
    write_header_line(datatype_line, output_file)
    # if patient and sample attributes are in file, print attribute type metadata header
    if len(set(attribute_type_line)) > 0 and is_mixed_attribute_types_format:
        write_header_line(attribute_type_line, output_file)
    write_header_line(priority_line, output_file)
# -----------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-s", "--secrets-file", help = "secrets file", required = True)
    parser.add_argument("-c", "--creds-file", help = "credentials file", required = True)
    parser.add_argument("-p", "--properties-file", help = "properties file", required = True)
    parser.add_argument("-f", "--files", nargs = "+", help = "file(s) to add metadata headers", required = True)
    parser.add_argument("-m", "--mixed-attribute-types", help = "flag for whether file is new/old format -- whether patient/sample attributes are seperated", action = "store_true")
    args = parser.parse_args()

    is_mixed_attribute_types_format = args.mixed_attribute_types
    secrets_filename = args.secrets_file
    creds_filename = args.creds_file
    properties_filename = args.properties_file
    clinical_files = args.files

    if not os.path.exists(properties_filename):
        print >> ERROR_FILE, 'properties file cannot be found: ' + properties_filename
        sys.exit(2)
    if not os.path.exists(creds_filename):
        print >> ERROR_FILE, 'credentials file cannot be found: ' + creds_filename
        sys.exit(2)
    if not os.path.exists(secrets_filename):
        print >> ERROR_FILE, 'secrets file cannot be found: ' + creds_filename
        sys.exit(2)
    # check file (args) validity and return error if any file fails check
    missing_clinical_files = [clinical_file for clinical_file in clinical_files if not os.path.exists(clinical_file)]
    if len(missing_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not found: ' + ', '.join(missing_clinical_files)
        sys.exit(2)
    not_writable_clinical_files = [clinical_file for clinical_file in clinical_files if not os.access(clinical_file,os.W_OK)]
    if len(not_writable_clinical_files) > 0:
        print >> ERROR_FILE, 'File(s) not writable: ' + ', '.join(not_writable_clinical_files)
        sys.exit(2)
    # parse/get relevant portal properties
    print >> OUTPUT_FILE, 'Reading portal properties file: ' + properties_filename
    portal_properties = get_portal_properties(properties_filename)
    if not portal_properties:
        print >> ERROR_FILE, 'Error reading %s, exiting' % properties_filename
        sys.exit(2)
    client = google_login(secrets_filename, creds_filename, portal_properties.google_id, portal_properties.google_pw, sys.argv[0])
    metadata_dictionary = {}
    all_attributes = set()
    # get a set of attributes used across all input files
    for clinical_file in clinical_files:
        all_attributes = all_attributes.union(get_header(clinical_file))
    metadata_dictionary = get_clinical_attribute_metadata_map(portal_properties.google_spreadsheet, client, all_attributes)
    # check metadata is defined for all attributes in google spreadsheet
    if len(metadata_dictionary.keys()) != len(all_attributes):
        print >> ERROR_FILE, 'Error, metadata not found for attribute(s): ' + ', '.join(all_attributes.difference(metadata_dictionary.keys()))
    for clinical_file in clinical_files:
        # create temp file to write to
        temp_file, temp_file_name = tempfile.mkstemp()
        header = get_header(clinical_file)
        write_headers(header, metadata_dictionary, temp_file, is_mixed_attribute_types_format)
        write_data(clinical_file, temp_file)
        os.close(temp_file)
        # replace original file with new file
        shutil.move(temp_file_name, clinical_file)
# ------------------------------------------------------------------------------
if __name__ == '__main__':
    main()
