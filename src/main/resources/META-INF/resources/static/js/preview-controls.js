(function() {
    if (window.__thymeleafletPreviewControlsInitialized) {
        return;
    }
    window.__thymeleafletPreviewControlsInitialized = true;

    const INITIAL_PREVIEW_HEIGHT = 80;
    const MIN_HEIGHT = 60;
    const MAX_HEIGHT = 2000;
    let previewContentHeight = INITIAL_PREVIEW_HEIGHT;
    let stableTimer = null;
    let lastMeasurementTime = 0;
    let pendingHeight = null;
    let previewIdCounter = 0;
    let fontsReadyPromise = null;
    let iframeResizeObserver = null;
    let iframeMutationObserver = null;
    let viewportState = {
        width: null,
        height: null,
        rotated: false,
        preset: 'responsive'
    };
    let fullscreenState = {
        active: false,
        placeholder: null,
        originalParent: null,
        originalNextSibling: null,
        previousBodyOverflow: ''
    };
    const rulerState = {
        enabled: false,
        lastHeight: 0
    };
    const previewState = {
        storyOverrides: {},
        lastRenderAt: null
    };
    if (window.__thymeleafletPendingOverrides && typeof window.__thymeleafletPendingOverrides === 'object') {
        previewState.storyOverrides = { ...window.__thymeleafletPendingOverrides };
        delete window.__thymeleafletPendingOverrides;
    }

    const dom = {
        previewHost: () => document.querySelector('#fragment-preview-host'),
        previewViewport: () => document.querySelector('#preview-viewport'),
        previewViewportFrame: () => document.querySelector('.preview-viewport-frame'),
        viewportSelect: () => document.getElementById('preview-viewport-select'),
        viewportRotateButton: () => document.getElementById('preview-viewport-rotate'),
        rulerToggleButton: () => document.getElementById('preview-ruler-toggle'),
        previewRuler: () => document.getElementById('preview-ruler'),
        fullscreenToggleButton: () => document.getElementById('preview-fullscreen-toggle'),
        fullscreenOverlay: () => document.getElementById('preview-fullscreen-overlay'),
        fullscreenHost: () => document.getElementById('preview-fullscreen-host'),
        previewContainer: () => document.getElementById('preview-container')
    };

    const viewportControls = {
        hasWidthPreset() {
            return Number.isFinite(viewportState.width);
        },
        isHeightFixed() {
            return Number.isFinite(viewportState.height);
        },
        getEffectiveSize() {
            if (!viewportControls.hasWidthPreset()) {
                return null;
            }
            const width = viewportState.width;
            const height = viewportState.height;
            return { width, height };
        }
    };
    const iframeControls = {};

    function setPreviewHeight(heightPx) {
        if (viewportControls.isHeightFixed()) {
            return;
        }
        const host = dom.previewHost();
        if (host) {
            host.style.height = heightPx + 'px';
        }
        updateRuler();
    }

    function resetPreviewHeight() {
        previewContentHeight = INITIAL_PREVIEW_HEIGHT;
        setPreviewHeight(INITIAL_PREVIEW_HEIGHT);
    }

    function handleResize(height) {
        if (viewportControls.isHeightFixed()) {
            return;
        }
        const adjustedHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
        const roundedHeight = Math.round(adjustedHeight / 8) * 8;
        const stabilizedHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, roundedHeight));

        previewContentHeight = stabilizedHeight;
        setPreviewHeight(stabilizedHeight);
    }

    function queueStableHeight(height) {
        if (viewportControls.isHeightFixed()) {
            return;
        }
        pendingHeight = height;
        lastMeasurementTime = Date.now();
        if (stableTimer) {
            clearTimeout(stableTimer);
        }
        stableTimer = setTimeout(() => {
            if (pendingHeight != null && Date.now() - lastMeasurementTime >= 300) {
                handleResize(pendingHeight);
                pendingHeight = null;
            }
        }, 320);
    }

    function getRulerHeight() {
        const frame = dom.previewViewportFrame();
        if (!frame) {
            return 0;
        }
        return Math.max(frame.scrollHeight, frame.clientHeight);
    }

    function renderRuler(height) {
        const ruler = dom.previewRuler();
        if (!ruler) {
            return;
        }
        const nextHeight = Math.max(height, MIN_HEIGHT);
        if (rulerState.lastHeight === nextHeight) {
            return;
        }
        rulerState.lastHeight = nextHeight;
        ruler.style.height = `${nextHeight}px`;
        const lines = [];
        for (let y = 0; y <= nextHeight; y += 100) {
            lines.push(
                `<div class="preview-ruler-line" style="top:${y}px;"><span class="preview-ruler-label">${y}</span></div>`
            );
        }
        ruler.innerHTML = lines.join('');
    }

    function updateRuler() {
        if (!rulerState.enabled) {
            return;
        }
        renderRuler(getRulerHeight());
    }

    function setRulerEnabled(enabled) {
        rulerState.enabled = enabled;
        const ruler = dom.previewRuler();
        if (!ruler) {
            return;
        }
        if (enabled) {
            ruler.classList.remove('hidden');
            updateRuler();
        } else {
            ruler.classList.add('hidden');
        }
        const button = dom.rulerToggleButton();
        if (button) {
            button.classList.toggle('ring-2', enabled);
            button.classList.toggle('ring-blue-500', enabled);
        }
    }

    function waitForFontsOnce() {
        if (fontsReadyPromise) {
            return fontsReadyPromise;
        }
        if (document.fonts && typeof document.fonts.ready === 'object') {
            fontsReadyPromise = Promise.race([
                document.fonts.ready,
                new Promise(resolve => setTimeout(resolve, 1000))
            ]);
        } else {
            fontsReadyPromise = Promise.resolve();
        }
        return fontsReadyPromise;
    }

    function cleanupIframeObservers() {
        if (iframeResizeObserver) {
            iframeResizeObserver.disconnect();
            iframeResizeObserver = null;
        }
        if (iframeMutationObserver) {
            iframeMutationObserver.disconnect();
            iframeMutationObserver = null;
        }
    }

    iframeControls.measureHeight = function(iframe) {
        try {
            const doc = iframe.contentDocument;
            if (!doc || !doc.documentElement || !doc.body) {
                return null;
            }
            const root = doc.getElementById('preview-root');
            const content = doc.getElementById('preview-content');
            if (root && content) {
                const contentRect = content.getBoundingClientRect();
                const rootStyle = getComputedStyle(root);
                const paddingTop = parseFloat(rootStyle.paddingTop) || 0;
                const paddingBottom = parseFloat(rootStyle.paddingBottom) || 0;
                const height = Math.ceil(contentRect.height + paddingTop + paddingBottom);
                return Math.max(MIN_HEIGHT, height);
            }
            return Math.ceil(Math.max(doc.documentElement.scrollHeight, doc.body.scrollHeight));
        } catch (error) {
            return null;
        }
    };

    iframeControls.setupObservers = function(iframe) {
        cleanupIframeObservers();

        const handle = () => {
            const height = iframeControls.measureHeight(iframe);
            if (height != null) {
                queueStableHeight(height);
            }
        };

        const attach = () => {
            try {
                const doc = iframe.contentDocument;
                if (!doc) return;
                if ('ResizeObserver' in window) {
                    iframeResizeObserver = new ResizeObserver(() => handle());
                    iframeResizeObserver.observe(doc.documentElement);
                }
                if ('MutationObserver' in window && doc.body) {
                    iframeMutationObserver = new MutationObserver(() => handle());
                    iframeMutationObserver.observe(doc.body, {
                        attributes: true,
                        childList: true,
                        subtree: true
                    });
                }
                handle();
            } catch (error) {
                // ignore access errors
            }
        };

        iframe.addEventListener('load', attach, { once: true });
        setTimeout(attach, 0);
    };

    function parseResourceList(rawValue) {
        return (rawValue || '')
            .split(',')
            .map(value => value.trim())
            .filter(Boolean);
    }

    iframeControls.getBackgroundColor = function(host) {
        if (!host) return 'transparent';
        const container = host.closest('#preview-container');
        if (!container) return 'transparent';
        return getComputedStyle(container).backgroundColor || 'transparent';
    };

    iframeControls.buildDocument = function(host, html, backgroundColor) {
        const styles = parseResourceList(host.dataset.previewStylesheets);
        const scripts = parseResourceList(host.dataset.previewScripts);
        const wrapper = host?.dataset?.previewWrapper || '';
        const wrappedHtml = wrapper && wrapper.includes('{{content}}')
            ? wrapper.replace('{{content}}', html)
            : html;
        const previewId = `preview-${Date.now()}-${previewIdCounter++}`;
        const resolvedBackground = backgroundColor || 'transparent';

        const headParts = [
            '<meta charset="utf-8">',
            '<meta name="viewport" content="width=device-width, initial-scale=1">'
        ];
        styles.forEach(href => {
            headParts.push(`<link rel="stylesheet" href="${href}">`);
        });
        headParts.push(
            `<style>
                html, body {
                    margin: 0;
                    padding: 0;
                    min-height: 100%;
                    background: transparent !important;
                }
                body { background-color: transparent !important; }
                #preview-root {
                    width: 100%;
                    min-height: 100%;
                    display: flex;
                    justify-content: center;
                    align-items: flex-start;
                    padding: 20px;
                    box-sizing: border-box;
                    background: transparent;
                }
                #preview-content {
                    display: inline-block;
                    width: fit-content;
                    max-width: 100%;
                }
            </style>`
        );

        const bodyParts = [
            `<div id="preview-root"><div id="preview-content">${wrappedHtml}</div></div>`
        ];
        scripts.forEach(src => {
            bodyParts.push(`<script src="${src}"></scr` + `ipt>`);
        });
        bodyParts.push(`<script>
            (function() {
                const previewId = ${JSON.stringify(previewId)};
                const postHeight = (height) => {
                    const safeHeight = Number.isFinite(height) ? height : 0;
                    parent.postMessage({ type: 'thymeleaflet:preview-height', id: previewId, height: safeHeight }, '*');
                };
                function notifyHeight() {
                const root = document.getElementById('preview-root');
                const content = document.getElementById('preview-content');
                let height = Math.ceil(
                    Math.max(
                        document.documentElement.scrollHeight,
                        document.body.scrollHeight
                    )
                );
                if (root && content) {
                    const rect = content.getBoundingClientRect();
                    const style = getComputedStyle(root);
                    const paddingTop = parseFloat(style.paddingTop) || 0;
                    const paddingBottom = parseFloat(style.paddingBottom) || 0;
                    height = Math.ceil(rect.height + paddingTop + paddingBottom);
                }
                postHeight(height);
            }
                if ('ResizeObserver' in window) {
                    const observer = new ResizeObserver(() => notifyHeight());
                    observer.observe(document.body);
                }
                if ('MutationObserver' in window) {
                    const mutationObserver = new MutationObserver(() => notifyHeight());
                    mutationObserver.observe(document.body, {
                        attributes: true,
                        childList: true,
                        subtree: true
                    });
                }
                document.addEventListener('click', () => {
                    setTimeout(notifyHeight, 30);
                    setTimeout(notifyHeight, 150);
                });
                window.addEventListener('load', notifyHeight);
                setTimeout(notifyHeight, 50);
                setTimeout(notifyHeight, 250);
            })();
        </scr` + `ipt>`);

        const doc = [
            '<!DOCTYPE html><html><head>',
            headParts.join(''),
            '</head><body>',
            bodyParts.join(''),
            '</body></html>'
        ].join('');

        return { doc, previewId };
    };

    iframeControls.ensureMessageListener = function() {
        if (window.__thymeleafletPreviewMessageListener) {
            return;
        }
        window.__thymeleafletPreviewMessageListener = true;
        window.addEventListener('message', event => {
            const data = event.data || {};
            if (data.type !== 'thymeleaflet:preview-height') {
                return;
            }
            const host = dom.previewHost();
            if (!host || host.dataset.previewId !== data.id) {
                return;
            }
            if (typeof data.height !== 'number') {
                return;
            }
            queueStableHeight(data.height);
        });
    };

    iframeControls.renderFrame = function(host, html) {
        host.innerHTML = '';
        const iframe = document.createElement('iframe');
        iframe.className = 'w-full border-0 bg-transparent';
        iframe.style.height = '100%';
        iframe.style.width = '100%';
        iframe.setAttribute('sandbox', 'allow-scripts allow-same-origin');
        iframe.setAttribute('title', 'Thymeleaflet Preview');
        const backgroundColor = iframeControls.getBackgroundColor(host);
        iframe.style.backgroundColor = backgroundColor;
        const { doc, previewId } = iframeControls.buildDocument(host, html, backgroundColor);
        host.dataset.previewId = previewId;
        iframe.srcdoc = doc;
        host.appendChild(iframe);
        iframeControls.setupObservers(iframe);
        viewportControls.applyState();
        iframeControls.refreshResponsiveHeight();
    };

    iframeControls.refreshResponsiveHeight = function() {
        if (viewportControls.isHeightFixed()) {
            return;
        }
        const host = dom.previewHost();
        const iframe = host?.querySelector('iframe');
        if (!iframe) {
            return;
        }
        const attempt = () => {
            const height = iframeControls.measureHeight(iframe);
            if (height != null) {
                handleResize(height);
            }
        };
        setTimeout(attempt, 50);
        setTimeout(attempt, 200);
        setTimeout(attempt, 600);
    };

    iframeControls.updateScrolling = function() {
        const host = dom.previewHost();
        if (!host) {
            return;
        }
        const iframe = host.querySelector('iframe');
        if (!iframe) {
            return;
        }
        iframe.setAttribute('scrolling', viewportControls.isHeightFixed() ? 'yes' : 'no');
    };

    Object.assign(viewportControls, {
        applyState() {
            const viewport = dom.previewViewport();
            if (!viewport) {
                return;
            }
            const frame = dom.previewViewportFrame();
            const host = dom.previewHost();
            const effective = viewportControls.getEffectiveSize();
            if (!effective) {
                viewport.style.width = '100%';
                viewport.style.height = 'auto';
                viewport.classList.remove('preview-viewport-fixed');
                viewport.classList.add('preview-viewport-responsive');
                viewport.classList.remove('bg-white');
                if (frame) {
                    frame.classList.remove('border', 'border-dashed', 'border-gray-300', 'bg-white');
                }
                if (host) {
                    host.style.width = '100%';
                    const iframe = host.querySelector('iframe');
                    const measuredHeight = iframe ? iframeControls.measureHeight(iframe) : null;
                    if (measuredHeight != null) {
                        handleResize(measuredHeight);
                    } else {
                        resetPreviewHeight();
                    }
                }
            } else {
                viewport.style.width = `${effective.width}px`;
                viewport.style.height = 'auto';
                viewport.classList.remove('preview-viewport-responsive');
                viewport.classList.add('preview-viewport-fixed');
                viewport.classList.add('bg-white');
                if (frame) {
                    frame.classList.add('border', 'border-dashed', 'border-gray-300', 'bg-white');
                }
                if (host) {
                    host.style.width = '100%';
                    const iframe = host.querySelector('iframe');
                    const measuredHeight = iframe ? iframeControls.measureHeight(iframe) : null;
                    if (measuredHeight != null) {
                        handleResize(measuredHeight);
                    } else {
                        resetPreviewHeight();
                    }
                }
            }
            iframeControls.updateScrolling();
            viewportControls.updateBadge();
            updateRuler();
        },
        updateFromSelect() {
            const select = dom.viewportSelect();
            if (!select) {
                return;
            }
            const option = select.options[select.selectedIndex];
            viewportState.preset = select.value || 'responsive';
            const width = option?.dataset?.width ? Number(option.dataset.width) : null;
            viewportState.width = Number.isFinite(width) ? width : null;
            viewportState.height = null;
            viewportState.rotated = false;
            viewportControls.applyState();
            viewportControls.updateRotateButton();
        },
        syncSelectFromState() {
            const select = dom.viewportSelect();
            if (!select) {
                return;
            }
            if (viewportState.preset && select.querySelector(`option[value="${viewportState.preset}"]`)) {
                select.value = viewportState.preset;
            } else {
                select.value = 'responsive';
                viewportState.preset = 'responsive';
            }
            const option = select.options[select.selectedIndex];
            const width = option?.dataset?.width ? Number(option.dataset.width) : null;
            viewportState.width = Number.isFinite(width) ? width : null;
            viewportState.height = null;
        }
    });

    function bindOnce(element, key, eventName, handler) {
        if (!element) {
            return;
        }
        if (element.dataset[key]) {
            return;
        }
        element.addEventListener(eventName, handler);
        element.dataset[key] = 'true';
    }

    Object.assign(viewportControls, {
        bindControls() {
            bindOnce(dom.viewportSelect(), 'boundViewportSelect', 'change', viewportControls.updateFromSelect);
            bindOnce(dom.viewportRotateButton(), 'boundViewportRotate', 'click', viewportControls.toggleRotation);
            bindOnce(dom.rulerToggleButton(), 'boundRulerToggle', 'click', () => {
                setRulerEnabled(!rulerState.enabled);
            });
        },
        updateRotateButton() {
            const button = dom.viewportRotateButton();
            if (!button) {
                return;
            }
            const disabled = !viewportControls.isHeightFixed();
            button.disabled = disabled;
            button.classList.toggle('opacity-50', disabled);
            button.classList.toggle('pointer-events-none', disabled);
        }
    });

    function setPreviewFullscreen(active) {
        const overlay = dom.fullscreenOverlay();
        const host = dom.fullscreenHost();
        const container = dom.previewContainer();
        if (!overlay || !host || !container) {
            return;
        }

        if (active === fullscreenState.active) {
            return;
        }

        if (active) {
            fullscreenState.originalParent = container.parentElement;
            fullscreenState.originalNextSibling = container.nextSibling;
            fullscreenState.placeholder = document.createComment('preview-fullscreen-placeholder');
            if (fullscreenState.originalParent) {
                fullscreenState.originalParent.insertBefore(fullscreenState.placeholder, container);
            }
            host.appendChild(container);
            container.classList.add('preview-fullscreen');
            overlay.classList.add('preview-fullscreen-active');
            overlay.setAttribute('aria-hidden', 'false');
            fullscreenState.previousBodyOverflow = document.body.style.overflow || '';
            document.body.style.overflow = 'hidden';
            fullscreenState.active = true;
        } else {
            overlay.classList.remove('preview-fullscreen-active');
            overlay.setAttribute('aria-hidden', 'true');
            container.classList.remove('preview-fullscreen');
            if (fullscreenState.placeholder && fullscreenState.originalParent) {
                fullscreenState.originalParent.insertBefore(container, fullscreenState.placeholder);
                fullscreenState.placeholder.remove();
            } else if (fullscreenState.originalParent) {
                fullscreenState.originalParent.appendChild(container);
            }
            document.body.style.overflow = fullscreenState.previousBodyOverflow;
            fullscreenState.active = false;
        }

        viewportControls.applyState();
        viewportControls.updateRotateButton();
        fullscreenControls.updateToggleButton();
    }

    const fullscreenControls = {
        updateToggleButton() {
            const button = dom.fullscreenToggleButton();
            if (!button) {
                return;
            }
            const enterLabel = button.dataset.labelEnter || 'Enter fullscreen';
            const exitLabel = button.dataset.labelExit || 'Exit fullscreen';
            const label = fullscreenState.active ? exitLabel : enterLabel;
            button.setAttribute('title', label);
            button.setAttribute('aria-label', label);
            const enterIcon = button.querySelector('[data-fullscreen-icon="enter"]');
            const exitIcon = button.querySelector('[data-fullscreen-icon="exit"]');
            if (enterIcon && exitIcon) {
                enterIcon.style.display = fullscreenState.active ? 'none' : 'inline-flex';
                exitIcon.style.display = fullscreenState.active ? 'inline-flex' : 'none';
            }
        },
        setActive(active) {
            setPreviewFullscreen(active);
        },
        toggle() {
            setPreviewFullscreen(!fullscreenState.active);
        },
        bindControls() {
            bindOnce(dom.fullscreenToggleButton(), 'boundFullscreenToggle', 'click', fullscreenControls.toggle);
            bindOnce(dom.fullscreenOverlay(), 'boundFullscreenOverlay', 'click', (event) => {
                if (event.target === event.currentTarget) {
                    fullscreenControls.setActive(false);
                }
            });
            if (!document.body.dataset.boundFullscreenEsc) {
                document.addEventListener('keydown', (event) => {
                    if (event.key === 'Escape' && fullscreenState.active) {
                        fullscreenControls.setActive(false);
                    }
                });
                document.body.dataset.boundFullscreenEsc = 'true';
            }
        }
    };

    Object.assign(viewportControls, {
        toggleRotation() {
            if (!viewportControls.isHeightFixed()) {
                return;
            }
            viewportState.rotated = !viewportState.rotated;
            viewportControls.applyState();
        },
        updateBadge() {
            const badge = document.getElementById('preview-viewport-badge');
            const select = dom.viewportSelect();
            if (!badge || !select) {
                return;
            }
            const option = select.options[select.selectedIndex];
            if (!option) {
                return;
            }
            badge.classList.remove('hidden');
            if (viewportControls.hasWidthPreset()) {
                const size = viewportControls.getEffectiveSize();
                badge.textContent = size ? `${size.width}px` : option.textContent;
            } else {
                badge.textContent = option.textContent;
            }
        }
    });

    function renderPreviewError(host, message) {
        const defaultMessage = document.body.dataset.previewLoadFailed || 'Failed to load preview.';
        const safeMessage = message || defaultMessage;
        const html = `
            <div class="w-full rounded border border-red-200 bg-red-50 px-4 py-3 text-left text-sm text-red-700">
                ${safeMessage}
            </div>
        `;
        iframeControls.renderFrame(host, html);
    }

    async function loadPreview(host) {
        const targetHost = host || dom.previewHost();
        if (!targetHost) return;

        iframeControls.ensureMessageListener();
        resetPreviewHeight();
        previewState.lastRenderAt = Date.now();

        const previewUrl = targetHost.dataset.previewUrl || '';
        if (!previewUrl) {
            targetHost.innerHTML = '';
            return;
        }

        try {
            const hasOverrides = previewState.storyOverrides && Object.keys(previewState.storyOverrides).length > 0;
            const response = await fetch(previewUrl, {
                method: hasOverrides ? 'POST' : 'GET',
                headers: {
                    'HX-Request': 'true',
                    'Content-Type': 'application/json'
                },
                body: hasOverrides ? JSON.stringify(previewState.storyOverrides) : undefined
            });
            if (!response.ok) {
                throw new Error(`Preview response status ${response.status}`);
            }
            const html = await response.text();
            if (html.includes('システムエラー') || html.includes('System Error')) {
                throw new Error('Preview error page detected');
            }
            iframeControls.renderFrame(targetHost, html);
            await waitForFontsOnce();
        } catch (error) {
            const baseMessage = document.body.dataset.previewLoadFailed || 'Failed to load preview.';
            const message = `${baseMessage}${error?.message ? ` (${error.message})` : ''}`;
            renderPreviewError(targetHost, message);
        }
    }

    function setStoryOverrides(overrides) {
        previewState.storyOverrides = overrides && typeof overrides === 'object'
            ? { ...overrides }
            : {};
    }

    function resetToDefaults() {
        previewState.storyOverrides = {};
        loadPreview();
    }

    function render() {
        loadPreview();
    }

    function initializePreviewUI() {
        loadPreview();
        viewportControls.bindControls();
        fullscreenControls.bindControls();
        viewportControls.syncSelectFromState();
        viewportControls.applyState();
        viewportControls.updateRotateButton();
        fullscreenControls.updateToggleButton();
        setRulerEnabled(false);
    }

    window.__thymeleafletResetPreviewHeight = resetPreviewHeight;
    window.__thymeleafletLoadShadowPreview = loadPreview;
    window.__thymeleafletLoadPreview = loadPreview;
    window.__thymeleafletRefreshPreview = () => loadPreview();
    window.__thymeleafletPreview = {
        setStoryOverrides,
        render,
        resetToDefaults
    };

    document.addEventListener('DOMContentLoaded', initializePreviewUI);

    document.addEventListener('htmx:afterSettle', function(event) {
        if (event.detail.target && event.detail.target.id === 'main-content-area') {
            initializePreviewUI();
        }
    });
})();
