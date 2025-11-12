#! /usr/bin/env python

# ------------------------------------------------------------------------------
# Script which adds new users from google spreadsheet into the the cgds
# user table.  The following properties must be specified in portal.properties:
#
# db.portal_db_name
# db.user
# db.password
# db.host
# google.id
# google.pw
# users.spreadsheet
# users.worksheet
# importer.spreadsheet
#
# The script considers all users in the google spreadsheet
# that have an "APPROVED" value in the "Status (APPROVED or BLANK)" column.  If that
# user does not exist in the user table of the cgds database, the user will be added
# to both the user table and authority table.  In addition, a confirmation email will
# be sent to the user notifying them of their acct activation.
#
# ------------------------------------------------------------------------------
# imports
import os
import sys
import time
import getopt
import MySQLdb
import re

import smtplib

import httplib2
from oauth2client import client
from oauth2client.file import Storage
from oauth2client.client import flow_from_clientsecrets
from oauth2client.tools import run_flow, argparser

from email.mime.multipart import MIMEMultipart
from email.mime.base import MIMEBase
from email.mime.text import MIMEText
from email.utils import COMMASPACE, formatdate
from email import encoders

from googleapiclient.discovery import build
# ------------------------------------------------------------------------------
# globals

# some file descriptors
ERROR_FILE = sys.stderr
OUTPUT_FILE = sys.stdout

# fields in portal.properties
CGDS_DATABASE_HOST = 'db.host'
CGDS_DATABASE_NAME = 'db.portal_db_name'
CGDS_DATABASE_USER = 'db.user'
CGDS_DATABASE_PW = 'db.password'
GOOGLE_ID = 'google.id'
GOOGLE_PW = 'google.pw'
CGDS_USERS_SPREADSHEET = 'users.spreadsheet'
CGDS_USERS_WORKSHEET = 'users.worksheet'
IMPORTER_SPREADSHEET = 'importer.spreadsheet'

# Worksheet that contains email contents
IMPORTER_WORKSHEET = 'import_user_email'

# Worksheet that contains portal names
ACCESS_CONTROL_WORKSHEET = 'access_control'

# column constants on google spreadsheet
FULLNAME_KEY = "fullname"
INST_EMAIL_KEY = "institutionalemailaddress"
OPENID_EMAIL_KEY = "googleoropenidaddress"
STATUS_KEY = "statusapprovedorblank"
AUTHORITIES_KEY = "authoritiesalloralltcgaandorsemicolondelimitedcancerstudylist"
TIMESTAMP_KEY = "timestamp"
SUBJECT_KEY = "subject"
BODY_KEY = "body"
PORTAL_NAME_KEY = 'portalname'
SPREADSHEET_NAME_KEY = 'spreadsheetname'

# possible values in status column
STATUS_APPROVED = "APPROVED"

DEFAULT_AUTHORITIES = "PUBLIC;EXTENDED;MSKPUB"

# consts used in email
MSKCC_EMAIL_SUFFIX = "@mskcc.org"
SKI_EMAIL_SUFFIX = "@sloankettering.edu"
SMTP_SERVER = "smtp.gmail.com"
MESSAGE_FROM_CMO = "cbioportal-access@cbioportal.org"
MESSAGE_BCC_CMO = ["cbioportal-access@cbioportal.org"]

MESSAGE_FROM_GENIE = "genie-cbioportal-access@cbioportal.org"
MESSAGE_BCC_GENIE = ["genie-cbioportal-access@cbioportal.org"]
AACR_GENIE_EMAIL = "info@aacrgenie.org"

ERROR_EMAIL_SUBJECT_GENIE = "AACR Project GENIE cBioPortal - Failed to register"
ERROR_EMAIL_BODY_GENIE = "Thank you for your interest in the AACR Project GENIE cBioPortal. There was a problem creating an account for you. Please check that you have a valid Google email account and try to register again. If the problem persists please send an email to " + AACR_GENIE_EMAIL  +"."
ERROR_EMAIL_SUBJECT_CMO = "cBioPortal User Registration - Failed to register"
ERROR_EMAIL_BODY_CMO = "Thank you for your interest in the cBioPortal. There was a problem creating an account for you. Please check that you have a valid email account and try to register again. If the problem persists please send an email to " + MESSAGE_FROM_CMO +"."

# ------------------------------------------------------------------------------
# class definitions

class PortalProperties(object):
    def __init__(self,
                 cgds_database_host,
                 cgds_database_name, cgds_database_user, cgds_database_pw,
                 google_id, google_pw, google_spreadsheet, google_worksheet,google_importer_spreadsheet):
        self.cgds_database_host = cgds_database_host
        self.cgds_database_name = cgds_database_name
        self.cgds_database_user = cgds_database_user
        self.cgds_database_pw = cgds_database_pw
        self.google_id = google_id
        self.google_pw = google_pw
        self.google_spreadsheet = google_spreadsheet
        self.google_worksheet = google_worksheet
        self.google_importer_spreadsheet = google_importer_spreadsheet

class User(object):
    def __init__(self, inst_email, google_email, name, enabled, authorities):
        self.inst_email = inst_email.lower()
        self.google_email = google_email.lower()
        self.name = name
        self.enabled = enabled
        self.authorities = authorities

# ------------------------------------------------------------------------------
# functions

#
# Uses smtplib to send email.
#
def send_mail(to, subject, body, gmail_username, gmail_password, sender=MESSAGE_FROM_CMO, bcc=MESSAGE_BCC_CMO, server=SMTP_SERVER):

    assert type(to)==list
    assert type(bcc)==list

    msg = MIMEMultipart()
    msg['Subject'] = subject
    msg['From'] = sender
    msg['To'] = COMMASPACE.join(to)
    msg['Date'] = formatdate(localtime=True)

    msg.attach(MIMEText(body))

    # combine to and bcc lists for sending
    combined_to_list = []
    for to_name in to:
        combined_to_list.append(to_name)
    for bcc_name in bcc:
        combined_to_list.append(bcc_name)

    smtp = smtplib.SMTP_SSL(server, 465)
    smtp.login(gmail_username, gmail_password)
    smtp.sendmail(sender, combined_to_list, msg.as_string() )
    smtp.close()


# ------------------------------------------------------------------------------

# logs into google spreadsheet client

def get_gdata_credentials(secrets, creds, scope, force=False):
    storage = Storage(creds)
    credentials = storage.get()

    if credentials.access_token_expired:
        credentials.refresh(httplib2.Http())

    if credentials is None or credentials.invalid or force:
      credentials = run_flow(flow_from_clientsecrets(secrets, scope=scope), storage, argparser.parse_args([]))

    return credentials

def google_login(secrets, creds, user, pw, app_name):
    google_credentials = get_gdata_credentials(secrets, creds, ["https://www.googleapis.com/auth/spreadsheets"], False)
    client = build('sheets', "v4", credentials = google_credentials)
    return client

# ------------------------------------------------------------------------------
# returns a list with each element being a row within the requested sheet

def get_sheet_records(client, ss, ws):
    try:
        sheet_records = []
        spreadsheet_service = client.spreadsheets()
        response = spreadsheet_service.values().get(spreadsheetId = ss, range = ws).execute()
        sheet_rows = response.get('values', [])
        header = [re.sub("[^0-9a-zA-Z]+", "", header_name.strip().lower()) for header_name in sheet_rows[0]]
        for row in sheet_rows[1:]:
            if len(row) == len(header):
                sheet_records.append(dict(zip(header, row)))
            else:
                new_record = {}
                for index in range(len(header)):
                    try:
                        new_record[header[index]] = row[index]
                    except:
                        new_record[header[index]] = None
                sheet_records.append(new_record)
    except Exception as e:
        print("There was an error connecting to google.", file = ERROR_FILE)
        exit(0)
 
    return sheet_records

# ------------------------------------------------------------------------------
# get title of spreadsheet

def get_spreadsheet_title(client, ss):
    spreadsheet_title = ""
    try:
        spreadsheet_service = client.spreadsheets()
        response = spreadsheet_service.get(spreadsheetId = ss).execute()
        data = response.get('properties', {})
        spreadsheet_title = data["title"]
    except Exception as e:
        print("There was an error connecting to google.", file = ERROR_FILE)
        exit(0)

    return spreadsheet_title
# ------------------------------------------------------------------------------
# insert new users into table - this list does not contain users already in table

def insert_new_users(cursor, new_user_list):
    # list of emails for users which returned an error when inserting into database
    emails_to_remove = []
    for user in new_user_list:
        print("new user: %s" % user.google_email, file = OUTPUT_FILE);
        try:
            user_name = user.name
            if isinstance(user_name, unicode):
                user_name = user_name.encode('utf-8')
            user_email_escaped=user.google_email.lower().replace('\'', '\\\'')
            cursor.execute("insert into users values('%s', '%s', '%s')" % (user_email_escaped, user_name, user.enabled))
            # authorities is semicolon delimited
            authorities = user.authorities
            cursor.executemany("insert into authorities values(%s, %s)", [(user_email_escaped, authority) for authority in authorities])
        except MySQLdb.Error as  e:
            print(e, file = OUTPUT_FILE)
            print("Removing user: %s" % user_name, file = OUTPUT_FILE)
            print(msg, file = ERROR_FILE)
            emails_to_remove.append(user.google_email.lower())
    return emails_to_remove

# ------------------------------------------------------------------------------
# get current users from database

def get_current_user_map(cursor):

    # map that we are returning
    # key is the email address of the user (primary key) and value is a User object
    to_return = {}

    # recall each tuple in user table is ['EMAIL', 'NAME', 'ENABLED'] &
    # no tuple can contain nulls
    try:
        cursor.execute('select * from users')
        for row in cursor.fetchall():
            to_return[row[0].lower()] = User(row[0].lower(), row[0].lower(), row[1], row[2], 'not_used_here')
    except MySQLdb.Error as e:
        print(e, file = ERROR_FILE)
        return None

    return to_return

# ------------------------------------------------------------------------------
# get current user authorities

def get_user_authorities(cursor, google_email):

        # list of authorities (cancer studies) we are returning -- as a set
        to_return = []

        # recall each tuple in authorities table is ['EMAIL', 'AUTHORITY']
        # no tuple can contain nulls
        try:
                cursor.execute('select * from authorities where email = (%s)', [google_email])
                for row in cursor.fetchall():
                        to_return.append(row[1])
        except MySQLdb.Error as e:
                print(e, file = ERROR_FILE)
                return None

        return to_return

# ------------------------------------------------------------------------------
# get current users from google spreadsheet

def get_new_user_map(spreadsheet, sheet_records, current_user_map, portal_name):

    # map that we are returning
    # key is the institutional email address + google (in case user has multiple google ids)
    # of the user and value is a User object
    to_return = {}
    for row in sheet_records:
        google_email = ''
        inst_email = ''
        # we are only concerned with 'APPROVED' entries
        if (row[STATUS_KEY] is not None and
            row[STATUS_KEY].strip() == STATUS_APPROVED):
            if row[INST_EMAIL_KEY] is not None:
                inst_email = row[INST_EMAIL_KEY].strip().lower()
            if row[OPENID_EMAIL_KEY] is not None:
                google_email = row[OPENID_EMAIL_KEY].strip().lower()
            name = row[FULLNAME_KEY].strip()
            if row[AUTHORITIES_KEY] is not None:
                authorities = row[AUTHORITIES_KEY].strip()
            else:
                authorities = ''
            # do not add row if this row is a current user
            # we lowercase google account because entries added to mysql are lowercased.
            if google_email.lower() not in current_user_map and google_email != '':
                if authorities[-1:] == ';':
                    authorities = authorities[:-1]
                if google_email.lower() in to_return:
                    # there may be multiple entries per email address
                    # in google spreadsheet, combine entries
                    user = to_return[google_email.lower()]
                    user.authorities.extend([portal_name + ':' + au for au in authorities.split(';')])
                    to_return[google_email.lower()] = user
                else:
                    to_return[google_email.lower()] = User(inst_email, google_email, name, 1,
                        [portal_name + ':' + au for au in authorities.split(';')])
    return to_return

# ------------------------------------------------------------------------------
# get db connection

def get_db_connection(portal_properties, port, ssl_ca_filename=None):

    # try and create a connection to the db
    try:
        if ssl_ca_filename:
            connection = MySQLdb.connect(host=portal_properties.cgds_database_host, port=int(port),
                                     user=portal_properties.cgds_database_user,
                                     passwd=portal_properties.cgds_database_pw,
                                     db=portal_properties.cgds_database_name,
                                     ssl={'ca': ssl_ca_filename})
        else:
            connection = MySQLdb.connect(host=portal_properties.cgds_database_host, port=int(port),
                                     user=portal_properties.cgds_database_user,
                                     passwd=portal_properties.cgds_database_pw,
                                     db=portal_properties.cgds_database_name)
    except MySQLdb.Error as e:
        print(e, file = ERROR_FILE)
        return None

    return connection


# ------------------------------------------------------------------------------
# parse portal.properties

def get_portal_properties(portal_properties_filename):

    properties = {}
    portal_properties_file = open(portal_properties_filename, 'r')
    for line in portal_properties_file:
        line = line.strip()
        # skip line if its blank or a comment
        if len(line) == 0 or line.startswith('#'):
            continue
        # store name/value
        property = line.split('=')
        # spreadsheet url contains an '=' sign
        if line.startswith(CGDS_USERS_SPREADSHEET):
            property = [property[0], line[line.index('=')+1:len(line)]]
        if (len(property) != 2):
            print('Skipping invalid entry in property file: ' + line, file = ERROR_FILE)
            continue
        properties[property[0]] = property[1].strip()
    portal_properties_file.close()

    # error check
    if (CGDS_DATABASE_HOST not in properties or len(properties[CGDS_DATABASE_HOST]) == 0 or
        CGDS_DATABASE_NAME not in properties or len(properties[CGDS_DATABASE_NAME]) == 0 or
        CGDS_DATABASE_USER not in properties or len(properties[CGDS_DATABASE_USER]) == 0 or
        CGDS_DATABASE_PW not in properties or len(properties[CGDS_DATABASE_PW]) == 0 or
        GOOGLE_ID not in properties or len(properties[GOOGLE_ID]) == 0 or
        GOOGLE_PW not in properties or len(properties[GOOGLE_PW]) == 0 or
        CGDS_USERS_SPREADSHEET not in properties or len(properties[CGDS_USERS_SPREADSHEET]) == 0 or
        CGDS_USERS_WORKSHEET not in properties or len(properties[CGDS_USERS_WORKSHEET]) == 0 or
        IMPORTER_SPREADSHEET not in properties or len(properties[IMPORTER_SPREADSHEET]) == 0):
        print('Missing one or more required properties, please check property file', file = ERROR_FILE)
        return None

    # return an instance of PortalProperties
    return PortalProperties(properties[CGDS_DATABASE_HOST],
                            properties[CGDS_DATABASE_NAME],
                            properties[CGDS_DATABASE_USER],
                            properties[CGDS_DATABASE_PW],
                            properties[GOOGLE_ID],
                            properties[GOOGLE_PW],
                            properties[CGDS_USERS_SPREADSHEET],
                            properties[CGDS_USERS_WORKSHEET],
                            properties[IMPORTER_SPREADSHEET])

# ------------------------------------------------------------------------------
# adds new users from the google spreadsheet into the cgds portal database
# returns new user map if users have been inserted, None otherwise

def manage_users(client, spreadsheet, cursor, sheet_records, portal_name):

    # get map of current portal users
    print('Getting list of current portal users', file = OUTPUT_FILE)
    current_user_map = get_current_user_map(cursor)
    if current_user_map is not None:
        print('We have found %s current portal users' % len(current_user_map), file = OUTPUT_FILE)
    else:
        print('Error reading user table', file = OUTPUT_FILE)
        return None, None

    # get list of new users and insert
    print('Checking for new users', file = OUTPUT_FILE)
    new_user_map = get_new_user_map(spreadsheet, sheet_records, current_user_map, portal_name)
    if (len(new_user_map) > 0):
        print('We have %s new user(s) to add' % len(new_user_map), file = OUTPUT_FILE)  
        emails_to_remove = insert_new_users(cursor, new_user_map.values())
        return new_user_map, emails_to_remove
    else:
        print('No new users to insert, exiting', file = OUTPUT_FILE)
        return None, None

# ------------------------------------------------------------------------------
# updates user study access

def update_user_authorities(spreadsheet, cursor, sheet_records, portal_name):

        # get map of current portal users
        print('Getting list of current portal users from spreadsheet', file = OUTPUT_FILE)
        all_user_map = get_new_user_map(spreadsheet, sheet_records, {}, portal_name)
        if all_user_map is None:
                return None;
        print('Updating authorities for each user in current portal user list', file = OUTPUT_FILE)
        for user in all_user_map.values():
                sheet_authorities = set(user.authorities)
                db_authorities = set(get_user_authorities(cursor, user.google_email))
                try:
                        cursor.executemany("insert into authorities values(%s, %s)",
                                           [(user.google_email, authority) for authority in sheet_authorities - db_authorities])
                except MySQLdb.Error as e:
                        print(e, file = ERROR_FILE)

# ------------------------------------------------------------------------------
# gets email parameters from google spreadsheet

def get_email_parameters(google_spreadsheet,client):
    subject = ''
    body = ''
    print('Getting email parameters from google spreadsheet', file = OUTPUT_FILE)
    email_sheet_records = get_sheet_records(client, google_spreadsheet, IMPORTER_WORKSHEET)
    for record in email_sheet_records:
        if record[SUBJECT_KEY] is not None and record[BODY_KEY] is not None:
            subject = record[SUBJECT_KEY].strip()
            body = record[BODY_KEY].strip()
    return subject, body

def get_portal_name_map(google_spreadsheet,client):
    portal_name = {}
    print('Getting access control parameter from google spreadsheet', file = OUTPUT_FILE)
    access_control_sheet = get_sheet_records(client,google_spreadsheet,ACCESS_CONTROL_WORKSHEET)
    for row in access_control_sheet: 
        if row[PORTAL_NAME_KEY] is not None and row[SPREADSHEET_NAME_KEY] is not None:
            portal_name[row[SPREADSHEET_NAME_KEY].strip()] = row[PORTAL_NAME_KEY].strip()
    return portal_name


def establish_new_db_connection(portal_properties, port, ssl_ca_filename):
    # get db connection & create cursor
    print('Connecting to database: ' + portal_properties.cgds_database_name, file = OUTPUT_FILE)  
    connection = get_db_connection(portal_properties, port, ssl_ca_filename)
    if connection is not None:
        cursor = connection.cursor()
    else:
        print('Error connecting to database, exiting', file = OUTPUT_FILE)
        sys.exit(2)
    return (connection, cursor)


# ------------------------------------------------------------------------------
# displays program usage (invalid args)

def usage():
    print('importUsers.py --secrets-file [google secrets.json] --creds-file [oauth creds filename] --properties-file [properties file] --send-email-confirm [true or false] --use-institutional-id [true or false] --port [mysql port number] --sender [sender identifier - optional] --ssl-ca [ssl certificate file - optional]', file = OUTPUT_FILE)

# ------------------------------------------------------------------------------
# the big deal main.

def main():

    # parse command line options
    try:
        opts, args = getopt.getopt(sys.argv[1:], '', ['secrets-file=', 'creds-file=', 'properties-file=', 'ssl-ca=', 'send-email-confirm=', 'use-institutional-id=', 'port=', 'sender=', 'gmail-username=', 'gmail-password='])
    except getopt.error as msg:
        print(msg, file = ERROR_FILE)
        usage()
        sys.exit(2)

    # process the options
    secrets_filename = ''
    creds_filename = ''
    properties_filename = ''
    send_email_confirm = ''
    port = ''
    sender = ''
    ssl_ca_filename = '' # not required

    for o, a in opts:
        if o == '--secrets-file':
            secrets_filename = a
        elif o == '--creds-file':
            creds_filename = a
        elif o == '--gmail-username':
            gmail_username = a
        elif o == '--gmail-password':
            gmail_password = a
        elif o == '--properties-file':
            properties_filename = a
        elif o == '--ssl-ca':
            ssl_ca_filename = a
        elif o == '--send-email-confirm':
            send_email_confirm = a
        elif o == '--sender':
            sender = a
        elif o == '--port':
            port = a

    if (secrets_filename == '' or creds_filename == '' or properties_filename == '' or send_email_confirm == '' or port == '' or
        (send_email_confirm != 'true' and send_email_confirm != 'false') or 
        (send_email_confirm == 'true' and (gmail_username == '' or gmail_password == ''))):
        usage()
        sys.exit(2)

    # check existence of file
    if not os.path.exists(properties_filename):
        print('properties file cannot be found: ' + properties_filename, file = ERROR_FILE)
        sys.exit(2)

    # parse/get relevant portal properties
    print('Reading portal properties file: ' + properties_filename, file = OUTPUT_FILE)
    portal_properties = get_portal_properties(properties_filename)
    if not portal_properties:
        print('Error reading %s, exiting' % properties_filename, file = OUTPUT_FILE)
        return

    # create client for interacting with google sheets api
    client = google_login(secrets_filename, creds_filename, portal_properties.google_id, portal_properties.google_pw, sys.argv[0])
    # connect to importer configuration spreadsheet and get mapping of spreadsheet to portal name
    portal_name_map = get_portal_name_map(portal_properties.google_importer_spreadsheet,client)

    google_spreadsheets = portal_properties.google_spreadsheet.split(';')
    for google_spreadsheet in google_spreadsheets:
        if not google_spreadsheet == '':
            (connection, cursor) = establish_new_db_connection(portal_properties, port, ssl_ca_filename)
            
            sheet_records = get_sheet_records(client, google_spreadsheet,
                                                portal_properties.google_worksheet)
            spreadsheet_title = get_spreadsheet_title(client, google_spreadsheet)
           
            print('Importing ' + spreadsheet_title + ' ...', file = OUTPUT_FILE)
            app_name = portal_name_map[spreadsheet_title]
            
            # the 'guts' of the script
            # note: original script depended on one to one mapping of spreadsheet to app name - and lookup was by spreadsheet
            # with a now decommissioned app (genie-archive) we wanted to be able to do one to many mapping (one spreadsheet to multiple apps)
            # to fit this logic would have to rework how we specify properties or introduce new column (db name) as index but might have other effects
            new_user_map, emails_to_remove = manage_users(client, google_spreadsheet, cursor, sheet_records, app_name)
            
            # update user authorities
            update_user_authorities(google_spreadsheet, cursor, sheet_records, app_name)
            
            # commit changes before moving on to next spreadsheet
            cursor.close()
            connection.commit()
            connection.close()

            # sending emails
            if new_user_map is not None:
                if send_email_confirm == 'true':
                    subject,body = get_email_parameters(google_spreadsheet,client)
                    for new_user_key in new_user_map.keys():
                        new_user = new_user_map[new_user_key]
                        from_field = MESSAGE_FROM_CMO
                        bcc_field = MESSAGE_BCC_CMO
                        error_subject = ERROR_EMAIL_SUBJECT_CMO
                        error_body = ERROR_EMAIL_BODY_CMO
                        if sender == 'GENIE':
                            from_field = MESSAGE_FROM_GENIE
                            bcc_field = MESSAGE_BCC_GENIE
                            error_subject = ERROR_EMAIL_SUBJECT_GENIE
                            error_body = ERROR_EMAIL_BODY_GENIE
                        if new_user_key not in emails_to_remove:
                            print(('Sending confirmation email to new user: %s at %s' %
                                               (new_user.name, new_user.inst_email)), file = OUTPUT_FILE)

                            send_mail([new_user.inst_email],subject,body, gmail_username, gmail_password, sender = from_field, bcc = bcc_field)
                        else:
                            send_mail([new_user_key], error_subject, error_body, gmail-username, gmail_password, sender = from_field, bcc = bcc_field)

# ------------------------------------------------------------------------------
# ready to roll

if __name__ == '__main__':
    main()
