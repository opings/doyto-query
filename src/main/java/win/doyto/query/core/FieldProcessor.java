package win.doyto.query.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import win.doyto.query.annotation.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static win.doyto.query.core.CommonUtil.wrapWithParenthesis;
import static win.doyto.query.core.Constant.*;

/**
 * FieldProcessor
 *
 * @author f0rb on 2019-06-04
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FieldProcessor {

    private static final Map<Field, Processor> FIELD_PROCESSOR_MAP = new ConcurrentHashMap<>();
    public static final Processor EMPTY_PROCESSOR = ((argList, value) -> EMPTY);

    static String resolvedNestedQueries(List<Object> argList, Object value, NestedQueries nestedQueries, Processor processor) {
        String rest = resolvedNestedQueries(nestedQueries);
        IntStream.range(0, StringUtils.countMatches(rest, REPLACE_HOLDER)).mapToObj(i -> value).forEach(argList::add);
        if (nestedQueries.appendWhere()) {
            rest += processor.process(argList, value);
        }
        return nestedQueries.column() + rest + StringUtils.repeat(')', nestedQueries.value().length);
    }

    private static String resolvedNestedQueries(NestedQueries nestedQueries) {
        StringBuilder subquery = new StringBuilder();
        String lastOp = nestedQueries.op();
        NestedQuery[] value = nestedQueries.value();
        NestedQuery nestedQuery = value[0];
        subquery.append(SPACE).append(lastOp).append(" (").append(getNestedQuery(nestedQuery));
        lastOp = nestedQuery.op();
        for (int i = 1, valueLength = value.length; i < valueLength; i++) {
            nestedQuery = value[i];
            subquery.append(WHERE).append(nestedQuery.left());
            subquery.append(SPACE).append(lastOp).append(" (").append(getNestedQuery(nestedQuery));
            lastOp = nestedQuery.op();
        }
        return subquery.toString();
    }

    private static String getNestedQuery(NestedQuery nestedQuery) {
        return SELECT +
            nestedQuery.left() +
            FROM +
            nestedQuery.table() +
            (nestedQuery.extra().isEmpty() ? EMPTY : SPACE + nestedQuery.extra());
    }

    @SuppressWarnings("unchecked")
    static String resolvedSubQuery(List<Object> argList, Object value, SubQuery subQuery, Processor processor) {
        String clause = SELECT + subQuery.left() + FROM + subQuery.table() + processor.process(argList, value);
        return subQuery.column() + SPACE + subQuery.op() + SPACE + wrapWithParenthesis(clause);
    }

    static void init(Field field) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        //Ternary operators should not be nested
        //squid:S3358
        //You can do something, doesn't mean you should
        Processor processor;
        if (boolean.class.isAssignableFrom(fieldType)) {
            processor = EMPTY_PROCESSOR;
        } else if (PageQuery.class.isAssignableFrom(fieldType)){
            processor = (argList, value) -> QueryBuilder.buildWhere("", value, argList);
        } else {
            processor = new DefaultProcessor(fieldName);
        }
        if (field.isAnnotationPresent(QueryTableAlias.class)) {
            String columnName = field.getAnnotation(QueryTableAlias.class).value();
            FIELD_PROCESSOR_MAP.put(field, (argList, value) -> QuerySuffix.buildAndSql(argList, value, columnName + "." + fieldName));
        } else if (field.isAnnotationPresent(QueryField.class)) {
            String andSQL = field.getAnnotation(QueryField.class).and();
            FIELD_PROCESSOR_MAP.put(field, (argList, value) -> {
                IntStream.range(0, StringUtils.countMatches(andSQL, REPLACE_HOLDER)).mapToObj(i -> value).forEach(argList::add);
                return andSQL;
            });
        } else if (field.isAnnotationPresent(SubQuery.class)) {
            SubQuery subQuery = field.getAnnotation(SubQuery.class);
            FIELD_PROCESSOR_MAP.put(field, (argList, value) -> resolvedSubQuery(argList, value, subQuery, processor));
        } else if (field.isAnnotationPresent(NestedQueries.class)) {
            NestedQueries nestedQueries = field.getAnnotation(NestedQueries.class);
            FIELD_PROCESSOR_MAP.put(field, (argList, value) -> resolvedNestedQueries(argList, value, nestedQueries, processor));
        } else {
            FIELD_PROCESSOR_MAP.put(field, (argList, value) -> QuerySuffix.buildAndSql(argList, value, fieldName));
        }
    }

    static String execute(List<Object> argList, Field field, Object value) {
        return FIELD_PROCESSOR_MAP.get(field).process(argList, value);
    }

    interface Processor {
        String process(List<Object> argList, Object value);
    }

    @AllArgsConstructor
    static class DefaultProcessor implements Processor {
        private String fieldName;

        @Override
        public String process(List<Object> argList, Object value) {
            return QuerySuffix.buildWhereSql(argList, value, fieldName);
        }
    }

}
