import {spawn} from 'child_process';
import {afterAll, beforeAll, describe, expect, test} from "@jest/globals";
import http from "http";
import net from "net";
import kill from "tree-kill";

const port = 9005;
const command = `sbt "run --port ${port}"`;
let online = false;
// let serverThread = exec(command, async (err, stdout, stderr) => {
//     if (err) {
//         console.error(err);
//         return;
//     }
//     console.log(stdout);
//     console.log(stderr);
//     online = true;
//
//     await new Promise(resolve => setTimeout(resolve, 1000));
//     console.log("Still online");
// });
// online = true;
const url = `http://localhost:${port}/`

jest.setTimeout(60000);

let serverThread = spawn('sbt', [`"run --port ${port}"`], {shell: true});

beforeAll(async () => {
    const maxAttempts = 30;
    let attempts = 0;

    while (attempts < maxAttempts && !online) {
        try {
            await new Promise((resolve, reject) => {
                http.get(url, (res: any) => {
                    console.log(`Server is online. Attempt ${attempts + 1} of ${maxAttempts}`);
                    online = true;
                    resolve(null);
                }).on('error', reject);
            });
        } catch (error) {
            console.log(`Server not online, received ${error}`)
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
        http.get(url, (res: any) => {
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
    serverThread.kill();

    if (serverThread.killed) {
        console.log('Server thread was killed');
    } else {
        console.error('Server thread was not killed');
    }
    serverThread = null;

    setTimeout(() => {
        process.exit(0);
    }, 1000);

});
