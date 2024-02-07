import {getLangSelector, processAction, startNodeBlank} from "./clickdeduce-opt";

let actionHistory: {
    langName: string;
    modeName: string;
    actionName: string;
    nodeString: string;
    treePath: string;
    extraArgsStrings: string[];
}[] = [];

export function getActionHistory(): {
    langName: string;
    modeName: string;
    actionName: string;
    nodeString: string;
    treePath: string;
    extraArgsStrings: string[];
}[] {
    return actionHistory;
}

export function checkActionExecuted(
    langName: string,
    modeName: string,
    actionName: string,
    nodeString: string,
    treePath: string,
    extraArgs: any[]
): boolean {
    const extraArgsStrings: string[] = extraArgs.map(arg => arg.toString());
    return actionHistory.some(
        action => action.langName === langName &&
            action.modeName === modeName &&
            action.actionName === actionName &&
            action.nodeString === nodeString &&
            action.treePath === treePath &&
            action.extraArgsStrings.every((arg, i) => arg === extraArgsStrings[i])
    );
}

export function clearActionHistory(): void {
    actionHistory = [];
}

let startNodeBlankHistory: {langName: string}[] = [];

export function getStartNodeBlankHistory(): {langName: string}[] {
    return startNodeBlankHistory;
}

export function checkStartNodeBlankExecuted(langName: string): boolean {
    return startNodeBlankHistory.some(start => start.langName === langName);
}

export function clearStartNodeBlankHistory(): void {
    startNodeBlankHistory = [];
}

export async function getLangSelectorRequest(): Promise<Response> {
    return fetch('get-lang-selector', {method: 'GET'});
}

export function getLangSelectorNew(): string {
    return getLangSelector();
}

export async function postStartNodeBlank(selectedLanguage: string): Promise<Response> {
    return fetch('/start-node-blank', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            langName: selectedLanguage,
        })
    });
}

export function postStartNodeBlankNew(selectedLanguage: string): [string, string] {
    startNodeBlankHistory.push({langName: selectedLanguage});
    // @ts-ignore
    return startNodeBlank(selectedLanguage);
}

export async function postProcessAction(
    langName: string,
    modeName: string,
    actionName: string,
    nodeString: string,
    treePath: string,
    extraArgs: any[]
): Promise<Response> {
    return fetch("/process-action", {
        method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify({
            langName,
            modeName,
            actionName,
            nodeString,
            treePath,
            extraArgs
        })
    });
}

export function postProcessActionNew(
    langName: string,
    modeName: string,
    actionName: string,
    nodeString: string,
    treePath: string,
    extraArgs: any[]
): [string, string] {
    const extraArgsStrings: string[] = extraArgs.map(arg => arg.toString());
    actionHistory.push({langName, modeName, actionName, nodeString, treePath, extraArgsStrings});
    // @ts-ignore
    return processAction(langName, modeName, actionName, nodeString, treePath, extraArgsStrings);
}
