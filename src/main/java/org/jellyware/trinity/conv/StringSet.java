package org.jellyware.trinity.conv;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonString;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class StringSet implements AttributeConverter<Set<String>, String> {
	@Override
	public String convertToDatabaseColumn(Set<String> attribute) {
		var builder = Json.createArrayBuilder();
		if (attribute != null)
			for (var string : (Iterable<String>) attribute.stream().sorted()::iterator)
				builder.add(string);
		return builder.build().toString();
	}

	@Override
	public Set<String> convertToEntityAttribute(String dbData) {
		if (dbData == null)
			return new HashSet<>();
		return Json.createReader(new StringReader(dbData)).readArray().stream().map(s -> ((JsonString) s).getString())
				.collect(Collectors.toCollection(HashSet::new));
	}
}
