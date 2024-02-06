import {beforeEach, describe, expect, jest, test} from "@jest/globals";
import {
    defaultHtml,
    mockEvent,
    prepareExampleTimesTree,
    resetRequestTracking,
    setActionErrorMessage,
    setActionFetchResponse,
} from "./requestMocking";
import {
    contextMenuSelect,
    getErrorDiv,
    getLeftmostExprDropdown,
    getTabbableElements,
    leftClickOn,
    loadHtmlTemplate,
    selectExprOption,
    slightDelay
} from "./helper";
import {initialise} from "../initialise";
import * as NS from "../../test_resources/node_strings";
import {doStartNodeBlank, handleLiteralChanged} from "../actions";
import {ClickDeduceResponseError} from "../ClickDeduceResponseError";

beforeEach(async () => {
    resetRequestTracking();
    document.body.innerHTML = defaultHtml;
    await initialise(true);
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

describe("tab cycling between input elements behaves correctly", () => {
    const nodeString = NS.TABBING_EXAMPLE;
    const html = loadHtmlTemplate('tabbing_example');

    beforeEach(async () => {
        await doStartNodeBlank(mockEvent);
        setActionFetchResponse(nodeString, html);
        await selectExprOption(getLeftmostExprDropdown(), "Num");
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
        expect(tabbableElements[0].classList).toContain('literal');
        expect(tabbableElements[1]).toBeInstanceOf(HTMLInputElement);
        expect(tabbableElements[1].classList).toContain('expr-selector-input');

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

        jest.useRealTimers();
    });
});
