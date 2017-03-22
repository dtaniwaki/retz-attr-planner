.PHONY: license jar

license:
	./gradlew licenseFormatMain licenseFormatTest

jar:
	./gradlew jar
