import {afterAll, beforeAll, beforeEach, describe, expect, jest, test} from "@jest/globals";
import {initialise} from "../initialise";
import {
    defaultHtml,
    mockEvent,
    resetRequestTracking,
    setActionFetchResponse,
    setUpFetchMock,
    startNodeBlankArithHTML
} from "./requestMocking";
import {doStartNodeBlank, handleLiteralChanged} from "../actions";
import {
    contextMenuSelect,
    getLeftmostExprDropdown,
    getRedoButton,
    getUndoButton,
    loadHtmlTemplate,
    selectExprOption,
    slightDelay
} from "./helper";
import * as NS from "../../test_resources/node_strings";
import {numNodeArithHTML, plusNodeArithHTML} from "./serverMock.test";
import {getNodeStringFromPath} from "../treeManipulation";

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

    beforeEach(async () => {
        await doStartNodeBlank(mockEvent);
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
        const prevHtml = document.getElementById('tree').innerHTML;
        await updateTree(nodeString2, html2);
        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(prevHtml);
    });

    test("pressing undo twice reverts the tree to the state before the previous state", async () => {
        expect.assertions(2);
        const state1Html = document.getElementById('tree').innerHTML;
        await updateTree(nodeString2, html2);
        const state2Html = document.getElementById('tree').innerHTML;
        await updateTree(nodeString3, html3);

        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state2Html);

        getUndoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state1Html);
    });

    test("pressing undo and then redo reverts the tree to the most recent state", async () => {
        expect.assertions(1);
        await updateTree(nodeString2, html2);
        const state2Html = document.getElementById('tree').innerHTML;
        getUndoButton().click();
        getRedoButton().click();
        expect(document.getElementById('tree').innerHTML).toEqual(state2Html);
    });

    test("pressing undo and then redo twice reverts the tree to the state before the most recent state", async () => {
        expect.assertions(2);
        await updateTree(nodeString2, html2);
        const state2Html = document.getElementById('tree').innerHTML;
        await updateTree(nodeString3, html3);
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
        await doStartNodeBlank(mockEvent);
        setActionFetchResponse(nodeString, html);
        await selectExprOption(getLeftmostExprDropdown(), "Num");
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
    const html = loadHtmlTemplate('phantom_example');

    const selector = `input.literal[name="eg"], select[name="77"]`;

    beforeEach(async () => {
        await doStartNodeBlank(mockEvent);

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

describe("node string can be queried correctly", () => {
    const nodeString = NS.NODE_STRING_PATH_TEST_EXAMPLE;
    const html = loadHtmlTemplate('times_left_filled_num_right_empty');

    beforeEach(async () => {
        await doStartNodeBlank(mockEvent);
        setActionFetchResponse(nodeString, html);
        await selectExprOption(getLeftmostExprDropdown(), "Times");
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
