import {afterAll, beforeAll, beforeEach, describe, expect, jest, test} from "@jest/globals";
import {initialise} from "../initialise";
import * as NS from "../../test_resources/node_strings";
import {ClickDeduceResponseError} from "../ClickDeduceResponseError";
import {
    contextMenuSelect,
    getErrorDiv,
    getRedoButton,
    getTabbableElements,
    getUndoButton,
    leftClickOn,
    loadHtmlTemplate,
    removeWhitespace,
    slightDelay
} from "./helper";
import {handleDropdownChange, handleLiteralChanged, handleSubmit} from "../actions";
import {
    checkActionRequestExecuted,
    getRequestsReceived,
    langSelectorHtml,
    langSelectorLanguages,
    mockEvent,
    optionsHtml,
    resetRequestTracking,
    setActionErrorMessage,
    setActionFetchResponse,
    setActionFetchResponseData,
    setDummyFetchResponse,
    setUpFetchMock,
    startNodeBlankArithHTML
} from "./request_mocking";

const defaultHtml = loadHtmlTemplate('../pages/index')

const plusNodeArithHTML = loadHtmlTemplate('plus_node_arith');
const numNodeArithHTML = loadHtmlTemplate('num_node_arith');


async function prepareExampleTimesTree(): Promise<void> {
    const nodeString2 = NS.TIMES_EMPTY;
    const html2 = plusNodeArithHTML;

    const nodeString3 = NS.TIMES_LEFT_NUM_RIGHT_EMPTY;
    const html3 = loadHtmlTemplate('times_left_num_right_empty');

    const nodeString4 = NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY;
    const html4 = loadHtmlTemplate('times_left_filled_num_right_empty');

    await handleSubmit(mockEvent, '/start-node-blank');
    setActionFetchResponse(nodeString2, html2);
    await handleDropdownChange(document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement, 'expr');
    setActionFetchResponse(nodeString3, html3);
    await handleDropdownChange(document.querySelectorAll('.expr-dropdown:not([readonly])')[0] as HTMLSelectElement, 'expr');
    setActionFetchResponse(nodeString4, html4);
    await handleLiteralChanged(document.querySelector('input[type="text"]') as HTMLInputElement);
}

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
})

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
        expect(getRequestsReceived()).toContainEqual({url: 'start-node-blank', request: correctRequest});
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

        expect(getRequestsReceived()).toContainEqual({url: 'start-node-blank', request: correctRequest});
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
    const dummyNodeString: string = NS.PLUS_EMPTY;

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');

        setActionFetchResponse(dummyNodeString, plusNodeArithHTML);

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
    const foo = "foo";

    const fooActionFetchResponse = {
        nodeString: `VariableNode(\"Num\", List(LiteralNode(\"${foo}\")))`,
        html: numNodeArithHTML.replace(
            `LiteralNode(&quot;&quot;)`,
            `LiteralNode(&quot;${foo}&quot;)`
        )
            .replace(
                `<input type="text" style="width: 2ch;" data-tree-path="0" value=""></div>`,
                `<input type="text" style="width: 2ch;" data-tree-path="0" value="${foo}"></div>`
            )
    };

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');

        setActionFetchResponse(dummyNodeString, numNodeArithHTML);

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

        const bar = "bar";

        setActionFetchResponseData(fooActionFetchResponse);

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
            fooActionFetchResponse.nodeString, "0", [bar]);
    });

    test("if the input text is the same as it was before (blank), no server request is made", async () => {
        expect.assertions(2);

        const input = document.querySelector('input[type="text"]') as HTMLInputElement;
        input.value = "";
        input.dispatchEvent(new Event('change'));

        const initialRequestsReceived = getRequestsReceived().length;

        input.dispatchEvent(new Event('change'));

        expect(getRequestsReceived()).toHaveLength(initialRequestsReceived);

        input.value = "foo";
        input.dispatchEvent(new Event('input'));
        input.value = "";

        input.dispatchEvent(new Event('change'));

        expect(getRequestsReceived()).toHaveLength(initialRequestsReceived);
    });

    test("if the input text is the same as it was before (not blank), no server request is made", async () => {
        expect.assertions(2);

        setActionFetchResponseData(fooActionFetchResponse);

        const input = document.querySelector('input[type="text"]') as HTMLInputElement;
        input.value = foo;
        input.dispatchEvent(new Event('change'));

        await slightDelay();

        const initialRequestsReceived = getRequestsReceived().length;

        input.dispatchEvent(new Event('change'));

        expect(getRequestsReceived()).toHaveLength(initialRequestsReceived);

        input.value = "bar";
        input.dispatchEvent(new Event('input'));
        input.value = foo;

        input.dispatchEvent(new Event('change'));

        expect(getRequestsReceived()).toHaveLength(initialRequestsReceived);
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
    const html4 = numNodeArithHTML.replace(
        `LiteralNode(&quot;&quot;)`,
        `LiteralNode(&quot;foo&quot;)`
    ).replace(
        `<input type="text" style="width: 2ch;" data-tree-path="0" value=""></div>`,
        `<input type="text" style="width: 2ch;" data-tree-path="0" value="foo"></div>`
    );

    async function updateTree(nodeString: string, html: string): Promise<void> {
        setActionFetchResponse(nodeString, html);
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

describe("hovering over a node behaves correctly", () => {
    const nodeString = NS.PLUS_LEFT_NUM_RIGHT_EMPTY;
    const html = loadHtmlTemplate('plus_left_num_right_empty');

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');
        setActionFetchResponse(nodeString, html);
        await handleDropdownChange(document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement, 'expr');
    });

    test("mousing over a node highlights it", async () => {
        expect.assertions(1);
        const node = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        node.dispatchEvent(new MouseEvent('mouseover', {
            bubbles: true,
            cancelable: true,
        }));
        expect(node.classList).toContain('highlight');
    });

    test("mousing out from a highlighted node unhighlights it", async () => {
        expect.assertions(1);
        const node = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        node.dispatchEvent(new MouseEvent('mouseover', {
            bubbles: true,
            cancelable: true,
        }));
        node.dispatchEvent(new MouseEvent('mouseout', {
            bubbles: true,
            cancelable: true,
        }));
        expect(node.classList).not.toContain('highlight');
    });

    test("mousing out from a highlighted node while it is focused by the context menu does not unhighlight it", async () => {
        expect.assertions(1);
        const node = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        node.dispatchEvent(new MouseEvent('mouseover', {
            bubbles: true,
            cancelable: true,
        }));
        contextMenuSelect(node);
        node.dispatchEvent(new MouseEvent('mouseout', {
            bubbles: true,
            cancelable: true,
        }));
        expect(node.classList).toContain('highlight');
    });
});

describe("context menu behaves correctly", () => {
    beforeEach(async () => {
        await prepareExampleTimesTree();
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

describe("delete, copy, and paste buttons behave correctly", () => {
    beforeEach(async () => {
        await prepareExampleTimesTree();
    });

    test("pressing delete makes the correct request to the server", async () => {
        expect.assertions(1);

        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        const deleteButton = document.getElementById('delete-button');
        deleteButton.click();

        checkActionRequestExecuted("DeleteAction", langSelectorLanguages[0], "edit",
            NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY, "0", []);
    });

    test("pressing copy does not make a request to the server", async () => {
        expect.assertions(1);

        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        const initialRequestsReceived = getRequestsReceived().length;

        const copyButton = document.getElementById('copy-button');
        copyButton.click();

        expect(getRequestsReceived().length).toEqual(initialRequestsReceived);
    });

    test("clicking paste has no effect before copying something", async () => {
        expect.assertions(1);

        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        const initialRequestsReceived = getRequestsReceived().length;

        const pasteButton = document.getElementById('paste-button');
        pasteButton.click();

        expect(getRequestsReceived().length).toEqual(initialRequestsReceived);
    });

    test("clicking paste on same element after copying it makes the correct request to the server", async () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        const copyButton = document.getElementById('copy-button');
        copyButton.click();

        contextMenuSelect(element);

        const pasteButton = document.getElementById('paste-button');
        pasteButton.click();

        checkActionRequestExecuted("PasteAction", langSelectorLanguages[0], "edit",
            NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY, "0", ["VariableNode(\"Num\", List(LiteralNode(\"4\")))"]);
    });

    test("clicking paste on another element after copying one makes the correct request to the server", async () => {
        const element1 = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element1);

        const copyButton = document.getElementById('copy-button');
        copyButton.click();

        const element2 = document.querySelector('.subtree[data-tree-path="1"]') as HTMLElement;
        contextMenuSelect(element2);

        const pasteButton = document.getElementById('paste-button');
        pasteButton.click();

        checkActionRequestExecuted("PasteAction", langSelectorLanguages[0], "edit",
            NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY, "1", ["VariableNode(\"Num\", List(LiteralNode(\"4\")))"]);
    });

    test("clicking paste after changing tree state makes the correct request to the server", async () => {
        contextMenuSelect(document.querySelector('[data-tree-path="0"]'));
        document.getElementById('copy-button').click();

        document.getElementById('undoButton').click();

        contextMenuSelect(document.querySelector('[data-tree-path=""]'));
        document.getElementById('paste-button').click();

        checkActionRequestExecuted("PasteAction", langSelectorLanguages[0], "edit",
            NS.TIMES_LEFT_NUM_RIGHT_EMPTY, "", ["VariableNode(\"Num\", List(LiteralNode(\"4\")))"]);
    });
});

describe("mode radio buttons behave correctly", () => {
    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');
    });

    test("the initial mode is edit", async () => {
        expect(document.getElementById('edit-mode-radio').getAttributeNames()).toContain('checked');
    });

    test("clicking the type-check mode button makes the correct request to the server", async () => {
        document.getElementById('type-check-mode-radio').click();

        checkActionRequestExecuted("IdentityAction", langSelectorLanguages[0], "type-check",
            "ExprChoiceNode()", "", []);
    });

    test("clicking the eval mode button makes the correct request to the server", async () => {
        document.getElementById('eval-mode-radio').click();

        checkActionRequestExecuted("IdentityAction", langSelectorLanguages[0], "eval",
            "ExprChoiceNode()", "", []);
    });

    test("clicking the edit mode button makes the correct request to the server", async () => {
        document.getElementById('type-check-mode-radio').click();
        document.getElementById('edit-mode-radio').click();

        checkActionRequestExecuted("IdentityAction", langSelectorLanguages[0], "edit",
            "ExprChoiceNode()", "", []);
    });

    test("after selecting a mode, future requests are made with that mode", async () => {
        document.getElementById('type-check-mode-radio').click();

        const dropdown = document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement;
        dropdown.selectedIndex = 1;
        await handleDropdownChange(dropdown, 'expr');

        checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "type-check",
            "ExprChoiceNode()", "", ["Num"]);
    });
});

describe("tab cycling between input elements behaves correctly", () => {
    const nodeString = NS.TABBING_EXAMPLE;
    const html = loadHtmlTemplate('tabbing_example');

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');
        setActionFetchResponse(nodeString, html);
        await handleDropdownChange(document.getElementsByClassName('expr-dropdown')[0] as HTMLSelectElement, 'expr');
    });

    test("test can find a list of tabbable elements", async () => {
        const tabbableElements = getTabbableElements();
        expect(tabbableElements).toHaveLength(5);

        const paths = ["0-0-0", "0-1-0", "1-0-0-0", "1-0-1-0", "1-1-0"];

        tabbableElements.forEach((element, index) => {
            expect(element.getAttribute("data-tree-path")).toEqual(paths[index]);
            expect(element).toBeInstanceOf(HTMLInputElement);
            expect(element.attributes).not.toContain('disabled');
        });
    });

    test("tabbing through the elements in order works", async () => {
        const tabbableElements = getTabbableElements();

        tabbableElements[0].focus();
        expect(document.activeElement).toEqual(tabbableElements[0]);

        tabbableElements.forEach((element, index) => {
            element.dispatchEvent(new KeyboardEvent('keydown', {
                key: 'Tab'
            }));
            expect(document.activeElement).toEqual(tabbableElements[(index + 1) % tabbableElements.length]);
        });
    });

    test("tabbing through the elements in reverse order works", async () => {
        const tabbableElements = getTabbableElements();

        tabbableElements[0].focus();
        expect(document.activeElement).toEqual(tabbableElements[0]);

        tabbableElements.forEach((element, index) => {
            element.dispatchEvent(new KeyboardEvent('keydown', {
                key: 'Tab',
                shiftKey: true
            }));
            expect(document.activeElement).toEqual(tabbableElements[(index - 1 + tabbableElements.length) % tabbableElements.length]);
        });
    });
});

describe("tab cycling between input and select elements behaves correctly", () => {
    beforeEach(async () => {
        await prepareExampleTimesTree();
    });

    test("test can find a list of tabbable elements", async () => {
        const tabbableElements = getTabbableElements(true);
        expect(tabbableElements).toHaveLength(2);

        expect(tabbableElements[0]).toBeInstanceOf(HTMLInputElement);
        expect(tabbableElements[1]).toBeInstanceOf(HTMLSelectElement);

        const paths = ["0-0", "1"];

        tabbableElements.forEach((element, index) => {
            expect(element.getAttribute("data-tree-path")).toEqual(paths[index]);
            expect(element.attributes).not.toContain('disabled');
        });
    });

    test("tabbing through the elements in order works", async () => {
        const tabbableElements = getTabbableElements(true);

        tabbableElements[0].focus();
        expect(document.activeElement).toEqual(tabbableElements[0]);

        tabbableElements.forEach((element, index) => {
            element.dispatchEvent(new KeyboardEvent('keydown', {
                key: 'Tab'
            }));
            expect(document.activeElement).toEqual(tabbableElements[(index + 1) % tabbableElements.length]);
        });
    });

    test("tabbing through the elements in reverse order works", async () => {
        const tabbableElements = getTabbableElements(true);

        tabbableElements[0].focus();
        expect(document.activeElement).toEqual(tabbableElements[0]);

        tabbableElements.forEach((element, index) => {
            element.dispatchEvent(new KeyboardEvent('keydown', {
                key: 'Tab',
                shiftKey: true
            }));
            expect(document.activeElement).toEqual(tabbableElements[(index - 1 + tabbableElements.length) % tabbableElements.length]);
        });
    });
});

describe("input focus is preserved when the tree is updated", () => {
    beforeEach(async () => {
        await prepareExampleTimesTree();
    });

    test("input focus is preserved when a literal is edited and ENTER is pressed", async () => {
        const input = document.querySelector('input[data-tree-path="0-0"]') as HTMLInputElement;
        input.focus();
        input.value = "8";
        setActionFetchResponse(
            NS.TIMES_LEFT_NUM_RIGHT_EMPTY.replace(`LiteralNode("4")`, `LiteralNode("8")`),
            loadHtmlTemplate('times_left_filled_num_right_empty_alt')
        );
        expect(input.value).toEqual("8");
        input.dispatchEvent(new KeyboardEvent('keydown', {
            key: 'Enter'
        }));
        await slightDelay();
        const newInput = document.querySelector('input[data-tree-path="0-0"]') as HTMLInputElement;
        expect(newInput.value).toEqual("8");
        expect(document.activeElement).toEqual(newInput);
        expect(newInput).not.toEqual(input);
    });

    test("input focus is not preserved when a literal is edited and then something else is clicked", async () => {
        const input = document.querySelector('input[data-tree-path="0-0"]') as HTMLInputElement;
        input.focus();
        setActionFetchResponse(
            NS.TIMES_LEFT_NUM_RIGHT_EMPTY.replace(`LiteralNode("4")`, `LiteralNode("8")`),
            loadHtmlTemplate('times_left_filled_num_right_empty_alt')
        );
        input.value = "8";
        input.dispatchEvent(new Event('blur'));

        await slightDelay();
        const newInput = document.querySelector('input[data-tree-path="0-0"]') as HTMLInputElement;
        expect(newInput.value).toEqual("8");
        expect(document.activeElement).not.toEqual(newInput);
        expect(document.activeElement).not.toEqual(input);
        expect(document.activeElement).toEqual(document.body);
    });
});

describe("input focus is preserved when tabbing as the tree is updated", () => {

});

describe("phantom inputs are made read-only and disabled", () => {
    const html = loadHtmlTemplate('phantom_example');

    const selector = `input[name="eg"], select[name="77"]`;

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');

        setActionFetchResponse(NS.PHANTOM_EXAMPLE, html);

        document.getElementById("eval-mode-radio").click();
        await slightDelay();
    });

    test("phantom inputs are made read-only and disabled", async () => {
        const inputs = document.querySelectorAll(selector);
        expect(inputs).toHaveLength(2);
        inputs.forEach(input => {
            expect(input.getAttributeNames()).toContain('readonly');
            expect(input.getAttributeNames()).toContain('disabled');
        });
    });
});

describe("responses to server errors are appropriate", () => {
    async function triggerError(message: string): Promise<void> {
        const input = document.querySelector('input[type="text"]') as HTMLInputElement;
        input.value = input.value + " foo";
        setActionErrorMessage(message);
        await handleLiteralChanged(input);
    }

    beforeEach(async () => {
        await prepareExampleTimesTree();
    });

    test("an error is thrown in the console", async () => {
        const message = "test";
        try {
            await triggerError(message)
        } catch (e) {
            expect(e).toBeInstanceOf(ClickDeduceResponseError);
        }
    });

    test("error div becomes visible", async () => {
        try {
            await triggerError("test");
        } catch (e) {
        }
        expect(getErrorDiv().classList).toContain('fade-in');
        expect(getErrorDiv().classList).not.toContain('fade-out');
    });

    test("error div contains the error message", async () => {
        let message = "test";
        try {
            await triggerError(message);
        } catch (e) {
        }
        expect(getErrorDiv().textContent).toEqual(message);

        message = "Stack overflow exception";
        try {
            await triggerError(message);
        } catch (e) {
        }
        expect(getErrorDiv().textContent).toEqual(message);
    });

    test("error div becomes invisible after a timeout", async () => {
        jest.useFakeTimers();

        try {
            await triggerError("test");
        } catch (e) {
        }

        jest.advanceTimersByTime(10000);

        expect(getErrorDiv().classList).not.toContain('fade-in');
        expect(getErrorDiv().classList).toContain('fade-out');
    });
});
