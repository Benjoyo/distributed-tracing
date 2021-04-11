package tracing.transport.mqtt;

import com.google.gson.Gson;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAckReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import tracing.Config;
import tracing.metrics.Metrics;
import tracing.transport.TracePacket;
import tracing.transport.TraceStartListener;
import tracing.transport.TraceTransport;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

/**
 * Transport implementation that uses the MQTT protocol.
 */
public class MQTTTransport implements TraceTransport {

    private static final String TRACE_TOPIC = "trace";
    private static final String READY_TOPIC = "ready";
    private static final String CONFIG_TOPIC = "config";
    private static final int RECONNECT_DELAY = 2000;

    private Mqtt5AsyncClient mqttClient;
    private final Gson gson = new Gson();

    private final String deviceName;
    private final String serverHost;
    private final BlockingQueue<TracePacket> traceQueue;
    private final TraceStartListener traceStartListener;

    public MQTTTransport(BlockingQueue<TracePacket> traceQueue, String deviceName, String serverHost, TraceStartListener traceStartListener) {
        this.traceQueue = traceQueue;
        this.traceStartListener = Objects.requireNonNull(traceStartListener);
        this.deviceName = deviceName;
        this.serverHost = serverHost;
    }

    @Override
    public void run() {
        // MQTT client to connect to broker/server
        this.mqttClient = MqttClient.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost(serverHost)
                .useMqttVersion5()
                .buildAsync();

        // subscribe to the config topic for this device to receive server config
        this.mqttClient.subscribeWith()
                .topicFilter(CONFIG_TOPIC + "/"  + deviceName)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(this::onConfigReceive)
                .send();

        // connect to server
        this.connect();
    }

    /**
     * Connect to server and send ready message.
     */
    private void connect() {
        System.out.println("Try connecting to MQTT broker...");

        var mqtt5Connect = Mqtt5Connect.builder()
                .cleanStart(false) // persistent storage of messages in case of connection loss
                .noSessionExpiry()
                .build();

        try {
            var res = this.mqttClient.toBlocking().connect(mqtt5Connect);
            if (res.getReasonCode() != Mqtt5ConnAckReasonCode.SUCCESS) {
                Thread.sleep(RECONNECT_DELAY);
                this.connect();
            }

            System.out.println("Connected to MQTT broker.");

            this.mqttClient.publishWith().topic(READY_TOPIC + "/"  + deviceName)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                Thread.sleep(RECONNECT_DELAY);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            this.connect();
        }
    }

    /**
     * Called when server config is received.
     * @param publish contains the config message
     */
    private void onConfigReceive(Mqtt5Publish publish) {
        publish.getPayload().ifPresent(byteBuffer -> {
            var payload = StandardCharsets.UTF_8.decode(byteBuffer).toString();
            var config = this.gson.fromJson(payload, Config.class);

            System.out.println("[Config] Received config.");

            config.getWatchpoints().forEach(wp -> {
                System.out.println("[Config] Watchpoint: " + String.format("%08x", wp.getAddress()) + ", " + wp.getSize() + " byte");
            });

            Metrics.registerQueue(traceQueue);

            // notify that we can start tracing, as we got the config
            this.traceStartListener.onTraceStarted(config);

            // start consuming and sending trace packets
            this.consumeTracePackets();
        });
    }

    /**
     * Takes packets from the queue populated by the source and sends them to the server.
     */
    private void consumeTracePackets() {
        try {
            while (true) {
                var tracePacket = this.traceQueue.take();
                Metrics.markOutputEvent();
                Metrics.updateQueueSize(this.traceQueue.size());
                this.sendTracePacket(tracePacket);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sends a packet to the server.
     * @param tracePacket the packet to send
     */
    private void sendTracePacket(TracePacket tracePacket) {
        var payload = this.gson.toJson(tracePacket);

        this.mqttClient.publishWith().topic(TRACE_TOPIC + "/" + deviceName)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(payload.getBytes())
                .send();
    }

    @Override
    public void close() {
        this.mqttClient.disconnect();
    }
}
