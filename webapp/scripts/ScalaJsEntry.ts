export declare const ScalaJsEntry: {
    startNodeBlank: (langName: string) => [string, string],
    getLangSelector: () => string,
    processAction: (langName: string, modeName: string, actionName: string, nodeString: string, treePath: string, extraArgs: string[]) => [string, string],
    convertToLaTeX: (langName: string, modeName: string, nodeString: string) => string,
}
