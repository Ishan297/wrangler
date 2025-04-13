package io.cdap.wrangler.executor.directive;

import io.cdap.wrangler.api.*;
import io.cdap.wrangler.api.parser.*;
import io.cdap.wrangler.api.row.Row;
import io.cdap.wrangler.api.executor.ExecutorContext;
import io.cdap.wrangler.api.executor.Store;
import io.cdap.wrangler.api.annotation.*;

import java.util.*;

/**
 * Directive to aggregate stats using ByteSize and TimeDuration tokens.
 */
@DirectiveInfo(
        name = "aggregate-stats",
        usage = "aggregate-stats <sizeCol> <timeCol> <outputSizeCol> <outputTimeCol> [<unit=sizeUnit>] [<type=avg|total>]",
        description = "Aggregates total/average of byte size and time duration columns."
)
public class AggregateStats implements Directive {

    private String sizeCol;
    private String timeCol;
    private String outputSizeCol;
    private String outputTimeCol;
    private String unit = "MB";      // default unit
    private String type = "total";   // default aggregation type

    @Override
    public UsageDefinition define() {
        return UsageDefinition.builder()
                .addRequiredArg("sizeCol", TokenType.COLUMN_NAME)
                .addRequiredArg("timeCol", TokenType.COLUMN_NAME)
                .addRequiredArg("outputSizeCol", TokenType.COLUMN_NAME)
                .addRequiredArg("outputTimeCol", TokenType.COLUMN_NAME)
                .addOptionalArg("unit", TokenType.STRING)
                .addOptionalArg("type", TokenType.STRING)
                .build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        sizeCol = ((ColumnName) args.value("sizeCol")).value();
        timeCol = ((ColumnName) args.value("timeCol")).value();
        outputSizeCol = ((ColumnName) args.value("outputSizeCol")).value();
        outputTimeCol = ((ColumnName) args.value("outputTimeCol")).value();

        if (args.contains("unit")) {
            unit = ((Text) args.value("unit")).value().toUpperCase();
        }

        if (args.contains("type")) {
            type = ((Text) args.value("type")).value().toLowerCase();
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        Store store = context.getStore("aggregate-stats");

        long totalBytes = store.getOrDefault("totalBytes", 0L);
        long totalTimeMs = store.getOrDefault("totalTimeMs", 0L);
        int count = store.getOrDefault("rowCount", 0);

        for (Row row : rows) {
            Object sizeVal = row.getValue(sizeCol);
            Object timeVal = row.getValue(timeCol);

            if (sizeVal instanceof String && timeVal instanceof String) {
                ByteSize size = new ByteSize((String) sizeVal);
                TimeDuration time = new TimeDuration((String) timeVal);
                totalBytes += size.getBytes();
                totalTimeMs += time.getMilliseconds();
                count++;
            }
        }

        store.set("totalBytes", totalBytes);
        store.set("totalTimeMs", totalTimeMs);
        store.set("rowCount", count);

        return new ArrayList<>();
    }

    @Override
    public List<Row> finalize(List<Row> rows, ExecutorContext context) {
        Store store = context.getStore("aggregate-stats");

        long totalBytes = store.getOrDefault("totalBytes", 0L);
        long totalTimeMs = store.getOrDefault("totalTimeMs", 0L);
        int count = store.getOrDefault("rowCount", 1);

        double sizeOutput;
        double timeOutput;

        // Convert size
        switch (unit) {
            case "KB":
                sizeOutput = totalBytes / 1024.0;
                break;
            case "GB":
                sizeOutput = totalBytes / (1024.0 * 1024 * 1024);
                break;
            case "MB":
            default:
                sizeOutput = totalBytes / (1024.0 * 1024);
        }

        // Convert time
        switch (unit) {
            case "S":
                timeOutput = totalTimeMs / 1000.0;
                break;
            case "MIN":
                timeOutput = totalTimeMs / (60.0 * 1000);
                break;
            default:
                timeOutput = totalTimeMs;
        }

        if (type.equals("avg")) {
            sizeOutput /= count;
            timeOutput /= count;
        }

        Row result = new Row();
        result.add(outputSizeCol, sizeOutput);
        result.add(outputTimeCol, timeOutput);

        return Collections.singletonList(result);
    }
}
