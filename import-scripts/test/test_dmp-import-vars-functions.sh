#!/usr/bin/env bash

test_source_file="./import-scripts/date-and-time-handling-functions.sh"
echo "unit tests of '$test_source_file' beginning"
if ! source "$test_source_file" ; then
    echo "could not source file '$test_source_file'"
    exit 1 
fi

failure_count=0

#--------------------------------------------------------------------------------
echo
echo "testing function stringIs1to4Digits()"

declare -a test_string_is_1_to_4_digits_success_expected_cases
test_string_is_1_to_4_digits_success_expected_cases+=("0")
test_string_is_1_to_4_digits_success_expected_cases+=("9")
test_string_is_1_to_4_digits_success_expected_cases+=("00")
test_string_is_1_to_4_digits_success_expected_cases+=("99")
test_string_is_1_to_4_digits_success_expected_cases+=("000")
test_string_is_1_to_4_digits_success_expected_cases+=("999")
test_string_is_1_to_4_digits_success_expected_cases+=("0000")
test_string_is_1_to_4_digits_success_expected_cases+=("9999")
declare -a test_string_is_1_to_4_digits_failure_expected_cases
test_string_is_1_to_4_digits_failure_expected_cases+=("")
test_string_is_1_to_4_digits_failure_expected_cases+=(" ")
test_string_is_1_to_4_digits_failure_expected_cases+=("a")
test_string_is_1_to_4_digits_failure_expected_cases+=("z")
test_string_is_1_to_4_digits_failure_expected_cases+=("!")
test_string_is_1_to_4_digits_failure_expected_cases+=("0a0")
test_string_is_1_to_4_digits_failure_expected_cases+=("0.0")
test_string_is_1_to_4_digits_failure_expected_cases+=("0 0")
test_string_is_1_to_4_digits_failure_expected_cases+=(" 0000")
test_string_is_1_to_4_digits_failure_expected_cases+=("0000a")
test_string_is_1_to_4_digits_failure_expected_cases+=("a0000")
test_string_is_1_to_4_digits_failure_expected_cases+=("0000 ")
test_string_is_1_to_4_digits_failure_expected_cases+=("00001")
test_string_is_1_to_4_digits_failure_expected_cases+=("10000")
test_string_is_1_to_4_digits_failure_expected_cases+=("-1")
test_string_is_1_to_4_digits_failure_expected_cases+=("-1000")

index=0
while [ $index -lt ${#test_string_is_1_to_4_digits_success_expected_cases[@]} ] ; do
    arg="${test_string_is_1_to_4_digits_success_expected_cases[$index]}"
    if stringIs1to4Digits "$arg" ; then
        echo stringIs1to4Digits \"$arg\" ... pass
    else
        echo stringIs1to4Digits \"$arg\" expected zero status, but received non-zero ... fail
        failure_count=$(( $failure_count + 1 ))
    fi
    index=$(( $index + 1 ))
done
index=0
while [ $index -lt ${#test_string_is_1_to_4_digits_failure_expected_cases[@]} ] ; do
    arg="${test_string_is_1_to_4_digits_failure_expected_cases[$index]}"
    if stringIs1to4Digits "$arg" ; then
        echo stringIs1to4Digits \"$arg\" expected non-zero status, but received zero ... fail
        failure_count=$(( $failure_count + 1 ))
    else
        echo stringIs1to4Digits \"$arg\" ... pass
    fi
    index=$(( $index + 1 ))
done

#--------------------------------------------------------------------------------
echo
echo "testing function stripZeroPrefixFromDigitString()"

declare -a strip_zero_prefix_from_digit_string_case_to_expected
strip_zero_prefix_from_digit_string_case_to_expected+=("" "fail")
strip_zero_prefix_from_digit_string_case_to_expected+=("a" "fail")
strip_zero_prefix_from_digit_string_case_to_expected+=(" " "fail")
strip_zero_prefix_from_digit_string_case_to_expected+=("0" "0")
strip_zero_prefix_from_digit_string_case_to_expected+=("00" "0")
strip_zero_prefix_from_digit_string_case_to_expected+=("000" "0")
strip_zero_prefix_from_digit_string_case_to_expected+=("0000" "0")
strip_zero_prefix_from_digit_string_case_to_expected+=("00000" "0")
strip_zero_prefix_from_digit_string_case_to_expected+=("9" "9")
strip_zero_prefix_from_digit_string_case_to_expected+=("99" "99")
strip_zero_prefix_from_digit_string_case_to_expected+=("999" "999")
strip_zero_prefix_from_digit_string_case_to_expected+=("9999" "9999")
strip_zero_prefix_from_digit_string_case_to_expected+=("99999" "99999")
strip_zero_prefix_from_digit_string_case_to_expected+=("09" "9")
strip_zero_prefix_from_digit_string_case_to_expected+=("099" "99")
strip_zero_prefix_from_digit_string_case_to_expected+=("0999" "999")
strip_zero_prefix_from_digit_string_case_to_expected+=("09999" "9999")
strip_zero_prefix_from_digit_string_case_to_expected+=("099999" "99999")
strip_zero_prefix_from_digit_string_case_to_expected+=("009" "9")
strip_zero_prefix_from_digit_string_case_to_expected+=("0099" "99")
strip_zero_prefix_from_digit_string_case_to_expected+=("00999" "999")
strip_zero_prefix_from_digit_string_case_to_expected+=("009999" "9999")
strip_zero_prefix_from_digit_string_case_to_expected+=("0099999" "99999")
strip_zero_prefix_from_digit_string_case_to_expected+=("0090" "90")
strip_zero_prefix_from_digit_string_case_to_expected+=("00990" "990")
strip_zero_prefix_from_digit_string_case_to_expected+=("009990" "9990")
strip_zero_prefix_from_digit_string_case_to_expected+=("0099990" "99990")
strip_zero_prefix_from_digit_string_case_to_expected+=("00999990" "999990")

index=0
while [ $index -lt ${#strip_zero_prefix_from_digit_string_case_to_expected[@]} ] ; do
    expected_index=$(( $index + 1 ))
    arg="${strip_zero_prefix_from_digit_string_case_to_expected[$index]}"
    expected=${strip_zero_prefix_from_digit_string_case_to_expected[$expected_index]}
    if stripZeroPrefixFromDigitString "$arg" ; then
        if [ "$expected" == "fail" ] ; then
            echo stripZeroPrefixFromDigitString \"$arg\" expected non-zero status, but received zero ... fail
            failure_count=$(( $failure_count + 1 ))
        else
            if [ "$expected" == "$digit_string_with_zero_prefix_stripped" ] ; then
                echo stripZeroPrefixFromDigitString \"$arg\" ... pass
            else
                echo stripZeroPrefixFromDigitString \"$arg\" expected result \"$expected\" but received \"$digit_string_with_zero_prefix_stripped\" ... fail
                failure_count=$(( $failure_count + 1 ))
            fi
        fi
    else
        if [ "$expected" == "fail" ] ; then
            echo stripZeroPrefixFromDigitString \"$arg\" ... pass
        else
            echo stripZeroPrefixFromDigitString \"$arg\" expected result \"$expected\" but received non-zero status ... fail
            failure_count=$(( $failure_count + 1 ))
        fi
    fi
    index=$(( $index + 2 ))
done

#--------------------------------------------------------------------------------
echo
echo "testing function computeMinutesSinceMidnightFromHourMinute()"

declare -a compute_minutes_since_midnight_from_hour_minute_case_to_expected
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("a" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=(" " "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0" "0")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("00" "0")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("000" "0")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0000" "0")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("00000" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("9999" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("9999" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("2360" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("2399" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("2400" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0060" "fail")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0059" "59")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0100" "60")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0200" "120")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0210" "130")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("0201" "121")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("2259" "1379")
compute_minutes_since_midnight_from_hour_minute_case_to_expected+=("2359" "1439")

index=0
while [ $index -lt ${#compute_minutes_since_midnight_from_hour_minute_case_to_expected[@]} ] ; do
    expected_index=$(( $index + 1 ))
    arg="${compute_minutes_since_midnight_from_hour_minute_case_to_expected[$index]}"
    expected=${compute_minutes_since_midnight_from_hour_minute_case_to_expected[$expected_index]}
    if computeMinutesSinceMidnightFromHourMinute "$arg" ; then
        if [ "$expected" == "fail" ] ; then
            echo computeMinutesSinceMidnightFromHourMinute \"$arg\" expected non-zero status, but received zero ... fail
            failure_count=$(( $failure_count + 1 ))
        else
            if [ "$expected" == "$computed_minutes_since_midnight" ] ; then
                echo computeMinutesSinceMidnightFromHourMinute \"$arg\" ... pass
            else
                echo computeMinutesSinceMidnightFromHourMinute \"$arg\" expected result \"$expected\" but received \"$computed_minutes_since_midnight\" ... fail
                failure_count=$(( $failure_count + 1 ))
            fi
        fi
    else
        if [ "$expected" == "fail" ] ; then
            echo computeMinutesSinceMidnightFromHourMinute \"$arg\" ... pass
        else
            echo computeMinutesSinceMidnightFromHourMinute \"$arg\" expected result \"$expected\" but received non-zero status ... fail
            failure_count=$(( $failure_count + 1 ))
        fi
    fi
    index=$(( $index + 2 ))
done

#--------------------------------------------------------------------------------
echo
echo "testing function compareHourMinuteStrings()"

declare -a compare_hour_minute_strings_case_to_expected
compare_hour_minute_strings_case_to_expected+=("" "1000" "$TIME_COMPARISON_ERROR_CODE")
compare_hour_minute_strings_case_to_expected+=("a" "1000" "$TIME_COMPARISON_ERROR_CODE")
compare_hour_minute_strings_case_to_expected+=(" " "1000" "$TIME_COMPARISON_ERROR_CODE")
compare_hour_minute_strings_case_to_expected+=("1000" "" "$TIME_COMPARISON_ERROR_CODE")
compare_hour_minute_strings_case_to_expected+=("1000" "a" "$TIME_COMPARISON_ERROR_CODE")
compare_hour_minute_strings_case_to_expected+=("1000" " " "$TIME_COMPARISON_ERROR_CODE")
compare_hour_minute_strings_case_to_expected+=("0" "0" "$TIME_COMPARISON_1_EQ_2_CODE")
compare_hour_minute_strings_case_to_expected+=("00" "0" "$TIME_COMPARISON_1_EQ_2_CODE")
compare_hour_minute_strings_case_to_expected+=("000" "0" "$TIME_COMPARISON_1_EQ_2_CODE")
compare_hour_minute_strings_case_to_expected+=("0000" "0" "$TIME_COMPARISON_1_EQ_2_CODE")
compare_hour_minute_strings_case_to_expected+=("0000" "0000" "$TIME_COMPARISON_1_EQ_2_CODE")
compare_hour_minute_strings_case_to_expected+=("0001" "0001" "$TIME_COMPARISON_1_EQ_2_CODE")
compare_hour_minute_strings_case_to_expected+=("1009" "1009" "$TIME_COMPARISON_1_EQ_2_CODE")
compare_hour_minute_strings_case_to_expected+=("0000" "0001" "$TIME_COMPARISON_1_LT_2_CODE")
compare_hour_minute_strings_case_to_expected+=("0000" "1009" "$TIME_COMPARISON_1_LT_2_CODE")
compare_hour_minute_strings_case_to_expected+=("0000" "2359" "$TIME_COMPARISON_1_LT_2_CODE")
compare_hour_minute_strings_case_to_expected+=("2358" "2359" "$TIME_COMPARISON_1_LT_2_CODE")
compare_hour_minute_strings_case_to_expected+=("0001" "0000" "$TIME_COMPARISON_2_LT_1_CODE")
compare_hour_minute_strings_case_to_expected+=("1009" "0000" "$TIME_COMPARISON_2_LT_1_CODE")
compare_hour_minute_strings_case_to_expected+=("2359" "0000" "$TIME_COMPARISON_2_LT_1_CODE")
compare_hour_minute_strings_case_to_expected+=("2359" "2358" "$TIME_COMPARISON_2_LT_1_CODE")

index=0
while [ $index -lt ${#compare_hour_minute_strings_case_to_expected[@]} ] ; do
    arg2_index=$(( $index + 1 ))
    expected_index=$(( $index + 2 ))
    arg1="${compare_hour_minute_strings_case_to_expected[$index]}"
    arg2="${compare_hour_minute_strings_case_to_expected[$arg2_index]}"
    expected=${compare_hour_minute_strings_case_to_expected[$expected_index]}
    compareHourMinuteStrings "$arg1" "$arg2"
    actual=$?
    if [ "$actual" == "$expected" ] ; then
        echo compareHourMinuteStrings \"$arg1\" \"$arg2\" ... pass
    else
        echo compareHourMinuteStrings \"$arg1\" \"$arg2\" expected \"$expected\" status, but received \"$actual\" ... fail
        failure_count=$(( $failure_count + 1 ))
    fi
    index=$(( $index + 3 ))
done

#--------------------------------------------------------------------------------
echo
echo "testing function computeStartHourMinuteFromMinutesSinceMidnight()"

declare -a compute_start_hour_minute_from_minutes_since_midnight_case_to_expected
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("" "fail")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("a" "fail")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=(" " "fail")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("0" "0000")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("00" "0000")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("000" "0000")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("0000" "0000")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("59" "0059")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("059" "0059")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("0059" "0059")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("60" "0100")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("120" "0200")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("130" "0210")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("121" "0201")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("1379" "2259")
compute_start_hour_minute_from_minutes_since_midnight_case_to_expected+=("1439" "2359")

index=0
while [ $index -lt ${#compute_start_hour_minute_from_minutes_since_midnight_case_to_expected[@]} ] ; do
    expected_index=$(( $index + 1 ))
    arg="${compute_start_hour_minute_from_minutes_since_midnight_case_to_expected[$index]}"
    expected=${compute_start_hour_minute_from_minutes_since_midnight_case_to_expected[$expected_index]}
    computeStartHourMinuteFromMinutesSinceMidnight "$arg"
    exit_status=$?
    if [ "$exit_status" -ne 0 ] ; then
        if [ "$expected" == "fail" ] ; then
            echo computeStartHourMinuteFromMinutesSinceMidnight \"$arg\" ... pass
        else
            echo computeStartHourMinuteFromMinutesSinceMidnight \"$arg\" expected zero exit status, but received non-zero ... fail
            failure_count=$(( $failure_count + 1 ))
        fi
    else
        actual=$computed_start_hour_minute_from_minutes_since_midnight
        if [ "$actual" == "$expected" ] ; then
            echo computeStartHourMinuteFromMinutesSinceMidnight \"$arg\" ... pass
        else
            echo computeStartHourMinuteFromMinutesSinceMidnight \"$arg\" expected \"$expected\" status, but received \"$actual\" ... fail
            failure_count=$(( $failure_count + 1 ))
        fi
    fi
    index=$(( $index + 2 ))
done

#--------------------------------------------------------------------------------
echo
echo "testing function computeStartHourMinutePlusMinutes()"

declare -a compute_start_hour_minute_plus_minutes_case_to_expected
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "" "fail" "fail")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "a" "fail" "fail")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" " " "fail" "fail")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "0" "0000" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "00" "0000" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "000" "0000" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "0000" "0000" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "59" "0059" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "059" "0059" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "0059" "0059" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "60" "0100" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "120" "0200" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "130" "0210" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "121" "0201" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "1379" "2259" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("0000" "1439" "2359" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "0000" "1900" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "59" "1959" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "059" "1959" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "0059" "1959" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "60" "2000" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "120" "2100" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "130" "2110" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "121" "2101" "0")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "1379" "1759" "1")
compute_start_hour_minute_plus_minutes_case_to_expected+=("1900" "1439" "1859" "1")

index=0
while [ $index -lt ${#compute_start_hour_minute_plus_minutes_case_to_expected[@]} ] ; do
    arg2_index=$(( $index + 1 ))
    expected_index=$(( $index + 2 ))
    next_day_flag_index=$(( $index + 3 ))
    arg1="${compute_start_hour_minute_plus_minutes_case_to_expected[$index]}"
    arg2="${compute_start_hour_minute_plus_minutes_case_to_expected[$arg2_index]}"
    expected=${compute_start_hour_minute_plus_minutes_case_to_expected[$expected_index]}
    expected_next_day_flag=${compute_start_hour_minute_plus_minutes_case_to_expected[$next_day_flag_index]}
    computeStartHourMinutePlusMinutes "$arg1" "$arg2"
    exit_status=$?
    actual="$computed_start_hour_minute_plus_minutes_result"
    actual_next_day_flag="$computed_start_hour_minute_plus_minutes_next_day_flag"
    if [ "$exit_status" -ne 0 ] ; then
        if [ "$expected" == "fail" ] ; then
            echo computeStartHourMinutePlusMinutes \"$arg1\" \"$arg2\" ... pass
        else
            echo computeStartHourMinutePlusMinutes \"$arg1\" \"$arg2\" expected zero exit status, but received non-zero ... fail
            failure_count=$(( $failure_count + 1 ))
        fi
    else
        actual=$computed_start_hour_minute_from_minutes_since_midnight
        if [ "$actual" == "$expected" ] && [ "$actual_next_day_flag" == "$expected_next_day_flag" ] ; then
            echo computeStartHourMinutePlusMinutes \"$arg1\" \"$arg2\" ... pass
        else
            echo computeStartHourMinutePlusMinutes \"$arg1\" \"$arg2\" expected \"$expected\" results and \"$expected_next_day_flag\" next_day_flag, but received \"$actual\" and \"$actual_next_day_flag\" ... fail
            failure_count=$(( $failure_count + 1 ))
        fi
    fi
    index=$(( $index + 4 ))
done

#--------------------------------------------------------------------------------
echo
echo "testing function currentlyWithinTimePeriod()"

declare -a currently_within_time_period_case_to_expected
currently_within_time_period_case_to_expected+=("0000" "1200" "1" "0" "1" "0000" "0")
currently_within_time_period_case_to_expected+=("0000" "1200" "1" "0" "1" "0001" "0")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "1159" "0")
currently_within_time_period_case_to_expected+=("0000" "1200" "1" "0" "1" "1200" "1")
currently_within_time_period_case_to_expected+=("0000" "1200" "1" "0" "1" "1201" "1")
currently_within_time_period_case_to_expected+=("0000" "1200" "1" "0" "2" "0000" "1")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "0000" "1")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "0001" "0")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "0002" "0")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "1159" "0")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "1200" "1")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "1201" "1")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "1" "2359" "1")
currently_within_time_period_case_to_expected+=("0001" "1200" "1" "0" "2" "0000" "1")
currently_within_time_period_case_to_expected+=("1200" "2359" "1" "0" "1" "1159" "1")
currently_within_time_period_case_to_expected+=("1200" "2359" "1" "0" "1" "1201" "0")
currently_within_time_period_case_to_expected+=("1200" "2359" "1" "0" "1" "1201" "0")
currently_within_time_period_case_to_expected+=("1200" "2359" "1" "0" "1" "2359" "1")
currently_within_time_period_case_to_expected+=("1200" "2359" "1" "0" "2" "0000" "1")
currently_within_time_period_case_to_expected+=("1200" "2359" "1" "0" "2" "0001" "1")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "1" "0000" "1")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "1" "0001" "1")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "1" "2259" "1")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "1" "2300" "0")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "1" "2301" "0")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "1" "2359" "0")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "2" "0000" "0")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "2" "0001" "0")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "2" "0059" "0")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "2" "0100" "1")
currently_within_time_period_case_to_expected+=("2300" "0100" "1" "1" "2" "2330" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "1" "0000" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "1" "0001" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "1" "2259" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "1" "2300" "0")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "1" "2301" "0")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "1" "2359" "0")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "2" "0000" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "2" "0001" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "2" "0059" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "2" "0100" "1")
currently_within_time_period_case_to_expected+=("2300" "0000" "1" "1" "2" "2330" "1")

index=0
while [ $index -lt ${#currently_within_time_period_case_to_expected[@]} ] ; do
    arg2_index=$(( $index + 1 ))
    arg3_index=$(( $index + 2 ))
    arg4_index=$(( $index + 3 ))
    arg5_index=$(( $index + 4 ))
    arg6_index=$(( $index + 5 ))
    expected_index=$(( $index + 6 ))
    arg1="${currently_within_time_period_case_to_expected[$index]}"
    arg2="${currently_within_time_period_case_to_expected[$arg2_index]}"
    arg3="${currently_within_time_period_case_to_expected[$arg3_index]}"
    arg4="${currently_within_time_period_case_to_expected[$arg4_index]}"
    arg5="${currently_within_time_period_case_to_expected[$arg5_index]}"
    arg6="${currently_within_time_period_case_to_expected[$arg6_index]}"
    expected=${currently_within_time_period_case_to_expected[$expected_index]}
    currentlyWithinTimePeriod "$arg1" "$arg2" "$arg3" "$arg4" "$arg5" "$arg6"
    actual=$?
    if [ "$actual" -ne 0 ] ; then
        if [ "$expected" -ne 0 ] ; then
            echo currentlyWithinTimePeriod \"$arg1\" \"$arg2\" \"$arg3\" \"$arg4\" \"$arg5\" \"$arg6\" ... pass
        else
            echo currentlyWithinTimePeriod \"$arg1\" \"$arg2\" \"$arg3\" \"$arg4\" \"$arg5\" \"$arg6\" expected zero exit status, but received non-zero ... fail
            failure_count=$(( $failure_count + 1 ))
        fi
    else
        if [ "$expected" -ne 0 ] ; then
            echo currentlyWithinTimePeriod \"$arg1\" \"$arg2\" \"$arg3\" \"$arg4\" \"$arg5\" \"$arg6\" expected non-zero exit status, but received zero ... fail
            failure_count=$(( $failure_count + 1 ))
        else
            echo currentlyWithinTimePeriod \"$arg1\" \"$arg2\" \"$arg3\" \"$arg4\" \"$arg5\" \"$arg6\" ... pass
        fi
    fi
    index=$(( $index + 7 ))
done

#--------------------------------------------------------------------------------
echo
echo "testing results"
echo "  count of failed test cases: $failure_count"
echo
echo "testing complete."
exit $failure_count
