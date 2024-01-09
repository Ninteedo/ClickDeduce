module.exports = {
    preset: "ts-jest",
    testEnvironment: "jsdom",
    testRegex: '(/__tests__/.*|(\\.|/)(test|spec))\\.[tj]s$',
    moduleNameMapper: {
        '\\.(css|less|scss|sass)$': 'jest-css-modules-transform'
    }
};
