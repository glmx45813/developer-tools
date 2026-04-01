let ConnectionManager = (function() {
    let stompClient = null;
    let connected = false;
    let reconnectAttempts = 0;
    let maxReconnectAttempts = 5;
    let reconnectDelay = 3000;
    let subscriptions = {};
    
    let mqttConnections = {};
    let tcpConnections = {};
    let activeMqttConnection = null;
    let activeTcpConnection = null;
    
    let messageHandlers = {
        mqtt: [],
        tcp: [],
        status: []
    };

    function connect(token) {
        if (stompClient && connected) {
            return Promise.resolve();
        }
        
        return new Promise(function(resolve, reject) {
            try {
                let socket = new SockJS(API_BASE + '/ws');
                stompClient = Stomp.over(socket);
                
                stompClient.debug = null;
                
                let headers = {};
                if (token) {
                    headers['Authorization'] = 'Bearer ' + token;
                }
                
                let sessionId = localStorage.getItem('sessionId');
                if (sessionId) {
                    headers['X-Session-Id'] = sessionId;
                }
                
                stompClient.connect(headers, function(frame) {
                    connected = true;
                    reconnectAttempts = 0;
                    console.log('WebSocket连接成功');
                    
                    subscribeToTopics();
                    loadExistingConnections();
                    
                    resolve();
                }, function(error) {
                    connected = false;
                    console.error('WebSocket连接失败:', error);
                    
                    if (reconnectAttempts < maxReconnectAttempts) {
                        reconnectAttempts++;
                        console.log('尝试重连... (' + reconnectAttempts + '/' + maxReconnectAttempts + ')');
                        setTimeout(function() {
                            connect(token);
                        }, reconnectDelay);
                    }
                    
                    reject(error);
                });
            } catch (e) {
                reject(e);
            }
        });
    }

    function disconnect() {
        if (stompClient) {
            Object.keys(subscriptions).forEach(function(key) {
                subscriptions[key].unsubscribe();
            });
            subscriptions = {};
            
            stompClient.disconnect();
            stompClient = null;
            connected = false;
        }
    }

    function subscribeToTopics() {
        if (!stompClient || !connected) return;
        
        subscriptions['mqtt'] = stompClient.subscribe('/user/topic/mqtt', function(message) {
            handleMqttMessage(JSON.parse(message.body));
        });
        
        subscriptions['tcp'] = stompClient.subscribe('/user/topic/tcp', function(message) {
            handleTcpMessage(JSON.parse(message.body));
        });
        
        subscriptions['connections'] = stompClient.subscribe('/user/topic/connections', function(message) {
            handleConnectionList(JSON.parse(message.body));
        });
    }

    function handleMqttMessage(wsMessage) {
        console.log('收到MQTT消息:', wsMessage);
        
        let connectionId = wsMessage.connectionId;
        
        if (wsMessage.type === 'MQTT_MESSAGE') {
            addMqttLog(connectionId, wsMessage.data);
            messageHandlers.mqtt.forEach(function(handler) {
                handler(connectionId, wsMessage.data);
            });
        } else if (wsMessage.type === 'MQTT_STATUS') {
            updateMqttConnectionStatus(connectionId, wsMessage.data);
            messageHandlers.status.forEach(function(handler) {
                handler('mqtt', connectionId, wsMessage.data);
            });
        }
    }

    function handleTcpMessage(wsMessage) {
        console.log('收到TCP消息:', wsMessage);
        
        let connectionId = wsMessage.connectionId;
        
        if (wsMessage.type === 'TCP_PACKET') {
            addTcpLog(connectionId, wsMessage.data);
            messageHandlers.tcp.forEach(function(handler) {
                handler(connectionId, wsMessage.data);
            });
        } else if (wsMessage.type === 'TCP_STATUS') {
            updateTcpConnectionStatus(connectionId, wsMessage.data);
            messageHandlers.status.forEach(function(handler) {
                handler('tcp', connectionId, wsMessage.data);
            });
        }
    }

    function handleConnectionList(wsMessage) {
        if (wsMessage.type === 'CONNECTION_LIST' && wsMessage.data) {
            if (wsMessage.data.mqtt) {
                wsMessage.data.mqtt.forEach(function(conn) {
                    mqttConnections[conn.clientId] = conn;
                });
            }
            if (wsMessage.data.tcp) {
                wsMessage.data.tcp.forEach(function(conn) {
                    tcpConnections[conn.connectionId] = conn;
                });
            }
            updateConnectionUI();
        }
    }

    async function loadExistingConnections() {
        try {
            let mqttConns = await apiCall(API_BASE + '/api/mqtt/connections');
            mqttConnections = {};
            if (mqttConns && Array.isArray(mqttConns)) {
                mqttConns.forEach(function(conn) {
                    mqttConnections[conn.clientId] = conn;
                });
            }
            
            let tcpConns = await apiCall(API_BASE + '/api/tcp/connections');
            tcpConnections = {};
            if (tcpConns && Array.isArray(tcpConns)) {
                tcpConns.forEach(function(conn) {
                    tcpConnections[conn.connectionId] = conn;
                });
            }
            
            updateConnectionUI();
        } catch (error) {
            console.error('加载连接列表失败:', error);
        }
    }

    function updateConnectionUI() {
        updateMqttConnectionList();
        updateTcpConnectionList();
    }

    function updateMqttConnectionList() {
        let container = document.getElementById('mqtt-connection-list');
        if (!container) return;
        
        let connections = Object.values(mqttConnections);
        
        if (connections.length === 0) {
            container.innerHTML = '<div class="text-muted small">暂无连接</div>';
            return;
        }
        
        let html = '<div class="connection-list">';
        connections.forEach(function(conn) {
            let isActive = conn.clientId === activeMqttConnection;
            let statusClass = conn.status === 'CONNECTED' ? 'status-connected' : 'status-disconnected';
            
            html += '<div class="connection-item ' + (isActive ? 'active' : '') + '" onclick="ConnectionManager.selectMqttConnection(\'' + conn.clientId + '\')">';
            html += '<span class="connection-status ' + statusClass + '"></span>';
            html += '<span class="connection-name">' + escapeHtml(conn.broker) + ':' + conn.port + '</span>';
            html += '<button class="btn btn-sm btn-outline-danger" onclick="event.stopPropagation(); ConnectionManager.disconnectMqtt(\'' + conn.clientId + '\')">';
            html += '<i class="bi bi-x"></i></button>';
            html += '</div>';
        });
        html += '</div>';
        
        container.innerHTML = html;
    }

    function updateTcpConnectionList() {
        let container = document.getElementById('tcp-connection-list');
        if (!container) return;
        
        let connections = Object.values(tcpConnections);
        
        if (connections.length === 0) {
            container.innerHTML = '<div class="text-muted small">暂无连接</div>';
            return;
        }
        
        let html = '<div class="connection-list">';
        connections.forEach(function(conn) {
            let isActive = conn.connectionId === activeTcpConnection;
            let statusClass = conn.status === 'CONNECTED' ? 'status-connected' : 'status-disconnected';
            
            html += '<div class="connection-item ' + (isActive ? 'active' : '') + '" onclick="ConnectionManager.selectTcpConnection(\'' + conn.connectionId + '\')">';
            html += '<span class="connection-status ' + statusClass + '"></span>';
            html += '<span class="connection-name">' + escapeHtml(conn.host) + ':' + conn.port + '</span>';
            html += '<button class="btn btn-sm btn-outline-danger" onclick="event.stopPropagation(); ConnectionManager.disconnectTcp(\'' + conn.connectionId + '\')">';
            html += '<i class="bi bi-x"></i></button>';
            html += '</div>';
        });
        html += '</div>';
        
        container.innerHTML = html;
    }

    function addMqttLog(connectionId, message) {
        let container = document.getElementById('mqtt-logs');
        if (!container) return;
        
        if (activeMqttConnection && connectionId !== activeMqttConnection) return;
        
        let logEntry = document.createElement('div');
        logEntry.className = 'log-entry ' + (message.direction === 'OUT' ? 'send' : 'receive');
        logEntry.innerHTML = 
            '<span class="log-time">' + (message.timestamp || new Date().toLocaleTimeString()) + '</span>' +
            '<span class="log-direction">' + (message.direction === 'OUT' ? '发送' : '接收') + '</span>' +
            '<span class="log-topic">' + escapeHtml(message.topic || '') + '</span>' +
            '<span class="log-payload">' + escapeHtml(message.payload || '') + '</span>';
        
        container.appendChild(logEntry);
        container.scrollTop = container.scrollHeight;
    }

    function addTcpLog(connectionId, packet) {
        let container = document.getElementById('tcp-logs');
        if (!container) return;
        
        if (activeTcpConnection && connectionId !== activeTcpConnection) return;
        
        let logEntry = document.createElement('div');
        logEntry.className = 'log-entry ' + (packet.direction === 'SEND' ? 'send' : 'receive');
        logEntry.innerHTML = 
            '<div class="log-header">' +
            '<span class="log-time">' + (packet.timestamp || new Date().toLocaleTimeString()) + '</span>' +
            '<span class="log-direction">' + (packet.direction === 'SEND' ? '发送' : '接收') + '</span>' +
            '</div>' +
            '<span class="log-payload">' + escapeHtml(packet.data || '') + '</span>' +
            (packet.hexData ? '<span class="log-hex">HEX: ' + escapeHtml(packet.hexData) + '</span>' : '');
        
        container.appendChild(logEntry);
        container.scrollTop = container.scrollHeight;
    }

    function updateMqttConnectionStatus(connectionId, status) {
        if (mqttConnections[connectionId]) {
            mqttConnections[connectionId].status = status;
            updateMqttConnectionList();
        }
    }

    function updateTcpConnectionStatus(connectionId, status) {
        if (tcpConnections[connectionId]) {
            tcpConnections[connectionId].status = status;
            updateTcpConnectionList();
        }
    }

    async function connectMqtt(request) {
        try {
            let result = await apiCall(API_BASE + '/api/mqtt/connect', {
                method: 'POST',
                body: JSON.stringify(request)
            });
            
            mqttConnections[result.clientId] = result;
            activeMqttConnection = result.clientId;
            updateMqttConnectionList();
            
            return result;
        } catch (error) {
            throw error;
        }
    }

    async function disconnectMqtt(connectionId) {
        try {
            await apiCall(API_BASE + '/api/mqtt/disconnect/' + connectionId, { method: 'POST' });
            delete mqttConnections[connectionId];
            
            if (activeMqttConnection === connectionId) {
                activeMqttConnection = null;
                let topicsContainer = document.getElementById('mqtt-subscribed-topics');
                if (topicsContainer) {
                    topicsContainer.innerHTML = '<small class="text-muted">暂无订阅</small>';
                }
                let logsContainer = document.getElementById('mqtt-logs');
                if (logsContainer) {
                    logsContainer.innerHTML = '';
                }
            }
            
            updateMqttConnectionList();
        } catch (error) {
            throw error;
        }
    }

    function selectMqttConnection(connectionId) {
        activeMqttConnection = connectionId;
        updateMqttConnectionList();
        
        let conn = mqttConnections[connectionId];
        if (conn) {
            document.getElementById('mqtt-broker').value = conn.broker || '';
            document.getElementById('mqtt-port').value = conn.port || 1883;
        }
        
        loadMqttLogs(connectionId);
        refreshMqttSubscribedTopics(connectionId);
    }

    async function loadMqttLogs(connectionId) {
        try {
            let logs = await apiCall(API_BASE + '/api/mqtt/logs/' + connectionId);
            let container = document.getElementById('mqtt-logs');
            if (!container) return;
            
            container.innerHTML = '';
            logs.forEach(function(log) {
                let logEntry = document.createElement('div');
                logEntry.className = 'log-entry ' + (log.direction === 'OUT' ? 'send' : 'receive');
                logEntry.innerHTML = 
                    '<span class="log-time">' + log.timestamp + '</span>' +
                    '<span class="log-direction">' + (log.direction === 'OUT' ? '发送' : '接收') + '</span>' +
                    '<span class="log-topic">' + escapeHtml(log.topic) + '</span>' +
                    '<span class="log-payload">' + escapeHtml(log.payload) + '</span>';
                container.appendChild(logEntry);
            });
            container.scrollTop = container.scrollHeight;
        } catch (error) {
            console.error('加载MQTT日志失败:', error);
        }
    }

    async function refreshMqttSubscribedTopics(connectionId) {
        if (!connectionId) return;
        
        try {
            let status = await apiCall(API_BASE + '/api/mqtt/status/' + connectionId);
            let container = document.getElementById('mqtt-subscribed-topics');
            if (!container) return;
            
            let topics = status.subscribedTopics || [];
            
            if (topics.length === 0) {
                container.innerHTML = '<small class="text-muted">暂无订阅</small>';
            } else {
                container.innerHTML = topics.map(function(topic) {
                    return '<div class="subscribed-topic-item">' +
                        '<span class="topic-name">' + escapeHtml(topic) + '</span>' +
                        '<button class="btn btn-sm btn-outline-danger" onclick="mqttUnsubscribe(\'' + escapeHtml(topic) + '\')">' +
                        '<i class="bi bi-x"></i></button></div>';
                }).join('');
            }
        } catch (error) {
            console.error('刷新订阅列表失败', error);
        }
    }

    async function connectTcp(request) {
        try {
            let result = await apiCall(API_BASE + '/api/tcp/connect', {
                method: 'POST',
                body: JSON.stringify(request)
            });
            
            tcpConnections[result.connectionId] = result;
            activeTcpConnection = result.connectionId;
            updateTcpConnectionList();
            
            return result;
        } catch (error) {
            throw error;
        }
    }

    async function disconnectTcp(connectionId) {
        try {
            await apiCall(API_BASE + '/api/tcp/disconnect/' + connectionId, { method: 'POST' });
            delete tcpConnections[connectionId];
            
            if (activeTcpConnection === connectionId) {
                activeTcpConnection = null;
                let logsContainer = document.getElementById('tcp-logs');
                if (logsContainer) {
                    logsContainer.innerHTML = '';
                }
            }
            
            updateTcpConnectionList();
        } catch (error) {
            throw error;
        }
    }

    function selectTcpConnection(connectionId) {
        activeTcpConnection = connectionId;
        updateTcpConnectionList();
        
        let conn = tcpConnections[connectionId];
        if (conn) {
            document.getElementById('tcp-host').value = conn.host || '';
            document.getElementById('tcp-port').value = conn.port || '';
        }
        
        loadTcpLogs(connectionId);
    }

    async function loadTcpLogs(connectionId) {
        try {
            let status = await apiCall(API_BASE + '/api/tcp/status/' + connectionId);
            let container = document.getElementById('tcp-logs');
            if (!container || !status.packetLogs) return;
            
            container.innerHTML = '';
            status.packetLogs.forEach(function(log) {
                let logEntry = document.createElement('div');
                logEntry.className = 'log-entry ' + (log.direction === 'SEND' ? 'send' : 'receive');
                logEntry.innerHTML = 
                    '<div class="log-header">' +
                    '<span class="log-time">' + log.timestamp + '</span>' +
                    '<span class="log-direction">' + (log.direction === 'SEND' ? '发送' : '接收') + '</span>' +
                    '</div>' +
                    '<span class="log-payload">' + escapeHtml(log.data) + '</span>' +
                    (log.hexData ? '<span class="log-hex">HEX: ' + escapeHtml(log.hexData) + '</span>' : '');
                container.appendChild(logEntry);
            });
            container.scrollTop = container.scrollHeight;
        } catch (error) {
            console.error('加载TCP日志失败:', error);
        }
    }

    function getActiveMqttConnection() {
        return activeMqttConnection;
    }

    function getActiveTcpConnection() {
        return activeTcpConnection;
    }

    function onMqttMessage(handler) {
        messageHandlers.mqtt.push(handler);
    }

    function onTcpMessage(handler) {
        messageHandlers.tcp.push(handler);
    }

    function onStatusChange(handler) {
        messageHandlers.status.push(handler);
    }

    function isConnected() {
        return connected;
    }

    return {
        connect: connect,
        disconnect: disconnect,
        isConnected: isConnected,
        connectMqtt: connectMqtt,
        disconnectMqtt: disconnectMqtt,
        selectMqttConnection: selectMqttConnection,
        getActiveMqttConnection: getActiveMqttConnection,
        connectTcp: connectTcp,
        disconnectTcp: disconnectTcp,
        selectTcpConnection: selectTcpConnection,
        getActiveTcpConnection: getActiveTcpConnection,
        loadExistingConnections: loadExistingConnections,
        onMqttMessage: onMqttMessage,
        onTcpMessage: onTcpMessage,
        onStatusChange: onStatusChange
    };
})();

function escapeHtml(text) {
    if (!text) return '';
    let div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
