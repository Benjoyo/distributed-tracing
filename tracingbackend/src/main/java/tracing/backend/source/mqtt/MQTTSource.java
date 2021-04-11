package tracing.backend.source.mqtt;

import com.google.gson.Gson;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import tracing.backend.Target;
import tracing.backend.configuration.JsonTargetProvider;
import tracing.backend.configuration.TargetProvider;
import tracing.backend.metrics.Metrics;
import tracing.backend.output.TraceOutput;
import tracing.backend.source.TraceSource;
import tracing.backend.trace.TraceEvent;
import tracing.backend.trace.TracePacket;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * TraceSource that communicates with observers using MQTT.
 * Uses a JSON configuration file tracing_config.json to provide target definitions.
 */
public class MQTTSource implements TraceSource {

    public static final String READY_TOPIC = "ready";
    private static final String TRACE_TOPIC = "trace";

    private final MQTTBroker mqttBroker = new MQTTBroker();
    private Mqtt5AsyncClient mqttClient;
    private final Gson gson = new Gson();

    private final JsonTargetProvider configLoader;
    private TraceOutput traceOutput;

    public MQTTSource(TraceOutput traceOutput) {
        this.traceOutput = traceOutput;
        // load json config
        this.configLoader = JsonTargetProvider.getInstance();
    }

    @Override
    public void init() {

        // start broker
        this.mqttBroker.setPublishListener(this::onPublish);
        this.mqttBroker.setDisconnectListener(this::onDisconnect);
        this.mqttBroker.start();

        // start client to later send configs to observers
        this.mqttClient = MqttClient.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost("localhost")
                .useMqttVersion5()
                .buildAsync();

        this.mqttClient.toBlocking().connect();

        System.out.println("Waiting for " + configLoader.getTargetConfigs().size() + " targets to connect...");
    }

    /**
     * Sends the client configs to observers.
     */
    private void sendClientConfig() {
        System.out.println("Sending configs to targets: " + configLoader.getTargetConfigs().toString());

        this.configLoader.getTargetConfigs().forEach(targetConfig -> {
            this.mqttClient.publishWith().topic("config/" + targetConfig.getName())
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .retain(false)
                    .payload(gson.toJson(targetConfig).getBytes())
                    .send();
        });

        Metrics.startReport();
    }

    /**
     * Called when a MQTT message is received.
     * @param message the message
     * @param host host address of observer
     */
    private void onPublish(PublishPacket message, InetAddress host) {
        if (message.getTopic().isEmpty() || !message.getTopic().contains("/")) {
            return;
        }
        var topic = message.getTopic().split("/");

        if (topic[0].equals(TRACE_TOPIC)) { // trace event
            var targetId = topic[1];
            if (message.getPayload().isPresent()) {

                var data = StandardCharsets.UTF_8.decode(message.getPayload().get()).toString();
                var tracePacket = gson.fromJson(data, TracePacket.class);

                // put out raw trace packet for debugging etc.
                this.traceOutput.putRaw(tracePacket);

                // get target definition
                var target = this.configLoader.getTarget(targetId);
                // parse trace event from packet
                var traceEvent = TraceEvent.from(target, tracePacket);

                Metrics.markInputEvent(targetId, traceEvent.uuid);
                Metrics.updateQueueSize(targetId, target.getTraceQueue().size());

                // add trace event to output queue
                target.getTraceQueue().add(traceEvent);
            }

        } else if (topic[0].equals(READY_TOPIC)) { // ready notification
            var targetId = topic[1];
            if (this.configLoader.hasTarget(targetId)) {
                var d = this.configLoader.getTarget(targetId);

                Metrics.registerInputQueue(targetId, d.getTraceQueue());

                d.setReady(true);
                if (host != null) {
                    d.setHost(host.getHostName());
                }
            }
            var readyCnt = this.configLoader.getTargets().stream().filter(Target::isReady).count();
            if (readyCnt == this.configLoader.getTargets().size()) {
                System.out.println("All targets are ready, go!");
                sendClientConfig();
            } else {
                System.out.println(readyCnt + " targets are ready, waiting for " + (this.configLoader.getTargets().size() - readyCnt) + " more...");
            }
        } else if (message.getTopic().equals("info")) {
            System.err.println("ITM_OVERFLOW");
        }
    }

    /**
     * Called when observer diconnected.
     * @param host host of disconnected observer
     */
    private void onDisconnect(String host) {
        this.configLoader.getTargets().stream().filter(device -> device.getHost().equals(host)).findAny().ifPresent(target -> {
            target.setReady(false);
            System.out.println("Target " + target.getTargetId() + " disconnected.");
        });
    }

    @Override
    public void close() {
        this.mqttClient.disconnect();
    }

    @Override
    public TargetProvider getTargetProvider() {
        return this.configLoader;
    }
}
