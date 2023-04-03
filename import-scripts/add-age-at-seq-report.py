import sys
import optparse
import csv
import os
import math
from datetime import datetime

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# globals
AGE_AT_SEQ_REPORT_FIELD = 'AGE_AT_SEQ_REPORT'
AGE_FIELD = 'AGE'
SEQ_DATE_FIELD = 'SEQ_DATE'
AVERAGE_DAYS_PER_YEAR = 365.2422

PATIENT_SAMPLE_MAP = {}
SAMPLE_SEQ_DATE_MAP = {}
PATIENT_AGE = {}


def load_sample_seq_date(seq_date_file, convert_to_days):
	""" Loads SEQ_DATE from seq date file provided. """
	data_file = open(seq_date_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')

	for line in data_reader:
		# skip samples not found in the clinical file
		if not line['SAMPLE_ID'] in PATIENT_SAMPLE_MAP.keys():
			continue
		seq_date = datetime.strptime(line['SEQ_DATE'], '%a, %d %b %Y %H:%M:%S %Z')
		difference_in_years = int(math.ceil(((datetime.now() - seq_date).days)/AVERAGE_DAYS_PER_YEAR))
		try:
			age_at_seq_report_value = (PATIENT_AGE[line['PATIENT_ID']] - difference_in_years)
			if convert_to_days:
				age_at_seq_report_value = age_at_seq_report_value*AVERAGE_DAYS_PER_YEAR
			SAMPLE_SEQ_DATE_MAP[line['SAMPLE_ID']] = str(int(math.ceil(age_at_seq_report_value)))
		except KeyError:
			print "Patient age not found for '" + line['PATIENT_ID'] + "'"
			SAMPLE_SEQ_DATE_MAP[line['SAMPLE_ID']] = 'NA'


def load_patient_age(age_file):
	""" Loads AGE from age file. """
	data_file = open(age_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')

	for line in data_reader:
		# skip records not found in clinical file 
		if not line['PATIENT_ID'] in PATIENT_SAMPLE_MAP.values():
			continue
		PATIENT_AGE[line['PATIENT_ID']] = int(line['AGE'])


def load_patient_sample_mapping(clinical_file):
	""" Loads patient-sample mapping. """
	data_file = open(clinical_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		PATIENT_SAMPLE_MAP[line['SAMPLE_ID']] = line['PATIENT_ID']
	data_file.close()


def add_age_at_seq_report(clinical_file):
	header = get_file_header(clinical_file)

	# add age at seq report column in not already present
	if not AGE_AT_SEQ_REPORT_FIELD in header:
		header.append(AGE_AT_SEQ_REPORT_FIELD)
	output_data = ['\t'.join(header)]

	data_file = open(clinical_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		line[AGE_AT_SEQ_REPORT_FIELD] = SAMPLE_SEQ_DATE_MAP[line['SAMPLE_ID']]
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
	print >> OUTPUT_FILE, "add-age-at-seq-report.py --clinical-file [path/to/clinical/file] --seq-date-file [path/to/seq/date/file] --age-file [path/to/age/file] --convert-to-days [true|false]"
	sys.exit(2)

def main():
	# get command line stuff
	parser = optparse.OptionParser()
	parser.add_option('-f', '--clinical-file', action = 'store', dest = 'clinfile')
	parser.add_option('-s', '--seq-date-file', action = 'store', dest = 'seqdatefile')
	parser.add_option('-a', '--age-file', action = 'store', dest = 'agefile')
	parser.add_option('-c', '--convert-to-days', action = 'store', dest = 'convertdays')

	(options, args) = parser.parse_args()
	clinical_file = options.clinfile
	seq_date_file = options.seqdatefile
	age_file = options.agefile
	convert_to_days_flag = options.convertdays


	if not clinical_file or not seq_date_file or not age_file:
		print >> ERROR_FILE, "Clinical file, seq date file, and age file must be provided."
		usage()

	if not os.path.exists(clinical_file):
		print >> ERROR_FILE, "No such file: " + clinical_file
		usage()

	if not os.path.exists(seq_date_file):
		print >> ERROR_FILE, "No such file: " + seq_date_file
		uage()

	if not os.path.exists(age_file):
		print >> ERROR_FILE, "No such file: " + age_file
		usage()

	# validate file headers
	validate_file_header(seq_date_file, SEQ_DATE_FIELD)
	validate_file_header(age_file, AGE_FIELD)

	convert_to_days = False
	if convert_to_days_flag != None and convert_to_days_flag != '' and convert_to_days_flag.lower() == 'true':
		convert_to_days = True

	load_patient_sample_mapping(clinical_file)
	load_patient_age(age_file)
	load_sample_seq_date(seq_date_file, convert_to_days)
	add_age_at_seq_report(clinical_file)


if __name__ == '__main__':
	main()
