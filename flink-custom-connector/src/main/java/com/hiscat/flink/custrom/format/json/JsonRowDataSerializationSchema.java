package com.hiscat.flink.custrom.format.json;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonFormatOptions;
import org.apache.flink.formats.json.JsonRowDataDeserializationSchema;
import org.apache.flink.formats.json.RowDataToJsonConverters;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonGenerator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;

import java.util.Objects;

/**
 * Serialization schema that serializes an object of Flink internal data structure into a JSON
 * bytes.
 *
 * <p>Serializes the input Flink object into a JSON string and converts it into <code>byte[]</code>.
 *
 * <p>Result <code>byte[]</code> messages can be deserialized using {@link
 * JsonRowDataDeserializationSchema}.
 */
@Internal
public class JsonRowDataSerializationSchema implements SerializationSchema<RowData> {
    private static final long serialVersionUID = 1L;

    /** RowType to generate the runtime converter. */
    private final RowType rowType;

    /** The converter that converts internal data formats to JsonNode. */
    private final RowDataToJsonConverters.RowDataToJsonConverter runtimeConverter;

    /** Object mapper that is used to create output JSON objects. */
    private final ObjectMapper mapper = new ObjectMapper();

    /** Reusable object node. */
    private transient ObjectNode node;

    /** Timestamp format specification which is used to parse timestamp. */
    private final TimestampFormat timestampFormat;

    /** The handling mode when serializing null keys for map data. */
    private final JsonFormatOptions.MapNullKeyMode mapNullKeyMode;

    /** The string literal when handling mode for map null key LITERAL. */
    private final String mapNullKeyLiteral;

    /** Flag indicating whether to serialize all decimals as plain numbers. */
    private final boolean encodeDecimalAsPlainNumber;

    public JsonRowDataSerializationSchema(
            RowType rowType,
            TimestampFormat timestampFormat,
            JsonFormatOptions.MapNullKeyMode mapNullKeyMode,
            String mapNullKeyLiteral,
            boolean encodeDecimalAsPlainNumber) {
        this.rowType = rowType;
        this.timestampFormat = timestampFormat;
        this.mapNullKeyMode = mapNullKeyMode;
        this.mapNullKeyLiteral = mapNullKeyLiteral;
        this.encodeDecimalAsPlainNumber = encodeDecimalAsPlainNumber;
        this.runtimeConverter =
                new RowDataToJsonConverters(timestampFormat, mapNullKeyMode, mapNullKeyLiteral)
                        .createConverter(rowType);

        mapper.configure(
                JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, encodeDecimalAsPlainNumber);
    }

    @Override
    public byte[] serialize(RowData row) {
        if (node == null) {
            node = mapper.createObjectNode();
        }

        try {
            runtimeConverter.convert(mapper, node, row);
            return mapper.writeValueAsBytes(node);
        } catch (Throwable t) {
            throw new RuntimeException("Could not serialize row '" + row + "'. ", t);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonRowDataSerializationSchema that = (JsonRowDataSerializationSchema) o;
        return rowType.equals(that.rowType)
                && timestampFormat.equals(that.timestampFormat)
                && mapNullKeyMode.equals(that.mapNullKeyMode)
                && mapNullKeyLiteral.equals(that.mapNullKeyLiteral)
                && encodeDecimalAsPlainNumber == that.encodeDecimalAsPlainNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                rowType,
                timestampFormat,
                mapNullKeyMode,
                mapNullKeyLiteral,
                encodeDecimalAsPlainNumber);
    }
}
