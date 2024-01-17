import {spawn} from 'child_process';
import {afterAll, beforeAll, beforeEach, describe, expect, test} from "@jest/globals";
import http from "http";
import net from "net";
import kill from "tree-kill";
import {handleSubmit, initialise} from "./script";
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

jest.setTimeout(60000);

let serverThread = spawn('sbt', [`"run --port ${port}"`], {shell: true});

beforeAll(async () => {
    const maxAttempts = 30;
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

afterAll(() => {
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
    console.log('Initialised');
    console.log(document.body.innerHTML);
});

describe('lang selector is correctly initialised on load', () => {
    function getLangSelectorDiv() {
        return document.getElementById('lang-selector-div');
    }

    function getLangSelector() {
        return document.getElementById('lang-selector');
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
    function getStartNodeButton() {
        return document.getElementById('start-node-button');
    }

    async function pressStartNodeButton() {
        await handleSubmit(new Event(""), '/start-node-blank')
    }

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
});
