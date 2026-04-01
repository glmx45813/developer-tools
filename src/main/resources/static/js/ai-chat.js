let conversationHistory = [];
let isStreaming = false;
let multimodalFiles = [];

let providerModels = {};
let modelCacheTime = {};
const MODEL_CACHE_DURATION = 30 * 60 * 1000;

const MAX_IMAGE_SIZE = 20 * 1024 * 1024;
const MAX_AUDIO_SIZE = 25 * 1024 * 1024;
const SUPPORTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'];
const SUPPORTED_AUDIO_TYPES = ['audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/ogg', 'audio/aac', 'audio/flac', 'audio/webm', 'audio/mp4'];

document.addEventListener('DOMContentLoaded', function() {
    initAIChat();
});

function initAIChat() {
    loadSavedConfig();
    loadProvidersInfo();
    
    const providerSelect = document.getElementById('ai-provider');
    const tempSlider = document.getElementById('ai-temperature');
    const tempValue = document.getElementById('temp-value');
    const chatInput = document.getElementById('chat-input');
    const apiKeyInput = document.getElementById('ai-apikey');
    
    if (providerSelect) {
        providerSelect.addEventListener('change', function() {
            onProviderChange(this.value);
        });
    }
    
    if (tempSlider) {
        tempSlider.addEventListener('input', function() {
            tempValue.textContent = this.value;
            saveConfig();
        });
    }
    
    if (chatInput) {
        chatInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
        
        chatInput.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 300) + 'px';
        });
        
        chatInput.addEventListener('paste', handlePaste);
    }
    
    const inputWrapper = document.getElementById('chat-input-wrapper');
    if (inputWrapper) {
        inputWrapper.addEventListener('dragover', function(e) {
            e.preventDefault();
            this.classList.add('drag-over');
        });
        
        inputWrapper.addEventListener('dragleave', function(e) {
            this.classList.remove('drag-over');
        });
        
        inputWrapper.addEventListener('drop', handleDrop);
    }
    
    if (apiKeyInput) {
        apiKeyInput.addEventListener('change', function() {
            saveConfig();
            const provider = document.getElementById('ai-provider')?.value;
            if (provider) {
                refreshModelsFromApi(provider);
            }
        });
    }
    
    const endpointInput = document.getElementById('ai-endpoint');
    if (endpointInput) {
        endpointInput.addEventListener('change', saveConfig);
    }
    
    const maxTokensInput = document.getElementById('ai-max-tokens');
    if (maxTokensInput) {
        maxTokensInput.addEventListener('change', saveConfig);
    }
}

async function loadProvidersInfo() {
    try {
        const response = await fetch(API_BASE + '/api/ai/providers/info');
        const data = await response.json();
        
        if (data.code === 200 && data.data) {
            const providerSelect = document.getElementById('ai-provider');
            if (providerSelect) {
                providerSelect.innerHTML = '<option value="">选择提供商</option>';
                data.data.forEach(function(provider) {
                    const option = document.createElement('option');
                    option.value = provider.name;
                    option.textContent = getProviderDisplayName(provider.name);
                    if (!provider.available) {
                        option.textContent += ' (未配置)';
                    }
                    providerSelect.appendChild(option);
                });
            }
        }
    } catch (e) {
        console.error('Failed to load providers info:', e);
    }
}

function getProviderDisplayName(name) {
    const displayNames = {
        'openai': 'OpenAI',
        'qwen': '阿里云通义千问',
        'moonshot': 'Moonshot (Kimi)',
        'doubao': '字节豆包',
        'deepseek': 'DeepSeek'
    };
    return displayNames[name] || name;
}

function loadSavedConfig() {
    try {
        const savedConfig = localStorage.getItem('aiChatConfig');
        if (savedConfig) {
            const config = JSON.parse(savedConfig);
            
            const providerSelect = document.getElementById('ai-provider');
            const apiKeyInput = document.getElementById('ai-apikey');
            const endpointInput = document.getElementById('ai-endpoint');
            const tempSlider = document.getElementById('ai-temperature');
            const tempValue = document.getElementById('temp-value');
            const maxTokensInput = document.getElementById('ai-max-tokens');
            
            if (config.provider && providerSelect) {
                providerSelect.value = config.provider;
                onProviderChange(config.provider);
            }
            
            if (config.apiKey && apiKeyInput) {
                apiKeyInput.value = config.apiKey;
            }
            
            if (config.endpoint && endpointInput) {
                endpointInput.value = config.endpoint;
            }
            
            if (config.temperature && tempSlider) {
                tempSlider.value = config.temperature;
                if (tempValue) tempValue.textContent = config.temperature;
            }
            
            if (config.maxTokens && maxTokensInput) {
                maxTokensInput.value = config.maxTokens;
            }
            
            if (config.model) {
                const modelSelect = document.getElementById('ai-model');
                if (modelSelect) {
                    setTimeout(function() {
                        modelSelect.value = config.model;
                    }, 100);
                }
            }
        }
    } catch (e) {
        console.error('Failed to load config:', e);
    }
}

function saveConfig() {
    const config = {
        provider: document.getElementById('ai-provider')?.value || '',
        apiKey: document.getElementById('ai-apikey')?.value || '',
        endpoint: document.getElementById('ai-endpoint')?.value || '',
        model: document.getElementById('ai-model')?.value || '',
        temperature: document.getElementById('ai-temperature')?.value || '0.7',
        maxTokens: document.getElementById('ai-max-tokens')?.value || '2048'
    };
    localStorage.setItem('aiChatConfig', JSON.stringify(config));
}

function onProviderChange(provider) {
    const modelSelect = document.getElementById('ai-model');
    const doubaoEndpointGroup = document.getElementById('doubao-endpoint-group');
    
    if (!modelSelect) return;
    
    if (doubaoEndpointGroup) {
        doubaoEndpointGroup.style.display = provider === 'doubao' ? 'block' : 'none';
    }
    
    if (!provider) {
        modelSelect.innerHTML = '<option value="">选择模型</option>';
        return;
    }
    
    const apiKey = getApiKey();
    if (!apiKey) {
        modelSelect.innerHTML = '<option value="">请先输入API Key</option>';
        showToast('请先输入 ' + getProviderDisplayName(provider) + ' 的 API Key', 'warning');
        saveConfig();
        return;
    }
    
    modelSelect.innerHTML = '<option value="">加载模型中...</option>';
    
    loadModelsFromApi(provider);
    
    saveConfig();
}

async function loadModelsFromApi(provider) {
    const modelSelect = document.getElementById('ai-model');
    
    const apiKey = getApiKey();
    
    if (!apiKey) {
        modelSelect.innerHTML = '<option value="">请先输入API Key</option>';
        return;
    }
    
    console.log('Loading models for provider:', provider, 'with API key:', apiKey ? 'provided' : 'not provided');
    
    try {
        const headers = {
            'Content-Type': 'application/json'
        };
        if (apiKey) {
            headers['X-API-Key'] = apiKey;
        }
        
        const response = await fetch(API_BASE + '/api/ai/models/' + provider, {
            headers: headers
        });
        
        const data = await response.json();
        
        console.log('Models response:', data);
        
        if (data.code === 200 && data.data && data.data.length > 0) {
            const models = data.data.map(function(m) {
                return {
                    id: m.id,
                    name: m.displayName || m.name || m.id,
                    multimodal: m.supportsMultimodal || m.supportsVision || false
                };
            });
            
            console.log('Parsed models:', models);
            
            providerModels[provider] = models;
            modelCacheTime[provider] = Date.now();
            
            populateModelSelect(provider, models);
            showToast('已加载 ' + models.length + ' 个模型', 'success');
        } else {
            console.log('No models returned, response:', data);
            modelSelect.innerHTML = '<option value="">获取模型失败，请检查API Key</option>';
            showToast('获取模型列表失败，请检查API Key是否正确', 'error');
        }
    } catch (e) {
        console.error('Failed to load models:', e);
        modelSelect.innerHTML = '<option value="">加载失败</option>';
        showToast('加载模型列表失败: ' + e.message, 'error');
    }
}

async function refreshModelsFromApi(provider) {
    const apiKey = getApiKey();
    
    if (!apiKey) {
        showToast('请先输入API Key', 'warning');
        return;
    }
    
    try {
        const headers = {
            'Content-Type': 'application/json'
        };
        headers['X-API-Key'] = apiKey;
        
        const response = await fetch(API_BASE + '/api/ai/models/' + provider + '/refresh', {
            method: 'POST',
            headers: headers
        });
        
        const data = await response.json();
        
        if (data.code === 200 && data.data && data.data.length > 0) {
            const models = data.data.map(function(m) {
                return {
                    id: m.id,
                    name: m.displayName || m.name || m.id,
                    multimodal: m.supportsMultimodal || m.supportsVision || false
                };
            });
            
            providerModels[provider] = models;
            modelCacheTime[provider] = Date.now();
            
            populateModelSelect(provider, models);
            showToast('已刷新 ' + models.length + ' 个模型', 'success');
        }
    } catch (e) {
        console.error('Failed to refresh models:', e);
        showToast('刷新模型列表失败: ' + e.message, 'error');
    }
}

function populateModelSelect(provider, models) {
    const modelSelect = document.getElementById('ai-model');
    if (!modelSelect) return;
    
    modelSelect.innerHTML = '<option value="">选择模型</option>';
    
    models.forEach(function(model) {
        const option = document.createElement('option');
        option.value = model.id;
        option.textContent = model.name + (model.multimodal ? ' (支持多模态)' : '');
        option.dataset.multimodal = model.multimodal;
        modelSelect.appendChild(option);
    });
    
    const savedConfig = localStorage.getItem('aiChatConfig');
    if (savedConfig) {
        const config = JSON.parse(savedConfig);
        if (config.model && modelSelect.querySelector('option[value="' + config.model + '"]')) {
            modelSelect.value = config.model;
        }
    }
}

function toggleApiKeyVisibility() {
    const apiKeyInput = document.getElementById('ai-apikey');
    const toggleIcon = document.getElementById('apikey-toggle-icon');
    
    if (apiKeyInput.type === 'password') {
        apiKeyInput.type = 'text';
        toggleIcon.className = 'bi bi-eye-slash';
    } else {
        apiKeyInput.type = 'password';
        toggleIcon.className = 'bi bi-eye';
    }
}

function toggleMultimodalMenu() {
    const menu = document.getElementById('multimodal-menu');
    if (menu) {
        menu.style.display = menu.style.display === 'none' ? 'flex' : 'none';
    }
}

function selectImageFile() {
    const fileInput = document.getElementById('multimodal-file-input');
    if (fileInput) {
        fileInput.accept = 'image/*';
        fileInput.click();
    }
}

function selectAudioFile() {
    const fileInput = document.getElementById('multimodal-file-input');
    if (fileInput) {
        fileInput.accept = 'audio/*';
        fileInput.click();
    }
}

function handleMultimodalFile(input) {
    const file = input.files[0];
    if (file) {
        addMultimodalFile(file);
    }
    input.value = '';
    toggleMultimodalMenu();
}

function handlePaste(e) {
    const items = e.clipboardData.items;
    for (let i = 0; i < items.length; i++) {
        if (items[i].type.indexOf('image') !== -1) {
            const file = items[i].getAsFile();
            addMultimodalFile(file);
            e.preventDefault();
        }
    }
}

function handleDrop(e) {
    e.preventDefault();
    e.target.classList.remove('drag-over');
    
    const files = e.dataTransfer.files;
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        if (file.type.startsWith('image/') || file.type.startsWith('audio/')) {
            addMultimodalFile(file);
        }
    }
}

function addMultimodalFile(file) {
    const fileType = file.type.startsWith('image/') ? 'image' : 'audio';
    
    if (fileType === 'image') {
        if (!SUPPORTED_IMAGE_TYPES.includes(file.type)) {
            showToast('不支持的图片格式。支持: JPEG, PNG, GIF, WebP, BMP', 'error');
            return;
        }
        if (file.size > MAX_IMAGE_SIZE) {
            showToast('图片大小超过限制 (最大20MB)', 'error');
            return;
        }
    } else {
        if (!SUPPORTED_AUDIO_TYPES.includes(file.type)) {
            showToast('不支持的音频格式。支持: MP3, WAV, OGG, AAC, FLAC, WebM', 'error');
            return;
        }
        if (file.size > MAX_AUDIO_SIZE) {
            showToast('音频大小超过限制 (最大25MB)', 'error');
            return;
        }
    }
    
    const id = Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    const fileData = {
        id: id,
        file: file,
        type: fileType,
        name: file.name,
        size: file.size,
        mimeType: file.type
    };
    
    const reader = new FileReader();
    reader.onload = function(e) {
        fileData.dataUrl = e.target.result;
        multimodalFiles.push(fileData);
        renderMultimodalPreview();
    };
    reader.onerror = function() {
        showToast('文件读取失败', 'error');
    };
    reader.readAsDataURL(file);
}

function removeMultimodalFile(id) {
    multimodalFiles = multimodalFiles.filter(function(f) {
        return f.id !== id;
    });
    renderMultimodalPreview();
}

function renderMultimodalPreview() {
    const container = document.getElementById('multimodal-preview');
    if (!container) return;
    
    if (multimodalFiles.length === 0) {
        container.style.display = 'none';
        container.innerHTML = '';
        return;
    }
    
    container.style.display = 'flex';
    
    let html = '';
    multimodalFiles.forEach(function(file) {
        html += '<div class="multimodal-preview-item">';
        
        if (file.type === 'image') {
            html += '<img src="' + file.dataUrl + '" alt="' + file.name + '">';
        } else {
            html += '<i class="bi bi-file-earmark-music" style="font-size: 2rem; color: #6c757d;"></i>';
        }
        
        html += '<div class="file-info">';
        html += '<span class="file-name" title="' + file.name + '">' + file.name + '</span>';
        html += '<span class="file-size">' + formatFileSize(file.size) + '</span>';
        html += '</div>';
        html += '<button class="remove-btn" onclick="removeMultimodalFile(\'' + file.id + '\')">&times;</button>';
        html += '</div>';
    });
    
    container.innerHTML = html;
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function isModelMultimodal(modelId) {
    const provider = document.getElementById('ai-provider')?.value;
    if (!provider || !providerModels[provider]) return false;
    
    const model = providerModels[provider].find(function(m) {
        return m.id === modelId;
    });
    return model ? model.multimodal : false;
}

function getApiKey() {
    return document.getElementById('ai-apikey')?.value || '';
}

function getEndpointId() {
    return document.getElementById('ai-endpoint')?.value || '';
}

function getAuthHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };
    
    const apiKey = getApiKey();
    const provider = document.getElementById('ai-provider')?.value;
    
    if (apiKey) {
        headers['X-API-Key'] = apiKey;
    }
    
    if (provider) {
        headers['X-Provider'] = provider;
    }
    
    const endpoint = getEndpointId();
    if (endpoint && provider === 'doubao') {
        headers['X-Endpoint-Id'] = endpoint;
    }
    
    return headers;
}

function isModelMultimodal(modelId) {
    const provider = document.getElementById('ai-provider')?.value;
    if (!provider || !providerModels[provider]) return false;
    
    const model = providerModels[provider].find(function(m) {
        return m.id === modelId;
    });
    return model ? model.multimodal : false;
}

async function sendMessage() {
    const input = document.getElementById('chat-input');
    const message = input.value.trim();
    const model = document.getElementById('ai-model').value;
    const provider = document.getElementById('ai-provider').value;
    const apiKey = getApiKey();
    
    if (!message && multimodalFiles.length === 0) {
        showToast('请输入消息或添加文件', 'error');
        return;
    }
    
    if (!provider) {
        showToast('请选择AI提供商', 'error');
        return;
    }
    
    if (!model) {
        showToast('请选择模型', 'error');
        return;
    }
    
    if (!apiKey) {
        showToast('请输入API Key', 'error');
        return;
    }
    
    if (isStreaming) {
        showToast('请等待当前响应完成', 'error');
        return;
    }
    
    const hasMultimodal = multimodalFiles.length > 0;
    
    if (hasMultimodal && !isModelMultimodal(model)) {
        showToast('当前模型不支持多模态输入，请选择支持多模态的模型', 'error');
        return;
    }
    
    const content = buildMessageContent(message, hasMultimodal);
    
    addMessageToUI('user', message, multimodalFiles);
    input.value = '';
    input.style.height = 'auto';
    
    conversationHistory.push({ role: 'user', content: content });
    
    const currentFiles = [...multimodalFiles];
    multimodalFiles = [];
    renderMultimodalPreview();
    
    const useStream = document.getElementById('ai-stream').checked;
    
    saveConfig();
    
    if (useStream) {
        await streamMessage(model, provider);
    } else {
        await normalMessage(model, provider);
    }
}

function buildMessageContent(text, hasMultimodal) {
    if (!hasMultimodal) {
        return text;
    }
    
    const content = [];
    
    multimodalFiles.forEach(function(file) {
        if (file.type === 'image') {
            content.push({
                type: 'image_url',
                image_url: {
                    url: file.dataUrl
                }
            });
        } else if (file.type === 'audio') {
            content.push({
                type: 'audio_url',
                audio_url: {
                    url: file.dataUrl
                }
            });
        }
    });
    
    if (text) {
        content.push({
            type: 'text',
            text: text
        });
    }
    
    return content;
}

async function streamMessage(model, provider) {
    isStreaming = true;
    const sendBtn = document.getElementById('send-btn');
    sendBtn.disabled = true;
    
    const assistantMessage = createAssistantMessage();
    const messageContent = assistantMessage.querySelector('.message-content');
    
    try {
        const response = await fetch(API_BASE + '/api/ai/chat/stream', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
                provider: provider,
                model: model,
                messages: conversationHistory,
                temperature: parseFloat(document.getElementById('ai-temperature').value),
                maxTokens: parseInt(document.getElementById('ai-max-tokens').value),
                stream: true
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(function() { return {}; });
            throw new Error(errorData.message || 'HTTP error! status: ' + response.status);
        }
        
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullContent = '';
        let buffer = '';
        
        while (true) {
            const result = await reader.read();
            if (result.done) break;
            
            buffer += decoder.decode(result.value);
            
            const lines = buffer.split('\n');
            buffer = lines.pop();
            
            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const content = line.substring(5).trim();
                    if (content) {
                        try {
                            // 尝试解析JSON格式的响应
                            const jsonContent = JSON.parse(content);
                            let text = '';
                            
                            // 处理不同格式的响应
                            if (jsonContent.text) {
                                text = jsonContent.text;
                            } else if (jsonContent.output) {
                                if (jsonContent.output.text) {
                                    text = jsonContent.output.text;
                                } else if (jsonContent.output.choices) {
                                    const choices = jsonContent.output.choices;
                                    if (choices && choices.length > 0) {
                                        const choice = choices[0];
                                        if (choice.message && choice.message.content) {
                                            const content = choice.message.content;
                                            if (Array.isArray(content) && content.length > 0) {
                                                if (content[0].text) {
                                                    text = content[0].text;
                                                }
                                            } else if (typeof content === 'string') {
                                                text = content;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (text) {
                                fullContent += text;
                                messageContent.innerHTML = renderMarkdown(fullContent);
                                highlightCode(messageContent);
                                scrollToBottom();
                            }
                        } catch (e) {
                            // 如果不是JSON，直接作为文本处理
                            fullContent += content;
                            messageContent.innerHTML = renderMarkdown(fullContent);
                            highlightCode(messageContent);
                            scrollToBottom();
                        }
                    }
                }
            }
        }
        
        if (buffer) {
            const lines = buffer.split('\n');
            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const content = line.substring(5).trim();
                    if (content) {
                        try {
                            // 尝试解析JSON格式的响应
                            const jsonContent = JSON.parse(content);
                            let text = '';
                            
                            // 处理不同格式的响应
                            if (jsonContent.text) {
                                text = jsonContent.text;
                            } else if (jsonContent.output) {
                                if (jsonContent.output.text) {
                                    text = jsonContent.output.text;
                                } else if (jsonContent.output.choices) {
                                    const choices = jsonContent.output.choices;
                                    if (choices && choices.length > 0) {
                                        const choice = choices[0];
                                        if (choice.message && choice.message.content) {
                                            const content = choice.message.content;
                                            if (Array.isArray(content) && content.length > 0) {
                                                if (content[0].text) {
                                                    text = content[0].text;
                                                }
                                            } else if (typeof content === 'string') {
                                                text = content;
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (text) {
                                fullContent += text;
                                messageContent.innerHTML = renderMarkdown(fullContent);
                                highlightCode(messageContent);
                                scrollToBottom();
                            }
                        } catch (e) {
                            // 如果不是JSON，直接作为文本处理
                            fullContent += content;
                            messageContent.innerHTML = renderMarkdown(fullContent);
                            highlightCode(messageContent);
                            scrollToBottom();
                        }
                    }
                }
            }
        }
        
        conversationHistory.push({ role: 'assistant', content: fullContent });
        
    } catch (error) {
        messageContent.innerHTML = '<div class="error-message">错误: ' + escapeHtml(error.message) + '</div>';
        showToast('请求失败: ' + error.message, 'error');
    } finally {
        isStreaming = false;
        sendBtn.disabled = false;
    }
}

async function normalMessage(model, provider) {
    const assistantMessage = createAssistantMessage();
    const messageContent = assistantMessage.querySelector('.message-content');
    messageContent.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div>';
    
    try {
        const response = await fetch(API_BASE + '/api/ai/chat', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
                provider: provider,
                model: model,
                messages: conversationHistory,
                temperature: parseFloat(document.getElementById('ai-temperature').value),
                maxTokens: parseInt(document.getElementById('ai-max-tokens').value),
                stream: false
            })
        });
        
        const data = await response.json();
        
        if (data.code !== 200) {
            throw new Error(data.message);
        }
        
        const content = data.data.message.content;
        messageContent.innerHTML = renderMarkdown(content);
        highlightCode(messageContent);
        
        conversationHistory.push({ role: 'assistant', content: content });
        
    } catch (error) {
        messageContent.innerHTML = '<div class="error-message">错误: ' + escapeHtml(error.message) + '</div>';
        showToast('请求失败: ' + error.message, 'error');
    }
}

function addMessageToUI(role, content, files) {
    const container = document.getElementById('chat-messages');
    
    const welcomeMessage = container.querySelector('.welcome-message');
    if (welcomeMessage) {
        welcomeMessage.remove();
    }
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'chat-message ' + role;
    
    const avatar = role === 'user' ? 'bi-person' : 'bi-robot';
    const avatarClass = role === 'user' ? 'user-avatar' : 'assistant-avatar';
    
    let contentHtml = '';
    
    if (role === 'user' && files && files.length > 0) {
        files.forEach(function(file) {
            if (file.type === 'image') {
                contentHtml += '<div class="message-image"><img src="' + file.dataUrl + '" style="max-width: 200px; border-radius: 8px; margin-bottom: 8px;"></div>';
            } else if (file.type === 'audio') {
                contentHtml += '<div class="message-audio"><audio controls src="' + file.dataUrl + '" style="max-width: 200px; margin-bottom: 8px;"></audio></div>';
            }
        });
    }
    
    if (content) {
        contentHtml += escapeHtml(content);
    }
    
    messageDiv.innerHTML = '<div class="message-avatar ' + avatarClass + '"><i class="bi ' + avatar + '"></i></div>' +
        '<div class="message-body">' +
        '<div class="message-content">' + contentHtml + '</div>' +
        '<div class="message-time">' + new Date().toLocaleTimeString() + '</div>' +
        '</div>';
    
    container.appendChild(messageDiv);
    scrollToBottom();
    
    return messageDiv;
}

function createAssistantMessage() {
    const container = document.getElementById('chat-messages');
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'chat-message assistant';
    
    messageDiv.innerHTML = '<div class="message-avatar assistant-avatar"><i class="bi bi-robot"></i></div>' +
        '<div class="message-body">' +
        '<div class="message-content"></div>' +
        '<div class="message-time">' + new Date().toLocaleTimeString() + '</div>' +
        '</div>';
    
    container.appendChild(messageDiv);
    scrollToBottom();
    
    return messageDiv;
}

function renderMarkdown(content) {
    try {
        if (typeof marked !== 'undefined') {
            return marked.parse(content);
        }
        return escapeHtml(content).replace(/\n/g, '<br>');
    } catch (e) {
        return escapeHtml(content);
    }
}

function highlightCode(element) {
    if (typeof hljs !== 'undefined') {
        element.querySelectorAll('pre code').forEach(function(block) {
            hljs.highlightElement(block);
        });
    }
}

function scrollToBottom() {
    const container = document.getElementById('chat-messages');
    if (container) {
        container.scrollTop = container.scrollHeight;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function clearChat() {
    conversationHistory = [];
    multimodalFiles = [];
    renderMultimodalPreview();
    
    const container = document.getElementById('chat-messages');
    if (container) {
        container.innerHTML = '<div class="welcome-message">' +
            '<h4><i class="bi bi-robot"></i> 欢迎使用AI助手</h4>' +
            '<p>选择提供商，输入API Key，选择模型后开始对话</p>' +
            '<p class="small text-muted">支持文本、图片、音频多模态输入</p>' +
            '</div>';
    }
    showToast('对话已清空');
}

function usePrompt(prompt) {
    const input = document.getElementById('chat-input');
    if (input) {
        input.value = prompt;
        input.focus();
    }
}

function showToast(message, type) {
    if (typeof window.appShowToast === 'function') {
        window.appShowToast(message, type);
    } else {
        alert(message);
    }
}

function openPromptOptimizer() {
    const input = document.getElementById('chat-input');
    const promptText = input ? input.value.trim() : '';
    
    if (!promptText) {
        showToast('请先输入需要优化的提示词', 'error');
        return;
    }
    
    const optimizePrompt = '请帮我优化以下提示词，使其更加清晰、具体、易于AI理解。保持原意的同时，增强提示词的结构性和完整性。优化后的提示词应该：\n1. 明确任务目标\n2. 提供必要的上下文\n3. 指定期望的输出格式\n4. 添加必要的约束条件\n\n原始提示词：\n' + promptText + '\n\n请直接返回优化后的提示词，不需要解释优化过程。';
    
    input.value = optimizePrompt;
    input.focus();
    
    showToast('已生成优化请求，点击发送按钮开始优化', 'success');
}

async function optimizePromptWithAI() {
    const input = document.getElementById('chat-input');
    const promptText = input ? input.value.trim() : '';
    
    if (!promptText) {
        showToast('请输入需要优化的提示词', 'error');
        return;
    }
    
    const optimizePrompt = '作为一个提示词优化专家，请优化以下提示词。要求：\n1. 保持原意不变\n2. 增强清晰度和具体性\n3. 添加必要的上下文和约束\n4. 改善结构化表达\n\n原始提示词：\n' + promptText + '\n\n请按以下格式返回：\n【优化后的提示词】\n(这里放优化后的内容)\n\n【优化说明】\n(简要说明做了哪些改进)';
    
    input.value = optimizePrompt;
    
    await sendMessage();
}
