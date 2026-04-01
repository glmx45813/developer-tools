// 动态获取API基础路径，支持域名+前缀部署
// 如果模板中已通过 Thymeleaf 注入 API_BASE，则使用注入的值
// 否则从 URL 路径推断 context-path
function getContextPath() {
    const pathname = window.location.pathname;
    const parts = pathname.split('/').filter(Boolean);
    if (parts.length > 0) {
        return '/' + parts[0];
    }
    return '';
}

if (typeof API_BASE === 'undefined') {
    var API_BASE = getContextPath();
}
let currentTool = 'ai-chat';

document.addEventListener('DOMContentLoaded', function() {
    try {
        initNavigation();
    } catch (e) {
        console.error('initNavigation error:', e);
    }
    try {
        initQuickActions();
    } catch (e) {
        console.error('initQuickActions error:', e);
    }
    try {
        initTabs();
    } catch (e) {
        console.error('initTabs error:', e);
    }
    try {
        loadRegexTemplates();
    } catch (e) {
        console.error('loadRegexTemplates error:', e);
    }
    try {
        initDateTimePicker();
    } catch (e) {
        console.error('initDateTimePicker error:', e);
    }
    try {
        initQrCodeColor();
    } catch (e) {
        console.error('initQrCodeColor error:', e);
    }
    try {
        initWebSocket();
    } catch (e) {
        console.error('initWebSocket error:', e);
    }
});

function initWebSocket() {
    let token = localStorage.getItem('token');
    if (typeof ConnectionManager !== 'undefined') {
        ConnectionManager.connect(token).then(function() {
            console.log('ConnectionManager WebSocket连接成功');
        }).catch(function(e) {
            console.warn('WebSocket连接失败，消息推送将不可用:', e);
        });
    }
}

let mqttLogPollingInterval = null;
let tcpLogPollingInterval = null;

function startMqttLogPolling(connectionId) {
    if (mqttLogPollingInterval) {
        clearInterval(mqttLogPollingInterval);
    }
    
    if (!connectionId) return;
    
    mqttLogPollingInterval = setInterval(async function() {
        let connId = connectionId;
        if (typeof ConnectionManager !== 'undefined' && ConnectionManager.getActiveMqttConnection()) {
            connId = ConnectionManager.getActiveMqttConnection();
        }
        
        if (!connId) return;
        
        try {
            let logs = await apiCall(API_BASE + '/api/mqtt/logs/' + connId);
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
    }, 2000);
}

function startTcpLogPolling(connectionId) {
    if (tcpLogPollingInterval) {
        clearInterval(tcpLogPollingInterval);
    }
    
    if (!connectionId) return;
    
    tcpLogPollingInterval = setInterval(async function() {
        let connId = connectionId;
        if (typeof ConnectionManager !== 'undefined' && ConnectionManager.getActiveTcpConnection()) {
            connId = ConnectionManager.getActiveTcpConnection();
        }
        
        if (!connId) return;
        
        try {
            let status = await apiCall(API_BASE + '/api/tcp/status/' + connId);
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
    }, 1000);
}

function initNavigation() {
    const navLinks = document.querySelectorAll('.sidebar .nav-link');
    if (!navLinks || navLinks.length === 0) {
        console.warn('No navigation links found');
        return;
    }
    navLinks.forEach(function(link) {
        link.addEventListener('click', function(e) {
            const tool = this.dataset.tool;
            const href = this.getAttribute('href');
            
            if (href && href !== '#' && !href.startsWith('javascript')) {
                return;
            }
            
            if (!tool) {
                return;
            }
            
            e.preventDefault();
            
            navLinks.forEach(function(l) {
                if (l && l.classList) {
                    l.classList.remove('active');
                }
            });
            if (this.classList) {
                this.classList.add('active');
            }
            
            document.querySelectorAll('.tool-panel').forEach(function(panel) {
                if (panel && panel.classList) {
                    panel.classList.remove('active');
                }
            });
            
            const targetPanel = document.getElementById('panel-' + tool);
            if (targetPanel && targetPanel.classList) {
                targetPanel.classList.add('active');
            }
            
            currentTool = tool;
            loadToolHistory(tool);
            
            if (window.innerWidth < 992) {
                closeSidebar();
            }
        });
    });
}

function initQuickActions() {
    const quickBtns = document.querySelectorAll('.quick-action-btn');
    quickBtns.forEach(function(btn) {
        btn.addEventListener('click', function(e) {
            const tool = this.dataset.tool;
            if (!tool) {
                return;
            }
            
            e.preventDefault();
            
            const navLink = document.querySelector('.nav-link[data-tool="' + tool + '"]');
            if (navLink) {
                navLink.click();
            }
        });
    });
}

function toggleSection(element) {
    const section = element.closest('.nav-section');
    if (section) {
        section.classList.toggle('collapsed');
    }
}

function filterTools(keyword) {
    keyword = keyword.toLowerCase().trim();
    const sections = document.querySelectorAll('.nav-section');
    
    sections.forEach(function(section) {
        const items = section.querySelectorAll('.nav-item');
        let hasVisibleItem = false;
        
        items.forEach(function(item) {
            const link = item.querySelector('.nav-link');
            if (link) {
                const text = link.textContent.toLowerCase();
                const toolName = link.dataset.tool ? link.dataset.tool.toLowerCase() : '';
                
                if (keyword === '' || text.includes(keyword) || toolName.includes(keyword)) {
                    item.style.display = '';
                    hasVisibleItem = true;
                } else {
                    item.style.display = 'none';
                }
            }
        });
        
        if (keyword === '' || hasVisibleItem) {
            section.style.display = '';
            if (keyword !== '' && hasVisibleItem) {
                section.classList.remove('collapsed');
            }
        } else {
            section.style.display = 'none';
        }
    });
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.querySelector('.sidebar-overlay');
    
    if (sidebar) {
        sidebar.classList.toggle('show');
    }
    if (overlay) {
        overlay.classList.toggle('show');
    }
}

function closeSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.querySelector('.sidebar-overlay');
    
    if (sidebar) {
        sidebar.classList.remove('show');
    }
    if (overlay) {
        overlay.classList.remove('show');
    }
}

function toggleSidebarCollapse() {
    const sidebar = document.getElementById('sidebar');
    const mainContent = document.querySelector('.main-content');
    const collapseBtn = document.querySelector('.sidebar-collapse-btn');
    
    if (sidebar) {
        sidebar.classList.toggle('collapsed');
        if (sidebar.classList.contains('collapsed')) {
            sidebar.style.width = 'var(--sidebar-collapsed-width)';
            if (mainContent) {
                mainContent.style.marginLeft = 'var(--sidebar-collapsed-width)';
            }
        } else {
            sidebar.style.width = '';
            if (mainContent) {
                mainContent.style.marginLeft = '';
            }
        }
    }
}

function initGlobalShortcuts() {
    document.addEventListener('keydown', function(e) {
        if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
            e.preventDefault();
            const searchInput = document.getElementById('tool-search');
            if (searchInput) {
                searchInput.focus();
            }
        }
        
        if (e.key === 'Escape') {
            const sidebar = document.getElementById('sidebar');
            if (sidebar && sidebar.classList.contains('show')) {
                closeSidebar();
            }
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    try {
        initGlobalShortcuts();
    } catch (e) {
        console.error('initGlobalShortcuts error:', e);
    }
});

function initTabs() {
    const tabs = document.querySelectorAll('.nav-tabs .nav-link');
    if (!tabs || tabs.length === 0) return;
    
    tabs.forEach(function(tab) {
        tab.addEventListener('click', function(e) {
            e.preventDefault();
        });
    });
}

function initDateTimePicker() {
    const picker = document.getElementById('datetime-picker');
    if (picker) {
        const now = new Date();
        const offset = now.getTimezoneOffset() * 60000;
        const localISOTime = new Date(now - offset).toISOString().slice(0, 19);
        picker.value = localISOTime;
    }
}

function initQrCodeColor() {
    const colorPicker = document.getElementById('qrcode-fg');
    const hexInput = document.getElementById('qrcode-fg-hex');
    if (colorPicker && hexInput) {
        colorPicker.addEventListener('input', function() {
            hexInput.value = this.value;
        });
    }
}

function getSessionId() {
    let sessionId = localStorage.getItem('sessionId');
    if (!sessionId) {
        sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        localStorage.setItem('sessionId', sessionId);
    }
    return sessionId;
}

async function apiCall(url, options = {}) {
    const token = localStorage.getItem('token');
    const sessionId = getSessionId();
    const headers = {
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId,
        ...(options.headers || {})
    };
    
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    const defaultOptions = {
        headers: headers
    };
    
    const response = await fetch(url, { ...defaultOptions, ...options, headers: headers });
    const data = await response.json();
    
    if (data.code !== 200) {
        throw new Error(data.message || '请求失败');
    }
    
    return data.data;
}

function showToast(message, type = 'success') {
    const toastContainer = document.querySelector('.toast-container') || createToastContainer();
    
    const toast = document.createElement('div');
    toast.className = 'toast show align-items-center text-white bg-' + (type === 'success' ? 'success' : 'danger') + ' border-0';
    toast.innerHTML = '<div class="d-flex"><div class="toast-body">' + message + '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" onclick="this.parentElement.parentElement.remove()"></button></div>';
    
    toastContainer.appendChild(toast);
    setTimeout(function() { toast.remove(); }, 3000);
}

window.appShowToast = showToast;

function createToastContainer() {
    const container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
    return container;
}

function copyToClipboard(elementId) {
    const element = document.getElementById(elementId);
    if (!element) return;
    
    const text = element.value || element.textContent;
    
    navigator.clipboard.writeText(text).then(function() {
        showToast('已复制到剪贴板');
    }).catch(function() {
        showToast('复制失败', 'error');
    });
}

// History Management
function saveHistory(tool, action, data) {
    const key = 'toolHistory_' + tool;
    let history = [];
    try {
        history = JSON.parse(localStorage.getItem(key) || '[]');
    } catch (e) {
        history = [];
    }
    
    history.unshift({
        action: action,
        data: data,
        time: new Date().toISOString()
    });
    
    if (history.length > 20) {
        history = history.slice(0, 20);
    }
    
    localStorage.setItem(key, JSON.stringify(history));
    loadToolHistory(tool);
}

function loadToolHistory(tool) {
    const container = document.getElementById(tool + '-history');
    if (!container) return;
    
    const key = 'toolHistory_' + tool;
    let history = [];
    try {
        history = JSON.parse(localStorage.getItem(key) || '[]');
    } catch (e) {
        history = [];
    }
    
    if (history.length === 0) {
        container.innerHTML = '';
        return;
    }
    
    let html = '<div class="tool-history-header">' +
        '<h6><i class="bi bi-clock-history"></i> 最近操作</h6>' +
        '<button class="btn btn-sm btn-outline-danger" onclick="clearToolHistory(\'' + tool + '\')">清除</button>' +
        '</div>' +
        '<div class="tool-history-list">';
    
    history.slice(0, 5).forEach(function(item, index) {
        const itemData = JSON.stringify(item).replace(/"/g, '&quot;');
        html += '<div class="history-item fade-in" onclick="showHistoryDetail(' + itemData + ')" style="cursor: pointer;">' +
            '<div class="history-item-info">' +
            '<p>' + escapeHtml(item.action) + '</p>' +
            '</div>' +
            '<div class="history-item-time">' + formatTime(item.time) + '</div>' +
            '</div>';
    });
    
    html += '</div>';
    container.innerHTML = html;
}

function clearToolHistory(tool) {
    const key = 'toolHistory_' + tool;
    localStorage.removeItem(key);
    loadToolHistory(tool);
    showToast('历史记录已清除');
}

function showHistoryDetail(item) {
    const modal = new bootstrap.Modal(document.getElementById('historyDetailModal'));
    
    document.getElementById('detail-time').textContent = formatTime(item.time);
    document.getElementById('detail-action').textContent = item.action;
    
    const inputSection = document.getElementById('detail-input-section');
    const outputSection = document.getElementById('detail-output-section');
    const inputEl = document.getElementById('detail-input');
    const outputEl = document.getElementById('detail-output');
    
    if (item.data && item.data.input) {
        inputSection.style.display = 'block';
        inputEl.textContent = typeof item.data.input === 'object' 
            ? JSON.stringify(item.data.input, null, 2) 
            : item.data.input;
    } else {
        inputSection.style.display = 'none';
    }
    
    if (item.data && item.data.output) {
        outputSection.style.display = 'block';
        outputEl.textContent = typeof item.data.output === 'object' 
            ? JSON.stringify(item.data.output, null, 2) 
            : item.data.output;
    } else if (item.data) {
        outputSection.style.display = 'block';
        outputEl.textContent = typeof item.data === 'object' 
            ? JSON.stringify(item.data, null, 2) 
            : item.data;
    } else {
        outputSection.style.display = 'none';
    }
    
    const copyBtn = document.getElementById('copyDetailBtn');
    copyBtn.onclick = function() {
        const textToCopy = outputEl.textContent || item.action;
        navigator.clipboard.writeText(textToCopy).then(function() {
            showToast('已复制到剪贴板');
        }).catch(function() {
            showToast('复制失败', 'error');
        });
    };
    
    modal.show();
}

function formatTime(isoString) {
    const date = new Date(isoString);
    return date.toLocaleString('zh-CN');
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// MD5工具
async function md5Encrypt() {
    const textEl = document.getElementById('md5-input');
    if (!textEl) return;
    
    const text = textEl.value;
    if (!text) {
        showToast('请输入要加密的文本', 'error');
        return;
    }
    
    const uppercaseEl = document.getElementById('md5-uppercase');
    const uppercase = uppercaseEl ? uppercaseEl.checked : false;
    
    try {
        const result = await apiCall(API_BASE + '/api/md5/encrypt', {
            method: 'POST',
            body: JSON.stringify({ text: text, type: 'STANDARD', uppercase: uppercase })
        });
        
        const result32El = document.getElementById('md5-result-32');
        const result16El = document.getElementById('md5-result-16');
        const resultEl = document.getElementById('md5-result');
        
        if (result32El) result32El.value = result.md5Standard || '';
        if (result16El) result16El.value = result.md5Bit16 || '';
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('md5', '加密: ' + text.substring(0, 20) + '...', { input: text, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function md5EncryptFile() {
    const fileInput = document.getElementById('md5-file-input');
    if (!fileInput || !fileInput.files[0]) {
        showToast('请选择文件', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    
    try {
        const response = await fetch(API_BASE + '/api/md5/encrypt-file', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const result32El = document.getElementById('md5-result-32');
        const result16El = document.getElementById('md5-result-16');
        const resultEl = document.getElementById('md5-result');
        
        if (result32El) result32El.value = data.data.md5Standard || '';
        if (result16El) result16El.value = data.data.md5Bit16 || '';
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('md5', '文件加密: ' + fileInput.files[0].name, { input: fileInput.files[0].name, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// Base64图片工具
async function imageToBase64() {
    const fileInput = document.getElementById('base64-image-input');
    if (!fileInput || !fileInput.files[0]) {
        showToast('请选择图片', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    
    try {
        const response = await fetch(API_BASE + '/api/base64/image-to-base64', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const outputEl = document.getElementById('base64-output');
        const dataurlEl = document.getElementById('base64-dataurl');
        const previewEl = document.getElementById('base64-preview-img');
        const infoEl = document.getElementById('base64-image-info');
        const resultEl = document.getElementById('base64-encode-result');
        
        if (outputEl) outputEl.value = data.data.base64 || '';
        if (dataurlEl) dataurlEl.value = data.data.dataUrl || '';
        if (previewEl) previewEl.src = data.data.dataUrl || '';
        if (infoEl) infoEl.innerHTML = '文件: ' + data.data.filename + '<br>格式: ' + data.data.format + '<br>大小: ' + formatBytes(data.data.size);
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('base64', '图片转Base64: ' + fileInput.files[0].name, { input: fileInput.files[0].name, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function base64ToImage() {
    const inputEl = document.getElementById('base64-input');
    if (!inputEl) return;
    
    const base64 = inputEl.value;
    if (!base64) {
        showToast('请输入Base64编码', 'error');
        return;
    }
    
    try {
        const result = await apiCall(API_BASE + '/api/base64/base64-to-image-info', {
            method: 'POST',
            body: JSON.stringify({ base64: base64 })
        });
        
        const imgEl = document.getElementById('base64-decoded-img');
        const formatEl = document.getElementById('base64-format');
        const dimensionEl = document.getElementById('base64-dimension');
        const sizeEl = document.getElementById('base64-size');
        const resultEl = document.getElementById('base64-decode-result');
        
        const imgSrc = base64.startsWith('data:') ? base64 : 'data:image/png;base64,' + base64;
        
        if (imgEl) imgEl.src = imgSrc;
        if (formatEl) formatEl.textContent = result.format || 'png';
        if (dimensionEl) dimensionEl.textContent = result.width + ' x ' + result.height;
        if (sizeEl) sizeEl.textContent = formatBytes(Math.ceil(base64.length * 0.75));
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('base64', 'Base64转图片', { input: base64, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function downloadBase64Image() {
    const inputEl = document.getElementById('base64-input');
    if (!inputEl) return;
    
    const base64 = inputEl.value;
    if (!base64) {
        showToast('请输入Base64编码', 'error');
        return;
    }
    
    let pureBase64 = base64;
    let format = 'png';
    
    if (base64.contains(',')) {
        const parts = base64.split(',');
        const header = parts[0];
        pureBase64 = parts[1];
        
        if (header.contains('image/')) {
            const start = header.indexOf('image/') + 6;
            const end = header.indexOf(';', start);
            if (end > start) {
                format = header.substring(start, end);
            }
        }
    }
    
    const byteCharacters = atob(pureBase64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    
    const blob = new Blob([byteArray], { type: 'image/' + format });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = 'image.' + format;
    a.click();
    
    URL.revokeObjectURL(url);
    showToast('图片已下载');
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// URL编解码工具
async function urlEncode() {
    const inputEl = document.getElementById('url-input');
    if (!inputEl) return;
    
    const text = inputEl.value;
    if (!text) {
        showToast('请输入要编码的文本', 'error');
        return;
    }
    
    const charsetEl = document.getElementById('url-charset');
    const charset = charsetEl ? charsetEl.value : 'UTF-8';
    
    try {
        const result = await apiCall(API_BASE + '/api/url/encode', {
            method: 'POST',
            body: JSON.stringify({ text: text, charset: charset })
        });
        
        const encodedEl = document.getElementById('url-encoded');
        const decodedEl = document.getElementById('url-decoded');
        const resultEl = document.getElementById('url-result');
        
        if (encodedEl) encodedEl.value = result.encoded || '';
        if (decodedEl) decodedEl.value = '';
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('url', 'URL编码', { input: text, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function urlDecode() {
    const inputEl = document.getElementById('url-input');
    if (!inputEl) return;
    
    const text = inputEl.value;
    if (!text) {
        showToast('请输入要解码的文本', 'error');
        return;
    }
    
    const charsetEl = document.getElementById('url-charset');
    const charset = charsetEl ? charsetEl.value : 'UTF-8';
    
    try {
        const result = await apiCall(API_BASE + '/api/url/decode', {
            method: 'POST',
            body: JSON.stringify({ text: text, charset: charset })
        });
        
        const encodedEl = document.getElementById('url-encoded');
        const decodedEl = document.getElementById('url-decoded');
        const resultEl = document.getElementById('url-result');
        
        if (encodedEl) encodedEl.value = '';
        if (decodedEl) decodedEl.value = result.decoded || '';
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('url', 'URL解码', { input: text, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// 二维码工具
async function generateQrCode() {
    const contentEl = document.getElementById('qrcode-content');
    if (!contentEl) return;
    
    const content = contentEl.value;
    if (!content) {
        showToast('请输入二维码内容', 'error');
        return;
    }
    
    const sizeEl = document.getElementById('qrcode-size');
    const levelEl = document.getElementById('qrcode-level');
    const fgEl = document.getElementById('qrcode-fg');
    
    const size = sizeEl ? parseInt(sizeEl.value) : 200;
    const errorCorrectionLevel = levelEl ? levelEl.value : 'M';
    const foregroundColor = fgEl ? fgEl.value.replace('#', '') : '000000';
    
    try {
        const result = await apiCall(API_BASE + '/api/qrcode/generate', {
            method: 'POST',
            body: JSON.stringify({
                content: content,
                size: size,
                errorCorrectionLevel: errorCorrectionLevel,
                foregroundColor: foregroundColor,
                backgroundColor: 'FFFFFF'
            })
        });
        
        const imgEl = document.getElementById('qrcode-img');
        const resultEl = document.getElementById('qrcode-result');
        
        if (imgEl) imgEl.src = result.dataUrl;
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('qrcode', '生成二维码: ' + content.substring(0, 20) + '...', { input: content, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function downloadQrCode() {
    const imgEl = document.getElementById('qrcode-img');
    if (!imgEl || !imgEl.src) {
        showToast('请先生成二维码', 'error');
        return;
    }
    
    const a = document.createElement('a');
    a.href = imgEl.src;
    a.download = 'qrcode.png';
    a.click();
    
    showToast('二维码已下载');
}

async function parseQrCode() {
    const fileInput = document.getElementById('qrcode-file-input');
    if (!fileInput || !fileInput.files[0]) {
        showToast('请选择二维码图片', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    
    try {
        const response = await fetch(API_BASE + '/api/qrcode/parse', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const contentEl = document.getElementById('qrcode-parsed-content');
        const resultEl = document.getElementById('qrcode-parse-result');
        
        if (contentEl) contentEl.value = data.data.contents.join('\n');
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('qrcode', '识别二维码', { input: '图片文件', output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// 时间戳工具
async function getCurrentTimestamp() {
    try {
        const result = await apiCall(API_BASE + '/api/timestamp/now');
        const inputEl = document.getElementById('timestamp-input');
        if (inputEl) inputEl.value = result.timestampMs;
        displayTimestampResult(result);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function timestampToDatetime() {
    const inputEl = document.getElementById('timestamp-input');
    if (!inputEl) return;
    
    const timestamp = inputEl.value;
    if (!timestamp) {
        showToast('请输入时间戳', 'error');
        return;
    }
    
    try {
        const result = await apiCall(API_BASE + '/api/timestamp/from-timestamp?timestamp=' + timestamp);
        displayTimestampResult(result);
        saveHistory('timestamp', '时间戳转日期', { input: timestamp, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function datetimeToTimestamp() {
    const pickerEl = document.getElementById('datetime-picker');
    const inputEl = document.getElementById('datetime-input');
    
    let datetime = '';
    if (inputEl && inputEl.value) {
        datetime = inputEl.value;
    } else if (pickerEl && pickerEl.value) {
        const date = new Date(pickerEl.value);
        datetime = date.getFullYear() + '-' + 
            String(date.getMonth() + 1).padStart(2, '0') + '-' + 
            String(date.getDate()).padStart(2, '0') + ' ' + 
            String(date.getHours()).padStart(2, '0') + ':' + 
            String(date.getMinutes()).padStart(2, '0') + ':' + 
            String(date.getSeconds()).padStart(2, '0');
    }
    
    if (!datetime) {
        showToast('请选择或输入日期时间', 'error');
        return;
    }
    
    try {
        const result = await apiCall(API_BASE + '/api/timestamp/from-datetime', {
            method: 'POST',
            body: JSON.stringify({ datetime: datetime, pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8' })
        });
        displayTimestampResult(result);
        saveHistory('timestamp', '日期转时间戳', { input: datetime, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function displayTimestampResult(result) {
    const msEl = document.getElementById('result-timestamp-ms');
    const secEl = document.getElementById('result-timestamp-sec');
    const datetimeEl = document.getElementById('result-datetime');
    const weekdayEl = document.getElementById('result-weekday');
    const isoEl = document.getElementById('result-iso');
    const resultEl = document.getElementById('timestamp-result');
    
    if (msEl) msEl.textContent = result.timestampMs;
    if (secEl) secEl.textContent = result.timestampSec;
    if (datetimeEl) datetimeEl.textContent = result.formatted;
    if (weekdayEl) weekdayEl.textContent = result.weekday || '-';
    if (isoEl) isoEl.value = result.iso8601 || '';
    if (resultEl) resultEl.style.display = 'block';
}

// 随机数工具
async function generateRandom() {
    const lengthEl = document.getElementById('random-length');
    const typeEl = document.getElementById('random-type');
    const countEl = document.getElementById('random-count');
    const upperEl = document.getElementById('random-uppercase');
    const lowerEl = document.getElementById('random-lowercase');
    const numberEl = document.getElementById('random-number');
    const specialEl = document.getElementById('random-special');
    const excludeEl = document.getElementById('random-exclude');
    
    const length = lengthEl ? parseInt(lengthEl.value) : 16;
    const type = typeEl ? typeEl.value : 'MIXED';
    const count = countEl ? parseInt(countEl.value) : 1;
    const includeUppercase = upperEl ? upperEl.checked : true;
    const includeLowercase = lowerEl ? lowerEl.checked : true;
    const includeNumber = numberEl ? numberEl.checked : true;
    const includeSpecial = specialEl ? specialEl.checked : false;
    const excludeChars = excludeEl ? excludeEl.value : '';
    
    try {
        const result = await apiCall(API_BASE + '/api/random/generate', {
            method: 'POST',
            body: JSON.stringify({
                length: length,
                type: type,
                count: count,
                includeUppercase: includeUppercase,
                includeLowercase: includeLowercase,
                includeNumber: includeNumber,
                includeSpecial: includeSpecial,
                excludeChars: excludeChars
            })
        });
        
        const container = document.getElementById('random-values');
        const resultEl = document.getElementById('random-result');
        
        if (container) {
            container.innerHTML = result.values.map(function(v) {
                return '<div class="random-value-item"><code>' + v + '</code>' +
                    '<button class="btn btn-sm btn-outline-primary" onclick="navigator.clipboard.writeText(\'' + v + '\').then(function() { showToast(\'已复制\'); })">' +
                    '<i class="bi bi-clipboard"></i></button></div>';
            }).join('');
        }
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('random', '生成' + count + '个随机数', { input: '长度:' + length + ', 数量:' + count, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function generateUuid() {
    const countEl = document.getElementById('uuid-count');
    const count = countEl ? parseInt(countEl.value) : 1;
    
    try {
        const result = await apiCall(API_BASE + '/api/random/uuid');
        const container = document.getElementById('random-values');
        const resultEl = document.getElementById('random-result');
        
        if (container) {
            let html = '<div class="random-value-item"><code>' + result.uuid + '</code>' +
                '<button class="btn btn-sm btn-outline-primary" onclick="navigator.clipboard.writeText(\'' + result.uuid + '\').then(function() { showToast(\'已复制\'); })">' +
                '<i class="bi bi-clipboard"></i></button></div>' +
                '<div class="random-value-item"><code>' + result.uuidNoDash + '</code>' +
                '<button class="btn btn-sm btn-outline-primary" onclick="navigator.clipboard.writeText(\'' + result.uuidNoDash + '\').then(function() { showToast(\'已复制\'); })">' +
                '<i class="bi bi-clipboard"></i></button></div>';
            container.innerHTML = html;
        }
        if (resultEl) resultEl.style.display = 'block';
        
        saveHistory('random', '生成UUID', { input: 'UUID', output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// JSON工具
async function jsonFormat() {
    const inputEl = document.getElementById('json-input');
    if (!inputEl) return;
    
    const json = inputEl.value;
    if (!json) {
        showToast('请输入JSON内容', 'error');
        return;
    }
    
    try {
        const result = await apiCall(API_BASE + '/api/json/format', {
            method: 'POST',
            body: JSON.stringify({ json: json })
        });
        
        if (!result.valid) {
            showJsonError(result);
            return;
        }
        
        const outputEl = document.getElementById('json-output');
        const statsEl = document.getElementById('json-stats');
        const resultEl = document.getElementById('json-result');
        const errorEl = document.getElementById('json-error');
        
        if (outputEl) outputEl.value = result.result;
        if (statsEl) statsEl.textContent = '原始: ' + result.originalSize + ' 字节 → 格式化后: ' + result.resultSize + ' 字节';
        if (resultEl) resultEl.style.display = 'block';
        if (errorEl) errorEl.style.display = 'none';
        
        saveHistory('json', 'JSON格式化', { input: json, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jsonCompress() {
    const inputEl = document.getElementById('json-input');
    if (!inputEl) return;
    
    const json = inputEl.value;
    if (!json) {
        showToast('请输入JSON内容', 'error');
        return;
    }
    
    try {
        const result = await apiCall(API_BASE + '/api/json/compress', {
            method: 'POST',
            body: JSON.stringify({ json: json })
        });
        
        if (!result.valid) {
            showJsonError(result);
            return;
        }
        
        const outputEl = document.getElementById('json-output');
        const statsEl = document.getElementById('json-stats');
        const resultEl = document.getElementById('json-result');
        const errorEl = document.getElementById('json-error');
        
        if (outputEl) outputEl.value = result.result;
        if (statsEl) statsEl.textContent = '原始: ' + result.originalSize + ' 字节 → 压缩后: ' + result.resultSize + ' 字节 (压缩率: ' + result.compressionRatio + ')';
        if (resultEl) resultEl.style.display = 'block';
        if (errorEl) errorEl.style.display = 'none';
        
        saveHistory('json', 'JSON压缩', { input: json, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jsonValidate() {
    const inputEl = document.getElementById('json-input');
    if (!inputEl) return;
    
    const json = inputEl.value;
    if (!json) {
        showToast('请输入JSON内容', 'error');
        return;
    }
    
    try {
        const result = await apiCall(API_BASE + '/api/json/validate', {
            method: 'POST',
            body: JSON.stringify({ json: json })
        });
        
        if (result.valid) {
            showToast('JSON格式有效');
        } else {
            showJsonError(result);
        }
        
        saveHistory('json', 'JSON校验', { input: json, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function showJsonError(result) {
    const errorEl = document.getElementById('json-error');
    const resultEl = document.getElementById('json-result');
    
    if (errorEl) {
        errorEl.innerHTML = '<strong>错误:</strong> ' + result.error + (result.errorLine ? ' (行: ' + result.errorLine + ', 列: ' + result.errorColumn + ')' : '');
        errorEl.style.display = 'block';
    }
    if (resultEl) resultEl.style.display = 'none';
}

// 正则表达式工具
async function loadRegexTemplates() {
    const container = document.getElementById('regex-templates');
    if (!container) {
        return;
    }
    
    try {
        const templates = await apiCall(API_BASE + '/api/regex/templates');
        
        if (!templates || !Array.isArray(templates) || templates.length === 0) {
            container.innerHTML = '<p class="text-muted">暂无模板</p>';
            return;
        }
        
        const categories = {};
        templates.forEach(function(t) {
            if (!categories[t.category]) categories[t.category] = [];
            categories[t.category].push(t);
        });
        
        let html = '';
        let index = 0;
        for (const category in categories) {
            if (categories.hasOwnProperty(category)) {
                html += '<div class="accordion-item">' +
                    '<h2 class="accordion-header">' +
                    '<button class="accordion-button ' + (index > 0 ? 'collapsed' : '') + '" type="button" data-bs-toggle="collapse" data-bs-target="#category-' + index + '">' +
                    category + '</button></h2>' +
                    '<div id="category-' + index + '" class="accordion-collapse collapse ' + (index === 0 ? 'show' : '') + '">' +
                    '<div class="accordion-body">';
                
                categories[category].forEach(function(t) {
                    html += '<div class="mb-2"><a href="#" onclick="useRegexTemplate(\'' + escapeHtml(t.pattern) + '\', \'' + escapeHtml(t.example) + '\')" title="' + escapeHtml(t.description) + '">' + t.name + '</a></div>';
                });
                
                html += '</div></div></div>';
                index++;
            }
        }
        
        container.innerHTML = html;
    } catch (error) {
        console.error('加载正则模板失败', error);
        container.innerHTML = '<p class="text-muted">加载模板失败</p>';
    }
}

function useRegexTemplate(pattern, example) {
    const patternEl = document.getElementById('regex-pattern');
    const textEl = document.getElementById('regex-text');
    
    if (patternEl) patternEl.value = pattern;
    if (textEl) textEl.value = example;
    return false;
}

async function testRegex() {
    const patternEl = document.getElementById('regex-pattern');
    const textEl = document.getElementById('regex-text');
    
    if (!patternEl || !textEl) return;
    
    const pattern = patternEl.value;
    const text = textEl.value;
    
    if (!pattern || !text) {
        showToast('请输入正则表达式和待匹配文本', 'error');
        return;
    }
    
    const modeEl = document.getElementById('regex-mode');
    const ignoreCaseEl = document.getElementById('regex-ignore-case');
    const multilineEl = document.getElementById('regex-multiline');
    
    const mode = modeEl ? modeEl.value : 'FIND';
    const ignoreCase = ignoreCaseEl ? ignoreCaseEl.checked : false;
    const multiline = multilineEl ? multilineEl.checked : false;
    
    try {
        const result = await apiCall(API_BASE + '/api/regex/test', {
            method: 'POST',
            body: JSON.stringify({ 
                pattern: pattern, 
                text: text, 
                mode: mode, 
                ignoreCase: ignoreCase, 
                multiline: multiline
            })
        });
        
        const container = document.getElementById('regex-matches');
        const resultEl = document.getElementById('regex-result');
        
        if (!container) return;
        
        if (!result.validPattern) {
            container.innerHTML = '<div class="alert alert-danger">' + escapeHtml(result.error) + '</div>';
        } else if (!result.matched) {
            container.innerHTML = '<div class="alert alert-warning">未匹配到内容</div>';
        } else {
            container.innerHTML = '<p>匹配到 ' + result.matchCount + ' 个结果</p>' +
                result.matches.map(function(m, i) {
                    return '<div class="match-item">' +
                        '<div class="d-flex justify-content-between">' +
                        '<strong>匹配 ' + (i + 1) + '</strong>' +
                        '<span class="text-muted">位置: ' + m.start + '-' + m.end + '</span>' +
                        '</div>' +
                        '<pre>' + escapeHtml(m.value) + '</pre></div>';
                }).join('');
        }
        
        if (resultEl) resultEl.style.display = 'block';
        saveHistory('regex', '正则测试', { input: { pattern: pattern, text: text }, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// MQTT工具
async function mqttConnect() {
    const brokerEl = document.getElementById('mqtt-broker');
    const portEl = document.getElementById('mqtt-port');
    const usernameEl = document.getElementById('mqtt-username');
    const passwordEl = document.getElementById('mqtt-password');
    const clientIdEl = document.getElementById('mqtt-client-id');
    const versionEl = document.getElementById('mqtt-version');
    
    if (!brokerEl) return;
    
    const broker = brokerEl.value;
    const port = portEl ? parseInt(portEl.value) : 1883;
    const username = usernameEl ? usernameEl.value : '';
    const password = passwordEl ? passwordEl.value : '';
    const clientId = clientIdEl ? clientIdEl.value : '';
    const version = versionEl ? versionEl.value : '3.1.1';
    
    if (!broker) {
        showToast('请输入服务器地址', 'error');
        return;
    }
    
    try {
        let result;
        if (typeof ConnectionManager !== 'undefined' && ConnectionManager.isConnected()) {
            result = await ConnectionManager.connectMqtt({ 
                broker: broker, 
                port: port, 
                username: username, 
                password: password, 
                clientId: clientId, 
                version: version 
            });
        } else {
            result = await apiCall(API_BASE + '/api/mqtt/connect', {
                method: 'POST',
                body: JSON.stringify({ 
                    broker: broker, 
                    port: port, 
                    username: username, 
                    password: password, 
                    clientId: clientId, 
                    version: version 
                })
            });
            startMqttLogPolling(result.clientId);
        }
        
        showToast('MQTT连接成功');
        saveHistory('mqtt', '连接MQTT', { input: { broker: broker, port: port }, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function mqttDisconnect(connectionId) {
    let connId = connectionId;
    if (!connId && typeof ConnectionManager !== 'undefined') {
        connId = ConnectionManager.getActiveMqttConnection();
    }
    
    if (!connId) {
        showToast('未选择连接', 'error');
        return;
    }
    
    try {
        if (typeof ConnectionManager !== 'undefined') {
            await ConnectionManager.disconnectMqtt(connId);
        } else {
            await apiCall(API_BASE + '/api/mqtt/disconnect/' + connId, { method: 'POST' });
            let topicsContainer = document.getElementById('mqtt-subscribed-topics');
            if (topicsContainer) {
                topicsContainer.innerHTML = '<small class="text-muted">暂无订阅</small>';
            }
            let logsContainer = document.getElementById('mqtt-logs');
            if (logsContainer) {
                logsContainer.innerHTML = '';
            }
        }
        showToast('已断开连接');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function mqttPublish() {
    let connectionId;
    if (typeof ConnectionManager !== 'undefined') {
        connectionId = ConnectionManager.getActiveMqttConnection();
    }
    
    if (!connectionId) {
        showToast('请先选择一个MQTT连接', 'error');
        return;
    }
    
    const topicEl = document.getElementById('mqtt-topic');
    const messageEl = document.getElementById('mqtt-message');
    const qosEl = document.getElementById('mqtt-qos');
    const retainedEl = document.getElementById('mqtt-retained');
    
    if (!topicEl || !messageEl) return;
    
    const topic = topicEl.value;
    const message = messageEl.value;
    
    if (!topic || !message) {
        showToast('请输入主题和消息', 'error');
        return;
    }
    
    const qos = qosEl ? parseInt(qosEl.value) : 0;
    const retained = retainedEl ? retainedEl.checked : false;
    
    try {
        await apiCall(API_BASE + '/api/mqtt/publish', {
            method: 'POST',
            body: JSON.stringify({
                clientId: connectionId,
                topic: topic,
                message: message,
                qos: qos,
                retained: retained
            })
        });
        showToast('消息已发布');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function mqttSubscribe() {
    let connectionId;
    if (typeof ConnectionManager !== 'undefined') {
        connectionId = ConnectionManager.getActiveMqttConnection();
    }
    
    if (!connectionId) {
        showToast('请先选择一个MQTT连接', 'error');
        return;
    }
    
    const topicEl = document.getElementById('mqtt-subscribe-topic');
    const qosEl = document.getElementById('mqtt-subscribe-qos');
    
    if (!topicEl) return;
    
    const topic = topicEl.value.trim();
    
    if (!topic) {
        showToast('请输入订阅主题', 'error');
        return;
    }
    
    const qos = qosEl ? parseInt(qosEl.value) : 0;
    
    try {
        await apiCall(API_BASE + '/api/mqtt/subscribe', {
            method: 'POST',
            body: JSON.stringify({
                clientId: connectionId,
                topics: [topic],
                qos: qos
            })
        });
        showToast('订阅成功: ' + topic);
        topicEl.value = '';
        refreshSubscribedTopics();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function mqttUnsubscribe(topic) {
    let connectionId;
    if (typeof ConnectionManager !== 'undefined') {
        connectionId = ConnectionManager.getActiveMqttConnection();
    }
    
    if (!connectionId) {
        showToast('未选择连接', 'error');
        return;
    }
    
    try {
        await apiCall(API_BASE + '/api/mqtt/unsubscribe?connectionId=' + encodeURIComponent(connectionId) + '&topic=' + encodeURIComponent(topic), {
            method: 'POST'
        });
        showToast('取消订阅: ' + topic);
        refreshSubscribedTopics();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function refreshSubscribedTopics() {
    let connectionId;
    if (typeof ConnectionManager !== 'undefined') {
        connectionId = ConnectionManager.getActiveMqttConnection();
    }
    
    if (!connectionId) return;
    
    try {
        const status = await apiCall(API_BASE + '/api/mqtt/status/' + connectionId);
        const container = document.getElementById('mqtt-subscribed-topics');
        if (!container) return;
        
        const topics = status.subscribedTopics || [];
        
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

function clearMqttLogs() {
    const container = document.getElementById('mqtt-logs');
    if (container) container.innerHTML = '';
}

// TCP工具
async function tcpConnect() {
    const hostEl = document.getElementById('tcp-host');
    const portEl = document.getElementById('tcp-port');
    const timeoutEl = document.getElementById('tcp-timeout');
    const charsetEl = document.getElementById('tcp-charset');
    
    if (!hostEl || !portEl) return;
    
    const host = hostEl.value;
    const port = parseInt(portEl.value);
    const timeout = timeoutEl ? parseInt(timeoutEl.value) : 5000;
    const charset = charsetEl ? charsetEl.value : 'UTF-8';
    
    if (!host || !port) {
        showToast('请输入服务器地址和端口', 'error');
        return;
    }
    
    try {
        let result;
        if (typeof ConnectionManager !== 'undefined' && ConnectionManager.isConnected()) {
            result = await ConnectionManager.connectTcp({ host: host, port: port, timeout: timeout, charset: charset });
        } else {
            result = await apiCall(API_BASE + '/api/tcp/connect', {
                method: 'POST',
                body: JSON.stringify({ host: host, port: port, timeout: timeout, charset: charset })
            });
            startTcpLogPolling(result.connectionId);
        }
        
        showToast('TCP连接成功');
        saveHistory('tcp', '连接TCP', { input: { host: host, port: port }, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function tcpDisconnect(connectionId) {
    let connId = connectionId;
    if (!connId && typeof ConnectionManager !== 'undefined') {
        connId = ConnectionManager.getActiveTcpConnection();
    }
    
    if (!connId) {
        showToast('未选择连接', 'error');
        return;
    }
    
    try {
        if (typeof ConnectionManager !== 'undefined') {
            await ConnectionManager.disconnectTcp(connId);
        } else {
            await apiCall(API_BASE + '/api/tcp/disconnect/' + connId, { method: 'POST' });
            let logsContainer = document.getElementById('tcp-logs');
            if (logsContainer) {
                logsContainer.innerHTML = '';
            }
        }
        showToast('已断开连接');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function tcpSend() {
    let connectionId;
    if (typeof ConnectionManager !== 'undefined') {
        connectionId = ConnectionManager.getActiveTcpConnection();
    }
    
    if (!connectionId) {
        showToast('请先选择一个TCP连接', 'error');
        return;
    }
    
    const dataEl = document.getElementById('tcp-send-data');
    const formatEl = document.getElementById('tcp-format');
    const addNewlineEl = document.getElementById('tcp-add-newline');
    
    if (!dataEl) return;
    
    const data = dataEl.value;
    if (!data) {
        showToast('请输入要发送的数据', 'error');
        return;
    }
    
    const format = formatEl ? formatEl.value : 'TEXT';
    const addNewLine = addNewlineEl ? addNewlineEl.checked : false;
    
    if (format === 'HEX') {
        const cleanHex = data.replace(/\s+/g, '');
        if (!/^[0-9A-Fa-f]*$/.test(cleanHex)) {
            showToast('HEX格式错误：只能包含0-9和A-F字符', 'error');
            return;
        }
        if (cleanHex.length % 2 !== 0) {
            showToast('HEX格式错误：字节数必须为偶数', 'error');
            return;
        }
    }
    
    try {
        await apiCall(API_BASE + '/api/tcp/send', {
            method: 'POST',
            body: JSON.stringify({
                connectionId: connectionId,
                data: data,
                format: format,
                addNewLine: addNewLine
            })
        });
        showToast('数据已发送');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function initTcpFormatListener() {
    const formatEl = document.getElementById('tcp-format');
    const hintEl = document.getElementById('tcp-format-hint');
    const addNewlineEl = document.getElementById('tcp-add-newline');
    const dataEl = document.getElementById('tcp-send-data');
    
    if (formatEl && hintEl) {
        formatEl.addEventListener('change', function() {
            if (this.value === 'HEX') {
                hintEl.textContent = 'HEX模式：输入十六进制字符串，如 68 10 06 01 或 68100601';
                if (dataEl) dataEl.placeholder = '输入十六进制数据，如：6810060111042520';
                if (addNewlineEl) {
                    addNewlineEl.checked = false;
                    addNewlineEl.disabled = true;
                }
            } else {
                hintEl.textContent = '文本模式：直接发送输入内容';
                if (dataEl) dataEl.placeholder = '输入要发送的数据';
                if (addNewlineEl) {
                    addNewlineEl.disabled = false;
                }
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', function() {
    initTcpFormatListener();
});

function clearTcpLogs() {
    const container = document.getElementById('tcp-logs');
    if (container) container.innerHTML = '';
}

// SHA256工具
async function sha256Encrypt() {
    const inputEl = document.getElementById('sha256-input');
    if (!inputEl) return;
    
    const text = inputEl.value;
    if (!text) {
        showToast('请输入要加密的文本', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/sha256/encrypt', {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: text
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('sha256-result-hex').value = data.data.hashHex;
        document.getElementById('sha256-result-base64').value = data.data.hashBase64;
        document.getElementById('sha256-result').style.display = 'block';
        
        saveHistory('sha256', 'SHA256加密', { input: text, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function sha256EncryptFile() {
    const fileInput = document.getElementById('sha256-file-input');
    if (!fileInput || !fileInput.files[0]) {
        showToast('请选择文件', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    
    try {
        const response = await fetch(API_BASE + '/api/sha256/encrypt-file', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('sha256-result-hex').value = data.data.hashHex;
        document.getElementById('sha256-result-base64').value = data.data.hashBase64;
        document.getElementById('sha256-result').style.display = 'block';
        
        saveHistory('sha256', '文件SHA256加密: ' + fileInput.files[0].name, { input: fileInput.files[0].name, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function sha256Verify() {
    const textEl = document.getElementById('sha256-verify-text');
    const hashEl = document.getElementById('sha256-verify-hash');
    
    if (!textEl || !hashEl) return;
    
    const text = textEl.value;
    const hash = hashEl.value;
    
    if (!text || !hash) {
        showToast('请输入原始文本和预期哈希值', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/sha256/verify?text=' + encodeURIComponent(text) + '&hash=' + encodeURIComponent(hash), {
            method: 'POST'
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const alertEl = document.getElementById('sha256-verify-alert');
        const resultEl = document.getElementById('sha256-verify-result');
        
        if (data.data === true) {
            alertEl.className = 'alert alert-success';
            alertEl.innerHTML = '<i class="bi bi-check-circle"></i> <strong>校验通过</strong> - 文本完整性验证成功';
        } else {
            alertEl.className = 'alert alert-danger';
            alertEl.innerHTML = '<i class="bi bi-x-circle"></i> <strong>校验失败</strong> - 文本可能已被修改';
        }
        
        resultEl.style.display = 'block';
        saveHistory('sha256', '完整性校验', { input: { text: text, hash: hash }, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// AES工具
async function aesGenerateKey() {
    const keySizeEl = document.getElementById('aes-key-size');
    const keySize = keySizeEl ? parseInt(keySizeEl.value) : 128;
    
    try {
        const response = await fetch(API_BASE + '/api/aes/generate-key?keySize=' + keySize);
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('aes-key').value = data.data;
        showToast('密钥已生成');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function aesGenerateIv() {
    try {
        const response = await fetch(API_BASE + '/api/aes/generate-iv');
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('aes-iv').value = data.data;
        showToast('IV向量已生成');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function aesEncrypt() {
    const inputEl = document.getElementById('aes-encrypt-input');
    const keyEl = document.getElementById('aes-key');
    const ivEl = document.getElementById('aes-iv');
    const keySizeEl = document.getElementById('aes-key-size');
    const modeEl = document.getElementById('aes-mode');
    const paddingEl = document.getElementById('aes-padding');
    
    if (!inputEl || !keyEl) return;
    
    const text = inputEl.value;
    const key = keyEl.value;
    
    if (!text) {
        showToast('请输入要加密的文本', 'error');
        return;
    }
    if (!key) {
        showToast('请输入或生成密钥', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/aes/encrypt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: text,
                key: key,
                iv: ivEl ? ivEl.value : '',
                mode: modeEl ? modeEl.value : 'CBC',
                padding: paddingEl ? paddingEl.value : 'PKCS5Padding',
                keySize: keySizeEl ? parseInt(keySizeEl.value) : 128
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('aes-result-label').textContent = '加密结果';
        document.getElementById('aes-result-data').value = data.data.encryptedData;
        document.getElementById('aes-result-iv').value = data.data.iv || '-';
        document.getElementById('aes-result-mode').value = data.data.mode;
        document.getElementById('aes-result-keysize').value = data.data.keySize + '位';
        document.getElementById('aes-result').style.display = 'block';
        
        saveHistory('aes', 'AES加密', { input: plainText, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function aesDecrypt() {
    const inputEl = document.getElementById('aes-decrypt-input');
    const keyEl = document.getElementById('aes-decrypt-key');
    const ivEl = document.getElementById('aes-decrypt-iv');
    const keySizeEl = document.getElementById('aes-decrypt-key-size');
    const modeEl = document.getElementById('aes-decrypt-mode');
    const paddingEl = document.getElementById('aes-decrypt-padding');
    
    if (!inputEl || !keyEl) return;
    
    const encryptedData = inputEl.value;
    const key = keyEl.value;
    
    if (!encryptedData) {
        showToast('请输入加密数据', 'error');
        return;
    }
    if (!key) {
        showToast('请输入密钥', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/aes/decrypt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                encryptedData: encryptedData,
                key: key,
                iv: ivEl ? ivEl.value : '',
                mode: modeEl ? modeEl.value : 'CBC',
                padding: paddingEl ? paddingEl.value : 'PKCS5Padding',
                keySize: keySizeEl ? parseInt(keySizeEl.value) : 128
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('aes-result-label').textContent = '解密结果';
        document.getElementById('aes-result-data').value = data.data.decryptedText;
        document.getElementById('aes-result-iv').value = data.data.iv || '-';
        document.getElementById('aes-result-mode').value = data.data.mode;
        document.getElementById('aes-result-keysize').value = data.data.keySize + '位';
        document.getElementById('aes-result').style.display = 'block';
        
        saveHistory('aes', 'AES解密', { input: encryptedData, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jasyptEncrypt() {
    const inputEl = document.getElementById('jasypt-encrypt-input');
    const passwordEl = document.getElementById('jasypt-password');
    const algorithmEl = document.getElementById('jasypt-algorithm');
    const iterationsEl = document.getElementById('jasypt-iterations');
    
    if (!inputEl || !passwordEl) return;
    
    const text = inputEl.value;
    const password = passwordEl.value;
    const algorithm = algorithmEl ? algorithmEl.value : 'PBEWithMD5AndDES';
    const iterations = iterationsEl ? parseInt(iterationsEl.value) : 1000;
    
    if (!text) {
        showToast('请输入待加密文本', 'error');
        return;
    }
    if (!password) {
        showToast('请输入加密密码', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/jasypt/encrypt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: text,
                password: password,
                algorithm: algorithm,
                saltIterations: iterations
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('jasypt-result-data').value = data.data.encryptedData;
        document.getElementById('jasypt-result-with-prefix').value = data.data.encryptedWithPrefix;
        document.getElementById('jasypt-encrypt-result').style.display = 'block';
        
        saveHistory('jasypt', 'Jasypt加密', { input: text, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jasyptDecrypt() {
    const inputEl = document.getElementById('jasypt-decrypt-input');
    const passwordEl = document.getElementById('jasypt-decrypt-password');
    const algorithmEl = document.getElementById('jasypt-decrypt-algorithm');
    const iterationsEl = document.getElementById('jasypt-decrypt-iterations');
    
    if (!inputEl || !passwordEl) return;
    
    const encryptedText = inputEl.value;
    const password = passwordEl.value;
    const algorithm = algorithmEl ? algorithmEl.value : 'PBEWithMD5AndDES';
    const iterations = iterationsEl ? parseInt(iterationsEl.value) : 1000;
    
    if (!encryptedText) {
        showToast('请输入加密文本', 'error');
        return;
    }
    if (!password) {
        showToast('请输入加密密码', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/jasypt/decrypt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                encryptedText: encryptedText,
                password: password,
                algorithm: algorithm,
                saltIterations: iterations
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('jasypt-decrypted-text').value = data.data.decryptedText;
        document.getElementById('jasypt-decrypt-result').style.display = 'block';
        
        saveHistory('jasypt', 'Jasypt解密', { input: encryptedText, output: data.data });
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// 提示词优化工具
async function optimizePrompt() {
    const inputEl = document.getElementById('prompt-input');
    const domainEl = document.getElementById('prompt-domain');
    const maxLengthEl = document.getElementById('prompt-max-length');
    const enhanceKeywordsEl = document.getElementById('prompt-enhance-keywords');
    const addStructureEl = document.getElementById('prompt-add-structure');
    const improveClarityEl = document.getElementById('prompt-improve-clarity');
    
    if (!inputEl) return;
    
    const prompt = inputEl.value;
    if (!prompt) {
        showToast('请输入提示词', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/prompt/optimize', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                prompt: prompt,
                domain: domainEl ? domainEl.value : 'general',
                maxLength: maxLengthEl ? parseInt(maxLengthEl.value) : null,
                enhanceKeywords: enhanceKeywordsEl ? enhanceKeywordsEl.checked : true,
                addStructure: addStructureEl ? addStructureEl.checked : true,
                improveClarity: improveClarityEl ? improveClarityEl.checked : true
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const result = data.data;
        
        document.getElementById('prompt-optimized').value = result.optimizedPrompt;
        document.getElementById('prompt-clarity-score').textContent = (result.analysis.clarityScore || 0).toFixed(0);
        document.getElementById('prompt-completeness-score').textContent = (result.analysis.completenessScore || 0).toFixed(0);
        document.getElementById('prompt-relevance-score').textContent = (result.analysis.relevanceScore || 0).toFixed(0);
        
        const keywordsContainer = document.getElementById('prompt-keywords');
        if (keywordsContainer && result.extractedKeywords) {
            keywordsContainer.innerHTML = result.extractedKeywords.map(function(k) {
                return '<span class="keyword-tag">' + escapeHtml(k) + '</span>';
            }).join('');
        }
        
        const suggestionsEl = document.getElementById('prompt-suggestions');
        if (suggestionsEl && result.suggestions) {
            suggestionsEl.innerHTML = result.suggestions.map(function(s) {
                return '<li class="list-group-item list-group-item-action">' + escapeHtml(s) + '</li>';
            }).join('');
        }
        
        document.getElementById('prompt-result').style.display = 'block';
        
        saveHistory('prompt', '提示词优化', { input: prompt, output: result });
    } catch (error) {
        showToast(error.message, 'error');
    }
}
