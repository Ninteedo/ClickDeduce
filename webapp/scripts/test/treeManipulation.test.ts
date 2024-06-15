import {beforeEach, describe, expect, test} from "vitest";
import {initialise} from "../initialise";
import {doStartNodeBlank} from "../actions";
import {
    changeLanguage,
    contextMenuSelect,
    doLiteralEdit,
    getDropdownAt,
    getLeftmostExprDropdown,
    getLiteralInputAt,
    getRedoButton,
    getTree,
    getUndoButton,
    loadHtmlTemplate,
    selectExprOption,
    slightDelay
} from "./helper";
import * as NS from "../../test_resources/node_strings";
import {getNodeStringFromPath} from "../treeManipulation";

const indexHtml = loadHtmlTemplate('../pages/index');

beforeEach(() => {
    document.body.innerHTML = indexHtml;
    initialise(true);
});

describe("undo and redo buttons behave correctly", () => {
    function doEdit1() {
        selectExprOption(getDropdownAt(""), "Num");
    }

    function doEdit2() {
        doLiteralEdit(getLiteralInputAt("0"), "5871");
    }

    function doEdit3() {
        changeLanguage(1);
        selectExprOption(getDropdownAt(""), "Bool");
    }

    beforeEach(() => {
        doStartNodeBlank();
    });

    test("undo and redo buttons begin disabled", () => {
        expect(getUndoButton().getAttributeNames()).toContain('disabled');
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("undo button is enabled after an action", () => {
        expect.assertions(1);
        doEdit1();
        expect(getUndoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("redo button is still disabled after an action", () => {
        expect.assertions(1);
        doEdit1();
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("undo button is disabled when there are no more history", () => {
        expect.assertions(1);
        doEdit1();
        getUndoButton().click();
        expect(getUndoButton().getAttributeNames()).toContain('disabled');
    });

    test("redo button is enabled after an undo", () => {
        expect.assertions(1);
        doEdit1();
        getUndoButton().click();
        expect(getRedoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("pressing undo reverts the tree to the previous state", () => {
        expect.assertions(1);
        const prevHtml = getTree().innerHTML;
        doEdit1();
        getUndoButton().click();
        expect(getTree().innerHTML).toEqual(prevHtml);
    });

    test("pressing undo twice reverts the tree to the state before the previous state", () => {
        expect.assertions(2);
        const state1Html = getTree().innerHTML;
        doEdit1();
        const state2Html = getTree().innerHTML;
        doEdit2();

        getUndoButton().click();
        expect(getTree().innerHTML).toEqual(state2Html);

        getUndoButton().click();
        expect(getTree().innerHTML).toEqual(state1Html);
    });

    test("pressing undo and then redo reverts the tree to the most recent state", () => {
        expect.assertions(1);
        doEdit1();
        const state2Html = getTree().innerHTML;
        getUndoButton().click();
        getRedoButton().click();
        expect(getTree().innerHTML).toEqual(state2Html);
    });

    test("pressing undo and then redo twice reverts the tree to the state before the most recent state", () => {
        expect.assertions(2);
        doEdit1();
        const state2Html = getTree().innerHTML;
        doEdit2();
        const state3Html = getTree().innerHTML;

        getUndoButton().click();
        getUndoButton().click();
        getRedoButton().click();
        expect(getTree().innerHTML).toEqual(state2Html);

        getRedoButton().click();
        expect(getTree().innerHTML).toEqual(state3Html);
    });

    test("pressing undo then performing an action disables redo", () => {
        expect.assertions(1);
        doEdit1();
        getUndoButton().click();
        doEdit3();
        expect(getRedoButton().getAttributeNames()).toContain('disabled');
    });

    test("pressing undo then performing an action then undoing again enables redo", () => {
        expect.assertions(1);
        doEdit1();
        getUndoButton().click();
        doEdit3();
        getUndoButton().click();
        expect(getRedoButton().getAttributeNames()).not.toContain('disabled');
    });

    test("pressing undo then performing an action then undoing again can be redone correctly", () => {
        expect.assertions(1);
        doEdit1();
        getUndoButton().click();
        doEdit3();
        const state2Html = getTree().innerHTML;
        getUndoButton().click();
        getRedoButton().click();
        expect(getTree().innerHTML).toEqual(state2Html);
    });
});

describe("hovering over a node behaves correctly", () => {
    beforeEach(() => {
        doStartNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Plus");
        selectExprOption(getDropdownAt("0"), "Num");
        doLiteralEdit(getLiteralInputAt("0-0"), "51617812");
    });

    test("mousing over a node highlights it", () => {
        expect.assertions(1);
        const node = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        node.dispatchEvent(new MouseEvent('mouseover', {
            bubbles: true,
            cancelable: true,
        }));
        expect(node.classList).toContain('highlight');
    });

    test("mousing out from a highlighted node unhighlights it", () => {
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

    test("mousing out from a highlighted node while it is focused by the context menu does not unhighlight it", () => {
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
    const literalSelector = `.phantom input.literal`;
    const placeholderSelector = `.phantom div.expr-selector-placeholder`;

    beforeEach(() => {
        doStartNodeBlank();

        changeLanguage(4);  // LLambda
        selectExprOption(getLeftmostExprDropdown(), "Apply");
        selectExprOption(getDropdownAt("0"), "Lambda");
        doLiteralEdit(getLiteralInputAt("0-0"), "x");
        selectExprOption(getDropdownAt("0-1"), "IntType");
        selectExprOption(getDropdownAt("0-2"), "Plus");
        selectExprOption(getDropdownAt("0-2-0"), "Var");
        doLiteralEdit(getLiteralInputAt("0-2-0-0"), "x");
        selectExprOption(getDropdownAt("1"), "Num");
        doLiteralEdit(getLiteralInputAt("1-0"), "hi");

        document.getElementById("eval-mode-radio")?.click();
        slightDelay();
    });

    test("phantom inputs are made read-only and disabled", () => {
        const literalInputs = document.querySelectorAll(literalSelector);
        expect(literalInputs).toHaveLength(2);
        literalInputs.forEach(input => {
            expect(input.getAttributeNames()).toContain('readonly');
            expect(input.getAttributeNames()).toContain('disabled');
        });

        const placeholderDivs = document.querySelectorAll(placeholderSelector);
        expect(placeholderDivs).toHaveLength(2);
    });
});

describe("node string can be queried correctly", () => {
    beforeEach(() => {
        changeLanguage(1);  // LIf
        doStartNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Plus");
        selectExprOption(getDropdownAt("0"), "Times");
        selectExprOption(getDropdownAt("0-0"), "Bool");
        doLiteralEdit(getLiteralInputAt("0-0-0"), "test\"()\\(\\)\\\")");
        selectExprOption(getDropdownAt("0-1"), "Num");
        selectExprOption(getDropdownAt("1"), "IfThenElse");
        selectExprOption(getDropdownAt("1-0"), "Bool");
        doLiteralEdit(getLiteralInputAt("1-0-0"), "eg");
    });

    test("node string can be queried correctly", () => {
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
