import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {MockResponse} from "./MockResponse";
import {initialise} from "./script";

const defaultHtml = `
    <div id="lang-selector-div"></div>
    <div id="mode-selector-div">
      <form>
        <input type="radio" id="edit-mode-radio" name="mode" value="edit" checked="checked">
        <label for="edit-mode-radio">Edit</label>
        <input type="radio" id="type-check-mode-radio" name="mode" value="type-check">
        <label for="type-check-mode-radio">Type-Check</label>
        <input type="radio" id="eval-mode-radio" name="mode" value="eval">
        <label for="eval-mode-radio">Evaluate</label>
      </form>
    </div>
    
    <form onsubmit="handleSubmit(event, '/start-node-blank')">
      <input id="start-node-button" type="submit" value="Start Node Blank">
    </form>
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
    
    <div id="custom-context-menu" class="custom-menu">
      <ul>
        <li onclick="clearTreeNode(event)">Delete</li>
        <li onclick="copyTreeNode(event)">Copy</li>
        <li onclick="pasteTreeNode(event)">Paste</li>
        <li onclick="zoomToFit()">Zoom to Fit</li>
      </ul>
    </div>
    
    <div id="error-message" class="error-message fade-out"></div>
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

const startNodeBlankArithHTML = `
    <div class="subtree axiom" data-tree-path="" data-term="BlankExprDropDown()" data-node-string="ExprChoiceNode()"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="1" data-tree-path="" style="display: inline;"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="annotation-axiom">ExprChoice</div></div>
`

const invalidResourceResponse: MockResponse = new MockResponse("The requested resource could not be found.", {
    status: 404,
    statusText: 'Not Found',
    headers: {
        'Content-type': 'application/json'
    }
});

let requestsReceived: { url: string, request: any }[] = [];
let dummyFetchResponse: any = null;
let actionFetchResponse: { nodeString: string, html: string } = null;

function checkActionRequestExecuted(actionName: string, langName: string, modeName: string, nodeString: string,
                                    treePath: string, extraArgs: string[]): void {
    const correctRequest = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            langName,
            modeName,
            actionName,
            nodeString,
            treePath,
            extraArgs
        })
    }
    expect(requestsReceived).toContainEqual({url: 'process-action', request: correctRequest});
}

function fetchMock(url: string, request: any): Promise<Response> {
    if (url.startsWith('/')) {
        url = url.substring(1);
    }
    requestsReceived.push({ url, request });

    console.log("fetchMock called with url: " + url + " and request: " + JSON.stringify(request));

    let responseJson: any = null;
    if (url === 'get-lang-selector') {
        if (request['method'] === 'GET') {
            responseJson = {langSelectorHtml};
        }
    } else if (url === 'dummy-url') {
        responseJson = dummyFetchResponse;
    } else if (url === 'start-node-blank') {
        if (request['method'] === 'POST') {
            responseJson = {nodeString: "ExprChoiceNode()", html: startNodeBlankArithHTML};
        }
    } else if (url === 'process-action') {
        if (request['method'] === 'POST') {
            if (actionFetchResponse === null) {
                actionFetchResponse = {nodeString: "", html: ""};
            }
            responseJson = actionFetchResponse;
            actionFetchResponse = null;
        }
    }

    // resource not found
    if (responseJson === null) {
        // @ts-ignore
        return Promise.resolve(invalidResourceResponse);
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

function slightDelay(delay: number = 10): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, delay));
}

beforeEach(() => {
    requestsReceived = [];
    document.body.innerHTML = defaultHtml;
    initialise();
});

afterEach(() => {
    dummyFetchResponse = null;
    actionFetchResponse = null;
});

describe("fetch is correctly mocked", () => {
    test("fetch returns the set response", async () => {
        let data = { test: "test" };
        dummyFetchResponse = data;
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
        fetch('get-lang-selector', { method: 'POST' }).then(response => response.ok).then(ok =>
            expect(ok).toEqual(false)
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

describe("start new node button behaves correctly", () => {
    test("clicking the button makes a request to the server", () => {
        const startNodeButton = document.getElementById('start-node-button') as HTMLButtonElement;
        startNodeButton.click();
        const correctRequest = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({langName: langSelectorLanguages[0]})
        }
        expect(requestsReceived).toContainEqual({url: 'start-node-blank', request: correctRequest});
    });

    test("the contents of the tree div are replaced with the new tree HTML", async () => {
        const startNodeButton = document.getElementById('start-node-button') as HTMLButtonElement;
        startNodeButton.click();

        await slightDelay();

        const tree = document.getElementById('tree');
        expect(removeWhitespace(tree.innerHTML)).toEqual(removeWhitespace(startNodeBlankArithHTML));
    });

    test("the request made respects the selected language", async () => {
        const langSelector = document.getElementById('lang-selector') as HTMLSelectElement;
        langSelector.selectedIndex = 1;
        langSelector.dispatchEvent(new Event('change'));
        const startNodeButton = document.getElementById('start-node-button') as HTMLButtonElement;
        startNodeButton.click();
        const correctRequest = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({langName: langSelectorLanguages[1]})
        }

        await slightDelay();

        expect(requestsReceived).toContainEqual({url: 'start-node-blank', request: correctRequest});
    });

    test("changing the selected language causes an identity action", async () => {
        const startNodeButton = document.getElementById('start-node-button') as HTMLButtonElement;
        startNodeButton.click();

        await slightDelay();

        const langSelector = document.getElementById('lang-selector') as HTMLSelectElement;
        console.log(langSelector.selectedIndex);
        langSelector.selectedIndex = 1;
        langSelector.dispatchEvent(new Event('change'));

        await slightDelay();

        checkActionRequestExecuted("IdentityAction", langSelectorLanguages[1], "edit",
            "ExprChoiceNode()", "", []);
    });
});

describe("selecting an option from the expr dropdown behaves correctly", () => {
    beforeEach(() => {
        const startNodeButton = document.getElementById('start-node-button') as HTMLButtonElement;
        startNodeButton.click();
    })

    test("select expr dropdown is available", async () => {
        await slightDelay();

        const exprDropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        expect(exprDropdown).toBeTruthy();
    })

    test("selecting an option makes a request to the server", async () => {
        await slightDelay();

        const exprDropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        exprDropdown.selectedIndex = 1;
        exprDropdown.dispatchEvent(new Event('change'));

        checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
            "ExprChoiceNode()", "", ["Num"]);
    });

    test("selecting the first option correctly requests a Num node", async () => {
        expect.assertions(1);

        await slightDelay();

        const exprDropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        exprDropdown.selectedIndex = 1;
        exprDropdown.dispatchEvent(new Event('change'));

        checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
            "ExprChoiceNode()", "", ["Num"]);
    });
});

function removeWhitespace(str: string): string {
    return str.replace(/\s/g, '');
}
