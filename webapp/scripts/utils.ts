/**
 * Get the value of the selected mode radio button.
 */
export function getSelectedMode(): string {
    const selectedRadio = document.querySelector('input[name="mode"]:checked') as HTMLInputElement | null;
    if (selectedRadio) {
        return selectedRadio.value;
    }
    throw new Error("No mode selected");
}

/**
 * Get the value of the selected language from the language selector.
 */
export function getSelectedLanguage(): string {
    const langSelector: HTMLSelectElement = document.getElementById('lang-selector') as HTMLSelectElement;
    return langSelector.value;
}

/**
 * Checks if the given element has the given class, or if any of its parents have the given class.
 *
 * @param element the element to begin the search from
 * @param className the class to search for
 */
export function hasClassOrParentHasClass(element: HTMLElement, className: string): boolean {
    return element.classList.contains(className) ||
        (!!element.parentElement && hasClassOrParentHasClass(element.parentElement, className));
}

export function parseTreePath(treePath: string): number[] {
    return treePath.split('-').map((s) => parseInt(s)).filter((n) => !isNaN(n));
}

export function compareTreePaths(path1: string, path2: string): number {
    const readPath1 = parseTreePath(path1);
    const readPath2 = parseTreePath(path2);
    for (let i = 0; i < Math.min(readPath1.length, readPath2.length); i++) {
        if (readPath1[i] < readPath2[i]) {
            return -1;
        } else if (readPath1[i] > readPath2[i]) {
            return 1;
        }
    }
    if (readPath1.length < readPath2.length) {
        return -1;
    } else if (readPath1.length > readPath2.length) {
        return 1;
    }
    return 0;
}
