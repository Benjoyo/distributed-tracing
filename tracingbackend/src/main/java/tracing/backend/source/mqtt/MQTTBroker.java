package tracing.backend.source.mqtt;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.embedded.EmbeddedHiveMQ;
import com.hivemq.embedded.EmbeddedHiveMQBuilder;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.migration.meta.PersistenceType;

import java.net.InetAddress;

/**
 * Embedded MQTT broker (server).
 */
public class MQTTBroker {

    private final EmbeddedHiveMQ hiveMQ;
    private final PublishInboundInterceptorExtension publishInboundInterceptorExtension;

    public MQTTBroker() {

        final EmbeddedHiveMQBuilder embeddedHiveMQBuilder = EmbeddedHiveMQ.builder();

        this.publishInboundInterceptorExtension = new PublishInboundInterceptorExtension();

        final EmbeddedExtension embeddedExtension = EmbeddedExtension.builder()
                .withId("embedded-ext-1")
                .withName("Embedded Extension")
                .withVersion("1.0.0")
                .withPriority(0)
                .withStartPriority(1000)
                .withAuthor("Me")
                .withExtensionMain(publishInboundInterceptorExtension) // register extension
                .build();

        embeddedHiveMQBuilder.withEmbeddedExtension(embeddedExtension);

        hiveMQ = embeddedHiveMQBuilder.build();

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE);
    }

    public void start() {
        hiveMQ.start().join();
    }

    public void setPublishListener(PublishListener publishListener) {
        this.publishInboundInterceptorExtension.setOnPublishListener(publishListener);
    }

    public void setDisconnectListener(DisconnectListener disconnectListener) {
        this.publishInboundInterceptorExtension.setDisconnectListener(disconnectListener);
    }

    public interface PublishListener {
        void onPublish(PublishPacket message, InetAddress host);
    }

    public interface DisconnectListener {
        void onDisconnect(String host);
    }
}
