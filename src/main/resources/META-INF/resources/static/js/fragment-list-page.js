// シンタックスハイライト共通処理関数
function applyHighlighting(container) {
    if (!container) return;

    const codeBlocks = container.querySelectorAll('pre code');
    codeBlocks.forEach(function(block) {
        // 既にハイライト済みの場合はリセット
        if (block.dataset.highlighted) {
            delete block.dataset.highlighted;
        }
        hljs.highlightElement(block);
    });
}

// ページ読み込み時にHighlight.jsを初期化
document.addEventListener('DOMContentLoaded', function() {
    hljs.highlightAll();
});

// HTMX部分更新後にHighlight.jsを再適用
document.addEventListener('htmx:afterSettle', function(event) {
    applyHighlighting(event.detail.target);
});

// HTMXリクエスト完了後にもHighlight.jsを適用（フォールバック）
document.addEventListener('htmx:afterRequest', function(event) {
    // 使用例エリアが更新された場合
    if (event.detail.target && event.detail.target.id === 'usage-example') {
        setTimeout(() => applyHighlighting(event.detail.target), 50);
    }
});

// メインコンテンツエリアの更新後にAlpine.js状態を同期
document.addEventListener('htmx:afterSettle', function(event) {
    // メインコンテンツエリアが更新された場合
    if (event.detail.target && event.detail.target.id === 'main-content-area') {
        // URLから現在の選択状態を抽出
        const currentUrl = window.location.pathname;
        const urlMatch = currentUrl.match(/\/thymeleaflet\/([^\/]+)\/([^\/]+)\/([^\/]+)/);

        if (urlMatch) {
            const [, templatePath, fragmentName, encodedStoryName] = urlMatch;
            const storyName = decodeURIComponent(encodedStoryName);
            console.log('🔍 URL解析:', { templatePath, fragmentName, encodedStoryName, storyName });

            // Alpine.jsの状態を取得
            const alpineData = Alpine.$data(document.querySelector('[x-data]'));
            console.log('🔍 alpineData取得:', alpineData ? 'OK' : 'NG');

            // テンプレートパスを正規化（ドットをスラッシュに戻す）
            const normalizedTemplatePath = alpineData ? alpineData.templatePathForFilePath(templatePath) : templatePath;
            console.log('🔍 正規化:', { templatePath, normalizedTemplatePath });
            if (alpineData && alpineData.allFragments) {
                console.log('🔍 フラグメント数:', alpineData.allFragments.length);
                const fragment = alpineData.allFragments.find(f =>
                    f?.templatePath === normalizedTemplatePath &&
                    f?.fragmentName === fragmentName
                );
                console.log('🔍 フラグメント検索:', fragment ? 'found' : 'not found');

                if (fragment) {
                    console.log('🔍 selectedFragment更新:', fragment);
                    alpineData.selectedFragment = fragment;

                    // ストーリーも更新
                    if (storyName && fragment.stories) {
                        const story = fragment.stories.find(s => s?.storyName === storyName);
                        if (story) {
                            alpineData.selectedStory = story;
                        }
                    }

                    console.log('Alpine.js状態を更新しました:', {
                        templatePath: normalizedTemplatePath,
                        fragmentName: fragmentName,
                        storyName: storyName
                    });
                }
            }
        }
    }
});
