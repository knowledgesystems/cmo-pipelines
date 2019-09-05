#!/usr/bin/env python

import os
import sys
import urllib
import json
from email.Utils import COMMASPACE, formatdate
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
import smtplib
import optparse

PARTA_CONSENTED_URL = 'http://draco.mskcc.org:9890/get_12245_list_parta'
PARTC_CONSENTED_URL = 'http://draco.mskcc.org:9890/get_12245_list_partc'

PARTA_FIELD_NAME = 'PARTA_CONSENTED_12_245'
PARTC_FIELD_NAME = 'PARTC_CONSENTED_12_245'

MESSAGE_SENDER = 'cbioportal@cbio.mskcc.org'
SMTP_SERVER = 'cbio.mskcc.org'
MESSAGE_RECIPIENTS = ['cbioportal-pipelines@cbio.mskcc.org']
CONSENT_STATUS_EMAIL_SUBJECT = 'CVR Part A & C Consent Status Updates'

CVR_CONSENT_STATUS_ENDPOINTS = {
    PARTA_FIELD_NAME : PARTA_CONSENTED_URL,
    PARTC_FIELD_NAME : PARTC_CONSENTED_URL
}

def fetch_expected_consent_status_values():
    '''
        Fetches expected consent status values for 12-245
        Part A and C from CVR web service.
    '''
    expected_consent_status_values = {}
    for field,url in CVR_CONSENT_STATUS_ENDPOINTS.items():
        response = urllib.urlopen(url)
        data = json.loads(response.read())
        consent_values = {}
        for pt,status in data['cases'].items():
            if status:
                consent_values[pt] = 'YES'
            else:
                consent_values[pt] = 'NO'
        expected_consent_status_values[field] = consent_values
    return expected_consent_status_values

def cvr_consent_status_fetcher_main(cvr_clinical_file, expected_consent_status_values):
    '''
        Checks the current consent status for
        Part A & C against the expected consent status values
        from CVR web service.

        A report of which samples need to be requeued or removed
        from data set is emailed to recipients.

        Samples are added to the requeue list if their expected consent
        status is 'YES' and their current status is 'NO'.

        Samples are added to the removal list if their expected consent
        status is 'NO' and their current status is 'YES'.
    '''
    samples_to_requeue = {}
    samples_to_remove = {}
    with open(cvr_clinical_file, 'rU') as data_file:
        header = []
        for line in data_file.readlines():
            if not header:
                header = map(str.strip, line.split('\t'))
                continue
            # update patient-sample mapping
            record = dict(zip(header, map(str.strip, line.split('\t'))))

            for field in CVR_CONSENT_STATUS_ENDPOINTS.keys():
                current_consent_status = record[field]
                expected_consent_status = expected_consent_status_values[field].get(record['PATIENT_ID'], 'NO')
                # if current and expected values are the same then skip
                if current_consent_status == expected_consent_status:
                    continue

                # if patient has granted consent then add samples to requeue list
                # otherwise if patient has since revoked consent then add sample to
                # set of samples to remove from data set
                if expected_consent_status == 'YES':
                    requeue_list = samples_to_requeue.get(field, set())
                    requeue_list.add(record['SAMPLE_ID'])
                    samples_to_requeue[field] = requeue_list
                elif expected_consent_status == 'NO':
                    remove_list = samples_to_remove.get(field, set())
                    remove_list.add(record['SAMPLE_ID'])
                    samples_to_remove[field] = remove_list

    if samples_to_requeue != {} or samples_to_remove != {}:
        email_consent_status_report(samples_to_requeue, samples_to_remove)

def generate_attachment(message, attachment_name, samples):
    '''
        Generates email attachment.
    '''
    report = MIMEText('\n'.join(list(samples)))
    report.add_header('Content-Disposition', 'attachment', filename = attachment_name)
    message.attach(report)

def email_consent_status_report(samples_to_requeue, samples_to_remove):
    '''
        Constructs and sends email reporting consent status updates.
    '''
    summary = 'Consent status report summary:\n'
    message = MIMEMultipart()
    if samples_to_requeue != {}:
        summary += '\n\nCONSENT GRANTED:'
        for field,samples in samples_to_requeue.items():
            summary += '\n\t%s:\t%s samples' % (field, len(samples))
            filename = field.lower() + '_consent_granted_report.txt'
            generate_attachment(message, filename, samples)

    if samples_to_remove != {}:
        summary += '\n\nCONSENT REVOKED:'
        for field,samples in samples_to_remove.items():
            summary += '\n\t%s:\t%s samples' % (field, len(samples))
            filename = field.lower() + '_consent_revoked_report.txt'
            generate_attachment(message, filename, samples)

    body = MIMEText(summary, 'plain')
    message.attach(body)

    message['Subject'] = CONSENT_STATUS_EMAIL_SUBJECT
    message['From'] = MESSAGE_SENDER
    message['To'] = COMMASPACE.join(MESSAGE_RECIPIENTS)
    message['Date'] = formatdate(localtime=True)

    s = smtplib.SMTP(SMTP_SERVER)
    s.sendmail(MESSAGE_SENDER, MESSAGE_RECIPIENTS, message.as_string())
    s.quit()

def main():
    parser = optparse.OptionParser()
    parser.add_option('-c', '--clinical-file', action = 'store', dest = 'clinfile', help = 'CVR clinical file')
    (options, args) = parser.parse_args()
    cvr_clinical_file = options.clinfile

    expected_consent_status_values = fetch_expected_consent_status_values()
    cvr_consent_status_fetcher_main(cvr_clinical_file, expected_consent_status_values)

if __name__ == '__main__':
    main()
