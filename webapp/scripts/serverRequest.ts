export async function getLangSelectorRequest(): Promise<Response> {
    return fetch('get-lang-selector', {method: 'GET'});
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
    })
}
