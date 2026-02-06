(function() {
    if (window.__thymeleafletPreviewControlsInitialized) {
        return;
    }
    window.__thymeleafletPreviewControlsInitialized = true;

    const INITIAL_PREVIEW_HEIGHT = 80;
    const MIN_HEIGHT = 60;
    const MAX_HEIGHT = 1200;
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
    const previewState = {
        storyOverrides: {},
        lastRenderAt: null
    };
    if (window.__thymeleafletPendingOverrides && typeof window.__thymeleafletPendingOverrides === 'object') {
        previewState.storyOverrides = { ...window.__thymeleafletPendingOverrides };
        delete window.__thymeleafletPendingOverrides;
    }

    function getPreviewHost() {
        return document.querySelector('#fragment-preview-host');
    }

    function getPreviewViewport() {
        return document.querySelector('#preview-viewport');
    }

    function getViewportSelect() {
        return document.getElementById('preview-viewport-select');
    }

    function getViewportRotateButton() {
        return document.getElementById('preview-viewport-rotate');
    }

    function isFixedViewport() {
        return Number.isFinite(viewportState.width) && Number.isFinite(viewportState.height);
    }

    function getEffectiveViewportSize() {
        if (!isFixedViewport()) {
            return null;
        }
        const width = viewportState.rotated ? viewportState.height : viewportState.width;
        const height = viewportState.rotated ? viewportState.width : viewportState.height;
        return { width, height };
    }

    function setPreviewHeight(heightPx) {
        if (isFixedViewport()) {
            return;
        }
        const host = getPreviewHost();
        if (host) {
            host.style.height = heightPx + 'px';
        }
    }

    function resetPreviewHeight() {
        previewContentHeight = INITIAL_PREVIEW_HEIGHT;
        setPreviewHeight(INITIAL_PREVIEW_HEIGHT);
    }

    function handleResize(height) {
        if (isFixedViewport()) {
            return;
        }
        const adjustedHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
        const roundedHeight = Math.round(adjustedHeight / 8) * 8;
        const stabilizedHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, roundedHeight));

        previewContentHeight = stabilizedHeight;
        setPreviewHeight(stabilizedHeight);
    }

    function queueStableHeight(height) {
        if (isFixedViewport()) {
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

    function measureIframeHeight(iframe) {
        try {
            const doc = iframe.contentDocument;
            if (!doc || !doc.documentElement || !doc.body) {
                return null;
            }
            return Math.ceil(Math.max(doc.documentElement.scrollHeight, doc.body.scrollHeight));
        } catch (error) {
            return null;
        }
    }

    function setupIframeObservers(iframe) {
        cleanupIframeObservers();

        const handle = () => {
            const height = measureIframeHeight(iframe);
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
    }

    function parseResourceList(rawValue) {
        return (rawValue || '')
            .split(',')
            .map(value => value.trim())
            .filter(Boolean);
    }

    function getPreviewBackgroundColor(host) {
        if (!host) return 'transparent';
        const container = host.closest('#preview-container');
        if (!container) return 'transparent';
        return getComputedStyle(container).backgroundColor || 'transparent';
    }

    function buildPreviewDocument(host, html, backgroundColor) {
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
                html, body { margin: 0; padding: 0; background: ${resolvedBackground} !important; }
                body { background-color: ${resolvedBackground} !important; }
                #preview-root {
                    width: 100%;
                    min-height: 50px;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    padding: 20px;
                    box-sizing: border-box;
                    background: ${resolvedBackground};
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
                    const height = Math.ceil(
                        Math.max(
                            document.documentElement.scrollHeight,
                            document.body.scrollHeight
                        )
                    );
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
    }

    function ensurePreviewMessageListener() {
        if (window.__thymeleafletPreviewMessageListener) {
            return;
        }
        window.__thymeleafletPreviewMessageListener = true;
        window.addEventListener('message', event => {
            const data = event.data || {};
            if (data.type !== 'thymeleaflet:preview-height') {
                return;
            }
            const host = getPreviewHost();
            if (!host || host.dataset.previewId !== data.id) {
                return;
            }
            if (typeof data.height !== 'number') {
                return;
            }
            queueStableHeight(data.height);
        });
    }

    function renderPreviewFrame(host, html) {
        host.innerHTML = '';
        const iframe = document.createElement('iframe');
        iframe.className = 'w-full border-0 bg-transparent';
        iframe.style.height = '100%';
        iframe.style.width = '100%';
        iframe.setAttribute('sandbox', 'allow-scripts allow-same-origin');
        iframe.setAttribute('title', 'Thymeleaflet Preview');
        const backgroundColor = getPreviewBackgroundColor(host);
        iframe.style.backgroundColor = backgroundColor;
        const { doc, previewId } = buildPreviewDocument(host, html, backgroundColor);
        host.dataset.previewId = previewId;
        iframe.srcdoc = doc;
        host.appendChild(iframe);
        setupIframeObservers(iframe);
        applyViewportState();
        refreshResponsiveHeight();
    }

    function refreshResponsiveHeight() {
        if (isFixedViewport()) {
            return;
        }
        const host = getPreviewHost();
        const iframe = host?.querySelector('iframe');
        if (!iframe) {
            return;
        }
        const attempt = () => {
            const height = measureIframeHeight(iframe);
            if (height != null) {
                handleResize(height);
            }
        };
        setTimeout(attempt, 50);
        setTimeout(attempt, 200);
        setTimeout(attempt, 600);
    }

    function updateIframeScrolling() {
        const host = getPreviewHost();
        if (!host) {
            return;
        }
        const iframe = host.querySelector('iframe');
        if (!iframe) {
            return;
        }
        iframe.setAttribute('scrolling', isFixedViewport() ? 'yes' : 'no');
    }

    function applyViewportState() {
        const viewport = getPreviewViewport();
        if (!viewport) {
            return;
        }
        const host = getPreviewHost();
        const effective = getEffectiveViewportSize();
        if (!effective) {
            viewport.style.width = '100%';
            viewport.style.height = 'auto';
            viewport.classList.remove('preview-viewport-fixed');
            viewport.classList.add('preview-viewport-responsive');
            viewport.classList.remove('border', 'border-dashed', 'border-gray-300', 'bg-white');
            if (host) {
                host.style.width = '100%';
                const iframe = host.querySelector('iframe');
                const measuredHeight = iframe ? measureIframeHeight(iframe) : null;
                if (measuredHeight != null) {
                    handleResize(measuredHeight);
                } else {
                    resetPreviewHeight();
                }
            }
        } else {
            viewport.style.width = `${effective.width}px`;
            viewport.style.height = `${effective.height}px`;
            viewport.classList.remove('preview-viewport-responsive');
            viewport.classList.add('preview-viewport-fixed');
            viewport.classList.add('border', 'border-dashed', 'border-gray-300', 'bg-white');
            if (host) {
                host.style.width = '100%';
                host.style.height = '100%';
            }
        }
        updateIframeScrolling();
        updateViewportBadge();
    }

    function updateViewportFromSelect() {
        const select = getViewportSelect();
        if (!select) {
            return;
        }
        const option = select.options[select.selectedIndex];
        viewportState.preset = select.value || 'responsive';
        const width = option?.dataset?.width ? Number(option.dataset.width) : null;
        const height = option?.dataset?.height ? Number(option.dataset.height) : null;
        viewportState.width = Number.isFinite(width) ? width : null;
        viewportState.height = Number.isFinite(height) ? height : null;
        viewportState.rotated = false;
        applyViewportState();
        updateViewportRotateButton();
    }

    function syncViewportSelectFromState() {
        const select = getViewportSelect();
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
        const height = option?.dataset?.height ? Number(option.dataset.height) : null;
        viewportState.width = Number.isFinite(width) ? width : null;
        viewportState.height = Number.isFinite(height) ? height : null;
    }

    function bindViewportControls() {
        const select = getViewportSelect();
        if (select && !select.dataset.boundViewport) {
            select.addEventListener('change', updateViewportFromSelect);
            select.dataset.boundViewport = 'true';
        }
        const rotate = getViewportRotateButton();
        if (rotate && !rotate.dataset.boundViewport) {
            rotate.addEventListener('click', toggleViewportRotation);
            rotate.dataset.boundViewport = 'true';
        }
    }

    function updateViewportRotateButton() {
        const button = getViewportRotateButton();
        if (!button) {
            return;
        }
        const disabled = !isFixedViewport();
        button.disabled = disabled;
        button.classList.toggle('opacity-50', disabled);
        button.classList.toggle('pointer-events-none', disabled);
    }

    function toggleViewportRotation() {
        if (!isFixedViewport()) {
            return;
        }
        viewportState.rotated = !viewportState.rotated;
        applyViewportState();
    }

    function updateViewportBadge() {
        const badge = document.getElementById('preview-viewport-badge');
        const select = getViewportSelect();
        if (!badge || !select) {
            return;
        }
        const option = select.options[select.selectedIndex];
        if (!option) {
            return;
        }
        badge.classList.remove('hidden');
        if (isFixedViewport()) {
            const size = getEffectiveViewportSize();
            badge.textContent = size ? `${size.width}×${size.height}` : option.textContent;
        } else {
            badge.textContent = option.textContent;
        }
    }

    function renderPreviewError(host, message) {
        const defaultMessage = document.body.dataset.previewLoadFailed || 'Failed to load preview.';
        const safeMessage = message || defaultMessage;
        const html = `
            <div class="w-full rounded border border-red-200 bg-red-50 px-4 py-3 text-left text-sm text-red-700">
                ${safeMessage}
            </div>
        `;
        renderPreviewFrame(host, html);
    }

    async function loadPreview(host) {
        const targetHost = host || getPreviewHost();
        if (!targetHost) return;

        ensurePreviewMessageListener();
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
            renderPreviewFrame(targetHost, html);
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

    window.__thymeleafletResetPreviewHeight = resetPreviewHeight;
    window.__thymeleafletLoadShadowPreview = loadPreview;
    window.__thymeleafletLoadPreview = loadPreview;
    window.__thymeleafletRefreshPreview = () => loadPreview();
    window.__thymeleafletPreview = {
        setStoryOverrides,
        render,
        resetToDefaults
    };

    document.addEventListener('DOMContentLoaded', function() {
        loadPreview();
        bindViewportControls();
        syncViewportSelectFromState();
        applyViewportState();
        updateViewportRotateButton();
    });

    document.addEventListener('htmx:afterSettle', function(event) {
        if (event.detail.target && event.detail.target.id === 'main-content-area') {
            loadPreview();
            bindViewportControls();
            syncViewportSelectFromState();
            applyViewportState();
            updateViewportRotateButton();
        }
    });
})();
