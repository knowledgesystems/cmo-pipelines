#!/usr/bin/env bash
function extractPropertiesFromFile() {
    # Populate extracted_properties with dummy values for test
    extracted_properties[db.user]="testuser"
    extracted_properties[db.password]="testpass"
    extracted_properties[db.host]="localhost"
    extracted_properties[db.portal_db_name]="testdb"
    return 0
}
