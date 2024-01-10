import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {MockResponse} from "./MockResponse";
import {handleDropdownChange, handleLiteralChanged, handleSubmit, initialise, undo} from "./script";

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

const startNodeBlankArithHTML = `<div class="subtree axiom" data-tree-path="" data-term="BlankExprDropDown()" data-node-string="ExprChoiceNode()"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="1" data-tree-path="" style="display: inline;"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="annotation-axiom">ExprChoice</div></div>`

const plusNodeArithHTML = `<div class="subtree" data-tree-path="" data-term="Plus(BlankExprDropDown(),BlankExprDropDown())" data-node-string="VariableNode(&quot;Plus&quot;, List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))"><div class="node"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><div class="expr"><div>(<select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="7" data-tree-path="0" readonly="readonly" disabled="disabled"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select> + <select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="8" data-tree-path="1" readonly="readonly" disabled="disabled"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select>)</div></div><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="args"><div class="subtree axiom" data-tree-path="0" data-term="BlankExprDropDown()" data-node-string="ExprChoiceNode()"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="9" data-tree-path="0" style="display: inline;"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="annotation-axiom">ExprChoice</div></div><div class="subtree axiom" data-tree-path="1" data-term="BlankExprDropDown()" data-node-string="ExprChoiceNode()"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="10" data-tree-path="1" style="display: inline;"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="annotation-axiom">ExprChoice</div></div><div class="annotation-new">Plus</div></div></div>`

const numNodeArithHTML = `<div class="subtree axiom" data-tree-path="" data-term="Num()" data-node-string="VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;&quot;)))"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><div style="display: inline;"><input type="text" style="width: 2ch;" data-tree-path="0" value=""></div><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">Num can only accept LiteralInt, not </div></span></div></div><div class="annotation-axiom">Num</div></div>`

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

const mockEvent = { preventDefault: jest.fn() } as unknown as Event;

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

describe("selecting an option from the root expr dropdown behaves correctly", () => {
    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');
    });

    test("select expr dropdown is available", async () => {
        expect.assertions(1);

        const exprDropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        expect(exprDropdown).toBeTruthy();
    });

    function selectOptionResultsInCorrectRequest(index: number, exprName: string) {
        test("selecting the " + exprName + " option makes the correct request to the server", async () => {
            expect.assertions(1);

            const exprDropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
            exprDropdown.selectedIndex = index;
            exprDropdown.dispatchEvent(new Event('change'));

            checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
                "ExprChoiceNode()", "", [exprName]);
        });
    }

    selectOptionResultsInCorrectRequest(1, "Num");
    selectOptionResultsInCorrectRequest(2, "Plus");
    selectOptionResultsInCorrectRequest(3, "Times");
});

describe("selecting an option from a non-root expr dropdown behaves correctly", () => {
    const dummyNodeString: string = 'VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))';

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');

        actionFetchResponse = {nodeString: dummyNodeString, html: plusNodeArithHTML};

        const exprDropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        exprDropdown.selectedIndex = 2;

        await handleDropdownChange(exprDropdown, 'expr');
    });

    test("left and right dropdowns are available", async () => {
        expect.assertions(9);

        const dropdowns = document.querySelectorAll('.expr-dropdown:not([readonly])');
        expect(dropdowns).toHaveLength(2);

        dropdowns.forEach(dropdown => {
            expect(dropdown).toBeTruthy();
            expect(dropdown).toBeInstanceOf(HTMLSelectElement);

            if (dropdown instanceof HTMLSelectElement) {
                expect(dropdown.selectedIndex).toEqual(0);
                expect(dropdown.children).toHaveLength(4);
            }
        });
    });

    function selectOptionResultsInCorrectRequest(index: number, exprName: string) {
        test("selecting the left " + exprName + " option makes the correct request to the server", async () => {
            expect.assertions(1);

            const leftDropdown = document.querySelectorAll('.expr-dropdown:not([readonly])').item(0) as HTMLSelectElement;
            leftDropdown.selectedIndex = index;
            leftDropdown.dispatchEvent(new Event('change'));

            checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
                dummyNodeString, "0", [exprName]);
        });

        test("selecting the right " + exprName + " option makes the correct request to the server", async () => {
            expect.assertions(1);

            const rightDropdown = document.querySelectorAll('.expr-dropdown:not([readonly])').item(1) as HTMLSelectElement;
            rightDropdown.selectedIndex = index;
            rightDropdown.dispatchEvent(new Event('change'));

            checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
                dummyNodeString, "1", [exprName]);
        });
    }

    selectOptionResultsInCorrectRequest(1, "Num");
    selectOptionResultsInCorrectRequest(2, "Plus");
    selectOptionResultsInCorrectRequest(3, "Times");
});

describe("entering text into a literal input behaves correctly", () => {
    const dummyNodeString: string = 'VariableNode("Num", List(LiteralNode("")))';

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');

        actionFetchResponse = {nodeString: dummyNodeString, html: numNodeArithHTML};

        const exprDropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        exprDropdown.selectedIndex = 1;

        await handleDropdownChange(exprDropdown, 'expr');
    });

    test("input is available", async () => {
        expect.assertions(1);

        const input = document.querySelector('input[type="text"]');
        expect(input).toBeInstanceOf(HTMLInputElement);
    });

    test("entering text makes the correct request to the server", async () => {
        expect.assertions(1);

        const input = document.querySelector('input[type="text"]') as HTMLInputElement;
        input.value = "foo";
        input.dispatchEvent(new Event('change'));

        checkActionRequestExecuted("EditLiteralAction", langSelectorLanguages[0], "edit",
            dummyNodeString, "0", ["foo"]);
    });

    test("entering text multiple times makes the correct requests to the server", async () => {
        expect.assertions(3);

        const foo = "foo";
        const bar = "bar";

        const newNodeString = `VariableNode(\"Num\", List(LiteralNode(\"${foo}\")))`;

        actionFetchResponse = {
            nodeString: newNodeString,
            html: numNodeArithHTML.replace(`LiteralNode(&quot;&quot;)`, `LiteralNode(&quot;${foo}&quot;)`)
                .replace(`<input type="text" style="width: 2ch;" data-tree-path="0" value=""></div>`, `<input type="text" style="width: 2ch;" data-tree-path="0" value="${foo}"></div>`)
        };

        let input = document.querySelector('input[type="text"]') as HTMLInputElement;
        input.value = foo;
        input.dispatchEvent(new Event('change'));

        checkActionRequestExecuted("EditLiteralAction", langSelectorLanguages[0], "edit",
            dummyNodeString, "0", [foo]);

        await slightDelay();

        input = document.querySelector('input[type="text"]') as HTMLInputElement;

        expect(input.value).toEqual(foo);

        input.value = bar;
        input.dispatchEvent(new Event('change'));

        checkActionRequestExecuted("EditLiteralAction", langSelectorLanguages[0], "edit",
            newNodeString, "0", [bar]);
    });
});

describe("undo and redo buttons behave correctly", () => {
    const nodeString1 = `ExprChoiceNode()`;
    const nodeString2 = `VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))`;
    const nodeString3 = `VariableNode("Num", List(LiteralNode("")))`;
    const nodeString4 = `VariableNode("Num", List(LiteralNode("foo")))`;

    const html1 = startNodeBlankArithHTML;
    const html2 = plusNodeArithHTML;
    const html3 = numNodeArithHTML;
    const html4 = numNodeArithHTML.replace(`LiteralNode(&quot;&quot;)`, `LiteralNode(&quot;foo&quot;)`)
        .replace(`<input type="text" style="width: 2ch;" data-tree-path="0" value=""></div>`, `<input type="text" style="width: 2ch;" data-tree-path="0" value="foo"></div>`);

    function getUndoButton(): HTMLButtonElement {
        return document.getElementById('undoButton') as HTMLButtonElement;
    }

    function getRedoButton(): HTMLButtonElement {
        return document.getElementById('redoButton') as HTMLButtonElement;
    }

    async function updateTree(nodeString: string, html: string): Promise<void> {
        actionFetchResponse = {nodeString, html};
        const dropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        if (dropdown) {
            await handleDropdownChange(dropdown, 'expr');
        } else {
            const literalInput = document.querySelector('input[type="text"]') as HTMLInputElement;
            literalInput.value = literalInput.value + " ";
            await handleLiteralChanged(literalInput);
        }
    }

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');
    });

    test("undo and redo buttons begin disabled", () => {
        expect(getUndoButton().getAttributeNames()).toContain('disabled');
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("undo button is enabled after an action", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        expect(getUndoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("redo button is still disabled after an action", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("undo button is disabled when there are no more history", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        expect(getUndoButton().getAttributeNames()).toContain('disabled');
    });

    test("redo button is enabled after an undo", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        expect(getRedoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("pressing undo reverts the tree to the previous state", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(html1);
    });

    test("pressing undo twice reverts the tree to the state before the previous state", async () => {
        expect.assertions(2);
        await updateTree(nodeString2, html2);
        await updateTree(nodeString3, html3);

        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(html2);

        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(html1);
    });

    test("pressing undo and then redo reverts the tree to the most recent state", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(html2);
    });

    test("pressing undo and then redo twice reverts the tree to the state before the most recent state", async () => {
        expect.assertions(2);
        await updateTree(nodeString2, html2);
        await updateTree(nodeString3, html3);

        getUndoButton().click();
        getUndoButton().click();
        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(html2);

        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(html3);
    });

    test("pressing undo then performing an action disables redo", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        await updateTree(nodeString3, html3);
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("pressing undo then performing an action then undoing again enables redo", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        await updateTree(nodeString3, html3);
        getUndoButton().click();
        expect(getRedoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("pressing undo then performing an action then undoing again can be redone correctly", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        await updateTree(nodeString3, html3);
        getUndoButton().click();
        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(html3);
    });
});

describe("context menu behaves correctly", () => {
    function contextMenuSelect(element: HTMLElement): void {
        element.dispatchEvent(new MouseEvent('mouseover', {
            bubbles: true,
            cancelable: true,
        }));

        element.dispatchEvent(new MouseEvent('contextmenu', {
            bubbles: true,
            cancelable: true,
            clientX: 0,
            clientY: 0,
            button: 2
        }));
    }

    function leftClickOn(element: HTMLElement): void {
        element.dispatchEvent(new MouseEvent('mouseover', {
            bubbles: true,
            cancelable: true,
        }));

        element.dispatchEvent(new MouseEvent('click', {
            bubbles: true,
            cancelable: true,
            clientX: 0,
            clientY: 0,
            button: 0
        }));
    }

    const nodeString2 = `VariableNode("Plus", List(SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))`;
    const html2 = plusNodeArithHTML;

    const nodeString3 = `VariableNode("Plus", List(SubExprNode(VariableNode("Num", List(LiteralNode("")))), SubExprNode(ExprChoiceNode())))`;
    const html3 = `<div class="subtree" data-tree-path="" data-term="Times(Num(),BlankExprDropDown())" data-node-string="VariableNode(&quot;Times&quot;, List(SubExprNode(VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;&quot;)))), SubExprNode(ExprChoiceNode())))"><div class="node"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><div class="expr"><div>(<div style="display: inline;"><input type="text" readonly="true" disabled="true" style="width: 1ch;" value=""></div> × <select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="1" data-tree-path="1" readonly="readonly" disabled="disabled"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select>)</div></div><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">Num can only accept LiteralInt, not </div></span></div></div><div class="args"><div class="subtree axiom" data-tree-path="0" data-term="Num()" data-node-string="VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;&quot;)))"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><div style="display: inline;"><input type="text" style="width: 2ch;" data-tree-path="0-0" value=""></div><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">Num can only accept LiteralInt, not </div></span></div></div><div class="annotation-axiom">Num</div></div><div class="subtree axiom" data-tree-path="1" data-term="BlankExprDropDown()" data-node-string="ExprChoiceNode()"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="2" data-tree-path="1" style="display: inline;"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="annotation-axiom">ExprChoice</div></div><div class="annotation-new">Times</div></div></div>`

    const nodeString4 = `VariableNode("Plus", List(SubExprNode(VariableNode("Num", List(LiteralNode("56")))), SubExprNode(ExprChoiceNode())))`;
    const html4 = `<div class="subtree" data-tree-path="" data-term="Times(Num(4),BlankExprDropDown())" data-node-string="VariableNode(&quot;Times&quot;, List(SubExprNode(VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;4&quot;)))), SubExprNode(ExprChoiceNode())))"><div class="node"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><div class="expr"><div>(<div style="display: inline;"><input type="text" readonly="true" disabled="true" style="width: 1ch;" value="4"></div> × <select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="3" data-tree-path="1" readonly="readonly" disabled="disabled"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select>)</div></div><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="args"><div class="subtree axiom" data-tree-path="0" data-term="Num(4)" data-node-string="VariableNode(&quot;Num&quot;, List(LiteralNode(&quot;4&quot;)))"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><div style="display: inline;"><input type="text" style="width: 2ch;" data-tree-path="0-0" value="4"></div><span style="padding-left: 1ch; padding-right: 1ch;">⇓</span><div class="eval-result" style="display: inline;"><span class="tooltip"><div style="display: inline;">4: Int</div><div class="tooltiptext">NumV(4): IntType()</div></span></div></div><div class="annotation-axiom">Num</div></div><div class="subtree axiom" data-tree-path="1" data-term="BlankExprDropDown()" data-node-string="ExprChoiceNode()"><div class="expr"><div class="scoped-variables" style="display: inline; padding-right: 0ch;"></div><select class="expr-dropdown" onchange="handleDropdownChange(this, &quot;expr&quot;)" name="4" data-tree-path="1" style="display: inline;"><option value="">Select Expr...</option><option value="Num">Num</option><option value="Plus">Plus</option><option value="Times">Times</option></select><span style="padding-left: 0.5ch; padding-right: 0.5ch;">:</span><div class="type-check-result" style="display: inline;"><span class="tooltip error-origin"><div style="display: inline;">?</div><div class="tooltiptext">BlankExprDropDown()</div></span></div></div><div class="annotation-axiom">ExprChoice</div></div><div class="annotation-new">Times</div></div></div>`

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');
        actionFetchResponse = {nodeString: nodeString2, html: html2};
        await handleDropdownChange(document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement, 'expr');
        actionFetchResponse = {nodeString: nodeString3, html: html3};
        await handleDropdownChange(document.querySelectorAll('.expr-dropdown:not([readonly])')[0] as HTMLSelectElement, 'expr');
        actionFetchResponse = {nodeString: nodeString4, html: html4};
        await handleLiteralChanged(document.querySelector('input[type="text"]') as HTMLInputElement);
    });

    test("context menu is initially hidden", async () => {
        expect(document.getElementById('custom-context-menu').style.display).toEqual('none');
    });

    test("right-clicking an element causes the context menu to appear", async () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        expect(document.getElementById('custom-context-menu').style.display).toEqual('block');
    });

    test("the selected element remains highlighted after the context menu appears", async () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        expect(element.classList).toContain('highlight');
    });

    test("the context menu disappears when clicking away", async () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        leftClickOn(document.querySelector('[data-tree-path=""]'))
        expect(document.getElementById('custom-context-menu').style.display).toEqual('none');
    });

    test("right-clicking another element when the context menu is out causes the context menu to disappear", async () => {
        const element1 = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element1);

        const element2 = document.querySelector('[data-tree-path="1"]') as HTMLElement;
        contextMenuSelect(element2);

        expect(document.getElementById('custom-context-menu').style.display).toEqual('none');
    });

    test("right-clicking the context menu causes the context menu to disappear", async () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        contextMenuSelect(document.getElementById('custom-context-menu'));
        expect(document.getElementById('custom-context-menu').style.display).toEqual('none');
    });

    test("right-clicking on the selected element again causes the context menu to disappear", async () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        contextMenuSelect(element);
        expect(document.getElementById('custom-context-menu').style.display).toEqual('none');
    });
});

function removeWhitespace(str: string): string {
    return str.replace(/\s/g, '');
}
