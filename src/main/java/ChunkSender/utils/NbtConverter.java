package ChunkSender.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.*;

import java.util.Objects;

public class NbtConverter {

	public static JsonObject compoundTagToJson(CompoundTag tag) {
		JsonObject json = new JsonObject();

		for (String tagName : tag.getAllKeys()) {
			addTagToJson(json, tagName, Objects.requireNonNull(tag.get(tagName)));
		}
		return json;
	}

	public static void addTagToJson(JsonObject json, String tagName, Tag tag) {
		json.add(tagName, getJsonType(tag));
	}

	public static JsonElement getJsonType(Tag tag) {
		switch (tag.getId()) {
			case Tag.TAG_STRING:
				return new JsonPrimitive(tag.getAsString());
			case Tag.TAG_BYTE:
			case Tag.TAG_SHORT:
			case Tag.TAG_INT:
				return new JsonPrimitive(((NumericTag) tag).getAsInt());
			case Tag.TAG_LONG:
				return new JsonPrimitive((int) ((NumericTag) tag).getAsLong());
			case Tag.TAG_FLOAT:
			case Tag.TAG_DOUBLE:
				return new JsonPrimitive(((NumericTag) tag).getAsDouble());
			case Tag.TAG_COMPOUND:
				return compoundTagToJson((CompoundTag) tag);
			case Tag.TAG_LIST:
				return listTagToJson((ListTag) tag);
			case Tag.TAG_INT_ARRAY:
			case Tag.TAG_BYTE_ARRAY:
			case Tag.TAG_LONG_ARRAY:
				return numericCollectionTagToJson((CollectionTag) tag);
			default:
				throw new RuntimeException("Invalid tag type " + tag.getId());
		}
	}

	public static JsonArray listTagToJson(ListTag tag) {
		JsonArray array = new JsonArray();
		for (Tag tag2 : tag) {
			array.add(getJsonType(tag2));
		}
		return array;
	}

	public static JsonArray numericCollectionTagToJson(CollectionTag<NumericTag> tag) {
		JsonArray array = new JsonArray();
		for (NumericTag number : tag) {
			array.add(number.getAsInt());
		}
		return array;
	}
}
