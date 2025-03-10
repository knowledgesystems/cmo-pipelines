/* Copyright (c) 2021, 2022, 2023, 2024 Memorial Sloan Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan Kettering Cancer Center
 * has been advised of the possibility of such damage.
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* import-tool.cc
 *
 * This tool allows regular users to trigger/request a run of the cancer study
 * import pipeline on the import server. We utilize a linux service account
 * with userid 'cbioportal_importer' to import cancer studies. Import jobs run
 * in processes owned by this linux user, and relevant data and program files
 * needed are owned by this user.
 *
 * The design of the importer system now includes a designated linux directory:
 * /data/portal-cron/import-trigger which is frequently monitored by processes
 * owned by the cbioportal_importer user. This tool allows any user with access
 * to the import pipeline server to create a trigger file in this directory.
 * Importantly, the created file will be owned by the cbioportal_importer user,
 * allowing appropriate clearing of the file when the import run finishes. To
 * make this possible, the compiled executable of this source code will be
 * owned by cbioportal_importer, will be executable by any user on the server,
 * and will have the linux Setuid file permission enabled so that the running
 * program will execute with the effective user id of cbioportal_importer. This
 * also allows the program to open and read other files owned by
 * cbioportal_importer, such as the log file generated by the import process.
 *
 * This program takes command line arguments:
 *   - the name of the importer to interact with (currently only 'triage')
 *   - a command argument, one of {start, kill, status, log}
 *   - optional extra arguments, depending on the command argument
 *
 * The start and kill commands each essentially create a designated file in the
 * trigger directory using the c++ fstream library.
 *
 * The status command uses the linux system call 'stat' to check for the
 * presence of various trigger and process indicator files in the trigger
 * directory. It also uses the 'time' system call to get the current system
 * time and the 'stat' system call to obtain the last modification time of
 * the importer log file. It formats and prints a short status report.
 *
 * The log command uses a combination of the linux POSIX library functions
 * 'fork', 'execl', and 'waitpid' to execute a subprocess which uses the
 * standard system utility program 'tail' to print a chosen number of lines
 * from the end of the importer log file.
 */

#include <cstdlib>
#include <ctime>
#include <errno.h>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <libgen.h>
#include <sstream>
#include <string.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>

using namespace std;

static string IMPORT_TRIGGER_BASEDIR("/data/portal-cron/import-trigger");
static string IMPORT_LOG_BASEDIR("/data/portal-cron/logs");
static string CONFIG_BASEDIR("/data/portal-cron/etc/import-tool");
static string CONFIG_FILENAME(CONFIG_BASEDIR + "/import-tool_config");
static string START_GENIE_IMPORT_TRIGGER_FILENAME(IMPORT_TRIGGER_BASEDIR + "/genie-import-start-request");
static string KILL_GENIE_IMPORT_TRIGGER_FILENAME(IMPORT_TRIGGER_BASEDIR + "/genie-import-kill-request");
static string GENIE_IMPORT_IN_PROGRESS_FILENAME(IMPORT_TRIGGER_BASEDIR + "/genie-import-in-progress");
static string GENIE_IMPORT_KILLING_FILENAME(IMPORT_TRIGGER_BASEDIR + "/genie-import-killing");
static string GENIE_IMPORT_LOG_FILENAME(IMPORT_LOG_BASEDIR + "/genie-aws-importer.log");
static string START_TRIAGE_IMPORT_TRIGGER_FILENAME(IMPORT_TRIGGER_BASEDIR + "/triage-import-start-request");
static string KILL_TRIAGE_IMPORT_TRIGGER_FILENAME(IMPORT_TRIGGER_BASEDIR + "/triage-import-kill-request");
static string TRIAGE_IMPORT_IN_PROGRESS_FILENAME(IMPORT_TRIGGER_BASEDIR + "/triage-import-in-progress");
static string TRIAGE_IMPORT_KILLING_FILENAME(IMPORT_TRIGGER_BASEDIR + "/triage-import-killing");
static string TRIAGE_IMPORT_LOG_FILENAME(IMPORT_LOG_BASEDIR + "/triage-cmo-importer.log");
static string START_HGNC_IMPORT_TRIGGER_FILENAME(IMPORT_TRIGGER_BASEDIR + "/hgnc-import-start-request");
static string KILL_HGNC_IMPORT_TRIGGER_FILENAME(IMPORT_TRIGGER_BASEDIR + "/hgnc-import-kill-request");
static string HGNC_IMPORT_IN_PROGRESS_FILENAME(IMPORT_TRIGGER_BASEDIR + "/hgnc-import-in-progress");
static string HGNC_IMPORT_KILLING_FILENAME(IMPORT_TRIGGER_BASEDIR + "/hgnc-import-killing");
static string HGNC_IMPORT_LOG_FILENAME(IMPORT_LOG_BASEDIR + "/hgnc-importer.log");

/* used for command line argument checking */
static vector<string> RECOGNIZED_IMPORTERS;
static vector<string> MANAGED_IMPORTERS;
static vector<string> RECOGNIZED_COMMANDS;

/* character_vector_from_string()
 * This function is needed because many cstdlib functions and system calls
 * are declared as accepting an object of type 'char *', but the c++ string
 * type provides objects of type 'const char *'. This function converts
 * a const char * object from the c++ string type (such as from s.c_str())
 * to a newly allocated object from which we can obtain 'char *' using
 * &character_vector[0]. These STL objects are automatically reclaimed when
 * they go out of scope.
 */
vector<char> character_vector_from_string(string s) {
    vector<char> v(s.begin(), s.end());
    v.push_back((char)0);
    return v;
}

/* string_is_in_vector()
 * Search in an unordered vector of strings
 */
bool string_is_in_vector(string s, const vector<string> &vector_of_strings) {
    for (int i = 0; i < vector_of_strings.size(); i++) {
        if (s.compare(vector_of_strings[i]) == 0) {
            return true;
        }
    }
    return false;
}

/* file_exists()
 * Uses 'stat' system call to see if file exists
 */
bool file_exists(string file_path) {
    vector<char> file_path_vector = character_vector_from_string(file_path);
    struct stat file_stat;
    if (stat(&file_path_vector[0], &file_stat) != 0) {
        int error_number = errno;
        if (error_number == ENOENT) {
            return false; // this is the expected error when the file does not exist
        }
        if (error_number == EACCES) {
            cerr << "Error : no permission to access path to trigger directory " << file_path << endl << endl;
            return false;
        }
        if (error_number == ENOTDIR) {
            cerr << "Error : path to trigger directory contains non-directory elements : " << file_path << endl << endl;
            return false;
        }
        cerr << "Error : an error (" << strerror(error_number) << ") occurred during check of directory_exists() of " << file_path << endl << endl;
        return false;
    }
    return S_ISREG(file_stat.st_mode);
}

/* directory_exists()
 * Uses 'stat' system call to see if directory exists
 */
bool directory_exists(string dir_path) {
    vector<char> dir_path_vector = character_vector_from_string(dir_path);
    struct stat dir_stat;
    if (stat(&dir_path_vector[0], &dir_stat) != 0) {
        int error_number = errno;
        if (error_number == ENOENT) {
            return false; // this is the expected error when the directory does not exist
        }
        if (error_number == EACCES) {
            cerr << "Error : no permission to access path to trigger directory " << dir_path << endl << endl;
            return false;
        }
        if (error_number == ENOTDIR) {
            cerr << "Error : path to trigger directory contains non-directory elements : " << dir_path << endl << endl;
            return false;
        }
        cerr << "Error : an error (" << strerror(error_number) << ") occurred during check of directory_exists() of " << dir_path << endl << endl;
        return false;
    }
    return S_ISDIR(dir_stat.st_mode);
}

/* initialize_static_managed_importers()
 * assume that every recognized importer is managed on this server unless a config file
 * is present on the system. If the config file is there, then the names of the importers
 * which are managed on this system are read from the config file. (They will only be
 * recognized if they are on the RECOGNIZED_IMPORTERS list)
 */
void initialize_static_managed_importers() {
    if (!file_exists(CONFIG_FILENAME)) {
        /* without a config file assume all recognized importers are managed on this server */
        for (int i = 0; i < RECOGNIZED_IMPORTERS.size(); i++) {
            MANAGED_IMPORTERS.push_back(RECOGNIZED_IMPORTERS[i]);
        }
        return;
    }
    fstream config_file;
    config_file.open(CONFIG_FILENAME.c_str(), ios::in);
    string line;
    while (getline(config_file, line)) {
        if (string_is_in_vector(line, RECOGNIZED_IMPORTERS)) {
            MANAGED_IMPORTERS.push_back(line);
        }
    }
    config_file.close();
}

void initialize_static_objects() {
    RECOGNIZED_IMPORTERS.push_back("genie");
    RECOGNIZED_IMPORTERS.push_back("triage");
    RECOGNIZED_IMPORTERS.push_back("hgnc");
    initialize_static_managed_importers();
    RECOGNIZED_COMMANDS.push_back("start");
    RECOGNIZED_COMMANDS.push_back("kill");
    RECOGNIZED_COMMANDS.push_back("status");
    RECOGNIZED_COMMANDS.push_back("log");
}

/* get_hostname()
 * Uses 'gethostname' system call
 */
string get_hostname() {
    char hostname_array[257] = "";
    if (gethostname(hostname_array, 256) != 0) {
        return "Undetermined";
    }
    hostname_array[256] = '\0';
    stringstream oss;
    oss << hostname_array;
    return oss.str();
}

void print_usage(string program_name) {
    vector<char> program_name_vector = character_vector_from_string(program_name);
    cerr << "Usage: " << basename(&program_name_vector[0]) << " importer_name command [extra_arguments]" << endl;
    cerr << "       importer_name must be \"genie\" or \"triage\" or \"hgnc\"" << endl;
    cerr << "       valid commands:" << endl;
    cerr << "           start : requests that an import run begins as soon as possible - this may wait for an import in progress to finish before starting" << endl;
    cerr << "           kill : requests that any import in progress be halted and that any requested start be canceled" << endl;
    cerr << "           status : print a report on pending requests and any import in progress" << endl;
    cerr << "           log : print the last N lines of the importer log file. N defaults to 1000, or can be set using extra_arguments." << endl;
}

bool is_recognized_importer(string s) {
    return string_is_in_vector(s, RECOGNIZED_IMPORTERS);
}

bool is_managed_importer(string s) {
    return string_is_in_vector(s, MANAGED_IMPORTERS);
}

bool is_recognized_command(string s) {
    return string_is_in_vector(s, RECOGNIZED_COMMANDS);
}

bool strings_are_equal(string s1, string s2) {
    return s1.compare(s2) == 0;
}

bool string_is_a_positive_integer(string s) {
    /* ignores flanking whitespace */
    stringstream iss(s);
    int i(0);
    iss >> skipws >> i;
    if (iss.fail()) {
        return false;
    }
    if (iss.eof()) {
        return i > 0;
    }
    // something remaining in string suffix ... allow whitespace but nothing else
    char a(0);
    iss >> skipws >> a;
    if (iss.bad()) {
        return false;
    }
    return i > 0 && a == (char)0;
}

bool arguments_are_valid(int argc, char **argv) {
    if (argc < 3) {
        return false; // must have at least importer and command args
    }
    if (! is_recognized_importer(argv[1])) {
        cerr << "Error : " << argv[1] << " is not a valid importer name" << endl << endl;
        return false;
    }
    if (! is_managed_importer(argv[1])) {
        cerr << "Error : " << argv[1] << " is a recognized importer, but is not managed on this server (" << get_hostname() << ")" << endl << endl;
        return false;
    }
    if (! is_recognized_command(argv[2])) {
        cerr << "Error : " << argv[2] << " is not a valid command" << endl << endl;
        return false;
    }
    if (strings_are_equal(argv[2], "start") ||
            strings_are_equal(argv[2], "kill") ||
            strings_are_equal(argv[2], "status")) {
        if (argc > 3) {
            cerr << "Error : " << argv[2] << " does not accept extra arguments" << endl << endl;
            return false;
        }
    }
    if (strings_are_equal(argv[2], "log")) {
        if (argc > 4) {
            cerr << "Error : " << argv[2] << " accepts only a single extra argument. More than one received." << endl << endl;
            return false;
        }
        if (argc == 4) {
            if (! string_is_a_positive_integer(argv[3])) {
                cerr << "Error : " << argv[2] << " allows an extra argument, but it must be a positive integer (max 2147483647). Received: " << argv[3] << endl << endl;
                return false;
            }
        }
    }
    return true;
}

void extract_extra_args(vector<string> & extra_args, int argc, char **argv) {
    for (int i = 3; i < argc; i++) {
        string arg(argv[i]);
        extra_args.push_back(arg);
    }
}

/* create_directory()
 * This functions similar to 'mkdir -p' - the path passed as an argument is
 * traversed upwards until an existing parent directory is found to exist,
 * and then each traversed subdirectory is created recursively.
 */
void create_directory(string dir_path, bool create_parent_dirs, bool exit_on_fail) {
    if (dir_path == "" || dir_path == "/") {
        return; // root directory always exists
    }
    vector<char> dir_path_vector = character_vector_from_string(dir_path);
    if (create_parent_dirs) {
        string dirname_base(basename(&dir_path_vector[0]));
        string parent_dir_path = dir_path.substr(0, dir_path.length() - (dirname_base.length() + 1));
        if (! directory_exists(parent_dir_path)) {
            create_directory(parent_dir_path, true, false);
        }
    }
    if (mkdir(&dir_path_vector[0], 0755) != 0) {
        if (exit_on_fail) {
            cerr << "Error : could not create directory " << dir_path << endl << flush;
            exit(1);
        }
    }
}

void create_base_directory_if_absent() {
    if (! directory_exists(IMPORT_TRIGGER_BASEDIR)) {
        create_directory(IMPORT_TRIGGER_BASEDIR, true, true);
    }
}

/* create_trigger_file()
 * File is created using fstream, and then success is tested
 */
int create_trigger_file(string filepath) {
    create_base_directory_if_absent();
    if (file_exists(filepath)) {
        return 0; // already exists
    }
    fstream my_file;
    my_file.open(filepath.c_str(), ios::out);
    my_file.close();
    if (file_exists(filepath)) {
        string hostname = get_hostname();
        cout << "created file on " << hostname << " server : " << filepath << endl;
        return 0;
    }
    cerr << "Error: could not create file " << filepath << endl;
    return 1;
}

/* format_utc_epoc_count_as_local_time()
 * use cstdlib function 'localtime' to parse time fields
 */
string format_utc_epoc_count_as_local_time(time_t t) {
    tm *local_time = localtime(&t);
    stringstream oss;
    oss << setfill('0') << (local_time->tm_year + 1900) << " " <<
            setw(2) << (local_time->tm_mon + 1) << " " << setw(2) << local_time->tm_mday << "  " <<
            setw(2) << local_time->tm_hour << ":" << setw(2) << local_time->tm_min << ":" << setw(2) << local_time->tm_sec;
    return oss.str();
}

/* get_current_time()
 * Uses 'time' system call
 */
string get_current_time() {
    time_t utc_epoc_count = time(0);
    return format_utc_epoc_count_as_local_time(utc_epoc_count);
}

/* get_file_modification_time()
 * Uses 'stat' system call
 */
string get_file_modification_time(string file_path) {
    vector<char> file_path_vector = character_vector_from_string(file_path);
    struct stat file_stat;
    if (stat(&file_path_vector[0], &file_stat) != 0) {
        return "Undetermined";
    }
    time_t last_modification_time(file_stat.st_mtime);
    return format_utc_epoc_count_as_local_time(last_modification_time);
}

// this function assumes importer arg is valid
int request_importer_start(string importer, string start_trigger_filename, string kill_trigger_filename, string killing_filename) {
    if (file_exists(killing_filename)) {
        cerr << "Cannot trigger importer start now because running importer is now being killed (wait a minute or two and try again)" << endl;
        return 1;
    }
    if (file_exists(kill_trigger_filename)) {
        cerr << "Cannot trigger importer start now because importer kill is currently triggered (wait a minute or two and try again)" << endl;
        return 1;
    }
    return create_trigger_file(start_trigger_filename);
}

int request_importer_start(string importer) {
    if (importer == "genie") {
        return request_importer_start(importer, START_GENIE_IMPORT_TRIGGER_FILENAME, KILL_GENIE_IMPORT_TRIGGER_FILENAME, GENIE_IMPORT_KILLING_FILENAME);
    }
    if (importer == "triage") {
        return request_importer_start(importer, START_TRIAGE_IMPORT_TRIGGER_FILENAME, KILL_TRIAGE_IMPORT_TRIGGER_FILENAME, TRIAGE_IMPORT_KILLING_FILENAME);
    }
    if (importer == "hgnc") {
        return request_importer_start(importer, START_HGNC_IMPORT_TRIGGER_FILENAME, KILL_HGNC_IMPORT_TRIGGER_FILENAME, HGNC_IMPORT_KILLING_FILENAME);
    }
    cerr << "Error : unrecognized importer " << importer << " during attempt to start import." << endl;
    return 1;
}

int request_importer_kill(string importer) {
    if (importer == "genie") {
        return create_trigger_file(KILL_GENIE_IMPORT_TRIGGER_FILENAME);
    }
    if (importer == "triage") {
        return create_trigger_file(KILL_TRIAGE_IMPORT_TRIGGER_FILENAME);
    }
    if (importer == "hgnc") {
        return create_trigger_file(KILL_HGNC_IMPORT_TRIGGER_FILENAME);
    }
    cerr << "Error : unrecognized importer " << importer << " during attempt to kill import." << endl;
    return 1;
}

// this function assumes importer arg is valid
int report_importer_status(string importer, string start_trigger_filename, string kill_trigger_filename, string in_progress_filename, string killing_filename, string log_filename) {
    bool import_start_triggered = file_exists(start_trigger_filename);
    bool import_kill_triggered = file_exists(kill_trigger_filename);
    bool import_in_progress = file_exists(in_progress_filename);
    bool import_killing = file_exists(killing_filename);
    string last_modification_of_log_file = get_file_modification_time(log_filename);
    string current_time = get_current_time();
    cout << "Current status for " << importer << " importer:" << endl;
    if (import_in_progress) {
        cout << "  importer is running (there is an import in progress)" << endl;
    } else {
        cout << "  importer is not running" << endl;
    }
    if (import_start_triggered) {
        cout << "  importer start has been triggered (this may wait until an import in progress completes)" << endl;
    } else {
        cout << "  importer start has not been triggered" << endl;
    }
    if (import_killing) {
        cout << "  importer is now being killed (triggers are being reset, and any running import is being halted)" << endl;
    } else {
    cout << "  importer is not now being killed" << endl;
    if (import_kill_triggered) {
        cout << "  importer kill has been triggered (this will soon cancel any importer start request, and will bring any import in progress to a halt)" << endl;
    } else {
        cout << "  importer kill has not been triggered" << endl;
        }
    }
    cout << "  importer log last modification time : " << last_modification_of_log_file << endl;
    cout << "  current time : " << current_time << endl;
    return 0;
}


int report_importer_status(string importer) {
    if (importer == "genie") {
        return report_importer_status(importer, START_GENIE_IMPORT_TRIGGER_FILENAME, KILL_GENIE_IMPORT_TRIGGER_FILENAME, GENIE_IMPORT_IN_PROGRESS_FILENAME, GENIE_IMPORT_KILLING_FILENAME, GENIE_IMPORT_LOG_FILENAME);
    }
    if (importer == "triage") {
        return report_importer_status(importer, START_TRIAGE_IMPORT_TRIGGER_FILENAME, KILL_TRIAGE_IMPORT_TRIGGER_FILENAME, TRIAGE_IMPORT_IN_PROGRESS_FILENAME, TRIAGE_IMPORT_KILLING_FILENAME, TRIAGE_IMPORT_LOG_FILENAME);
    }
    if (importer == "hgnc") {
        return report_importer_status(importer, START_HGNC_IMPORT_TRIGGER_FILENAME, KILL_HGNC_IMPORT_TRIGGER_FILENAME, HGNC_IMPORT_IN_PROGRESS_FILENAME, HGNC_IMPORT_KILLING_FILENAME, HGNC_IMPORT_LOG_FILENAME);
    }
    cerr << "Error : unrecognized importer " << importer << " during attempt to report status." << endl;
    return 1;
}

/* run_command3_and_wait()
 * Uses POSIX functions 'fork', 'execl', and 'waitpid' to run an executable program
 * in a subprocess and wait for its completion, returning its result. This is a
 * replacement of the 'system' function call, which was unsuccessfully employed
 * previously. The 'system' function call implementation now has reduced privileges,
 * and despite the Setuid file permission being present, it was unable to read files
 * owned by the effective user. The 'fork' / 'exec' combination was successful.
 *
 * This function takes exactly 3 command line arguments for the command to be
 * executed, as needed by a call such as 'tail -n 1000 log_filename.txt'.
 * Constructing a vector of arguments instead to be passed to execv was attempted
 * unsuccessfully.
 */
int run_command3_and_wait(string command, string arg1, string arg2, string arg3) {
    int fork_pid = fork();
    if (fork_pid == -1) {
        cerr << "Error : unable to fork in order to execute command " << command << " " << arg1 << " " << arg2 << " " << arg3 << endl << flush;
        exit(1);
    }
    if (fork_pid == 0) {
        // this is the child / forked process -- we start the exec immediately
        int return_code_of_cmd = execl(command.c_str(), command.c_str(), arg1.c_str(), arg2.c_str(), arg3.c_str(), NULL);
        exit(return_code_of_cmd);
    }
    // this is the parent / original process -- we wait for the child to finish and return the child's return code
    int child_return_code;
    int wait_return_code = waitpid(fork_pid, &child_return_code, 0);
    if (wait_return_code == -1) {
        cerr << "Error : wait failed on forked child process executing command unable to fork in order to execute command " << command << " " << arg1 << " " << arg2 << " " << arg3 << endl << flush;
        exit(1);
    }
    return child_return_code;
}

// this function assumes importer arg is valid
int report_importer_log(string importer, string log_filename, vector<string> & extra_args) {
    int linecount = 1000;
    if (extra_args.size() == 1) {
        stringstream iss(extra_args[0]);
        iss >> linecount;
    }
    if (file_exists(log_filename)) {
        cout << "Printing the last " << linecount << " lines from file " << log_filename << " :" << endl;
        stringstream oss;
        oss << linecount;
        run_command3_and_wait("/usr/bin/tail", "-n", oss.str(), log_filename);
    } else {
        cout << "File " << log_filename << " could not be found." << endl;
    }
    return 0;
}

int report_importer_log(string importer, vector<string> & extra_args) {
    if (importer == "genie") {
        return report_importer_log(importer, GENIE_IMPORT_LOG_FILENAME, extra_args);
    }
    if (importer == "triage") {
        return report_importer_log(importer, TRIAGE_IMPORT_LOG_FILENAME, extra_args);
    }
    if (importer == "hgnc") {
        return report_importer_log(importer, HGNC_IMPORT_LOG_FILENAME, extra_args);
    }
    cerr << "Error : unrecognized importer " << importer << " during attempt to report log." << endl;
    return 1;
}

int main(int argc, char **argv) {
    initialize_static_objects();
    if (! arguments_are_valid(argc, argv)) {
        print_usage(argv[0]);
        return 1;
    }
    string importer(argv[1]);
    string command(argv[2]);
    vector<string> extra_args;
    extract_extra_args(extra_args, argc, argv);
    if (command == "start") {
        return request_importer_start(importer);
    }
    if (command == "kill") {
        return request_importer_kill(importer);
    }
    if (command == "status") {
        return report_importer_status(importer);
    }
    if (command == "log") {
        return report_importer_log(importer, extra_args);
    }
    cerr << "Error : unrecognized command " << command << " encountered." << endl;
    return 1;
}
