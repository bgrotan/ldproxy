<a name="transformations"></a>

# Transformations

Transformations are supported in multiple parts of the configuration.

Transformations do not affect data sources, they are applied on-the-fly as part of the encoding.

Filter expressions do not take transformations into account, they have to be based on the source values.


|Transformation |Data Type |Description
| --- | --- | ---
|`rename` |string |Rename a property.
|`remove` |enum |`IN_COLLECTION` (until version 3.0: `OVERVIEW`) skips the property only for the *Features* resource, `ALWAYS` always skips it, `NEVER` never skips it.
|`flatten` |string |Flattens object or array properties using the given separator. For arrays the property name is formed by the original property name followed by pairs of separator and array position. For objects the property name is formed by concatenating the original property separated by the given separator. Can only be applied on the feature level in the provider or using the wildcard property name `*` otherwise.
|`reduceStringFormat` |string |Reduces an object to a string using the same syntax as `stringFormat` but with additional replacements for objects property names.
|`nullify` |array |Maps all values matching the list of regular expressions to `null`. Not applicable for properties containing objects.
|`null` |regex |*Deprecated* See `nullify`.
|`stringFormat` |string |Format a value, where `{{value}}` is replaced with the actual value and `{{serviceUrl}}` is replaced with the API landing page URI. Additonal operations can be applied to `{{value}}` by chaining them with `|`, see the examples below.
|`dateFormat` |string |Format date(-time) values with the given [pattern](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns), e.g. `dd.MM.yyyy` for German notation.
|`codelist`|string |Maps the value according to the given [codelist](../../codelists/README.md). If the value is not found in the codelist or the codelist does not exist, the original value is passed through.  Falls der Wert nicht in der Codelist enthalten ist oder die Codelist nicht gefunden wird, bleibt der Wert unverändert. Not applicable for properties containing objects.

### Examples for `stringFormat`

* `https://example.com/id/kinder/kita/{{value}}` inserts the value into the URI template.
* `{{value | replace:'\\s*[0-9].*$':''}}` removes all white space and numbers at the end (e.g. to remove a street number)
* `{{value | replace:'^[^0-9]*':''}}` removes everything before the first digit
* `{{value | toUpper}}` transforms the value to upper case
* `{{value | toLower}}` transforms the value to lower case
* `{{value | urlEncode}}` encodes special characters in the text for usage as aprt of an URI
* `[{{value}}](https://de.wikipedia.org/wiki/{{value | replace:' ':'_' | urlencode}})` transforms a value into a markdown link to a Wikipedia entry
