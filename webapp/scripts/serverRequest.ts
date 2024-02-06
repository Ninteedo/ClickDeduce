import {getLangSelector, processAction, startNodeBlank} from "./clickdeduce-fastopt";

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
    return processAction(langName, modeName, actionName, nodeString, treePath, extraArgsStrings);
}
