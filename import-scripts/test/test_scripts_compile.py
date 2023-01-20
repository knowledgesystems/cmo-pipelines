# run all unit tests with:
#     import-scripts> python -m unittest discover test
#
# Author: Manda Wilson and Angelica Ochoa

import unittest
import subprocess
import glob
import os.path

class TestScriptsCompile(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.scripts_dir = "."

    # file_pattern should be a string like "*.sh" or "*.py"
    # compile_cmd should be a string like "bash -n %s" or "python -m py_compile %s"
    def check_for_compile_errors(self, file_pattern, compile_cmd, excluded_pattern = None):
        script_pattern = os.path.join(self.scripts_dir, file_pattern)
        matched_files = glob.glob(script_pattern)
        if (excluded_pattern):
            files = [file for file in matched_files if excluded_pattern not in file]
        else:
            files = matched_files
        # check there is at least one file
        self.assertTrue(files, "Expected at least one shell script, but '" + script_pattern + "' only found: '" + ",".join(files) + "'")
        compile_errors = {}
        for file in files:
            cmd_to_run = compile_cmd % (file)
            try:
                subprocess.check_output(cmd_to_run.split(), stderr=subprocess.STDOUT)
            except subprocess.CalledProcessError as cpe:
                compile_errors[file] = cpe.output

        if compile_errors:
            return "The following files failed to compile:\n\t" + "\n\t".join(["%s: %s" % (bash_file, output) for bash_file, output in compile_errors.iteritems()])
        return None

    def test_bash_scripts_compile(self):
        error_message = self.check_for_compile_errors("*.sh", "bash -n %s")
        if error_message:
            self.fail(error_message)

    #TODO: we can remove this hardcoded filename pattern once we fully migrate scripts over to python3
    def test_python2_scripts_compile(self):
        error_message = self.check_for_compile_errors("*.py", "python -m py_compile %s", "py3")
        if error_message:
            self.fail(error_message)

    def test_python3_scripts_compile(self):
        error_message = self.check_for_compile_errors("*py3*.py", "python3 -m py_compile %s")
        if error_message:
            self.fail(error_message)

if __name__ == '__main__':
    unittest.main()
