import {beforeEach, describe, expect, test} from "@jest/globals";
import {initialise} from "../initialise";
import {doStartNodeBlank, handleLiteralChanged} from "../actions";
import {
    changeLanguage,
    contextMenuSelect,
    doLiteralEdit,
    getDropdownAt,
    getLeftmostExprDropdown,
    getLiteralInputAt,
    getRedoButton,
    getUndoButton,
    loadHtmlTemplate,
    selectExprOption,
    slightDelay
} from "./helper";
import * as NS from "../../test_resources/node_strings";
import {getNodeStringFromPath} from "../treeManipulation";

const indexHtml = loadHtmlTemplate('../pages/index');

beforeEach(async () => {
    document.body.innerHTML = indexHtml;
    await initialise(true);
});

describe("undo and redo buttons behave correctly", () => {
    async function updateTree(nodeString: string, html: string): Promise<void> {
        const dropdown = getLeftmostExprDropdown();
        if (dropdown) {
            await selectExprOption(getLeftmostExprDropdown(), "Num");
            document.querySelectorAll('input.expr-selector-input').forEach(input => input.dispatchEvent(new Event('blur')));
            console.log("done");
        } else {
            const literalInput = document.querySelector('input.literal') as HTMLInputElement;
            literalInput.value = literalInput.value + " ";
            await handleLiteralChanged(literalInput);
        }
    }

    async function doEdit1() {
        await selectExprOption(getDropdownAt(""), "Num");
    }

    async function doEdit2() {
        await doLiteralEdit(getLiteralInputAt("0"), "5871");
    }

    async function doEdit3() {
        await selectExprOption(getDropdownAt(""), "Bool");
    }

    beforeEach(async () => {
        await doStartNodeBlank();
    });

    test("undo and redo buttons begin disabled", () => {
        expect(getUndoButton().getAttributeNames()).toContain('disabled');
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("undo button is enabled after an action", async () => {
        expect.assertions(1);
        await doEdit1();
        expect(getUndoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("redo button is still disabled after an action", async () => {
        expect.assertions(1);
        await doEdit1();
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("undo button is disabled when there are no more history", async () => {
        expect.assertions(1);
        await doEdit1();
        getUndoButton().click();
        expect(getUndoButton().getAttributeNames()).toContain('disabled');
    });

    test("redo button is enabled after an undo", async () => {
        expect.assertions(1);
        await doEdit1();
        getUndoButton().click();
        expect(getRedoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("pressing undo reverts the tree to the previous state", async () => {
        expect.assertions(1);
        const prevHtml = document.getElementById('tree').innerHTML;
        await doEdit1();
        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(prevHtml);
    });

    test("pressing undo twice reverts the tree to the state before the previous state", async () => {
        expect.assertions(2);
        const state1Html = document.getElementById('tree').innerHTML;
        await doEdit1();
        const state2Html = document.getElementById('tree').innerHTML;
        await doEdit2();

        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state2Html);

        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state1Html);
    });

    test("pressing undo and then redo reverts the tree to the most recent state", async () => {
        expect.assertions(1);
        await doEdit1();
        const state2Html = document.getElementById('tree').innerHTML;
        getUndoButton().click();
        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state2Html);
    });

    test("pressing undo and then redo twice reverts the tree to the state before the most recent state", async () => {
        expect.assertions(2);
        await doEdit1();
        const state2Html = document.getElementById('tree').innerHTML;
        await doEdit2();
        const state3Html = document.getElementById('tree').innerHTML;

        getUndoButton().click();
        getUndoButton().click();
        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state2Html);

        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state3Html);
    });

    test("pressing undo then performing an action disables redo", async () => {
        expect.assertions(1);
        await doEdit1();
        getUndoButton().click();
        await doEdit3();
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("pressing undo then performing an action then undoing again enables redo", async () => {
        expect.assertions(1);
        await doEdit1();
        getUndoButton().click();
        await doEdit3();
        getUndoButton().click();
        expect(getRedoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("pressing undo then performing an action then undoing again can be redone correctly", async () => {
        expect.assertions(1);
        await doEdit1();
        getUndoButton().click();
        await doEdit3();
        const state2Html = document.getElementById('tree').innerHTML;
        getUndoButton().click();
        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state2Html);
    });
});

describe("hovering over a node behaves correctly", () => {
    beforeEach(async () => {
        await doStartNodeBlank();
        await selectExprOption(getLeftmostExprDropdown(), "Plus");
        await selectExprOption(getDropdownAt("0"), "Num");
        await doLiteralEdit(getLiteralInputAt("0-0"), "51617812");
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

describe("phantom inputs are made read-only and disabled", () => {
    const selector = `.phantom input.literal, select[class="expr-dropdown"]`;

    beforeEach(async () => {
        await doStartNodeBlank();

        await changeLanguage(4);  // LLambda
        await selectExprOption(getLeftmostExprDropdown(), "Apply");
        await selectExprOption(getDropdownAt("0"), "Lambda");
        await doLiteralEdit(getLiteralInputAt("0-0"), "x");
        await selectExprOption(getDropdownAt("0-1"), "IntType");
        await selectExprOption(getDropdownAt("0-2"), "Plus");
        await selectExprOption(getDropdownAt("0-2-0"), "Var");
        await doLiteralEdit(getLiteralInputAt("0-2-0-0"), "x");
        await selectExprOption(getDropdownAt("1"), "Num");
        await doLiteralEdit(getLiteralInputAt("1-0"), "hi");

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

describe("node string can be queried correctly", () => {
    beforeEach(async () => {
        await doStartNodeBlank();
        await selectExprOption(getLeftmostExprDropdown(), "Plus");
        await selectExprOption(getDropdownAt("0"), "Times");
        await selectExprOption(getDropdownAt("0-0"), "Bool");
        await doLiteralEdit(getLiteralInputAt("0-0-0"), "test\"()\\(\\)\\\")");
        await selectExprOption(getDropdownAt("0-1"), "Num");
        await selectExprOption(getDropdownAt("1"), "IfThenElse");
        await selectExprOption(getDropdownAt("1-0"), "Bool");
        await doLiteralEdit(getLiteralInputAt("1-0-0"), "eg");
    });

    test("node string can be queried correctly", async () => {
        expect(getNodeStringFromPath("")).toEqual(NS.NODE_STRING_PATH_TEST_EXAMPLE);
        expect(getNodeStringFromPath("0")).toEqual(`VariableNode("Times", List(SubExprNode(VariableNode("Bool", List(LiteralNode("test\\"()\\\\(\\\\)\\\\\\")")))), SubExprNode(VariableNode("Num", List(LiteralNode(""))))))`);
        expect(getNodeStringFromPath("1")).toEqual(`VariableNode("IfThenElse", List(SubExprNode(VariableNode("Bool", List(LiteralNode("eg")))), SubExprNode(ExprChoiceNode()), SubExprNode(ExprChoiceNode())))`);
        expect(getNodeStringFromPath("0-0")).toEqual(`VariableNode("Bool", List(LiteralNode("test\\"()\\\\(\\\\)\\\\\\")")))`);
        expect(getNodeStringFromPath("0-1")).toEqual(`VariableNode("Num", List(LiteralNode("")))`);
        expect(getNodeStringFromPath("1-0")).toEqual(`VariableNode("Bool", List(LiteralNode("eg")))`);
        expect(getNodeStringFromPath("1-1")).toEqual(`ExprChoiceNode()`);
        expect(getNodeStringFromPath("1-2")).toEqual(`ExprChoiceNode()`);
    });
})
