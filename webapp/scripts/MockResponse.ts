export class MockResponse {
    private body: string;
    private status: number;
    private statusText: string;
    private headers: Map<string, string>;

    constructor(body: string, init: any = {}) {
        this.body = body;
        this.status = init.status || 200;
        this.statusText = init.statusText || 'OK';

        const headersIterable = Object.entries(init.headers || {});
        // @ts-ignore
        this.headers = new Map(headersIterable);
    }

    async text(): Promise<string> {
        return Promise.resolve(this.body);
    }

    async json(): Promise<any> {
        try {
            return Promise.resolve(JSON.parse(this.body));
        } catch (e) {
            return Promise.reject(e);
        }
    }
}
