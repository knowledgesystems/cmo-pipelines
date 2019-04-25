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
import sys

from subset_and_merge_crdb_pdx_studies import *
from clinicalfile_utils import *

class TestSubsetAndMergePDXStudies(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.annotator_jar = os.getenv("ANNOTATOR_JAR")
        # needed because our bash scripts reference $PYTHON_BINARY
        os.environ["PYTHON_BINARY"] = sys.executable

        resource_dir = "test/resources/subset_and_merge_pdx/"
        cls.data_repos = os.path.join(resource_dir, "data_repos/")
        cls.crdb_fetch_directory_backup = os.path.join(cls.data_repos, "crdb_pdx_repos/crdb_pdx_raw_data/")
        cls.expected_files = os.path.join(resource_dir, "expected_outputs")

        # move all data into a temporary directory for manipulation
        cls.temp_dir = os.path.join(resource_dir, tempfile.mkdtemp())
        if os.path.isdir(cls.temp_dir):
            shutil.rmtree(cls.temp_dir)
        shutil.copytree(cls.data_repos, cls.temp_dir)

        cls.lib = "./"
        cls.root_directory = os.path.join(cls.temp_dir, "crdb_pdx_repos/")
        cls.dmp_directory = os.path.join(cls.temp_dir, "dmp_source_repo/")
        cls.cmo_directory = os.path.join(cls.temp_dir, "cmo_source_repo/")
        cls.datahub_directory = os.path.join(cls.temp_dir, "datahub_source_repo/")

        cls.crdb_fetch_directory = os.path.join(cls.root_directory, "crdb_pdx_raw_data/")
        cls.destination_to_source_mapping_file = os.path.join(cls.crdb_fetch_directory, "source_to_destination_mappings.txt")
        cls.clinical_annotations_mapping_file = os.path.join(cls.crdb_fetch_directory, "clinical_annotations_mappings.txt")

        # load source mapping one time - reuse same mapping for all tests
        cls.destination_source_patient_mapping_records = parse_file(cls.destination_to_source_mapping_file, False)
        cls.destination_source_clinical_annotation_mapping_records = parse_file(cls.clinical_annotations_mapping_file, False)
        cls.destination_to_source_mapping = create_destination_to_source_mapping(cls.destination_source_patient_mapping_records, cls.destination_source_clinical_annotation_mapping_records, cls.root_directory)
        cls.source_id_to_path_mapping = create_source_id_to_path_mapping(cls.destination_to_source_mapping, [cls.datahub_directory, cls.cmo_directory, cls.dmp_directory], cls.crdb_fetch_directory)

        cls.mock_destination_to_source_mapping = cls.mock_destination_to_source_mapping()

    @classmethod
    def tearDownClass(cls):
        # clean up copied tempdir/data
        shutil.rmtree(cls.temp_dir)

    @classmethod
    def mock_destination_to_source_mapping(cls):
        destination_to_source_mapping = {
            "test_destination_study_1" :
            {
                "test_source_study_1" : SourceMappings([Patient("P-MSK-0002","P-DMP-P002"),
                                        Patient("MSK_LX27","MSK_LX27"),
                                        Patient("MSK_LX6","MSK_LX6"),
                                        Patient("P-MSK-0001","P-MSK-0001")]),
                "test_msk_solid_heme" : SourceMappings([Patient("P-DMP-P001","P-DMP-P001"), Patient("P-DMP-P002","P-DMP-P002")], ["MSK_SLIDE_ID", "PED_IND"]),
                "cmo_test_source_study_1" : SourceMappings([Patient("p_C_001055","P-DMP-P001")]),
                "crdb_pdx_raw_data": SourceMappings([Patient("p_C_001055","P-DMP-P001"), Patient("P-DMP-P001","P-DMP-P001"),
                                     Patient("P-DMP-P002","P-DMP-P002"), Patient("P-MSK-0002","P-DMP-P002"),
                                     Patient("MSK_LX27","MSK_LX27"), Patient("MSK_LX6","MSK_LX6"),
                                     Patient("P-MSK-0001","P-MSK-0001")])
            },
            "test_destination_study_2" :
            {
                "test_source_study_1" : SourceMappings([Patient("P-MSK-0001","P-MSK-0001")], ["SMOKING_PACK_YEARS"]),
                "test_msk_solid_heme" : SourceMappings([Patient("P-DMP-P001","P-DMP-P001")], ["GRADE"]),
                "cmo_test_source_study_1" : SourceMappings([Patient("p_C_001055","P-DMP-P001")]),
                "crdb_pdx_raw_data" : SourceMappings([Patient("p_C_001055","P-DMP-P001"), Patient("P-DMP-P001","P-DMP-P001"), Patient("P-MSK-0001","P-MSK-0001")])
            }
        }
        return destination_to_source_mapping

    def test_resolve_source_study_path(self):
        """
            Test that CMO studies will be found by substituting underscores
            Not found studies will return None
        """
        self.assertEquals(os.path.join(self.cmo_directory, "cmo/test/source/study_1"), resolve_source_study_path("cmo_test_source_study_1", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))
        self.assertEquals(os.path.join(self.dmp_directory, "test_msk_solid_heme"), resolve_source_study_path("test_msk_solid_heme", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))
        self.assertFalse(resolve_source_study_path("fake_study_path", [self.datahub_directory, self.cmo_directory, self.dmp_directory]))

    def test_destination_source_mappings(self):
        """
            Test Step 1a: check source-destination mappings are loaded in correctly
        """
        expected_destination_ids = ["test_destination_study_1", "test_destination_study_2"]
        expected_source_ids = ["test_source_study_1", "cmo_test_source_study_1", "test_msk_solid_heme", "crdb_pdx_raw_data"]
        destination_ids = self.destination_to_source_mapping.keys()
        # test correct destination ids were loaded
        self.assertEquals(set(expected_destination_ids), set(destination_ids))
        # test correct source ids are linked to destination ids
        for destination_id in expected_destination_ids:
            self.assertEquals(set(expected_source_ids), set(self.destination_to_source_mapping[destination_id].keys()))

    def test_destination_source_patient_mappings(self):
        """
            Test Step 1b: check patient mappings per source-destination pair
        """
        expected_patients = {
            "test_destination_study_1test_source_study_1" : [("P-MSK-0002","P-DMP-P002"),("MSK_LX27","MSK_LX27"),("MSK_LX6","MSK_LX6"),("P-MSK-0001","P-MSK-0001")],
            "test_destination_study_2test_source_study_1" : [("P-MSK-0001","P-MSK-0001")],
            "test_destination_study_1test_msk_solid_heme" : [("P-DMP-P001","P-DMP-P001"),("P-DMP-P002","P-DMP-P002")],
            "test_destination_study_2test_msk_solid_heme" : [("P-DMP-P001","P-DMP-P001")],
            "test_destination_study_1cmo_test_source_study_1" : [("p_C_001055","P-DMP-P001")],
            "test_destination_study_2cmo_test_source_study_1" : [("p_C_001055","P-DMP-P001")],
            "test_destination_study_1crdb_pdx_raw_data" : [("p_C_001055","P-DMP-P001"),("P-DMP-P001","P-DMP-P001"),("P-DMP-P002","P-DMP-P002"),("P-MSK-0002","P-DMP-P002"),("MSK_LX27","MSK_LX27"),("MSK_LX6","MSK_LX6"),("P-MSK-0001","P-MSK-0001")],
            "test_destination_study_2crdb_pdx_raw_data" : [("p_C_001055","P-DMP-P001"),("P-DMP-P001","P-DMP-P001"),("P-MSK-0001","P-MSK-0001")]
        }
        for destination, source_to_sourcemapping in self.destination_to_source_mapping.items():
            for source, sourcemapping in source_to_sourcemapping.items():
                lookup_key = destination + source
                self.assertEquals(set(expected_patients[lookup_key]), set([(patient.source_pid, patient.destination_pid) for patient in sourcemapping.patients]))

    def test_destination_source_clinical_annotations_mappings(self):
        """
            Test Step 1c: check clinical annotation mapping per source-destination pair
        """
        expected_clinical_annotations =  {
            "test_destination_study_1test_msk_solid_heme" : ["MSK_SLIDE_ID", "PED_IND"],
            "test_destination_study_2test_msk_solid_heme" : ["GRADE"],
            "test_destination_study_2test_source_study_1" : ["SMOKING_PACK_YEARS"]
        }
        for destination, source_to_sourcemapping in self.destination_to_source_mapping.items():
            for source, sourcemapping in source_to_sourcemapping.items():
                lookup_key = destination + source
                if lookup_key in expected_clinical_annotations:
                    self.assertEquals(set(expected_clinical_annotations[lookup_key]), set(sourcemapping.clinical_annotations))
                else:
                    # clinical annotations list should be empty - evaluates to False
                    self.assertFalse(sourcemapping.clinical_annotations)

    def test_subset_source_step(self):
        """
            Test Step 2: check subset source studies per destination
        """
        # setup `temp directory` to model entire data repo - everything is needed for subsetting
        shutil.rmtree(self.temp_dir)
        shutil.copytree(self.data_repos, self.temp_dir)
        subset_source_directories_for_destination_studies(self.mock_destination_to_source_mapping, self.source_id_to_path_mapping, self.root_directory, self.lib)
        self.check_subset_source_step()

    def check_subset_source_step(self):
        for destination, source_to_sourcemapping in self.mock_destination_to_source_mapping.items():
            for source, sourcemapping in source_to_sourcemapping.items():
                expected_directory = os.path.join(self.expected_files, "subset_source_step", destination, source)
                actual_directory = os.path.join(self.root_directory, destination, source)
                for datafile in [filename for filename in os.listdir(expected_directory) if "temp" not in filename and (filename.endswith(".txt") or filename.endswith(".seg"))]:
                    self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, datafile), os.path.join(expected_directory, datafile)))

    def test_filter_clinical_annotations_step(self):
        """
            Test Step 3a: check clinical annotations are correctly filtered
        """
        # setup `crdb_pdx_repos` to expected files after intial subset
        # crdb-pdx-raw-data is not present (not needed for this test) - data changes only applied to subset outputs
        self.setup_root_directory_with_previous_test_output("subset_source_step")
        remove_unwanted_clinical_annotations_in_subsetted_source_studies(self.mock_destination_to_source_mapping, self.root_directory)
        self.compare_clinical_files_for_step("filter_clinical_annotations_step")

    def test_rename_patients_step(self):
        """
            Test Step 3b: check patient ids are correctly renamed
        """
        # setup `crdb_pdx_repos` to expected files after filtering clinical annotations
        # crdb-pdx-raw-data is not present (not needed for this test) - data changes only applied to subset outputs
        self.setup_root_directory_with_previous_test_output("filter_clinical_annotations_step")
        convert_source_to_destination_pids_in_subsetted_source_studies(self.mock_destination_to_source_mapping, self.root_directory)
        self.compare_clinical_files_for_step("rename_patients_step")

    def compare_clinical_files_for_step(self, step_name):
        for destination, source_to_sourcemapping in self.mock_destination_to_source_mapping.items():
            for source, sourcemapping in source_to_sourcemapping.items():
                expected_directory = os.path.join(self.expected_files, step_name, destination, source)
                actual_directory = os.path.join(self.root_directory, destination, source)
                for clinical_file in [filename for filename in os.listdir(expected_directory) if is_clinical_file(filename)]:
                    self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, clinical_file), os.path.join(expected_directory, clinical_file)))

    def test_merge_source_directories_step(self):
        """
            Test Step 4: check sources are correctly merged into destination studies
        """
        self.setup_root_directory_with_previous_test_output("rename_patients_step")
        merge_source_directories_for_destination_studies(self.mock_destination_to_source_mapping, self.root_directory, self.lib)
        remove_source_subdirectories(self.mock_destination_to_source_mapping, self.root_directory)
        self.check_merge_source_directories_step()

    def check_merge_source_directories_step(self):
        for destination in self.mock_destination_to_source_mapping:
            expected_directory = os.path.join(self.expected_files, "merge_source_directories_step", destination)
            actual_directory = os.path.join(self.root_directory, destination)
            for datafile in [filename for filename in os.listdir(expected_directory) if "temp" not in filename and (filename.endswith(".txt") or filename.endswith(".seg"))]:
                self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, datafile), os.path.join(expected_directory, datafile)))

    def test_merge_clinical_files_step(self):
        """
            Test Step 5: check legacy and current clinical files are merged together
        """
        self.setup_root_directory_with_previous_test_output("merge_source_directories_step")
        merge_clinical_files(self.mock_destination_to_source_mapping, self.root_directory, self.lib)
        self.check_merge_clinical_files_step()

    def check_merge_clinical_files_step(self):
        for destination in self.mock_destination_to_source_mapping:
            expected_directory = os.path.join(self.expected_files, "merge_clinical_files_step",  destination)
            actual_directory = os.path.join(self.root_directory, destination)
            self.assertTrue("data_clinical.txt" not in os.listdir(actual_directory))
            for clinical_file in [filename for filename in os.listdir(expected_directory) if is_clinical_file(filename)]:
                self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, clinical_file), os.path.join(expected_directory, clinical_file)))

    def test_subset_timeline_files_step(self):
        """
            Test Step 6: check timeline files are subsetted into destination study from crdb-fetched directory
        """
        self.setup_root_directory_with_previous_test_output("merge_clinical_files_step")
        shutil.copytree(self.crdb_fetch_directory_backup, self.crdb_fetch_directory)
        subset_timeline_files(self.mock_destination_to_source_mapping, self.source_id_to_path_mapping, self.root_directory, self.crdb_fetch_directory, self.lib)
        self.check_subset_timeline_files_step()

    def check_subset_timeline_files_step(self):
        for destination in self.mock_destination_to_source_mapping:
            expected_directory = os.path.join(self.expected_files, "subset_timeline_files_step", destination)
            actual_directory = os.path.join(self.root_directory, destination)
            for timeline_file in [filename for filename in os.listdir(expected_directory) if "data_timeline" in filename and (filename.endswith(".txt") or filename.endswith(".seg"))]:
                self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, timeline_file), os.path.join(expected_directory, timeline_file)))

    def test_insert_sequenced_sample_header_step(self):
        """
            Test Step 7(a): check that inserted sequenced samples header match between files.
        """
        self.setup_root_directory_with_previous_test_output("subset_timeline_files_step")
        insert_maf_sequenced_samples_header(self.mock_destination_to_source_mapping, self.root_directory)
        self.check_insert_maf_sequenced_samples_header_step()

    def check_insert_maf_sequenced_samples_header_step(self):
        for destination in self.mock_destination_to_source_mapping:
            actual_directory = os.path.join(self.root_directory, destination)
            expected_directory = os.path.join(self.expected_files, "post_process_seq_samples_header", destination)
            self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, MUTATION_FILE_PATTERN), os.path.join(expected_directory, MUTATION_FILE_PATTERN)))

    def test_add_display_sample_name_column_step(self):
        """
            Test Step 7(b): check that DISPLAY_SAMPLE_NAME column is added to clinical header
        """
        self.setup_root_directory_with_previous_test_output("subset_timeline_files_step")
        add_display_sample_name_column(self.mock_destination_to_source_mapping, self.root_directory)
        self.check_add_display_sample_name_column_step()

    def check_add_display_sample_name_column_step(self):
        for destination in self.mock_destination_to_source_mapping:
            actual_directory = os.path.join(self.root_directory, destination)
            expected_directory = os.path.join(self.expected_files, "add_display_sample_name_column_step", destination)
            self.assertTrue(self.sort_and_compare_files(os.path.join(actual_directory, "data_clinical_sample.txt"), os.path.join(expected_directory, "data_clinical_sample.txt")))

    def test_annotate_maf(self):
        """
            Test Step 7(c): check that MAF is annotated.
        """
        self.assertTrue(os.path.isfile(self.annotator_jar), "ANNOTATOR_JAR could not be resolved from sys environment! %s" % (self.annotator_jar))
        self.setup_root_directory_with_previous_test_output("subset_timeline_files_step")
        self.copy_maf_and_drop_hgvsp_short_column()
        annotate_maf(self.mock_destination_to_source_mapping, self.root_directory, self.annotator_jar)
        self.check_annotate_maf_step()

    def check_annotate_maf_step(self):
        hgvsp_column_values = []
        for destination in self.mock_destination_to_source_mapping:
            destination_directory = os.path.join(self.root_directory, destination)
            orig_maf_copy = os.path.join(destination_directory, "orig_data_mutations_extended.txt")
            annot_maf = os.path.join(destination_directory, MUTATION_FILE_PATTERN)
            self.assertFalse((HGVSP_SHORT_COLUMN in get_header(orig_maf_copy)))
            self.assertTrue((HGVSP_SHORT_COLUMN in get_header(annot_maf)))

            for record in parse_file(annot_maf, True):
                if record[HGVSP_SHORT_COLUMN] and not record[HGVSP_SHORT_COLUMN] in ["NA", "N/A"]:
                    hgvsp_column_values.append(record[HGVSP_SHORT_COLUMN])
            self.assertTrue(len(hgvsp_column_values) > 0)

    def copy_maf_and_drop_hgvsp_short_column(self):
        """
            Make a copy of the MAF and drop the HGVSp_Short column from the file.
            When the annotate_maf() step runs, the original MAF is replaced with the
            annotated MAF so this copy is necessary for testing.
        """
        for destination in self.mock_destination_to_source_mapping:
            destination_directory = os.path.join(self.root_directory, destination)
            maf = os.path.join(destination_directory, MUTATION_FILE_PATTERN)
            maf_copy = os.path.join(destination_directory, "orig_data_mutations_extended.txt")
            shutil.copy(maf, maf_copy)
            header = get_header(maf_copy)
            if HGVSP_SHORT_COLUMN in header:
                hgvsp_short_index = header.index(HGVSP_SHORT_COLUMN)
                maf_to_write = []
                with open(maf, "rU") as maf_file:
                    for line in maf_file.readlines():
                        if line.startswith("#"):
                            maf_to_write.append(line.rstrip("\n"))
                        else:
                            # remove data from record in HGVSp_Short column
                            record = line.rstrip("\n").split('\t')
                            del record[hgvsp_short_index]
                            maf_to_write.append('\t'.join(record))
                write_data_list_to_file(maf_copy, maf_to_write)

    def setup_root_directory_with_previous_test_output(self, previous_step):
        shutil.rmtree(self.root_directory)
        for destination in self.mock_destination_to_source_mapping:
            expected_directory = os.path.join(self.expected_files, previous_step, destination)
            actual_directory = os.path.join(self.root_directory, destination)
            shutil.copytree(expected_directory, actual_directory)

    def sort_and_compare_files(self, actual_file, expected_file):
        if not os.path.isfile(actual_file):
            return False
        actual_header = get_header(actual_file)
        expected_header = get_header(expected_file)
        if sorted(actual_header) != sorted(expected_header):
            return False
        # sort files and compare
        # NOTE: expected and actual file headers contain the same columns but may not necessarily be
        # in the same exact order so we use 'expected_header' when sorting the actual file generated
        # for comparison against the expected file output
        sorted_actual_file = self.sort_records_and_lines_in_file(expected_header, actual_file)
        sorted_expected_file = self.sort_records_and_lines_in_file(expected_header, expected_file)
        file_contents_are_equal = filecmp.cmp(sorted_expected_file, sorted_actual_file)
        os.remove(sorted_actual_file)
        os.remove(sorted_expected_file)
        return file_contents_are_equal

    def sort_records_and_lines_in_file(self, ordered_header, filename):
        to_write = []
        f = open(filename, "r")
        header = get_header(filename)
        for line in f.readlines():
            # sequenced sampels header - has to be sorted before being compared
            if line.startswith("#sequenced_samples:"):
                sequenced_samples = line.replace("#sequenced_samples:", "").strip().split(" ")
                to_write.append(' '.join(sorted(sequenced_samples)))
            # commented lines
            elif line.startswith("#") and len(line.split("\t")) != (len(ordered_header)):
                to_write.append(line.strip("\n"))
            # load row into a dictionary mapped to header
            # write out record to specific order
            else:
                # strip # sign at the beginning because header might start with different column
                data = dict(zip(header, map(str.strip, line.strip("#").split('\t'))))
                sorted_data = map(lambda x: data.get(x,''), ordered_header)
                to_write.append('\t'.join(sorted_data))
        f.close()
        sorted_to_write = sorted(to_write)
        sorted_filename = filename + "_sorted"
        f = open(sorted_filename, "w")
        f.write('\n'.join(sorted_to_write))
        f.close()
        return sorted_filename

if __name__ == '__main__':
    unittest.main()
