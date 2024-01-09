export class MockResponse {
    private body: string;
    private status: number;
    private statusText: string;
    private headers: Map<string, string>;

    public ok: boolean = true;

    constructor(body: string, init: any = {}) {
        this.body = body;
        this.status = init.status || 200;
        this.statusText = init.statusText || 'OK';

        if (this.status >= 400) {
            this.ok = false;
        }

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
