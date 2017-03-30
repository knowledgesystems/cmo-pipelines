#! /usr/bin/env python

import sys
import subprocess
import os
import getopt
import time
import csv
import re
from datetime import datetime

#import seaborn as sns

#import matplotlib.pyplot as plt
#import matplotlib.dates as mdate


# ------------------------------------------------------------------------------
# globals

# some file descriptors
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

CLINICAL_FILENAME = 'data_clinical.txt'
MUTATION_FILENAME = 'data_mutations_extended.txt'
CNA_FILENAME = 'data_CNA.txt'
SAMPLES_TITLE = 'Number of Samples in MSK-IMPACT Over Time'
SAMPLES_Y_LABEL = 'Number of Samples'
SAMPLES_X_LABEL = 'Date'
MUTATIONS_TITLE = 'Number of Mutations in MSK-IMPACT Over Time'
MUTATIONS_Y_LABEL = 'Number of Mutations'
MUTATIONS_X_LABEL = 'Date'
CNA_TITLE = 'Number of CNA Events in MSK-IMPACT Over Time'
CNA_Y_LABEL = 'Number of CNA Events'
CNA_X_LABEL = 'Date'
sample_date_map = {}
sample_patient_map = {}
sample_list = []
current_sample_list = []


def append_zero(number):
	""" Appends a leading 0 to a month/day/second string for uniformity """
	if len(number) == 1:
		return '0' + number
	return number

def create_date_dict(log_lines):

	""" 

	Iterates through the selected log lines, creating a dictionary of changesets:dates

	Then, figures out which changesets occured on the same dates, and filteres the original
	dictionary keeping only the latest changeset that occured in a day

	"""

	changeset_date_dict = {}
	
	for i,line in enumerate(log_lines):
		if 'date:' in line:
			changeset = log_lines[i-1].split(':')[1].strip()
			date = time.strptime(":".join(line.split(':')[1:]).split('-')[0].strip(), "%a %b %d %H:%M:%S %Y")
			date = time.strftime("%Y/%m/%d", date)
			#date = ''.join(map(append_zero, map(str, date[0:5])))
			changeset_date_dict[changeset] = date

	changesets_by_day = {}
	
	for cs, d in changeset_date_dict.iteritems():
		if d in changesets_by_day:
			changesets_by_day[d].append(cs)
		else:
			changesets_by_day[d] = [cs]

	# only take latest one in a day
	to_include = []
	
	for same_day_changesets in changesets_by_day.values():
		to_include.append(max(map(int, same_day_changesets)))

	

	filtered_changeset_date_dict = {int(k):v for (k, v) in changeset_date_dict.iteritems() if k in map(str,to_include)}

	return filtered_changeset_date_dict

def check_num_samples(last_num_samples, num_samples):
	""" Only needed if it is desired to filter days when the number of samples decreased """
	if last_num_samples < num_samples and last_num_samples != -1:
		return False
	return True

def get_num_cna_events(cna_file):
	""" 

	Gets the number of CNA events from a file 
	Only counts at deep deletions (-2) and amplificaitons (+2)

	""" 

	header = True
	num_events = 0
	for line in cna_file:
		if header:
			header = False
			continue
		data = line.strip().split('\t')[1:]
		for datum in data:
			# we don't care about anything that isn't a float
			try:
				if float(datum) != 0:
					num_events = num_events + 1
			except ValueError:
				continue
	return num_events

def map_samples_to_date(clin_file, date, first):
	
    """
    Goes through samples and assigns them the date.
    Because we are going from recent -> oldest, can just replace if
    the sample already has a date assigned
    """

    reader = csv.DictReader(clin_file, dialect='excel-tab')

    try:
        for line in reader:
            if 'DMP' not in line['SAMPLE_ID']:
                sample_date_map[line['SAMPLE_ID']] = date
                if line['SAMPLE_ID'] not in sample_patient_map:
                    sample_patient_map[line['SAMPLE_ID']] = line['PATIENT_ID']
                if first:
                    sample_list.append(line['SAMPLE_ID'])

    except Exception, e:
        print 'Something happened on: ' + date
        print str(e)
        return

def write_map_to_file(filtered_samples, hgrepo):

    fieldnames = ['PATIENT_ID', 'SAMPLE_ID', 'DATE_ADDED']
    output_file = open(os.path.join(hgrepo,'data_clinical_supp_date.txt'), 'w')
    writer = csv.DictWriter(output_file, fieldnames = fieldnames, dialect = 'excel-tab')
    writer.writeheader()
    for k, v in filtered_samples.iteritems():
        if k.strip() in current_sample_list:
            writer.writerow({'PATIENT_ID':sample_patient_map[k], 'SAMPLE_ID':k, 'DATE_ADDED':v})

def filter_samples():
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
    clin_file = open(clin_filename, 'rU')
    is_first = True
    for line in clin_file:
        if is_first:
            is_first = False
            continue
        current_sample_list.append(line.split('\t')[0].strip())        

def process_hg(changeset_date_dict, hgrepo):

    """ 

    Goes through every changeset of interest and reverts the files to that state.
    Then, the files are processed for that changeset and values are extracted and mapped to dates.

    """

    sample_date_dict = {}
    mut_date_dict = {}
    cna_date_dict = {}
    clin_filename = os.path.join(hgrepo,CLINICAL_FILENAME)
    mut_filename = os.path.join(hgrepo,MUTATION_FILENAME)
    cna_filename = os.path.join(hgrepo,CNA_FILENAME)
    last_num_samples = -1
    first = True

    make_sample_list(clin_filename)

    for cs, d in reversed(sorted(changeset_date_dict.items())):
        subprocess.call(['hg', 'revert', '-r', str(cs), clin_filename, '--cwd', hgrepo])
        #subprocess.call(['hg', 'revert', '-r', str(cs), mut_filename, '--cwd', hgrepo])
        #subprocess.call(['hg', 'revert', '-r', str(cs), cna_filename, '--cwd', hgrepo])
		
        try:
            clin_file = open(os.path.join(hgrepo, CLINICAL_FILENAME), 'rU')
            #mut_file = open(os.path.join(hgrepo, MUTATION_FILENAME), 'rU')
            #cna_file = open(os.path.join(hgrepo, CNA_FILENAME), 'rU')
        except IOError:
            print >> ERROR_FILE, 'failed at cs ' + str(cs)
            fix_repo(hgrepo)
            sys.exit(1)
		
        map_samples_to_date(clin_file, d, first)
        #num_samples = sum(1 for line in clin_file) - 1
        #num_mutations = sum(1 for line in mut_file) - 2
        #num_cna = get_num_cna_events(cna_file)

        #sample_date_dict[d] = num_samples
        #mut_date_dict[d] = num_mutations
        #cna_date_dict[d] = num_cna
		
        clin_file.close()
        #mut_file.close()
		#cna_file.close()

        first = False

    #create_over_time(sample_date_dict, title = SAMPLES_TITLE, y_label = SAMPLES_Y_LABEL, x_label = SAMPLES_X_LABEL)
    #create_over_time(mut_date_dict, title = MUTATIONS_TITLE, y_label = MUTATIONS_Y_LABEL, x_label = MUTATIONS_X_LABEL)
    #create_over_time(cna_date_dict, title = CNA_TITLE, y_label = CNA_Y_LABEL, x_label = CNA_X_LABEL)
    fix_repo(hgrepo)
    filtered_samples = filter_samples()
    write_map_to_file(filtered_samples, hgrepo)


def create_over_time(sample_date_dict, title = '', y_label = '', x_label = ''):
	""" Creates plot for number of events/items over time """
	x = []
	y = []

	# this converts the date to epoch.
	#fix_date = lambda x: int(datetime(int(x[0:-4][0:4]), int(x[0:-4][4:6]), int(x[0:-4][6:8])).strftime('%s'))

	for k, v in sorted(sample_date_dict.items()):
		x.append(k)
		y.append(time.strftime('%s', v))

	#sns.set_style('whitegrid')
	fig, ax = plt.subplots()
	ax.plot_date(mdate.epoch2num(x),y)
	date_fmt = '%b-%Y'
	date_formatter = mdate.DateFormatter(date_fmt)
	ax.xaxis.set_major_formatter(date_formatter)

	fig.autofmt_xdate()
	plt.xlabel(x_label)
	plt.ylabel(y_label)
	plt.title(title)
	#if set_range:
	#	lower = y[0]
	#	upper = y[-1]
	#	plt.ylim([lower,upper])
	plt.show()

def make_graphs(hgrepo):

	""" 

	Function which facilitates creation of the timeline information
	
	Parses the mercurial log information for changesets and dates
	and calls the functions to process this data.

	Returns dictionary containing dates as keys, number of samples as values

	"""
	#subprocess.call(['hg', 'pull', '--cwd', hgrepo])
	fix_repo(hgrepo)
	changesets = subprocess.check_output(['hg','identify', '--cwd', hgrepo, '--num'])
	log = subprocess.check_output(['hg','log', '--cwd', hgrepo])

	log_lines = log.split('\n')
	log_lines = [l for l in log_lines if 'changeset:' in l or 'date:' in l]

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

	# make the graphs
	make_graphs(hgrepo)

# do it up
if __name__ == '__main__':
	main()

