import {beforeEach, describe, expect, test} from "vitest";
import {initialise} from "../initialise";
import {
    basicMocks,
    contextMenuSelect,
    doLiteralEdit,
    getDropdownAt,
    getLeftmostExprDropdown,
    getLiteralInputAt,
    getUndoButton,
    loadIndexHtmlTemplate,
    pressStartNodeButton,
    selectExprOption
} from "./helper";
import {doStartNodeBlank} from "../actions";
import * as NS from "../../test_resources/node_strings";
import {
    clearActionHistory,
    clearStartNodeBlankHistory,
    getActionHistory,
    getStartNodeBlankHistory
} from "../serverRequest";
import {getCopyButton, getDeleteButton, getPasteButton} from "../interface";
import {getExprSelectors, getLiteralInputs} from "../treeManipulation";

const indexHtml = loadIndexHtmlTemplate();

beforeEach(() => {
    clearActionHistory();
    clearStartNodeBlankHistory();
    document.body.innerHTML = indexHtml;
    initialise(true);
    basicMocks();
});

const langSelectorLanguages = ["LArith", "LIf", "LLet"];

export function checkActionExecuted(
    langName: string,
    modeName: string,
    actionName: string,
    nodeString: string,
    treePath: string,
    extraArgs: any[]
): void {
    const extraArgsStrings: string[] = extraArgs.map(arg => arg.toString());
    const history = getActionHistory();
    expect(history).toContainEqual({
        langName,
        modeName,
        actionName,
        nodeString,
        treePath,
        extraArgsStrings
    });
}

function checkStartNodeBlankExecuted(langName: string): void {
    expect(getStartNodeBlankHistory()).toContainEqual({langName});
}

describe("start new node button behaves correctly", () => {
    test("clicking the button makes a request to the server", () => {
        pressStartNodeButton();
        checkStartNodeBlankExecuted(langSelectorLanguages[0]);
    });

    test("the request made respects the selected language", () => {
        const langSelector = document.getElementById('lang-selector') as HTMLSelectElement;
        langSelector.selectedIndex = 1;
        langSelector.dispatchEvent(new Event('change'));
        pressStartNodeButton();

        checkStartNodeBlankExecuted(langSelectorLanguages[1]);
    });

    test("changing the selected language causes an identity action", () => {
        pressStartNodeButton();

        const langSelector = document.getElementById('lang-selector') as HTMLSelectElement;
        console.log(langSelector.selectedIndex);
        langSelector.selectedIndex = 1;
        langSelector.dispatchEvent(new Event('change'));

        checkActionExecuted(langSelectorLanguages[1], "edit", "IdentityAction",
            "ExprChoiceNode()", "", []);
    });
});

describe("selecting an option from the root expr dropdown behaves correctly", () => {
    beforeEach(() => {
        doStartNodeBlank();
    });

    test("select expr dropdown is available", () => {
        expect.assertions(1);

        const exprDropdown = getLeftmostExprDropdown();
        expect(exprDropdown).toBeTruthy();
    });

    function selectOptionResultsInCorrectRequest(exprName: string) {
        test("selecting the " + exprName + " option makes the correct request to the server", () => {
            selectExprOption(getLeftmostExprDropdown(), exprName);
            checkActionExecuted(langSelectorLanguages[0], "edit", "SelectExprAction",
                "ExprChoiceNode()", "", [exprName]);
        });
    }

    selectOptionResultsInCorrectRequest("Num");
    selectOptionResultsInCorrectRequest("Plus");
    selectOptionResultsInCorrectRequest("Times");
});

describe("selecting an option from a non-root expr dropdown behaves correctly", () => {
    const dummyNodeString: string = NS.PLUS_EMPTY;

    beforeEach(() => {
        doStartNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Plus");
    });

    test("left and right dropdowns are available", () => {
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
        test("selecting the left " + exprName + " option makes the correct request to the server", () => {
            expect.assertions(1);

            const leftDropdown = getLeftmostExprDropdown();
            console.log(exprName)
            selectExprOption(leftDropdown, exprName);

            checkActionExecuted(langSelectorLanguages[0], "edit", "SelectExprAction",
                dummyNodeString, "0", [exprName]);
        });

        test("selecting the right " + exprName + " option makes the correct request to the server", () => {
            expect.assertions(1);

            const rightSelector = getExprSelectors()[1];
            selectExprOption(rightSelector, exprName);

            checkActionExecuted(langSelectorLanguages[0], "edit", "SelectExprAction",
                dummyNodeString, "1", [exprName]);
        });
    }

    selectOptionResultsInCorrectRequest("Num");
    selectOptionResultsInCorrectRequest("Plus");
    selectOptionResultsInCorrectRequest("Times");
});

describe("entering text into a literal input behaves correctly", () => {
    const dummyNodeString: string = 'VariableNode("Num", List(LiteralNode(LiteralInt(0))))';
    const foo = "12345";

    beforeEach(() => {
        doStartNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Num");
    });

    test("input is available", () => {
        expect.assertions(1);
        expect(getLiteralInputs()[0]).toBeTruthy();
    });

    test("entering text makes the correct request to the server", () => {
        expect.assertions(1);

        const input = getLiteralInputs()[0];
        input.setValue("50");
        input.blur();

        checkActionExecuted(langSelectorLanguages[0], "edit", "EditLiteralAction",
            dummyNodeString, "0", ["50"]);
    });

    test("entering text multiple times makes the correct requests to the server", () => {
        expect.assertions(3);

        const bar = "5754";

        let input = getLiteralInputs()[0];
        input.setValue(foo);
        input.blur();

        checkActionExecuted(langSelectorLanguages[0], "edit", "EditLiteralAction",
            dummyNodeString, "0", [foo]);


        input = getLiteralInputs()[0];

        expect(input.getValue()).toEqual(foo);

        input.setValue(bar);
        input.blur();

        checkActionExecuted(langSelectorLanguages[0], "edit", "EditLiteralAction",
            `VariableNode("Num", List(LiteralNode(LiteralInt(${foo}))))`, "0", [bar]);
    });

    test("if the input text is the same as it was before (0), no server request is made", () => {
        expect.assertions(2);

        const input = getLiteralInputs()[0];
        input.setValue("0");
        input.blur();

        const initialRequestsReceived = getActionHistory().length;

        input.focus();
        input.blur();

        expect(getActionHistory()).toHaveLength(initialRequestsReceived);

        input.setValue(foo);
        input.setValue("0");
        input.blur();

        expect(getActionHistory()).toHaveLength(initialRequestsReceived);
    });

    test("if the input text is the same as it was before (not blank), no server request is made", () => {
        expect.assertions(2);

        let input = getLiteralInputs()[0];
        input.setValue(foo);
        input.blur();

        const initialRequestsReceived = getActionHistory().length;

        input = getLiteralInputs()[0];
        input.focus();
        input.blur();

        expect(getActionHistory()).toHaveLength(initialRequestsReceived);

        input.setValue("72838236872");
        input.setValue(foo);
        input.blur();

        expect(getActionHistory()).toHaveLength(initialRequestsReceived);
    });
});

describe("delete, copy, and paste buttons behave correctly", () => {
    beforeEach(() => {
        doStartNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getDropdownAt("0"), "Num");
        doLiteralEdit(getLiteralInputAt("0-0"), "4");
    });

    test("pressing delete makes the correct request to the server", () => {
        expect.assertions(1);

        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        getDeleteButton().click();

        checkActionExecuted(langSelectorLanguages[0], "edit", "DeleteAction",
            NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY, "0", []);
    });

    test("pressing copy does not make a request to the server", () => {
        expect.assertions(1);

        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        const initialRequestsReceived = getActionHistory().length;
        getCopyButton().click();
        expect(getActionHistory().length).toEqual(initialRequestsReceived);
    });

    test("clicking paste has no effect before copying something", () => {
        expect.assertions(1);
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        const initialRequestsReceived = getActionHistory().length;
        getPasteButton().click();
        expect(getActionHistory().length).toEqual(initialRequestsReceived);
    });

    test("clicking paste on same element after copying it makes the correct request to the server", () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        getCopyButton().click();
        contextMenuSelect(element);
        getPasteButton().click();
        checkActionExecuted(langSelectorLanguages[0], "edit", "PasteAction",
            NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY, "0", ["VariableNode(\"Num\", List(LiteralNode(LiteralInt(4))))"]);
    });

    test("clicking paste on another element after copying one makes the correct request to the server", () => {
        const element1 = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element1);

        getCopyButton().click();

        const element2 = document.querySelector('.subtree[data-tree-path="1"]') as HTMLElement;
        contextMenuSelect(element2);

        getPasteButton().click();

        checkActionExecuted(langSelectorLanguages[0], "edit", "PasteAction",
            NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY, "1", ["VariableNode(\"Num\", List(LiteralNode(LiteralInt(4))))"]);
    });

    test("clicking paste after changing tree state makes the correct request to the server", () => {
        contextMenuSelect(document.querySelector('[data-tree-path="0"]'));
        getCopyButton().click();

        getUndoButton().click();

        contextMenuSelect(document.querySelector('[data-tree-path=""]'));
        getPasteButton().click();

        checkActionExecuted(langSelectorLanguages[0], "edit", "PasteAction",
            NS.TIMES_LEFT_NUM_RIGHT_EMPTY, "", ["VariableNode(\"Num\", List(LiteralNode(LiteralInt(4))))"]);
    });
});

describe("mode radio buttons behave correctly", () => {
    beforeEach(() => {
        doStartNodeBlank();
    });

    test("the initial mode is edit", () => {
        expect(document.getElementById('edit-mode-radio')?.getAttributeNames()).toContain('checked');
    });

    test("clicking the type-check mode button makes the correct request to the server", () => {
        document.getElementById('type-check-mode-radio')?.click();

        checkActionExecuted(langSelectorLanguages[0], "type-check", "IdentityAction",
            "ExprChoiceNode()", "", []);
    });

    test("clicking the eval mode button makes the correct request to the server", () => {
        document.getElementById('eval-mode-radio')?.click();

        checkActionExecuted(langSelectorLanguages[0], "eval", "IdentityAction",
            "ExprChoiceNode()", "", []);
    });

    test("clicking the edit mode button makes the correct request to the server", () => {
        document.getElementById('type-check-mode-radio')?.click();

        document.getElementById('edit-mode-radio')?.click();

        checkActionExecuted(langSelectorLanguages[0], "edit", "IdentityAction",
            "ExprChoiceNode()", "", []);
    });

    test("after selecting a mode, future requests are made with that mode", () => {
        document.getElementById('type-check-mode-radio')?.click();

        selectExprOption(getLeftmostExprDropdown(), "Num");

        checkActionExecuted(langSelectorLanguages[0], "type-check", "SelectExprAction",
            "ExprChoiceNode()", "", ["Num"]);
    });
});
