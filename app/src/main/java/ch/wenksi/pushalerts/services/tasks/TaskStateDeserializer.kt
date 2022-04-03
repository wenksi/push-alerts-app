package ch.wenksi.pushalerts.services.tasks

import ch.wenksi.pushalerts.models.TaskState
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class TaskStateDeserializer : JsonDeserializer<TaskState> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): TaskState {
        return TaskState.values().first { s -> s.ordinal == json.asInt }
    }
}