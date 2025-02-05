import {beforeEach, describe, expect, test} from "vitest";
import {initialise} from "../initialise";
import {
    basicMocks,
    changeLanguage,
    doLiteralEdit,
    getActiveSubtreeContextMenu,
    getExprDropdownOptions,
    getLeftmostExprDropdown,
    loadIndexHtmlTemplate,
    pressStartNodeButton,
    selectExprOption,
    slightDelay
} from "./helper";
import {CustomExprSelector} from "../components/customExprSelector";
import {getExprSelectors} from "../treeManipulation";
import {getLangSelector, getTree, getUndoButton} from "../globals/elements";

const indexHtml = loadIndexHtmlTemplate();

beforeEach(() => {
    document.body.innerHTML = indexHtml;
    initialise(true);
    basicMocks();
});

describe('lang selector is correctly initialised on load', () => {
    function getLangSelectorDiv(): HTMLElement {
        const langSelectorDiv = document.getElementById('lang-selector-div');
        if (!langSelectorDiv) throw new Error('lang-selector-div not found');``
        return langSelectorDiv;
    }

    function getLangSelectorOptions() {
        return document.querySelectorAll('#lang-selector option');
    }

    test('lang selector container is present', () => {
        expect(getLangSelectorDiv()).toBeTruthy();
    });

    test('lang selector is present', () => {
        expect(getLangSelector()).toBeTruthy();
    });

    test('lang selector is inside lang selector container', () => {
        expect(getLangSelectorDiv().children).toContain(getLangSelector());
    });

    test('lang selector is a select element', () => {
        expect(getLangSelector().tagName.toLowerCase()).toBe('select');
    });

    test('lang selector has at least one option', () => {
        expect(getLangSelectorOptions().length).toBeGreaterThan(0);
    });

    test('each lang selector option has a value attribute and a text content', () => {
        getLangSelectorOptions().forEach(option => {
            expect(option.getAttributeNames()).toContain('value');
            expect(option.getAttribute('value')?.length).toBeGreaterThan(0);
            expect(option.textContent).toBeTruthy();
            expect(option.textContent?.length).toBeGreaterThan(0);
        });
    });

    test('each lang selector option has unique value and text content', () => {
        let values: Set<string> = new Set();
        let texts: Set<string> = new Set();

        getLangSelectorOptions().forEach(option => {
            values.add(option.getAttribute('value')!);
            texts.add(option.textContent!);
        });

        expect(values.size).toBe(getLangSelectorOptions().length);
        expect(texts.size).toBe(getLangSelectorOptions().length);
    });
});

describe('start node function has correct effect', () => {
    test('start node results in a single node being added to the empty tree', () => {
        const tree = getTree();
        pressStartNodeButton();

        expect(tree.children).toHaveLength(1);
        expect(tree.querySelectorAll('.subtree')).toHaveLength(1);
    });

    test('the added node has the correct content', () => {
        pressStartNodeButton();
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(1);
        expect(document.querySelectorAll('input.expr-selector-input')).toHaveLength(1);
        expect(document.querySelectorAll('.expr-selector-dropdown > ul > li')).toHaveLength(4);
        expect(document.querySelectorAll('.annotation-axiom')).toHaveLength(1);
    });
});

describe('selecting an option from the expression choice dropdown has the correct effect', () => {
    beforeEach(() => {
        pressStartNodeButton();
    });

    test('selecting the "Num" option has correct effect', () => {
        selectExprOption(getLeftmostExprDropdown(), "Num");

        expect(document.querySelectorAll('.subtree')).toHaveLength(1);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(1);
        expect(document.querySelectorAll('select.expr-dropdown')).toHaveLength(0);
        expect(document.querySelectorAll('.annotation-axiom')).toHaveLength(1);
        expect(document.querySelectorAll('.expr')).toHaveLength(1);

        const node = document.querySelector('.subtree.axiom') as HTMLElement;
        expect(node.getAttribute('data-tree-path')).toBe('');

        expect(node.querySelectorAll('input')).toHaveLength(1);
        const input = node.querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('0');
        expect(input.getAttribute('data-tree-path')).toBe('0');
    });

    test('selecting the "Plus" option has correct effect', () => {
        selectExprOption(getLeftmostExprDropdown(), "Plus");

        expect(document.querySelectorAll('.subtree')).toHaveLength(3);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(2);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(2);
        expect(document.querySelectorAll('.expr')).toHaveLength(3);
    });

    test('selecting the "Times" option then the "Plus" and "Num" options has correct effect', () => {
        selectExprOption(getLeftmostExprDropdown(), "Times");

        expect(document.querySelectorAll('.subtree')).toHaveLength(3);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(2);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(2);
        expect(document.querySelectorAll('.expr')).toHaveLength(3);

        selectExprOption(getLeftmostExprDropdown(), "Plus");
        expect(document.querySelectorAll('.subtree')).toHaveLength(5);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(3);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(3);
        expect(document.querySelectorAll('.expr')).toHaveLength(5);

        selectExprOption(getLeftmostExprDropdown(), "Num");
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(2);

        selectExprOption(getLeftmostExprDropdown(), "Num");
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(1);

        selectExprOption(getLeftmostExprDropdown(), "Num");
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(0);

        const tree = getTree();

        expect(tree.querySelectorAll('.subtree')).toHaveLength(5);
        expect(tree.querySelectorAll('.subtree.axiom')).toHaveLength(3);
        expect(tree.querySelectorAll('.expr')).toHaveLength(5);
        expect(tree.querySelectorAll('input.expr-selector-input')).toHaveLength(0);
        expect(tree.querySelectorAll('input.literal:not([readonly])')).toHaveLength(3);
        expect(tree.querySelectorAll('.annotation-new')).toHaveLength(2);

        const inputDataPaths = ['0-0-0', '0-1-0', '1-0'];
        tree.querySelectorAll('input.literal:not([readonly])').forEach((input, index) => {
            expect(input.getAttribute('data-tree-path')).toBe(inputDataPaths[index]);
        });
    });
});

describe('behaviour of changing the selected language is correct', () => {
    beforeEach(() => {
        pressStartNodeButton();
    });

    test('changing the selected language with only the initial node present has correct effect', () => {
        let prevSelect = getLeftmostExprDropdown();

        for (let i = 1; i < getLangSelector().options.length; i++) {
            changeLanguage(i);
            expect(getLeftmostExprDropdown()).not.toBe(prevSelect);
            expect(getExprDropdownOptions(getLeftmostExprDropdown()).length).toBeGreaterThan(getExprDropdownOptions(prevSelect).length);
        }
    });

    test('can change to a child language with more existing expressions', () => {
        changeLanguage(1);

        selectExprOption(getLeftmostExprDropdown(), "IfThenElse");
        selectExprOption(getLeftmostExprDropdown(), "Bool");
        selectExprOption(getLeftmostExprDropdown(), "Plus");

        let prevSelect = getLeftmostExprDropdown();

        for (let i = 2; i < getLangSelector().options.length; i++) {
            changeLanguage(i);
            expect(getLeftmostExprDropdown()).not.toBe(prevSelect);
            expect(getExprDropdownOptions(getLeftmostExprDropdown()).length).toBeGreaterThan(getExprDropdownOptions(prevSelect).length);
        }
    });

    test('can change to a parent language, as long as the current tree only uses expressions present in the parent language', () => {
        let languageSizes: number[] = [getExprDropdownOptions(getLeftmostExprDropdown()).length];
        changeLanguage(1);
        languageSizes.push(getExprDropdownOptions(getLeftmostExprDropdown()).length);
        changeLanguage(2);
        languageSizes.push(getExprDropdownOptions(getLeftmostExprDropdown()).length);

        selectExprOption(getLeftmostExprDropdown(), "IfThenElse");
        selectExprOption(getLeftmostExprDropdown(), "Bool");

        const nodeString = document.querySelector('.subtree.axiom')?.getAttribute('data-node-string');

        changeLanguage(1);

        expect(getExprDropdownOptions(getLeftmostExprDropdown()).length).toBe(languageSizes[1]);
        expect(document.querySelector('.subtree.axiom')?.getAttribute('data-node-string')).toBe(nodeString);
    });

    // test('cannot change to a parent language if the current tree uses expressions not present in the parent language', () => {
    //     changeLanguage(1);
    //
    //     selectExprOption(getLeftmostExprDropdown(), 5);
    //     selectExprOption(getLeftmostExprDropdown(), 4);
    //     selectExprOption(getLeftmostExprDropdown(), 2);
    //
    //     try {
    //         changeLanguage(0).then(() => {});
    //     } catch (error) {
    //         expect(error).toBeInstanceOf(ClickDeduceResponseError);
    //     }
    // });
});

describe('behaviour of editing literals is correct', () => {
    beforeEach(() => {
        pressStartNodeButton();
    });

    test('can edit the literal of a single literal node', () => {
        selectExprOption(getLeftmostExprDropdown(), "Num");

        let input = getTree().querySelector('input') as HTMLInputElement;
        doLiteralEdit(input, '123');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('123');
    });

    test('can edit the literal of a "Let"', () => {
        changeLanguage(2);
        selectExprOption(getLeftmostExprDropdown(), "Let");

        let input = getTree().querySelector('input') as HTMLInputElement;
        doLiteralEdit(input, 'foo');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('foo');
    });

    test('can edit one literal multiple times', () => {
        selectExprOption(getLeftmostExprDropdown(), "Num");

        let input = getTree().querySelector('input') as HTMLInputElement;
        doLiteralEdit(input, '123');
        doLiteralEdit(input, '456');
        doLiteralEdit(input, '789');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('789');
    });
});

function getLeftmostTypeDropdown(): CustomExprSelector {
    return getExprSelectors().find(selector => selector.isTypeSelector())!;
}

function selectTypeOption(selector: CustomExprSelector, typeName: string): void {
    selectExprOption(selector, typeName);
}

describe('behaviour of manipulating trees with type selections is correct', () => {
    beforeEach(() => {
        pressStartNodeButton();
        changeLanguage(3);
        selectExprOption(getLeftmostExprDropdown(), "Lambda");  // lambda
    });

    test('can select a simple type (int)', () => {
        selectTypeOption(getLeftmostTypeDropdown(), "IntType");
        const typeSubtree = getTree().querySelector('.subtree[data-tree-path="1"]');
        expect(typeSubtree).toBeTruthy();
    });

    test('can select a simple type (bool)', () => {
        selectTypeOption(getLeftmostTypeDropdown(), "BoolType");
        const typeSubtree = getTree().querySelector('.subtree[data-tree-path="1"]');
        expect(typeSubtree).toBeTruthy();
    });

    test('can select a complex type (func int -> bool)', () => {
        selectTypeOption(getLeftmostTypeDropdown(), "Func");

        expect(getTree().querySelectorAll('.expr-selector-container[data-kind="type"]')).toHaveLength(2);

        selectTypeOption(getLeftmostTypeDropdown(), "IntType");
        expect(getTree().querySelectorAll('.expr-selector-container[data-kind="type"]')).toHaveLength(1);
        selectTypeOption(getLeftmostTypeDropdown(), "BoolType");
        expect(getTree().querySelectorAll('.expr-selector-container[data-kind="type"]')).toHaveLength(0);
    });
});

describe("delete, copy, and paste buttons behave correctly", () => {
    function contextMenuSelect(element: HTMLElement | null): void {
        if (!element) throw new Error('Element is null');
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

    beforeEach(() => {
        slightDelay(10);
        pressStartNodeButton();
        changeLanguage(3);
        selectExprOption(getLeftmostExprDropdown(), "Plus");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        doLiteralEdit(getTree().querySelector('input.literal[data-tree-path="0-0"]') as HTMLInputElement, 'foo');
        selectExprOption(getLeftmostExprDropdown(), "Bool");
        doLiteralEdit(getTree().querySelector('input.literal[data-tree-path="1-0"]') as HTMLInputElement, 'bar');
        console.log('Tree setup done');
    });

    test("pressing delete clears the selected node", () => {
        expect.assertions(2);

        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        getActiveSubtreeContextMenu().deleteEntry.doClick();

        new Promise(resolve => setTimeout(resolve, 50));

        expect(getTree().querySelector('.subtree[data-tree-path="0"]')).toBeTruthy();
        expect(getTree().querySelector('.subtree[data-tree-path="0-1"]')).toBeFalsy();
    });

    test("clicking paste on same element after copying it results in the same tree", () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        getActiveSubtreeContextMenu().copyEntry.doClick();

        const initialTreeState = getTree().innerHTML;

        contextMenuSelect(element);

        getActiveSubtreeContextMenu().pasteEntry.doClick();

        new Promise(resolve => setTimeout(resolve, 50));

        expect(getTree().innerHTML).toBe(initialTreeState);
    });

    test("clicking paste on another element after copying one results in the correct tree", () => {
        const element1 = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element1);

        getActiveSubtreeContextMenu().copyEntry.doClick();

        const element2 = document.querySelector('.subtree[data-tree-path="1"]') as HTMLElement;
        contextMenuSelect(element2);

        getActiveSubtreeContextMenu().pasteEntry.doClick();

        new Promise(resolve => setTimeout(resolve, 50));

        expect(getTree().innerHTML).not.toBe(element1.innerHTML);
    });

    test("clicking paste after changing tree state makes the correct request to the server", () => {
        contextMenuSelect(document.querySelector('[data-tree-path="0"]'));
        getActiveSubtreeContextMenu().copyEntry.doClick();
        new Promise(resolve => setTimeout(resolve, 50));

        getUndoButton().click();
        new Promise(resolve => setTimeout(resolve, 50));

        contextMenuSelect(document.querySelector('[data-tree-path=""]'));
        getActiveSubtreeContextMenu().pasteEntry.doClick();

        new Promise(resolve => setTimeout(resolve, 50));

        expect(document.querySelector('[data-tree-path=""]')).toBeTruthy();
        expect(document.querySelector('[data-tree-path="0-0"]')).toBeFalsy();
        expect(document.querySelector('[data-tree-path="1"]')).toBeFalsy();
    });
});

describe("input focus is preserved when tabbing as the tree is updated", () => {
    beforeEach(() => {
        pressStartNodeButton();
        selectExprOption(getLeftmostExprDropdown(), "Times");
        selectExprOption(getLeftmostExprDropdown(), "Plus");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        selectExprOption(getLeftmostExprDropdown(), "Num");
        doLiteralEdit(getTree().querySelector('input[data-tree-path="0-0-0"]') as HTMLInputElement, '44');
        doLiteralEdit(getTree().querySelector('input[data-tree-path="0-1-0"]') as HTMLInputElement, '55');
    });

    test("editing a literal and then tabbing to the next input element updates the tree correctly", () => {
        const input = getTree().querySelector('input[data-tree-path="0-0-0"]') as HTMLInputElement;
        input.value = '77';
        input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));
        input.dispatchEvent(new Event('blur'));

        slightDelay();

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]')?.getAttribute('value')).toBe('77');
    });

    test("editing a literal and then tabbing to another literal sets the focus to the next input element", () => {
        const input = getTree().querySelector('input[data-tree-path="0-0-0"]') as HTMLInputElement;
        input.value = '77';
        input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));

        slightDelay();

        expect(document.activeElement).toBe(getTree().querySelector('input[data-tree-path="0-1-0"]'));
    });

    test("editing a literal and then pressing shift + tab to another literal sets the focus to the previous input element", () => {
        const input = getTree().querySelector('input[data-tree-path="0-1-0"]') as HTMLInputElement;
        input.value = '77';
        input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab', shiftKey: true}));
        input.blur();

        slightDelay();

        expect(document.activeElement).toBe(getTree().querySelector('input[data-tree-path="0-0-0"]'));
    });

    // does not work, limitation of focusing select elements
    // test("editing a literal and then tabbing to a dropdown sets the focus to it", () => {
    //     const input = getTree().querySelector('input[data-tree-path="0-1-0"]') as HTMLInputElement;
    //     doLiteralEdit(input, '77');
    //     input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));
    //
    //     slightDelay();
    //
    //     expect(document.activeElement).toBe(getTree().querySelector('select[data-tree-path="0-1"]'));
    // });
});

describe("user can perform a sequence of actions", () => {
    beforeEach(() => {
        pressStartNodeButton();
    });

    test("LArith: (1 + 2) * 3", () => {
        selectExprOption(getLeftmostExprDropdown(), "Times");  // Times
        selectExprOption(getLeftmostExprDropdown(), "Plus");  // Plus
        selectExprOption(getLeftmostExprDropdown(), "Num");  // Num
        selectExprOption(getLeftmostExprDropdown(), "Num");  // Num

        getTree().querySelector('input[data-tree-path="0-0-0"]')?.setAttribute('value', '1');
        getTree().querySelector('input[data-tree-path="0-0-0"]')?.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));
        slightDelay();

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]')?.getAttribute('value')).toBe('1');
        expect(document.activeElement).toBe(getTree()?.querySelector('input[data-tree-path="0-1-0"]'))

        document.activeElement?.setAttribute('value', '2');
        document.activeElement?.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter'}));

        slightDelay();

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]')?.getAttribute('value')).toBe('1');
        expect(getTree().querySelector('input[data-tree-path="0-1-0"]')?.getAttribute('value')).toBe('2');

        selectExprOption(getLeftmostExprDropdown(), "Num");  // Num
        doLiteralEdit(getTree().querySelector('input[data-tree-path="1-0"]') as HTMLInputElement, '3');

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]')?.getAttribute('value')).toBe('1');
        expect(getTree().querySelector('input[data-tree-path="0-1-0"]')?.getAttribute('value')).toBe('2');
        expect(getTree().querySelector('input[data-tree-path="1-0"]')?.getAttribute('value')).toBe('3');
    });
});
