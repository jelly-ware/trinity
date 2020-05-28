package org.jellyware.trinity.conv;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class Error implements AttributeConverter<org.jellyware.beef.Error, String> {
	@Inject
	private Jsonb jsonb;

	@Override
	public String convertToDatabaseColumn(org.jellyware.beef.Error attribute) {
		return jsonb.toJson(attribute);
	}

	@Override
	public org.jellyware.beef.Error convertToEntityAttribute(String dbData) {
		return jsonb.fromJson(dbData, org.jellyware.beef.Error.class);
	}
}
