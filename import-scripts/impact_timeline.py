#! /usr/bin/env python

import sys
import subprocess
import os
import getopt
import time
import csv
import re
import datetime as dt
from datetime import datetime

# ------------------------------------------------------------------------------
# globals

# some file descriptors
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

CLINICAL_FILENAME = 'data_clinical.txt'
sample_date_map = {}
sample_patient_map = {}
sample_list = []
current_sample_list = []

def create_date_dict(log_lines):

	""" 

	Iterates through the selected log lines, creating a dictionary of changesets:dates

	Then, figures out which changesets occured on the same dates, and filters the original
	dictionary keeping only the latest changeset that occured in a day

	Example log output:
		changeset:   0:51e4f8d10633
		user:        Benjamin Gross <benjamin.gross@gmail.com>
		date:        Wed Jan 02 22:05:18 2013 -0500
		summary:     start of cvs migration. brca-bccrc move
	"""
	
	changesets_by_day = {}
	for i,line in enumerate(log_lines):
		if 'date:' in line:
			changeset = int(log_lines[i-1].split(':')[1].strip())
			date_str = ":".join(line.split(':')[1:]).split('-')[0].strip()  # parses out the date (ex: Wed Jan 02 22:05:18 2013)
			date = time.strptime(date_str, "%a %b %d %H:%M:%S %Y") # creates struct_time object
			date = time.strftime("%Y/%m/%d", date) # formats date to YYYY/MM/DD (ex: 2013/01/02)

			# organize changesets by date
			if date in changesets_by_day:
				changesets_by_day[date].append(changeset)
			else:
				changesets_by_day[date] = [changeset]

	# only take latest changeset in a day
	filtered_changeset_date_dict = {}
	for date,same_day_changesets in changesets_by_day.items():
		latest_changeset = max(map(int, same_day_changesets))
		filtered_changeset_date_dict[latest_changeset] = date

	return filtered_changeset_date_dict

def map_samples_to_date(clin_file, date, first):
	
    """
    Goes through samples and assigns them the date.
    Because we are going from recent -> oldest, can just replace if
    the sample already has a date assigned
    """

    reader = csv.DictReader(clin_file, dialect='excel-tab')
    try:
		for line in reader:
			if 'DMP' not in line['SAMPLE_ID'].strip():
				sample_date_map[line['SAMPLE_ID'].strip()] = date
				if line['SAMPLE_ID'].strip() not in sample_patient_map:
					sample_patient_map[line['SAMPLE_ID'].strip()] = line['PATIENT_ID'].strip()
				
				# first refers to the latest checked in data_clinical.txt - only the samples in latest data_clinical.txt get added to this list
				if first:
					sample_list.append(line['SAMPLE_ID'].strip())
    except Exception, e:
        print >> OUTPUT_FILE, 'Something happened on: ' + date
        print >> OUTPUT_FILE, str(e)
        return

def write_map_to_file(filtered_samples, hgrepo):

    fieldnames = ['PATIENT_ID', 'SAMPLE_ID', 'DATE_ADDED', 'MONTH_ADDED', 'WEEK_ADDED']
    output_file = open(os.path.join(hgrepo,'data_clinical_supp_date.txt'), 'wb')
    writer = csv.DictWriter(output_file, fieldnames = fieldnames, dialect = 'excel-tab', lineterminator = '\n')
    writer.writeheader()
    for k, v in filtered_samples.iteritems():
        if k in current_sample_list:
            date_parts = v.split('/')
            month_date = date_parts[0] + '/' + date_parts[1]
            week_date = date_parts[0] + ', Wk. ' + dt.date(int(date_parts[0]), int(date_parts[1]), int(date_parts[2])).strftime('%V')
            writer.writerow({'PATIENT_ID':sample_patient_map[k], 'SAMPLE_ID':k, 'DATE_ADDED':v, 'MONTH_ADDED':month_date, 'WEEK_ADDED':week_date})

def filter_samples():
	""" Filters the sample-date dict using the latest data_clinical.txt """
	filtered_samples = {}
	for sample, date in sample_date_map.iteritems():
		if sample in sample_list:
			filtered_samples[sample] = date
	return filtered_samples

def get_patient_id(id):
    """ Get patient ids from sublist (MSKIMPACT ONLY) """
    patientid = ""
    p = re.compile('(P-\d*)-T\d\d-IM\d')
    match = p.match(id)		
    if match:
        patientid = match.group(1)
    else:
        patientid = id
    return patientid

def make_sample_list(clin_filename):
	""" Generates the current sample list from latest data_clinical.txt """
	clin_file = open(clin_filename, 'rU')
	clin_reader = csv.DictReader(clin_file, dialect = 'excel-tab')
	for line in clin_reader:
		current_sample_list.append(line['SAMPLE_ID'].strip())
	clin_file.close()

def process_hg(changeset_date_dict, hgrepo):

    """ 

    Goes through every changeset of interest and reverts the files to that state.
    Then, the files are processed for that changeset and values are extracted and mapped to dates.

    """
    clin_filename = os.path.join(hgrepo,CLINICAL_FILENAME)
    first = True

    make_sample_list(clin_filename)

    # sorts dict from latest --> earliest changeset
    # this order is chosen so that we only write date information for samples we currently have checked in 
    # that way we do not end up including data for samples that have had their ID's changed 
    # or whose data have been redacted at an earlier date
    for changeset, date in reversed(sorted(changeset_date_dict.items())):
    	# reverts clinical file to specificied changeset 
        subprocess.call(['hg', 'revert', '-r', str(changeset), clin_filename, '--cwd', hgrepo, '--no-backup'])
        if not os.path.exists(clin_filename):
        	continue
        try:
            clin_file = open(os.path.join(hgrepo, CLINICAL_FILENAME), 'rU')
        except IOError:
            print >> ERROR_FILE, 'failed at changeset ' + str(changeset)
            fix_repo(hgrepo)
            sys.exit(1)
		
        map_samples_to_date(clin_file, date, first)
        clin_file.close()
        first = False

    fix_repo(hgrepo)
    filtered_samples = filter_samples()
    write_map_to_file(filtered_samples, hgrepo)

def generate_impact_date_added_data(hgrepo):

	""" 

	Function which facilitates creation of the timeline information
	
	Parses the mercurial log information for changesets and dates
	and calls the functions to process this data.

	Returns dictionary containing dates as keys, number of samples as values

	"""
	fix_repo(hgrepo)
	changesets = subprocess.check_output(['hg','identify', '--cwd', hgrepo, '--num'])
	print >> OUTPUT_FILE, 'Extracting date added information from ' + changesets.strip() + ' changesets'	
	
	# get logs for dmp repo
	log = subprocess.check_output(['hg','log', '--cwd', hgrepo])
	log_lines = [l for l in log.split('\n') if 'changeset:' in l or 'date:' in l]

	changeset_date_dict = create_date_dict(log_lines)
	process_hg(changeset_date_dict, hgrepo)

def fix_repo(hgrepo):
	""" resets the hg repo """
	subprocess.call(['hg', 'revert', '--cwd', hgrepo, '--all'])

def usage():
	print >> OUTPUT_FILE, 'impact_timeline.py --hgrepo <path/to/repository>'

def main():

	""" 

	Main function

	"""
	try:
		opts, args = getopt.getopt(sys.argv[1:], '', ['hgrepo='])
	except getopt.error, msg:
		print >> ERROR_FILE, msg
		usage()
		sys.exit(2)

	hgrepo = ''
	# get the input
	for o, a in opts:
		if o == '--hgrepo':
			hgrepo = a

	# check the input
	if not os.path.isdir(hgrepo):
		print >> ERROR_FILE, 'not a directory'
		usage()
		sys.exit(2)

	# generate the impact date added data
	generate_impact_date_added_data(hgrepo)

# do it up
if __name__ == '__main__':
	main()

