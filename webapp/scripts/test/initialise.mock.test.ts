import {afterAll, beforeAll, beforeEach, describe, expect, jest, test} from "@jest/globals";
import {defaultHtml, getRequestsReceived, optionsHtml, resetRequestTracking, setUpFetchMock} from "./requestMocking";
import {removeWhitespace} from "./helper";
import {initialise} from "../initialise";

beforeAll(() => {
    setUpFetchMock();
});

beforeEach(async () => {
    resetRequestTracking();
    document.body.innerHTML = defaultHtml;
    await initialise(true);
});

afterAll(() => {
    jest.clearAllMocks();
});

describe("initialise behaves correctly", () => {
    test("a request is made to get the language selector HTML", () => {
        expect(getRequestsReceived()).toContainEqual({url: 'get-lang-selector', request: {method: 'GET'}});
    });

    test("lang selector is populated correctly", () => {
        expect(removeWhitespace(document.getElementById('lang-selector').innerHTML)).toEqual(
            removeWhitespace(optionsHtml));
    });

    test("undo button is disabled", () => {
        const undoButton = document.getElementById('undoButton') as HTMLButtonElement;
        expect(undoButton.getAttributeNames()).toContain('disabled');
    });

    test("redo button is disabled", () => {
        const redoButton = document.getElementById('redoButton') as HTMLButtonElement;
        expect(redoButton.getAttributeNames()).toContain('disabled');
    });
});
