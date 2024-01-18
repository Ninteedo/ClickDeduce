import {afterAll, beforeAll, beforeEach, describe, expect, jest, test} from "@jest/globals";
import {initialise} from "../initialise";
import {
    defaultHtml,
    mockEvent,
    resetRequestTracking,
    setActionFetchResponse,
    setUpFetchMock,
    startNodeBlankArithHTML
} from "./request_mocking";
import {handleDropdownChange, handleLiteralChanged, handleSubmit} from "../actions";
import {contextMenuSelect, getRedoButton, getUndoButton, loadHtmlTemplate, slightDelay} from "./helper";
import * as NS from "../../test_resources/node_strings";
import {numNodeArithHTML, plusNodeArithHTML} from "./server_mock.test";

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
