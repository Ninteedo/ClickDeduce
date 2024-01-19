import {afterAll, beforeAll, beforeEach, describe, expect, jest, test} from "@jest/globals";
import {initialise} from "../initialise";
import {loadHtmlTemplate} from "./helper";
import {
    defaultHtml,
    langSelectorHtml,
    resetRequestTracking,
    setDummyFetchResponse,
    setUpFetchMock
} from "./requestMocking";

export const plusNodeArithHTML = loadHtmlTemplate('plus_node_arith');
export const numNodeArithHTML = loadHtmlTemplate('num_node_arith');

beforeAll(() => {
    setUpFetchMock();
});

beforeEach(async () => {
    resetRequestTracking();
    document.body.innerHTML = defaultHtml;
    await initialise();
});

afterAll(() => {
    jest.clearAllMocks();
});


describe("fetch is correctly mocked", () => {
    test("fetch returns the set response", async () => {
        let data = {test: "test"};
        setDummyFetchResponse(data);
        fetch('dummy-url', {}).then(response => response.json()).then(contents =>
            expect(contents).toEqual(data)
        );
    });

    test("fetch returns correct language selector HTML", async () => {
        fetch('get-lang-selector', {method: 'GET'}).then(response => response.json()).then(contents =>
            expect(contents).toEqual({langSelectorHtml})
        );
    });

    test("fetch results in an error if using POST on get-lang-selector", async () => {
        fetch('get-lang-selector', {method: 'POST'}).then(response => response.ok).then(ok =>
            expect(ok).toEqual(false)
        );
    });
});
