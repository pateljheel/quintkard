package io.quintkard.quintkardapp.agenttool;

public record ConvertTimeZoneArgs(
        String dateTime,
        String fromTimeZone,
        String toTimeZone
) {
}
