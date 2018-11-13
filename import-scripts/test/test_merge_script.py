# run all unit tests with:
#     import-scripts> python -m unittest discover
#
# Author: Angelica Ochoa

import unittest
import subprocess
import glob
import os.path
from clinicalfile_utils import *

NON_CASE_IDS = [
    'MIRNA',
    'LOCUS',
    'ID',
    'GENE SYMBOL',
    'ENTREZ_GENE_ID',
    'HUGO_SYMBOL',
    'LOCUS ID',
    'CYTOBAND',
    'COMPOSITE.ELEMENT.REF',
    'HYBRIDIZATION REF',
]

MOCK_STUDY_PATHS = ['./test/resources/merge_studies/mockstudy1',
                    './test/resources/merge_studies/mockstudy2']

PATIENT_SUBSET_FILENAME = 'patient_subset_list.txt'
PATIENT_EXCLUDE_FILENAME = 'patient_exclude_list.txt'
SAMPLE_SUBSET_FILENAME = 'sample_subset_list.txt'
SAMPLE_EXCLUDE_FILENAME = 'sample_exclude_list.txt'

STANDARD_MERGE_OUTPUT_DIR = 'temp_standard_merge'
PATIENT_SUBSET_OUTPUT_DIR = 'temp_patient_subset'
PATIENT_EXCLUDE_OUTPUT_DIR = 'temp_patient_exclude'
SAMPLE_SUBSET_OUTPUT_DIR = 'temp_sample_subset'
SAMPLE_EXCLUDE_OUTPUT_DIR = 'temp_sample_exclude'

EXPECTED_ROW_COUNT = {'./test/resources/merge_studies/temp_standard_merge/data_CNA.txt': 576,
                    './test/resources/merge_studies/temp_standard_merge/data_clinical_patient.txt': 10,
                    './test/resources/merge_studies/temp_standard_merge/data_clinical_sample.txt': 11,
                    './test/resources/merge_studies/temp_standard_merge/data_mutations_extended.txt': 70,
                    './test/resources/merge_studies/temp_patient_subset/data_CNA.txt':576,
                    './test/resources/merge_studies/temp_patient_subset/data_clinical_patient.txt':4,
                    './test/resources/merge_studies/temp_patient_subset/data_clinical_sample.txt':4,
                    './test/resources/merge_studies/temp_patient_subset/data_mutations_extended.txt':38,
                    './test/resources/merge_studies/temp_patient_exclude/data_CNA.txt':576,
                    './test/resources/merge_studies/temp_patient_exclude/data_clinical_patient.txt':7,
                    './test/resources/merge_studies/temp_patient_exclude/data_clinical_sample.txt':8,
                    './test/resources/merge_studies/temp_patient_exclude/data_mutations_extended.txt':54,
                    './test/resources/merge_studies/temp_sample_subset/data_CNA.txt':576,
                    './test/resources/merge_studies/temp_sample_subset/data_clinical_patient.txt':5,
                    './test/resources/merge_studies/temp_sample_subset/data_clinical_sample.txt':5,
                    './test/resources/merge_studies/temp_sample_subset/data_mutations_extended.txt':15,
                    './test/resources/merge_studies/temp_sample_exclude/data_CNA.txt':576,
                    './test/resources/merge_studies/temp_sample_exclude/data_clinical_patient.txt':6,
                    './test/resources/merge_studies/temp_sample_exclude/data_clinical_sample.txt':6,
                    './test/resources/merge_studies/temp_sample_exclude/data_mutations_extended.txt':48}

class TestMergeScript(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.scripts_dir = "."
        cls.mock_studies_dir = "./test/resources/merge_studies"

    def load_cases_from_file(self, filename):
        """
            Load cases from data file.
        """
        header = get_header(filename)
        if 'data_CNA' in filename:
            cases = set([col for col in header if not col.upper() in NON_CASE_IDS])
        else:
            # determine which case_id_col to use
            if 'data_clinical_patient' in filename:
                case_id_col = 'PATIENT_ID'
            elif 'data_clinical_sample' in filename:
                case_id_col = 'SAMPLE_ID'
            elif 'data_mutations_extended' in filename:
                case_id_col = 'Tumor_Sample_Barcode'
            else:
                self.fail("Unknown filename: " + filename)

            # assert that case_id_col in file header
            self.assertTrue((case_id_col in header), "Could not find '" + case_id_col + "' in file header: " + filename)

            cases = set()
            with open(filename, 'r') as data_file:
                data_reader = [line for line in data_file.readlines() if not line.startswith('#')][1:]
                for line in data_reader:
                    data = map(str.strip, line.split('\t'))
                    cases.add(data[header.index(case_id_col)])
        # assert that cases is not empty
        self.assertTrue(len(cases) > 0, "Error loading cases from file: " + filename)
        return cases

    def load_reference_set_from_file(self, filename):
        """
            Load reference set of samples (either subset or excluded samples).
        """
        reference_set = set()
        with open(filename, 'rU') as data_file:
            reference_set = set([sample_id.strip() for sample_id in data_file.readlines()])
        # assert that reference set is not empty
        self.assertTrue(len(reference_set) > 0, "Error loading reference set from: " + filename)
        return reference_set

    def validate_cases_in_file(self, filename, reference_set, keep_match):
        """
            Load cases from data file and compare to reference set
        """
        cases = self.load_cases_from_file(filename)
        if keep_match:
            # only cases in reference set should be found in the output data file
            # any not in the reference set should be reported
            cases_found = set([case_id for case_id in cases if not case_id in reference_set])
            error_message = "Found cases in file that are not in reference_set from: " + filename
            for case_id in cases_found:
                error_message = error_message + "\n\t" + case_id
            self.assertTrue(len(cases_found) == 0, error_message)
        else:
            # cases in reference set should not exist in cases from file
            # keep track of cases found for error message
            cases_found = set([case_id for case_id in reference_set if case_id in cases])
            error_message = "Found cases from reference set in data file: " + filename
            for case_id in cases_found:
                error_message = error_message + "\n\t" + case_id
            self.assertTrue(len(cases_found) == 0, error_message)

    def update_reference_set_with_matching_patient_samples(self, filename, reference_set):
        """
            Expands reference_set with matching samples if reference set is patient-based.
        """
        header = get_header(filename)
        with open(filename, 'rU') as data_file:
            data_reader = [line for line in data_file.readlines() if not line.startswith('#')][1:]
            for line in data_reader:
                data = map(str.strip, line.split('\t'))
                pid = data[header.index('PATIENT_ID')]
                sid = data[header.index('SAMPLE_ID')]
                if pid in reference_set:
                    reference_set.add(sid)
                elif sid in reference_set:
                    reference_set.add(pid)
        return reference_set

    def build_merge_command(self, input_study_paths, output_directory, reference_set_filename, keep_match):
        """
            Build merge script command to run.
        """
        merge_command = "python " + self.scripts_dir + "/merge.py --study-id mockmergeoutput --merge-clinical true --exclude-supplemental-data true "
        merge_command += "--output-directory " + output_directory + " "
        if reference_set_filename != None:
            self.assertFalse(keep_match == None, "Parameter 'keep_match' cannot be 'None' if reference set filename is not 'None'")

            if keep_match:
                merge_command += "--subset " + reference_set_filename + " "
            else:
                merge_command += "--excluded-samples " + reference_set_filename + " "
        merge_command += " ".join(input_study_paths)
        return merge_command

    def validate_expected_row_counts(self, output_directory):
        """
            Validate expected row counts for data files in directory.
        """
        data_files_found = 0
        for filename in os.listdir(output_directory):
            # only want to validate row counts for data files, no meta files.
            if not 'data' in filename:
                continue
            full_file_path = os.path.join(output_directory, filename)
            row_count = subprocess.check_output(["wc", "-l", full_file_path], stderr = subprocess.STDOUT)
            row_count = row_count.split()[0] # TO-DO: MAKE REGEX INSTEAD
            self.assertTrue(EXPECTED_ROW_COUNT[full_file_path] == int(row_count), "Output file row count does not match expected row count: " + row_count + " != " + str(EXPECTED_ROW_COUNT[full_file_path]) + ": " + full_file_path)
            data_files_found += 1
        self.assertTrue(data_files_found == 4, "Output directory should contain 4 data files - found " + str(data_files_found) + " instead: " + output_directory)


    def test_standard_merge(self):
        """
            Test a standard merge of 2 studies and validate against expected row counts.
        """
        try:
            # build merge command
            output_directory = os.path.join(self.mock_studies_dir, STANDARD_MERGE_OUTPUT_DIR)
            merge_command = self.build_merge_command(MOCK_STUDY_PATHS, output_directory, None, None)
            subprocess.check_output(merge_command.split(), stderr = subprocess.STDOUT)
            self.validate_expected_row_counts(output_directory)
        except subprocess.CalledProcessError as cpe:
            self.fail(cpe.output)

        # attempt to clean up temp output directory
        try:
            subprocess.check_output(["rm", "-rf", output_directory], stderr = subprocess.STDOUT)
        except:
            self.fail("Failed to clean up temp directory: " + output_directory)

    def test_subset_patient_merge(self):
        """
            Test a merge using a subset patient list. Validate against expected row counts.
        """
        try:
            patient_subset_filepath = os.path.join(self.mock_studies_dir, PATIENT_SUBSET_FILENAME)
            keep_match = True
            output_directory = os.path.join(self.mock_studies_dir, PATIENT_SUBSET_OUTPUT_DIR)
            merge_command = self.build_merge_command(MOCK_STUDY_PATHS, output_directory, patient_subset_filepath, keep_match)
            subprocess.check_output(merge_command.split(), stderr = subprocess.STDOUT)
            self.validate_expected_row_counts(output_directory)

            # expand reference set with patient-linked samples
            reference_set = self.load_reference_set_from_file(patient_subset_filepath)
            for study_path in MOCK_STUDY_PATHS:
                clinical_sample_file = os.path.join(study_path, 'data_clinical_sample.txt')
                reference_set = self.update_reference_set_with_matching_patient_samples(clinical_sample_file, reference_set)

            # verify that the cases in the output data files are expected case ids
            for filename in os.listdir(output_directory):
                if not 'data' in filename:
                    continue
                full_file_path = os.path.join(output_directory, filename)
                self.validate_cases_in_file(full_file_path, reference_set, keep_match)
        except subprocess.CalledProcessError as cpe:
            self.fail(cpe.output)

        # attempt to clean up temp output directory
        try:
            subprocess.check_output(["rm", "-rf", output_directory], stderr = subprocess.STDOUT)
        except:
            self.fail("Failed to clean up temp directory: " + output_directory)

    def test_subset_patient_exclude(self):
        """
            Test a merge using an excluded patient list. Validate against expected row counts.
        """
        try:
            patient_exclude_filepath = os.path.join(self.mock_studies_dir, PATIENT_EXCLUDE_FILENAME)
            keep_match = False
            output_directory = os.path.join(self.mock_studies_dir, PATIENT_EXCLUDE_OUTPUT_DIR)
            merge_command = self.build_merge_command(MOCK_STUDY_PATHS, output_directory, patient_exclude_filepath, keep_match)
            subprocess.check_output(merge_command.split(), stderr = subprocess.STDOUT)
            self.validate_expected_row_counts(output_directory)

            # expand reference set with patient-linked samples
            reference_set = self.load_reference_set_from_file(patient_exclude_filepath)
            for study_path in MOCK_STUDY_PATHS:
                clinical_sample_file = os.path.join(study_path, 'data_clinical_sample.txt')
                reference_set = self.update_reference_set_with_matching_patient_samples(clinical_sample_file, reference_set)

            # verify that the cases in the output data files are expected case ids
            for filename in os.listdir(output_directory):
                if not 'data' in filename:
                    continue
                full_file_path = os.path.join(output_directory, filename)
                self.validate_cases_in_file(full_file_path, reference_set, keep_match)
        except subprocess.CalledProcessError as cpe:
            self.fail(cpe.output)

        # attempt to clean up temp output directory
        try:
            subprocess.check_output(["rm", "-rf", output_directory], stderr = subprocess.STDOUT)
        except:
            self.fail("Failed to clean up temp directory: " + output_directory)

    def test_subset_sample_merge(self):
        """
            Test a merge using a subset sample list. Validate against expected row counts.
        """
        try:
            sample_subset_filepath = os.path.join(self.mock_studies_dir, SAMPLE_SUBSET_FILENAME)
            keep_match = True
            output_directory = os.path.join(self.mock_studies_dir, SAMPLE_SUBSET_OUTPUT_DIR)
            merge_command = self.build_merge_command(MOCK_STUDY_PATHS, output_directory, sample_subset_filepath, keep_match)
            subprocess.check_output(merge_command.split(), stderr = subprocess.STDOUT)
            self.validate_expected_row_counts(output_directory)

            # expand reference set with sample-linked patients
            reference_set = self.load_reference_set_from_file(sample_subset_filepath)
            for study_path in MOCK_STUDY_PATHS:
                clinical_sample_file = os.path.join(study_path, 'data_clinical_sample.txt')
                reference_set = self.update_reference_set_with_matching_patient_samples(clinical_sample_file, reference_set)

            # verify that the cases in the output data files are expected case ids
            for filename in os.listdir(output_directory):
                if not 'data' in filename:
                    continue
                full_file_path = os.path.join(output_directory, filename)
                self.validate_cases_in_file(full_file_path, reference_set, keep_match)
        except subprocess.CalledProcessError as cpe:
            self.fail(cpe.output)


        # attempt to clean up temp output directory
        try:
            subprocess.check_output(["rm", "-rf", output_directory], stderr = subprocess.STDOUT)
        except:
            self.fail("Failed to clean up temp directory: " + output_directory)

    def test_subset_sample_exclude(self):
        """
            Test a merge using an excluded sample list. Validate against expected row counts.
        """
        try:
            sample_exclude_filepath = os.path.join(self.mock_studies_dir, SAMPLE_EXCLUDE_FILENAME)
            keep_match = False
            output_directory = os.path.join(self.mock_studies_dir, SAMPLE_EXCLUDE_OUTPUT_DIR)
            merge_command = self.build_merge_command(MOCK_STUDY_PATHS, output_directory, sample_exclude_filepath, keep_match)
            subprocess.check_output(merge_command.split(), stderr = subprocess.STDOUT)
            self.validate_expected_row_counts(output_directory)

            # expand reference set with sample-linked patients
            reference_set = self.load_reference_set_from_file(sample_exclude_filepath)
            for study_path in MOCK_STUDY_PATHS:
                clinical_sample_file = os.path.join(study_path, 'data_clinical_sample.txt')
                reference_set = self.update_reference_set_with_matching_patient_samples(clinical_sample_file, reference_set)

            # verify that the cases in the output data files are expected case ids
            for filename in os.listdir(output_directory):
                if not 'data' in filename:
                    continue
                full_file_path = os.path.join(output_directory, filename)
                self.validate_cases_in_file(full_file_path, reference_set, keep_match)
        except subprocess.CalledProcessError as cpe:
            self.fail(cpe.output)

        # attempt to clean up temp output directory
        try:
            subprocess.check_output(["rm", "-rf", output_directory], stderr = subprocess.STDOUT)
        except:
            self.fail("Failed to clean up temp directory: " + output_directory)

if __name__ == '__main__':
    unittest.main()
