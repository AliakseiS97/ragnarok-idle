package com.ragnarok.idle.math;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * autoApply=true — применяется ко всем полям типа BigNum во всех сущностях
 * автоматически, без @Convert на каждом поле (их будет много: gold, ash,
 * baseDps, price, урон боссов и т.д. — см. модель данных в GDD §12.3).
 */
@Converter(autoApply = true)
public class BigNumConverter implements AttributeConverter<BigNum, String> {

    @Override
    public String convertToDatabaseColumn(BigNum attribute) {
        return attribute == null ? null : attribute.toStorageString();
    }

    @Override
    public BigNum convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BigNum.fromStorageString(dbData);
    }
}
