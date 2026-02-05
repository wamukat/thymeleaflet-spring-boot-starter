function hierarchicalFragmentList() {
    // JSONデータを読み込み
    const fragmentsData = JSON.parse(document.getElementById('fragmentsData')?.textContent || '[]');
    const hierarchicalData = JSON.parse(document.getElementById('hierarchicalData')?.textContent || '{}');
    const originalHierarchyTree = JSON.parse(JSON.stringify(hierarchicalData || {}));

    const expandedFolders = {};
    const expandNode = (node, currentPath) => {
        if (currentPath) {
            expandedFolders[currentPath] = true;
        }
        if (node && typeof node === 'object') {
            if (node._fragments) {
                Object.keys(node._fragments).forEach(templateName => {
                    const templatePath = currentPath ? `${currentPath}/${templateName}` : templateName;
                    expandedFolders[templatePath] = true;
                });
            }
            Object.keys(node).forEach(key => {
                if (key === '_fragments') {
                    return;
                }
                const nextPath = currentPath ? `${currentPath}/${key}` : key;
                expandNode(node[key], nextPath);
            });
        }
    };
    Object.keys(hierarchicalData || {}).forEach(rootKey => {
        expandNode(hierarchicalData[rootKey], rootKey);
    });

    return {
        allFragments: fragmentsData || [],
        hierarchyTree: hierarchicalData || {},
        originalHierarchyTree: originalHierarchyTree,
        selectedFragment: null,
        customStoryState: { parameters: {}, model: {} },

        // Phase 4.3: SecureTemplatePath 対応 - templatePath変換ヘルパー
        templatePathForUrl: function(templatePath) {
            // SecureTemplatePath.forUrl() と同じ変換: "/" → "."
            return templatePath.replace(/\//g, '.');
        },
        templatePathForFilePath: function(templatePath) {
            // SecureTemplatePath.forFilePath() と同じ変換: "." → "/"
            return templatePath.replace(/\./g, '/');
        },
        // URLエンコード用ヘルパー関数
        encodeStoryName: function(storyName) {
            return encodeURIComponent(storyName);
        },
        storyParameters: function(story) {
            if (!story || !story.parameters) {
                return [];
            }
            if (Array.isArray(story.parameters)) {
                return story.parameters;
            }
            return Object.entries(story.parameters).map(([key, value]) => ({ key, value }));
        },
        selectedStory: null,
        searchQuery: '',
        sidebarOpen: false,
        expandedFolders: expandedFolders,
        customStoryStoragePrefix: 'thymeleaflet:custom:',
        customStoryRawValues: {},
        customStoryJsonErrors: {},

        initializeFromServerState() {
            // サーバーサイドから渡された選択状態を使用
            const selectedTemplatePath = document.querySelector('meta[name="selected-template-path"]')?.content;
            const selectedFragmentName = document.querySelector('meta[name="selected-fragment-name"]')?.content;
            const selectedStoryName = document.querySelector('meta[name="selected-story-name"]')?.content;

            if (selectedTemplatePath && selectedFragmentName) {
                // 該当するフラグメントを検索
                const fragment = this.allFragments?.find(f =>
                    f?.templatePath === selectedTemplatePath &&
                    f?.fragmentName === selectedFragmentName
                );

                if (fragment) {
                    this.setSelectedFragment(fragment);
                    this.expandFoldersForFragment(fragment);

                    // ストーリーも選択
                    if (selectedStoryName && fragment.stories) {
                        const story = fragment.stories.find(s => s?.storyName === selectedStoryName);
                        if (story) {
                            this.setSelectedStory(story);
                        }
                    }
                }
            }
        },

        setSelectedFragment(fragment) {
            this.selectedFragment = fragment;
        },

        setSelectedStory(story) {
            const wasCustom = this.isCustomStory(this.selectedStory);
            this.selectedStory = story;
            if (this.isCustomStory(story)) {
                this.ensureCustomStoryValues(this.selectedFragment);
                this.applyCustomOverrides();
            } else if (wasCustom) {
                this.resetPreviewOverrides();
            }
        },

        isCustomStory(story) {
            return story && story.storyName === 'custom';
        },
        isCustomSelected() {
            return this.isCustomStory(this.selectedStory);
        },
        hasFragmentStories() {
            return !!(this.selectedFragment?.stories && this.selectedFragment.stories.length > 0);
        },
        hasSelectedStoryTitle() {
            return !!(this.selectedStory && this.selectedStory.displayTitle);
        },
        shouldShowParametersCard() {
            return !!(this.selectedFragment && (this.selectedFragment.parameters?.length > 0 || this.storyParameters(this.selectedStory).length > 0));
        },
        shouldShowFragmentParameters() {
            return !!(this.selectedFragment?.parameters && this.selectedFragment.parameters.length > 0);
        },
        shouldShowStoryValues() {
            if (!this.selectedStory || !this.isCustomStory(this.selectedStory)) {
                return this.storyParameters(this.selectedStory).length > 0;
            }
            return this.customStoryEntries('parameters').length > 0 || this.customStoryEntries('model').length > 0;
        },
        isObjectStoryValue(storyParameter) {
            return !!(storyParameter && typeof storyParameter.value === 'object' && storyParameter.value !== null);
        },
        hasCustomJsonError(entry) {
            return !!(this.customStoryJsonErrors && this.customStoryJsonErrors[`${entry.kind}:${entry.key}`]);
        },
        shouldShowNoParameters() {
            const noFragmentParams = !this.selectedFragment?.parameters || this.selectedFragment.parameters.length === 0;
            if (!this.selectedStory || !this.isCustomStory(this.selectedStory)) {
                return noFragmentParams && this.storyParameters(this.selectedStory).length === 0;
            }
            return noFragmentParams &&
                this.customStoryEntries('parameters').length === 0 &&
                this.customStoryEntries('model').length === 0;
        },

        getCustomStorageKey(fragment) {
            if (!fragment) {
                return null;
            }
            return `${this.customStoryStoragePrefix}${fragment.templatePath}/${fragment.fragmentName}`;
        },

        loadCustomStoryValues(fragment) {
            const storageKey = this.getCustomStorageKey(fragment);
            if (!storageKey) {
                return { parameters: {}, model: {} };
            }
            try {
                const raw = sessionStorage.getItem(storageKey);
                if (!raw) {
                    return { parameters: {}, model: {} };
                }
                const parsed = JSON.parse(raw);
                if (parsed && typeof parsed === 'object') {
                    if ('parameters' in parsed || 'model' in parsed) {
                        return {
                            parameters: parsed.parameters && typeof parsed.parameters === 'object' ? parsed.parameters : {},
                            model: parsed.model && typeof parsed.model === 'object' ? parsed.model : {}
                        };
                    }
                    return { parameters: parsed, model: {} };
                }
                return { parameters: {}, model: {} };
            } catch (error) {
                console.warn('Failed to load custom story values', error);
                return { parameters: {}, model: {} };
            }
        },

        saveCustomStoryValues(fragment, values) {
            const storageKey = this.getCustomStorageKey(fragment);
            if (!storageKey) {
                return;
            }
            try {
                const payload = values && typeof values === 'object' ? values : { parameters: {}, model: {} };
                sessionStorage.setItem(storageKey, JSON.stringify(payload));
            } catch (error) {
                console.warn('Failed to save custom story values', error);
            }
        },

        getCustomBaseStory(fragment) {
            if (!fragment || !Array.isArray(fragment.stories)) {
                return null;
            }
            const defaultStory = fragment.stories.find(story => story?.storyName === 'default');
            if (defaultStory) {
                return defaultStory;
            }
            return fragment.stories.find(story => story?.storyName !== 'custom') || null;
        },

        ensureCustomStoryValues(fragment) {
            if (!fragment) {
                return;
            }
            const stored = this.loadCustomStoryValues(fragment);
            if (stored && (Object.keys(stored.parameters || {}).length > 0 || Object.keys(stored.model || {}).length > 0)) {
                this.customStoryState = stored;
                this.customStoryRawValues = this.buildRawValuesFromCustom(stored);
                this.customStoryJsonErrors = {};
                return;
            }

            const baseStory = this.getCustomBaseStory(fragment);
            const baseParams = baseStory && baseStory.parameters && !Array.isArray(baseStory.parameters)
                ? baseStory.parameters
                : {};
            const baseModel = baseStory && baseStory.model && !Array.isArray(baseStory.model)
                ? baseStory.model
                : {};
            this.customStoryState = { parameters: { ...baseParams }, model: { ...baseModel } };
            this.customStoryRawValues = this.buildRawValuesFromCustom(this.customStoryState);
            this.customStoryJsonErrors = {};
            this.saveCustomStoryValues(fragment, this.customStoryState);
        },

        buildRawValuesFromCustom(state) {
            const rawValues = {};
            ['parameters', 'model'].forEach(kind => {
                const bucket = state?.[kind] || {};
                Object.entries(bucket).forEach(([key, value]) => {
                    if (value !== null && typeof value === 'object') {
                        rawValues[`${kind}:${key}`] = JSON.stringify(value, null, 2);
                    }
                });
            });
            return rawValues;
        },

        customStoryEntries(kind) {
            const bucket = this.customStoryState?.[kind] || {};
            return Object.entries(bucket).map(([key, value]) => ({
                kind,
                key,
                value,
                type: this.getCustomValueType(value),
                rawValue: this.customStoryRawValues?.[`${kind}:${key}`]
            }));
        },

        getCustomValueType(value) {
            if (Array.isArray(value)) {
                return 'array';
            }
            if (value === null) {
                return 'string';
            }
            return typeof value;
        },

        getCustomInputValue(entry) {
            if (!entry) {
                return '';
            }
            if (entry.type === 'object' || entry.type === 'array') {
                return entry.rawValue ?? JSON.stringify(entry.value, null, 2);
            }
            if (entry.type === 'boolean') {
                return !!entry.value;
            }
            return entry.value ?? '';
        },

        updateCustomValue(kind, key, rawValue, valueType, event) {
            let nextValue = rawValue;
            if (valueType === 'number') {
                nextValue = rawValue === '' ? null : Number(rawValue);
            } else if (valueType === 'boolean') {
                nextValue = event?.target?.checked === true;
            } else if (valueType === 'object' || valueType === 'array') {
                this.customStoryRawValues = { ...this.customStoryRawValues, [`${kind}:${key}`]: rawValue };
                try {
                    nextValue = rawValue === '' ? (valueType === 'array' ? [] : {}) : JSON.parse(rawValue);
                    const { [`${kind}:${key}`]: _, ...rest } = this.customStoryJsonErrors || {};
                    this.customStoryJsonErrors = rest;
                } catch (error) {
                    this.customStoryJsonErrors = { ...this.customStoryJsonErrors, [`${kind}:${key}`]: true };
                    return;
                }
            }

            const nextState = {
                parameters: { ...(this.customStoryState?.parameters || {}) },
                model: { ...(this.customStoryState?.model || {}) }
            };
            nextState[kind][key] = nextValue;
            this.customStoryState = nextState;
            if (valueType !== 'object' && valueType !== 'array') {
                const { [`${kind}:${key}`]: _, ...restRaw } = this.customStoryRawValues || {};
                this.customStoryRawValues = restRaw;
            }
            this.saveCustomStoryValues(this.selectedFragment, this.customStoryState);
            this.applyCustomOverrides();
        },

        applyCustomOverrides() {
            if (!this.isCustomStory(this.selectedStory)) {
                return;
            }
            if (window.__thymeleafletPreview?.setStoryOverrides) {
                window.__thymeleafletPreview.setStoryOverrides(this.customStoryState);
                window.__thymeleafletPreview.render();
                return;
            }
            window.__thymeleafletPendingOverrides = { ...this.customStoryState };
        },

        downloadCustomStoryYaml() {
            if (!this.selectedFragment || !this.isCustomStory(this.selectedStory)) {
                return;
            }
            const parameters = { ...(this.customStoryState?.parameters || {}) };
            const model = { ...(this.customStoryState?.model || {}) };
            const yaml = this.buildCustomStoryYaml(parameters, model);
            const templatePart = this.sanitizeFilePart(this.selectedFragment.templatePath);
            const fragmentPart = this.sanitizeFilePart(this.selectedFragment.fragmentName);
            const fileName = `${templatePart}__${fragmentPart}__custom.yaml`;
            const blob = new Blob([yaml], { type: 'text/yaml;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
        },

        buildCustomStoryYaml(parameters, model) {
            const lines = [
                '# Paste into storyGroups.<fragmentName>.stories',
                '# Rename "custom" before saving',
                '- name: custom',
                '  title: Custom'
            ];
            if (parameters && Object.keys(parameters).length > 0) {
                lines.push('  parameters:');
                lines.push(this.indentYaml(this.toYaml(parameters, 1), 2));
            }
            if (model && Object.keys(model).length > 0) {
                lines.push('  model:');
                lines.push(this.indentYaml(this.toYaml(model, 1), 2));
            }
            return lines.join('\n').replace(/\n{3,}/g, '\n\n') + '\n';
        },

        indentYaml(yaml, indentSize) {
            const pad = ' '.repeat(indentSize);
            return yaml
                .split('\n')
                .map(line => (line.length > 0 ? `${pad}${line}` : line))
                .join('\n');
        },

        sanitizeFilePart(value) {
            return String(value || '')
                .replace(/\s+/g, '-')
                .replace(/[^a-zA-Z0-9_-]/g, '-')
                .replace(/-+/g, '-')
                .replace(/^-+|-+$/g, '');
        },

        toYaml(value, indent = 0) {
            const pad = '  '.repeat(indent);
            if (value === null || value === undefined) {
                return 'null';
            }
            if (Array.isArray(value)) {
                if (value.length === 0) {
                    return '[]';
                }
                return value
                    .map(item => {
                        const rendered = this.toYaml(item, indent + 1);
                        if (typeof item === 'object' && item !== null) {
                            return `${pad}- ${rendered.replace(/^\s*/, '')}`;
                        }
                        return `${pad}- ${rendered}`;
                    })
                    .join('\n');
            }
            if (typeof value === 'object') {
                const entries = Object.entries(value);
                if (entries.length === 0) {
                    return '{}';
                }
                return entries
                    .map(([key, val]) => {
                        const rendered = this.toYaml(val, indent + 1);
                        if (typeof val === 'object' && val !== null && !Array.isArray(val)) {
                            return `${pad}${key}:\n${rendered}`;
                        }
                        return `${pad}${key}: ${rendered}`;
                    })
                    .join('\n');
            }
            if (typeof value === 'string') {
                const needsQuote = value === '' || /[:#\n\r\t]|^\s|\s$/.test(value);
                return needsQuote ? JSON.stringify(value) : value;
            }
            return String(value);
        },

        resetPreviewOverrides() {
            if (window.__thymeleafletPreview?.resetToDefaults) {
                window.__thymeleafletPreview.resetToDefaults();
            }
        },


        expandFoldersForFragment(fragment) {
            const pathParts = fragment.templatePath.split('/');
            let currentPath = '';

            // パスの各レベルを展開
            for (let i = 0; i < pathParts.length; i++) {
                if (i === 0) {
                    currentPath = pathParts[i];
                } else {
                    currentPath += '/' + pathParts[i];
                }
                this.expandedFolders[currentPath] = true;

                // テンプレートファイル名も展開
                if (i === pathParts.length - 1) {
                    const templateName = fragment.templatePath.split('/').pop();
                    this.expandedFolders[currentPath + '/' + templateName] = true;
                }
            }
        },

        expandAllFolders() {
            const expandNode = (node, currentPath) => {
                if (currentPath) {
                    this.expandedFolders[currentPath] = true;
                }

                if (node && typeof node === 'object') {
                    if (node._fragments) {
                        Object.keys(node._fragments).forEach(templateName => {
                            const templatePath = currentPath ? `${currentPath}/${templateName}` : templateName;
                            this.expandedFolders[templatePath] = true;
                        });
                    }

                    Object.keys(node).forEach(key => {
                        if (key === '_fragments') {
                            return;
                        }
                        const nextPath = currentPath ? `${currentPath}/${key}` : key;
                        expandNode(node[key], nextPath);
                    });
                }
            };

            Object.keys(this.hierarchyTree || {}).forEach(rootKey => {
                expandNode(this.hierarchyTree[rootKey], rootKey);
            });
        },


        toggleFolder(path) {
            this.expandedFolders[path] = !this.expandedFolders[path];
        },


        getCategoryCount(category) {
            let count = 0;

            // 再帰的にカテゴリ内のフラグメントをカウント
            const countFragmentsRecursively = (cat) => {
                let localCount = 0;

                // 直接のフラグメントをカウント
                if (cat._fragments) {
                    Object.values(cat._fragments).forEach(fragments => {
                        localCount += fragments.length;
                    });
                }

                // サブカテゴリを再帰的に処理
                Object.keys(cat).forEach(key => {
                    if (key !== '_fragments' && typeof cat[key] === 'object' && cat[key] !== null) {
                        localCount += countFragmentsRecursively(cat[key]);
                    }
                });

                return localCount;
            };

            return countFragmentsRecursively(category);
        },

        filterFragments() {
            const query = this.searchQuery?.trim().toLowerCase();
            if (!query) {
                this.hierarchyTree = this.originalHierarchyTree;
                return;
            }

            const matchesFragment = (fragment) => {
                if (!fragment) {
                    return false;
                }
                const storyText = Array.isArray(fragment.stories)
                    ? fragment.stories
                          .map(story => `${story?.storyName ?? ''} ${story?.displayTitle ?? ''} ${story?.displayDescription ?? ''}`)
                          .join(' ')
                    : '';
                const haystack = `${fragment.templatePath ?? ''} ${fragment.fragmentName ?? ''} ${fragment.type ?? ''} ${storyText}`.toLowerCase();
                return haystack.includes(query);
            };

            const filterNode = (node) => {
                if (!node || typeof node !== 'object') {
                    return null;
                }
                const result = {};
                if (node._fragments) {
                    const filteredFragments = {};
                    Object.entries(node._fragments).forEach(([templateName, fragmentGroup]) => {
                        const filteredGroup = (fragmentGroup || []).filter(matchesFragment);
                        if (filteredGroup.length > 0) {
                            filteredFragments[templateName] = filteredGroup;
                        }
                    });
                    if (Object.keys(filteredFragments).length > 0) {
                        result._fragments = filteredFragments;
                    }
                }
                Object.keys(node).forEach(key => {
                    if (key === '_fragments') {
                        return;
                    }
                    const child = filterNode(node[key]);
                    if (child && Object.keys(child).length > 0) {
                        result[key] = child;
                    }
                });
                return Object.keys(result).length > 0 ? result : null;
            };

            const filteredTree = {};
            Object.entries(this.originalHierarchyTree || {}).forEach(([key, value]) => {
                const filteredNode = filterNode(value);
                if (filteredNode) {
                    filteredTree[key] = filteredNode;
                }
            });

            this.hierarchyTree = filteredTree;
            this.$nextTick(() => {
                this.expandAllFolders();
            });
        },

        copyUsageExample() {
            // 表示されている使用例をそのままコピー
            const usageExample = document.querySelector('#usage-example');
            if (usageExample) {
                const text = usageExample.textContent.trim();
                navigator.clipboard.writeText(text).then(() => {
                    console.log('Usage example copied to clipboard');
                });
            }
        },

        // Story/Fallbackバッジを動的にレンダリング
        renderStoryBadges() {
            const placeholders = document.querySelectorAll('.story-badge-placeholder');
            placeholders.forEach(placeholder => {
                const hasStoryConfig = placeholder.dataset.hasStoryConfig === 'true';
                const storyName = placeholder.dataset.storyName;
                const badgeClass = storyName === 'custom'
                    ? 'badge-custom'
                    : (hasStoryConfig ? 'badge-story' : 'badge-fallback');
                const badgeText = storyName === 'custom'
                    ? 'Custom'
                    : (hasStoryConfig ? 'Story' : 'Fallback');

                placeholder.innerHTML = `<span class="${badgeClass}">${badgeText}</span>`;
            });
        },

        // Alpine.jsデータ変更時にバッジを更新
        updateStoryBadges() {
            this.$nextTick(() => {
                this.renderStoryBadges();
            });
        }

    };
}

// Story/Fallbackバッジ管理
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(() => {
        renderStoryBadges();
    }, 100);
});

// HTMXコンテンツ更新後にバッジをレンダリング
document.addEventListener('htmx:afterSettle', function() {
    setTimeout(() => {
        renderStoryBadges();
    }, 50);
});

// HTMX更新後に選択状態をAlpine.jsに同期
document.addEventListener('htmx:afterSettle', function(event) {
    if (!event?.detail?.target || event.detail.target.id !== 'main-content-area') {
        return;
    }
    if (typeof Alpine === 'undefined') {
        return;
    }
    const root = document.querySelector('[x-data]');
    if (!root) {
        return;
    }
    const alpineData = Alpine.$data(root);
    if (!alpineData) {
        return;
    }
    const urlMatch = window.location.pathname.match(/\/thymeleaflet\/([^\/]+)\/([^\/]+)\/([^\/]+)/);
    if (!urlMatch) {
        alpineData.selectedFragment = null;
        alpineData.selectedStory = null;
        return;
    }
    const templatePathEncoded = urlMatch[1];
    const fragmentName = decodeURIComponent(urlMatch[2]);
    const storyName = decodeURIComponent(urlMatch[3]);
    const templatePath = typeof alpineData.templatePathForFilePath === 'function'
        ? alpineData.templatePathForFilePath(templatePathEncoded)
        : templatePathEncoded.replace(/\./g, '/');
    const fragment = (alpineData.allFragments || []).find(f =>
        f?.templatePath === templatePath && f?.fragmentName === fragmentName
    );
    if (!fragment) {
        return;
    }
    if (typeof alpineData.setSelectedFragment === 'function') {
        alpineData.setSelectedFragment(fragment);
    } else {
        alpineData.selectedFragment = fragment;
    }
    if (typeof alpineData.expandFoldersForFragment === 'function') {
        alpineData.expandFoldersForFragment(fragment);
    }
    if (Array.isArray(fragment.stories) && typeof alpineData.setSelectedStory === 'function') {
        const story = fragment.stories.find(s => s?.storyName === storyName);
        if (story) {
            alpineData.setSelectedStory(story);
        }
    }
});

// バッジレンダリング関数（グローバル）
function renderStoryBadges() {
    const placeholders = document.querySelectorAll('.story-badge-placeholder');
    placeholders.forEach(placeholder => {
        const hasStoryConfig = placeholder.dataset.hasStoryConfig === 'true';
        const storyName = placeholder.dataset.storyName;
        const badgeClass = storyName === 'custom'
            ? 'badge-custom'
            : (hasStoryConfig ? 'badge-story' : 'badge-fallback');
        const badgeText = storyName === 'custom'
            ? 'Custom'
            : (hasStoryConfig ? 'Story' : 'Fallback');

        placeholder.innerHTML = `<span class="${badgeClass}">${badgeText}</span>`;
    });
}
