package tracing.backend.output.elk;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import tracing.backend.output.TraceOutput;
import tracing.backend.trace.TraceEvent;
import tracing.backend.trace.TracePacket;
import tracing.backend.trace.Transient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Connects to an ElasticSearch database and creates an index for the raw and processed trace.
 */
public class ElasticsearchOutput implements TraceOutput {

    public static final int BULK_ACTIONS = 500;
    public static final String RAW_TRACE_INDEX = "raw_trace";
    public static final String TRACE_INDEX = "trace";

    // whether raw events should be put into a raw_trace index
    private final boolean enableRawTrace;

    // global sequence number of output events
    private long globalSeq = 0;

    private RestHighLevelClient client;
    private BulkProcessor bulkProcessor;

    /**
     * Creates an ElasticsearchOutput that connects to a local ELK instance.
     * @param enableRawTrace whether raw events should be put into a raw_trace index
     */
    public ElasticsearchOutput(boolean enableRawTrace) {
        this.enableRawTrace = enableRawTrace;
    }

    @Override
    public void init() {
        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")
                )
        );

        var listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (response.hasFailures()) {
                    System.out.println("[Elastic] afterBulk failed: " + response.status().toString());
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                failure.printStackTrace();
            }
        };

        this.bulkProcessor = BulkProcessor.builder((request, bulkListener) -> this.client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener)
                .setBulkActions(BULK_ACTIONS)
                .setConcurrentRequests(1)
                .build();
    }

    @Override
    public void put(TraceEvent traceEvent) {
        // ignore events marked as transient
        if (traceEvent instanceof Transient) return;

        Map<String, Object> jsonMap = new HashMap<>();
        traceEvent.serialize(jsonMap);

        jsonMap.put("seq", globalSeq++);

        // add map of event fields to bulk processor
        var req = new IndexRequest(TRACE_INDEX)
                .source(jsonMap);
        this.bulkProcessor.add(req);
    }

    @Override
    public void putRaw(TracePacket tracePacket) {
        if (!enableRawTrace) return;

        var req = new IndexRequest(RAW_TRACE_INDEX)
                .source(Map.of("raw", tracePacket.toString()));
        this.bulkProcessor.add(req);
    }

    @Override
    public void close() {
        try {
            this.client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.bulkProcessor.close();
    }
}
