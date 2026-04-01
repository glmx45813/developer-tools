
document.addEventListener('DOMContentLoaded', function() {
    initInputModeListeners();
});

function initInputModeListeners() {
    const headerPlaceholders = {
        json: '{"Content-Type": "application/json", "Authorization": "Bearer xxx"}',
        kv: 'Content-Type: application/json\nAuthorization: Bearer xxx'
    };
    
    const bodyPlaceholders = {
        json: '{"key": "value", "name": "test"}',
        form: 'key=value\nname=test',
        raw: '原始文本内容'
    };
    
    const headerModes = ['http-headers-mode', 'stress-headers-mode'];
    headerModes.forEach(function(modeName) {
        const radios = document.querySelectorAll('input[name="' + modeName + '"]');
        radios.forEach(function(radio) {
            radio.addEventListener('change', function() {
                const textareaId = modeName.replace('-headers-mode', '') + '-headers';
                const textarea = document.getElementById(textareaId);
                if (textarea) {
                    textarea.placeholder = headerPlaceholders[this.value] || headerPlaceholders.json;
                }
            });
        });
    });
    
    const bodyModes = ['http-body-mode', 'stress-body-mode'];
    bodyModes.forEach(function(modeName) {
        const radios = document.querySelectorAll('input[name="' + modeName + '"]');
        radios.forEach(function(radio) {
            radio.addEventListener('change', function() {
                const textareaId = modeName.replace('-body-mode', '') + '-body';
                const textarea = document.getElementById(textareaId);
                if (textarea) {
                    textarea.placeholder = bodyPlaceholders[this.value] || bodyPlaceholders.json;
                }
            });
        });
    });
}

async function rsaGenerateKeyPair() {
    const keySizeEl = document.getElementById('rsa-keysize');
    const keySize = keySizeEl ? parseInt(keySizeEl.value) : 2048;
    
    try {
        const response = await fetch(API_BASE + '/api/rsa/generate-keypair?keySize=' + keySize);
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('rsa-public-key').value = data.data.publicKey;
        document.getElementById('rsa-private-key').value = data.data.privateKey;
        showToast('密钥对已生成');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function optimizePrompt() {
    const inputEl = document.getElementById('prompt-input');
    const domainEl = document.getElementById('prompt-domain');
    const maxLengthEl = document.getElementById('prompt-max-length');
    const enhanceKeywordsEl = document.getElementById('prompt-enhance-keywords');
    const addStructureEl = document.getElementById('prompt-add-structure');
    const improveClarityEl = document.getElementById('prompt-improve-clarity');
    
    if (!inputEl || !inputEl.value.trim()) {
        showToast('请输入需要优化的提示词', 'error');
        return;
    }
    
    const originalPrompt = inputEl.value.trim();
    const domain = domainEl ? domainEl.value : 'general';
    const maxLength = maxLengthEl ? parseInt(maxLengthEl.value) : 500;
    const enhanceKeywords = enhanceKeywordsEl ? enhanceKeywordsEl.checked : true;
    const addStructure = addStructureEl ? addStructureEl.checked : true;
    const improveClarity = improveClarityEl ? improveClarityEl.checked : true;
    
    const domainNames = {
        'general': '通用',
        'programming': '编程开发',
        'writing': '写作创作',
        'analysis': '数据分析',
        'translation': '翻译',
        'marketing': '营销策划',
        'education': '教育培训',
        'legal': '法律咨询',
        'medical': '医疗健康',
        'finance': '金融财务'
    };
    
    let optimizationInstructions = [];
    if (enhanceKeywords) optimizationInstructions.push('关键词增强');
    if (addStructure) optimizationInstructions.push('添加结构');
    if (improveClarity) optimizationInstructions.push('改进清晰度');
    
    const optimizedPrompt = enhancePromptLocally(originalPrompt, domain, maxLength, {
        enhanceKeywords,
        addStructure,
        improveClarity
    });
    
    const resultEl = document.getElementById('prompt-result');
    const optimizedEl = document.getElementById('prompt-optimized');
    const clarityScoreEl = document.getElementById('prompt-clarity-score');
    const completenessScoreEl = document.getElementById('prompt-completeness-score');
    const relevanceScoreEl = document.getElementById('prompt-relevance-score');
    const keywordsEl = document.getElementById('prompt-keywords');
    const suggestionsEl = document.getElementById('prompt-suggestions');
    
    if (optimizedEl) optimizedEl.value = optimizedPrompt;
    
    const scores = calculatePromptScores(originalPrompt, optimizedPrompt);
    if (clarityScoreEl) clarityScoreEl.textContent = scores.clarity;
    if (completenessScoreEl) completenessScoreEl.textContent = scores.completeness;
    if (relevanceScoreEl) relevanceScoreEl.textContent = scores.relevance;
    
    const keywords = extractKeywords(originalPrompt);
    if (keywordsEl) {
        keywordsEl.innerHTML = keywords.map(function(kw) {
            return '<span class="keyword-tag">' + kw + '</span>';
        }).join('');
    }
    
    const suggestions = generateSuggestions(originalPrompt, optimizedPrompt);
    if (suggestionsEl) {
        suggestionsEl.innerHTML = suggestions.map(function(s) {
            return '<li class="list-group-item"><i class="bi bi-check-circle text-success me-2"></i>' + s + '</li>';
        }).join('');
    }
    
    if (resultEl) resultEl.style.display = 'block';
    
    showToast('提示词优化完成');
}

function enhancePromptLocally(prompt, domain, maxLength, options) {
    let enhanced = prompt;
    
    if (options.addStructure) {
        if (!prompt.includes('请') && !prompt.includes('帮我') && !prompt.includes('需要')) {
            enhanced = '请' + enhanced;
        }
        
        if (!prompt.includes('输出') && !prompt.includes('格式') && !prompt.includes('结果')) {
            enhanced += '\n\n请提供详细、结构化的回答。';
        }
    }
    
    if (options.enhanceKeywords) {
        const domainKeywords = {
            'programming': ['代码', '实现', '功能', '优化', '性能', '错误处理'],
            'writing': ['内容', '风格', '语气', '结构', '创意', '读者'],
            'analysis': ['数据', '分析', '趋势', '洞察', '可视化', '结论'],
            'translation': ['翻译', '准确', '地道', '语境', '文化', '表达'],
            'marketing': ['目标', '受众', '策略', '渠道', '转化', '品牌'],
            'education': ['学习', '理解', '示例', '练习', '知识点', '难度'],
            'legal': ['法律', '条款', '合规', '权利', '义务', '风险'],
            'medical': ['症状', '诊断', '治疗', '预防', '健康', '注意'],
            'finance': ['投资', '风险', '收益', '市场', '策略', '分析']
        };
        
        const keywords = domainKeywords[domain] || [];
        if (keywords.length > 0 && !keywords.some(function(kw) { return prompt.includes(kw); })) {
            enhanced = '[领域：' + domain + ']\n' + enhanced;
        }
    }
    
    if (options.improveClarity) {
        enhanced = enhanced.replace(/很|非常|特别/g, '十分');
        enhanced = enhanced.replace(/\s+/g, ' ').trim();
    }
    
    if (maxLength > 0 && enhanced.length > maxLength) {
        enhanced = enhanced.substring(0, maxLength) + '...';
    }
    
    return enhanced;
}

function calculatePromptScores(original, optimized) {
    const originalLength = original.length;
    const optimizedLength = optimized.length;
    const lengthImprovement = Math.min((optimizedLength / Math.max(originalLength, 1)) * 50, 50);
    
    const hasStructure = optimized.includes('\n') || optimized.includes('：') || optimized.includes(':');
    const structureScore = hasStructure ? 30 : 15;
    
    const hasKeywords = optimized.includes('请') || optimized.includes('需要') || optimized.includes('要求');
    const keywordScore = hasKeywords ? 20 : 10;
    
    const clarity = Math.min(Math.round(60 + lengthImprovement * 0.4 + structureScore * 0.3), 95);
    const completeness = Math.min(Math.round(50 + structureScore + keywordScore), 92);
    const relevance = Math.min(Math.round(70 + keywordScore * 0.5 + lengthImprovement * 0.3), 98);
    
    return { clarity, completeness, relevance };
}

function extractKeywords(prompt) {
    const stopWords = ['的', '是', '在', '了', '和', '与', '或', '有', '一个', '这个', '那个', '我', '你', '他', '她', '它'];
    const words = prompt.match(/[\u4e00-\u9fa5]{2,}/g) || [];
    
    const frequency = {};
    words.forEach(function(word) {
        if (!stopWords.includes(word) && word.length >= 2) {
            frequency[word] = (frequency[word] || 0) + 1;
        }
    });
    
    return Object.keys(frequency)
        .sort(function(a, b) { return frequency[b] - frequency[a]; })
        .slice(0, 5);
}

function generateSuggestions(original, optimized) {
    const suggestions = [];
    
    if (optimized.length > original.length * 1.2) {
        suggestions.push('增加了更多上下文信息，使提示词更加完整');
    }
    
    if (optimized.includes('\n')) {
        suggestions.push('添加了结构化格式，提高了可读性');
    }
    
    if (optimized.includes('请') && !original.includes('请')) {
        suggestions.push('添加了礼貌用语，改善交互体验');
    }
    
    if (optimized.includes('输出') || optimized.includes('格式')) {
        suggestions.push('明确了输出要求，便于获得期望结果');
    }
    
    if (suggestions.length === 0) {
        suggestions.push('提示词已优化，结构更加清晰');
        suggestions.push('建议：可以添加更多具体的要求或示例');
    }
    
    return suggestions;
}


async function rsaEncrypt() {
    const textEl = document.getElementById('rsa-encrypt-input');
    const publicKeyEl = document.getElementById('rsa-public-key');
    const paddingEl = document.getElementById('rsa-padding');
    
    if (!textEl || !publicKeyEl) return;
    
    const text = textEl.value;
    const publicKey = publicKeyEl.value;
    
    if (!text) {
        showToast('请输入要加密的文本', 'error');
        return;
    }
    if (!publicKey) {
        showToast('请先生成或输入公钥', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/rsa/encrypt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: text,
                publicKey: publicKey,
                padding: paddingEl ? paddingEl.value : 'PKCS1'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('rsa-result').value = data.data.encryptedData;
        document.getElementById('rsa-result-container').style.display = 'block';
        showToast('加密成功');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function rsaDecrypt() {
    const encryptedDataEl = document.getElementById('rsa-decrypt-input');
    const privateKeyEl = document.getElementById('rsa-private-key');
    const paddingEl = document.getElementById('rsa-padding');
    
    if (!encryptedDataEl || !privateKeyEl) return;
    
    const encryptedData = encryptedDataEl.value;
    const privateKey = privateKeyEl.value;
    
    if (!encryptedData) {
        showToast('请输入加密数据', 'error');
        return;
    }
    if (!privateKey) {
        showToast('请输入私钥', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/rsa/decrypt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                encryptedData: encryptedData,
                privateKey: privateKey,
                padding: paddingEl ? paddingEl.value : 'PKCS1'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('rsa-result').value = data.data.decryptedText;
        document.getElementById('rsa-result-container').style.display = 'block';
        showToast('解密成功');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function rsaSign() {
    const textEl = document.getElementById('rsa-sign-input');
    const privateKeyEl = document.getElementById('rsa-private-key');
    const algorithmEl = document.getElementById('rsa-sign-algorithm');
    
    if (!textEl || !privateKeyEl) return;
    
    const text = textEl.value;
    const privateKey = privateKeyEl.value;
    
    if (!text) {
        showToast('请输入要签名的文本', 'error');
        return;
    }
    if (!privateKey) {
        showToast('请输入私钥', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/rsa/sign', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: text,
                privateKey: privateKey,
                algorithm: algorithmEl ? algorithmEl.value : 'SHA256withRSA'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('rsa-signature-result').value = data.data.signature;
        document.getElementById('rsa-signature-container').style.display = 'block';
        showToast('签名成功');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function rsaVerify() {
    const textEl = document.getElementById('rsa-verify-input');
    const signatureEl = document.getElementById('rsa-verify-signature');
    const publicKeyEl = document.getElementById('rsa-public-key');
    const algorithmEl = document.getElementById('rsa-sign-algorithm');
    
    if (!textEl || !signatureEl || !publicKeyEl) return;
    
    const text = textEl.value;
    const signature = signatureEl.value;
    const publicKey = publicKeyEl.value;
    
    if (!text || !signature || !publicKey) {
        showToast('请填写所有必填项', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/rsa/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: text,
                signature: signature,
                publicKey: publicKey,
                algorithm: algorithmEl ? algorithmEl.value : 'SHA256withRSA'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const resultEl = document.getElementById('rsa-verify-result');
        if (data.data.verified) {
            resultEl.innerHTML = '<div class="alert alert-success"><i class="bi bi-check-circle"></i> 签名验证通过</div>';
        } else {
            resultEl.innerHTML = '<div class="alert alert-danger"><i class="bi bi-x-circle"></i> 签名验证失败</div>';
        }
        resultEl.style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jwtGenerate() {
    const secretEl = document.getElementById('jwt-secret');
    const subjectEl = document.getElementById('jwt-subject');
    const issuerEl = document.getElementById('jwt-issuer');
    const audienceEl = document.getElementById('jwt-audience');
    const expirationEl = document.getElementById('jwt-expiration');
    const algorithmEl = document.getElementById('jwt-algorithm');
    const claimsEl = document.getElementById('jwt-claims');
    
    if (!secretEl || !secretEl.value) {
        showToast('请输入密钥', 'error');
        return;
    }
    
    let claims = null;
    if (claimsEl && claimsEl.value) {
        try {
            claims = JSON.parse(claimsEl.value);
        } catch (e) {
            showToast('自定义Claims格式错误', 'error');
            return;
        }
    }
    
    try {
        const response = await fetch(API_BASE + '/api/jwt/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                secret: secretEl.value,
                subject: subjectEl ? subjectEl.value : '',
                issuer: issuerEl ? issuerEl.value : '',
                audience: audienceEl ? audienceEl.value : '',
                expiration: expirationEl ? parseInt(expirationEl.value) : null,
                algorithm: algorithmEl ? algorithmEl.value : 'HS256',
                claims: claims
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('jwt-token').value = data.data.token;
        showToast('Token生成成功');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jwtDecode() {
    const tokenEl = document.getElementById('jwt-token-decode');
    if (!tokenEl || !tokenEl.value) {
        showToast('请输入Token', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/jwt/decode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token: tokenEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('jwt-header').value = JSON.stringify(JSON.parse(data.data.header), null, 2);
        document.getElementById('jwt-payload').value = JSON.stringify(JSON.parse(data.data.payload), null, 2);
        document.getElementById('jwt-signature').value = data.data.signature;
        document.getElementById('jwt-decode-result').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jwtVerify() {
    const tokenEl = document.getElementById('jwt-token-decode');
    const secretEl = document.getElementById('jwt-secret-verify');
    
    if (!tokenEl || !tokenEl.value) {
        showToast('请输入Token', 'error');
        return;
    }
    if (!secretEl || !secretEl.value) {
        showToast('请输入密钥', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/jwt/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                token: tokenEl.value,
                secret: secretEl.value
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const resultEl = document.getElementById('jwt-verify-result');
        if (data.data.valid) {
            resultEl.innerHTML = '<div class="alert alert-success"><i class="bi bi-check-circle"></i> ' + data.data.message + '</div>';
        } else {
            resultEl.innerHTML = '<div class="alert alert-danger"><i class="bi bi-x-circle"></i> ' + data.data.message + '</div>';
        }
        resultEl.style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function unicodeEncode() {
    const textEl = document.getElementById('unicode-input');
    const formatEl = document.getElementById('unicode-format');
    const chineseOnlyEl = document.getElementById('unicode-chinese-only');
    
    if (!textEl || !textEl.value) {
        showToast('请输入文本', 'error');
        return;
    }
    
    const chineseOnly = chineseOnlyEl ? chineseOnlyEl.checked : false;
    const url = chineseOnly ? '/api/unicode/encode-chinese' : '/api/unicode/encode';
    
    try {
        const response = await fetch(API_BASE + url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: textEl.value,
                format: formatEl ? formatEl.value : '\\u'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('unicode-result').value = data.data.encoded;
        document.getElementById('unicode-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function unicodeDecode() {
    const textEl = document.getElementById('unicode-input');
    
    if (!textEl || !textEl.value) {
        showToast('请输入编码文本', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/unicode/decode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: textEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('unicode-result').value = data.data.decoded;
        document.getElementById('unicode-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function htmlEntityEncode() {
    const textEl = document.getElementById('html-entity-input');
    const modeEl = document.getElementById('html-entity-mode');
    
    if (!textEl || !textEl.value) {
        showToast('请输入文本', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/html-entity/encode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text: textEl.value,
                mode: modeEl ? modeEl.value : 'named'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('html-entity-result').value = data.data.result;
        document.getElementById('html-entity-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function htmlEntityDecode() {
    const textEl = document.getElementById('html-entity-input');
    
    if (!textEl || !textEl.value) {
        showToast('请输入编码文本', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/html-entity/decode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: textEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('html-entity-result').value = data.data.result;
        document.getElementById('html-entity-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function diffCompare() {
    const text1El = document.getElementById('diff-text1');
    const text2El = document.getElementById('diff-text2');
    const modeEl = document.getElementById('diff-mode');
    
    if (!text1El || !text2El) return;
    
    try {
        const response = await fetch(API_BASE + '/api/diff/html', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                text1: text1El.value,
                text2: text2El.value,
                mode: modeEl ? modeEl.value : 'line'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('diff-result').innerHTML = data.data;
        document.getElementById('diff-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function xmlFormat() {
    const xmlEl = document.getElementById('xml-input');
    const indentEl = document.getElementById('xml-indent');
    
    if (!xmlEl || !xmlEl.value) {
        showToast('请输入XML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/xml/format', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                xml: xmlEl.value,
                indent: indentEl ? parseInt(indentEl.value) : 2
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('xml-result').value = data.data.result;
        document.getElementById('xml-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function xmlCompress() {
    const xmlEl = document.getElementById('xml-input');
    
    if (!xmlEl || !xmlEl.value) {
        showToast('请输入XML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/xml/compress', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ xml: xmlEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('xml-result').value = data.data.result;
        document.getElementById('xml-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function xmlValidate() {
    const xmlEl = document.getElementById('xml-input');
    
    if (!xmlEl || !xmlEl.value) {
        showToast('请输入XML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/xml/validate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ xml: xmlEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        if (data.data.valid) {
            showToast('XML格式有效');
        } else {
            showToast(data.data.error, 'error');
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function xmlToJson() {
    const xmlEl = document.getElementById('xml-input');
    
    if (!xmlEl || !xmlEl.value) {
        showToast('请输入XML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/xml/to-json', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ xml: xmlEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('xml-result').value = data.data.result;
        document.getElementById('xml-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function yamlFormat() {
    const yamlEl = document.getElementById('yaml-input');
    const indentEl = document.getElementById('yaml-indent');
    
    if (!yamlEl || !yamlEl.value) {
        showToast('请输入YAML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/yaml/format', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                yaml: yamlEl.value,
                indent: indentEl ? parseInt(indentEl.value) : 2
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('yaml-result').value = data.data.result;
        document.getElementById('yaml-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function yamlValidate() {
    const yamlEl = document.getElementById('yaml-input');
    
    if (!yamlEl || !yamlEl.value) {
        showToast('请输入YAML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/yaml/validate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ yaml: yamlEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        if (data.data.valid) {
            showToast('YAML格式有效');
        } else {
            showToast(data.data.error, 'error');
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function yamlToJson() {
    const yamlEl = document.getElementById('yaml-input');
    
    if (!yamlEl || !yamlEl.value) {
        showToast('请输入YAML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/yaml/to-json', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ yaml: yamlEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('yaml-result').value = data.data.result;
        document.getElementById('yaml-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jsonToYaml() {
    const jsonEl = document.getElementById('yaml-input');
    
    if (!jsonEl || !jsonEl.value) {
        showToast('请输入JSON内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/yaml/from-json', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ json: jsonEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('yaml-result').value = data.data.result;
        document.getElementById('yaml-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function cronGenerate() {
    const secondEl = document.getElementById('cron-second');
    const minuteEl = document.getElementById('cron-minute');
    const hourEl = document.getElementById('cron-hour');
    const dayOfMonthEl = document.getElementById('cron-day-of-month');
    const monthEl = document.getElementById('cron-month');
    const dayOfWeekEl = document.getElementById('cron-day-of-week');
    
    try {
        const response = await fetch(API_BASE + '/api/cron/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                second: secondEl ? secondEl.value : '0',
                minute: minuteEl ? minuteEl.value : '*',
                hour: hourEl ? hourEl.value : '*',
                dayOfMonth: dayOfMonthEl ? dayOfMonthEl.value : '*',
                month: monthEl ? monthEl.value : '*',
                dayOfWeek: dayOfWeekEl ? dayOfWeekEl.value : '*'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('cron-expression').value = data.data.expression;
        document.getElementById('cron-description').textContent = data.data.description;
        
        const nextExecutionsEl = document.getElementById('cron-next-executions');
        if (nextExecutionsEl && data.data.nextExecutions) {
            nextExecutionsEl.innerHTML = data.data.nextExecutions.map(function(time) {
                return '<li class="list-group-item">' + time + '</li>';
            }).join('');
        }
        
        document.getElementById('cron-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function cronParse() {
    const expressionEl = document.getElementById('cron-expression');
    
    if (!expressionEl || !expressionEl.value) {
        showToast('请输入Cron表达式', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/cron/parse', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ expression: expressionEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('cron-description').textContent = data.data.description;
        
        const nextExecutionsEl = document.getElementById('cron-next-executions');
        if (nextExecutionsEl && data.data.nextExecutions) {
            nextExecutionsEl.innerHTML = data.data.nextExecutions.map(function(time) {
                return '<li class="list-group-item">' + time + '</li>';
            }).join('');
        }
        
        document.getElementById('cron-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadCronPresets() {
    try {
        const response = await fetch(API_BASE + '/api/cron/presets');
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const container = document.getElementById('cron-presets');
        if (container && data.data) {
            container.innerHTML = data.data.map(function(preset) {
                return '<button class="btn btn-outline-primary btn-sm" onclick="useCronPreset(\'' + 
                    preset.expression + '\')">' + preset.name + '</button>';
            }).join(' ');
        }
    } catch (error) {
        console.error('加载Cron预设失败', error);
    }
}

function useCronPreset(expression) {
    const expressionEl = document.getElementById('cron-expression');
    if (expressionEl) {
        expressionEl.value = expression;
        cronParse();
    }
}

async function sqlFormat() {
    const sqlEl = document.getElementById('sql-input');
    const indentEl = document.getElementById('sql-indent');
    
    if (!sqlEl || !sqlEl.value) {
        showToast('请输入SQL内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/sql/format', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sql: sqlEl.value,
                indentSize: indentEl ? parseInt(indentEl.value) : 2
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('sql-result').value = data.data.result;
        document.getElementById('sql-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function sqlCompress() {
    const sqlEl = document.getElementById('sql-input');
    
    if (!sqlEl || !sqlEl.value) {
        showToast('请输入SQL内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/sql/compress', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sql: sqlEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('sql-result').value = data.data.result;
        document.getElementById('sql-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function sqlValidate() {
    const sqlEl = document.getElementById('sql-input');
    
    if (!sqlEl || !sqlEl.value) {
        showToast('请输入SQL内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/sql/validate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sql: sqlEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        if (data.data.valid) {
            showToast('SQL语法验证通过');
        } else {
            showToast(data.data.error, 'error');
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function propertiesToYaml() {
    const inputEl = document.getElementById('properties-input');
    if (!inputEl || !inputEl.value) {
        showToast('请输入Properties内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/properties/to-yaml', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: inputEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('properties-result').value = data.data.result;
        document.getElementById('properties-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function propertiesToJson() {
    const inputEl = document.getElementById('properties-input');
    if (!inputEl || !inputEl.value) {
        showToast('请输入Properties内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/properties/to-json', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: inputEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('properties-result').value = data.data.result;
        document.getElementById('properties-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function yamlToProperties() {
    const inputEl = document.getElementById('properties-input');
    if (!inputEl || !inputEl.value) {
        showToast('请输入YAML内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/properties/yaml-to-properties', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: inputEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('properties-result').value = data.data.result;
        document.getElementById('properties-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function jsonToProperties() {
    const inputEl = document.getElementById('properties-input');
    if (!inputEl || !inputEl.value) {
        showToast('请输入JSON内容', 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/properties/json-to-properties', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: inputEl.value })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        document.getElementById('properties-result').value = data.data.result;
        document.getElementById('properties-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function runStressTest() {
    const urlEl = document.getElementById('stress-url');
    const methodEl = document.getElementById('stress-method');
    const threadsEl = document.getElementById('stress-threads');
    const requestsPerThreadEl = document.getElementById('stress-requests-per-thread');
    const headersEl = document.getElementById('stress-headers');
    const bodyEl = document.getElementById('stress-body');
    const contentTypeEl = document.getElementById('stress-content-type');
    const btnEl = document.getElementById('stress-test-btn');
    
    if (!urlEl || !urlEl.value) {
        showToast('请输入URL', 'error');
        return;
    }
    
    const headersMode = getInputMode('stress-headers-mode');
    const bodyMode = getInputMode('stress-body-mode');
    
    let headers = {};
    try {
        headers = parseHeadersInput(headersEl ? headersEl.value : '', headersMode);
    } catch (e) {
        showToast(e.message, 'error');
        return;
    }
    
    let body = '';
    try {
        body = parseBodyInput(bodyEl ? bodyEl.value : '', bodyMode, contentTypeEl ? contentTypeEl.value : 'application/json');
    } catch (e) {
        showToast(e.message, 'error');
        return;
    }
    
    btnEl.disabled = true;
    btnEl.innerHTML = '<i class="bi bi-hourglass-split"></i> 压测中...';
    
    try {
        const response = await fetch(API_BASE + '/api/stress-test/run', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                url: urlEl.value,
                method: methodEl ? methodEl.value : 'GET',
                threads: threadsEl ? parseInt(threadsEl.value) : 10,
                requestsPerThread: requestsPerThreadEl ? parseInt(requestsPerThreadEl.value) : 10,
                headers: headers,
                body: body,
                contentType: contentTypeEl ? contentTypeEl.value : 'application/json'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const result = data.data;
        
        document.getElementById('stress-total-requests').textContent = result.totalRequests;
        document.getElementById('stress-success').textContent = result.successCount;
        document.getElementById('stress-failure').textContent = result.failureCount;
        document.getElementById('stress-qps').textContent = result.requestsPerSecond.toFixed(2);
        document.getElementById('stress-total-time').textContent = result.totalTime;
        document.getElementById('stress-avg-time').textContent = result.averageResponseTime.toFixed(2);
        document.getElementById('stress-min-time').textContent = result.minResponseTime;
        document.getElementById('stress-max-time').textContent = result.maxResponseTime;
        
        if (result.percentiles) {
            document.getElementById('stress-p50').textContent = result.percentiles.p50 + 'ms';
            document.getElementById('stress-p75').textContent = result.percentiles.p75 + 'ms';
            document.getElementById('stress-p90').textContent = result.percentiles.p90 + 'ms';
            document.getElementById('stress-p95-row').textContent = result.percentiles.p95 + 'ms';
            document.getElementById('stress-p99-row').textContent = result.percentiles.p99 + 'ms';
            document.getElementById('stress-p95').textContent = result.percentiles.p95;
            document.getElementById('stress-p99').textContent = result.percentiles.p99;
        }
        
        const statusCodesEl = document.getElementById('stress-status-codes');
        if (statusCodesEl && result.statusCodes) {
            statusCodesEl.innerHTML = '';
            for (const [code, count] of Object.entries(result.statusCodes)) {
                const badge = document.createElement('span');
                badge.className = 'badge bg-' + (code.startsWith('2') ? 'success' : code.startsWith('4') ? 'warning' : code.startsWith('5') ? 'danger' : 'secondary');
                badge.textContent = code + ': ' + count;
                statusCodesEl.appendChild(badge);
            }
        }
        
        document.getElementById('stress-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        btnEl.disabled = false;
        btnEl.innerHTML = '<i class="bi bi-play-fill"></i> 开始压测';
    }
}

let currentImageBase64 = null;
let currentImageFormat = null;

async function compressImage() {
    const fileEl = document.getElementById('image-compress-file');
    const qualityEl = document.getElementById('image-quality');
    const maxWidthEl = document.getElementById('image-max-width');
    const maxHeightEl = document.getElementById('image-max-height');
    
    if (!fileEl || !fileEl.files || fileEl.files.length === 0) {
        showToast('请选择图片文件', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileEl.files[0]);
    formData.append('quality', qualityEl ? qualityEl.value : '0.8');
    if (maxWidthEl && maxWidthEl.value) formData.append('maxWidth', maxWidthEl.value);
    if (maxHeightEl && maxHeightEl.value) formData.append('maxHeight', maxHeightEl.value);
    
    try {
        const response = await fetch(API_BASE + '/api/image/compress', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        displayImageResult(data.data);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function convertImage() {
    const fileEl = document.getElementById('image-convert-file');
    const formatEl = document.getElementById('image-target-format');
    
    if (!fileEl || !fileEl.files || fileEl.files.length === 0) {
        showToast('请选择图片文件', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileEl.files[0]);
    formData.append('format', formatEl ? formatEl.value : 'png');
    
    try {
        const response = await fetch(API_BASE + '/api/image/convert', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        displayImageResult(data.data);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function resizeImage() {
    const fileEl = document.getElementById('image-resize-file');
    const widthEl = document.getElementById('image-resize-width');
    const heightEl = document.getElementById('image-resize-height');
    const keepRatioEl = document.getElementById('image-keep-ratio');
    
    if (!fileEl || !fileEl.files || fileEl.files.length === 0) {
        showToast('请选择图片文件', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileEl.files[0]);
    if (widthEl && widthEl.value) formData.append('width', widthEl.value);
    if (heightEl && heightEl.value) formData.append('height', heightEl.value);
    formData.append('keepRatio', keepRatioEl ? keepRatioEl.value : 'true');
    
    try {
        const response = await fetch(API_BASE + '/api/image/resize', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        displayImageResult(data.data);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function rotateImage() {
    const fileEl = document.getElementById('image-rotate-file');
    const angleEl = document.getElementById('image-rotate-angle');
    
    if (!fileEl || !fileEl.files || fileEl.files.length === 0) {
        showToast('请选择图片文件', 'error');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', fileEl.files[0]);
    formData.append('angle', angleEl ? angleEl.value : '90');
    
    try {
        const response = await fetch(API_BASE + '/api/image/rotate', {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        displayImageResult(data.data);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function displayImageResult(result) {
    currentImageBase64 = result.base64;
    currentImageFormat = result.format;
    
    document.getElementById('image-original-size').textContent = formatFileSize(result.originalSize);
    document.getElementById('image-result-size').textContent = formatFileSize(result.resultSize);
    document.getElementById('image-original-dimension').textContent = result.originalWidth + 'x' + result.originalHeight;
    document.getElementById('image-result-dimension').textContent = result.resultWidth + 'x' + result.resultHeight;
    
    document.getElementById('image-preview').src = 'data:' + result.mimeType + ';base64,' + result.base64;
    document.getElementById('image-result-container').style.display = 'block';
}

function downloadImage() {
    if (!currentImageBase64 || !currentImageFormat) {
        showToast('没有可下载的图片', 'error');
        return;
    }
    
    const link = document.createElement('a');
    link.href = 'data:image/' + currentImageFormat + ';base64,' + currentImageBase64;
    link.download = 'processed-image.' + currentImageFormat;
    link.click();
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
}

function parseHeadersInput(text, mode) {
    if (!text || !text.trim()) return {};
    
    if (mode === 'json') {
        try {
            return JSON.parse(text);
        } catch (e) {
            throw new Error('请求头JSON格式错误');
        }
    } else if (mode === 'kv') {
        const headers = {};
        const lines = text.split('\n');
        for (const line of lines) {
            const trimmedLine = line.trim();
            if (!trimmedLine || trimmedLine.startsWith('#')) continue;
            
            const separatorIndex = Math.max(
                trimmedLine.indexOf(':'),
                trimmedLine.indexOf('=')
            );
            
            if (separatorIndex > 0) {
                const key = trimmedLine.substring(0, separatorIndex).trim();
                const value = trimmedLine.substring(separatorIndex + 1).trim();
                if (key) {
                    headers[key] = value;
                }
            }
        }
        return headers;
    }
    return {};
}

function parseBodyInput(text, mode, contentType) {
    if (!text || !text.trim()) return '';
    
    if (mode === 'json') {
        try {
            JSON.parse(text);
            return text;
        } catch (e) {
            throw new Error('请求体JSON格式错误');
        }
    } else if (mode === 'form') {
        const params = [];
        const lines = text.split('\n');
        for (const line of lines) {
            const trimmedLine = line.trim();
            if (!trimmedLine || trimmedLine.startsWith('#')) continue;
            
            const separatorIndex = trimmedLine.indexOf('=');
            if (separatorIndex > 0) {
                const key = trimmedLine.substring(0, separatorIndex).trim();
                const value = trimmedLine.substring(separatorIndex + 1).trim();
                if (key) {
                    params.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
                }
            }
        }
        return params.join('&');
    } else if (mode === 'raw') {
        return text;
    }
    return text;
}

function getInputMode(name) {
    const radio = document.querySelector('input[name="' + name + '"]:checked');
    return radio ? radio.value : 'json';
}

async function httpSendRequest() {
    const urlEl = document.getElementById('http-url');
    const methodEl = document.getElementById('http-method');
    const headersEl = document.getElementById('http-headers');
    const bodyEl = document.getElementById('http-body');
    const contentTypeEl = document.getElementById('http-content-type');
    
    if (!urlEl || !urlEl.value) {
        showToast('请输入URL', 'error');
        return;
    }
    
    const headersMode = getInputMode('http-headers-mode');
    const bodyMode = getInputMode('http-body-mode');
    
    let headers = {};
    try {
        headers = parseHeadersInput(headersEl ? headersEl.value : '', headersMode);
    } catch (e) {
        showToast(e.message, 'error');
        return;
    }
    
    let body = '';
    try {
        body = parseBodyInput(bodyEl ? bodyEl.value : '', bodyMode, contentTypeEl ? contentTypeEl.value : 'application/json');
    } catch (e) {
        showToast(e.message, 'error');
        return;
    }
    
    try {
        const response = await fetch(API_BASE + '/api/http/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                url: urlEl.value,
                method: methodEl ? methodEl.value : 'GET',
                headers: headers,
                body: body,
                contentType: contentTypeEl ? contentTypeEl.value : 'application/json'
            })
        });
        const data = await response.json();
        
        if (data.code !== 200) throw new Error(data.message);
        
        const result = data.data;
        
        document.getElementById('http-status').textContent = result.statusCode + ' ' + result.statusMessage;
        document.getElementById('http-time').textContent = result.responseTime + 'ms';
        document.getElementById('http-size').textContent = result.size + ' bytes';
        
        if (result.headers) {
            document.getElementById('http-response-headers').value = JSON.stringify(result.headers, null, 2);
        }
        
        const responseBodyEl = document.getElementById('http-response-body');
        const formattedBody = formatResponseBody(result.body, result.contentType);
        responseBodyEl.value = formattedBody;
        document.getElementById('http-result-container').style.display = 'block';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function formatResponseBody(body, contentType) {
    if (!body) return '';
    
    if (contentType && contentType.includes('application/json')) {
        try {
            const parsed = JSON.parse(body);
            return JSON.stringify(parsed, null, 2);
        } catch (e) {
            return body;
        }
    }
    
    try {
        const parsed = JSON.parse(body);
        return JSON.stringify(parsed, null, 2);
    } catch (e) {
        return body;
    }
}
