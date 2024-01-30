import {afterAll, beforeAll, beforeEach, expect, jest} from "@jest/globals";
import {MockResponse} from "./MockResponse";
import {getLeftmostExprDropdown, loadHtmlTemplate, selectExprOption} from "./helper";
import {initialise} from "../initialise";
import * as NS from "../../test_resources/node_strings";
import {handleLiteralChanged, handleSubmit} from "../actions";

let dummyFetchResponse: any = null;
let actionFetchResponse: { nodeString: string, html: string } = null;
let actionErrorMessage: string = null;
let requestsReceived: { url: string, request: any }[] = [];

const invalidResourceResponse: MockResponse = new MockResponse("The requested resource could not be found.", {
    status: 404,
    statusText: 'Not Found',
    headers: {
        'Content-type': 'application/json'
    }
});

export const langSelectorLanguages = ["LArith", "LIf"];
export const optionsHtml = langSelectorLanguages.map(lang => {
    return `<option value="${lang}">${lang}</option>`;
}).join('\n');

export const langSelectorHtml = `
    <select id="lang-selector" name="lang-name">
      ${optionsHtml}
    </select>
`;

export const startNodeBlankArithHTML = loadHtmlTemplate('start_node_blank_arith');
export const defaultHtml = loadHtmlTemplate('../pages/index');

export const mockEvent = {preventDefault: jest.fn()} as unknown as Event;

export function resetRequestTracking(): void {
    dummyFetchResponse = null;
    actionFetchResponse = null;
    actionErrorMessage = null;
    requestsReceived = [];
}

export function checkActionRequestExecuted(actionName: string, langName: string, modeName: string, nodeString: string,
                                           treePath: string, extraArgs: string[]): void {
    const correctRequest = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            langName,
            modeName,
            actionName,
            nodeString,
            treePath,
            extraArgs
        })
    }
    expect(requestsReceived).toContainEqual({url: 'process-action', request: correctRequest});
}

export function fetchMock(url: string, request: any): Promise<Response> {
    if (url.startsWith('/')) {
        url = url.substring(1);
    }
    requestsReceived.push({url, request});

    console.log("fetchMock called with url: " + url + " and request: " + JSON.stringify(request));

    let responseJson: any = null;
    if (url === 'get-lang-selector') {
        if (request['method'] === 'GET') {
            responseJson = {langSelectorHtml};
        }
    } else if (url === 'dummy-url') {
        responseJson = dummyFetchResponse;
    } else if (url === 'start-node-blank') {
        if (request['method'] === 'POST') {
            responseJson = {nodeString: "ExprChoiceNode()", html: startNodeBlankArithHTML};
        }
    } else if (url === 'process-action') {
        if (request['method'] === 'POST') {
            if (actionFetchResponse === null) {
                if (actionErrorMessage !== null) {
                    // @ts-ignore
                    return Promise.resolve(new MockResponse(actionErrorMessage,
                        {status: 400, statusText: 'Bad Request'}));
                } else {
                    actionFetchResponse = {nodeString: "", html: ""};
                }
            }
            responseJson = actionFetchResponse;
            actionFetchResponse = null;
        }
    }

    // resource not found
    if (responseJson === null) {
        // @ts-ignore
        return Promise.resolve(invalidResourceResponse);
    }

    const response: MockResponse = new MockResponse(JSON.stringify(responseJson),
        {
            status: 200,
            headers: {
                'Content-type': 'application/json'
            }
        }
    );
    // @ts-ignore
    return Promise.resolve(response);
}

export function setUpFetchMock(): void {
    global.fetch = jest.fn(fetchMock);

// mock panzoom module, doesn't need to do anything
    jest.mock('panzoom', () => ({
        __esModule: true,
        default: jest.fn().mockImplementation(() => ({}))
    }))
}

export function setActionFetchResponse(nodeString: string, html: string): void {
    actionFetchResponse = {nodeString, html};
}

export function setActionFetchResponseData(data: { nodeString: string, html: string }): void {
    actionFetchResponse = data;
}

export function setActionErrorMessage(message: string): void {
    actionErrorMessage = message;
}

export function setDummyFetchResponse(response: any): void {
    dummyFetchResponse = response;
}

export function getRequestsReceived(): { url: string, request: any }[] {
    return requestsReceived;
}

export function setUpJestBeforeAndAfter(): void {
    beforeAll(() => {
        setUpFetchMock();
    });

    beforeEach(async () => {
        resetRequestTracking();
        document.body.innerHTML = defaultHtml;
        await initialise();
    });

    afterAll(() => {
        jest.clearAllMocks();
    })
}

const plusNodeArithHTML = loadHtmlTemplate('plus_node_arith');

export async function prepareExampleTimesTree(): Promise<void> {
    const nodeString2 = NS.TIMES_EMPTY;
    const html2 = plusNodeArithHTML;

    const nodeString3 = NS.TIMES_LEFT_NUM_RIGHT_EMPTY;
    const html3 = loadHtmlTemplate('times_left_num_right_empty');

    const nodeString4 = NS.TIMES_LEFT_FILLED_NUM_RIGHT_EMPTY;
    const html4 = loadHtmlTemplate('times_left_filled_num_right_empty');

    await handleSubmit(mockEvent, '/start-node-blank');
    setActionFetchResponse(nodeString2, html2);
    await selectExprOption(getLeftmostExprDropdown(), "Num");
    setActionFetchResponse(nodeString3, html3);
    await selectExprOption(getLeftmostExprDropdown(), "Num");
    setActionFetchResponse(nodeString4, html4);
    await handleLiteralChanged(document.querySelector('input[type="text"]') as HTMLInputElement);
}
