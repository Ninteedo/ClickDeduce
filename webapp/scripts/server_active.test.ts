import {spawn} from 'child_process';
import {afterAll, beforeAll, beforeEach, describe, expect, test} from "@jest/globals";
import http from "http";
import net from "net";
import kill from "tree-kill";
import {handleDropdownChange, handleSubmit, initialise} from "./script";
import fs from "fs";
import path from "path";

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

    await new Promise(resolve => setTimeout(resolve, 1000));

    if (attempts === maxAttempts) {
        throw new Error('Server did not start within the expected time.');
    }
});

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

afterAll(async () => {
    console.log('Killing server thread');
    kill(serverThread.pid, 'SIGKILL');
});

function loadHtmlTemplate(filename: string): string {
    const readResult: string = fs.readFileSync(path.resolve(__dirname, '../test_resources', `${filename}.html`), 'utf8');
    return readResult.replace(/\r\n/g, '\n');
}

const indexHtml = loadHtmlTemplate('../pages/index');

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
    await initialise();
});

function getTree() {
    return document.getElementById('tree');
}

function getStartNodeButton() {
    return document.getElementById('start-node-button');
}

async function pressStartNodeButton() {
    await handleSubmit(new Event(""), '/start-node-blank')
}

function getLangSelector() {
    return document.getElementById('lang-selector') as HTMLSelectElement;
}

async function changeLanguage(langIndex: number): Promise<void> {
    const langSelector = getLangSelector();
    langSelector.selectedIndex = langIndex;
    langSelector.dispatchEvent(new Event('change'));
    await new Promise(resolve => setTimeout(resolve, 50));
}

function getLeftmostExprDropdown(): HTMLSelectElement {
    return document.querySelector('select.expr-dropdown:not([disabled])') as HTMLSelectElement;
}

async function selectExprOption(dropdown: HTMLSelectElement, optionIndex: number): Promise<void> {
    dropdown.selectedIndex = optionIndex;
    await handleDropdownChange(dropdown, 'expr');
}

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
        const treeContent = tree.innerHTML;
        await pressStartNodeButton();

        expect(tree.innerHTML).not.toBe(treeContent);
        expect(tree.children).toHaveLength(1);
        expect(tree.querySelectorAll('.subtree')).toHaveLength(1);
    });

    test('the added node has the correct content', async () => {
        await pressStartNodeButton();
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(1);
        expect(document.querySelectorAll('select.expr-dropdown')).toHaveLength(1);
        expect(document.querySelectorAll('select.expr-dropdown option')).toHaveLength(4);
        expect(document.querySelectorAll('.annotation-axiom')).toHaveLength(1);
    });
});

describe('selecting an option from the expression choice dropdown has the correct effect', () => {
    beforeEach(async () => {
        await pressStartNodeButton();
    });

    test('selecting the "Num" option has correct effect', async () => {
        await selectExprOption(getLeftmostExprDropdown(), 1);

        expect(document.querySelectorAll('.subtree')).toHaveLength(1);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(1);
        expect(document.querySelectorAll('select.expr-dropdown')).toHaveLength(0);
        expect(document.querySelectorAll('.annotation-axiom')).toHaveLength(1);
        expect(document.querySelectorAll('.expr')).toHaveLength(1);

        const node = document.querySelector('.subtree.axiom') as HTMLElement;
        expect(node.getAttribute('data-node-string')).toBe('VariableNode(\"Num\", List(LiteralNode(\"\")))');
        expect(node.getAttribute('data-tree-path')).toBe('');

        expect(node.querySelectorAll('input')).toHaveLength(1);
        const input = node.querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('');
        expect(input.getAttribute('data-tree-path')).toBe('0');
    });

    test('selecting the "Plus" option has correct effect', async () => {
        await selectExprOption(getLeftmostExprDropdown(), 2);

        expect(document.querySelectorAll('.subtree')).toHaveLength(3);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(2);
        expect(document.querySelectorAll('select.expr-dropdown:not([disabled])')).toHaveLength(2);
        expect(document.querySelectorAll('.expr')).toHaveLength(3);
    });

    test('selecting the "Times" option then the "Plus" and "Num" options has correct effect', async () => {
        await selectExprOption(getLeftmostExprDropdown(), 3);

        expect(document.querySelectorAll('.subtree')).toHaveLength(3);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(2);
        expect(document.querySelectorAll('select.expr-dropdown:not([disabled])')).toHaveLength(2);
        expect(document.querySelectorAll('.expr')).toHaveLength(3);

        await selectExprOption(getLeftmostExprDropdown(), 2);
        expect(document.querySelectorAll('.subtree')).toHaveLength(5);
        expect(document.querySelectorAll('.subtree.axiom')).toHaveLength(3);
        expect(document.querySelectorAll('select.expr-dropdown:not([disabled])')).toHaveLength(3);
        expect(document.querySelectorAll('.expr')).toHaveLength(5);

        await selectExprOption(getLeftmostExprDropdown(), 1);
        expect(document.querySelectorAll('select.expr-dropdown:not([disabled])')).toHaveLength(2);

        await selectExprOption(getLeftmostExprDropdown(), 1);
        expect(document.querySelectorAll('select.expr-dropdown:not([disabled])')).toHaveLength(1);

        await selectExprOption(getLeftmostExprDropdown(), 1);
        expect(document.querySelectorAll('select.expr-dropdown:not([disabled])')).toHaveLength(0);

        const tree = document.getElementById('tree');

        expect(tree.querySelectorAll('.subtree')).toHaveLength(5);
        expect(tree.querySelectorAll('.subtree.axiom')).toHaveLength(3);
        expect(tree.querySelectorAll('.expr')).toHaveLength(5);
        expect(tree.querySelectorAll('select')).toHaveLength(0);
        expect(tree.querySelectorAll('input:not([readonly])')).toHaveLength(3);
        expect(tree.querySelectorAll('.annotation-new')).toHaveLength(2);

        const inputDataPaths = ['0-0-0', '0-1-0', '1-0'];
        tree.querySelectorAll('input:not([readonly])').forEach((input, index) => {
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
            expect(getLeftmostExprDropdown().options.length).toBeGreaterThan(prevSelect.options.length);
        }
    });

    test('can change to a child language with more existing nodes', async () => {
        await changeLanguage(1);

        await selectExprOption(getLeftmostExprDropdown(), 5);
        await selectExprOption(getLeftmostExprDropdown(), 4);
        await selectExprOption(getLeftmostExprDropdown(), 2);

        let prevSelect = getLeftmostExprDropdown();

        for (let i = 2; i < getLangSelector().options.length; i++) {
            await changeLanguage(i);
            expect(getLeftmostExprDropdown().outerHTML).not.toBe(prevSelect.outerHTML);
            expect(getLeftmostExprDropdown().options.length).toBeGreaterThan(prevSelect.options.length);
        }
    });

    test('can change to a parent language, as long as the current tree only uses expressions present in the parent language', async () => {
        let languageSizes: number[] = [getLeftmostExprDropdown().options.length];
        await changeLanguage(1);
        languageSizes.push(getLeftmostExprDropdown().options.length);
        await changeLanguage(2);
        languageSizes.push(getLeftmostExprDropdown().options.length);

        await selectExprOption(getLeftmostExprDropdown(), 5);
        await selectExprOption(getLeftmostExprDropdown(), 4);

        const nodeString = document.querySelector('.subtree.axiom').getAttribute('data-node-string');

        await changeLanguage(1);

        expect(getLeftmostExprDropdown().options.length).toBe(languageSizes[1]);
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
    async function doLiteralEdit(input: HTMLInputElement, newValue: string): Promise<void> {
        input.value = newValue;
        input.dispatchEvent(new Event('change'));
        await new Promise(resolve => setTimeout(resolve, 100));
    }

    beforeEach(async () => {
        await pressStartNodeButton();
    });

    test('can edit the literal of a single literal node', async () => {
        await selectExprOption(getLeftmostExprDropdown(), 1);

        let input = getTree().querySelector('input') as HTMLInputElement;
        await doLiteralEdit(input, '123');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('123');
    });

    test('can edit the literal of a "Let"', async () => {
        await changeLanguage(2);
        await selectExprOption(getLeftmostExprDropdown(), 8);

        let input = getTree().querySelector('input') as HTMLInputElement;
        await doLiteralEdit(input, 'foo');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('foo');
    });

    test('can edit one literal multiple times', async () => {
        await selectExprOption(getLeftmostExprDropdown(), 1);

        let input = getTree().querySelector('input') as HTMLInputElement;
        await doLiteralEdit(input, '123');
        await doLiteralEdit(input, '456');
        await doLiteralEdit(input, '789');

        input = getTree().querySelector('input') as HTMLInputElement;
        expect(input.value).toBe('789');
    });
});
