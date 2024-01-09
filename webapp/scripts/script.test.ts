const { test, beforeEach, afterEach, describe, expect} = require("@jest/globals");

const defaultHtml = `
    <div id="lang-selector-div"></div>
    <button onclick="undo()" id="undoButton">Undo</button>
    <button onclick="redo()" id="redoButton">Redo</button>
    <div id="tree-container">
      <div id="tree-buttons">
        <button onclick="zoomToFit()">
          <img src="images/zoom_to_fit.svg" alt="Zoom to Fit">
        </button>
      </div>
      <div id="tree"></div>
    </div>
`;

import {initialise} from "./script";

const langSelectorLanguages = ["LArith", "LIf"];
const optionsHtml = langSelectorLanguages.map(lang => {
    return `<option value="${lang}">${lang}</option>`;
}).join('\n');
const langSelectorHtml = `
    <select id="lang-selector" name="lang-name">
      ${optionsHtml}
    </select>
`;


let dummyFetchResponse: any = null;

function setDummyFetchResponse(response: any): void {
    dummyFetchResponse = response;
}

function clearDummyFetchResponse(): void {
    dummyFetchResponse = null;
}

class MockResponse {
    private body: string;
    private status: number;
    private statusText: string;
    private headers: Map<string, string>;

    constructor(body: string, init: any = {}) {
        this.body = body;
        this.status = init.status || 200;
        this.statusText = init.statusText || 'OK';

        const headersIterable = Object.entries(init.headers || {});
        // @ts-ignore
        this.headers = new Map(headersIterable);
    }

    async text(): Promise<string> {
        return Promise.resolve(this.body);
    }

    async json(): Promise<any> {
        try {
            return Promise.resolve(JSON.parse(this.body));
        } catch (e) {
            return Promise.reject(e);
        }
    }
}

function fetchMock(url: string, request: any): Promise<Response> {
    let responseJson: any = null;
    if (url === 'get-lang-selector') {
        if (request['method'] === 'GET') {
            responseJson = {langSelectorHtml};
        } else {
            return Promise.reject("Error: Cannot use POST on get-lang-selector");
        }
    } else if (url === 'dummy-url') {
        responseJson = dummyFetchResponse;
    } else {
        return Promise.reject("Error: Unknown URL");
    }
    const response: MockResponse = new MockResponse(JSON.stringify(responseJson),
        {
            status: 200,
            headers: {
                'Content-type': 'application/json'
            }
        }
    );
    // @ts-ignore
    return Promise.resolve(response);
}

global.fetch = jest.fn(fetchMock);

// mock panzoom module, doesn't need to do anything
jest.mock('panzoom', () => ({
    __esModule: true,
    default: jest.fn().mockImplementation(() => ({

    }))
}))

beforeEach(() => {
    document.body.innerHTML = defaultHtml;
    initialise();
});

afterEach(() => {
    clearDummyFetchResponse();
});

describe("fetch is correctly mocked", () => {
    test("fetch returns the set response", async () => {
        let data = { test: "test" };
        setDummyFetchResponse(data);
        fetch('dummy-url', {}).then(response => response.json()).then(contents =>
            expect(contents).toEqual(data)
        );
    })

    test("fetch returns correct language selector HTML", async () => {
        fetch('get-lang-selector', { method: 'GET' }).then(response => response.json()).then(contents =>
            expect(contents).toEqual({ langSelectorHtml })
        );
    })

    test("fetch results in an error if using POST on get-lang-selector", async () => {
        fetch('get-lang-selector', { method: 'POST' }).then(response => response.json()).catch(error =>
            expect(error).toEqual("Error: Cannot use POST on get-lang-selector")
        );
    })
});
