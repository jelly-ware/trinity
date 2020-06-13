package org.jellyware.trinity.conv;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonString;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class StringList implements AttributeConverter<List<String>, String> {
	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		var builder = Json.createArrayBuilder();
		if (attribute != null)
			for (var string : attribute)
				builder.add(string);
		return builder.build().toString();
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null)
			return new ArrayList<>();
		return Json.createReader(new StringReader(dbData)).readArray().stream().map(s -> ((JsonString) s).getString())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
