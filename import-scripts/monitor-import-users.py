#! /usr/bin/env python

from importUsers import *
import os
import sys
import time

'''
Script for periodically checking if import-users script is working
Gets last timestamped email from google spreadsheet (GENIE Public)
and queries for it inside the database.
Exceptions will be caught by crontab and result in an email to pipelines
''' 
GENIE_USERS_WORKSHEET = 'Request Access to the Public cBio GENIE Cancer Genomics Portal'

def check_if_user_in_db(cursor, email):
    '''
        Given an email, check if it is in the users table for specified database
        Return if count of matching records is not zero
    '''
    try:
        cursor.execute("select count(*) from users where email='%s'" % (email))
        result = cursor.fetchone()
        return result[0] != 0
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        return None

def get_latest_email(secrets_file, creds_file, portal_properties, appname):
    '''
        Iterates through the users worksheet (hardcoded to GENIE Public)
        Will return the last timestamped email
    '''
    client = google_login(secrets_file, creds_file, portal_properties.google_id, portal_properties.google_pw, appname)
    worksheet_feed = get_worksheet_feed(client, GENIE_USERS_WORKSHEET, portal_properties.google_worksheet) 
    latest_email = ''
    for entry in worksheet_feed.entry:
        if entry.custom[TIMESTAMP_KEY].text:
            latest_email = entry.custom[OPENID_EMAIL_KEY].text.strip().lower()
    if latest_email:
        return latest_email
    print >> OUTPUT_FILE, 'Unable to retrieve email from google spreadsheet, spreadsheet may be corrupted.'
    raise Exception('Unable to retrieve email from google spreadsheet, spreadsheet may be corrupted.')

def parse_monitor_import_users_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-p', '--portal-properties', action = 'store', dest = 'portal_properties_file', required = True, help = 'Path to portal.properties file')
    parser.add_argument('-P', '--port', action = 'store', dest = 'port', required = True, help = 'Database port')
    parser.add_argument('-s', '--secrets-filename', action = 'store', dest = 'secrets_filename', required = True, help = 'The file containing the secret for accessing gdata API')
    parser.add_argument('-c', '--credentials', action = 'store', dest = 'credentials', required = True, help = 'Credentials for accessing gdata API')
    return parser

def main(args):
    portal_properties = args.portal_properties
    secrets_file = args.secrets_filename
    creds_file = args.credentials

    if not os.path.exists(portal_properties):
        print >> OUTPUT_FILE, 'Specified portal properties file %s does not exist' % (portal_properties)
        return
    if not os.path.exists(secrets_file):
        print >> OUTPUT_FILE, 'Specified secrets file %s does not exist' % (secrets_file)
        return
    if not os.path.exists(creds_file):
        print >> OUTPUT_FILE, 'Specified credentialss file %s does not exist' % (creds_file)
        return

    print >> OUTPUT_FILE, 'Reading portal properties file: ' + portal_properties
    portal_properties = get_portal_properties(portal_properties)
    if not portal_properties:
        print >> OUTPUT_FILE, 'Error reading %s, exiting' % portal_properties
        return
    get_latest_email(secrets_file, creds_file, portal_properties, sys.argv[0])
   
    # sleep for three minutes to allow latest email to be imported (process runs every minute)
    time.sleep(180)    
    
    connection = get_db_connection(portal_properties, port)
    if connection is not None:
        cursor = connection.cursor()
    else:
        print >> OUTPUT_FILE, 'Error connecting to database, exiting'
        return
    
    user_in_db = check_if_user_in_db(cursor, latest_email)
    cursor.close()
    connection.commit()
    connection.close()
   
    # latest user is not found despite waiting several minutes since email was entered
    # indicates import-portal-users.sh is not working as expected 
    if not user_in_db:
        print >> OUTPUT_FILE, 'Database not updated with latest users, import-portal-users.sh is not working'
        raise Exception('Database not updated with latest users, import-portal-users.sh is not working')

if __name__ == '__main__':
    parser = parse_monitor_import_users_args()
    args = parser.parse_args()
    main(args)
