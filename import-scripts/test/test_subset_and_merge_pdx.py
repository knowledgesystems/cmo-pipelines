# run all unit tests with:
#     import-scripts> python -m unittest discover
#
# TODO this only tests process_clinical_file, other methods should be tested too
# TODO fix the removing data bug
# TODO add some unit tests which explore the different modes related to --force option and overwriting
#
# Author: Avery Wang

import unittest
import tempfile
import os.path
import os
import io

from subset_and_merge_crdb_pdx_studies import *

class TestSubsetAndMergePDXStudies(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        resource_dir = "test/resources/subset_and_merge_pdx/"
        data_repos = "data_repos/"
        expected_files = "expected_output/"
        cls.root_directory = os.path.join(resource_dir, data_repos, "crdb_pdx_repos/")
        cls.dmp_directory = os.path.join(resource_dir, data_repos, "dmp_source_repo/")
        cls.cmo_directory = os.path.join(resource_dir, data_repos, "cmo_source_repo/")
        cls.datahub_directory = os.path.join(resource_dir, data_repos, "datahub_source_repo/")
        cls.crdb_fetch_directory = os.path.join(cls.root_directory, "crdb_pdx_raw_data/")
        cls.destination_to_source_mapping_file = os.path.join(cls.crdb_fetch_directory, "source_to_destination_mappings.txt")
        cls.clinical_annotations_mapping_file = os.path.join(cls.crdb_fetch_directory, "clinical_annotations_mappings.txt")

    def test_load_destination_source_patient_mappings(self):
        # parse the two mapping files provided (which patients and which clinical attributes)
        destination_source_patient_mapping_records = parse_file(self.destination_to_source_mapping_file)
        destination_source_clinical_annotation_mapping_records = parse_file(self.clinical_annotations_mapping_file)
        # create a dictionary mapping (destination - source) to a SourceMappings object
        destination_to_source_mapping = create_destination_to_source_mapping(destination_source_patient_mapping_records, destination_source_clinical_annotation_mapping_records, self.root_directory)
        self.check_destination_source_mappings(destination_to_source_mapping) 
        self.check_destination_source_clinical_annotations_mappings(destination_to_source_mapping)
        self.check_destination_source_patient_mappings(destination_to_source_mapping)

    def check_destination_source_mappings(self, destination_to_source_mapping):
        expected_destination_ids = ["test_destination_study_1", "test_destination_study_2"]
        expected_source_ids = ["test_source_study_1", "cmo_test_source_study_1", "test_msk_solid_heme", "crdb_pdx_raw_data"] 
        destination_ids = destination_to_source_mapping.keys()
        # test correct destination ids were loaded
        self.assertEquals(set(expected_destination_ids), set(destination_ids))
        # test correct source ids are linked to destination ids
        for destination_id in expected_destination_ids:
            self.assertEquals(set(expected_source_ids), set(destination_to_source_mapping[destination_id].keys()))

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

    # test that cmo studies will be found by substituting underscores
    # not found studies will return None
    def test_resolve_source_study_path(self):
        self.assertEquals(os.path.join(self.cmo_directory, "cmo/test/source/study_1"), resolve_source_study_path("cmo_test_source_study_1", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))
        self.assertEquals(os.path.join(self.dmp_directory, "test_msk_solid_heme"), resolve_source_study_path("test_msk_solid_heme", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))
        self.assertFalse(resolve_source_study_path("fake_study_path", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))
 
if __name__ == '__main__':
    unittest.main()
