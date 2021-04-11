package tracing.backend.source.mqtt;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;

/**
 * Extension for broker to intercept all inbound messages (easier than explicitly subscribing to all topics)
 */
public class PublishInboundInterceptorExtension implements ExtensionMain {

    private MQTTBroker.PublishListener publishListener;
    private MQTTBroker.DisconnectListener disconnectListener;

    public void setOnPublishListener(MQTTBroker.PublishListener publishListener) {
        this.publishListener = publishListener;
    }

    @Override
    public void extensionStart(@NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput) {
        final PublishInboundInterceptor publishInboundInterceptor = (publishInboundInput, publishInboundOutput) -> {
            try {
                var packet = publishInboundInput.getPublishPacket();
                var host = publishInboundInput.getConnectionInformation().getInetAddress();

                if (this.publishListener != null) {
                    this.publishListener.onPublish(packet, host.orElse(null));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        final DisconnectInboundInterceptor disconnectInboundInterceptor = (publishInboundInput, publishInboundOutput) -> {
            try {
                var host = publishInboundInput.getConnectionInformation().getInetAddress();

                if (this.disconnectListener != null && host.isPresent()) {
                    this.disconnectListener.onDisconnect(host.get().getHostName());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // create a new client initializer
        final ClientInitializer clientInitializer = (initializerInput, clientContext) -> {
            // add the interceptor to the context of the connecting client
            clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
            clientContext.addDisconnectInboundInterceptor(disconnectInboundInterceptor);
        };

        //register the client initializer
        Services.initializerRegistry().setClientInitializer(clientInitializer);
    }

    @Override
    public void extensionStop(@NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput) { }

    public void setDisconnectListener(MQTTBroker.DisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
    }
}