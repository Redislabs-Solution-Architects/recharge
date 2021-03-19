package com.redislabs.riot.processor;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionInvocationTargetException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpelProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private final StandardEvaluationContext context;
    private final Map<String, Expression> expressions = new LinkedHashMap<>();
    private final AtomicLong index = new AtomicLong();

    public SpelProcessor(StatefulConnection<String, String> connection, DateFormat dateFormat, Map<String, String> variables, Map<String, String> fields) {
        Assert.notNull(connection, "A Redis connection is required.");
        Assert.notNull(dateFormat, "A DateFormat instance is required.");
        Assert.isTrue(fields != null && !fields.isEmpty(), "At least one field is required.");
        // keep a reference to the connection object to avoid GC reclaiming it
        this.context = new StandardEvaluationContext();
        context.setVariable("date", dateFormat);
        context.setVariable("index", index);
        context.setVariable("redis", connection instanceof StatefulRedisClusterConnection ? ((StatefulRedisClusterConnection<String, String>) connection).sync() : ((StatefulRedisConnection<String, String>) connection).sync());
        SpelExpressionParser parser = new SpelExpressionParser();
        if (variables != null) {
            variables.forEach((k, v) -> context.setVariable(k, parser.parseExpression(v).getValue(context)));
        }
        Method geoMethod;
        try {
            geoMethod = getClass().getDeclaredMethod("geo", String.class, String.class);
            context.registerFunction("geo", geoMethod);
        } catch (NoSuchMethodException | SecurityException e) {
            log.error("Could not register geo function", e);
        }
        context.setPropertyAccessors(Collections.singletonList(new MapAccessor()));
        fields.forEach((k, v) -> expressions.put(k, parser.parseExpression(v)));
    }

    @Override
    public Map<String, Object> process(Map<String, Object> item) {
        Map<String, Object> map = new HashMap<>(item);
        synchronized (context) {
            for (String field : expressions.keySet()) {
                try {
                    Object value = expressions.get(field).getValue(context, map);
                    if (value != null) {
                        map.put(field, value);
                    }
                } catch (ExpressionInvocationTargetException e) {
                    log.error("Error while evaluating field {}", field, e);
                    throw e;
                }
            }
            index.incrementAndGet();
        }
        return map;
    }

    public static String geo(String longitude, String latitude) {
        if (longitude == null || latitude == null) {
            return null;
        }
        return longitude + "," + latitude;
    }

}