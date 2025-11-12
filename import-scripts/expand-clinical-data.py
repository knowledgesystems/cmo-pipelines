import sys
import optparse
import csv
import os
import re

ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

SUPPLEMENTAL_CLINICAL_DATA = {}
FILTERED_SAMPLE_IDS = set()
IMPACT_SAMPLE_PATTERN = re.compile('P-\d*-T\d\d-IM\d*')
IMPACT_PATIENT_PATTERN = re.compile('P-\d*')

def expand_clinical_data_main(clinical_filename, fields, impact_data_only, identifier_column_name):
	""" Loads supplemental clinical data from data_clinical.txt and writes data to output directory. """
	# add supp fields to header if not already exists
	header = get_file_header(clinical_filename)
	for supp_field in fields:
		if not supp_field in header:
			header.append(supp_field)

	# load data from clinical_filename and write data to output directory
	data_file = open(clinical_filename, 'rU')
	data_reader = [line for line in data_file.readlines() if not line.startswith('#')][1:]
	output_data = ['\t'.join(header)]
	for line in data_reader:
		line = dict(zip(header, map(str.strip, line.split('\t'))))
		if impact_data_only and not is_impact_sample_or_patient(line[identifier_column_name].strip()):
			continue
		# update line with supplemental sample clinical data and format data as string for output file
		line.update(SUPPLEMENTAL_CLINICAL_DATA.get(line[identifier_column_name].strip(), {}))
		data = map(lambda v: line.get(v,''), header)
		output_data.append('\t'.join(data))
	data_file.close()	

	# write data to output file
	output_file = open(clinical_filename, 'w')
	output_file.write('\n'.join(output_data))
	output_file.close()
	print >> OUTPUT_FILE, 'Updated clinical file `' + clinical_filename + '` with supp fields: ' + ', '.join(fields)

def load_supplemental_clinical_data(supplemental_clinical_filename, supplemental_fields, study_id, identifier_column_name):
	""" Loads supplemental clinical data from supplemental_clinical_filename into SUPPLEMENTAL_CLINICAL_DATA. """
	header = get_file_header(supplemental_clinical_filename)
	data_file = open(supplemental_clinical_filename, 'rU')
	data_reader = [line for line in data_file.readlines() if not line.startswith('#')][1:]
	for line in data_reader:
		line = dict(zip(header, map(str.strip, line.split('\t'))))
		if study_id == 'genie' and identifier_column_name == 'SAMPLE_ID':
			normalize_genie_sample_type(line)
		SUPPLEMENTAL_CLINICAL_DATA[line[identifier_column_name].strip()] = dict({(k,v) for k,v in line.items() if k in supplemental_fields})
	data_file.close()

def normalize_genie_sample_type(data):
	try:
		if data['SAMPLE_TYPE'].strip() == 'Metastasis':
			data['SAMPLE_TYPE'] = '4'
		elif data['SAMPLE_TYPE'].strip() == 'Primary':
			data['SAMPLE_TYPE'] = '1'
	except KeyError:
		print >> ERROR_FILE, "No SAMPLE_TYPE column detected, cannot normalize genie sample type"
	return data

def is_impact_sample_or_patient(case_identifier):
	""" Determine whether sample id is from IMPACT """
	if IMPACT_SAMPLE_PATTERN.match(case_identifier) or IMPACT_PATIENT_PATTERN.match(case_identifier):
		return True
	return False

def get_file_header(filename):
	""" Returns the file header. """
	data_file = open(filename, 'rU')
	filedata = [x for x in data_file.readlines() if not x.startswith('#')]
	header = map(str.strip, filedata[0].split('\t'))
	data_file.close()
	return header

def usage():
	print >> OUTPUT_FILE, "expand-clinical-data.py --study-id [study id] --clinical-file [path to clinical file] --clinical-supp-file [path to clinical supp file] --fields [comma-delimited list of fields to expand the clinical file] --identifier-column-name [PATIENT_ID|SAMPLE_ID]"
	sys.exit(2)

def main():
	# get command line stuff
	parser = optparse.OptionParser()
	parser.add_option('-c', '--clinical-file', action = 'store', dest = 'clinfile')
	parser.add_option('-s', '--clinical-supp-file', action = 'store', dest = 'clinsuppfile')
	parser.add_option('-f', '--fields', action = 'store', dest = 'fields')
	parser.add_option('-i', '--study-id', action = 'store', dest = 'studyid')
	parser.add_option('-d', '--identifier-column-name', action = 'store', dest = 'idcolumn')

	(options, args) = parser.parse_args()
	clin_file = options.clinfile
	clin_supp_file = options.clinsuppfile
	fields = options.fields.split(',')
	study_id = options.studyid
	identifier_column_name = options.idcolumn

	# check arguments
	if not clin_file or not clin_supp_file or not fields or not study_id or not identifier_column_name:
		print >> ERROR_FILE, "Clinical file, clinical supp file, fields, study id, and identifier column name must be provided!"
		usage()
	if not os.path.exists(clin_file):
		print >> ERROR_FILE, "No such file: " + clin_file
		usage()
	if not os.path.exists(clin_supp_file):
		print >> ERROR_FILE, "No such file: " + clin_supp_file
		usage()
	if identifier_column_name != 'SAMPLE_ID' and identifier_column_name != 'PATIENT_ID':
		print >> ERROR_FILE, "Identifier column name provided must equal PATIENT_ID or SAMPLE_ID"
		usage()

	impact_data_only = False
	if study_id == 'genie':
		impact_data_only = True

	# load supplemental data from the clinical supp file
	load_supplemental_clinical_data(clin_supp_file, fields, study_id, identifier_column_name)
	expand_clinical_data_main(clin_file, fields, impact_data_only, identifier_column_name)



if __name__ == '__main__':
	main()
