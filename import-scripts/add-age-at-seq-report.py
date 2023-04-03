#!/usr/bin/env python

import sys
import optparse
import csv
import os
import math

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# globals
AGE_AT_SEQ_REPORT_FIELD = 'AGE_AT_SEQ_REPORT'
AGE_AT_SEQ_REPORTED_YEARS_FIELD = 'AGE_AT_SEQ_REPORTED_YEARS'
AVERAGE_DAYS_PER_YEAR = 365.2422

PATIENT_SAMPLE_MAP = {}
SAMPLE_AGE_AT_SEQ_REPORT_MAP = {}


def read_sample_file(sample_file):
    """ Generator function that yields all non-commented lines from the clinical sample file. """
    with open(sample_file, 'rU') as f:
        for line in f:
            if line.startswith('#'):
                continue
            yield line


def load_age_at_seq_reported_years(sample_file, convert_to_days):
	""" Loads AGE_AT_SEQ_REPORTED_YEARS from clinical sample file and converts it to days. """

	data_reader = csv.DictReader(read_sample_file(sample_file), dialect = 'excel-tab')
	for line in data_reader:
		sample_id = line['SAMPLE_ID']
		# skip samples not found in the clinical file
		if not sample_id in PATIENT_SAMPLE_MAP.keys():
			continue
		age_at_seq_reported_years = line[AGE_AT_SEQ_REPORTED_YEARS_FIELD]
		try:
			if convert_to_days:
				age_at_seq_report_value = int(age_at_seq_reported_years) * AVERAGE_DAYS_PER_YEAR
			SAMPLE_AGE_AT_SEQ_REPORT_MAP[sample_id] = str(int(math.floor(age_at_seq_report_value)))
		except ValueError:
			print AGE_AT_SEQ_REPORTED_YEARS_FIELD + " not found for '" + sample_id + "'"
			SAMPLE_AGE_AT_SEQ_REPORT_MAP[sample_id] = 'NA'


def load_patient_sample_mapping(clinical_file):
	""" Loads patient-sample mapping. """
	data_file = open(clinical_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		PATIENT_SAMPLE_MAP[line['SAMPLE_ID']] = line['PATIENT_ID']
	data_file.close()


def add_age_at_seq_report(clinical_file):
	header = get_file_header(clinical_file)

	# add age at seq report column if not already present
	if not AGE_AT_SEQ_REPORT_FIELD in header:
		header.append(AGE_AT_SEQ_REPORT_FIELD)
	output_data = ['\t'.join(header)]

	data_file = open(clinical_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		line[AGE_AT_SEQ_REPORT_FIELD] = SAMPLE_AGE_AT_SEQ_REPORT_MAP[line['SAMPLE_ID']]
		formatted_data = map(lambda x: line.get(x, ''), header)
		output_data.append('\t'.join(formatted_data))
	data_file.close()
	output_file = open(clinical_file, 'w')
	output_file.write('\n'.join(output_data))
	output_file.close()
	print 'Finished adding age at seq report'


def get_file_header(filename):
	""" Returns the file header. """
	data_file = open(filename, 'rU')
	filedata = [x for x in data_file.readlines() if not x.startswith('#')]
	header = map(str.strip, filedata[0].split('\t'))
	data_file.close()
	return header


def validate_file_header(filename, key_column):
	""" Validates that the key column exists in the file header. """
	if not key_column in get_file_header(filename):
		print >> ERROR_FILE, "Could not find key column '" + key_column + "' in file header for: " + filename + "! Please make sure this column exists before running script."
		usage()


def usage():
	print >> OUTPUT_FILE, "add-age-at-seq-report.py --clinical-file [path/to/clinical/file] --sample-file [path/to/sample/file] --convert-to-days [true|false]"
	sys.exit(2)


def main():
	# get command line stuff
	parser = optparse.OptionParser()
	parser.add_option('-f', '--clinical-file', action = 'store', dest = 'clinfile')
	parser.add_option('-s', '--sample-file', action = 'store', dest = 'samplefile')
	parser.add_option('-c', '--convert-to-days', action = 'store', dest = 'convertdays')

	(options, args) = parser.parse_args()
	clinical_file = options.clinfile
	sample_file = options.samplefile
	convert_to_days_flag = options.convertdays

	if not clinical_file or not sample_file:
		print >> ERROR_FILE, "Clinical file and sample file must be provided."
		usage()

	if not os.path.exists(clinical_file):
		print >> ERROR_FILE, "No such file: " + clinical_file
		usage()

	if not os.path.exists(sample_file):
		print >> ERROR_FILE, "No such file: " + sample_file
		usage()

	# validate file headers
	validate_file_header(sample_file, AGE_AT_SEQ_REPORTED_YEARS_FIELD)

	convert_to_days = False
	if convert_to_days_flag != None and convert_to_days_flag != '' and convert_to_days_flag.lower() == 'true':
		convert_to_days = True

	load_patient_sample_mapping(clinical_file)
	load_age_at_seq_reported_years(sample_file, convert_to_days)
	add_age_at_seq_report(clinical_file)


if __name__ == '__main__':
	main()
