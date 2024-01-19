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
        (element.parentElement && hasClassOrParentHasClass(element.parentElement, className));
}
