package com.redislabs.riot.stream;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.core.convert.converter.Converter;

import java.util.HashMap;
import java.util.Map;

public class AvroToMapConverter implements Converter<ConsumerRecord<String, Object>, Map<String, String>> {

    private final Converter<Map<String, Object>, Map<String, String>> flattener;

    public AvroToMapConverter(Converter<Map<String, Object>, Map<String, String>> flattener) {
        this.flattener = flattener;
    }

    @Override
    public Map<String, String> convert(ConsumerRecord<String, Object> source) {
        GenericRecord record = (GenericRecord) source.value();
        Map<String, Object> map = new HashMap<>();
        record.getSchema().getFields().forEach(field -> map.put(field.name(), record.get(field.name())));
        return flattener.convert(map);
    }

}