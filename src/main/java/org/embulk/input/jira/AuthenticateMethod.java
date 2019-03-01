package org.embulk.input.jira;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.embulk.config.ConfigException;

public enum AuthenticateMethod {
    BASIC;
    @JsonValue
    @Override
    public String toString()
    {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static AuthenticateMethod fromString(String value)
    {
        switch(value) {
        case "basic":
            return BASIC;
        default:
            throw new ConfigException(String.format("Unknown AuthenticateMethod value '%s'. Supported values is basic.", value));
        }
    }
}
