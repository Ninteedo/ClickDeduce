import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {MockResponse} from "./MockResponse";
import {initialise} from "./script";

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

const langSelectorLanguages = ["LArith", "LIf"];
const optionsHtml = langSelectorLanguages.map(lang => {
    return `<option value="${lang}">${lang}</option>`;
}).join('\n');
const langSelectorHtml = `
    <select id="lang-selector" name="lang-name">
      ${optionsHtml}
    </select>
`;


let requestsReceived: { url: string, request: any }[] = [];
let dummyFetchResponse: any = null;

function setDummyFetchResponse(response: any): void {
    dummyFetchResponse = response;
}

function clearDummyFetchResponse(): void {
    dummyFetchResponse = null;
}

function fetchMock(url: string, request: any): Promise<Response> {
    requestsReceived.push({ url, request });

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
    requestsReceived = [];
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
    });

    test("fetch returns correct language selector HTML", async () => {
        fetch('get-lang-selector', { method: 'GET' }).then(response => response.json()).then(contents =>
            expect(contents).toEqual({ langSelectorHtml })
        );
    });

    test("fetch results in an error if using POST on get-lang-selector", async () => {
        fetch('get-lang-selector', { method: 'POST' }).then(response => response.json()).catch(error =>
            expect(error).toEqual("Error: Cannot use POST on get-lang-selector")
        );
    });
});

describe("initialise behaves correctly", () => {
    test("a request is made to get the language selector HTML", () => {
        expect(requestsReceived).toContainEqual({url: 'get-lang-selector', request: {method: 'GET'}});
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

function removeWhitespace(str: string): string {
    return str.replace(/\s/g, '');
}
