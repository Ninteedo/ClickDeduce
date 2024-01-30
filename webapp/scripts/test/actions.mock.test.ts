import {afterAll, beforeAll, beforeEach, describe, expect, jest, test} from "@jest/globals";
import {initialise} from "../initialise";
import {contextMenuSelect, getLeftmostExprDropdown, selectExprOption, slightDelay} from "./helper";
import {
    checkActionRequestExecuted,
    defaultHtml,
    getRequestsReceived,
    langSelectorLanguages,
    mockEvent,
    prepareExampleTimesTree,
    resetRequestTracking,
    setActionFetchResponse,
    setActionFetchResponseData,
    setUpFetchMock
} from "./requestMocking";
import {handleSubmit} from "../actions";
import * as NS from "../../test_resources/node_strings";
import {numNodeArithHTML, plusNodeArithHTML} from "./serverMock.test";

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

    // test("the contents of the tree div are replaced with the new tree HTML", async () => {
    //     const startNodeButton = document.getElementById('start-node-button') as HTMLButtonElement;
    //     startNodeButton.click();
    //
    //     await slightDelay();
    //
    //     const tree = document.getElementById('tree');
    //     expect(removeWhitespace(tree.innerHTML)).toEqual(removeWhitespace(startNodeBlankArithHTML));
    // });

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

        const exprDropdown = getLeftmostExprDropdown();
        expect(exprDropdown).toBeTruthy();
    });

    function selectOptionResultsInCorrectRequest(exprName: string) {
        test("selecting the " + exprName + " option makes the correct request to the server", async () => {
            await selectExprOption(getLeftmostExprDropdown(), exprName);
            checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
                "ExprChoiceNode()", "", [exprName]);
        });
    }

    selectOptionResultsInCorrectRequest("Num");
    selectOptionResultsInCorrectRequest("Plus");
    selectOptionResultsInCorrectRequest("Times");
});

describe("selecting an option from a non-root expr dropdown behaves correctly", () => {
    const dummyNodeString: string = NS.PLUS_EMPTY;

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');

        setActionFetchResponse(dummyNodeString, plusNodeArithHTML);

        await selectExprOption(getLeftmostExprDropdown(), "Plus");
    });

    test("left and right dropdowns are available", async () => {
        expect.assertions(7);

        const dropdowns = document.querySelectorAll('.expr-selector-dropdown:not([readonly])');
        expect(dropdowns).toHaveLength(2);

        dropdowns.forEach(dropdown => {
            expect(dropdown).toBeTruthy();
            expect(dropdown).toBeInstanceOf(HTMLDivElement);

            if (dropdown instanceof HTMLDivElement) {
                expect(dropdown.querySelectorAll('ul > li')).toHaveLength(3);
            }
        });
    });

    function selectOptionResultsInCorrectRequest(exprName: string) {
        test("selecting the left " + exprName + " option makes the correct request to the server", async () => {
            expect.assertions(1);

            const leftDropdown = document.querySelectorAll('.expr-selector-container:not([readonly])').item(0) as HTMLDivElement;
            console.log(exprName)
            await selectExprOption(leftDropdown, exprName);

            checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
                dummyNodeString, "0", [exprName]);
        });

        test("selecting the right " + exprName + " option makes the correct request to the server", async () => {
            expect.assertions(1);

            const rightSelector = document.querySelectorAll('.expr-selector-container:not([readonly])').item(1) as HTMLDivElement;
            await selectExprOption(rightSelector, exprName);

            checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "edit",
                dummyNodeString, "1", [exprName]);
        });
    }

    selectOptionResultsInCorrectRequest("Num");
    selectOptionResultsInCorrectRequest("Plus");
    selectOptionResultsInCorrectRequest("Times");
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
                `<input type="text" class="literal" style="width: 2ch;" data-tree-path="0" value=""></div>`,
                `<input type="text" class="literal" style="width: 2ch;" data-tree-path="0" value="${foo}"></div>`
            )
    };

    beforeEach(async () => {
        await handleSubmit(mockEvent, '/start-node-blank');

        setActionFetchResponse(dummyNodeString, numNodeArithHTML);

        await selectExprOption(getLeftmostExprDropdown(), "Num");
    });

    test("input is available", async () => {
        expect.assertions(1);

        const input = document.querySelector('input[type="text"]');
        expect(input).toBeInstanceOf(HTMLInputElement);
    });

    test("entering text makes the correct request to the server", async () => {
        expect.assertions(1);

        const input = document.querySelector('input.literal[type="text"]') as HTMLInputElement;
        input.value = "foo";
        input.dispatchEvent(new Event('change'));

        checkActionRequestExecuted("EditLiteralAction", langSelectorLanguages[0], "edit",
            dummyNodeString, "0", ["foo"]);
    });

    test("entering text multiple times makes the correct requests to the server", async () => {
        expect.assertions(3);

        const bar = "bar";

        setActionFetchResponseData(fooActionFetchResponse);

        let input = document.querySelector('input.literal[type="text"]') as HTMLInputElement;
        input.value = foo;
        input.dispatchEvent(new Event('change'));

        checkActionRequestExecuted("EditLiteralAction", langSelectorLanguages[0], "edit",
            dummyNodeString, "0", [foo]);

        await slightDelay();

        input = document.querySelector('input.literal[type="text"]') as HTMLInputElement;

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

        const input = document.querySelector('input.literal[type="text"]') as HTMLInputElement;
        input.value = foo;
        input.dispatchEvent(new Event('change'));

        await slightDelay();

        const initialRequestsReceived = getRequestsReceived().length;

        input.dispatchEvent(new Event('change'));

        await slightDelay();

        expect(getRequestsReceived()).toHaveLength(initialRequestsReceived);

        input.value = "bar";
        input.dispatchEvent(new Event('input'));
        input.value = foo;

        input.dispatchEvent(new Event('change'));

        expect(getRequestsReceived()).toHaveLength(initialRequestsReceived);
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
        setActionFetchResponse("ExprChoiceNode()", plusNodeArithHTML);
        document.getElementById('type-check-mode-radio').click();
        await slightDelay();

        document.getElementById('edit-mode-radio').click();

        checkActionRequestExecuted("IdentityAction", langSelectorLanguages[0], "edit",
            "ExprChoiceNode()", "", []);
    });

    test("after selecting a mode, future requests are made with that mode", async () => {
        document.getElementById('type-check-mode-radio').click();

        await selectExprOption(getLeftmostExprDropdown(), "Num");

        checkActionRequestExecuted("SelectExprAction", langSelectorLanguages[0], "type-check",
            "ExprChoiceNode()", "", ["Num"]);
    });
});
