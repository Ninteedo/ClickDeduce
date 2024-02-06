import {spawn} from 'child_process';
import {afterAll, beforeAll, beforeEach, describe, expect, jest, test} from "@jest/globals";
import http from "http";
import net from "net";
import kill from "tree-kill";
import {initialise} from "../initialise";
import {
    changeLanguage,
    doLiteralEdit,
    getExprDropdownOptions,
    getLangSelector,
    getLeftmostExprDropdown,
    getStartNodeButton,
    getTree,
    loadHtmlTemplate,
    pressStartNodeButton,
    selectExprOption,
    slightDelay
} from "./helper";

const port = 9005;
const command = `sbt "run --port ${port}"`;
let online = false;
const siteUrl = `http://localhost:${port}/`

const nodeFetch = require('node-fetch');
global.fetch = function (url: string, options: any) {
    const finalUrl = new URL(url, siteUrl);
    return nodeFetch(finalUrl.toString(), options);
};

jest.setTimeout(120000);

let serverThread = spawn('sbt', [`"run --port ${port}"`], {shell: true});

beforeAll(async () => {
    const maxAttempts = 60;
    let attempts = 0;

    while (attempts < maxAttempts && !online) {
        let req: http.ClientRequest;
        try {
            await new Promise((resolve, reject) => {
                req = http.get(siteUrl, (res: any) => {
                    console.log(`Server is online. Attempt ${attempts + 1} of ${maxAttempts}`);
                    online = true;
                    resolve(null);
                });
                req.on('error', reject);
                req.setTimeout(5000, () => {
                    console.log("Request timed out");
                    reject(new Error('Request timed out'));
                });
            });
        } catch (error) {
            console.log(`Server not online, received ${error}`)
        } finally {
            req.destroy();
        }

        if (!online) {
            console.log(`Server not online. Attempt ${attempts + 1} of ${maxAttempts}`);
            attempts++;
            await new Promise(resolve => setTimeout(resolve, 2000));
        }
    }

    jest.setTimeout(10000);
    await new Promise(resolve => setTimeout(resolve, 500));

    if (attempts === maxAttempts) {
        throw new Error('Server did not start within the expected time.');
    }
});

afterAll(async () => {
    console.log('Killing server thread');
    kill(serverThread.pid, 'SIGKILL');
});

const indexHtml = loadHtmlTemplate('../pages/index');

describe('server is online', () => {
    test('server thread is running', () => {
        expect(serverThread).not.toBeNull();
    });

    test("can connect to server", () => {
        const client = new net.Socket();
        client.connect(port, 'localhost', function () {
            console.log('Connected');
            client.write('Hello, server! Love, Client.');
            client.end();
            client.destroySoon();
        });
    });

    test("response validation", done => {
        expect.assertions(1);
        http.get(siteUrl, (res: any) => {
            let data = '';

            res.on('data', (chunk: any) => {
                data += chunk;
            });

            res.on('end', () => {
                console.log(data);
                expect(data).toContain('<title>ClickDeduce</title>');
                done();
            });
        }).on('error', (err: Error) => {
            done(err);
        });
    });
});


describe('fetch works correctly', () => {
    test('fetch is defined', () => {
        expect(fetch).toBeDefined();
    });

    test('can fetch the index page', async () => {
        expect.assertions(1);
        const response = await fetch(siteUrl, {method: 'GET'});
        const text = await response.text();
        expect(text).toContain('<title>ClickDeduce</title>');
    });

    test('can fetch the lang selector', async () => {
        expect.assertions(1);
        await fetch(
            'get-lang-selector', {method: 'GET'}
        ).then(response =>
            response.json()
        ).then(json => {
            expect(json['langSelectorHtml']).toContain('<select id="lang-selector"');
        });
    });
});

beforeEach(async () => {
    document.body.innerHTML = indexHtml;
    await initialise(true);
});

describe('lang selector is correctly initialised on load', () => {
    function getLangSelectorDiv() {
        return document.getElementById('lang-selector-div');
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
            expect(option.getAttribute('value').length).toBeGreaterThan(0);
            expect(option.textContent).toBeTruthy();
            expect(option.textContent.length).toBeGreaterThan(0);
        });
    });

    test('each lang selector option has unique value and text content', () => {
        let values: Set<string> = new Set();
        let texts: Set<string> = new Set();

        getLangSelectorOptions().forEach(option => {
            values.add(option.getAttribute('value'));
            texts.add(option.textContent);
        });

        expect(values.size).toBe(getLangSelectorOptions().length);
        expect(texts.size).toBe(getLangSelectorOptions().length);
    });
});

describe('start node button has correct effect', () => {
    test('start node button is present', () => {
        expect(getStartNodeButton()).toBeTruthy();
    });

    test('clicking start node button results in a single node being added to the empty tree', async () => {
        const tree = document.getElementById('tree');
        await pressStartNodeButton();

        expect(tree.children).toHaveLength(1);
        expect(tree.querySelectorAll('.subtree')).toHaveLength(1);
    });

    test('the added node has the correct content', async () => {
        await pressStartNodeButton();
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(1);
        expect(document.querySelectorAll('input.expr-selector-input')).toHaveLength(1);
        expect(document.querySelectorAll('.expr-selector-dropdown > ul > li')).toHaveLength(3);
        expect(document.querySelectorAll('.annotation-axiom')).toHaveLength(1);
    });
});

describe('selecting an option from the expression choice dropdown has the correct effect', () => {
    beforeEach(async () => {
        await pressStartNodeButton();
    });

    test('selecting the "Num" option has correct effect', async () => {
        await selectExprOption(getLeftmostExprDropdown(), "Num", true);

        expect(document.querySelectorAll('.subtree')).toHaveLength(1);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(1);
        expect(document.querySelectorAll('select.expr-dropdown')).toHaveLength(0);
        expect(document.querySelectorAll('.annotation-axiom')).toHaveLength(1);
        expect(document.querySelectorAll('.expr')).toHaveLength(1);

        const node = document.querySelector('.subtree.axiom') as HTMLElement;
        expect(node.getAttribute('data-tree-path')).toBe('');

        expect(node.querySelectorAll('input')).toHaveLength(1);
        const input = node.querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('');
        expect(input.getAttribute('data-tree-path')).toBe('0');
    });

    test('selecting the "Plus" option has correct effect', async () => {
        await selectExprOption(getLeftmostExprDropdown(), "Plus", true);

        expect(document.querySelectorAll('.subtree')).toHaveLength(3);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(2);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(2);
        expect(document.querySelectorAll('.expr')).toHaveLength(3);
    });

    test('selecting the "Times" option then the "Plus" and "Num" options has correct effect', async () => {
        await selectExprOption(getLeftmostExprDropdown(), "Times", true);

        expect(document.querySelectorAll('.subtree')).toHaveLength(3);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(2);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(2);
        expect(document.querySelectorAll('.expr')).toHaveLength(3);

        await selectExprOption(getLeftmostExprDropdown(), "Plus", true);
        expect(document.querySelectorAll('.subtree')).toHaveLength(5);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(3);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(3);
        expect(document.querySelectorAll('.expr')).toHaveLength(5);

        await selectExprOption(getLeftmostExprDropdown(), "Num", true);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(2);

        await selectExprOption(getLeftmostExprDropdown(), "Num", true);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(1);

        await selectExprOption(getLeftmostExprDropdown(), "Num", true);
        expect(document.querySelectorAll('input.expr-selector-input:not([disabled])')).toHaveLength(0);

        const tree = document.getElementById('tree');

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
    beforeEach(async () => {
        await pressStartNodeButton();
    });

    test('changing the selected language with only the initial node present has correct effect', async () => {
        let prevSelect = getLeftmostExprDropdown();

        for (let i = 1; i < getLangSelector().options.length; i++) {
            await changeLanguage(i);
            expect(getLeftmostExprDropdown().outerHTML).not.toBe(prevSelect.outerHTML);
            expect(getExprDropdownOptions(getLeftmostExprDropdown()).length).toBeGreaterThan(getExprDropdownOptions(prevSelect).length);
        }
    });

    test('can change to a child language with more existing expressions', async () => {
        await changeLanguage(1);

        await selectExprOption(getLeftmostExprDropdown(), "IfThenElse");
        await selectExprOption(getLeftmostExprDropdown(), "Bool");
        await selectExprOption(getLeftmostExprDropdown(), "Plus");

        let prevSelect = getLeftmostExprDropdown();

        for (let i = 2; i < getLangSelector().options.length; i++) {
            await changeLanguage(i);
            expect(getLeftmostExprDropdown().outerHTML).not.toBe(prevSelect.outerHTML);
            expect(getExprDropdownOptions(getLeftmostExprDropdown()).length).toBeGreaterThan(getExprDropdownOptions(prevSelect).length);
        }
    });

    test('can change to a parent language, as long as the current tree only uses expressions present in the parent language', async () => {
        let languageSizes: number[] = [getExprDropdownOptions(getLeftmostExprDropdown()).length];
        await changeLanguage(1);
        languageSizes.push(getExprDropdownOptions(getLeftmostExprDropdown()).length);
        await changeLanguage(2);
        languageSizes.push(getExprDropdownOptions(getLeftmostExprDropdown()).length);

        await selectExprOption(getLeftmostExprDropdown(), "IfThenElse");
        await selectExprOption(getLeftmostExprDropdown(), "Bool");

        const nodeString = document.querySelector('.subtree.axiom').getAttribute('data-node-string');

        await changeLanguage(1);

        expect(getExprDropdownOptions(getLeftmostExprDropdown()).length).toBe(languageSizes[1]);
        expect(document.querySelector('.subtree.axiom').getAttribute('data-node-string')).toBe(nodeString);
    });

    // test('cannot change to a parent language if the current tree uses expressions not present in the parent language', async () => {
    //     await changeLanguage(1);
    //
    //     await selectExprOption(getLeftmostExprDropdown(), 5);
    //     await selectExprOption(getLeftmostExprDropdown(), 4);
    //     await selectExprOption(getLeftmostExprDropdown(), 2);
    //
    //     try {
    //         changeLanguage(0).then(() => {});
    //     } catch (error) {
    //         expect(error).toBeInstanceOf(ClickDeduceResponseError);
    //     }
    // });
});

describe('behaviour of editing literals is correct', () => {
    beforeEach(async () => {
        await pressStartNodeButton();
    });

    test('can edit the literal of a single literal node', async () => {
        await selectExprOption(getLeftmostExprDropdown(), "nNum");

        let input = getTree().querySelector('input') as HTMLInputElement;
        await doLiteralEdit(input, '123');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('123');
    });

    test('can edit the literal of a "Let"', async () => {
        await changeLanguage(2);
        await selectExprOption(getLeftmostExprDropdown(), "Let");

        let input = getTree().querySelector('input') as HTMLInputElement;
        await doLiteralEdit(input, 'foo');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('foo');
    });

    test('can edit one literal multiple times', async () => {
        await selectExprOption(getLeftmostExprDropdown(), "Num");

        let input = getTree().querySelector('input') as HTMLInputElement;
        await doLiteralEdit(input, '123');
        await doLiteralEdit(input, '456');
        await doLiteralEdit(input, '789');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('789');
    });
});

function getLeftmostTypeDropdown(): HTMLDivElement {
    return document.querySelector('.expr-selector-container[data-kind="type"]') as HTMLDivElement;
}

async function selectTypeOption(selector: HTMLDivElement, typeName: string): Promise<void> {
    await selectExprOption(selector, typeName);
}

describe('behaviour of manipulating trees with type selections is correct', () => {
    beforeEach(async () => {
        await pressStartNodeButton();
        await changeLanguage(3);
        await selectExprOption(getLeftmostExprDropdown(), "Lambda");  // lambda
    });

    test('can select a simple type (int)', async () => {
        await selectTypeOption(getLeftmostTypeDropdown(), "IntType");
        const typeSubtree = getTree().querySelector('.subtree[data-tree-path="1"]');
        expect(typeSubtree).toBeTruthy();
    });

    test('can select a simple type (bool)', async () => {
        await selectTypeOption(getLeftmostTypeDropdown(), "BoolType");
        const typeSubtree = getTree().querySelector('.subtree[data-tree-path="1"]');
        expect(typeSubtree).toBeTruthy();
    });

    test('can select a complex type (func int -> bool)', async () => {
        await selectTypeOption(getLeftmostTypeDropdown(), "Func");

        expect(getTree().querySelectorAll('.expr-selector-container[data-kind="type"]')).toHaveLength(2);

        await selectTypeOption(getLeftmostTypeDropdown(), "IntType");
        expect(getTree().querySelectorAll('.expr-selector-container[data-kind="type"]')).toHaveLength(1);
        await selectTypeOption(getLeftmostTypeDropdown(), "BoolType");
        expect(getTree().querySelectorAll('.expr-selector-container[data-kind="type"]')).toHaveLength(0);
    });
});

test('images are correctly loaded', async () => {
    // Fetch the HTML content of the page
    const response = await fetch("", {method: 'GET'});
    const html = await response.text();

    // Use DOMParser to parse the HTML content
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');

    // Get all image elements
    const images = doc.getElementsByTagName('img');

    for (let i = 0; i < images.length; i++) {
        const image = images[i];
        expect(image.getAttributeNames()).toContain('src');

        const src = image.getAttribute('src');
        expect(src.length).toBeGreaterThan(0);

        console.log(`Fetching image ${src}`);

        // Fetch the image
        const imageResponse = await fetch(src, {method: 'GET'});
        expect(imageResponse.status).toBe(200);
        expect(imageResponse.headers.get('content-type')).toContain('image/');
    }
});

describe("delete, copy, and paste buttons behave correctly", () => {
    function contextMenuSelect(element: HTMLElement): void {
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

    beforeEach(async () => {
        await slightDelay(50);
        await pressStartNodeButton();
        await changeLanguage(3);
        await selectExprOption(getLeftmostExprDropdown(), "Plus");
        await selectExprOption(getLeftmostExprDropdown(), "Num");
        await doLiteralEdit(getTree().querySelector('input.literal[data-tree-path="0-0"]') as HTMLInputElement, 'foo');
        await selectExprOption(getLeftmostExprDropdown(), "Bool");
        await doLiteralEdit(getTree().querySelector('input.literal[data-tree-path="1-0"]') as HTMLInputElement, 'bar');
        console.log('Tree setup done');
    });

    test("pressing delete clears the selected node", async () => {
        expect.assertions(2);

        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        const deleteButton = document.getElementById('delete-button');
        deleteButton.click();

        await new Promise(resolve => setTimeout(resolve, 50));

        expect(getTree().querySelector('.subtree[data-tree-path="0"]')).toBeTruthy();
        expect(getTree().querySelector('.subtree[data-tree-path="0-1"]')).toBeFalsy();
    });

    test("clicking paste on same element after copying it results in the same tree", async () => {
        const element = document.querySelector('[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element);

        const copyButton = document.getElementById('copy-button');
        copyButton.click();

        const initialTreeState = getTree().innerHTML;

        contextMenuSelect(element);

        const pasteButton = document.getElementById('paste-button');
        pasteButton.click();

        await new Promise(resolve => setTimeout(resolve, 50));

        expect(getTree().innerHTML).toBe(initialTreeState);
    });

    test("clicking paste on another element after copying one results in the correct tree", async () => {
        const element1 = document.querySelector('.subtree[data-tree-path="0"]') as HTMLElement;
        contextMenuSelect(element1);

        const copyButton = document.getElementById('copy-button');
        copyButton.click();

        const element2 = document.querySelector('.subtree[data-tree-path="1"]') as HTMLElement;
        contextMenuSelect(element2);

        const pasteButton = document.getElementById('paste-button');
        pasteButton.click();

        await new Promise(resolve => setTimeout(resolve, 50));

        expect(getTree().innerHTML).not.toBe(element1.innerHTML);
    });

    test("clicking paste after changing tree state makes the correct request to the server", async () => {
        contextMenuSelect(document.querySelector('[data-tree-path="0"]'));
        document.getElementById('copy-button').click();
        await new Promise(resolve => setTimeout(resolve, 50));

        document.getElementById('undoButton').click();
        await new Promise(resolve => setTimeout(resolve, 50));

        contextMenuSelect(document.querySelector('[data-tree-path=""]'));
        document.getElementById('paste-button').click();

        await new Promise(resolve => setTimeout(resolve, 50));

        expect(document.querySelector('[data-tree-path=""]')).toBeTruthy();
        expect(document.querySelector('[data-tree-path="0-0"]')).toBeFalsy();
        expect(document.querySelector('[data-tree-path="1"]')).toBeFalsy();
    });
});

describe("input focus is preserved when tabbing as the tree is updated", () => {
    beforeEach(async () => {
        await pressStartNodeButton();
        await selectExprOption(getLeftmostExprDropdown(), "Times");
        await selectExprOption(getLeftmostExprDropdown(), "Plus");
        await selectExprOption(getLeftmostExprDropdown(), "Num");
        await selectExprOption(getLeftmostExprDropdown(), "Num");
        await doLiteralEdit(getTree().querySelector('input[data-tree-path="0-0-0"]') as HTMLInputElement, '44');
        await doLiteralEdit(getTree().querySelector('input[data-tree-path="0-1-0"]') as HTMLInputElement, '55');
    });

    test("editing a literal and then tabbing to the next input element updates the tree correctly", async () => {
        const input = getTree().querySelector('input[data-tree-path="0-0-0"]') as HTMLInputElement;
        input.value = '77';
        input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));
        input.dispatchEvent(new Event('blur'));

        await slightDelay();

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]').getAttribute('value')).toBe('77');
    });

    test("editing a literal and then tabbing to another literal sets the focus to the next input element", async () => {
        const input = getTree().querySelector('input[data-tree-path="0-0-0"]') as HTMLInputElement;
        input.value = '77';
        input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));
        input.dispatchEvent(new Event('blur'));

        await slightDelay();

        expect(document.activeElement).toBe(getTree().querySelector('input[data-tree-path="0-1-0"]'));
    });

    test("editing a literal and then pressing shift + tab to another literal sets the focus to the previous input element", async () => {
        const input = getTree().querySelector('input[data-tree-path="0-1-0"]') as HTMLInputElement;
        input.value = '77';
        input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab', shiftKey: true}));
        input.blur();

        await slightDelay();

        expect(document.activeElement).toBe(getTree().querySelector('input[data-tree-path="0-0-0"]'));
    });

    // does not work, limitation of focusing select elements
    // test("editing a literal and then tabbing to a dropdown sets the focus to it", async () => {
    //     const input = getTree().querySelector('input[data-tree-path="0-1-0"]') as HTMLInputElement;
    //     await doLiteralEdit(input, '77');
    //     input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));
    //
    //     await slightDelay();
    //
    //     expect(document.activeElement).toBe(getTree().querySelector('select[data-tree-path="0-1"]'));
    // });
});

describe("user can perform a sequence of actions", () => {
    beforeEach(async () => {
        await pressStartNodeButton();
    });

    test("LArith: (1 + 2) * 3", async () => {
        await selectExprOption(getLeftmostExprDropdown(), "Times");  // Times
        await selectExprOption(getLeftmostExprDropdown(), "Plus");  // Plus
        await selectExprOption(getLeftmostExprDropdown(), "Num");  // Num
        await selectExprOption(getLeftmostExprDropdown(), "Num");  // Num

        getTree().querySelector('input[data-tree-path="0-0-0"]').setAttribute('value', '1');
        getTree().querySelector('input[data-tree-path="0-0-0"]').dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));
        getTree().querySelector('input[data-tree-path="0-0-0"]').dispatchEvent(new Event('blur'));
        await slightDelay();

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]').getAttribute('value')).toBe('1');
        expect(document.activeElement).toBe(getTree().querySelector('input[data-tree-path="0-1-0"]'))

        document.activeElement.setAttribute('value', '2');
        document.activeElement.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter'}));

        await slightDelay();

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]').getAttribute('value')).toBe('1');
        expect(getTree().querySelector('input[data-tree-path="0-1-0"]').getAttribute('value')).toBe('2');

        await selectExprOption(getLeftmostExprDropdown(), "Num");  // Num
        await doLiteralEdit(getTree().querySelector('input[data-tree-path="1-0"]') as HTMLInputElement, '3');

        expect(getTree().querySelector('input[data-tree-path="0-0-0"]').getAttribute('value')).toBe('1');
        expect(getTree().querySelector('input[data-tree-path="0-1-0"]').getAttribute('value')).toBe('2');
        expect(getTree().querySelector('input[data-tree-path="1-0"]').getAttribute('value')).toBe('3');
    });
});
