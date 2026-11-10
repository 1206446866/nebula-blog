package com.nebula.api.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import java.util.List;

/** 索引只提供候选身份和排序键，公开展示字段由本机MySQL重新读取。 */
public record SearchHit(String kind, long id, long generation, List<FieldValue> sort) {
    public String key() { return kind + ":" + id; }
}
