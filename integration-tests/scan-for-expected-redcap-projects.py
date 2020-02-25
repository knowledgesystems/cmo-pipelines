#!/usr/bin/python
import sys
import os
import optparse
import re
import shutil
import subprocess


def get_expected_project_mapping(filename):
    """
        returns a dictionary of project name sets (per stable id)
    """
    mapping = {}
    mapping_file = open(filename, 'r')
    for line in mapping_file:
        if line.startswith('#'):
            continue
        words = line.split('\t')
        if len(words) != 2:
            print >> sys.stderr, 'error : ' + expected_project_mapping_filename + ' contains a line which is not properly formatted:' + line
            sys.exit(2)
        stable_id = words[0].rstrip()
        project_name = words[1].rstrip()
        if not stable_id in mapping:
            mapping[stable_id] = set()
        mapping[stable_id].add(project_name)
    mapping_file.close()
    return mapping

def convertfilename_to_projectname(filename):
    if not filename.endswith('.txt'):
        print >> sys.stderr, 'error : encountered file which did not end with .txt (as expected for all exported redcap files'
        sys.exit(2)
    if filename.startswith('data_clinical_') or filename.startswith('data_timeline_'):
        projectname = filename[14:-4]
    else:
        print >> sys.stderr, 'error : encountered file which did not start with "data_clinical" or "data_timeline" (as expected for all exported redcap files'
        sys.exit(2)
    return projectname

def get_observed_project_mapping(stable_id_list, temporary_directory_name, redcap_jar, truststore, truststore_password):
    mapping = {}
    for stable_id in stable_id_list:
        mapping[stable_id] = set()
        study_tempdir_name = os.path.join(temporary_directory_name, stable_id)
        if os.path.isdir(study_tempdir_name):
            shutil.rmtree(study_tempdir_name)
        os.mkdir(study_tempdir_name)
        #export raw mode redcap-pipeline
        command = 'java -Djavax.net.ssl.trustStore=' + truststore + '  -Djavax.net.ssl.trustStorePassword=' + truststore_password + ' -jar ' + redcap_jar + ' --export-mode --raw-data --stable-id ' + stable_id + ' --directory ' + study_tempdir_name
        status = subprocess.call(command, shell=True)
        if status != 0:
            print >> sys.stderr, 'error : command had non-zero exit status : "' + command + '"'
            sys.exit(2)
        #add projects to dictionary
        files_exported = os.listdir(study_tempdir_name)
        for f in files_exported:
            projectname = convertfilename_to_projectname(f)
            mapping[stable_id].add(projectname)
        shutil.rmtree(study_tempdir_name)
    return mapping

def compare_expected_to_observed(expected_project_mapping, observed_project_mapping):
    key_difference = set(expected_project_mapping.keys()) ^ set(observed_project_mapping.keys())
    if len(key_difference):
        print >> sys.stderr, 'error : there was a discrepancy in the stable_ids expected:'
        print >> sys.stderr, '  expected:' + ', '.join(expected_project_mapping.keys())
        print >> sys.stderr, '  observed:' + ', '.join(observed_project_mapping.keys())
        sys.exit(2)
    for stable_id in expected_project_mapping.keys():
        set_difference = expected_project_mapping[stable_id] ^ observed_project_mapping[stable_id]
        if len(set_difference) > 0:
            print >> sys.stderr, 'error : project list for stable_id ' + stable_id + ' differed from expected list:'
            print >> sys.stderr, ', '.join(set_difference)
            sys.exit(2)

def usage(parser):
    parser.print_help()
    sys.exit(2)

def main():
    # get command line stuff
    parser = optparse.OptionParser()
    parser.add_option('-e', '--expected-project-mapping-file', action = 'store', dest = 'expected_project_mapping_filename', help = 'file with expected projects for study ids')
    parser.add_option('-t', '--temporary-directory', action = 'store', dest = 'temporary_directory_name', help = 'temporary directory for exporting redcap projects')
    parser.add_option('-j', '--redcap-jar', action = 'store', dest = 'redcap_jar', help = 'path to redcap jar for redcap interaction')
    parser.add_option("-s", "--truststore", help = "Truststore with SSL certs")
    parser.add_option("-p", "--truststore-password", help = "password for truststore")

    (options, args) = parser.parse_args()
    expected_project_mapping_filename = options.expected_project_mapping_filename
    temporary_directory_name = options.temporary_directory_name 
    redcap_jar = options.redcap_jar
    truststore = options.truststore
    truststore_password = options.truststore_password

    if not expected_project_mapping_filename or not temporary_directory_name:
        usage(parser)
    if not os.path.isfile(redcap_jar):
        print >> sys.stderr, 'error : ' + redcap_jar + ' is not a file.'
    if not os.path.isfile(expected_project_mapping_filename):
        print >> sys.stderr, 'error : ' + expected_project_mapping_filename + ' is not a file.'
        sys.exit(2)
    if not os.path.isdir(temporary_directory_name) or not os.access(temporary_directory_name, os.W_OK):
        print >> sys.stderr, 'error : ' + temporary_directory_name + ' is not a writable directory.'
        sys.exit(2)
    expected_project_mapping = get_expected_project_mapping(expected_project_mapping_filename)
    observed_project_mapping = get_observed_project_mapping(expected_project_mapping.keys(), temporary_directory_name, redcap_jar, truststore, truststore_password)
    compare_expected_to_observed(expected_project_mapping, observed_project_mapping)
    print 'redcap id mappings table matches what was expected'
    sys.exit(0)

if __name__ == '__main__':
    main()
