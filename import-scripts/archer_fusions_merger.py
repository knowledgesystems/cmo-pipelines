import sys
import os
import argparse
import csv
import re
import fileinput
import smtplib

from clinicalfile_utils import *
from email.Utils import COMMASPACE, formatdate
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

# some file descriptors
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

SMTP_SERVER = "smtp.gmail.com"
MESSAGE_RECIPIENTS = ["cbioportal-dmp-operations@cbioportal.org", "cbioportal-pipelines@cbioportal.org"]
MESSAGE_SENDER = "cbioportal@cbioportal.org"

## remove from mixedpact so as to not double count
KNOWN_SAMPLE_MASTERLIST = set()
LINKED_ARCHER_CASES = {}
SEEN_FUSION_EVENTS = set()
SAMPLES_MISSING_CLINICAL_DATA = set()

MSKIMPACT_STUDY_ID = "mskimpact"
HEMEPACT_STUDY_ID = "mskimpact_heme"
IMPACT_SAMPLE_PATTERN = re.compile('(P-\d*-T\d\d)-IM\S\d*')
HEME_SAMPLE_PATTERN = re.compile('(P-\d*-T\d\d)-IH\S\d*')

STUDY_SAMPLE_REGEX_PATTERNS = {
	MSKIMPACT_STUDY_ID:IMPACT_SAMPLE_PATTERN,
	HEMEPACT_STUDY_ID:HEME_SAMPLE_PATTERN
}

def load_study_sample_masterlist(clinical_filename):
	"""
		Loads samples from clinical file that are known to the study being processed.
		This list of samples is used to ensure that any sample ids that are mapped
		from the ARCHER dataset exist in the clinical data as well to prevent
		assertion errors during the fusion data import.
	"""
	with open(clinical_filename) as clinical_file:
		reader = csv.DictReader(clinical_file, dialect = 'excel-tab')
		for line in reader:
			KNOWN_SAMPLE_MASTERLIST.add(line['SAMPLE_ID'].strip())


def is_valid_study_sample_id(sample_id, study_id):
	""" Simple regex to determine whether sample id matches expected sample id pattern for given study. """
	return STUDY_SAMPLE_REGEX_PATTERNS.get(study_id).match(sample_id)

def get_fusion_event_key(data):
	""" Returns a 'key' used for identifying fusion events. """
	return (data['Tumor_Sample_Barcode'].strip(), data['Fusion'].strip(), data['Hugo_Symbol'].strip())

def load_linked_archer_cases(linked_archer_cases_filename, study_id):
	""" Get the mapping between archer -> impact/heme samples """
	with open(linked_archer_cases_filename) as linked_archer_cases_file:
		reader = csv.DictReader(linked_archer_cases_file, dialect = 'excel-tab')
		for line in reader:
			archer_sid = line['SAMPLE_ID'].strip()
			linked_sid = line['LINKED_MSKIMPACT_CASE'].strip()
			# skip linked sample id if 'NA' or doesn't belong to current study being processed
			if linked_sid is 'NA' or not is_valid_study_sample_id(linked_sid, study_id):
				continue
			# if linked sample id is not in clinical data then update SAMPLES_MISSING_CLINICAL_DATA and skip
			if not linked_sid in KNOWN_SAMPLE_MASTERLIST:
				SAMPLES_MISSING_CLINICAL_DATA.add(linked_sid)
				continue

			# update LINKED_ARCHER_CASES w/current mapping
			LINKED_ARCHER_CASES[archer_sid] = linked_sid
	print >> OUTPUT_FILE, 'Number of linked archer cases for mskimpact: ' + str(len(LINKED_ARCHER_CASES.keys()))

def get_existing_fusions(fusions_filename):
	"""
		Process the msk fusions file to get all current fusions, and to populate the SEEN_FUSION_EVENTS set to prevent duplicates
		The columns that need to be looked at are the Tumor_Sample_Barcode, the Fusion, and Hugo_Symbol columns. If these are the same,
		the two fusions are identical from the perspective of the importer/portal
	"""
	header = get_header(fusions_filename)
	existing_fusions = []
	with open(fusions_filename) as fusions_file:
		reader = csv.DictReader(fusions_file, dialect = 'excel-tab')
		for line in reader:
			SEEN_FUSION_EVENTS.add(get_fusion_event_key(line))
			existing_fusions.append(line)
	return existing_fusions, header

def get_archer_fusions(archer_fusions_filename, header, existing_fusions, study_id):
	"""
		As we process the ARCHER fusions file, we want to associate the fusions with the corresponding MSKIMPACT/HEMEPACT id.
		Lookup the sample in the sample map and add a fusion for each sample we need to.
	"""
	archer_fusions_added = 0
	with open(archer_fusions_filename) as archer_fusions_file:
		reader = csv.DictReader(archer_fusions_file, dialect = 'excel-tab')
		for line in reader:
			archer_sid = line['Tumor_Sample_Barcode'].strip()
			if archer_sid in LINKED_ARCHER_CASES.keys():
				mapped_case_id = LINKED_ARCHER_CASES[archer_sid].strip()

				# check that mapped case id belongs to current study being processed
				if is_valid_study_sample_id(mapped_case_id, study_id):
					# update current fusion event with mapped case id and update fusion datum
					line['Tumor_Sample_Barcode'] = mapped_case_id
					line['Fusion'] = line['Fusion'] + ' - Archer'

					# check if we've seen event already to prevent duplicates
					if get_fusion_event_key(line) not in SEEN_FUSION_EVENTS:
						SEEN_FUSION_EVENTS.add(get_fusion_event_key(line))
						existing_fusions.append(line)
						archer_fusions_added += 1
	return archer_fusions_added

def update_fusions_file(fusions_filename, header, existing_fusions):
	""" Update the fusions file with the new ARCHER fusions. """
	with open(fusions_filename, 'w') as fusions_file:
		fusions_file.write('\t'.join(header))
		for fusion_event in existing_fusions:
			formatted_data = map(lambda x: fusion_event.get(x, '').strip(), header)
			fusions_file.write('\n' + '\t'.join(formatted_data))
		fusions_file.write('\n')

def add_clinical_attribute_to_clinical(clinical_filename):
	""" Update the 'ARCHER' clinical attribute """
	clinical_header = get_header(clinical_filename)
	for line in fileinput.input(clinical_filename, inplace = 1):
		data = map(str.strip, line.split('\t'))
		if data[clinical_header.index('SAMPLE_ID')] in LINKED_ARCHER_CASES.values():
			data[clinical_header.index('ARCHER')] = 'YES'
		print '\t'.join(data)

def update_mapped_archer_samples_file(mapped_archer_samples_filename):
	""" Write out the archer sample ids to a filename for them to be excluded from subsequent merges or subsets involving ARCHER data """
	compiled_archer_samples_set = set(LINKED_ARCHER_CASES.keys())

	# load samples from existing file and update with new archer samples to add
	with open(mapped_archer_samples_filename) as mapped_archer_samples_file:
		compiled_archer_samples_set.update(map(str.strip, mapped_archer_samples_file.readlines()))

	# save updated list of archer samples to file
	with open(mapped_archer_samples_filename, 'w') as mapped_archer_samples_file:
		mapped_archer_samples_file.write('\n'.join(compiled_archer_samples_set) + '\n')

def send_samples_missing_clinical_data_report(study_id, clinical_filename, gmail_username, gmail_password):
	""" Send email reporting ARCHER-linked cases that are missing from the clinical data. """

	# construct message body
	msg = MIMEMultipart()
	message = "** NOTE - Although these samples are linked to ARCHER fusion events, they were not "
	message += "added to the fusions data file since they are missing from the clinical data file. "
	message += "These samples may need to be requeued if they are not already in the DMP queue for "
	message += "the next CVR data fetch. If a sample fails to requeue and/or does not appear in the "
	message += "CVR JSON from the following CVR fetch then please alert the DMP team to address any issues.\n\n"

	message += "Found " + str(len(SAMPLES_MISSING_CLINICAL_DATA)) + " ARCHER-linked sample(s) missing clinical data in: " + clinical_filename + "\n"
	for sample_id in SAMPLES_MISSING_CLINICAL_DATA:
		message += "\n\t" + sample_id
	email_body = MIMEText(message, "plain")
	msg.attach(email_body)

	assert type(MESSAGE_RECIPIENTS)==list

	msg['Subject'] = "ARCHER-linked cases missing clinical data: " + study_id
	msg['From'] = MESSAGE_SENDER
	msg['To'] = COMMASPACE.join(MESSAGE_RECIPIENTS)
	msg['Date'] = formatdate(localtime=True)

	print >> OUTPUT_FILE, "Sending email..."
	# send email
	s = smtplib.SMTP_SSL(SMTP_SERVER, 465)
	s.login(gmail_username, gmail_password)
	s.sendmail(MESSAGE_SENDER, MESSAGE_RECIPIENTS, msg.as_string())
	s.quit()

def merge_fusions(archer_fusions_filename, fusions_filename, linked_cases_filename, clinical_filename, mapped_archer_samples_filename, study_id, gmail_username, gmail_password):
	""" Driver function that calls helper functions to merge fusion records """

	# load masterlist of known samples for given study - check if error loading samples from file
	load_study_sample_masterlist(clinical_filename)
	if not KNOWN_SAMPLE_MASTERLIST:
		print >> ERROR_FILE, "Error loading samples from clinical file: " + clinical_filename + " - exiting..."
		sys.exit(2)

	# load mapping of ARCHER - IMPACT/HEME based on given study id
	load_linked_archer_cases(linked_cases_filename, study_id)

	# get existing fusions and add any new archer fusion events if applicable
	# if archer_fusions_added is > 0 then update existing fusions file, otherwise nothing to do
	existing_fusions, header = get_existing_fusions(fusions_filename)
	archer_fusions_added = get_archer_fusions(archer_fusions_filename, header, existing_fusions, study_id)
	if archer_fusions_added > 0:
		print >> OUTPUT_FILE, "Updating " + fusions_filename + " with " + str(archer_fusions_added) + " ARCHER-linked events for study '" + study_id + "'"
		update_fusions_file(fusions_filename, header, existing_fusions)
	else:
		print >> OUTPUT_FILE, "No new ARCHER fusion events for '" + study_id + "' - skipping updates to: " + fusions_filename

	# update clinical file and mapped archer samples file in case clin attr or mapped archer sample list udpates got missed somehow
	add_clinical_attribute_to_clinical(clinical_filename)
	update_mapped_archer_samples_file(mapped_archer_samples_filename)
	# log and email any archer-linked cases that are missing from the corresponding study's clinical data
	# these samples may need to be requeued if not already in queue for next CVR fetch
	if len(SAMPLES_MISSING_CLINICAL_DATA) > 0:
		print >> ERROR_FILE, "Found " + str(len(SAMPLES_MISSING_CLINICAL_DATA)) + " sample(s) missing clinical data from: " + clinical_filename
		send_samples_missing_clinical_data_report(study_id, clinical_filename, gmail_username, gmail_password)
		for sid in SAMPLES_MISSING_CLINICAL_DATA:
			print >> ERROR_FILE, "\t" + sid

def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('-a', '--archer-fusions', action = 'store', dest = 'archer_fusions_filename', required = True, help = 'data_fusions.txt from the Archer dataset')
	parser.add_argument('-l', '--linked-cases-filename', action = 'store', dest = 'linked_cases_filename' , required = True, help = 'linked_cases.txt from the Archer dataset')
	parser.add_argument('-f', '--fusions-filename', action = 'store', dest = 'fusions_filename', required = True, help = 'data_fusions.txt from the mskimpact or heme dataset')
	parser.add_argument('-c', '--clinical-filename', action = 'store', dest = 'clinical_filename', required = True, help = 'data_clinical*.txt from the mskimpact or heme CVR fetch')
	parser.add_argument('-m', '--mapped-archer-samples-filename', action = 'store', dest = 'mapped_archer_samples_filename', required = True, help = 'Output file storing the archer ids that need to be removed from the mixedpact study')
	parser.add_argument('-i', '--study-id', action = 'store', dest = 'study_id', required = True, help = 'Cancer study identifier [mskimpact | mskimpact_heme]')
	parser.add_argument('-p', '--gmail-password', action = 'store', dest = 'gmail_password', required = True, help = 'Gmail SMTP password')
	parser.add_argument('-u', '--gmail-username', action = 'store', dest = 'gmail_username', required = True, help = 'Gmail username')

	args = parser.parse_args()

	archer_fusions_filename = args.archer_fusions_filename
	fusions_filename = args.fusions_filename
	linked_cases_filename = args.linked_cases_filename
	clinical_filename = args.clinical_filename
	mapped_archer_samples_filename = args.mapped_archer_samples_filename
	study_id = args.study_id
	gmail_username = args.gmail_username
	gmail_password = args.gmail_password

	if not os.path.exists(archer_fusions_filename):
		print 'Archer fusions file cannot be found ' + archer_fusions_filename
		sys.exit(2)
	if not os.path.exists(fusions_filename):
		print 'MSKIMPACT or HEME fusions file cannot be found ' + fusions_filename
		sys.exit(2)
	if not os.path.exists(clinical_filename):
		print 'CVR clinical file cannot be found ' + clinical_filename
		sys.exit(2)
	if not os.path.exists(linked_cases_filename):
		print 'Linked mskimpact cases file cannot be found ' + linked_cases_filename
		sys.exit(2)
	if not study_id:
		print 'Cancer study identifier must be provided!'
		sys.exit(2)
	if not study_id in [MSKIMPACT_STUDY_ID, HEMEPACT_STUDY_ID]:
		print 'Invalid study id provided - only these studies supported: ' + ','.join([MSKIMPACT_STUDY_ID, HEMEPACT_STUDY_ID])
		sys.exit(2)

	merge_fusions(archer_fusions_filename, fusions_filename, linked_cases_filename, clinical_filename, mapped_archer_samples_filename, study_id, gmail_username, gmail_password)

if __name__ == '__main__':
	main()
