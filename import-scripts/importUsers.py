#! /usr/bin/env python

# ------------------------------------------------------------------------------
# Script which adds new users fram google spreadsheet into the the cgds
# user table.  The following properties must be specified in portal.properties:
#
# db.name
# db.user
# db.password
# db.host
# google.id
# google.pw
# users.spreadsheet
# users.worksheet
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

import smtplib
import gdata.docs.client
import gdata.docs.service
import gdata.spreadsheet.service

import httplib2
from oauth2client import client
from oauth2client.file import Storage
from oauth2client.client import flow_from_clientsecrets
from oauth2client.tools import run_flow, argparser

from email.MIMEMultipart import MIMEMultipart
from email.MIMEBase import MIMEBase
from email.MIMEText import MIMEText
from email.Utils import COMMASPACE, formatdate
from email import Encoders

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
MSKCC_EMAIL_KEY = "mskccemailaddress"
OPENID_EMAIL_KEY = "googleoropenidaddress"
STATUS_KEY = "statusapprovedorblank"
AUTHORITIES_KEY = "authoritiesalloralltcgaandorsemicolondelimitedcancerstudylist"
LAB_PI_KEY = "labpi"
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
SMTP_SERVER = "cbio.mskcc.org"
MESSAGE_FROM_CMO = "cbioportal-access@cbio.mskcc.org"
MESSAGE_BCC_CMO = ["cbioportal-access@cbio.mskcc.org"]

MESSAGE_FROM_GENIE = "genie-cbioportal-access@cbio.mskcc.org"
MESSAGE_BCC_GENIE = ["genie-cbioportal-access@cbio.mskcc.org"]
AACR_GENIE_EMAIL = 'info@aacrgenie.org'

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
def send_mail(to, subject, body, sender=MESSAGE_FROM_CMO, bcc=MESSAGE_BCC_CMO, server=SMTP_SERVER):

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

    smtp = smtplib.SMTP(server)
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

    credentials = get_gdata_credentials(secrets, creds, ["https://spreadsheets.google.com/feeds"], False)
    client = gdata.spreadsheet.service.SpreadsheetsService(additional_headers={'Authorization' : 'Bearer %s' % credentials.access_token})

    # google spreadsheet
    client.email = user
    client.password = pw
    client.source = app_name
    client.ProgrammaticLogin()

    return client


# ------------------------------------------------------------------------------
# given a feed & feed name, returns its id
#
def get_feed_id(feed, name):

    to_return = ''

    for entry in feed.entry:
        if entry.title.text.strip() == name:
            id_parts = entry.id.text.split('/')
            to_return = id_parts[len(id_parts) - 1]

    return to_return

# ------------------------------------------------------------------------------
# gets a worksheet feed

def get_worksheet_feed(client, ss, ws):

    try:
        ss_id = get_feed_id(client.GetSpreadsheetsFeed(), ss)
        ws_id = get_feed_id(client.GetWorksheetsFeed(ss_id), ws)
        list_feed = client.GetListFeed(ss_id, ws_id)
    except gdata.service.RequestError:
        print >> ERROR_FILE, "There was an error connecting to google."
        sys.exit(0)

    return list_feed

# ------------------------------------------------------------------------------
# insert new users into table - this list does not contain users already in table

def insert_new_users(cursor, new_user_list):
    # list of emails for users which returned an error when inserting into database
    emails_to_remove = []
    for user in new_user_list:
        print >> OUTPUT_FILE, "new user: %s" % user.google_email;
        try:
            user_name = user.name
            if isinstance(user_name, unicode):
                user_name = user_name.encode('utf-8')
            cursor.execute("insert into users values('%s', '%s', '%s')" % (user.google_email.lower(), user_name, user.enabled))
            # authorities is semicolon delimited
            authorities = user.authorities
            cursor.executemany("insert into authorities values(%s, %s)", [(user.google_email.lower(), authority) for authority in authorities])
        except MySQLdb.Error, msg:
            print >> OUTPUT_FILE, msg
            print >> OUTPUT_FILE, "Removing user: %s" % user_name
            print >> ERROR_FILE, msg
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
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
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
        except MySQLdb.Error, msg:
                print >> ERROR_FILE, msg
                return None

        return to_return

# ------------------------------------------------------------------------------
# get current users from google spreadsheet

def get_new_user_map(spreadsheet, worksheet_feed, current_user_map, portal_name,mskcc_user_spreadsheet):

    # map that we are returning
    # key is the institutional email address + google (in case user has multiple google ids)
    # of the user and value is a User object
    to_return = {}

    for entry in worksheet_feed.entry:
        google_email = ''
        inst_email = ''
        # we are only concerned with 'APPROVED' entries
        if (entry.custom[STATUS_KEY].text is not None and
            entry.custom[STATUS_KEY].text.strip() == STATUS_APPROVED):
            if spreadsheet == mskcc_user_spreadsheet:
                if entry.custom[MSKCC_EMAIL_KEY].text is not None:
                    inst_email = entry.custom[MSKCC_EMAIL_KEY].text.strip().lower()
                    google_email = entry.custom[MSKCC_EMAIL_KEY].text.strip().lower()
            else:
                if entry.custom[INST_EMAIL_KEY].text is not None:
                    inst_email = entry.custom[INST_EMAIL_KEY].text.strip().lower()
                if entry.custom[OPENID_EMAIL_KEY].text is not None:
                    google_email = entry.custom[OPENID_EMAIL_KEY].text.strip().lower()
            name = entry.custom[FULLNAME_KEY].text.strip()
            if entry.custom[AUTHORITIES_KEY].text is not None:
                authorities = entry.custom[AUTHORITIES_KEY].text.strip()
            else:
                authorities = ''
            # do not add entry if this entry is a current user
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

def delete_row_from_spreadsheet(client, spreadsheet, worksheet_feed, email):
    to_delete = ''
    for entry in worksheet_feed.entry:
        google_email = entry.custom[OPENID_EMAIL_KEY].text.strip().lower()
        if google_email == email:
            to_delete = entry
    if to_delete != '':
        client.DeleteRow(to_delete)

# ------------------------------------------------------------------------------
# get all users from google spreadsheet.  note only inst & google email is returned

def get_all_user_map(spreadsheet, worksheet_feed,mskcc_user_spreadsheet):

    # map that we are returning
    # key is the institutional email address + google (in case user has multiple google ids)
    # of the user and value is a User object
    to_return = {}

    for entry in worksheet_feed.entry:
        if spreadsheet == mskcc_user_spreadsheet:
            inst_email = entry.custom[MSKCC_EMAIL_KEY].text.strip().lower()
            google_email = entry.custom[MSKCC_EMAIL_KEY].text.strip().lower()
        else:
            inst_email = entry.custom[INST_EMAIL_KEY].text.strip().lower()
            google_email = entry.custom[OPENID_EMAIL_KEY].text.strip().lower()
        to_return[google_email] = User(inst_email, google_email, "not_used", 1, "not_used")

    return to_return

# ------------------------------------------------------------------------------
# get db connection
def get_db_connection(portal_properties, port):

    # try and create a connection to the db
    try:
        connection = MySQLdb.connect(host=portal_properties.cgds_database_host, port=int(port),
                                     user=portal_properties.cgds_database_user,
                                     passwd=portal_properties.cgds_database_pw,
                                     db=portal_properties.cgds_database_name)
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
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
            print >> ERROR_FILE, 'Skipping invalid entry in property file: ' + line
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
        print >> ERROR_FILE, 'Missing one or more required properties, please check property file'
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

def manage_users(client, spreadsheet, cursor, worksheet_feed, portal_name, mskcc_user_spreadsheet):

    # get map of current portal users
    print >> OUTPUT_FILE, 'Getting list of current portal users'
    current_user_map = get_current_user_map(cursor)
    if current_user_map is not None:
        print >> OUTPUT_FILE, 'We have found %s current portal users' % len(current_user_map)
    else:
        print >> OUTPUT_FILE, 'Error reading user table'
        return None, None

    # get list of new users and insert
    print >> OUTPUT_FILE, 'Checking for new users'
    new_user_map = get_new_user_map(spreadsheet, worksheet_feed, current_user_map, portal_name, mskcc_user_spreadsheet)
    if (len(new_user_map) > 0):
        print >> OUTPUT_FILE, 'We have %s new user(s) to add' % len(new_user_map)
        emails_to_remove = insert_new_users(cursor, new_user_map.values())
        return new_user_map, emails_to_remove
    else:
        print >> OUTPUT_FILE, 'No new users to insert, exiting'
        return None, None

# ------------------------------------------------------------------------------
# updates user study access
def update_user_authorities(spreadsheet, cursor, worksheet_feed, portal_name, mskcc_user_spreadsheet):

        # get map of current portal users
        print >> OUTPUT_FILE, 'Getting list of current portal users from spreadsheet'
        all_user_map = get_new_user_map(spreadsheet, worksheet_feed, {}, portal_name, mskcc_user_spreadsheet)
        if all_user_map is None:
                return None;
        print >> OUTPUT_FILE, 'Updating authorities for each user in current portal user list'
        for user in all_user_map.values():
                worksheet_authorities = set(user.authorities)
                db_authorities = set(get_user_authorities(cursor, user.google_email))
                try:
                        cursor.executemany("insert into authorities values(%s, %s)",
                                           [(user.google_email, authority) for authority in worksheet_authorities - db_authorities])
                except MySQLdb.Error, msg:
                        print >> ERROR_FILE, msg

# ------------------------------------------------------------------------------
# Adds unknown users to user spreadsheet. MSKCC users are given default access.
# during MSK signon.  If this happens, we want to make sure they get into the google
# spreadsheet for tracking purposes.
def add_unknown_users_to_spreadsheet(client, cursor, spreadsheet, worksheet, mskcc_user_spreadsheet):

    # get map of all users in google spreadsheet and portal database
    worksheet_feed = get_worksheet_feed(client, spreadsheet, worksheet)
    google_spreadsheet_user_map = get_all_user_map(spreadsheet, worksheet_feed, mskcc_user_spreadsheet)
    portal_db_user_map = get_current_user_map(cursor)
    current_time = time.strftime("%m/%d/%y %H:%M:%S")
    # for each user in portal database not in google spreadsheet, insert user into google spreadsheet
    for email in portal_db_user_map.keys():
        if (email.endswith(MSKCC_EMAIL_SUFFIX) or email.endswith(SKI_EMAIL_SUFFIX)) and email not in google_spreadsheet_user_map:
            user = portal_db_user_map[email]
            print >> OUTPUT_FILE, user.name
            def_authorities = DEFAULT_AUTHORITIES + ";" + email[0:email.index('@')].upper()
            # we only got here if user was inserted via MSK AD - in which case name is formatted as:
            # Gross, Benjamin E./Sloan Kettering Institute
            if "/" in user.name:
                user_name_parts = user.name.split("/")
                row = { TIMESTAMP_KEY : current_time, MSKCC_EMAIL_KEY : user.inst_email, FULLNAME_KEY : user_name_parts[0], LAB_PI_KEY : user_name_parts[1], STATUS_KEY : STATUS_APPROVED, AUTHORITIES_KEY : def_authorities }
            else:
                row = { TIMESTAMP_KEY : current_time, MSKCC_EMAIL_KEY : user.inst_email, FULLNAME_KEY : user.name, STATUS_KEY : STATUS_APPROVED, AUTHORITIES_KEY : def_authorities }
            add_row_to_google_worksheet(client, spreadsheet, worksheet, row)


# ------------------------------------------------------------------------------
# adds a row to the google spreadsheet
def add_row_to_google_worksheet(client, spreadsheet, worksheet, row):
    ss_id = get_feed_id(client.GetSpreadsheetsFeed(), spreadsheet)
    ws_id = get_feed_id(client.GetWorksheetsFeed(ss_id), worksheet)
    client.InsertRow(row, ss_id, ws_id);

# ------------------------------------------------------------------------------
# gets email parameters from google spreadsheet

def get_email_parameters(google_spreadsheet,client):
    subject = ''
    body = ''
    print >> OUTPUT_FILE, 'Getting email parameters from google spreadsheet'
    email_worksheet_feed = get_worksheet_feed(client, google_spreadsheet, IMPORTER_WORKSHEET)
    for entry in email_worksheet_feed.entry:
        if entry.custom[SUBJECT_KEY].text is not None and entry.custom[BODY_KEY].text is not None:
            subject = entry.custom[SUBJECT_KEY].text.strip()
            body = entry.custom[BODY_KEY].text.strip()
    return subject, body

def get_portal_name_map(google_spreadsheet,client):
    portal_name = {}
    print >> OUTPUT_FILE, 'Getting access control parameter from google spreadsheet'
    access_control_worksheet_feed = get_worksheet_feed(client,google_spreadsheet,ACCESS_CONTROL_WORKSHEET)
    for entry in access_control_worksheet_feed.entry:
        if entry.custom[PORTAL_NAME_KEY] is not None and entry.custom[SPREADSHEET_NAME_KEY] is not None:
            portal_name[entry.custom[SPREADSHEET_NAME_KEY].text.strip()] = entry.custom[PORTAL_NAME_KEY].text.strip()
            if entry.custom[PORTAL_NAME_KEY].text.strip() == 'mskcc-portal':
                mskcc_user_spreadsheet = entry.custom[SPREADSHEET_NAME_KEY].text.strip()

    return portal_name,mskcc_user_spreadsheet


# ------------------------------------------------------------------------------
# displays program usage (invalid args)

def usage():
    print >> OUTPUT_FILE, 'importUsers.py --secrets-file [google secrets.json] --creds-file [oauth creds filename] --properties-file [properties file] --send-email-confirm [true or false] --use-institutional-id [true or false] --port [mysql port number] --sender [sender identifier - optional]'

# ------------------------------------------------------------------------------
# the big deal main.

def main():

    # parse command line options
    try:
        opts, args = getopt.getopt(sys.argv[1:], '', ['secrets-file=', 'creds-file=', 'properties-file=', 'send-email-confirm=', 'use-institutional-id=', 'port=', 'sender='])
    except getopt.error, msg:
        print >> ERROR_FILE, msg
        usage()
        sys.exit(2)

    # process the options
    secrets_filename = ''
    creds_filename = ''
    properties_filename = ''
    send_email_confirm = ''
    port = ''
    sender = ''

    for o, a in opts:
        if o == '--secrets-file':
            secrets_filename = a
        elif o == '--creds-file':
            creds_filename = a
        elif o == '--properties-file':
            properties_filename = a
        elif o == '--send-email-confirm':
            send_email_confirm = a
        elif o == '--sender':
            sender = a
        elif o == '--port':
            port = a

    if (secrets_filename == '' or creds_filename == '' or properties_filename == '' or send_email_confirm == '' or port == '' or
        (send_email_confirm != 'true' and send_email_confirm != 'false')):
        usage()
        sys.exit(2)

    # check existence of file
    if not os.path.exists(properties_filename):
        print >> ERROR_FILE, 'properties file cannot be found: ' + properties_filename
        sys.exit(2)

    # parse/get relevant portal properties
    print >> OUTPUT_FILE, 'Reading portal properties file: ' + properties_filename
    portal_properties = get_portal_properties(properties_filename)
    if not portal_properties:
        print >> OUTPUT_FILE, 'Error reading %s, exiting' % properties_filename
        return

    # get db connection & create cursor
    print >> OUTPUT_FILE, 'Connecting to database: ' + portal_properties.cgds_database_name
    connection = get_db_connection(portal_properties, port)
    if connection is not None:
        cursor = connection.cursor()
    else:
        print >> OUTPUT_FILE, 'Error connecting to database, exiting'
        return

    # login to google and get spreadsheet feed
    client = google_login(secrets_filename, creds_filename, portal_properties.google_id, portal_properties.google_pw, sys.argv[0])

    portal_name_map,mskcc_user_spreadsheet = get_portal_name_map(portal_properties.google_importer_spreadsheet,client)

    google_spreadsheets = portal_properties.google_spreadsheet.split(';')
    for google_spreadsheet in google_spreadsheets:
        if not google_spreadsheet == '':
            print >> OUTPUT_FILE, 'Importing ' + google_spreadsheet + ' ...'

            worksheet_feed = get_worksheet_feed(client, google_spreadsheet,
                                                portal_properties.google_worksheet)

            # the 'guts' of the script
            new_user_map, emails_to_remove = manage_users(client, google_spreadsheet, cursor, worksheet_feed, portal_name_map[google_spreadsheet], mskcc_user_spreadsheet)

            # update user authorities
            update_user_authorities(google_spreadsheet, cursor, worksheet_feed, portal_name_map[google_spreadsheet], mskcc_user_spreadsheet)

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
                            print >> OUTPUT_FILE, ('Sending confirmation email to new user: %s at %s' %
                                               (new_user.name, new_user.inst_email))
                            send_mail([new_user.inst_email],subject,body, sender = from_field, bcc = bcc_field)
                        else:
                            send_mail([new_user_key], error_subject, error_body, sender = from_field, bcc = bcc_field)

            if google_spreadsheet == mskcc_user_spreadsheet:
                add_unknown_users_to_spreadsheet(client, cursor, google_spreadsheet, portal_properties.google_worksheet,mskcc_user_spreadsheet)

    # clean up
    cursor.close()
    connection.commit()
    connection.close()


# ------------------------------------------------------------------------------
# ready to roll

if __name__ == '__main__':
    main()
