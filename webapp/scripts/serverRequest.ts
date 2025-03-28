import {
    checkTask,
    exprParsePreviewHtml,
    exprText,
    getExprRulePreview,
    getLangSelector,
    getTasks,
    parseExpr,
    processAction,
    startNodeBlank
} from "scalajs:main.js";

import {Task} from "./components/tasks/task";
import {DisplayMode} from "./globals/displayMode";

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

export function clearActionHistory(): void {
    actionHistory = [];
}

let startNodeBlankHistory: { langName: string }[] = [];

export function getStartNodeBlankHistory(): { langName: string }[] {
    return startNodeBlankHistory;
}

export function clearStartNodeBlankHistory(): void {
    startNodeBlankHistory = [];
}

export function getLangSelectorNew(): string {
    return getLangSelector();
}

export function postStartNodeBlankNew(selectedLanguage: string): [string, string] {
    startNodeBlankHistory.push({langName: selectedLanguage});
    return startNodeBlank(selectedLanguage);
}

export function postProcessActionNew(
    langName: string,
    modeName: DisplayMode,
    actionName: string,
    nodeString: string,
    treePath: string,
    extraArgs: any[]
): [string, string] {
    const extraArgsStrings: string[] = extraArgs.map(arg => arg.toString());
    actionHistory.push({langName, modeName, actionName, nodeString, treePath, extraArgsStrings});
    return processAction(langName, modeName, actionName, nodeString, treePath, extraArgsStrings);
}

export function getTasksList(langName: string): Task[] {
    return getTasks(langName).map((details: any[]) => {
        return {
            name: details[0],
            description: details[1],
            difficulty: details[2]
        };
    });
}

export function checkTaskFulfilled(langName: string, taskName: string, nodeString: string): boolean {
    return checkTask(langName, taskName, nodeString);
}

export function getRulePreview(langName: string, exprName: string): string {
    return getExprRulePreview(langName, exprName);
}

export function parseExprText(langName: string, exprText: string): string {
    return parseExpr(langName, exprText);
}

export function getExprText(langName: string, nodeString: string, treePathString: string): string {
    return exprText(langName, nodeString, treePathString);
}

export function getExprParsePreviewHtml(
    langName: string,
    exprText: string,
    modeName: DisplayMode,
    nodeString: string,
    treePathString: string
): [number, string] {
    return exprParsePreviewHtml(langName, exprText, modeName, nodeString, treePathString);
}
