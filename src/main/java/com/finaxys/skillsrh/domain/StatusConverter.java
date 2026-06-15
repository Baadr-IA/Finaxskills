package com.finaxys.skillsrh.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StatusConverter implements AttributeConverter<Status, String> {

    @Override
    public String convertToDatabaseColumn(Status attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        return Status.fromLabel(dbData);
    }
}

