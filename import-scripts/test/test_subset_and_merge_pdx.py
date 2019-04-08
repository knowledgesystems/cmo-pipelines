# run all unit tests with:
#     import-scripts> python -m unittest discover
#
# Author: Avery Wang

import unittest
import filecmp
import tempfile
import os.path
import os
import io

from subset_and_merge_crdb_pdx_studies import *

class TestSubsetAndMergePDXStudies(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        resource_dir = "test/resources/subset_and_merge_pdx/"
        data_repos = os.path.join(resource_dir, "data_repos/")
        cls.expected_files = os.path.join(resource_dir, "expected_outputs")
        
        # move all data into a temporary directory for manipulation
        cls.temp_dir = os.path.join(resource_dir, "tmp")
        if os.path.isdir(cls.temp_dir):
            shutil.rmtree(cls.temp_dir)
        shutil.copytree(data_repos, cls.temp_dir)
        
        cls.lib = "./"
        cls.root_directory = os.path.join(cls.temp_dir, "crdb_pdx_repos/")
        cls.dmp_directory = os.path.join(cls.temp_dir, "dmp_source_repo/")
        cls.cmo_directory = os.path.join(cls.temp_dir, "cmo_source_repo/")
        cls.datahub_directory = os.path.join(cls.temp_dir, "datahub_source_repo/")
        
        cls.crdb_fetch_directory = os.path.join(cls.root_directory, "crdb_pdx_raw_data/")
        cls.destination_to_source_mapping_file = os.path.join(cls.crdb_fetch_directory, "source_to_destination_mappings.txt")
        cls.clinical_annotations_mapping_file = os.path.join(cls.crdb_fetch_directory, "clinical_annotations_mappings.txt")
    
    @classmethod
    def tearDownClass(cls):
        # clean up copied tempdir/data
        shutil.rmtree(cls.temp_dir)
    
    # test that cmo studies will be found by substituting underscores
    # not found studies will return None
    def test_resolve_source_study_path(self):
        self.assertEquals(os.path.join(self.cmo_directory, "cmo/test/source/study_1"), resolve_source_study_path("cmo_test_source_study_1", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))
        self.assertEquals(os.path.join(self.dmp_directory, "test_msk_solid_heme"), resolve_source_study_path("test_msk_solid_heme", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))
        self.assertFalse(resolve_source_study_path("fake_study_path", [self.datahub_directory, self.cmo_directory, self.dmp_directory])) 
    
    def test_load_destination_source_patient_mappings(self):
        destination_source_patient_mapping_records = parse_file(self.destination_to_source_mapping_file)
        destination_source_clinical_annotation_mapping_records = parse_file(self.clinical_annotations_mapping_file)
        destination_to_source_mapping = create_destination_to_source_mapping(destination_source_patient_mapping_records, destination_source_clinical_annotation_mapping_records, self.root_directory)
        
        self.check_destination_source_mappings(destination_to_source_mapping) 
        self.check_destination_source_clinical_annotations_mappings(destination_to_source_mapping)
        self.check_destination_source_patient_mappings(destination_to_source_mapping)
        
        source_id_to_path_mapping = create_source_id_to_path_mapping(destination_to_source_mapping, [self.datahub_directory, self.cmo_directory, self.dmp_directory], self.crdb_fetch_directory)
        subset_source_directories(destination_to_source_mapping, source_id_to_path_mapping, self.root_directory, self.lib)
        self.check_subset_source_step(destination_to_source_mapping)

        remove_unwanted_clinical_annotations_in_subsetted_source_studies(destination_to_source_mapping, self.root_directory)
        self.compare_clinical_files_for_step(destination_to_source_mapping, "filter_clinical_annotations_step")

        convert_cmo_to_dmp_pids_in_subsetted_source_studies(destination_to_source_mapping, self.root_directory)
        self.compare_clinical_files_for_step(destination_to_source_mapping, "rename_patients_step")

        #merge_source_directories(destination_to_source_mapping, self.root_directory, self.lib)
        #remove_source_subdirectories(destination_to_source_mapping, self.root_directory)
        #self.check_merge_source_directories_step(destination_to_source_mapping, self.root_directory)
   
    # Step 1a: check source-destination mappings
    def check_destination_source_mappings(self, destination_to_source_mapping):
        expected_destination_ids = ["test_destination_study_1", "test_destination_study_2"]
        expected_source_ids = ["test_source_study_1", "cmo_test_source_study_1", "test_msk_solid_heme", "crdb_pdx_raw_data"] 
        destination_ids = destination_to_source_mapping.keys()
        # test correct destination ids were loaded
        self.assertEquals(set(expected_destination_ids), set(destination_ids))
        # test correct source ids are linked to destination ids
        for destination_id in expected_destination_ids:
            self.assertEquals(set(expected_source_ids), set(destination_to_source_mapping[destination_id].keys()))
   
    # Step 1b: check patient mappings per source-destination pair 
    def check_destination_source_patient_mappings(self, destination_to_source_mapping):
        expected_patients = {
            "test_destination_study_1test_source_study_1" : [("MSK_LX229","P-0005562"),("MSK_LX27","MSK_LX27"),("MSK_LX6","MSK_LX6"),("MSK_LX96","MSK_LX96")],
            "test_destination_study_2test_source_study_1" : [("MSK_LX96","MSK_LX96")],
            "test_destination_study_1test_msk_solid_heme" : [("P-0003329","P-0003329"),("P-0005562","P-0005562")],
            "test_destination_study_2test_msk_solid_heme" : [("P-0003329","P-0003329")],
            "test_destination_study_1cmo_test_source_study_1" : [("p_C_001055","P-0003329")],       
            "test_destination_study_2cmo_test_source_study_1" : [("p_C_001055","P-0003329")],
            "test_destination_study_1crdb_pdx_raw_data" : [("p_C_001055","P-0003329"),("P-0003329","P-0003329"),("P-0005562","P-0005562"),("MSK_LX229","P-0005562"),("MSK_LX27","MSK_LX27"),("MSK_LX6","MSK_LX6"),("MSK_LX96","MSK_LX96")],
            "test_destination_study_2crdb_pdx_raw_data" : [("p_C_001055","P-0003329"),("P-0003329","P-0003329"),("MSK_LX96","MSK_LX96")]
        }
        for destination, source_to_source_mapping in destination_to_source_mapping.items():
            for source, source_mapping in source_to_source_mapping.items():
                lookup_key = destination + source
                self.assertEquals(set(expected_patients[lookup_key]), set([(patient.cmo_pid, patient.dmp_pid) for patient in source_mapping.patients]))
    
    # Step 1c: check clinical annotation mapping per source-destination pair
    def check_destination_source_clinical_annotations_mappings(self, destination_to_source_mapping):
        expected_clinical_annotations =  {
            "test_destination_study_1test_msk_solid_heme" : ["MSK_SLIDE_ID", "PED_IND"],
            "test_destination_study_2test_msk_solid_heme" : ["GRADE"],
            "test_destination_study_2test_source_study_1" : ["SMOKING_PACK_YEARS"]
        }
        for destination, source_to_source_mapping in destination_to_source_mapping.items():
            for source, source_mapping in source_to_source_mapping.items():
                lookup_key = destination + source
                if lookup_key in expected_clinical_annotations:
                    self.assertEquals(set(expected_clinical_annotations[lookup_key]), set(source_mapping.clinical_annotations))
                else:
                    # clinical annotations list should be empty - evaluates to False
                    self.assertFalse(source_mapping.clinical_annotations)

    # test that cmo studies will be found by substituting underscores
    def check_subset_source_step(self, destination_to_source_mapping):
        for destination, source_to_source_mapping in destination_to_source_mapping.items():
            for source, source_mapping in source_to_source_mapping.items():
                expected_directory = os.path.join(self.expected_files, "subset_source_step", destination, source)
                actual_directory = os.path.join(self.root_directory, destination, source)
                if not "test_destination_study_2" in actual_directory:
                    continue
                for datafile in os.listdir(expected_directory):
                    self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, datafile), os.path.join(expected_directory, datafile)))
    
    def compare_clinical_files_for_step(self, destination_to_source_mapping, step_name):
        for destination, source_to_source_mapping in destination_to_source_mapping.items():
            for source, source_mapping in source_to_source_mapping.items():
                expected_directory = os.path.join(self.expected_files, step_name, destination, source)
                actual_directory = os.path.join(self.root_directory, destination, source)
                if not "test_destination_study_2" in actual_directory:
                    continue
                for clinical_file in [filename for filename in os.listdir(expected_directory) if "data_clinical" in filename]:
                    self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, clinical_file), os.path.join(expected_directory, clinical_file)))

    def check_merge_source_directories_step(self, destination_to_source_mapping):
        for destination in destination_to_source_mapping:
            expected_directory = os.path.join(self.expected_files, "merge_source_directories_step", destination)
            actual_directory = os.path.join(self.root_directory, destination)
            if not "test_destination_study_2" in actual_directory:
                 continue
            for clinical_file in [filename for filename in os.listdir(expected_directory) if "meta" not in filename]:
                self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, clinical_file), os.path.join(expected_directory, clinical_file)))

    def sort_and_compare_files(self, actual_file, expected_file):
        sorted_actual_file = self.sort_lines_in_file(actual_file)
        sorted_expected_file = self.sort_lines_in_file(expected_file)
        files_are_equal = filecmp.cmp(sorted_expected_file, sorted_actual_file)
        os.remove(sorted_actual_file)
        os.remove(sorted_expected_file)
        return files_are_equal
        
    def sort_lines_in_file(self, filename):
        f = open(filename, "r")
        to_write = sorted(f.readlines())
        f.close()
        sorted_filename = filename + "_sorted"
        f = open(sorted_filename, "w")
        f.write(''.join(to_write))
        f.close()
        return sorted_filename

if __name__ == '__main__':
    unittest.main()
