package RM2;

import java.util.List;
import java.util.Map;

public class CommonOutput {
    public static final String general_fail = "failed";
    public static final String general_success = "successful";
    public static final String addEvent_fail_cannot_decrease_capacity = "Cannot decrease capacity";
    public static final String addMovie_success_added = "Movie added successfully";
    public static final String addEvent_success_capacity_updated = "Movie updated successfully";
    public static final String removeMovie_fail_no_such_show = "No such show";
    public static final String bookEvent_fail_no_such_event = "No such show";
    public static final String bookEvent_fail_no_capacity = "show is full";
    public static final String bookEvent_fail_weekly_limit = "Weekly limit reached";
    public static final String cancelEvent_fail_not_registered_in_event = "You are not registered in show";
    public static final String cancelEvent_fail_no_such_event = "No such show";
    public static final String swapEvent_fail_no_such_event = "No such show";
    public static final String swapEvent_fail_not_registered_in_event = "You are not registered in show";
    private static final String SUCCESS = "Success:";
    private static final String FAIL = "Fail:";

    private static String standardOutput(boolean isSuccess, String method, String output) {
        if (isSuccess)
            return SUCCESS + method + " > " + output;
        else
            return FAIL + method + " > " + output;
    }

    public static String addMovieOutput(boolean isSuccess, String reason) {
        if (isSuccess) {
            if (reason == null) {
                reason = general_success;
            }
        } else {
            if (reason == null) {
                reason = general_fail;
            }
        }
        return standardOutput(isSuccess, "addMovie", reason);
    }

    /**
     * Format of each string in allEventIDsWithCapacity --> EventID+ one space + remainingCapacity
     */
    public static String listEventAvailabilityOutput(boolean isSuccess, List<String> allEventIDsWithCapacity, String reason) {
        if (isSuccess) {
            reason = general_success;
            if (allEventIDsWithCapacity.size() > 0) {
                reason = RM1.CommonOutput.getString(allEventIDsWithCapacity, reason);
            }
        } else {
            reason = general_fail;
        }
        return standardOutput(isSuccess, "listMovieAvailability", reason);
    }

    public static String removeMovieOutput(boolean isSuccess, String reason) {
        if (isSuccess) {
            reason = general_success;
        } else {
            if (reason == null) {
                reason = general_fail;
            }
        }
        return standardOutput(isSuccess, "removeMovie", reason);
    }

    public static String bookMovieOutput(boolean isSuccess, String reason) {
        if (isSuccess) {
            reason = general_success;
        } else {
            if (reason == null) {
                reason = general_fail;
            }
        }
        return standardOutput(isSuccess, "bookMovie", reason);
    }

    //Format for output EventType+ one space + EventID
    public static String getBookingScheduleOutput(boolean isSuccess, Map<String, List<String>> events, String reason) {
        if (isSuccess) {
            reason = general_success;
            if (events.size() > 0) {
                StringBuilder reasonBuilder = new StringBuilder();
                for (String eventType :
                        events.keySet()) {
                    for (String eventID :
                            events.get(eventType)) {
                        reasonBuilder.append(eventType).append(" ").append(eventID).append("@");
                    }
                }
                reason = reasonBuilder.toString();
                if (!reason.equals(""))
                    reason = reason.substring(0, reason.length() - 1);
            }
        } else {
            if (reason == null) {
                reason = general_fail;
            }
        }
        return standardOutput(isSuccess, "getBookingSchedule", reason);
    }

    public static String cancelEventOutput(boolean isSuccess, String reason) {
        if (isSuccess) {
            reason = general_success;
        } else {
            if (reason == null) {
                reason = general_fail;
            }
        }
        return standardOutput(isSuccess, "cancelMovie", reason);
    }


    public static String swapEventOutput(boolean isSuccess, String reason) {
        if (isSuccess) {
            reason = general_success;
        } else {
            if (reason == null) {
                reason = general_fail;
            }
        }
        return standardOutput(isSuccess, "swapMovie", reason);
    }
}