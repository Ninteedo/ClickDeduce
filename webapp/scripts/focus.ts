import {getTreePathOfElement} from "./globals/elements";
import {AbstractTreeInput} from "./components/abstractTreeInput";
import {compareTreePaths, parseTreePath} from "./utils";
import {getActiveInputs} from "./activeInputs";

/**
 * Changes the focus to the next input element when TAB is pressed.
 * @param e the keydown event
 */
export function handleTabPressed(e: KeyboardEvent): void {
    if (e.key === 'Tab' && (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) {
        e.preventDefault();
        const targetOuterPath: string = getTreePathOfElement(e.target);
        e.target.dispatchEvent(new Event('blur'));
        handleTabPressedFromPath(targetOuterPath, e.shiftKey ? -1 : 1);
    }
}

export function handleTabPressedFromPath(treePath: string, change: number): void {
    const activeInputPaths: string[] = getActiveInputs().map(input => input.getTreePath());
    let activeElemIndex = activeInputPaths.indexOf(treePath);
    activeElemIndex += change;
    if (activeElemIndex < 0) {
        activeElemIndex = getActiveInputs().length - 1;
    } else if (activeElemIndex >= getActiveInputs().length) {
        activeElemIndex = 0;
    }
    getActiveInputs()[activeElemIndex].focus();
    setNextFocusElement(null);
}

/**
 * Sets the focus to the input element with the given tree path.
 * If that element is not found, will instead try to focus on the first input element in a subtree of that path.
 * If there is no input element in the subtree, will do nothing.
 */
export function setFocusElement(path: string): void {
    let focusedElement = getActiveInputs().find(input => compareTreePaths(path, input.getTreePath()) <= 0);

    if (!focusedElement) {
        const parsedPath = parseTreePath(path);

        function isBefore(a: number[], b: number[]): boolean {
            let i = 0;
            while (i < a.length && i < b.length) {
                if (a[i] < b[i]) {
                    return true;
                } else if (a[i] > b[i]) {
                    return false;
                }
                i++;
            }
            return true;
        }

        function findLast<T>(arr: T[], pred: (elem: T) => boolean): T | undefined {
            for (let i = arr.length - 1; i >= 0; i--) {
                if (pred(arr[i])) return arr[i];
            }
            return undefined;
        }

        focusedElement = findLast(getActiveInputs(), input => isBefore(parseTreePath(input.getTreePath()), parsedPath));
    }

    focusedElement?.focus();
}

export let nextFocusPath: string | null = null;

export function setNextFocusElement(input: AbstractTreeInput | null): void {
    nextFocusPath = input?.getTreePath() ?? null;
}

export function setNextFocusTreePath(path: string | null): void {
    nextFocusPath = path;
}

export function getNextFocusTreePath(): string | null {
    return nextFocusPath;
}

export function getFocusedElementTreePath(): string | null {
    const active = document.activeElement;
    if (!active || !active.hasAttribute('data-tree-path')) return null;
    return active.getAttribute('data-tree-path')!;
}

export function getFocusedSubtreePath(): string | null {
    const elPath = getFocusedElementTreePath();
    if (!elPath) return null;
    if (!elPath.includes('-')) return "";
    return elPath.slice(0, elPath.lastIndexOf('-'));
}
