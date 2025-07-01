package com.yourname.mycreateaddon.content.kinetics.node;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;


public class OreNodeModelLoader implements IGeometryLoader<OreNodeModelGeometry> {
    @Override
    public OreNodeModelGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        // 이 모델은 JSON에서 추가 데이터를 읽지 않으므로, 그냥 새 인스턴스를 반환합니다.
        return new OreNodeModelGeometry();
    }
}