package org.jellyware.trinity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		System.out.println(LocalDate.now()
				.format(DateTimeFormatter.ofPattern("dd-MMM-uuuu")));
	}
}
