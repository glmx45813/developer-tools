package com.glmx.tools.service;

import com.glmx.tools.common.BusinessException;
import com.glmx.tools.common.ResultCode;
import com.glmx.tools.dto.MqttConnectRequest;
import com.glmx.tools.dto.MqttConnectionStatus;
import com.glmx.tools.dto.MqttPublishRequest;
import com.glmx.tools.dto.MqttSubscribeRequest;
import com.glmx.tools.dto.MqttMessage;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MqttService {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final Map<String, MqttAsyncClient> clients = new ConcurrentHashMap<>();
    private final Map<String, MqttConnectionStatus> connectionStatuses = new ConcurrentHashMap<>();
    private final Map<String, List<MqttMessage>> messageLogs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> subscribedTopics = new ConcurrentHashMap<>();
    private final Map<String, String> connectionOwner = new ConcurrentHashMap<>();

    private static final int MAX_LOG_SIZE = 100;
    private static final String DEFAULT_WS_PATH = "/mqtt";

    @Autowired
    private WebSocketNotificationService notificationService;

    @PreDestroy
    public void destroy() {
        for (String connectionId : new ArrayList<>(clients.keySet())) {
            try {
                disconnect(connectionId);
            } catch (Exception e) {
                log.warn("关闭连接失败: {}", connectionId, e);
            }
        }
    }

    public MqttConnectionStatus connect(String username, MqttConnectRequest request) {
        String mqttClientId = request.getClientId();
        if (mqttClientId == null || mqttClientId.isEmpty()) {
            mqttClientId = UUID.randomUUID().toString().substring(0, 8);
        }
        
        String connectionId = generateConnectionId(username, mqttClientId);

        if (clients.containsKey(connectionId)) {
            try {
                MqttAsyncClient existingClient = clients.get(connectionId);
                if (existingClient.isConnected()) {
                    existingClient.disconnect().waitForCompletion();
                }
                existingClient.close();
            } catch (MqttException e) {
                log.warn("关闭现有连接失败: {}", e.getMessage());
            }
            clients.remove(connectionId);
        }

        try {
            String brokerUrl = buildBrokerUrl(request);
            log.info("正在连接MQTT服务器: {}, user={}, connectionId={}", brokerUrl, username, connectionId);
            
            MqttAsyncClient client = new MqttAsyncClient(brokerUrl, mqttClientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(request.getCleanSession());
            options.setConnectionTimeout(request.getConnectionTimeout());
            options.setKeepAliveInterval(request.getKeepAliveInterval());
            options.setAutomaticReconnect(request.getAutoReconnect());
            options.setMaxReconnectDelay(request.getMaxReconnectDelay() * 1000);
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

            if (request.getUsername() != null && !request.getUsername().isEmpty()) {
                options.setUserName(request.getUsername());
            }
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                options.setPassword(request.getPassword().toCharArray());
            }

            String protocol = determineProtocol(request);
            boolean isSecure = "ssl".equalsIgnoreCase(protocol) || "wss".equalsIgnoreCase(protocol);
            
            if (isSecure) {
                configureSsl(options, request);
            }

            if (request.getWillTopic() != null && !request.getWillTopic().isEmpty()) {
                options.setWill(request.getWillTopic(), 
                    request.getWillMessage() != null ? request.getWillMessage().getBytes() : new byte[0],
                    request.getWillQos(), 
                    Boolean.TRUE.equals(request.getWillRetained()));
            }

            final String finalProtocol = protocol;
            final String finalUsername = username;
            final String finalConnectionId = connectionId;
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        log.info("MQTT重连成功: connectionId={}, serverURI={}", finalConnectionId, serverURI);
                        updateConnectionStatus(finalConnectionId, "CONNECTED");
                        incrementReconnectCount(finalConnectionId);
                        resubscribeTopics(finalConnectionId);
                        notificationService.sendMqttStatus(finalUsername, finalConnectionId, "CONNECTED");
                    } else {
                        log.info("MQTT连接成功: connectionId={}, serverURI={}", finalConnectionId, serverURI);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT连接丢失: connectionId={}, cause={}", finalConnectionId, cause.getMessage());
                    updateConnectionStatus(finalConnectionId, "DISCONNECTED", cause.getMessage());
                    notificationService.sendMqttStatus(finalUsername, finalConnectionId, "DISCONNECTED");
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                    String payload = new String(message.getPayload());
                    log.info("收到MQTT消息: connectionId={}, topic={}, payload={}", finalConnectionId, topic, payload);
                    MqttMessage msg = addMessageLog(finalConnectionId, topic, payload, message.getQos(), message.isRetained(), "IN");
                    incrementReceivedCount(finalConnectionId);
                    notificationService.sendMqttMessage(finalUsername, finalConnectionId, msg);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    log.debug("消息投递完成: connectionId={}, messageId={}", finalConnectionId, token.getMessageId());
                }
            });

            IMqttToken token = client.connect(options);
            token.waitForCompletion();

            clients.put(connectionId, client);
            subscribedTopics.put(connectionId, ConcurrentHashMap.newKeySet());
            messageLogs.put(connectionId, new CopyOnWriteArrayList<>());
            connectionOwner.put(connectionId, username);

            MqttConnectionStatus status = new MqttConnectionStatus();
            status.setClientId(connectionId);
            status.setBroker(request.getBroker());
            status.setPort(request.getPort());
            status.setProtocol(finalProtocol);
            status.setStatus("CONNECTED");
            status.setConnectedAt(LocalDateTime.now());
            status.setSubscribedTopics(new ArrayList<>());
            status.setSslEnabled(isSecure);
            status.setAutoReconnect(request.getAutoReconnect());
            status.setReconnectCount(0);
            
            MqttConnectionStatus.MessageStats stats = new MqttConnectionStatus.MessageStats();
            stats.setSentCount(0L);
            stats.setReceivedCount(0L);
            status.setMessageStats(stats);

            connectionStatuses.put(connectionId, status);

            return status;

        } catch (MqttException e) {
            log.error("MQTT连接失败: reasonCode={}, message={}", e.getReasonCode(), e.getMessage());
            String errorMsg = translateMqttException(e);
            throw new BusinessException(ResultCode.MQTT_CONNECT_ERROR.getCode(), errorMsg);
        } catch (Exception e) {
            log.error("MQTT连接配置失败", e);
            throw new BusinessException(ResultCode.MQTT_CONNECT_ERROR.getCode(), "MQTT连接配置失败: " + e.getMessage());
        }
    }

    private String generateConnectionId(String username, String baseClientId) {
        return username + "_" + baseClientId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void disconnect(String username, String connectionId) {
        validateOwnership(username, connectionId);
        disconnect(connectionId);
    }

    private void disconnect(String connectionId) {
        MqttAsyncClient client = clients.remove(connectionId);
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect().waitForCompletion();
                }
                client.close();
            } catch (MqttException e) {
                log.warn("断开MQTT连接失败: {}", e.getMessage());
            } finally {
                MqttConnectionStatus status = connectionStatuses.remove(connectionId);
                if (status != null) {
                    status.setStatus("DISCONNECTED");
                    status.setDisconnectedAt(LocalDateTime.now());
                }
                subscribedTopics.remove(connectionId);
                messageLogs.remove(connectionId);
                connectionOwner.remove(connectionId);
            }
        }
    }

    public void publish(String username, MqttPublishRequest request) {
        validateOwnership(username, request.getClientId());
        
        MqttAsyncClient client = clients.get(request.getClientId());
        if (client == null || !client.isConnected()) {
            throw new BusinessException("MQTT客户端未连接");
        }

        try {
            org.eclipse.paho.client.mqttv3.MqttMessage message = new org.eclipse.paho.client.mqttv3.MqttMessage(request.getMessage().getBytes());
            message.setQos(request.getQos());
            message.setRetained(request.getRetained());

            client.publish(request.getTopic(), message).waitForCompletion();

            MqttMessage msg = addMessageLog(request.getClientId(), request.getTopic(), request.getMessage(), request.getQos(), request.getRetained(), "OUT");
            incrementSentCount(request.getClientId());
            notificationService.sendMqttMessage(username, request.getClientId(), msg);

        } catch (MqttException e) {
            log.error("MQTT消息发布失败", e);
            throw new BusinessException(ResultCode.MQTT_PUBLISH_ERROR.getCode(), "消息发布失败: " + e.getMessage());
        }
    }

    public void subscribe(String username, MqttSubscribeRequest request) {
        validateOwnership(username, request.getClientId());
        
        MqttAsyncClient client = clients.get(request.getClientId());
        if (client == null) {
            throw new BusinessException("MQTT客户端不存在，请先连接");
        }
        
        if (!client.isConnected()) {
            MqttConnectionStatus status = connectionStatuses.get(request.getClientId());
            boolean autoReconnect = status != null && Boolean.TRUE.equals(status.getAutoReconnect());
            
            if (autoReconnect) {
                log.info("客户端未连接，等待自动重连: connectionId={}", request.getClientId());
                boolean connected = waitForReconnect(request.getClientId(), 5000);
                if (!connected) {
                    throw new BusinessException("连接已断开，正在重连中，请稍后重试");
                }
            } else {
                throw new BusinessException("MQTT客户端未连接");
            }
        }

        try {
            Set<String> topics = subscribedTopics.get(request.getClientId());
            
            if (request.getTopics() != null && !request.getTopics().isEmpty()) {
                for (String topicItem : request.getTopics()) {
                    String topic;
                    int qos = request.getQos();
                    
                    if (topicItem.contains(":")) {
                        String[] parts = topicItem.split(":", 2);
                        topic = parts[0];
                        if (parts.length > 1) {
                            try {
                                qos = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                log.warn("无效的QoS值: {}, 使用默认值: {}", parts[1], qos);
                            }
                        }
                    } else {
                        topic = topicItem;
                    }
                    
                    client.subscribe(topic, qos).waitForCompletion();
                    topics.add(topic);
                    log.info("订阅主题成功: connectionId={}, topic={}, qos={}", request.getClientId(), topic, qos);
                }

                MqttConnectionStatus status = connectionStatuses.get(request.getClientId());
                if (status != null) {
                    status.setSubscribedTopics(new ArrayList<>(topics));
                }

            }

        } catch (MqttException e) {
            log.error("MQTT订阅失败", e);
            throw new BusinessException("订阅失败: " + e.getMessage());
        }
    }

    public void unsubscribe(String username, String connectionId, String topic) {
        validateOwnership(username, connectionId);
        
        MqttAsyncClient client = clients.get(connectionId);
        if (client == null || !client.isConnected()) {
            throw new BusinessException("MQTT客户端未连接");
        }

        try {
            client.unsubscribe(topic).waitForCompletion();

            Set<String> topics = subscribedTopics.get(connectionId);
            if (topics != null) {
                topics.remove(topic);
            }

            MqttConnectionStatus status = connectionStatuses.get(connectionId);
            if (status != null) {
                status.setSubscribedTopics(new ArrayList<>(topics));
            }
            
            log.info("取消订阅成功: connectionId={}, topic={}", connectionId, topic);

        } catch (MqttException e) {
            log.error("MQTT取消订阅失败", e);
            throw new BusinessException("取消订阅失败: " + e.getMessage());
        }
    }

    public void unsubscribeMultiple(String username, String connectionId, List<String> topics) {
        validateOwnership(username, connectionId);
        
        MqttAsyncClient client = clients.get(connectionId);
        if (client == null || !client.isConnected()) {
            throw new BusinessException("MQTT客户端未连接");
        }

        try {
            String[] topicArray = topics.toArray(new String[0]);
            client.unsubscribe(topicArray).waitForCompletion();

            Set<String> subscribedTopicSet = subscribedTopics.get(connectionId);
            if (subscribedTopicSet != null) {
                subscribedTopicSet.removeAll(topics);
            }

            MqttConnectionStatus status = connectionStatuses.get(connectionId);
            if (status != null) {
                status.setSubscribedTopics(new ArrayList<>(subscribedTopicSet));
            }
            
            log.info("批量取消订阅成功: connectionId={}, topics={}", connectionId, topics);

        } catch (MqttException e) {
            log.error("MQTT批量取消订阅失败", e);
            throw new BusinessException("批量取消订阅失败: " + e.getMessage());
        }
    }

    public MqttConnectionStatus getStatus(String username, String connectionId) {
        validateOwnership(username, connectionId);
        return getStatus(connectionId);
    }

    private MqttConnectionStatus getStatus(String connectionId) {
        MqttConnectionStatus status = connectionStatuses.get(connectionId);
        if (status != null) {
            MqttAsyncClient client = clients.get(connectionId);
            if (client != null) {
                status.setStatus(client.isConnected() ? "CONNECTED" : "DISCONNECTED");
            }
        }
        return status;
    }

    public List<MqttMessage> getMessageLogs(String username, String connectionId) {
        validateOwnership(username, connectionId);
        return messageLogs.getOrDefault(connectionId, new ArrayList<>());
    }

    public void clearMessageLogs(String username, String connectionId) {
        validateOwnership(username, connectionId);
        List<MqttMessage> logs = messageLogs.get(connectionId);
        if (logs != null) {
            logs.clear();
        }
    }

    public List<MqttConnectionStatus> getUserConnections(String username) {
        List<MqttConnectionStatus> result = new ArrayList<>();
        for (String connectionId : connectionStatuses.keySet()) {
            String owner = connectionOwner.get(connectionId);
            if (username.equals(owner)) {
                result.add(getStatus(connectionId));
            }
        }
        return result;
    }

    private void validateOwnership(String username, String connectionId) {
        String owner = connectionOwner.get(connectionId);
        if (owner == null) {
            throw new BusinessException("连接不存在");
        }
        if (!username.equals(owner)) {
            throw new BusinessException("无权访问此连接");
        }
    }

    private String buildBrokerUrl(MqttConnectRequest request) {
        String protocol = determineProtocol(request);
        String broker = request.getBroker();
        int port = request.getPort();
        
        if (port <= 0) {
            port = getDefaultPort(protocol);
        }
        
        if ("ws".equalsIgnoreCase(protocol) || "wss".equalsIgnoreCase(protocol)) {
            String path = extractWebSocketPath(broker);
            broker = extractHost(broker);
            return String.format("%s://%s:%d%s", protocol, broker, port, path);
        } else {
            broker = extractHost(broker);
            return String.format("%s://%s:%d", protocol, broker, port);
        }
    }

    private String extractHost(String broker) {
        if (broker == null || broker.isEmpty()) {
            return broker;
        }
        
        try {
            if (broker.contains("://")) {
                URI uri = new URI(broker);
                return uri.getHost();
            }
            
            if (broker.contains("/")) {
                return broker.substring(0, broker.indexOf("/"));
            }
            
            if (broker.contains(":") && !broker.startsWith("[")) {
                int lastColon = broker.lastIndexOf(":");
                boolean isPort = true;
                for (int i = lastColon + 1; i < broker.length(); i++) {
                    if (!Character.isDigit(broker.charAt(i))) {
                        isPort = false;
                        break;
                    }
                }
                if (isPort) {
                    return broker.substring(0, lastColon);
                }
            }
            
            return broker;
        } catch (URISyntaxException e) {
            log.warn("解析broker地址失败: {}", e.getMessage());
            return broker;
        }
    }

    private String extractWebSocketPath(String broker) {
        if (broker == null || broker.isEmpty()) {
            return DEFAULT_WS_PATH;
        }
        
        try {
            if (broker.contains("://")) {
                URI uri = new URI(broker);
                String path = uri.getPath();
                return (path != null && !path.isEmpty()) ? path : DEFAULT_WS_PATH;
            }
            
            if (broker.contains("/")) {
                return broker.substring(broker.indexOf("/"));
            }
            
            return DEFAULT_WS_PATH;
        } catch (URISyntaxException e) {
            return DEFAULT_WS_PATH;
        }
    }

    private int getDefaultPort(String protocol) {
        switch (protocol.toLowerCase()) {
            case "ssl":
            case "wss":
                return 8883;
            case "ws":
                return 8083;
            case "tcp":
            default:
                return 1883;
        }
    }

    private String determineProtocol(MqttConnectRequest request) {
        String protocol = request.getProtocol();
        int port = request.getPort();
        
        String inferredProtocol = null;
        if (port > 0) {
            if (port == 8883) {
                inferredProtocol = "ssl";
            } else if (port == 8083) {
                inferredProtocol = "ws";
            } else if (port == 8084 || port == 443) {
                inferredProtocol = "wss";
            }
        }
        
        if (inferredProtocol != null) {
            if (protocol != null && !protocol.isEmpty() && !protocol.equalsIgnoreCase(inferredProtocol)) {
                log.warn("协议与端口不匹配: 指定protocol={}, 端口{}建议使用protocol={}，已自动修正", 
                    protocol, port, inferredProtocol);
            }
            return inferredProtocol;
        }
        
        if (protocol != null && !protocol.isEmpty()) {
            return protocol.toLowerCase();
        }
        
        if (Boolean.TRUE.equals(request.getSslEnabled())) {
            return "ssl";
        }
        
        return "tcp";
    }

    private void configureSsl(MqttConnectOptions options, MqttConnectRequest request) throws Exception {
        if (request.getSslCertificatePath() != null && !request.getSslCertificatePath().isEmpty()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(request.getSslCertificatePath())) {
                char[] password = request.getSslCertificatePassword() != null 
                    ? request.getSslCertificatePassword().toCharArray() 
                    : new char[0];
                keyStore.load(fis, password);
            }
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{createTrustManager(keyStore)}, null);
            options.setSocketFactory(sslContext.getSocketFactory());
        } else {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{createTrustAllManager()}, null);
            options.setSocketFactory(sslContext.getSocketFactory());
        }
    }

    private TrustManager createTrustManager(KeyStore keyStore) throws Exception {
        return new X509TrustManager() {
            private final X509TrustManager defaultTrustManager = createDefaultTrustManager(keyStore);
            
            private X509TrustManager createDefaultTrustManager(KeyStore ks) throws Exception {
                javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                return (X509TrustManager) tmf.getTrustManagers()[0];
            }
            
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                defaultTrustManager.checkClientTrusted(chain, authType);
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                try {
                    defaultTrustManager.checkServerTrusted(chain, authType);
                } catch (java.security.cert.CertificateException e) {
                    log.warn("证书验证失败，但允许连接: {}", e.getMessage());
                }
            }
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return defaultTrustManager.getAcceptedIssuers();
            }
        };
    }

    private TrustManager createTrustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    private String translateMqttException(MqttException e) {
        switch (e.getReasonCode()) {
            case MqttException.REASON_CODE_CONNECTION_LOST:
                return "连接丢失，请检查网络或服务器状态";
            case MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION:
                return "MQTT协议版本不兼容";
            case MqttException.REASON_CODE_INVALID_CLIENT_ID:
                return "客户端ID无效或已被使用";
            case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                return "MQTT服务器不可用";
            case MqttException.REASON_CODE_NOT_AUTHORIZED:
                return "认证失败，请检查用户名和密码";
            case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                return "连接超时，请检查服务器地址和端口";
            case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                return "服务器连接错误，请确认协议类型正确(TCP:1883, SSL:8883)";
            default:
                return String.format("MQTT连接失败(错误码:%d): %s", e.getReasonCode(), e.getMessage());
        }
    }

    private void resubscribeTopics(String connectionId) {
        Set<String> topics = subscribedTopics.get(connectionId);
        MqttAsyncClient client = clients.get(connectionId);
        
        if (topics != null && client != null && client.isConnected()) {
            for (String topic : topics) {
                try {
                    client.subscribe(topic, 0).waitForCompletion();
                    log.info("重新订阅主题成功: connectionId={}, topic={}", connectionId, topic);
                } catch (MqttException e) {
                    log.warn("重新订阅主题失败: connectionId={}, topic={}, error={}", connectionId, topic, e.getMessage());
                }
            }
        }
    }

    private boolean waitForReconnect(String connectionId, long timeoutMs) {
        MqttAsyncClient client = clients.get(connectionId);
        if (client == null) return false;
        
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (client.isConnected()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return client.isConnected();
    }

    private void updateConnectionStatus(String connectionId, String status) {
        updateConnectionStatus(connectionId, status, null);
    }

    private void updateConnectionStatus(String connectionId, String status, String error) {
        MqttConnectionStatus connectionStatus = connectionStatuses.get(connectionId);
        if (connectionStatus != null) {
            connectionStatus.setStatus(status);
            if (error != null) {
                connectionStatus.setLastError(error);
            }
            if ("DISCONNECTED".equals(status)) {
                connectionStatus.setDisconnectedAt(LocalDateTime.now());
            }
        }
    }

    private void incrementReconnectCount(String connectionId) {
        MqttConnectionStatus status = connectionStatuses.get(connectionId);
        if (status != null) {
            int count = status.getReconnectCount() != null ? status.getReconnectCount() : 0;
            status.setReconnectCount(count + 1);
        }
    }

    private MqttMessage addMessageLog(String connectionId, String topic, String payload, int qos, boolean retained, String direction) {
        List<MqttMessage> logs = messageLogs.get(connectionId);
        MqttMessage message = new MqttMessage();
        message.setId(UUID.randomUUID().toString().substring(0, 8));
        message.setTopic(topic);
        message.setPayload(payload);
        message.setQos(qos);
        message.setRetained(retained);
        message.setTimestamp(LocalDateTime.now());
        message.setDirection(direction);

        if (logs != null) {
            logs.add(message);

            while (logs.size() > MAX_LOG_SIZE) {
                logs.remove(0);
            }
        }
        return message;
    }

    private void incrementSentCount(String connectionId) {
        MqttConnectionStatus status = connectionStatuses.get(connectionId);
        if (status != null && status.getMessageStats() != null) {
            status.getMessageStats().setSentCount(status.getMessageStats().getSentCount() + 1);
        }
    }

    private void incrementReceivedCount(String connectionId) {
        MqttConnectionStatus status = connectionStatuses.get(connectionId);
        if (status != null && status.getMessageStats() != null) {
            status.getMessageStats().setReceivedCount(status.getMessageStats().getReceivedCount() + 1);
        }
    }
}
