#!/usr/bin/env python

"""
This script generates and writes the AGE_AT_SEQ_REPORT attribute to a given clinical file.
This attribute represents the age, in days, of a given patient on the date that their sample
was sequenced. This value is calculated by reading the AGE_AT_SEQ_REPORTED_YEARS attribute from
the clinical sample file and converting its value to days. This is functionality is primarily
used for the Genie dataset.

Usage:
	python add-age-at-seq-report.py \
		--clinical-output-file [path/to/clinical/output/file] \
		--clinical-sample-file [path/to/clinical/sample/file] \
		--convert-to-days [true|false]
"""

import csv
import math
import optparse
import os
import sys

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# Global variables
PATIENT_ID_FIELD = 'PATIENT_ID'
SAMPLE_ID_FIELD = 'SAMPLE_ID'
AGE_AT_SEQ_REPORTED_YEARS_FIELD = 'AGE_AT_SEQ_REPORTED_YEARS'
AGE_AT_SEQ_REPORT_FIELD = 'AGE_AT_SEQ_REPORT'
AVERAGE_DAYS_PER_YEAR = 365.25

PATIENT_SAMPLE_MAP = {}
SAMPLE_AGE_AT_SEQ_REPORT_MAP = {}


def add_age_at_seq_report(clinical_output_file):
	""" Writes AGE_AT_SEQ_REPORT attribute to the clinical output file. """
	# Add AGE_AT_SEQ_REPORT column to header if not already present
	header = get_file_header(clinical_output_file)
	if not AGE_AT_SEQ_REPORT_FIELD in header:
		header.append(AGE_AT_SEQ_REPORT_FIELD)
	output_data = ['\t'.join(header)]

	# Populate AGE_AT_SEQ_REPORT column
	data_file = open(clinical_output_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		line[AGE_AT_SEQ_REPORT_FIELD] = SAMPLE_AGE_AT_SEQ_REPORT_MAP[line[SAMPLE_ID_FIELD]]
		formatted_data = map(lambda x: line.get(x, ''), header)
		output_data.append('\t'.join(formatted_data))
	data_file.close()

	# Write data to file
	output_file = open(clinical_output_file, 'w')
	output_file.write('\n'.join(output_data))
	output_file.close()
	print 'Finished adding age at seq report'


def read_clinical_sample_file(clinical_sample_file):
    """ Generator function that yields all non-commented lines from the clinical sample file. """
    with open(clinical_sample_file, 'rU') as f:
        for line in f:
            if line.startswith('#'):
                continue
            yield line


def load_age_at_seq_reported_years(clinical_sample_file, convert_to_days):
	""" Loads AGE_AT_SEQ_REPORTED_YEARS from clinical sample file and converts it to days. """
	data_reader = csv.DictReader(read_clinical_sample_file(clinical_sample_file), dialect = 'excel-tab')
	for line in data_reader:
		sample_id = line[SAMPLE_ID_FIELD]

		# Skip samples not found in the clinical file
		if not sample_id in PATIENT_SAMPLE_MAP.keys():
			continue
		age_at_seq_reported_years = line[AGE_AT_SEQ_REPORTED_YEARS_FIELD].strip()

		# Handle values beginning with < or >
		first_char = ''
		if age_at_seq_reported_years[0] == '<' or age_at_seq_reported_years[0] == '>':
			first_char = age_at_seq_reported_years[0]
			age_at_seq_reported_years = age_at_seq_reported_years[1:]

		# Convert AGE_AT_SEQ_REPORTED_YEARS to days and store the value
		# Default to NA if type conversion fails
		# Note that we are performing the reverse of this calculation to keep the age at seq year value consistent:
		# https://github.com/Sage-Bionetworks/Genie/blob/53db39ccb6e423743df49c1f0e115c9a7576af69/genie/database_to_staging.py#L1133-L1138
		try:
			age_at_seq_report_value = int(age_at_seq_reported_years)
			if convert_to_days:
				age_at_seq_report_value = age_at_seq_report_value * AVERAGE_DAYS_PER_YEAR
			SAMPLE_AGE_AT_SEQ_REPORT_MAP[sample_id] = first_char + str(int(math.ceil(age_at_seq_report_value)))
		except ValueError:
			print AGE_AT_SEQ_REPORTED_YEARS_FIELD + " not found for '" + sample_id + "'"
			SAMPLE_AGE_AT_SEQ_REPORT_MAP[sample_id] = 'NA'


def load_patient_sample_mapping(clinical_output_file):
	""" Loads patient-sample mapping. """
	data_file = open(clinical_output_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		PATIENT_SAMPLE_MAP[line[SAMPLE_ID_FIELD]] = line[PATIENT_ID_FIELD]
	data_file.close()


def get_file_header(filename):
	""" Returns the file header. """
	data_file = open(filename, 'rU')
	filedata = [x for x in data_file.readlines() if not x.startswith('#')]
	header = map(str.strip, filedata[0].split('\t'))
	data_file.close()
	return header


def validate_file_header(filename, key_columns):
	""" Validates that the key column exists in the file header. """
	missing_columns = []
	header = get_file_header(filename)

	for column in key_columns:
		if not column in header:
			missing_columns.append(column)

	if missing_columns:
		print >> ERROR_FILE, "Could not find key column(s) '" + ', '.join(missing_columns) + "' in file header for: " + filename + "! Please make sure column(s) exist before running script."
		usage()


def usage():
	print >> OUTPUT_FILE, "add-age-at-seq-report.py --clinical-output-file [path/to/clinical/output/file] --clinical-sample-file [path/to/clinical/sample/file] --convert-to-days [true|false]"
	sys.exit(2)


def main():
	# Get command line stuff
	parser = optparse.OptionParser()
	parser.add_option('-f', '--clinical-output-file', action = 'store', dest = 'clinfile')
	parser.add_option('-s', '--clinical-sample-file', action = 'store', dest = 'samplefile')
	parser.add_option('-c', '--convert-to-days', action = 'store', dest = 'convertdays')

	(options, args) = parser.parse_args()
	clinical_output_file = options.clinfile
	clinical_sample_file = options.samplefile
	convert_to_days_flag = options.convertdays

	if not clinical_output_file or not clinical_sample_file:
		print >> ERROR_FILE, "Clinical output file and clinical sample file must be provided."
		usage()

	if not os.path.exists(clinical_output_file):
		print >> ERROR_FILE, "No such file: " + clinical_output_file
		usage()

	if not os.path.exists(clinical_sample_file):
		print >> ERROR_FILE, "No such file: " + clinical_sample_file
		usage()

	# Validate file headers
	validate_file_header(clinical_output_file, [PATIENT_ID_FIELD, SAMPLE_ID_FIELD])
	validate_file_header(clinical_sample_file, [AGE_AT_SEQ_REPORTED_YEARS_FIELD, SAMPLE_ID_FIELD])

	convert_to_days = False
	if convert_to_days_flag != None and convert_to_days_flag != '' and convert_to_days_flag.lower() == 'true':
		convert_to_days = True

	load_patient_sample_mapping(clinical_output_file)
	load_age_at_seq_reported_years(clinical_sample_file, convert_to_days)
	add_age_at_seq_report(clinical_output_file)


if __name__ == '__main__':
	main()
