/** @type {import('tailwindcss').Config} */
module.exports = {
  // thymeleaflet-spring-boot-starterモジュール専用の設定
  content: [
    // テンプレートファイル
    './src/main/resources/templates/**/*.html',
    // 静的リソース内のHTML
    './src/main/resources/static/**/*.html',
    // CSSファイル内のクラス参照
    './src/main/resources/storybook/**/*.css',
    // JavaコントローラーやサービスクラスでのCSSクラス参照
    './src/main/java/**/*.java'
  ],
  theme: {
    extend: {
      // thymeleaflet固有のカスタムカラー
      colors: {
        'primary': {
          DEFAULT: '#0052cc',
          light: '#4c9aff',
        },
        'secondary': {
          DEFAULT: '#6c757d',
          light: '#adb5bd',
        },
        'success': {
          DEFAULT: '#00875a',
          light: '#e3fcef',
        },
        'danger': {
          DEFAULT: '#de350b',
          light: '#ffebe6',
        },
        'warning': {
          DEFAULT: '#ff8b00',
          light: '#fff4e6',
        },
        'neutral': {
          50: '#fafbfc',
          100: '#f4f5f7',
          200: '#ebecf0', 
          300: '#dfe1e6',
          400: '#a3a3a3',
          500: '#737373',
          600: '#525252',
          700: '#425a6e',
          800: '#262626',
          900: '#091e42',
        }
      },
      fontFamily: {
        // 日本語フォントの設定
        'sans': [
          'system-ui',
          '-apple-system',
          'Hiragino Kaku Gothic ProN',
          'Yu Gothic',
          'Meiryo',
          'sans-serif'
        ],
        'mono': [
          'ui-monospace',
          'SFMono-Regular', 
          'Menlo',
          'Monaco',
          'Consolas',
          'Liberation Mono',
          'Courier New',
          'monospace'
        ]
      },
      fontSize: {
        // thymeleaflet用のフォントサイズ
        'xs': ['0.75rem', { lineHeight: '1rem' }],
        'sm': ['0.875rem', { lineHeight: '1.25rem' }],
        'base': ['1rem', { lineHeight: '1.5rem' }],
        'lg': ['1.125rem', { lineHeight: '1.75rem' }],
        'xl': ['1.25rem', { lineHeight: '1.75rem' }],
        '2xl': ['1.5rem', { lineHeight: '2rem' }],
        '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
      },
      spacing: {
        // thymeleaflet用のスペーシング
        '18': '4.5rem',
        '88': '22rem',
      },
      borderRadius: {
        // thymeleaflet用の角丸設定
        'sm': '0.25rem',
        'DEFAULT': '0.375rem',
        'md': '0.375rem',
        'lg': '0.5rem',
        'xl': '0.75rem',
        '2xl': '1rem',
      },
      boxShadow: {
        // thymeleaflet用のシャドウ
        'sm': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        'DEFAULT': '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
        'md': '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
        'lg': '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
        'inner': 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)',
      },
      animation: {
        // thymeleaflet用のアニメーション
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'fade-out': 'fadeOut 0.3s ease-in-out',
        'shimmer': 'shimmer 1.5s infinite',
      },
      keyframes: {
        fadeIn: {
          'from': { opacity: '0' },
          'to': { opacity: '1' },
        },
        fadeOut: {
          'from': { opacity: '1' },
          'to': { opacity: '0' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      }
    },
  },
  plugins: [
    // フォーム要素のデフォルトスタイル
    require('@tailwindcss/forms')({
      strategy: 'class', // .form-inputのようなクラスベースで適用
    }),
    // タイポグラフィプラグイン
    require('@tailwindcss/typography'),
  ],
  corePlugins: {
    // プリフライトスタイル（CSSリセット）を含める
    preflight: true,
  },
}