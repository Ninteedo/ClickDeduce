import {beforeEach, describe, expect, test, vitest} from "vitest";
import {
    basicMocks,
    changeLanguage,
    contextMenuSelect,
    doLiteralEdit,
    getLeftmostExprDropdown,
    getLiteralInputAt,
    getTabbableElements,
    leftClickOn,
    loadIndexHtmlTemplate,
    selectExprOption,
    slightDelay
} from "./helper";
import {initialise} from "../initialise";
import {doStartNodeBlank, startNodeBlank} from "../actions";
import {ClickDeduceResponseError} from "../ClickDeduceResponseError";
import {getErrorDiv} from "../globals/elements";
import {getActiveContextMenu} from "../components/contextMenu/contextMenu";
import {IdDict} from "../globals/idDict";

const indexHtml = loadIndexHtmlTemplate();

beforeEach(() => {
    document.body.innerHTML = indexHtml;
    basicMocks();
    initialise(true);
});

describe("context menu behaves correctly", () => {
    beforeEach(() => {
        doStartNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        selectExprOption(getLeftmostExprDropdown(), "Plus");
    });

    test("context menu is initially hidden", () => {
        expect(getActiveContextMenu()).toBeFalsy();
        expect(document.getElementById(IdDict.CUSTOM_CONTEXT_MENU)).toBeFalsy();
    });

    test("right-clicking an element causes the context menu to appear", () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        expect(document.getElementById(IdDict.CUSTOM_CONTEXT_MENU)?.style.display).toEqual('block');
    });

    test("the selected element remains highlighted after the context menu appears", () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        expect(element.classList).toContain('highlight');
    });

    test("the context menu disappears when clicking away", () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        expect(getActiveContextMenu()).toBeTruthy();
        leftClickOn(document.querySelector('[data-tree-path=""]'));
        expect(getActiveContextMenu()).toBeFalsy();
        expect(document.getElementById(IdDict.CUSTOM_CONTEXT_MENU)).toBeFalsy();
    });

    test("right-clicking another element when the context menu is out causes the context menu to disappear", () => {
        const element1 = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element1);

        const element2 = document.querySelector('[data-tree-path="1"]') as HTMLElement;
        contextMenuSelect(element2);

        expect(document.getElementById(IdDict.CUSTOM_CONTEXT_MENU)).toBeFalsy();
    });

    test("right-clicking the context menu causes the context menu to disappear", () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        contextMenuSelect(document.getElementById(IdDict.CUSTOM_CONTEXT_MENU));
        expect(document.getElementById(IdDict.CUSTOM_CONTEXT_MENU)).toBeFalsy();
    });

    test("right-clicking on the selected element again causes the context menu to disappear", () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);
        contextMenuSelect(element);
        expect(document.getElementById(IdDict.CUSTOM_CONTEXT_MENU)).toBeFalsy();
    });
});

describe("tab cycling between input elements behaves correctly", () => {
    beforeEach(() => {
        doStartNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        doLiteralEdit(getLiteralInputAt("0-0-0"), "1");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        doLiteralEdit(getLiteralInputAt("0-1-0"), "2");
        selectExprOption(getLeftmostExprDropdown(), "Plus");
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        doLiteralEdit(getLiteralInputAt("1-0-0-0"), "3");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        doLiteralEdit(getLiteralInputAt("1-0-1-0"), "4");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        doLiteralEdit(getLiteralInputAt("1-1-0"), "5");
    });

    test("test can find a list of tabbable elements", () => {
        const tabbableElements = getTabbableElements();
        expect(tabbableElements).toHaveLength(5);

        const paths = ["0-0-0", "0-1-0", "1-0-0-0", "1-0-1-0", "1-1-0"];

        tabbableElements.forEach((element, index) => {
            expect(element.getAttribute("data-tree-path")).toEqual(paths[index]);
            expect(element).toBeInstanceOf(HTMLInputElement);
            expect(element.attributes).not.toContain('disabled');
        });
    });

    test("tabbing through the elements in order works", () => {
        const tabbableElements = getTabbableElements();

        tabbableElements[0].focus();
        expect(document.activeElement).toEqual(tabbableElements[0]);

        tabbableElements.forEach((element, index) => {
            element.dispatchEvent(new KeyboardEvent('keydown', {
                key: 'Tab'
            }));
            console.log(`index: ${index}`);
            expect(document.activeElement).toEqual(tabbableElements[(index + 1) % tabbableElements.length]);
        });
    });

    test("tabbing through the elements in reverse order works", () => {
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
    beforeEach(() => {
        startNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getLeftmostExprDropdown(), "Num");
    });

    test("test can find a list of tabbable elements", () => {
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

    test("tabbing through the elements in order works", () => {
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

    test("tabbing through the elements in reverse order works", () => {
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
    beforeEach(() => {
        startNodeBlank();
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getLeftmostExprDropdown(), "Num");
    });

    test("input focus is preserved when a literal is edited and ENTER is pressed", async () => {
        const input = document.querySelector('input[data-tree-path="0-0"]') as HTMLInputElement;
        input.focus();
        input.value = "8";
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

    test("input focus is not preserved when a literal is edited and then something else is clicked", () => {
        const input = document.querySelector('input[data-tree-path="0-0"]') as HTMLInputElement;
        input.focus();
        input.value = "8";
        input.dispatchEvent(new Event('blur'));

        slightDelay();
        const newInput = document.querySelector('input[data-tree-path="0-0"]') as HTMLInputElement;
        expect(newInput.value).toEqual("8");
        expect(document.activeElement).not.toEqual(newInput);
        expect(document.activeElement).not.toEqual(input);
        expect(document.activeElement).toEqual(document.body);
    });
});

describe("responses to server errors are appropriate", () => {
    function triggerError(): void {
        changeLanguage(0);
    }

    beforeEach(() => {
        doStartNodeBlank();
        changeLanguage(1);
        selectExprOption(getLeftmostExprDropdown(), "Bool");
    });

    test("an error is thrown in the console", () => {
        try {
            triggerError()
        } catch (e) {
            expect(e).toBeInstanceOf(ClickDeduceResponseError);
        }
    });

    test("error div becomes visible", () => {
        try {
            triggerError();
        } catch (e) {
        }
        expect(getErrorDiv().classList).toContain('fade-in');
        expect(getErrorDiv().classList).not.toContain('fade-out');
    });

    test("error div contains the error message", () => {
        try {
            triggerError();
        } catch (e) {
        }
        expect(getErrorDiv().textContent).toContain("Bool");
        expect(getErrorDiv().textContent?.length).toBeGreaterThan(5);
    });

    test("error div becomes invisible after a timeout", () => {
        vitest.useFakeTimers();

        try {
            triggerError();
        } catch (e) {
        }

        vitest.advanceTimersByTime(10000);

        expect(getErrorDiv().classList).not.toContain('fade-in');
        expect(getErrorDiv().classList).toContain('fade-out');

        vitest.useRealTimers();
    });
});
