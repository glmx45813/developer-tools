const USER_TEMPLATES_KEY = 'userPromptTemplates';
const USER_TEMPLATE_USAGE_KEY = 'userTemplateUsage';

let systemTemplates = [];
let userTemplates = [];
let allTemplates = [];
let currentTemplate = null;
let templateModal = null;
let useTemplateModal = null;
let detailModal = null;

document.addEventListener('DOMContentLoaded', function() {
    templateModal = new bootstrap.Modal(document.getElementById('templateModal'));
    useTemplateModal = new bootstrap.Modal(document.getElementById('useTemplateModal'));
    detailModal = new bootstrap.Modal(document.getElementById('detailModal'));
    
    loadCategories();
    loadTemplates();
});

function loadCategories() {
    fetch(API_BASE + '/api/prompt-template/categories')
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                renderCategories(data.data);
            }
        })
        .catch(error => {
            console.error('Failed to load categories:', error);
        });
}

function renderCategories(categories) {
    const container = document.getElementById('category-list');
    const existingItems = container.querySelectorAll('.nav-item:not(:first-child)');
    existingItems.forEach(item => item.remove());
    
    categories.forEach(function(category) {
        const li = document.createElement('li');
        li.className = 'nav-item';
        li.innerHTML = '<a class="nav-link" href="#" data-category="' + category + '" onclick="filterByCategory(\'' + category + '\')">' +
            '<i class="bi bi-folder2"></i>' +
            '<span>' + category + '</span>' +
            '</a>';
        container.appendChild(li);
    });
}

function loadTemplates() {
    fetch(API_BASE + '/api/prompt-template/list')
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                systemTemplates = data.data || [];
                userTemplates = loadUserTemplates();
                mergeAndRenderTemplates();
            }
        })
        .catch(error => {
            console.error('Failed to load templates:', error);
            showToast('加载模板失败', 'error');
        });
}

function loadUserTemplates() {
    try {
        const saved = localStorage.getItem(USER_TEMPLATES_KEY);
        return saved ? JSON.parse(saved) : [];
    } catch (e) {
        console.error('Failed to load user templates:', e);
        return [];
    }
}

function saveUserTemplates() {
    try {
        localStorage.setItem(USER_TEMPLATES_KEY, JSON.stringify(userTemplates));
    } catch (e) {
        console.error('Failed to save user templates:', e);
    }
}

function getUserTemplateUsage(templateId) {
    try {
        const saved = localStorage.getItem(USER_TEMPLATE_USAGE_KEY);
        const usage = saved ? JSON.parse(saved) : {};
        return usage[templateId] || 0;
    } catch (e) {
        return 0;
    }
}

function incrementUserTemplateUsage(templateId) {
    try {
        const saved = localStorage.getItem(USER_TEMPLATE_USAGE_KEY);
        const usage = saved ? JSON.parse(saved) : {};
        usage[templateId] = (usage[templateId] || 0) + 1;
        localStorage.setItem(USER_TEMPLATE_USAGE_KEY, JSON.stringify(usage));
    } catch (e) {
        console.error('Failed to update usage:', e);
    }
}

function mergeAndRenderTemplates() {
    allTemplates = [...systemTemplates];
    
    userTemplates.forEach(function(ut) {
        ut.isSystem = false;
        ut.usageCount = getUserTemplateUsage(ut.id);
        allTemplates.push(ut);
    });
    
    renderTemplates(allTemplates);
    renderHotTemplates();
}

function renderHotTemplates() {
    const container = document.getElementById('hot-templates');
    const hotSection = document.getElementById('hot-section');
    
    const sortedTemplates = [...allTemplates].sort(function(a, b) {
        return (b.usageCount || 0) - (a.usageCount || 0);
    }).slice(0, 5);
    
    if (sortedTemplates.length === 0) {
        hotSection.style.display = 'none';
        return;
    }
    
    hotSection.style.display = 'block';
    
    let html = '';
    sortedTemplates.forEach(function(template) {
        html += '<div class="hot-template-card" onclick="showDetail(\'' + template.id + '\')">' +
            '<div class="name">' + escapeHtml(template.name) + '</div>' +
            '<div class="usage"><i class="bi bi-fire"></i> ' + (template.usageCount || 0) + ' 次使用</div>' +
            '</div>';
    });
    
    container.innerHTML = html;
}

function renderTemplates(templates) {
    const container = document.getElementById('templates-grid');
    const countEl = document.getElementById('template-count');
    
    countEl.textContent = '共 ' + templates.length + ' 个模板';
    
    if (!templates || templates.length === 0) {
        container.innerHTML = '<div class="empty-state">' +
            '<i class="bi bi-inbox"></i>' +
            '<h5>暂无模板</h5>' +
            '<p>点击上方"创建模板"按钮添加您的第一个模板</p>' +
            '</div>';
        return;
    }
    
    let html = '';
    templates.forEach(function(template) {
        const preview = template.content.substring(0, 100) + (template.content.length > 100 ? '...' : '');
        const isSystem = template.isSystem !== false;
        const badge = isSystem ? '<span class="badge bg-secondary">系统</span>' : '<span class="badge bg-info">我的</span>';
        
        html += '<div class="template-card" onclick="showDetail(\'' + template.id + '\')">' +
            '<div class="card-header">' +
            '<h6 class="card-title">' + escapeHtml(template.name) + '</h6>' +
            '<span class="card-category">' + escapeHtml(template.category) + '</span>' +
            '</div>' +
            '<p class="card-desc">' + badge + ' ' + escapeHtml(template.description || '暂无描述') + '</p>' +
            '<div class="card-preview">' + escapeHtml(preview) + '</div>' +
            '<div class="card-footer">' +
            '<span class="card-usage"><i class="bi bi-eye"></i> ' + (template.usageCount || 0) + ' 次使用</span>' +
            '<div class="card-actions">';
        
        if (!isSystem) {
            html += '<button class="btn btn-sm btn-outline-secondary me-1" onclick="event.stopPropagation(); showEditModal(\'' + template.id + '\')" title="编辑">' +
                '<i class="bi bi-pencil"></i></button>' +
                '<button class="btn btn-sm btn-outline-danger me-1" onclick="event.stopPropagation(); deleteTemplate(\'' + template.id + '\')" title="删除">' +
                '<i class="bi bi-trash"></i></button>';
        }
        
        html += '<button class="btn btn-sm btn-outline-primary" onclick="event.stopPropagation(); openUseModal(\'' + template.id + '\')">' +
            '<i class="bi bi-play-fill"></i> 使用</button>' +
            '</div>' +
            '</div>' +
            '</div>';
    });
    
    container.innerHTML = html;
}

function filterByCategory(category) {
    const navLinks = document.querySelectorAll('#category-list .nav-link');
    navLinks.forEach(function(link) {
        link.classList.remove('active');
        if (link.dataset.category === category) {
            link.classList.add('active');
        }
    });
    
    if (!category) {
        renderTemplates(allTemplates);
    } else {
        const filtered = allTemplates.filter(function(t) {
            return t.category === category;
        });
        renderTemplates(filtered);
    }
}

function searchTemplates(keyword) {
    if (!keyword || keyword.trim() === '') {
        renderTemplates(allTemplates);
        return;
    }
    
    const lowerKeyword = keyword.toLowerCase();
    const filtered = allTemplates.filter(function(t) {
        return t.name.toLowerCase().includes(lowerKeyword) ||
               (t.description && t.description.toLowerCase().includes(lowerKeyword)) ||
               t.category.toLowerCase().includes(lowerKeyword);
    });
    
    renderTemplates(filtered);
}

function showCreateModal() {
    document.getElementById('modal-title').textContent = '创建模板';
    document.getElementById('template-form').reset();
    document.getElementById('template-id').value = '';
    templateModal.show();
}

function showEditModal(id) {
    const template = userTemplates.find(function(t) {
        return t.id === id;
    });
    
    if (!template) {
        showToast('模板不存在或无权编辑', 'error');
        return;
    }
    
    document.getElementById('modal-title').textContent = '编辑模板';
    document.getElementById('template-id').value = template.id;
    document.getElementById('template-name').value = template.name;
    document.getElementById('template-category').value = template.category;
    document.getElementById('template-description').value = template.description || '';
    document.getElementById('template-content').value = template.content;
    document.getElementById('template-tags').value = template.tags ? template.tags.join(', ') : '';
    
    templateModal.show();
}

function saveTemplate() {
    const id = document.getElementById('template-id').value;
    const name = document.getElementById('template-name').value.trim();
    const category = document.getElementById('template-category').value;
    const description = document.getElementById('template-description').value.trim();
    const content = document.getElementById('template-content').value.trim();
    const tagsStr = document.getElementById('template-tags').value.trim();
    
    if (!name || !category || !content) {
        showToast('请填写必填项', 'error');
        return;
    }
    
    const tags = tagsStr ? tagsStr.split(',').map(function(t) { return t.trim(); }).filter(function(t) { return t; }) : [];
    
    const template = {
        name: name,
        category: category,
        description: description,
        content: content,
        tags: tags,
        isSystem: false
    };
    
    if (id) {
        const index = userTemplates.findIndex(function(t) { return t.id === id; });
        if (index >= 0) {
            template.id = id;
            template.createdAt = userTemplates[index].createdAt;
            template.updatedAt = new Date().toISOString();
            userTemplates[index] = template;
            showToast('模板已更新', 'success');
        }
    } else {
        template.id = 'user-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
        template.createdAt = new Date().toISOString();
        template.updatedAt = template.createdAt;
        template.usageCount = 0;
        userTemplates.push(template);
        showToast('模板创建成功', 'success');
    }
    
    saveUserTemplates();
    templateModal.hide();
    mergeAndRenderTemplates();
}

function showDetail(id) {
    const template = allTemplates.find(function(t) {
        return t.id === id;
    });
    
    if (!template) {
        fetch(API_BASE + '/api/prompt-template/' + id)
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    currentTemplate = data.data;
                    currentTemplate.isSystem = true;
                    renderDetail(currentTemplate);
                    detailModal.show();
                }
            })
            .catch(error => {
                console.error('Failed to load template:', error);
                showToast('加载模板详情失败', 'error');
            });
    } else {
        currentTemplate = template;
        renderDetail(currentTemplate);
        detailModal.show();
    }
}

function renderDetail(template) {
    const isSystem = template.isSystem !== false;
    
    document.getElementById('detail-modal-title').textContent = template.name;
    document.getElementById('detail-name').textContent = template.name;
    document.getElementById('detail-category').textContent = template.category;
    document.getElementById('detail-description').textContent = template.description || '暂无描述';
    document.getElementById('detail-usage').textContent = (template.usageCount || 0) + ' 次';
    document.getElementById('detail-content').textContent = template.content;
    
    const footer = document.querySelector('#detailModal .modal-footer');
    let footerHtml = '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>' +
        '<button type="button" class="btn btn-outline-primary" onclick="copyDetailContent()">' +
        '<i class="bi bi-clipboard"></i> 复制</button>' +
        '<button type="button" class="btn btn-primary" onclick="useTemplateFromDetail()">' +
        '<i class="bi bi-play-fill"></i> 使用模板</button>';
    
    if (!isSystem) {
        footerHtml = '<button type="button" class="btn btn-outline-danger me-auto" onclick="deleteTemplate(\'' + template.id + '\')">' +
            '<i class="bi bi-trash"></i> 删除</button>' +
            '<button type="button" class="btn btn-outline-warning" onclick="showEditModal(\'' + template.id + '\'); detailModal.hide();">' +
            '<i class="bi bi-pencil"></i> 编辑</button>' +
            footerHtml;
    }
    
    footer.innerHTML = footerHtml;
}

function openUseModal(id) {
    const template = allTemplates.find(function(t) {
        return t.id === id;
    });
    
    if (template) {
        currentTemplate = template;
        incrementUserTemplateUsage(id);
        renderUseModal(currentTemplate);
        useTemplateModal.show();
    } else {
        fetch(API_BASE + '/api/prompt-template/' + id)
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    currentTemplate = data.data;
                    currentTemplate.isSystem = true;
                    renderUseModal(currentTemplate);
                    useTemplateModal.show();
                }
            })
            .catch(error => {
                console.error('Failed to load template:', error);
                showToast('加载模板失败', 'error');
            });
    }
}

function renderUseModal(template) {
    document.getElementById('use-modal-title').textContent = '使用模板: ' + template.name;
    document.getElementById('preview-template-name').textContent = template.name;
    document.getElementById('preview-template-desc').textContent = template.description || '';
    
    const variablesForm = document.getElementById('variables-form');
    const previewContent = document.getElementById('preview-content');
    
    const variablePattern = /\{\{(\w+)\}\}/g;
    const variables = [];
    let match;
    
    while ((match = variablePattern.exec(template.content)) !== null) {
        if (!variables.includes(match[1])) {
            variables.push(match[1]);
        }
    }
    
    if (variables.length === 0) {
        variablesForm.innerHTML = '<p class="text-muted">此模板无需填写变量</p>';
        previewContent.textContent = template.content;
    } else {
        let html = '';
        variables.forEach(function(variable) {
            html += '<div class="variable-item">' +
                '<label class="form-label">{{' + variable + '}}</label>' +
                '<textarea class="form-control" id="var-' + variable + '" rows="3" oninput="updatePreview()"></textarea>' +
                '</div>';
        });
        variablesForm.innerHTML = html;
        previewContent.textContent = template.content;
    }
}

function updatePreview() {
    if (!currentTemplate) return;
    
    let content = currentTemplate.content;
    const variablePattern = /\{\{(\w+)\}\}/g;
    let match;
    
    while ((match = variablePattern.exec(content)) !== null) {
        const input = document.getElementById('var-' + match[1]);
        if (input) {
            content = content.replace(new RegExp('\\{\\{' + match[1] + '\\}\\}', 'g'), input.value || '');
        }
    }
    
    document.getElementById('preview-content').textContent = content;
}

function copyTemplateContent() {
    const content = document.getElementById('preview-content').textContent;
    copyToClipboard(content);
}

function useInAIChat() {
    const content = document.getElementById('preview-content').textContent;
    localStorage.setItem('aiChatPrompt', content);
    useTemplateModal.hide();
    window.location.href = '/ai-chat';
}

function useTemplateFromDetail() {
    detailModal.hide();
    openUseModal(currentTemplate.id);
}

function copyDetailContent() {
    const content = document.getElementById('detail-content').textContent;
    copyToClipboard(content);
}

function deleteTemplate(id) {
    if (!confirm('确定要删除此模板吗？')) {
        return;
    }
    
    const index = userTemplates.findIndex(function(t) { return t.id === id; });
    if (index >= 0) {
        userTemplates.splice(index, 1);
        saveUserTemplates();
        showToast('模板已删除', 'success');
        detailModal.hide();
        mergeAndRenderTemplates();
    } else {
        showToast('模板不存在或无权删除', 'error');
    }
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.querySelector('.sidebar-overlay');
    
    if (sidebar.classList.contains('show')) {
        sidebar.classList.remove('show');
        overlay.classList.remove('show');
    } else {
        sidebar.classList.add('show');
        overlay.classList.add('show');
    }
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(function() {
        showToast('已复制到剪贴板', 'success');
    }).catch(function() {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
        showToast('已复制到剪贴板', 'success');
    });
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    const toastIcon = document.getElementById('toast-icon');
    const toastMessage = document.getElementById('toast-message');
    
    toastMessage.textContent = message;
    
    toastIcon.className = 'bi me-2';
    if (type === 'success') {
        toastIcon.classList.add('bi-check-circle', 'text-success');
    } else if (type === 'error') {
        toastIcon.classList.add('bi-exclamation-circle', 'text-danger');
    } else {
        toastIcon.classList.add('bi-info-circle', 'text-info');
    }
    
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
}
