import sys
import os
import optparse
import datetime

import smtplib
from email.MIMEMultipart import MIMEMultipart
from email.MIMEBase import MIMEBase
from email.MIMEText import MIMEText
from email.Utils import COMMASPACE, formatdate
from email import Encoders
import MySQLdb

import gdata.docs.client
import gdata.docs.service
import gdata.spreadsheet.service

import httplib2
from oauth2client import client
from oauth2client.file import Storage
from oauth2client.client import flow_from_clientsecrets
from oauth2client.tools import run_flow, argparser

import ldap
import ldap.filter
import traceback

# ------------------------------------------------------------------------------
# globals
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
LDAP_USERNAME = 'ldap.username'
LDAP_PASSWORD = 'ldap.password'
LDAP_SERVER = 'ldap.server'
LDAP_BASE_DN = 'ldap.base_dn'

# column constants on google spreadsheet
FULLNAME_KEY = 'fullname'
INST_EMAIL_KEY = "institutionalemailaddress"
MSKCC_EMAIL_KEY = "mskccemailaddress"
OPENID_EMAIL_KEY = "googleoropenidaddress"
STATUS_KEY = 'statusapprovedorblank'
AUTHORITIES_KEY = 'authoritiesalloralltcgaandorsemicolondelimitedcancerstudylist'
LAB_PI_KEY = 'labpi'
TIMESTAMP_KEY = 'timestamp'
SUBJECT_KEY = 'subject'
BODY_KEY = 'body'
PORTAL_NAME_KEY = 'portalname'
SPREADSHEET_NAME_KEY = 'spreadsheetname'
ACCESS_CONTROL_WORKSHEET = 'access_control'

# possible values in status column
STATUS_APPROVED = "APPROVED"

MSKCC_EMAIL_SUFFIX = '@mskcc.org'
MSKCC_APP_NAME = 'mskcc-portal'
MYSQL_PORT = 3306
SPREADSHEET_USERS_REMOVED_FILENAME = 'ldap_spreadsheet_users_removed.txt'
DB_USERS_REMOVED_FILENAME = 'ldap_db_users_removed.txt'
PIPELINES_EMAIL = 'cbioportal-pipelines@cbioportal.org'
MESSAGE_SUBJECT = 'LDAP user removal summary'
SMTP_SERVER = 'smtp.gmail.com'

# LDAPURLS = ["ldaps://ldapha.mskcc.root.mskcc.org/",
#             "ldap://ldapglb.mskcc.org:389/", this URL works from pipelines
#             "ldaps://ldapglb.mskcc.org:389/",
#             "ldaps://ldapglb.mskcc.org:636/<ldaps://ldapglb.mskcc.org:389/>",
#             "ldaps://ldapglb.mskcc.org:636/",
#             "ldaps://ldapha.mskcc.root.mskcc.org/"]

# ------------------------------------------------------------------------------
# class defintions
class PortalProperties(object):
    def __init__(self, properties_filename):
        properties = self.parse_properties(properties_filename)
        self.cgds_database_host = properties[CGDS_DATABASE_HOST]
        self.cgds_database_name = properties[CGDS_DATABASE_NAME]
        self.cgds_database_user = properties[CGDS_DATABASE_USER]
        self.cgds_database_pw = properties[CGDS_DATABASE_PW]
        self.google_id = properties[GOOGLE_ID]
        self.google_pw = properties[GOOGLE_PW]
        self.google_spreadsheet = properties[CGDS_USERS_SPREADSHEET]
        self.google_worksheet = properties[CGDS_USERS_WORKSHEET]
        self.google_importer_spreadsheet = properties[IMPORTER_SPREADSHEET]
        self.ldap_user = properties[LDAP_USERNAME]
        self.ldap_pw = properties[LDAP_PASSWORD]
        self.ldap_server = properties[LDAP_SERVER]
        self.ldap_base_dn = properties[LDAP_BASE_DN]

    def parse_properties(self, properties_filename):
        properties = {}
        with open(properties_filename, 'rU') as properties_file:
            for line in properties_file:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                property = map(str.strip, line.split('='))
                if line.startswith(CGDS_USERS_SPREADSHEET) or line.startswith(LDAP_BASE_DN):
                    property = [property[0], line[line.index('=')+1:len(line)]]
                if len(property) != 2:
                    print >> ERROR_FILE, 'Skipping invalid entry in property file: ' + line
                    continue
                properties[property[0]] = property[1]
        # error check
        if (CGDS_DATABASE_HOST not in properties or len(properties[CGDS_DATABASE_HOST]) == 0 or
            CGDS_DATABASE_NAME not in properties or len(properties[CGDS_DATABASE_NAME]) == 0 or
            CGDS_DATABASE_USER not in properties or len(properties[CGDS_DATABASE_USER]) == 0 or
            CGDS_DATABASE_PW not in properties or len(properties[CGDS_DATABASE_PW]) == 0 or
            GOOGLE_ID not in properties or len(properties[GOOGLE_ID]) == 0 or
            GOOGLE_PW not in properties or len(properties[GOOGLE_PW]) == 0 or
            CGDS_USERS_SPREADSHEET not in properties or len(properties[CGDS_USERS_SPREADSHEET]) == 0 or
            CGDS_USERS_WORKSHEET not in properties or len(properties[CGDS_USERS_WORKSHEET]) == 0 or
            IMPORTER_SPREADSHEET not in properties or len(properties[IMPORTER_SPREADSHEET]) == 0 or
            LDAP_USERNAME not in properties or len(properties[LDAP_USERNAME]) == 0 or
            LDAP_PASSWORD not in properties or len(properties[LDAP_PASSWORD]) == 0 or
            LDAP_SERVER not in properties or len(properties[LDAP_SERVER]) == 0 or
            LDAP_BASE_DN not in properties or len(properties[LDAP_BASE_DN]) == 0):
            print >> ERROR_FILE, 'Missing one or more required properties, please check property file'
            sys.exit(2)
        return properties

# ------------------------------------------------------------------------------
# functions

# ------------------------------------------------------------------------------
# Google client functions

def get_gdata_credentials(secrets, creds, scope, force=False):
    '''
        Returns GData credentials.
    '''
    storage = Storage(creds)
    credentials = storage.get()
    if credentials.access_token_expired:
        credentials.refresh(httplib2.Http())
    if credentials is None or credentials.invalid or force:
        credentials = run_flow(flow_from_clientsecrets(secrets, scope=scope), storage, argparser.parse_args([]))
    return credentials

def google_login(secrets, creds, user, pw, app_name):
    '''
        Establishes Google login.
    '''
    credentials = get_gdata_credentials(secrets, creds, ["https://spreadsheets.google.com/feeds"], False)
    client = gdata.spreadsheet.service.SpreadsheetsService(additional_headers={'Authorization' : 'Bearer %s' % credentials.access_token})

    # google spreadsheet
    client.email = user
    client.password = pw
    client.source = app_name
    client.ProgrammaticLogin()

    return client

def get_feed_id(feed, name):
    '''
        Returns Google spreadsheet / worksheet feed id.
    '''
    to_return = ''
    for entry in feed.entry:
        if entry.title.text.strip() == name:
            id_parts = entry.id.text.split('/')
            to_return = id_parts[len(id_parts) - 1]
    return to_return

def get_worksheet_feed(client, ss, ws):
    '''
        Returns Google worksheet feed.
    '''
    try:
        ss_id = get_feed_id(client.GetSpreadsheetsFeed(), ss)
        ws_id = get_feed_id(client.GetWorksheetsFeed(ss_id), ws)
        list_feed = client.GetListFeed(ss_id, ws_id)
    except gdata.service.RequestError:
        print >> ERROR_FILE, 'There was an error connecting to google for (spreadsheet, worksheet): (' + ss + ', ' + ws + ')'
        sys.exit(0)
    return list_feed

def get_portal_name_map(client, google_spreadsheet):
    '''
        Returns map of spreadsheet name to portal name from portal importer configuration spreadsheet access control worksheet.'
    '''
    portal_name_map = {}
    print >> OUTPUT_FILE, 'Getting access control parameter from google spreadsheet...'
    access_control_worksheet_feed = get_worksheet_feed(client, google_spreadsheet, ACCESS_CONTROL_WORKSHEET)
    for entry in access_control_worksheet_feed.entry:
        if entry.custom[PORTAL_NAME_KEY] is not None and entry.custom[SPREADSHEET_NAME_KEY] is not None:
            portal_name_map[entry.custom[SPREADSHEET_NAME_KEY].text.strip()] = entry.custom[PORTAL_NAME_KEY].text.strip()
    return portal_name_map

def get_mskcc_email_from_worksheet_entry(worksheet_feed_entry, portal_name):
    '''
        Returns the MSK email address from the current worksheet feed entry.
        If there is not MSK email address found then None is returned.
    '''
    if portal_name == MSKCC_APP_NAME:
        return worksheet_feed_entry.custom[MSKCC_EMAIL_KEY].text.strip().lower()
    else:
        inst_email = worksheet_feed_entry.custom[INST_EMAIL_KEY].text
        if inst_email and inst_email.strip().lower().endswith(MSKCC_EMAIL_SUFFIX):
            return inst_email.strip().lower()
        # try google / open id email value if inst_email is not MSK
        google_email = worksheet_feed_entry.custom[OPENID_EMAIL_KEY].text
        if google_email and google_email.strip().lower().endswith(MSKCC_EMAIL_SUFFIX):
            return google_email.strip().lower()
    return None


def get_current_ldap_spreadsheet_users(client, spreadsheet, worksheet, portal_name):
    '''
        Returns set of current users in MSKCC request access spreadsheet.
    '''
    worksheet_feed = get_worksheet_feed(client, spreadsheet, worksheet)
    to_return = set()
    for entry in worksheet_feed.entry:
        mskcc_email = get_mskcc_email_from_worksheet_entry(entry, portal_name)
        # skip users that are not MSK
        if not mskcc_email:
            continue
        to_return.add(mskcc_email)
    return to_return

def delete_users_from_spreadsheet(client, spreadsheet, worksheet, spreadsheet_users_to_remove, portal_name):
    '''
        Delete entries from spreadsheet if email address in list of spreadsheet users to remove.
    '''
    # rows must be deleted in reverse order OR ELSE! ...
    # deleting worksheet rows from top-bottom messes with the worksheet indexing
    # causing the wrong rows to be deleted
    worksheet_feed = get_worksheet_feed(client, spreadsheet, worksheet)
    for entry in worksheet_feed.entry[::-1]:
        mskcc_email = get_mskcc_email_from_worksheet_entry(entry, portal_name)
        if mskcc_email in spreadsheet_users_to_remove:
            client.DeleteRow(entry)

def process_user_access_spreadsheet(client, portal_name, properties, spreadsheet, ldap_connection):
    '''
        Checks users in Google spreadsheets against LDAP server.
    '''
    # get current spreadsheet usernames and check them against LDAP server
    google_spreadsheet_users = get_current_ldap_spreadsheet_users(client, spreadsheet, properties.google_worksheet, portal_name)
    spreadsheet_users_to_remove = find_deactivated_ldap_users(properties, ldap_connection, google_spreadsheet_users)

    # remove deactivated users from spreadsheet if any found
    if len(spreadsheet_users_to_remove) > 0:
        print >> OUTPUT_FILE, 'Removing ' + str(len(spreadsheet_users_to_remove)) + ' users from spreadsheet "' + spreadsheet + '"...'
        delete_users_from_spreadsheet(client, spreadsheet, properties.google_worksheet, spreadsheet_users_to_remove, portal_name)
    else:
        print >> OUTPUT_FILE, 'No deactivated LDAP users in Google Spreadsheet "' + spreadsheet + '", nothing to do...'
    return spreadsheet_users_to_remove

def find_and_remove_deactivated_ldap_spreadsheet_users(client, properties, ldap_connection, ss_users_removed_filepath):
    '''
        Finds and removes MSK users from all user access Google spreadsheets.
    '''
    spreadsheet_users_to_remove = set()
    portal_name_map = get_portal_name_map(client, properties.google_importer_spreadsheet)
    google_spreadsheets = [spreadsheet for spreadsheet in properties.google_spreadsheet.split(';') if spreadsheet != '']
    for spreadsheet in google_spreadsheets:
        # get corresponding portal name for spreadsheet and process current spreadsheet
        portal_name = portal_name_map[spreadsheet]
        if not portal_name:
            print >> ERROR_FILE, 'Could not find portal name for spreadsheet: "' + spreadsheet + '"'
            continue
        users_to_remove = process_user_access_spreadsheet(client, portal_name, properties, spreadsheet, ldap_connection)
        if users_to_remove:
            spreadsheet_users_to_remove.update(users_to_remove)

    # save list of users removed from spreadsheets
    if len(spreadsheet_users_to_remove) > 0:
        save_users_removed_list(ss_users_removed_filepath, spreadsheet_users_to_remove)
    return spreadsheet_users_to_remove

# ------------------------------------------------------------------------------
# db functions

def establish_db_connection(properties):
    '''
        Establishes database connection.
    '''
    try:
        connection = MySQLdb.connect(host=properties.cgds_database_host, port=MYSQL_PORT,
                            user=properties.cgds_database_user,
                            passwd=properties.cgds_database_pw,
                            db=properties.cgds_database_name)
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        return None
    return connection

def get_current_db_user_map(cursor):
    '''
        Returns set of current databse users with matching authorities to MSKCC_APP_NAME ('mskcc-portal').

        database.`users` table schema:
            ['EMAIL', 'NAME', 'ENABLED']
    '''
    to_return = set()
    try:
        cursor.execute('SELECT * FROM users WHERE email LIKE "%' +  MSKCC_EMAIL_SUFFIX + '"')
        for row in cursor.fetchall():
            to_return.add(row[0].lower())
    except MySQLdb.Error, msg:
        print >> ERROR_FILE, msg
        return None
    return to_return

def delete_users_from_database(cursor, db_users_to_remove):
    '''
        Delete users form database.
    '''
    for user in db_users_to_remove:
        try:
            cursor.execute('DELETE FROM users WHERE email = "' + user + '"')
        except MySQLdb.Error, msg:
            print >> ERROR_FILE, msg
            sys.exit(2)

def find_and_remove_deactivated_ldap_db_users(db_connection, properties, ldap_connection, db_users_removed_filepath):
    '''
        Queries database for users and checks them against the LDAP server.
    '''
    if db_connection is not None:
        cursor = db_connection.cursor()
    else:
        print >> OUTPUT_FILE, 'Error connecting to database, exiting'
        sys.exit(2)

    # get current db users and check them against LDAP server
    db_users = get_current_db_user_map(cursor)
    db_users_to_remove = find_deactivated_ldap_users(properties, ldap_connection, db_users)

    # remove users from db and save results if any found, otherwise do nothing
    if len(db_users_to_remove) > 0:
        print >> OUTPUT_FILE, 'Removing ' + str(len(db_users_to_remove)) + ' users from database...'
        delete_users_from_database(cursor, db_users_to_remove)
        db_connection.commit()
        save_users_removed_list(db_users_removed_filepath, db_users_to_remove)
    else:
        print >> OUTPUT_FILE, 'No deactivated LDAP users found in database, nothing to do...'
    db_connection.close()
    return db_users_to_remove

# ------------------------------------------------------------------------------
# ldap functions

def generate_ldap_users_removed_messag_body(users_removed, users_type, filename):
    '''
        Returns formatted message body.
    '''
    message = users_type + ' users removed:  ' + str(len(users_removed)) + ' user(s)'
    if len(users_removed) > 0:
        message += '\n\t --> list of users saved to: ' + filename
    message += '\n\n'
    return message

def email_ldap_summary_results(db_users_removed_filepath, db_users_removed, ss_users_removed_filepath, spreadsheet_users_removed, gmail_username, gmail_password):
    '''
        Send LDAP summary email.
    '''
    msg = MIMEMultipart()
    msg['Subject'] = MESSAGE_SUBJECT
    msg['From'] = PIPELINES_EMAIL
    msg['To'] = PIPELINES_EMAIL
    msg['Date'] = formatdate(localtime=True)

    # construct message body
    message_body = ''
    message_body += generate_ldap_users_removed_messag_body(db_users_removed, 'Database', db_users_removed_filepath)
    message_body += generate_ldap_users_removed_messag_body(spreadsheet_users_removed, 'Google Spreadsheet', ss_users_removed_filepath)

    msg.attach(MIMEText(message_body))
    smtp = smtplib.SMTP_SSL(SMTP_SERVER, 465)
    smtp.login(gmail_username, gmail_password)
    smtp.sendmail(PIPELINES_EMAIL, PIPELINES_EMAIL, msg.as_string())
    smtp.close()

def save_users_removed_list(filename, users_removed):
    '''
        Save list of users removed from Google spreadsheet
        and/or database to file.
    '''
    output_file = open(filename, 'w')
    output_file.write('\n'.join(users_removed) + '\n')
    print >> OUTPUT_FILE, 'Saved list of users removed to: ' + filename

def establish_ldap_connection(properties):
    '''
        Connects to LDAP server.
    '''
    ldap_connection = ldap.initialize(properties.ldap_server)
    ldap_connection.set_option(ldap.OPT_REFERRALS, 0)
    ldap_connection.simple_bind_s(properties.ldap_user, properties.ldap_pw)
    return ldap_connection

def get_ldap_user_details(properties, ldap_connection, user_id):
    '''
        Queries LDAP server for user details.

        Response JSON schema:
            attr_result:  {
                'mail': ['username@mskcc.org'],
                'displayName': ['Last Name, First Name'],
                'sAMAccountName': ['username']
            }
        Returns MSK email account or None if user does not exist.
    '''
    filters = "(&(objectclass=person)(displayName=*)(sAMAccountName=%s))" % ldap.filter.escape_filter_chars(user_id)
    attrs = ['sAMAccountName', 'displayName', 'mail']
    ldap_results = ldap_connection.search_s(properties.ldap_base_dn, ldap.SCOPE_SUBTREE, filters, attrs)
    result = [attr_result for (code, attr_result) in ldap_results if code != None and 'sAMAccountName' in attr_result and 'displayName' in attr_result and 'mail' in attr_result]
    if result:
        return result[0]['mail'][0]
    return None


def find_deactivated_ldap_users(properties, ldap_connection, users):
    '''
        Queries LDAP server for user details.
        Returns list of deactivated users.
    '''
    users_to_remove = set()
    for user in users:
        user_details = get_ldap_user_details(properties, ldap_connection, user.split('@')[0])
        if not user_details:
            users_to_remove.add(user)
    return users_to_remove

def purge_deactivated_ldap_users(properties, secrets_filename, creds_filename, tmp_directory, gmail_username, gmail_password):
    '''
        Purge deactivated LDAP users from database and google spreadsheet.
        Log users that are removed.
    '''
    ldap_connection = establish_ldap_connection(properties)
    db_connection = establish_db_connection(properties)
    client = google_login(secrets_filename, creds_filename, properties.google_id, properties.google_pw, MSKCC_APP_NAME)

    # filenames where list of users removed are stored
    now = datetime.datetime.now().strftime('%Y-%m-%d')
    db_users_removed_filepath = os.path.join(tmp_directory, DB_USERS_REMOVED_FILENAME + '.' + now)
    ss_users_removed_filepath = os.path.join(tmp_directory, SPREADSHEET_USERS_REMOVED_FILENAME + '.' + now)

    db_users_removed = find_and_remove_deactivated_ldap_db_users(db_connection, properties, ldap_connection, db_users_removed_filepath)
    spreadsheet_users_removed = find_and_remove_deactivated_ldap_spreadsheet_users(client, properties, ldap_connection, ss_users_removed_filepath)

    # save results and send LDAP summary email
    email_ldap_summary_results(db_users_removed_filepath, db_users_removed, ss_users_removed_filepath, spreadsheet_users_removed, gmail_username, gmail_password)
    ldap_connection.unbind_s()

def usage(parser):
    print >> OUTPUT_FILE, parser.print_help()
    sys.exit(2)

def main():
    # parse command line options
    parser = optparse.OptionParser()
    parser.add_option('-p', '--properties-file', action = 'store', dest = 'properties', help = 'portal properties file')
    parser.add_option('-s', '--secrets-file', action = 'store', dest = 'secrets', help = 'google secrets.json')
    parser.add_option('-c', '--creds-file', action = 'store', dest = 'creds', help = 'oauth creds filename')
    parser.add_option('-t', '--tmp-directory', action = 'store', dest = 'tmpdir', help = 'tmp directory where purged usernames are written to')
    parser.add_argument('-g', '--gmail-password', action = 'store', dest = 'gmail_password', required = True, help = 'Gmail SMTP password')
    parser.add_argument('-u', '--gmail-username', action = 'store', dest = 'gmail_username', required = True, help = 'Gmail username')
    
    (options, args) = parser.parse_args()
    secrets_filename = options.secrets
    creds_filename = options.creds
    properties_filename = options.properties
    tmp_directory = options.tmpdir
    gmail_username = options.gmail_username
    gmail_password = options.gmail_password

    if not secrets_filename or not creds_filename or not properties_filename or not tmp_directory:
        usage(parser)

    properties = PortalProperties(properties_filename)
    purge_deactivated_ldap_users(properties, secrets_filename, creds_filename, tmp_directory, gmail_username, gmail_password)

if __name__ == '__main__':
    main()
