#!/usr/bin/env bash

# This library of bash functions provide time handling and management capabilities.
# The primary purpose is to support the observance of certain "lockout" time periods
# where particular network resources will not be interacted with because these are
# observed periods of instability in those servers.
#
# The library is scaled to a granularity of minutes, and time periods are specified
# using a "hour_minute" format string, such as "1820" which represents 6:20 pm. The
# valid range for an hour_minute string is 0000 (midnight) to 2359 (11:59 pm).
#
# Natural number durations in minutes can be used to define the length of a period
# from a specified start time. Periods are properly handled when they extend into
# the following day (when the duration leads to a end time which occurs on the day
# after the day on which the start time began. Durations must be less than one
# full day, so only one midnight transition is supported.
#
# General comparison of times in hour_minute format is available through the
# compareHourMinuteStrings() function, and timestamp acquisition in the hour_minute
# format for the current time of day is available through the captureDayHourMinute()
# function. The main function provided is waitWhileWithinTimePeriod(), which will
# observe a pause during a "lockout" time period.

# GLOBAL CONSTANTS
TIME_COMPARISON_1_EQ_2_CODE=0
TIME_COMPARISON_1_LT_2_CODE=1
TIME_COMPARISON_2_LT_1_CODE=2
TIME_COMPARISON_ERROR_CODE=99
DEFAULT_SLEEP_INTERVAL_SECONDS=30

# GLOBAL VARIABLES for returning values from called functions
captured_day_of_week=0
captured_hour_minute=0
digit_string_with_zero_prefix_stripped=0
computed_minutes_since_midnight=0
computed_start_hour_minute_from_minutes_since_midnight=0
computed_start_hour_minute_plus_minutes_result=0
computed_start_hour_minute_plus_minutes_next_day_flag=0

# FUNCTIONS

# sets captured_day_of_week and captured_hour_minute according to the current time
function captureDayHourMinute() {
    split_day_hour_minute_re='^([0-9])(.*)$'
    day_hour_minute=$(date +"%w%H%M")
    if [[ "$day_hour_minute" =~ $split_day_hour_minute_re ]] ; then
        captured_day_of_week="${BASH_REMATCH[1]}"
        captured_hour_minute="${BASH_REMATCH[2]}"
    else
        captured_day_of_week=0
        captured_hour_minute=0
    fi
}

# checks if argument is a string containing only 1 to 4 digits ([0-9])
# returns 0 for yes/success, 1 for no/failure
function stringIs1to4Digits() {
    s=$1
    one_to_four_digits_re='^[0-9][0-9]?[0-9]?[0-9]?$'
    if [[ "$s" =~ $one_to_four_digits_re ]] ; then
        return 0
    fi
    return 1
}

# expected input is a natural number, possibly with a prefix including one or more zeros
# if function is successful digit_string_with_zero_prefix_stripped is set to the same number without the zeros prefix (if all zeros, a single 0 is returned)
function stripZeroPrefixFromDigitString() {
    number=$1
    drop_zero_prefix_re='^0*([0-9]+)$'
    if [[ "$number" =~ $drop_zero_prefix_re ]] ; then
        digit_string_with_zero_prefix_stripped="${BASH_REMATCH[1]}"
        if [ -z "$digit_string_with_zero_prefix_stripped" ] ; then
            digit_string_with_zero_prefix_stripped="0"
        fi
        return 0
    fi
    digit_string_with_zero_prefix_stripped=0
    return 1
}

# sets computed_minutes_since_midnight and returns 0 on success
# expected input is HHMM string : (from 0000 to 2359) : return 0 for success
function computeMinutesSinceMidnightFromHourMinute() {
    hour_minute="$1"
    computed_minutes_since_midnight=0
    if ! stringIs1to4Digits "$hour_minute" ; then
        return 1
    fi
    # zero prefix must be removed before doing math .. or bash interprets strings as octal
    if stripZeroPrefixFromDigitString "$hour_minute" ; then
        hour_minute=$digit_string_with_zero_prefix_stripped
    else
        return 1
    fi
    if [ "$hour_minute" -lt 60 ] ; then
        computed_minutes_since_midnight=$hour_minute # 0 - 59 means minutes since midnight
        return 0
    fi
    if [ "$hour_minute" -lt 100 ] ; then
        return 1 # 60 - 99 are not valid ... MM cannot be over 59
    fi
    split_hour_minute_re='^([0-9]+)([0-5][0-9])$'
    if [[ "$hour_minute" =~ $split_hour_minute_re ]] ; then
        hour_field="${BASH_REMATCH[1]}"
        minute_field="${BASH_REMATCH[2]}"
        stripZeroPrefixFromDigitString "$hour_field"
        hour_field="$digit_string_with_zero_prefix_stripped"
        stripZeroPrefixFromDigitString "$minute_field"
        minute_field="$digit_string_with_zero_prefix_stripped"
        if [ $hour_field -gt 23 ] ; then
            return 1 # maximum hour is 23
        fi
        computed_minutes_since_midnight=$(( $hour_field * 60 + $minute_field ))
        return 0
    fi
    return 1
}

# expected input is 2 hour_minute strings of format HHMM : (from 0000 to 2359)
# returns:
#   TIME_COMPARISON_ERROR_CODE if either string is malformated
#   TIME_COMPARISON_1_EQ_2_CODE if the two times are equivalent
#   TIME_COMPARISON_1_LT_2_CODE if time string 1 is less than time string 2
#   TIME_COMPARISON_2_LT_1_CODE if time string 2 is less than time string 1
function compareHourMinuteStrings() {
    hour_minute_1=$1
    hour_minute_2=$2
    minutes_since_midnight_1=0
    minutes_since_midnight_2=0
    if computeMinutesSinceMidnightFromHourMinute $hour_minute_1 ; then
        minutes_since_midnight_1=$computed_minutes_since_midnight
    else
        return $TIME_COMPARISON_ERROR_CODE
    fi
    if computeMinutesSinceMidnightFromHourMinute $hour_minute_2 ; then
        minutes_since_midnight_2=$computed_minutes_since_midnight
    else
        return $TIME_COMPARISON_ERROR_CODE
    fi
    if [ "$minutes_since_midnight_1" == "$minutes_since_midnight_2" ] ; then
        return $TIME_COMPARISON_1_EQ_2_CODE
    fi
    if [ "$minutes_since_midnight_1" -lt "$minutes_since_midnight_2" ] ; then
        return $TIME_COMPARISON_1_LT_2_CODE
    fi
    if [ "$minutes_since_midnight_1" -gt "$minutes_since_midnight_2" ] ; then
        return $TIME_COMPARISON_2_LT_1_CODE
    fi
    return $TIME_COMPARISON_ERROR_CODE
}

# expected input is a count of minutes since midnight (max: 1439)
# if function succeeds, computed_start_hour_minute_from_minutes_since_midnight is set to the hour_minute string of format HHMM : (from 0000 to 2359)
# return value is 0 on success
function computeStartHourMinuteFromMinutesSinceMidnight() {
    minutes_since_midnight=$1
    if stripZeroPrefixFromDigitString "$minutes_since_midnight" ; then
        minutes_since_midnight=$digit_string_with_zero_prefix_stripped
    else
        return 1
    fi
    # period must be within one day
    if [ "$minutes_since_midnight" -gt 1439 ] ; then
        return 1
    fi
    hours=$(( $minutes_since_midnight / 60 ))
    minutes=$(( $minutes_since_midnight % 60 ))
    computed_start_hour_minute_from_minutes_since_midnight=$(printf "%02d%02d" $hours $minutes)
    return 0
}

# expected input is an hour_minute strings of format HHMM : (from 0000 to 2359) and an (integer) duration count of minutes (max: 1439)
# if function succeeds, computed_start_hour_minute_plus_minutes_result is set to the hour_minute string of the start time plus duration minutes
# if result is the following day, computed_start_hour_minute_plus_minutes_next_day_flag is set to 1, otherwise it is set to 0
# return value is 0 on success
function computeStartHourMinutePlusMinutes() {
    start_hour_minute=$1
    duration_minutes=$2
    computed_start_hour_minute_plus_minutes_result=0
    computed_start_hour_minute_plus_minutes_next_day_flag=0
    start_minutes_since_midnight=0
    if computeMinutesSinceMidnightFromHourMinute $start_hour_minute ; then
        start_minutes_since_midnight=$computed_minutes_since_midnight
    else
        return 1
    fi
    if stripZeroPrefixFromDigitString "$duration_minutes" ; then
        duration_minutes=$digit_string_with_zero_prefix_stripped
    else
        return 1
    fi
    if [ "$duration_minutes" -gt 1439 ] ; then
        # maximum duration is 23 hours 59 minutes, so the end time cannot be later than the following day
        return 1
    fi
    end_minutes_since_midnight=$(( $start_minutes_since_midnight + $duration_minutes ))
    if [ "$end_minutes_since_midnight" -gt 1439 ] ; then
        computed_start_hour_minute_plus_minutes_next_day_flag=1
        end_minutes_since_midnight=$(( $end_minutes_since_midnight - 1440 ))
    fi
    if computeStartHourMinuteFromMinutesSinceMidnight $end_minutes_since_midnight ; then
        computed_start_hour_minute_plus_minutes_result=$computed_start_hour_minute_from_minutes_since_midnight
        return 0
    fi
    return 1
}

# receives 6 arguments (see below) that define (first 4) the time period and (last 2) the current day/time
# returns 0 if the provided current day/hour_minute is within the period start/end, or 1 otherwise
#   This function is written to allow a time period which begins at any specified start time
#   (such as '1530' for starting the time period at 3:30 pm local time) and lasting up until the end time
#   specified in the same format. Time periods which begin before midnight and end after midnight are
#   supported, and so there is an argument "period_next_day_flag" which should be provided as '1' if the
#   end time should be interpreted as occurring on the following day from the start_time (or as '0' if
#   the start time and end time are to be interpreted as occurring on the same day.)
#   The argument "period_start_day_of_week" should be the weekday number of the day on which the time period
#   begins (such as '0' for Sunday, '1' for Monday, etc).
#   The current day of the week (as a weekday number) and the current hour_minute (such as 1540) is used
#   to compute a binary indication of whether or not we are in the time period. Time periods which
#   span midnight are evaluated with separate logic from those which do not. The approach here is:
#   For periods which do not span midnight:
#       We are in the time period if all of these are true:
#       -  the current time is at or after the start time
#       -  the current time is not at or after the end time
#       -  we are on the same day as the time period (midnight has not passed while waiting)
#   For periods which do span midnight there are two caes:
#       We are in the time period if (case 1) all of these are true:
#       -  the current time is at or after the start time
#       -  we are on the same day as the time period start (midnight has not passed while waiting)
#       or if (case 2) all of these are true:
#       -  the current time is not at or after the end time
#       -  we are on day following the time period start (midnight has passed while waiting)
#   If (through erroneous input arguments or bug) an error occurs, we default to returning '1'
function currentlyWithinTimePeriod() {
    period_start_hour_minute=$1 # period start hour_minutes in HHMM format
    period_end_hour_minute=$2 # period end hour_minutes in HHMM format
    period_start_day_of_week=$3 # period_start day of week in [0-6] format
    period_next_day_flag=$4 # period_next_day_flag as 0/1 for false/true
    current_day_of_week=$5 # current day of week in [0-6] format
    current_hour_minute=$6 # current hour_minutes in HHMM format
    midnight_has_passed_while_waiting_flag=0 # 0=no 1=yes
    if [ "$period_start_day_of_week" -ne "$current_day_of_week" ] ; then
        midnight_has_passed_while_waiting_flag=1
    fi
    if [ "$period_next_day_flag" -eq 0 ] ; then
        # time period is entirely within the current day
        if [ "$midnight_has_passed_while_waiting_flag" -eq 1 ] ; then
            return 1 # we have passed the entire day of the period
        fi
        # when the period start and end are both on the same day, we check whether we are after the start and before the end
        compareHourMinuteStrings "$current_hour_minute" "$period_start_hour_minute"
        result=$?
        if [ "$result" == "$TIME_COMPARISON_1_LT_2_CODE" ] ; then
            return 1 # we are not yet at the start of the period
        fi
        if [ "$result" == "$TIME_COMPARISON_1_EQ_2_CODE" ] ; then
            return 0 # we are at the start of the period ... we are within
        fi
        if [ "$result" == "$TIME_COMPARISON_2_LT_1_CODE" ] ; then
            # we have passed the period start ... check period end
            compareHourMinuteStrings "$current_hour_minute" "$period_end_hour_minute"
            result=$?
            if [ "$result" == "$TIME_COMPARISON_1_LT_2_CODE" ] ; then
                return 0 # we are not yet at the end of the period ... we are within
            fi
            if [ "$result" == "$TIME_COMPARISON_1_EQ_2_CODE" ] || [ "$result" == "$TIME_COMPARISON_2_LT_1_CODE" ] ; then
                return 1 # we have reached the end of the period or passed it
            fi
        fi
    else
        # time period spans midnight and ends in the following day (period end is on the day following period start)
        # to support this, we detect whether the current day is on the period start day or on the period end day
        # if the current day is on the period start day, we check if we have passed the start time
        # if the current day is on the period end day, we check if we are before the end time
        if [ "$midnight_has_passed_while_waiting_flag" -eq 1 ] ; then
            # we are on the day after the period started - check if we are before the end of the period end hour_minute
            compareHourMinuteStrings "$current_hour_minute" "$period_end_hour_minute"
            result=$?
            if [ "$result" == "$TIME_COMPARISON_1_LT_2_CODE" ] ; then
                return 0 # we are not yet at the end of the period ... we are within
            fi
            if [ "$result" == "$TIME_COMPARISON_1_EQ_2_CODE" ] || [ "$result" == "$TIME_COMPARISON_2_LT_1_CODE" ] ; then
                return 1 # we have reached the end of the period or passed it
            fi
        else
            # we are on the same day the period starts - check if we have reached the period start hour_minute
            compareHourMinuteStrings "$current_hour_minute" "$period_start_hour_minute"
            result=$?
            if [ "$result" == "$TIME_COMPARISON_1_LT_2_CODE" ] ; then
                return 1 # we are not yet at the start of the period
            fi
            if [ "$result" == "$TIME_COMPARISON_1_EQ_2_CODE" ] || [ "$result" == "$TIME_COMPARISON_2_LT_1_CODE" ] ; then
                return 0 # we have reached the start of the period or passed it ... we are within
            fi
        fi
    fi
    return 1 # an error occurred - return default
}

# if the current time is within the time period, this function will enter a sleep loop until that is no longer true
function waitWithinTimePeriodDetailed() {
    period_start_hour_minute="$1"
    period_end_hour_minute="$2"
    period_start_day_of_week="$3"
    period_next_day_flag="$4"
    sleep_interval_seconds="$5"
    captureDayHourMinute
    current_day_of_week="$captured_day_of_week"
    current_hour_minute="$captured_hour_minute"
    # so long as the current day/time is within the time period, we continue to sleep
    while currentlyWithinTimePeriod "$period_start_hour_minute" "$period_end_hour_minute" "$period_start_day_of_week" "$period_next_day_flag" "$current_day_of_week" "$current_hour_minute" ; do
        sleep $sleep_interval_seconds
        captureDayHourMinute
        current_day_of_week="$captured_day_of_week"
        current_hour_minute="$captured_hour_minute"
    done
    return 0
}

# this is a convenience function which converts the time period start time and duration into start time, end time, start date, and a flag indicating whether midnight is spanned by the time period
# an optional third argument provides the sleep interval per cycle
# it then calls the waitWithinTimePeriodDetailed() function
function waitWhileWithinTimePeriod() {
    period_start_hour_minute="$1"
    period_duration_minutes="$2"
    sleep_interval_seconds="$3"
    captureDayHourMinute
    period_start_day_of_week="$captured_day_of_week"
    computeStartHourMinutePlusMinutes "$period_start_hour_minute" "$period_duration_minutes"
    period_end_hour_minute="$computed_start_hour_minute_plus_minutes_result"
    period_next_day_flag="$computed_start_hour_minute_plus_minutes_next_day_flag"
    if ! stringIs1to4Digits "$sleep_interval_seconds" ; then
        sleep_interval_seconds="$DEFAULT_SLEEP_INTERVAL_SECONDS"
    fi
    waitWithinTimePeriodDetailed "$period_start_hour_minute" "$period_end_hour_minute" "$period_start_day_of_week" "$period_next_day_flag" "$sleep_interval_seconds"
    return 0
}
