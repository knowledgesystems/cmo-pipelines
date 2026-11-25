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

    # a script is treated as python3 if "py3" appears in its filename or if its
    # shebang names python3 - scripts migrated to python3 do not always get renamed
    def is_python3_script(self, file):
        if "py3" in os.path.basename(file):
            return True
        with open(file) as script:
            return "python3" in script.readline()

    # file_pattern should be a string like "*.sh" or "*.py"
    # compile_cmd should be a string like "bash -n %s" or "python -m py_compile %s"
    # file_filter should be a function taking a filename and returning whether to compile it
    def check_for_compile_errors(self, file_pattern, compile_cmd, file_filter=None):
        script_pattern = os.path.join(self.scripts_dir, file_pattern)
        matched_files = glob.glob(script_pattern)
        if file_filter:
            files = [file for file in matched_files if file_filter(file)]
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

    def test_python2_scripts_compile(self):
        error_message = self.check_for_compile_errors("*.py", "python -m py_compile %s", lambda file: not self.is_python3_script(file))
        if error_message:
            self.fail(error_message)

    def test_python3_scripts_compile(self):
        error_message = self.check_for_compile_errors("*.py", "python3 -m py_compile %s", self.is_python3_script)
        if error_message:
            self.fail(error_message)

if __name__ == '__main__':
    unittest.main()
