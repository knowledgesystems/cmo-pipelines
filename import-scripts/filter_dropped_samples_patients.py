#! /usr/bin/env python

from clinicalfile_utils import *
from email.Utils import COMMASPACE, formatdate
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import argparse
import fileinput
import smtplib
import sys
#---------------------------------------------------------------------------------------------------
# Use: Filter out samples which are dropped from masterlist but still in supp files before importing (from data_clinical_sample.txt)
#      Filter out patients which no longer have samples on the masterlist (from data_clinical_patient.txt/data_timeline.txt)
#
# Script filters out samples based on whether or not sample is found in sample masterlist
# Patients with valid samples are added to a masterlist which is used for filtering patients
# Email is sent out with general report (number of dropped samples/patients) and text attachments with specific dropped sample/patient lists
#--------------------------------------------------------------------------------------------------
ERROR_FILE = sys.stderr

# Constants used for email
SMTP_SERVER = "smtp.gmail.com"
DROPPED_SAMPLE_FILENAME = "dropped_samples.txt"
DROPPED_PATIENT_FILENAME = "dropped_patients.txt"
DROPPED_TIMELINE_FILENAME = "dropped_timeline.txt"
MESSAGE_RECIPIENTS = ["cbioportal-pipelines@cbioportal.org"]
DROPPED_SAMPLES_PATIENTS_SUBJECT = "Dropped Samples/Patients Report"
MESSAGE_SENDER = "cbioportal@cbioportal.org"

# Returns a set containing sample ids from masterlist
# adds "SAMPLE_ID" so header gets written out
def generate_sample_masterlist(filtered_samples_file):
    sample_masterlist = set()
    # add attribute name so header gets written
    sample_masterlist.add("SAMPLE_ID")
    with open(filtered_samples_file) as file:
        for sample_id in file.readlines():
            sample_masterlist.add(sample_id.rstrip())
    return sample_masterlist

# Filters out samples based on whether the sample ID is in the sample_masterlist
# Samples in the sample_masterlist have their patients added to patient_masterlist dictionary
def filter_dropped_samples(sample_file, patient_masterlist, dropped_sample_list, sample_masterlist):
    header = get_header(sample_file)
    patient_id_index = header.index("PATIENT_ID")
    sample_id_index = header.index("SAMPLE_ID")

    f = fileinput.input(sample_file, inplace=1)
    for line in f:
        if line.startswith("#"):
            print line.rstrip("\n")
            continue
        record = line.rstrip("\n").split('\t')
        if record[sample_id_index] in sample_masterlist:
            patient_masterlist[record[patient_id_index]] = True
            print '\t'.join(record)
        else:
            dropped_sample_list.add(record[sample_id_index])
    f.close()

# Filters out patients (with no valid samples) based on patient masterlist dictionary
# Patients not in patient masterlist dictioanry did not have a valid sample during sample filtering
def filter_dropped_patients(patient_file, patient_masterlist, dropped_patient_list):
    header = get_header(patient_file)
    patient_id_index = header.index("PATIENT_ID")
    f = fileinput.input(patient_file, inplace=1)
    for line in f:
        if line.startswith("#"):
            print line.rstrip("\n")
            continue
        record = line.rstrip("\n").split('\t')
        if record[patient_id_index] in patient_masterlist:
            print '\t'.join(record)
        else:
            dropped_patient_list.add(record[patient_id_index])
    f.close()

# Generates attachements with dropped sample/patient lists
def add_dropped_report(message, dropped_items_list, item_type, attachment_name):
    if len(dropped_items_list) > 0:
        dropped_items_report = MIMEText("Dropped " + str(len(dropped_items_list)) + " " + item_type + ":\n\t" + '\n\t'.join(dropped_items_list))
        dropped_items_report.add_header('Content-Disposition', 'attachment', filename = attachment_name)
        message.attach(dropped_items_report)

def generate_message_body(dropped_items_list, item_type):
    message_body = ""
    if len(dropped_items_list) > 0:
        message_body = message_body + "Number of " + item_type + " dropped: " + str(len(dropped_items_list)) + "\n"
    return message_body

def usage():
    print 'python filter_dropped_samples_patients.py --clinical-sample-file [path/to/clinical-sample/file] --clinical-patient-file [path/to/clinical-patient/file] --timeline-file [path/to/timeline/file] --filtered-samples-file [path/to/sample-masterlist/file]'
    sys.exit(2)

def main():
    usage_statement = '''filter_dropped_samples_patients.py: -f <FILTERED_SAMPLES_FILE>
                                           -s <CLINICAL_SAMPLE_FILE>
                                           [-t TIMELINE_FILE]
                                           [-p CLINICAL_PATIENT_FILE]'''
    parser = argparse.ArgumentParser(usage = usage_statement)
    parser.add_argument("-f", "--filtered-samples-file", help = "list of samples on masterlist", required = True)
    parser.add_argument("-p", "--clinical-patient-file", help = "data_clinical_patient.txt")
    parser.add_argument("-s", "--clinical-sample-file", help = "data_clinical_sample.txt", required = True)
    parser.add_argument("-t", "--timeline-file", help = "data_timeline.txt")
    parser.add_argument('-g', '--gmail-password', action = 'store', dest = 'gmail_password', required = True, help = 'Gmail SMTP password')
    parser.add_argument('-u', '--gmail-username', action = 'store', dest = 'gmail_username', required = True, help = 'Gmail username')
    args = parser.parse_args()

    sample_file = args.clinical_sample_file
    patient_file = args.clinical_patient_file
    timeline_file = args.timeline_file
    filtered_samples_file = args.filtered_samples_file
    gmail_username = args.gmail_username
    gmail_password = args.gmail_password

    if not os.path.exists(sample_file):
        print >> ERROR_FILE, 'clinical sample file not found: ' + sample_file
        usage()
    if args.clinical_patient_file and not os.path.exists(patient_file):
        print "HEY"
        print >> ERROR_FILE, 'clinical patient file not found: ' + patient_file
        usage()
    if args.timeline_file and not os.path.exists(timeline_file):
        print >> ERROR_FILE, 'timeline file not found: ' + timeline_file
        usage()
    if not os.path.exists(filtered_samples_file):
        print >> ERROR_FILE, 'filtered samples file not found: ' + filtered_samples_file
        usage()

    sample_masterlist = generate_sample_masterlist(filtered_samples_file)
    patient_masterlist = {}
    dropped_sample_list = set()
    dropped_patient_list = set()
    dropped_timeline_list = set()

    msg = MIMEMultipart()

    filter_dropped_samples(sample_file, patient_masterlist, dropped_sample_list, sample_masterlist)
    add_dropped_report(msg, dropped_sample_list, "clinical samples", DROPPED_SAMPLE_FILENAME)
    message_body = generate_message_body(dropped_sample_list, "clinical samples")

    if args.clinical_patient_file:
        filter_dropped_patients(patient_file, patient_masterlist, dropped_patient_list)
        add_dropped_report(msg, dropped_patient_list, "clinical patients", DROPPED_PATIENT_FILENAME)
        message_body = message_body + generate_message_body(dropped_patient_list, "clinical patients")

    if args.timeline_file:
        filter_dropped_patients(timeline_file, patient_masterlist, dropped_timeline_list)
        add_dropped_report(msg, dropped_timeline_list, "timeline patients", DROPPED_TIMELINE_FILENAME)
        message_body = message_body + generate_message_body(dropped_timeline_list, "timeline patients")

    body = MIMEText(message_body, "plain")
    msg.attach(body)

    assert type(MESSAGE_RECIPIENTS)==list

    msg['Subject'] = DROPPED_SAMPLES_PATIENTS_SUBJECT
    msg['From'] = MESSAGE_SENDER
    msg['To'] = COMMASPACE.join(MESSAGE_RECIPIENTS)
    msg['Date'] = formatdate(localtime=True)

    all_recipients_list = []
    for recipient in MESSAGE_RECIPIENTS:
        all_recipients_list.append(recipient)

    # Only send out report if something was dropped
    if (len(dropped_sample_list) > 0 or len(dropped_patient_list) > 0 or len(dropped_timeline_list) > 0):
        s = smtplib.SMTP_SSL(SMTP_SERVER, 465)
        s.login(gmail_username, gmail_password)
        s.sendmail(MESSAGE_SENDER, all_recipients_list, msg.as_string())
        s.quit()

if __name__ == '__main__':
    main()
