function expandFolderTree(tree, expandedFolders) {
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

    Object.keys(tree || {}).forEach(rootKey => {
        expandNode(tree[rootKey], rootKey);
    });
}

function buildExpandedFolders(hierarchicalData) {
    const expandedFolders = {};
    expandFolderTree(hierarchicalData, expandedFolders);
    return expandedFolders;
}

function normalizeObjectLiteral(text) {
    const trimmed = String(text || '').trim();
    if (!trimmed) {
        return null;
    }
    if (!(trimmed.startsWith('{') || trimmed.startsWith('['))) {
        return null;
    }
    // Quote unquoted keys: {name: "A"} -> {"name": "A"}
    const withQuotedKeys = trimmed.replace(/([{\[,]\s*)([A-Za-z_$][\w$]*)\s*:/g, '$1"$2":');
    // Convert single-quoted strings to double-quoted strings
    const withDoubleQuotes = withQuotedKeys.replace(/'([^'\\]*(?:\\.[^'\\]*)*)'/g, (_, value) => {
        const normalizedValue = value.replace(/\\'/g, "'").replace(/\"/g, '\\"');
        return `"${normalizedValue}"`;
    });
    return withDoubleQuotes;
}

function parseCustomModelValue(rawValue) {
    if (rawValue === '') {
        return {};
    }
    const text = String(rawValue).trim();
    if (!text) {
        return null;
    }
    const normalized = normalizeObjectLiteral(text);
    if (!normalized) {
        return null;
    }
    try {
        const parsed = JSON.parse(normalized);
        if (parsed === null || parsed === undefined) {
            return null;
        }
        if (typeof parsed === 'object') {
            return parsed;
        }
    } catch (error) {
        return null;
    }
    return null;
}

function stringifyObjectLiteral(value, indent = 0) {
    const pad = '  '.repeat(indent);
    if (value === null || value === undefined) {
        return 'null';
    }
    if (Array.isArray(value)) {
        if (value.length === 0) {
            return '[]';
        }
        const items = value.map(item => stringifyObjectLiteral(item, indent + 1));
        const multiline = items.some(item => item.includes('\n'));
        if (!multiline && items.join(', ').length <= 80) {
            return `[${items.join(', ')}]`;
        }
        return `[\n${items.map(item => `${'  '.repeat(indent + 1)}${item}`).join(',\n')}\n${pad}]`;
    }
    if (typeof value === 'object') {
        const entries = Object.entries(value);
        if (entries.length === 0) {
            return '{}';
        }
        const rendered = entries.map(([key, val]) => {
            const safeKey = /^[A-Za-z_$][A-Za-z0-9_$]*$/.test(key) ? key : JSON.stringify(key);
            const renderedValue = stringifyObjectLiteral(val, indent + 1);
            if (renderedValue.includes('\n')) {
                return `${safeKey}: ${renderedValue}`;
            }
            return `${safeKey}: ${renderedValue}`;
        });
        const multiline = rendered.some(item => item.includes('\n')) || rendered.join(', ').length > 80;
        if (!multiline) {
            return `{${rendered.join(', ')}}`;
        }
        return `{\n${rendered.map(item => `${'  '.repeat(indent + 1)}${item}`).join(',\n')}\n${pad}}`;
    }
    if (typeof value === 'string') {
        return JSON.stringify(value);
    }
    return String(value);
}

function indentYaml(yaml, indentSize) {
    const pad = ' '.repeat(indentSize);
    return yaml
        .split('\n')
        .map(line => (line.length > 0 ? `${pad}${line}` : line))
        .join('\n');
}

function sanitizeFilePart(value) {
    return String(value || '')
        .replace(/\s+/g, '-')
        .replace(/[^a-zA-Z0-9_-]/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-+|-+$/g, '');
}

function toYaml(value, indent = 0) {
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
                const rendered = toYaml(item, indent + 1);
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
                const rendered = toYaml(val, indent + 1);
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
}

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

function syncSelectionFromUrl(alpineData) {
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
}

function hierarchicalFragmentList() {
    // JSONデータを読み込み
    const fragmentsData = JSON.parse(document.getElementById('fragmentsData')?.textContent || '[]');
    const hierarchicalData = JSON.parse(document.getElementById('hierarchicalData')?.textContent || '{}');
    const originalHierarchyTree = JSON.parse(JSON.stringify(hierarchicalData || {}));

    return {
        allFragments: fragmentsData || [],
        hierarchyTree: hierarchicalData || {},
        originalHierarchyTree: originalHierarchyTree,
        selectedFragment: null,
        customStoryState: { parameters: {}, model: {} },
        customStoryTypes: { parameters: {}, model: {} },
        customStoryNullFlags: { parameters: {}, model: {} },
        customStoryNullBackups: { parameters: {}, model: {} },
        customPreviewWrapper: '',
        yamlPreviewOpen: false,
        yamlPreviewContent: '',
        yamlPreviewTitle: '',

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
            const parameterSpecOrder = this.getParameterSpecOrder();
            if (Array.isArray(story.parameters)) {
                return this.sortStoryParameterEntries(story.parameters, parameterSpecOrder);
            }
            const entries = Object.entries(story.parameters).map(([key, value]) => ({ key, value }));
            return this.sortStoryParameterEntries(entries, parameterSpecOrder);
        },
        selectedStory: null,
        searchQuery: '',
        sidebarOpen: false,
        expandedFolders: buildExpandedFolders(hierarchicalData),
        customStoryStoragePrefix: 'thymeleaflet:custom:',
        customStoryRawValues: {},
        customStoryJsonErrors: {},
        customModelJson: '{}',
        customModelJsonError: false,

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
                return { parameters: {}, model: {}, types: { parameters: {}, model: {} }, nullFlags: { parameters: {}, model: {} }, nullFlagsDefined: false };
            }
            try {
                const raw = sessionStorage.getItem(storageKey);
                if (!raw) {
                    return { parameters: {}, model: {}, types: { parameters: {}, model: {} }, nullFlags: { parameters: {}, model: {} }, nullFlagsDefined: false };
                }
                const parsed = JSON.parse(raw);
                if (parsed && typeof parsed === 'object') {
                    if ('parameters' in parsed || 'model' in parsed) {
                        const hasWrapper = Object.prototype.hasOwnProperty.call(parsed, 'wrapper');
                        const rawTypes = parsed.types && typeof parsed.types === 'object' ? parsed.types : {};
                        const hasNullFlags = Object.prototype.hasOwnProperty.call(parsed, 'nullFlags');
                        const rawNullFlags = parsed.nullFlags && typeof parsed.nullFlags === 'object' ? parsed.nullFlags : {};
                        return {
                            parameters: parsed.parameters && typeof parsed.parameters === 'object' ? parsed.parameters : {},
                            model: parsed.model && typeof parsed.model === 'object' ? parsed.model : {},
                            types: {
                                parameters: rawTypes.parameters && typeof rawTypes.parameters === 'object' ? rawTypes.parameters : {},
                                model: rawTypes.model && typeof rawTypes.model === 'object' ? rawTypes.model : {}
                            },
                            nullFlags: {
                                parameters: rawNullFlags.parameters && typeof rawNullFlags.parameters === 'object' ? rawNullFlags.parameters : {},
                                model: rawNullFlags.model && typeof rawNullFlags.model === 'object' ? rawNullFlags.model : {}
                            },
                            nullFlagsDefined: hasNullFlags,
                            wrapper: hasWrapper && typeof parsed.wrapper === 'string' ? parsed.wrapper : null,
                            wrapperDefined: hasWrapper
                        };
                    }
                    return {
                        parameters: parsed,
                        model: {},
                        types: { parameters: {}, model: {} },
                        nullFlags: { parameters: {}, model: {} },
                        nullFlagsDefined: false,
                        wrapper: null,
                        wrapperDefined: false
                    };
                }
                return { parameters: {}, model: {}, types: { parameters: {}, model: {} }, nullFlags: { parameters: {}, model: {} }, nullFlagsDefined: false, wrapper: null, wrapperDefined: false };
            } catch (error) {
                console.warn('Failed to load custom story values', error);
                return { parameters: {}, model: {}, types: { parameters: {}, model: {} }, nullFlags: { parameters: {}, model: {} }, nullFlagsDefined: false, wrapper: null, wrapperDefined: false };
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
            const baseStory = this.getCustomBaseStory(fragment);
            const baseParams = baseStory && baseStory.parameters && !Array.isArray(baseStory.parameters)
                ? baseStory.parameters
                : {};
            const baseModel = baseStory && baseStory.model && !Array.isArray(baseStory.model)
                ? baseStory.model
                : {};

            const normalizedState = this.buildNormalizedCustomState(fragment, {
                parameters: { ...baseParams, ...(stored.parameters || {}) },
                model: { ...baseModel, ...(stored.model || {}) }
            });
            const normalizedTypes = this.buildNormalizedCustomTypes(
                normalizedState,
                stored.types || { parameters: {}, model: {} }
            );
            const normalizedNullFlags = this.buildNormalizedCustomNullFlags(
                normalizedState,
                stored.nullFlags || { parameters: {}, model: {} },
                stored.nullFlagsDefined !== true
            );

            if (stored && (
                Object.keys(stored.parameters || {}).length > 0 ||
                Object.keys(stored.model || {}).length > 0 ||
                (typeof stored.wrapper === 'string' && stored.wrapper.length > 0)
            )) {
                const inheritedWrapper = this.getStoryWrapper(baseStory) || this.getPreviewWrapperFromHost();
                this.customStoryState = normalizedState;
                this.customStoryTypes = normalizedTypes;
                this.customStoryNullFlags = normalizedNullFlags;
                this.customPreviewWrapper = stored.wrapperDefined === false || !stored.wrapper || !stored.wrapper.trim()
                    ? inheritedWrapper
                    : stored.wrapper;
                this.customStoryRawValues = this.buildRawValuesFromCustom(normalizedState);
                this.customStoryJsonErrors = {};
                this.customModelJson = stringifyObjectLiteral(this.customStoryState.model || {});
                this.customModelJsonError = false;
                this.saveCustomStoryValues(fragment, this.buildCustomStoryPayload());
                return;
            }

            this.customStoryState = this.buildNormalizedCustomState(fragment, {
                parameters: { ...baseParams },
                model: { ...baseModel }
            });
            this.customStoryTypes = this.buildNormalizedCustomTypes(this.customStoryState, { parameters: {}, model: {} });
            this.customStoryNullFlags = this.buildNormalizedCustomNullFlags(this.customStoryState, { parameters: {}, model: {} }, true);
            this.customPreviewWrapper = this.getStoryWrapper(baseStory) || this.getPreviewWrapperFromHost();
            this.customStoryRawValues = this.buildRawValuesFromCustom(this.customStoryState);
            this.customStoryJsonErrors = {};
            this.customModelJson = stringifyObjectLiteral(this.customStoryState.model || {});
            this.customModelJsonError = false;
            this.saveCustomStoryValues(fragment, this.buildCustomStoryPayload());
        },

        buildNormalizedCustomState(fragment, state) {
            const params = state?.parameters && typeof state.parameters === 'object' ? { ...state.parameters } : {};
            const fragmentParameters = Array.isArray(fragment?.parameters)
                ? fragment.parameters.filter(name => typeof name === 'string' && name.length > 0)
                : [];
            fragmentParameters.forEach(parameterName => {
                if (!Object.prototype.hasOwnProperty.call(params, parameterName)) {
                    params[parameterName] = '';
                }
            });
            const model = state?.model && typeof state.model === 'object' ? { ...state.model } : {};
            return { parameters: params, model };
        },

        buildNormalizedCustomTypes(state, types) {
            const normalizedTypes = {
                parameters: { ...(types?.parameters || {}) },
                model: { ...(types?.model || {}) }
            };
            ['parameters', 'model'].forEach(kind => {
                const bucket = state?.[kind] || {};
                Object.entries(bucket).forEach(([key, value]) => {
                    if (!this.isSupportedCustomValueType(normalizedTypes[kind][key])) {
                        normalizedTypes[kind][key] = this.getCustomValueType(value);
                    }
                });
            });
            return normalizedTypes;
        },

        buildNormalizedCustomNullFlags(state, nullFlags, fallbackToNullValues) {
            const normalizedFlags = {
                parameters: { ...(nullFlags?.parameters || {}) },
                model: { ...(nullFlags?.model || {}) }
            };
            ['parameters', 'model'].forEach(kind => {
                const bucket = state?.[kind] || {};
                Object.entries(bucket).forEach(([key, value]) => {
                    if (typeof normalizedFlags[kind][key] !== 'boolean') {
                        normalizedFlags[kind][key] = fallbackToNullValues === true && value === null;
                    }
                });
            });
            return normalizedFlags;
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
            const entries = Object.entries(bucket).map(([key, value]) => ({
                kind,
                key,
                value,
                type: this.getCustomEntryType(kind, key, value),
                rawValue: this.customStoryRawValues?.[`${kind}:${key}`],
                isNull: this.customStoryNullFlags?.[kind]?.[key] === true
            }));
            if (kind !== 'parameters') {
                return entries;
            }
            return this.sortStoryParameterEntries(entries, this.getParameterSpecOrder());
        },

        getParameterSpecOrder() {
            const javadocParameters = this.selectedFragment?.javadocInfo?.parameters;
            if (!Array.isArray(javadocParameters) || javadocParameters.length === 0) {
                return null;
            }
            const orderedNames = javadocParameters
                .map(param => param?.name)
                .filter(name => typeof name === 'string' && name.length > 0);
            return orderedNames.length > 0 ? orderedNames : null;
        },

        sortStoryParameterEntries(entries, parameterSpecOrder) {
            if (!Array.isArray(entries) || entries.length === 0) {
                return [];
            }
            if (!Array.isArray(parameterSpecOrder) || parameterSpecOrder.length === 0) {
                return entries;
            }
            const orderMap = new Map(parameterSpecOrder.map((name, index) => [name, index]));
            return [...entries].sort((a, b) => {
                const aOrder = orderMap.has(a?.key) ? orderMap.get(a.key) : Number.MAX_SAFE_INTEGER;
                const bOrder = orderMap.has(b?.key) ? orderMap.get(b.key) : Number.MAX_SAFE_INTEGER;
                if (aOrder !== bOrder) {
                    return aOrder - bOrder;
                }
                return 0;
            });
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

        getCustomEntryType(kind, key, value) {
            const explicitType = this.customStoryTypes?.[kind]?.[key];
            if (this.isSupportedCustomValueType(explicitType)) {
                return explicitType;
            }
            return this.getCustomValueType(value);
        },

        isSupportedCustomValueType(type) {
            return type === 'string' || type === 'number' || type === 'boolean' || type === 'object' || type === 'array';
        },

        getCustomInputValue(entry) {
            if (!entry) {
                return '';
            }
            if (this.isCustomNullSelected(entry)) {
                return entry.type === 'boolean' ? false : '';
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
            this.setCustomNullFlag(kind, key, false);
            if (kind === 'model') {
                this.customModelJson = stringifyObjectLiteral(nextState.model || {});
                this.customModelJsonError = false;
            }
            if (valueType !== 'object' && valueType !== 'array') {
                const { [`${kind}:${key}`]: _, ...restRaw } = this.customStoryRawValues || {};
                this.customStoryRawValues = restRaw;
            }
            this.saveCustomStoryValues(this.selectedFragment, this.buildCustomStoryPayload());
            this.applyCustomOverrides();
        },

        updateCustomValueType(kind, key, nextType) {
            if (!this.isSupportedCustomValueType(nextType)) {
                return;
            }
            const currentValue = this.customStoryState?.[kind]?.[key];
            const isNullSelected = this.customStoryNullFlags?.[kind]?.[key] === true;
            const coercedValue = isNullSelected ? null : this.coerceCustomValueByType(currentValue, nextType);
            const nextState = {
                parameters: { ...(this.customStoryState?.parameters || {}) },
                model: { ...(this.customStoryState?.model || {}) }
            };
            nextState[kind][key] = coercedValue;
            this.customStoryState = nextState;
            this.customStoryTypes = {
                parameters: { ...(this.customStoryTypes?.parameters || {}) },
                model: { ...(this.customStoryTypes?.model || {}) }
            };
            this.customStoryTypes[kind][key] = nextType;
            if (!isNullSelected && (nextType === 'object' || nextType === 'array')) {
                this.customStoryRawValues = {
                    ...(this.customStoryRawValues || {}),
                    [`${kind}:${key}`]: JSON.stringify(coercedValue, null, 2)
                };
            } else {
                const { [`${kind}:${key}`]: _, ...restRaw } = this.customStoryRawValues || {};
                this.customStoryRawValues = restRaw;
            }
            const { [`${kind}:${key}`]: __, ...restErrors } = this.customStoryJsonErrors || {};
            this.customStoryJsonErrors = restErrors;
            if (kind === 'model') {
                this.customModelJson = stringifyObjectLiteral(nextState.model || {});
                this.customModelJsonError = false;
            }
            this.saveCustomStoryValues(this.selectedFragment, this.buildCustomStoryPayload());
            this.applyCustomOverrides();
        },

        isCustomNullSelected(entry) {
            if (!entry) {
                return false;
            }
            return this.customStoryNullFlags?.[entry.kind]?.[entry.key] === true;
        },

        setCustomNullFlag(kind, key, enabled) {
            this.customStoryNullFlags = {
                parameters: { ...(this.customStoryNullFlags?.parameters || {}) },
                model: { ...(this.customStoryNullFlags?.model || {}) }
            };
            this.customStoryNullFlags[kind][key] = enabled === true;
        },

        setCustomNullBackup(kind, key, value, rawValue) {
            this.customStoryNullBackups = {
                parameters: { ...(this.customStoryNullBackups?.parameters || {}) },
                model: { ...(this.customStoryNullBackups?.model || {}) }
            };
            this.customStoryNullBackups[kind][key] = { value, rawValue };
        },

        getCustomNullBackup(kind, key) {
            return this.customStoryNullBackups?.[kind]?.[key];
        },

        clearCustomNullBackup(kind, key) {
            const nextBackups = {
                parameters: { ...(this.customStoryNullBackups?.parameters || {}) },
                model: { ...(this.customStoryNullBackups?.model || {}) }
            };
            if (nextBackups[kind] && Object.prototype.hasOwnProperty.call(nextBackups[kind], key)) {
                delete nextBackups[kind][key];
            }
            this.customStoryNullBackups = nextBackups;
        },

        toggleCustomNull(kind, key, enabled) {
            const isNull = enabled === true;
            const nextState = {
                parameters: { ...(this.customStoryState?.parameters || {}) },
                model: { ...(this.customStoryState?.model || {}) }
            };
            if (isNull) {
                this.setCustomNullBackup(
                    kind,
                    key,
                    nextState[kind][key],
                    this.customStoryRawValues?.[`${kind}:${key}`]
                );
                nextState[kind][key] = null;
                const { [`${kind}:${key}`]: _raw, ...restRaw } = this.customStoryRawValues || {};
                this.customStoryRawValues = restRaw;
                const { [`${kind}:${key}`]: _err, ...restErr } = this.customStoryJsonErrors || {};
                this.customStoryJsonErrors = restErr;
            } else {
                const entryType = this.getCustomEntryType(kind, key, nextState[kind][key]);
                const backup = this.getCustomNullBackup(kind, key);
                if (backup && Object.prototype.hasOwnProperty.call(backup, 'value')) {
                    nextState[kind][key] = backup.value;
                    if ((entryType === 'object' || entryType === 'array') && typeof backup.rawValue === 'string') {
                        this.customStoryRawValues = {
                            ...(this.customStoryRawValues || {}),
                            [`${kind}:${key}`]: backup.rawValue
                        };
                    }
                    this.clearCustomNullBackup(kind, key);
                } else {
                    nextState[kind][key] = this.coerceCustomValueByType(undefined, entryType);
                }
            }
            this.customStoryState = nextState;
            this.setCustomNullFlag(kind, key, isNull);
            if (kind === 'model') {
                this.customModelJson = stringifyObjectLiteral(nextState.model || {});
                this.customModelJsonError = false;
            }
            this.saveCustomStoryValues(this.selectedFragment, this.buildCustomStoryPayload());
            this.applyCustomOverrides();
        },

        coerceCustomValueByType(value, targetType) {
            if (targetType === 'array') {
                if (Array.isArray(value)) {
                    return value;
                }
                if (typeof value === 'string' && value.trim() !== '') {
                    try {
                        const parsed = JSON.parse(value);
                        return Array.isArray(parsed) ? parsed : [];
                    } catch (_error) {
                        return [];
                    }
                }
                return [];
            }
            if (targetType === 'object') {
                if (value && typeof value === 'object' && !Array.isArray(value)) {
                    return value;
                }
                if (typeof value === 'string' && value.trim() !== '') {
                    try {
                        const parsed = JSON.parse(value);
                        return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
                    } catch (_error) {
                        return {};
                    }
                }
                return {};
            }
            if (targetType === 'boolean') {
                if (typeof value === 'boolean') {
                    return value;
                }
                if (typeof value === 'string') {
                    const normalized = value.trim().toLowerCase();
                    if (normalized === 'true') {
                        return true;
                    }
                    if (normalized === 'false') {
                        return false;
                    }
                }
                return false;
            }
            if (targetType === 'number') {
                if (typeof value === 'number' && Number.isFinite(value)) {
                    return value;
                }
                if (typeof value === 'string' && value.trim() !== '') {
                    const parsed = Number(value);
                    if (Number.isFinite(parsed)) {
                        return parsed;
                    }
                }
                return null;
            }
            if (value === null || value === undefined) {
                return '';
            }
            if (typeof value === 'object') {
                return JSON.stringify(value);
            }
            return String(value);
        },

        updateCustomModelJson(rawValue) {
            this.customModelJson = rawValue;
            const parsed = parseCustomModelValue(rawValue);
            if (parsed && typeof parsed === 'object') {
                this.customStoryState = {
                    parameters: { ...(this.customStoryState?.parameters || {}) },
                    model: parsed
                };
                this.customModelJsonError = false;
                this.saveCustomStoryValues(this.selectedFragment, {
                    ...this.buildCustomStoryPayload()
                });
                this.applyCustomOverrides();
                return;
            }
            this.customModelJsonError = true;
        },

        updateCustomPreviewWrapper(rawValue) {
            this.customPreviewWrapper = rawValue || '';
            this.saveCustomStoryValues(this.selectedFragment, this.buildCustomStoryPayload());
            this.applyCustomOverrides();
        },

        buildCustomStoryPayload() {
            return {
                ...this.customStoryState,
                types: this.customStoryTypes,
                nullFlags: this.customStoryNullFlags,
                wrapper: this.customPreviewWrapper
            };
        },

        getStoryWrapper(story) {
            const preview = story?.story?.preview || story?.preview;
            return (preview && typeof preview.wrapper === 'string') ? preview.wrapper : '';
        },

        getPreviewWrapperFromHost() {
            const host = document.getElementById('fragment-preview-host');
            return host?.dataset?.previewWrapper || '';
        },

        applyCustomOverrides() {
            if (!this.isCustomStory(this.selectedStory)) {
                return;
            }
            if (window.__thymeleafletPreview?.setWrapperOverride) {
                window.__thymeleafletPreview.setWrapperOverride(this.customPreviewWrapper);
            }
            if (window.__thymeleafletPreview?.setStoryOverrides) {
                window.__thymeleafletPreview.setStoryOverrides(this.customStoryState);
                window.__thymeleafletPreview.render();
                return;
            }
            window.__thymeleafletPendingOverrides = { ...this.customStoryState };
            window.__thymeleafletPendingWrapperOverride = this.customPreviewWrapper;
        },

        downloadCustomStoryYaml() {
            if (!this.selectedFragment || !this.isCustomStory(this.selectedStory)) {
                return;
            }
            const parameters = { ...(this.customStoryState?.parameters || {}) };
            const model = { ...(this.customStoryState?.model || {}) };
            const yaml = this.buildCustomStoryYaml(parameters, model, this.customPreviewWrapper);
            const templatePart = sanitizeFilePart(this.selectedFragment.templatePath);
            const fragmentPart = sanitizeFilePart(this.selectedFragment.fragmentName);
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

        buildCustomStoryYaml(parameters, model, wrapper) {
            const lines = [
                '# Paste into storyGroups.<fragmentName>.stories',
                '# Rename "custom" before saving',
                '- name: custom',
                '  title: Custom'
            ];
            if (parameters && Object.keys(parameters).length > 0) {
                lines.push('  parameters:');
                lines.push(indentYaml(toYaml(parameters, 1), 2));
            }
            if (model && Object.keys(model).length > 0) {
                lines.push('  model:');
                lines.push(indentYaml(toYaml(model, 1), 2));
            }
            if (wrapper && wrapper.trim() !== '') {
                lines.push('  preview:');
                lines.push('    wrapper: |');
                lines.push(indentYaml(wrapper, 6));
            }
            return lines.join('\n').replace(/\n{3,}/g, '\n\n') + '\n';
        },

        buildSelectedStoryYaml() {
            if (!this.selectedStory) {
                return '';
            }
            const storyName = this.selectedStory.storyName || 'story';
            const title = this.selectedStory.displayTitle || storyName;
            const rawParameters = this.selectedStory.parameters && !Array.isArray(this.selectedStory.parameters)
                ? this.selectedStory.parameters
                : {};
            const rawModel = this.selectedStory.model && !Array.isArray(this.selectedStory.model)
                ? this.selectedStory.model
                : {};
            const wrapper = this.getStoryWrapper(this.selectedStory) || this.getPreviewWrapperFromHost();
            const lines = [
                `- name: ${storyName}`,
                `  title: ${title}`
            ];
            if (rawParameters && Object.keys(rawParameters).length > 0) {
                lines.push('  parameters:');
                lines.push(indentYaml(toYaml(rawParameters, 1), 2));
            }
            if (rawModel && Object.keys(rawModel).length > 0) {
                lines.push('  model:');
                lines.push(indentYaml(toYaml(rawModel, 1), 2));
            }
            if (wrapper && wrapper.trim() !== '') {
                lines.push('  preview:');
                lines.push('    wrapper: |');
                lines.push(indentYaml(wrapper, 6));
            }
            return lines.join('\n').replace(/\n{3,}/g, '\n\n') + '\n';
        },

        showStoryYamlPreview() {
            const yaml = this.buildSelectedStoryYaml();
            this.yamlPreviewTitle = this.selectedStory?.displayTitle || this.selectedStory?.storyName || '';
            this.yamlPreviewContent = yaml;
            this.yamlPreviewOpen = true;
        },

        closeStoryYamlPreview() {
            this.yamlPreviewOpen = false;
        },

        resetPreviewOverrides() {
            this.customPreviewWrapper = this.getStoryWrapper(this.selectedStory) || this.getPreviewWrapperFromHost();
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
            expandFolderTree(this.hierarchyTree, this.expandedFolders);
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

        // Alpine.jsデータ変更時にバッジを更新
        updateStoryBadges() {
            this.$nextTick(() => {
                renderStoryBadges();
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
    syncSelectionFromUrl(alpineData);
});

// バッジレンダリング関数（グローバル）
