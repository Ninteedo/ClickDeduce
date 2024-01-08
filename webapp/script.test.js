const { test, beforeEach, afterEach, describe, expect } = require("@jest/globals");

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

const script = require("./script.js");

const langSelectorLanguages = ["LArith", "LIf"];
const optionsHtml = langSelectorLanguages.map(lang => {
    return `<option value="${lang}">${lang}</option>`;
}).join('\n');
const langSelectorHtml = `
    <select id="lang-selector" name="lang-name">
      ${optionsHtml}
    </select>
`;


let dummyFetchResponse;

function setDummyFetchResponse(response) {
    dummyFetchResponse = response;
}

function clearDummyFetchResponse() {
    dummyFetchResponse = null;
}

function fetchMock(url, request) {
    if (url === 'get-lang-selector') {
        if (request['method'] === 'GET') {
            return Promise.resolve({
                json: () => Promise.resolve({
                    langSelectorHtml
                })
            });
        } else {
            return fail("get-lang-selector endpoint must be called with GET");
        }
    } else if (url === 'dummy-url') {
        return Promise.resolve({
            json: () => Promise.resolve(dummyFetchResponse)
        });
    } else {
        return undefined;
    }
}

global.fetch = jest.fn(fetchMock);

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
});
