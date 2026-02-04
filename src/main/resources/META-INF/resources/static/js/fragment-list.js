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
        customStoryValues: {},

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
            this.selectedStory = story;
            if (this.isCustomStory(story)) {
                this.ensureCustomStoryValues(this.selectedFragment);
            }
        },

        isCustomStory(story) {
            return story && story.storyName === 'custom';
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
                return {};
            }
            try {
                const raw = sessionStorage.getItem(storageKey);
                if (!raw) {
                    return {};
                }
                const parsed = JSON.parse(raw);
                return parsed && typeof parsed === 'object' ? parsed : {};
            } catch (error) {
                console.warn('Failed to load custom story values', error);
                return {};
            }
        },

        saveCustomStoryValues(fragment, values) {
            const storageKey = this.getCustomStorageKey(fragment);
            if (!storageKey) {
                return;
            }
            try {
                const payload = values && typeof values === 'object' ? values : {};
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
            if (stored && Object.keys(stored).length > 0) {
                this.customStoryValues = stored;
                this.customStoryRawValues = this.buildRawValuesFromCustom(stored);
                this.customStoryJsonErrors = {};
                return;
            }

            const baseStory = this.getCustomBaseStory(fragment);
            const baseParams = baseStory && baseStory.parameters && !Array.isArray(baseStory.parameters)
                ? baseStory.parameters
                : {};
            this.customStoryValues = { ...baseParams };
            this.customStoryRawValues = this.buildRawValuesFromCustom(this.customStoryValues);
            this.customStoryJsonErrors = {};
            this.saveCustomStoryValues(fragment, this.customStoryValues);
        },

        buildRawValuesFromCustom(values) {
            const rawValues = {};
            Object.entries(values || {}).forEach(([key, value]) => {
                if (value !== null && typeof value === 'object') {
                    rawValues[key] = JSON.stringify(value, null, 2);
                }
            });
            return rawValues;
        },

        customStoryEntries() {
            return Object.entries(this.customStoryValues || {}).map(([key, value]) => ({
                key,
                value,
                type: this.getCustomValueType(value),
                rawValue: this.customStoryRawValues?.[key]
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

        updateCustomValue(key, rawValue, valueType, event) {
            let nextValue = rawValue;
            if (valueType === 'number') {
                nextValue = rawValue === '' ? null : Number(rawValue);
            } else if (valueType === 'boolean') {
                nextValue = event?.target?.checked === true;
            } else if (valueType === 'object' || valueType === 'array') {
                this.customStoryRawValues = { ...this.customStoryRawValues, [key]: rawValue };
                try {
                    nextValue = rawValue === '' ? (valueType === 'array' ? [] : {}) : JSON.parse(rawValue);
                    const { [key]: _, ...rest } = this.customStoryJsonErrors || {};
                    this.customStoryJsonErrors = rest;
                } catch (error) {
                    this.customStoryJsonErrors = { ...this.customStoryJsonErrors, [key]: true };
                    return;
                }
            }

            this.customStoryValues = { ...this.customStoryValues, [key]: nextValue };
            if (valueType !== 'object' && valueType !== 'array') {
                const { [key]: _, ...restRaw } = this.customStoryRawValues || {};
                this.customStoryRawValues = restRaw;
            }
            this.saveCustomStoryValues(this.selectedFragment, this.customStoryValues);
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
                const badgeClass = hasStoryConfig ? 'badge-story' : 'badge-fallback';
                const badgeText = hasStoryConfig ? 'Story' : 'Fallback';

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

// バッジレンダリング関数（グローバル）
function renderStoryBadges() {
    const placeholders = document.querySelectorAll('.story-badge-placeholder');
    placeholders.forEach(placeholder => {
        const hasStoryConfig = placeholder.dataset.hasStoryConfig === 'true';
        const badgeClass = hasStoryConfig ? 'badge-story' : 'badge-fallback';
        const badgeText = hasStoryConfig ? 'Story' : 'Fallback';

        placeholder.innerHTML = `<span class="${badgeClass}">${badgeText}</span>`;
    });
}
