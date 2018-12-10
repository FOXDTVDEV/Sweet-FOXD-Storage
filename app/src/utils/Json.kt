package fr.rhaz.ipfs.sweet.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

fun json(value: Boolean) = JsonPrimitive(value)
fun json(value: Int) = JsonPrimitive(value)
fun json(value: String) = JsonPrimitive(value)
fun json(value: List<String>) = JsonArray().apply{value.forEach(::add)}

fun JsonObject.set(key: String, value: JsonElement) = add(key, value)
fun JsonObject.boolean(key: String) = getAsJsonPrimitive(key).asBoolean
fun JsonObject.string(key: String) = getAsJsonPrimitive(key).asString
fun JsonObject.int(key: String) = getAsJsonPrimitive(key).asInt

fun JsonObject.array(key: String): JsonArray {
    if(key !in keySet()) set(key, JsonArray())
    return getAsJsonArray(key)
}

fun JsonObject.obj(key: String): JsonObject {
    if(key !in keySet()) set(key, JsonObject())
    return getAsJsonObject(key)
}
