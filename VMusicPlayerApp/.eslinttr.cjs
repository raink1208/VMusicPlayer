// .eslintrc.cjs
module.exports = {
    root: true,
    env: {
        browser: true,
        es2022: true,
        node: true,
    },
    parserOptions: {
        ecmaVersion: "latest",
        sourceType: "module",
    },
    extends: [
        "@nuxt/eslint-config",
        "plugin:vue/vue3-recommended",
        "plugin:prettier/recommended",
    ],
    rules: {
        "vue/multi-word-component-names": "off",
        "prettier/prettier": ["error", { endOfLine: "auto" }],
    },
}
