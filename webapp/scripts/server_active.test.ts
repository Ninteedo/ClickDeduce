import {exec} from 'child_process';
import {afterAll, beforeAll, describe, expect, test} from "@jest/globals";
import http from "http";
import net from "net";

const port = 9000;
const command = `sbt "run --port ${port}"`;
let online = false;
const serverThread = exec(command, async (err, stdout, stderr) => {
    if (err) {
        console.error(err);
        return;
    }
    console.log(stdout);
    console.log(stderr);
    online = true;

    await new Promise(resolve => setTimeout(resolve, 1000));
    console.log("Still online");
});
// online = true;
const url = `http://localhost:${port}/`

jest.setTimeout(30000);

// let serverThread = spawn('sbt', [`"run --port ${port}"`], {shell: true});

beforeAll(async () => {
    serverThread.stdout.on('data', (data) => {
        console.log(`stdout: ${data}`);
        if (data.includes('Server online')) {
            online = true;
        }
    });

    serverThread.stderr.on('data', (data) => {
        console.error(`stderr: ${data}`);
    });

    serverThread.on('close', (code) => {
        console.log(`child process exited with code ${code}`);
    });

    const maxAttempts = 20;
    let attempts = 0;

    while (attempts < maxAttempts && !online) {
        // try {
        //     await new Promise((resolve, reject) => {
        //         http.get(url, (res: any) => {
        //             console.log(`Server is online. Attempt ${attempts + 1} of ${maxAttempts}`);
        //             online = true;
        //             resolve(null);
        //         }).on('error', reject);
        //     });
        // } catch (error) {
        //     console.log(`Server not online, received ${error}`)
        // }

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
        client.connect(port, 'localhost', function() {
            console.log('Connected');
            client.write('Hello, server! Love, Client.');
        });
    });

    test("response validation", done => {
        http.get(url, (res: any) => {
            let data = '';

            res.on('data', (chunk: any) => {
                data += chunk;
            });

            res.on('end', () => {
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
    serverThread.on('exit', (code, signal) => {
        if (signal === 'SIGINT') {
            console.log('Server thread was killed');
        } else {
            console.error(`Server thread was not killed, exited with code ${code} and signal ${signal}`);
        }
    });
    serverThread.kill('SIGINT');

    if (serverThread.killed) {
        console.log('Server thread was killed');
    } else {
        console.error('Server thread was not killed');
    }
});
