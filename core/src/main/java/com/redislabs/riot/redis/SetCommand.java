package com.redislabs.riot.redis;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.redislabs.riot.convert.ObjectMapperConverter;
import io.lettuce.core.RedisFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.redis.support.CommandBuilder;
import org.springframework.core.convert.converter.Converter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.function.BiFunction;

@Slf4j
@Command(name = "set", description = "Set strings from input")
public class SetCommand extends AbstractKeyCommand {

    private enum StringFormat {
        RAW, XML, JSON
    }

    @Option(names = "--format", description = "Serialization: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})", paramLabel = "<fmt>")
    private StringFormat format = StringFormat.JSON;
    @SuppressWarnings("unused")
    @Option(names = "--field", description = "Raw value field", paramLabel = "<field>")
    private String field;
    @SuppressWarnings("unused")
    @Option(names = "--root", description = "XML root element name", paramLabel = "<name>")
    private String root;

    @Override
    public BiFunction<?, Map<String, Object>, RedisFuture<?>> command() {
        return configureKeyCommandBuilder(CommandBuilder.set()).valueConverter(stringValueConverter()).build();
    }

    private Converter<Map<String, Object>, String> stringValueConverter() {
        switch (format) {
            case RAW:
                if (field==null) {
                    log.warn("Raw value field name not set");
                }
                return stringFieldExtractor(field);
            case XML:
                return new ObjectMapperConverter<>(new XmlMapper().writer().withRootName(root));
            default:
                ObjectMapper jsonMapper = new ObjectMapper();
                jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                jsonMapper.setSerializationInclusion(Include.NON_NULL);
                return new ObjectMapperConverter<>(jsonMapper.writer().withRootName(root));
        }
    }

}