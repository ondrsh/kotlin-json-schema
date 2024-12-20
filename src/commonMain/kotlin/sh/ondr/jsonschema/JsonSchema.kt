package sh.ondr.jsonschema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Provides a default configuration for encoding [JsonSchema] instances into JSON.
 */
object SchemaEncoder {
	val format = Json {
		encodeDefaults = true
		explicitNulls = false
	}
}

/**
 * Converts this [JsonSchema] instance into a [JsonElement] using [SchemaEncoder].
 */
fun JsonSchema.toJsonElement(): JsonElement = SchemaEncoder.format.encodeToJsonElement(this)

/**
 * Represents a JSON Schema definition.
 *
 * This sealed class and its subclasses model different types in JSON Schema:
 *
 * - [ObjectSchema]: Represents a JSON object with defined properties.
 * - [StringSchema]: Represents a string type, possibly with an enumeration of allowed values.
 * - [NumberSchema]: Represents a numeric type (integers, floats, doubles).
 * - [ArraySchema]: Represents an array type, specifying items as another schema.
 * - [BooleanSchema]: Represents a boolean type.
 */
@Serializable
sealed class JsonSchema {
	/**
	 * Represents an "object" type schema, describing its properties, required fields, and optionally
	 * additional properties.
	 */
	@Serializable
	@SerialName("object")
	class ObjectSchema(
		val properties: Map<String, JsonSchema>? = null,
		val required: List<String>? = null,
		val additionalProperties: JsonElement? = null,
	) : JsonSchema()

	/**
	 * Represents a "string" type schema. The [enum] field, if present, restricts the string to a
	 * predefined set of values.
	 */
	@Serializable
	@SerialName("string")
	class StringSchema(val enum: List<String>? = null) : JsonSchema()

	/**
	 * Represents a "number" type schema. In JSON Schema, "number" covers both integers and floating
	 * point numbers.
	 */
	@Serializable
	@SerialName("number")
	class NumberSchema() : JsonSchema()

	/**
	 * Represents an "array" type schema. The [items] field specifies the schema for elements in the array.
	 */
	@Serializable
	@SerialName("array")
	class ArraySchema(val items: JsonSchema) : JsonSchema()

	/**
	 * Represents a "boolean" type schema.
	 */
	@Serializable
	@SerialName("boolean")
	class BooleanSchema() : JsonSchema()
}
