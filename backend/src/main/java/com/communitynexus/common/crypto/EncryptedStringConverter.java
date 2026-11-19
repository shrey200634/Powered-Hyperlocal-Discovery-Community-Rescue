package com.communitynexus.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Usage on any entity field:
 *   @Convert(converter = EncryptedStringConverter.class)
 *   @Column(name = "phone_enc", length = 120)
 *   private String phone;
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : FieldEncryptor.get().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FieldEncryptor.get().decrypt(dbData);
    }
}