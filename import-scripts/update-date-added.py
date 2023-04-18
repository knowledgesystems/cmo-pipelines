import os
import sys
import csv
import optparse
from datetime import datetime, date

DATE_ADDED_FILE_HEADER = ['SAMPLE_ID', 'PATIENT_ID', 'MONTH_ADDED', 'WEEK_ADDED']
DATE_ADDED_DATA = {}
EXISTING_SAMPLES = set()
EXISTING_DATE_ADDED_DATA = {}
NEW_SAMPLE_DATA = {}


def update_date_added(date_added_file):
	""" Updates the clinical timeline file with data for new samples. """
	output_file = open(date_added_file, 'w')
	output_file.write('\t'.join(DATE_ADDED_FILE_HEADER))

	# skip samples that are not in the current samples list
	for sample_id,sample_data in EXISTING_DATE_ADDED_DATA.items():
		if not sample_id in EXISTING_SAMPLES:
			continue
		formatted_data = map(lambda x: sample_data.get(x,''), DATE_ADDED_FILE_HEADER)
		output_file.write('\n' + '\t'.join(formatted_data))	

	# start with new samples first
	for sample_id,sample_data in NEW_SAMPLE_DATA.items():
		formatted_data = map(lambda x: sample_data.get(x,''), DATE_ADDED_FILE_HEADER)
		output_file.write('\n' + '\t'.join(formatted_data))
		
	output_file.close()
	print 'Finished updating date added file:', date_added_file

def load_current_samples(clinical_file):
	""" 
		Loads sample and patient ids from data_clinical.txt. 
		Updates NEW_SAMPLE_DATA dict with id and date added info for each sample.
	"""
	data_file = open(clinical_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		# want to keep track of samples currently checked in to the clinical file
		# so that the samples in the clinical file and the date added file are the same
		EXISTING_SAMPLES.add(line['SAMPLE_ID'])
		# only want to update date added info for new samples
		if line['SAMPLE_ID'] in EXISTING_DATE_ADDED_DATA.keys():
			continue
		NEW_SAMPLE_DATA[line['SAMPLE_ID']] = {'SAMPLE_ID':line['SAMPLE_ID'], 'PATIENT_ID':line['PATIENT_ID']}
		NEW_SAMPLE_DATA[line['SAMPLE_ID']].update(DATE_ADDED_DATA)
	data_file.close()

def load_existing_added_data(date_added_file):
	""" Loads existing date added info. """
	data_file = open(date_added_file, 'rU')
	data_reader = csv.DictReader(data_file, dialect = 'excel-tab')
	for line in data_reader:
		EXISTING_DATE_ADDED_DATA[line['SAMPLE_ID']] = line
	data_file.close()

def generate_todays_date_added_data():
	""" Formats today's date into MONTH_ADDED AND WEEK_ADDED attributes. """
	now = datetime.now()
	month_added = datetime.strftime(now, '%Y/%m')

	# handle special cases where samples added to the portal in late december or early
	# january fall into week #53 territory
	week_number = date(now.year, now.month, now.day).strftime('%V')
	if int(week_number) == 53:
		if now.month == 12:
			week_added = str(now.year) + ', Wk. 52'
		elif now.month == 1:
			week_added = str(now.year) + ', Wk. 01'
	else:
		week_added = str(now.year) + ', Wk. ' + date(now.year, now.month, now.day).strftime('%V')
	DATE_ADDED_DATA.update({'MONTH_ADDED':month_added, 'WEEK_ADDED':week_added})

def usage():
	print 'python update-date-added.py --clinical-file [path/to/clinical/file] --date-added-file [path/to/date/added/file]'
	sys.exit(2)

def main():
	# get command line stuff
	parser = optparse.OptionParser()
	parser.add_option('-d', '--date-added-file', action = 'store', dest = 'dataddedfile')
	parser.add_option('-c', '--clinical-file', action = 'store', dest = 'clinfile')

	(options, args) = parser.parse_args()
	date_added_file = options.dataddedfile
	clinical_file = options.clinfile

	if not date_added_file and not clinical_file:
		print 'Clincal file and date added file must be provided!'
		usage()
	if not os.path.exists(clinical_file):
		print 'No such file:', clinical_file
		usage()
	if not os.path.exists(date_added_file):
		print 'No such file:', date_added_file
		usage()

	generate_todays_date_added_data()
	load_existing_added_data(date_added_file)
	load_current_samples(clinical_file)
	update_date_added(date_added_file)


if __name__ == '__main__':
	main()
